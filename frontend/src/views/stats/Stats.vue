<template>
  <div class="stats">
    <el-card>
      <el-form :inline="true" class="filter-form">
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
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="趋势图" name="trend">
          <div ref="trendChartRef" class="chart"></div>
        </el-tab-pane>
        <el-tab-pane label="贸易方式分布" name="tradeMode">
          <div ref="tradeModeChartRef" class="chart"></div>
        </el-tab-pane>
        <el-tab-pane label="国家分布" name="country">
          <div ref="countryChartRef" class="chart"></div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { statsApi } from '@/api'
import useECharts from '@/composables/useECharts'
import dayjs from 'dayjs'

const activeTab = ref('trend')
const dateRange = ref([])

const trendChartRef = ref(null)
const tradeModeChartRef = ref(null)
const countryChartRef = ref(null)

const trendChart = useECharts(trendChartRef)
const tradeModeChart = useECharts(tradeModeChartRef)
const countryChart = useECharts(countryChartRef)


const handleSearch = () => {
  loadTrendChart()
  loadTradeModeChart()
  loadCountryChart()
}

const handleReset = () => {
  dateRange.value = []
  handleSearch()
}

const loadTrendChart = async () => {
  try {
    let startMonth = dayjs().subtract(11, 'month').format('YYYYMM')
    let endMonth = dayjs().format('YYYYMM')

    if (dateRange.value && dateRange.value.length === 2) {
      startMonth = dateRange.value[0]
      endMonth = dateRange.value[1]
    }

    const res = await statsApi.getTrend({ startMonth, endMonth })

    const option = {
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: res.data?.map(item => item.month) || []
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          formatter: '¥{value}'
        }
      },
      series: [
        {
          name: '贸易金额',
          type: 'line',
          smooth: true,
          data: res.data?.map(item => item.amount || 0) || [],
          areaStyle: { opacity: 0.3 }
        }
      ]
    }
    trendChart.setOption(option)
  } catch (error) {
    console.error('加载趋势图表失败', error)
  }
}

const loadTradeModeChart = async () => {
  try {
    const res = await statsApi.getTradeModeRatio()

    const option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' }
      },
      xAxis: {
        type: 'value',
        axisLabel: { formatter: '{value}' }
      },
      yAxis: {
        type: 'category',
        data: res.data?.map(item => item.name) || []
      },
      series: [
        {
          name: '条数',
          type: 'bar',
          data: res.data?.map(item => item.value) || []
        }
      ]
    }
    tradeModeChart.setOption(option)
  } catch (error) {
    console.error('加载贸易方式图表失败', error)
  }
}

const loadCountryChart = async () => {
  try {
    const res = await statsApi.getCountryRatio()

    const option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: params => {
          const p = Array.isArray(params) ? params[0] : params
          if (!p) return ''
          return `${p.name}: ¥${p.value}`
        }
      },
      xAxis: {
        type: 'value',
        axisLabel: { formatter: '¥{value}' }
      },
      yAxis: {
        type: 'category',
        data: res.data?.map(item => item.name) || []
      },
      series: [
        {
          name: '贸易金额',
          type: 'bar',
          data: res.data?.map(item => item.value) || []
        }
      ]
    }
    countryChart.setOption(option)
  } catch (error) {
    console.error('加载国家分布图表失败', error)
  }
}

onMounted(() => {
  loadTrendChart()
  loadTradeModeChart()
  loadCountryChart()
})
</script>

<style scoped>
.stats {
  padding: 0;
}

.filter-form {
  margin-bottom: 20px;
}

.chart {
  height: 500px;
}
</style>
