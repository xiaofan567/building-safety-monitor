/**
 * 塔吊数据处理冒烟测试脚本
 *
 * 复用设备模拟器的 TcpDeviceClient 与后端 Netty(8888) 通讯，
 * 分阶段发送可精确覆盖「正常/预警/报警」三态的实时数据，最后发一条作业数据。
 *
 * 运行：node test-tower.mjs （需已安装依赖 npm install）
 */
import { fileURLToPath } from 'url'
import path from 'path'
import fs from 'fs'
import TcpDeviceClient from './tcp-client.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const configPath = path.join(__dirname, 'config', 'device-config.json')

// 塔吊 code 与后端 equipment_assets(asset331) 的 platform_eq_code 对应
const CODE = JSON.parse(fs.readFileSync(configPath, 'utf-8')).towerCraneRealTime.code

const client = new TcpDeviceClient('test-tower')

/** 构造一条塔吊实时数据帧（moment/wind 可手动指定以覆盖三态） */
function realtime(moment, wind) {
  const pad = (n) => String(n).padStart(2, '0')
  const now = new Date()
  const monitorTime = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ` +
    `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
  return {
    code: CODE,
    id: 'TC1001',
    properties: {
      monitorTime,                       // 带该字段 -> 实时数据(tower_data)
      moment: String(moment),            // 额定力矩7.0 => >6.3预警 >7.0报警
      windSpeed: String(wind),            // 阈值 10.8预警 17.2报警
      weight: '6.0',
      amplitude: '22.0',
      height: '75.0',
      rotation: '180.0',
      obliquity: '0.2'
    }
  }
}

/** 构造一条塔吊作业数据帧（无 monitorTime -> 作业数据 tower_work） */
function work() {
  return {
    code: CODE,
    id: 'TC1001',
    properties: {
      weight: '3.2', maxRadius: '23.0', maxPercent: '42.0', maxHeight: '28.0',
      maxWindSpeed: '4.5', minRadius: '16.0', maxLoad: '3.2', minHeight: '9.0',
      startTime: '2026-09-09 12:00:00', endTime: '2026-09-09 12:08:00'
    }
  }
}

function wait(ms) {
  return new Promise((r) => setTimeout(r, ms))
}

async function run() {
  client.connect()
  await wait(800)

  console.log('\n===== 阶段1：力矩三态（moment，风速正常3.0）=====')
  client.send(realtime(5.0, 3.0))  // 正常（<6.3）
  await wait(1500)
  client.send(realtime(6.6, 3.0))  // 预警（6.3~7.0）
  await wait(1500)
  client.send(realtime(7.8, 3.0))  // 报警（>7.0)
  await wait(1500)
  client.send(realtime(4.0, 3.0))  // 回落正常 -> 写恢复值

  console.log('\n===== 阶段2：风速三态（风力正常3.0，moment正常2.0）=====')
  client.send(realtime(2.0, 8.0))  // 正常（<10.8）
  await wait(1500)
  client.send(realtime(2.0, 13.0)) // 预警（10.8~17.2）
  await wait(1500)
  client.send(realtime(2.0, 18.0)) // 报警（>17.2）
  await wait(1500)
  client.send(realtime(2.0, 5.0))  // 回落正常 -> 写恢复值

  console.log('\n===== 阶段3：作业数据 =====')
  client.send(work())

  console.log('\n已全部发送，观察后端日志，再通过查询接口复核。')
  await wait(500)
  client.close()
  process.exit(0)
}

run().catch((e) => {
  console.error('测试失败:', e)
  client.close()
  process.exit(1)
})