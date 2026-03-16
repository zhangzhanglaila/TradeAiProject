<template>
  <div class="kg-create">
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
        <el-button type="primary" :disabled="selectedIds.length === 0" @click="handleGenerate">
          生成图谱（已选 {{ selectedIds.length }} 条）
        </el-button>
      </div>

      <el-table
        :data="tableData"
        style="width: 100%"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { newsApi, knowledgeGraphApi } from '@/api'
import { useRouter } from 'vue-router'

const router = useRouter()

const loading = ref(false)
const dateRange = ref([])
const tableData = ref([])
const selectedIds = ref([])

const searchForm = reactive({
  keyword: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

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

    // 翻页后清空选择，避免选择错位
    selectedIds.value = []
  } catch (e) {
    console.error('加载新闻失败', e)
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

const handleSelectionChange = (rows) => {
  selectedIds.value = (rows || []).map(r => r.id).filter(Boolean)
}

const handleGenerate = async () => {
  try {
    const res = await knowledgeGraphApi.generate({ newsIds: selectedIds.value })
    const historyId = res.data?.historyId
    if (!historyId) {
      ElMessage.error('生成失败：未返回 historyId')
      return
    }
    ElMessage.success('生成成功')
    router.push(`/knowledge-graph/view/${historyId}`)
  } catch (e) {
    console.error('生成图谱失败', e)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.kg-create {
  padding: 0;
}

.search-form {
  margin-bottom: 20px;
}

.table-toolbar {
  margin-bottom: 15px;
}
</style>
