<template>
  <div class="kg-canvas" ref="containerRef"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import G6 from '@antv/g6'

const props = defineProps({
  nodes: {
    type: Array,
    default: () => []
  },
  edges: {
    type: Array,
    default: () => []
  }
})

defineExpose({
  fitView: (padding = 20) => {
    if (graph) {
      graph.fitView(padding)
    }
  },
  exportPng: (fileName = 'knowledge-graph') => {
    if (graph) {
      // 导出全图 PNG（包含不在当前可视区域的部分）
      graph.downloadFullImage(fileName, 'image/png', {
        backgroundColor: '#ffffff',
        padding: 20
      })
    }
  }
})

const containerRef = ref(null)
let graph = null

const colorByType = (type) => {
  if (type === 'News') return '#3b82f6'
  if (type === '人物') return '#ef4444'
  if (type === '组织') return '#f59e0b'
  if (type === '地点') return '#10b981'
  if (type === '事件') return '#8b5cf6'
  return '#64748b'
}

const buildData = () => {
  const nodes = (props.nodes || []).map((n) => {
    const type = n.type || 'Entity'
    const label = n.label || n.id
    const fill = colorByType(type)
    return {
      id: n.id,
      label,
      type,
      style: {
        fill,
        stroke: fill,
        lineWidth: 1
      },
      labelCfg: {
        style: {
          fill: '#111827',
          fontSize: 12
        }
      },
      _raw: n
    }
  })

  const edges = (props.edges || []).map((e) => {
    const label = e.label || ''
    return {
      id: e.id,
      source: e.source,
      target: e.target,
      label,
      style: {
        stroke: '#94a3b8',
        endArrow: true
      },
      labelCfg: {
        autoRotate: true,
        style: {
          fill: '#334155',
          fontSize: 11,
          background: {
            fill: '#ffffff',
            padding: [2, 2, 2, 2],
            radius: 2
          }
        }
      },
      _raw: e
    }
  })

  return { nodes, edges }
}

const render = () => {
  if (!containerRef.value) return

  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight

  if (!graph) {
    graph = new G6.Graph({
      container: containerRef.value,
      width,
      height,
      layout: {
        type: 'force',
        preventOverlap: true,
        linkDistance: 120,
        nodeStrength: -300
      },
      defaultNode: {
        type: 'circle',
        size: 32
      },
      defaultEdge: {
        type: 'line'
      },
      modes: {
        default: ['drag-canvas', 'zoom-canvas', 'drag-node']
      },
      plugins: [
        new G6.Tooltip({
          offsetX: 10,
          offsetY: 10,
          itemTypes: ['node', 'edge'],
          getContent: (e) => {
            const div = document.createElement('div')
            div.style.padding = '6px 8px'
            div.style.maxWidth = '320px'
            div.style.whiteSpace = 'pre-wrap'

            const model = e?.item?.getModel?.() || {}
            const raw = model._raw || {}
            div.innerText = JSON.stringify(raw.properties || raw, null, 2)
            return div
          }
        })
      ]
    })

    window.addEventListener('resize', handleResize)
  }

  const data = buildData()
  graph.data(data)
  graph.render()
  graph.fitView(20)
}

const handleResize = () => {
  if (!graph || !containerRef.value) return
  graph.changeSize(containerRef.value.clientWidth, containerRef.value.clientHeight)
}

watch(
  () => [props.nodes, props.edges],
  () => {
    if (graph) {
      const data = buildData()
      graph.changeData(data)
      graph.fitView(20)
    } else {
      render()
    }
  },
  { deep: true }
)

onMounted(() => {
  render()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (graph) {
    graph.destroy()
    graph = null
  }
})
</script>

<style scoped>
.kg-canvas {
  width: 100%;
  height: 600px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}
</style>
