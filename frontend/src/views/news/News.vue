<template>
  <div class="news">
    <el-card>
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="请输入标题关键词" clearable />
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <el-button type="primary" @click="handleAdd">新增</el-button>
      </div>

      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="title" label="标题" />
        <el-table-column label="发布日期" width="120">
          <template #default="{ row }">
            {{ (row.pubDate || '').toString().slice(0, 10) }}
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" width="200">
          <template #default="{ row }">
            <el-tag v-for="(tag, index) in (row.tags || '').split(',')" :key="index" size="small" style="margin-right: 5px;">
              {{ tag }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="发布日期" prop="pubDate">
          <el-date-picker v-model="form.pubDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-input v-model="form.tags" placeholder="多个标签用逗号分隔" />
        </el-form-item>
        <el-form-item label="内容" prop="contentText">
          <el-input v-model="form.contentText" type="textarea" :rows="10" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="详情" width="800px">
      <div class="detail-content">
        <h3>{{ detailData.title }}</h3>
        <p class="detail-meta">
          <span>发布日期：{{ (detailData.pubDate || '').toString().slice(0, 10) }}</span>
          <span v-if="detailData.tags" style="margin-left: 20px;">
            标签：
            <el-tag v-for="(tag, index) in detailData.tags.split(',')" :key="index" size="small" style="margin-right: 5px;">
              {{ tag }}
            </el-tag>
          </span>
        </p>
        <pre class="detail-body">{{ detailData.contentText || '' }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { newsApi } from '@/api'

const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref(null)
const dateRange = ref([])
const tableData = ref([])
const detailData = reactive({})

const searchForm = reactive({
  keyword: ''
})

const form = reactive({
  id: null,
  title: '',
  pubDate: '',
  tags: '',
  contentText: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  pubDate: [{ required: true, message: '请选择发布日期', trigger: 'change' }],
  contentText: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const dialogTitle = computed(() => form.id ? '编辑' : '新增')

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      ...searchForm
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await newsApi.getPage(params)
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    console.error('加载数据失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  searchForm.keyword = ''
  dateRange.value = []
  handleSearch()
}

const handleAdd = () => {
  Object.assign(form, { id: null, title: '', pubDate: '', tags: '', contentText: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  const patched = { ...row }
  // date-picker value-format 为 YYYY-MM-DD
  if (patched.pubDate && typeof patched.pubDate === 'string' && patched.pubDate.includes('T')) {
    patched.pubDate = patched.pubDate.slice(0, 10)
  }
  Object.assign(form, patched)
  dialogVisible.value = true
}

const handleDetail = async (row) => {
  try {
    const res = await newsApi.getDetail(row.id)
    Object.assign(detailData, res.data)
    detailVisible.value = true
  } catch (error) {
    console.error('获取详情失败', error)
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条记录吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await newsApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败', error)
    }
  }
}

const normalizeSubmitPayload = (raw) => {
  const payload = { ...raw }

  // 后端字段为 LocalDateTime，前端 date-picker 给 YYYY-MM-DD
  if (payload.pubDate && typeof payload.pubDate === 'string' && !payload.pubDate.includes('T')) {
    payload.pubDate = `${payload.pubDate}T00:00:00`
  }

  return payload
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const payload = normalizeSubmitPayload(form)
        if (form.id) {
          await newsApi.update(payload)
          ElMessage.success('修改成功')
        } else {
          await newsApi.add(payload)
          ElMessage.success('新增成功')
        }
        dialogVisible.value = false
        loadData()
      } catch (error) {
        console.error('提交失败', error)
      }
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.news {
  padding: 0;
}

.search-form {
  margin-bottom: 20px;
}

.table-toolbar {
  margin-bottom: 20px;
}

.detail-content h3 {
  margin: 0 0 15px 0;
  color: #303133;
}

.detail-meta {
  color: #909399;
  font-size: 14px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.detail-body {
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}
</style>
