/**
 * TCP 客户端 - 模拟真实设备通过 TCP 长连接向后端 Netty 服务上报数据
 *
 * 帧协议（与后端 TCPNettyServer 的 LengthFieldBasedFrameDecoder 对应）：
 *   [4字节长度(int32BE)][JSON数据(UTF-8)]
 *
 * 集成方式：设备组建立连接时调用 connect()，断开时调用 close()，
 * 设备发送数据时调用 send(payload)。
 */
import net from 'net'

// 后端 Netty TCP 服务地址（与后端 application.yml 中 device.tcp.port 对应）
const TCP_CONFIG = {
  host: '127.0.0.1',
  port: 8888
}

export default class TcpDeviceClient {
  /**
   * @param {string} name 设备组名称（仅用于日志展示）
   */
  constructor(name, { host, port } = TCP_CONFIG) {
    this.name = name
    this.host = host
    this.port = port
    this.socket = null
    this.connected = false
    this.reconnectTimer = null
    this.closed = false
    this.onResponse = null // 可选：接收服务端响应的回调
    this.pendingQueue = [] // 连接建立前暂存的数据帧，连接后自动补发
  }

  /**
   * 建立 TCP 连接（断线后自动重连，直到主动 close）
   */
  connect() {
    this.closed = false
    if (this.connected) return
    const socket = net.createConnection({ host: this.host, port: this.port })
    this.socket = socket

    socket.on('connect', () => {
      this.connected = true
      console.log(`[tcp:${this.name}] 已连接到后端 Netty 服务 ${this.host}:${this.port}`)
      // 补发连接建立前暂存的数据
      if (this.pendingQueue.length > 0) {
        const queued = this.pendingQueue.splice(0)
        console.log(`[tcp:${this.name}] 连接建立，补发 ${queued.length} 帧暂存数据`)
        queued.forEach((payload) => this.sendNow(payload))
      }
    })

    // 处理服务端响应（帧格式：[4字节长度][JSON]）
    socket.on('data', (buf) => {
      try {
        const len = buf.readInt32BE(0)
        const body = buf.subarray(4, 4 + len).toString('utf-8')
        console.log(`[tcp:${this.name}] 服务端响应: ${body}`)
        if (this.onResponse) this.onResponse(body)
      } catch (e) {
        console.log(`[tcp:${this.name}] 收到无法解析的响应: ${buf.toString()}`)
      }
    })

    socket.on('close', () => {
      this.connected = false
      console.log(`[tcp:${this.name}] 连接已关闭`)
      this.scheduleReconnect()
    })

    socket.on('error', (err) => {
      console.error(`[tcp:${this.name}] 连接错误: ${err.message}`)
    })
  }

  /**
   * 断线重连（3 秒后重试，主动 close 后不再重连）
   */
  scheduleReconnect() {
    if (this.closed) return
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
    this.reconnectTimer = setTimeout(() => {
      console.log(`[tcp:${this.name}] 尝试重新连接...`)
      this.connect()
    }, 3000)
  }

  /**
   * 发送一帧数据（[4字节长度][JSON]）
   * @param {Object} payload 设备上报数据 { code, id, properties }
   * @returns {boolean} 是否发送成功
   */
  send(payload) {
    if (!this.connected || !this.socket) {
      // 未连接时先暂存，连接建立后补发（避免启动设备与 TCP 建连的时序竞态丢数据）
      this.pendingQueue.push(payload)
      console.warn(`[tcp:${this.name}] 未连接，数据已暂存等待补发`)
      return false
    }
    return this.sendNow(payload)
  }

  /**
   * 立即发送一帧数据（[4字节长度][JSON]）
   * @param {Object} payload 设备上报数据 { code, id, properties }
   * @returns {boolean} 是否发送成功
   */
  sendNow(payload) {
    if (!this.connected || !this.socket) {
      return false
    }
    const body = Buffer.from(JSON.stringify(payload), 'utf-8')
    const frame = Buffer.alloc(4 + body.length)
    frame.writeInt32BE(body.length, 0)
    body.copy(frame, 4)
    this.socket.write(frame)
    console.log(`[tcp:${this.name}] 已发送数据帧(${body.length}字节) code=${payload.code} id=${payload.id}`)
    return true
  }

  /**
   * 主动断开 TCP 连接（触发后端设备下线处理）
   */
  close() {
    this.closed = true
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.socket) {
      this.socket.end()
      this.socket.destroy()
      this.socket = null
    }
    this.connected = false
    console.log(`[tcp:${this.name}] 已主动断开与后端 Netty 服务的连接`)
  }
}
