import request from '@/utils/request'

const FIXED_TIME_RULES = [
  { hour: 12, text: '12时' },
  { hour: 15, text: '15时' },
  { hour: 18, text: '18时' },
  { hour: 21, text: '21时' },
  { hour: 0, text: '00时' },
  { hour: 3, text: '03时' },
  { hour: 6, text: '06时' },
  { hour: 9, text: '09时' }
]

const asArray = (value) => (Array.isArray(value) ? value : [])

const getDateParts = (value) => {
  const text = String(value ?? '')
  const matched = text.match(/^(\d{4})-(\d{2})-(\d{2})/)
  if (!matched) return null
  return {
    year: Number(matched[1]),
    month: Number(matched[2]),
    day: Number(matched[3]),
    key: `${matched[1]}-${matched[2]}-${matched[3]}`
  }
}

const getTimestamp = (value) => {
  const text = String(value ?? '').replace(' ', 'T')
  const timestamp = new Date(text).getTime()
  return Number.isNaN(timestamp) ? 0 : timestamp
}

const getHour = (value) => {
  const matched = String(value ?? '').match(/(?:T|\s)(\d{1,2}):/)
  if (matched) return Number(matched[1])
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? -1 : date.getHours()
}

const getFixedHourValue = (list, targetHour) => {
  const matched = asArray(list).find(item => getHour(item.monitorTime) === targetHour)
  return matched?.avgValue ?? 0
}

const getLatestDayEntries = (list) => {
  const dateMap = new Map()

  asArray(list).forEach(item => {
    const date = getDateParts(item.monitorTime)
    if (!date) return

    const timestamp = getTimestamp(item.monitorTime)
    const current = dateMap.get(date.key)
    if (!current || timestamp >= current.timestamp) {
      dateMap.set(date.key, { date, item, timestamp })
    }
  })

  const latest = Array.from(dateMap.values())
    .sort((a, b) => b.timestamp - a.timestamp)
    .slice(0, 7)

  const filledKeys = new Set(latest.map(entry => entry.date.key))
  let fillIndex = 1
  while (latest.length < 7) {
    const base = latest.at(-1)?.date || getDateParts(new Date().toISOString())
    const fillDate = new Date(base.year, base.month - 1, base.day)
    fillDate.setDate(fillDate.getDate() - fillIndex)
    fillIndex += 1

    const year = String(fillDate.getFullYear())
    const month = String(fillDate.getMonth() + 1).padStart(2, '0')
    const day = String(fillDate.getDate()).padStart(2, '0')
    const key = `${year}-${month}-${day}`
    if (filledKeys.has(key)) continue

    filledKeys.add(key)
    latest.push({
      date: { year: Number(year), month: Number(month), day: Number(day), key },
      item: null,
      timestamp: fillDate.getTime()
    })
  }

  return latest.sort((a, b) => a.timestamp - b.timestamp)
}

const formatDay = ({ month, day }) => `${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`

export async function selectEnvData() {
  const response = await request({
    url: '/environmentValue/getEnvironmentInfo',
    method: 'post',
    data: {},
    headers: { 'Content-Type': 'application/json;charset=UTF-8' }
  })
  return response?.data ?? {}
}

export function updateEnvironmentMonitorPoint(data) {
  return request({
    url: '/environmentMonitorPoint/update',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json;charset=UTF-8' }
  })
}

export async function formatNoiseData(noiseList) {
  const noise = FIXED_TIME_RULES.map(rule => getFixedHourValue(noiseList, rule.hour))
  return {
    hours: FIXED_TIME_RULES.map(rule => rule.text),
    noise
  }
}

export async function formatFixedAirQualityData(pm25List, pm10List) {
  return {
    hours: FIXED_TIME_RULES.map(rule => rule.text),
    pm25: FIXED_TIME_RULES.map(rule => getFixedHourValue(pm25List, rule.hour)),
    pm10: FIXED_TIME_RULES.map(rule => getFixedHourValue(pm10List, rule.hour))
  }
}

export async function formatTemperatureData(temperatureList) {
  const entries = getLatestDayEntries(temperatureList)
  return {
    days: entries.map(entry => formatDay(entry.date)),
    maxTemp: entries.map(entry => entry.item?.maxValue ?? 0),
    minTemp: entries.map(entry => entry.item?.minValue ?? 0)
  }
}

export async function formatWeeklyAirQualityData(pm25AvgList) {
  const entries = getLatestDayEntries(pm25AvgList)
  return {
    days: entries.map(entry => formatDay(entry.date)),
    aqi: entries.map(entry => entry.item?.avgValue ?? 0)
  }
}

export async function selectEnvHisData(data) {
  const response = await request({
    url: '/environmentValue/list',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json;charset=UTF-8' }
  })
  return response?.data ?? {}
}

export async function selectSprayData(data) {
  const response = await request({
    url: '/spraySystem/list',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json;charset=UTF-8' }
  })
  return response?.data ?? {}
}

export async function selectSprayTaskData(data) {
  const response = await request({
    url: '/spraySystem/taskList',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json;charset=UTF-8' }
  })
  return response?.data ?? {}
}

export async function saveSprayTask(data) {
  const response = await request({
    url: '/spraySystem/update',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json;charset=UTF-8' }
  })
  return response?.data ?? {}
}

export async function deleteSprayTask(id) {
  const response = await request({
    url: '/spraySystem/delete',
    method: 'post',
    data: { id },
    headers: { 'Content-Type': 'application/json;charset=UTF-8' }
  })
  return response?.data ?? {}
}

export async function selectEnvAlarmRecord(data) {
  const response = await request({
    url: '/monitorPointAlert/selectPage',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json;charset=UTF-8' }
  })
  return response?.data ?? {}
}

export function updHandleData(data) {
  return request({
    url: '/monitorPointAlert/update',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json;charset=UTF-8' }
  })
}
