<template>
  <div class="captcha-container">
    <img 
      :src="captchaImage" 
      @click="refresh" 
      alt="验证码" 
      class="captcha-img"
      v-if="captchaImage"
    />
    <div class="captcha-placeholder" v-else @click="refresh">
      点击获取验证码
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  uuid: {
    type: String,
    default: ''
  },
  img: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['refresh'])

const captchaImage = ref('')

watch(() => props.img, (newVal) => {
  if (newVal) {
    // 后端可能返回完整 DataURL（data:image/...;base64,xxx）或纯 base64
    captchaImage.value = String(newVal).startsWith('data:image')
      ? String(newVal)
      : `data:image/png;base64,${newVal}`
  } else {
    captchaImage.value = ''
  }
}, { immediate: true })

const refresh = () => {
  captchaImage.value = ''
  emit('refresh')
}
</script>

<style scoped>
.captcha-container {
  display: flex;
  align-items: center;
}

.captcha-img {
  cursor: pointer;
  height: 40px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}

.captcha-placeholder {
  height: 40px;
  padding: 0 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  color: #909399;
  font-size: 14px;
}
</style>
