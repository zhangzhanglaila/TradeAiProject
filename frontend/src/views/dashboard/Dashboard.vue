<template>
  <div class="dashboard">
    <el-card class="header-card">
      <div class="header-content">
        <div class="welcome">
          <h2>欢迎回来，{{ username }}！</h2>
          <p>当前时间：{{ currentTime }}</p>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #409EFF;">
              <el-icon :size="30"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalRecords || 0 }}</div>
              <div class="stat-label">总记录数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #67C23A;">
              <el-icon :size="30"><Money /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">¥{{ formatAmount(overview.totalAmount) }}</div>
              <div class="stat-label">总贸易金额</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #E6A23C;">
              <el-icon :size="30"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ tradeModeCount }}</div>
              <div class="stat-label">贸易方式数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #F56C6C;">
              <el-icon :size="30"><Calendar /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ currentMonth }}</div>
              <div class="stat-label">当前月份</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>月度贸易趋势（最近12个月）</span>
            </div>
          </template>
          <div ref="trendChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>贸易方式数量占比</span>
            </div>
          </template>
          <div ref="pieChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>最新数据（最近5条）</span>
        </div>
      </template>
      <el-table :data="latestData" style="width: 100%" v-loading="tableLoading">
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
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { statsApi, tradeApi } from '@/api'
import useECharts from '@/composables/useECharts'
import dayjs from 'dayjs'
import { Document, Money, TrendCharts, Calendar } from '@element-plus/icons-vue'
import { formatAmount } from '@/utils/format'

const userStore = useUserStore()
const username = computed(() => userStore.username)

const currentTime = ref('')
const overview = reactive({
  totalRecords: 0,
  totalAmount: 0,
  tradeModeStats: []
})

const tradeModeCount = computed(() => overview.tradeModeStats?.length || 0)

const latestData = ref([])
const tableLoading = ref(false)

const trendChartRef = ref(null)
const pieChartRef = ref(null)
const trendChart = useECharts(trendChartRef)
const pieChart = useECharts(pieChartRef)

let timeTimer = null

const currentMonth = dayjs().format('YYYY年MM月')

const updateTime = () => {
  currentTime.value = dayjs().format('YYYY年MM月DD日 HH:mm:ss')
}

const loadOverview = async () => {
  try {
    const res = await statsApi.getOverview()
    Object.assign(overview, res.data)
  } catch (error) {
    console.error('加载总览数据失败', error)
  }
}

const loadTrendChart = async () => {
  try {
    const endMonth = dayjs().format('YYYYMM')
    const startMonth = dayjs().subtract(11, 'month').format('YYYYMM')
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
          data: res.data?.map(item => item.amount) || [],
          areaStyle: {
            opacity: 0.3
          }
        }
      ]
    }
    trendChart.setOption(option)
  } catch (error) {
    console.error('加载趋势图表失败', error)
  }
}

const loadPieChart = async () => {
  try {
    const res = await statsApi.getTradeModeRatio()

    const option = {
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left'
      },
      series: [
        {
          name: '贸易方式',
          type: 'pie',
          radius: '50%',
          data: res.data?.map(item => ({
            name: item.name,
            value: item.value
          })) || [],
          emphasis: {
            itemShadowBlur: 10,
            itemShadowOffsetX: 0,
            itemShadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      ]
    }
    pieChart.setOption(option)
  } catch (error) {
    console.error('加载饼图失败', error)
  }
}

const loadLatestData = async () => {
  tableLoading.value = true
  try {
    const res = await tradeApi.getPage({ current: 1, size: 5 })
    latestData.value = res.data?.records || []
  } catch (error) {
    console.error('加载最新数据失败', error)
  } finally {
    tableLoading.value = false
  }
}

onMounted(() => {
  updateTime()
  timeTimer = setInterval(updateTime, 1000)

  loadOverview()
  loadTrendChart()
  loadPieChart()
  loadLatestData()
})

onUnmounted(() => {
  if (timeTimer) clearInterval(timeTimer)
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.header-card {
  margin-bottom: 20px;
}

.welcome h2 {
  margin: 0 0 10px 0;
  color: #303133;
}

.welcome p {
  margin: 0;
  color: #909399;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.charts-row {
  margin-bottom: 20px;
}

.card-header {
  font-weight: bold;
  color: #303133;
}

.chart {
  height: 350px;
}

.table-card {
  margin-bottom: 20px;
}
</style>
