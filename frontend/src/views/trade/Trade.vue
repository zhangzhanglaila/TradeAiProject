<template>
  <div class="trade">
    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="数据查询" name="query">
          <el-form :inline="true" :model="searchForm" class="search-form">
            <el-form-item label="年月范围">
              <el-date-picker
                v-model="dateRange"
                type="monthrange"
                range-separator="至"
                start-placeholder="开始月"
                end-placeholder="结束月"
                value-format="YYYYMM"
              />
            </el-form-item>
            <el-form-item label="关键词">
              <el-input v-model="searchForm.keyword" placeholder="商品/伙伴名称" clearable />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">搜索</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>

          <div class="table-toolbar">
            <el-button type="primary" @click="handleAdd">新增</el-button>
            <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除</el-button>
            <el-button type="success" @click="handleExport">导出</el-button>
          </div>

          <el-table :data="tableData" style="width: 100%" v-loading="loading" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" />
            <el-table-column prop="dataYearMonth" label="年月" width="120" />
            <el-table-column prop="partnerName" label="贸易伙伴" width="180" />
            <el-table-column prop="regName" label="注册地" width="160" />
            <el-table-column prop="productName" label="商品名称" />
            <el-table-column prop="tradeModeName" label="贸易方式" width="140" />
            <el-table-column prop="amountRmb" label="人民币金额" width="160">
              <template #default="{ row }">
                ¥{{ formatAmount(row.amountRmb) }}
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
        </el-tab-pane>

        <el-tab-pane label="数据导入" name="import">
          <el-form :model="importForm" label-width="120px" style="max-width: 600px;">
            <el-form-item label="选择国家">
              <el-select v-model="importForm.country" placeholder="请选择国家">
                <el-option label="哈萨克斯坦" value="哈萨克斯坦" />
              </el-select>
            </el-form-item>
            <el-form-item label="进出口方向">
              <el-select v-model="importForm.tradeDirection" placeholder="请选择">
                <el-option label="进口" value="进口" />
                <el-option label="出口" value="出口" />
              </el-select>
            </el-form-item>
            <el-form-item label="合并模式">
              <el-radio-group v-model="importForm.mergeMode">
                <el-radio label="skip">跳过重复</el-radio>
                <el-radio label="overwrite">覆盖重复</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="最大导入行数">
              <el-input-number v-model="importForm.maxRows" :min="1" :max="10000" />
            </el-form-item>
            <el-form-item label="选择文件">
              <UploadCsv @upload="handleFileUpload" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="importLoading" @click="handleImport">开始导入</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="年月" prop="dataYearMonth">
          <el-date-picker v-model="form.dataYearMonth" type="month" placeholder="选择年月" value-format="YYYYMM" />
        </el-form-item>
        <el-form-item label="贸易伙伴" prop="partnerName">
          <el-input v-model="form.partnerName" placeholder="请输入贸易伙伴" />
        </el-form-item>
        <el-form-item label="注册地" prop="regName">
          <el-input v-model="form.regName" placeholder="请输入注册地" />
        </el-form-item>
        <el-form-item label="商品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="贸易方式" prop="tradeModeName">
          <el-input v-model="form.tradeModeName" placeholder="请输入贸易方式" />
        </el-form-item>
        <el-form-item label="人民币金额" prop="amountRmb">
          <el-input-number v-model="form.amountRmb" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { tradeApi } from '@/api'
import UploadCsv from '@/components/UploadCsv.vue'
import { formatAmount } from '@/utils/format'
import { downloadFile } from '@/utils/download'

const activeTab = ref('query')
const loading = ref(false)
const importLoading = ref(false)
const dialogVisible = ref(false)
const formRef = ref(null)
const dateRange = ref([])
const selectedIds = ref([])
const tableData = ref([])

const searchForm = reactive({
  keyword: ''
})

const importForm = reactive({
  country: '',
  tradeDirection: '',
  mergeMode: 'skip',
  maxRows: 200,
  file: null
})

const form = reactive({
  id: null,
  dataYearMonth: '',
  partnerName: '',
  regName: '',
  productName: '',
  tradeModeName: '',
  amountRmb: 0,
  remark: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const rules = {
  dataYearMonth: [{ required: true, message: '请选择年月', trigger: 'change' }],
  partnerName: [{ required: true, message: '请输入贸易伙伴', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }]
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
      params.startMonth = dateRange.value[0]
      params.endMonth = dateRange.value[1]
    }
    const res = await tradeApi.getPage(params)
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
  Object.assign(searchForm, { keyword: '' })
  dateRange.value = []
  handleSearch()
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

const handleAdd = () => {
  Object.assign(form, {
    id: null,
    dataYearMonth: '',
    partnerName: '',
    regName: '',
    productName: '',
    tradeModeName: '',
    amountRmb: 0,
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const handleDetail = (row) => {
  ElMessageBox.alert(JSON.stringify(row, null, 2), '详情', {
    confirmButtonText: '确定'
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条记录吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await tradeApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败', error)
    }
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条记录吗?`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await tradeApi.batchDelete(selectedIds.value)
    ElMessage.success('批量删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败', error)
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (form.id) {
          await tradeApi.update(form)
          ElMessage.success('修改成功')
        } else {
          await tradeApi.add(form)
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

const handleFileUpload = (file) => {
  importForm.file = file
}

const handleImport = async () => {
  if (!importForm.file) {
    ElMessage.warning('请选择文件')
    return
  }
  importLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', importForm.file)
    formData.append('country', importForm.country)
    formData.append('tradeDirection', importForm.tradeDirection)
    formData.append('mergeMode', importForm.mergeMode)
    formData.append('maxRows', importForm.maxRows)
    await tradeApi.upload(formData)
    ElMessage.success('导入成功')
    activeTab.value = 'query'
    loadData()
  } catch (error) {
    console.error('导入失败', error)
  } finally {
    importLoading.value = false
  }
}

const handleExport = async () => {
  try {
    const params = {
      keyword: searchForm.keyword
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startMonth = dateRange.value[0]
      params.endMonth = dateRange.value[1]
    }
    await downloadFile({
      url: '/trade/export',
      params,
      filename: 'trade_data.csv'
    })
  } catch (error) {
    console.error('导出失败', error)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.trade {
  padding: 0;
}

.search-form {
  margin-bottom: 20px;
}

.table-toolbar {
  margin-bottom: 20px;
}
</style>
