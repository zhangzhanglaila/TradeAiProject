import * as echarts from 'echarts'
import { onUnmounted } from 'vue'

export default function useECharts(elRef) {
  let chart = null
  let resizeTimer = null

  const handleResize = () => {
    if (!chart) return
    if (resizeTimer) clearTimeout(resizeTimer)
    resizeTimer = setTimeout(() => {
      chart?.resize()
    }, 150)
  }

  const init = () => {
    if (chart) return chart

    const el = elRef?.value || elRef
    if (!el) return null

    chart = echarts.init(el)
    window.addEventListener('resize', handleResize)
    return chart
  }

  const setOption = (option, opts) => {
    init()
    chart?.setOption(option, opts)

    // 首次渲染后立即 resize 一次，避免容器初始尺寸未稳定导致的空白/错位
    if (chart) {
      setTimeout(() => {
        chart?.resize()
      }, 0)
    }
  }

  const resize = () => {
    handleResize()
  }

  const dispose = () => {
    if (resizeTimer) {
      clearTimeout(resizeTimer)
      resizeTimer = null
    }
    window.removeEventListener('resize', handleResize)
    chart?.dispose()
    chart = null
  }

  onUnmounted(dispose)

  return {
    init,
    setOption,
    resize,
    dispose,
    getInstance: () => chart
  }
}
