<template>
  <div class="kg-view">
    <el-card>
      <div class="toolbar">
        <el-button @click="handleFit">自适应/居中</el-button>
        <el-button @click="handleExport">导出图片</el-button>
        <el-button type="primary" @click="openSaveDialog">保存/命名</el-button>
      </div>

      <el-alert
        v-if="progressText"
        :title="`图谱构建中：${progressText}`"
        type="info"
        show-icon
        :closable="false"
        style="margin-bottom: 12px;"
      />

      <KnowledgeGraphCanvas ref="canvasRef" :nodes="graph.nodes" :edges="graph.edges" />
    </el-card>

    <el-dialog v-model="saveDialogVisible" title="保存/命名" width="420px">
      <el-form :model="saveForm" label-width="90px">
        <el-form-item label="图谱名称">
          <el-input v-model="saveForm.graphName" placeholder="请输入名称" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { knowledgeGraphApi } from '@/api'
import KnowledgeGraphCanvas from './components/KnowledgeGraphCanvas.vue'

const route = useRoute()
const canvasRef = ref(null)

const graph = reactive({
  nodes: [],
  edges: []
})

const saveDialogVisible = ref(false)
const saveForm = reactive({
  graphName: ''
})

const progressText = ref('')

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const loadGraph = async () => {
  const id = route.params.id
  if (!id) return

  // 轮询进度，直到 SUCCEEDED 再拉取图谱数据
  for (;;) {
    const p = await knowledgeGraphApi.getProgress(id)
    const status = p.data?.status
    const progress = p.data?.progress ?? 0
    const msg = p.data?.message || ''

    if (status === 'SUCCEEDED') {
      break
    }

    if (status === 'FAILED') {
      throw new Error(msg || '图谱构建失败')
    }

    // BUILDING
    progressText.value = `${progress}% ${msg}`
    await sleep(1000)
  }

  const res = await knowledgeGraphApi.getGraph(id)
  graph.nodes = res.data?.nodes || []
  graph.edges = res.data?.edges || []
  progressText.value = ''
}

const handleFit = () => {
  canvasRef.value?.fitView?.(20)
}

const handleExport = () => {
  const id = route.params.id
  canvasRef.value?.exportPng?.(`knowledge-graph-${id || ''}`)
}

const openSaveDialog = () => {
  saveDialogVisible.value = true
}

const handleSave = async () => {
  const id = route.params.id
  await knowledgeGraphApi.saveHistory({
    historyId: Number(id),
    graphName: saveForm.graphName
  })
  saveDialogVisible.value = false
  ElMessage.success('保存成功')
}

onMounted(() => {
  loadGraph().catch((e) => console.error('加载图谱失败', e))
})
</script>

<style scoped>
.kg-view {
  padding: 0;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
</style>
