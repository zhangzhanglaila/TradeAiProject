<template>
  <div class="ai">
    <el-card class="ai-container">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="history-panel">
            <div class="history-header">
              <h3>历史对话</h3>
              <el-button type="text" @click="handleClearHistory">清空</el-button>
            </div>
            <el-scrollbar style="height: calc(100% - 50px);">
              <div v-for="(item, index) in historyList" :key="index" class="history-item" @click="selectHistory(item)">
                <el-icon><ChatDotRound /></el-icon>
                <span class="history-title">{{ item.title }}</span>
              </div>
            </el-scrollbar>
          </div>
        </el-col>
        <el-col :span="18">
          <div class="chat-panel">
            <div class="chat-messages" ref="messagesRef">
              <div v-for="(msg, index) in messages" :key="index" class="message" :class="msg.role">
                <div class="message-avatar">
                  <el-icon v-if="msg.role === 'user'"><User /></el-icon>
                  <el-icon v-else><Service /></el-icon>
                </div>
                <div class="message-content">
                  <div class="message-bubble">
                    {{ msg.content }}
                  </div>
                  <div class="message-time">{{ msg.time }}</div>
                </div>
              </div>
              <div v-if="loading" class="message ai">
                <div class="message-avatar">
                  <el-icon><Service /></el-icon>
                </div>
                <div class="message-content">
                  <div class="message-bubble">
                    <span class="loading-dots">思考中<span>.</span><span>.</span><span>.</span></span>
                  </div>
                </div>
              </div>
            </div>
            <div class="chat-input">
              <el-input
                v-model="inputMessage"
                type="textarea"
                :rows="3"
                placeholder="请输入您的问题..."
                @keyup.ctrl.enter="handleSend"
              />
              <div class="input-actions">
                <span class="tip">Ctrl + Enter 发送</span>
                <el-button type="primary" :loading="loading" @click="handleSend">
                  <el-icon><Promotion /></el-icon>
                  发送
                </el-button>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aiApi } from '@/api'
import dayjs from 'dayjs'
import { ChatDotRound, User, Service, Promotion } from '@element-plus/icons-vue'

const messagesRef = ref(null)
const loading = ref(false)
const inputMessage = ref('')
const messages = ref([])
const historyList = ref([])

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const handleSend = async () => {
  if (!inputMessage.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }

  const userMessage = {
    role: 'user',
    content: inputMessage.value,
    time: dayjs().format('HH:mm:ss')
  }
  messages.value.push(userMessage)
  const question = inputMessage.value
  inputMessage.value = ''
  scrollToBottom()

  loading.value = true
  try {
    const res = await aiApi.ask({ question })
    const answer = typeof res?.data === 'string' ? res.data : res?.data?.answer
    const aiMessage = {
      role: 'ai',
      content: answer || '抱歉，我暂时无法回答这个问题。',
      time: dayjs().format('HH:mm:ss')
    }
    messages.value.push(aiMessage)
    
    if (historyList.value.length === 0 || historyList.value[0].title !== question.substring(0, 20)) {
      historyList.value.unshift({
        title: question.substring(0, 20) + (question.length > 20 ? '...' : ''),
        messages: [...messages.value]
      })
    }
  } catch (error) {
    console.error('发送失败', error)
    ElMessage.error('发送失败，请稍后重试')
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const selectHistory = (item) => {
  messages.value = [...item.messages]
  scrollToBottom()
}

const handleClearHistory = () => {
  ElMessageBox.confirm('确定要清空历史对话吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    historyList.value = []
    messages.value = []
    ElMessage.success('已清空')
  }).catch(() => {})
}

onMounted(() => {
  messages.value = [
    {
      role: 'ai',
      content: '您好！我是贸易数据智能助手，您可以问我关于贸易数据的问题，比如：\n1. 最近的贸易趋势如何？\n2. 主要贸易方式有哪些？\n3. 哈萨克斯坦的贸易数据统计',
      time: dayjs().format('HH:mm:ss')
    }
  ]
})
</script>

<style scoped>
.ai {
  padding: 0;
  height: 100%;
}

.ai-container {
  height: 100%;
}

.history-panel {
  height: calc(100vh - 140px);
  border-right: 1px solid #eee;
  padding-right: 10px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  height: 40px;
}

.history-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 8px;
  transition: all 0.3s;
}

.history-item:hover {
  background-color: #f5f7fa;
}

.history-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: #606266;
}

.chat-panel {
  height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #409EFF;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.message.ai .message-avatar {
  background: #67C23A;
}

.message-content {
  max-width: 70%;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 8px;
  background: #f5f7fa;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message.user .message-bubble {
  background: #409EFF;
  color: #fff;
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.message.user .message-time {
  text-align: right;
}

.loading-dots {
  display: inline-block;
}

.loading-dots span {
  animation: blink 1.4s infinite both;
}

.loading-dots span:nth-child(2) {
  animation-delay: 0.2s;
}

.loading-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes blink {
  0%, 60%, 100% { opacity: 0; }
  30% { opacity: 1; }
}

.chat-input {
  border-top: 1px solid #eee;
  padding: 20px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.tip {
  font-size: 12px;
  color: #909399;
}
</style>
