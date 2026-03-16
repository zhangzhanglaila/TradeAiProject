<template>
  <div class="upload-csv">
    <el-upload
      drag
      accept=".csv"
      :auto-upload="false"
      :on-change="handleChange"
      :show-file-list="false"
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">
        将文件拖到此处，或<em>点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          只支持 CSV 格式文件
        </div>
      </template>
    </el-upload>

    <el-progress v-if="showProgress" :percentage="progress" style="margin-top: 20px;" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['upload'])

const showProgress = ref(false)
const progress = ref(0)
const currentFile = ref(null)

const handleChange = (file) => {
  const isCSV = file.name.endsWith('.csv')
  if (!isCSV) {
    ElMessage.error('只能上传 CSV 文件!')
    return
  }
  currentFile.value = file.raw
  emit('upload', file.raw)
}

const setProgress = (val) => {
  progress.value = val
  showProgress.value = val > 0 && val < 100
}

defineExpose({
  setProgress,
  currentFile
})
</script>

<style scoped>
.upload-csv {
  width: 100%;
}
</style>
