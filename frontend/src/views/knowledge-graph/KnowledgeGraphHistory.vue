<template>
  <div class="kg-history">
    <el-card>
      <div class="table-toolbar">
        <el-button type="primary" @click="goCreate">创建/生成</el-button>
      </div>

      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="graphName" label="名称" min-width="200">
          <template #default="{ row }">
            {{ row.graphName || '未命名图谱' }}
          </template>
        </el-table-column>
        <el-table-column label="新闻数" width="90">
          <template #default="{ row }">
            {{ (row.newsIds || []).length }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ (row.createdAt || '').toString().replace('T', ' ').slice(0, 19) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goView(row)">查看</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeGraphApi } from '@/api'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await knowledgeGraphApi.getHistory({
      current: pagination.current,
      size: pagination.size
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (e) {
    console.error('加载历史失败', e)
  } finally {
    loading.value = false
  }
}

const goCreate = () => {
  router.push('/knowledge-graph/create')
}

const goView = (row) => {
  router.push(`/knowledge-graph/view/${row.id}`)
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('是否同时清理 Neo4j 中的图数据？', '删除确认', {
      confirmButtonText: '删除并清理',
      cancelButtonText: '仅删历史',
      distinguishCancelAndClose: true,
      type: 'warning'
    })

    await knowledgeGraphApi.deleteHistory(row.id, { deleteGraph: true })
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // cancel 表示点了“仅删历史”
    if (e === 'cancel') {
      try {
        await knowledgeGraphApi.deleteHistory(row.id, { deleteGraph: false })
        ElMessage.success('删除成功')
        loadData()
      } catch (err) {
        console.error('删除失败', err)
      }
      return
    }

    // close/其他
    if (e !== 'close') {
      console.error('删除失败', e)
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.kg-history {
  padding: 0;
}

.table-toolbar {
  margin-bottom: 15px;
}
</style>
