# 视频监控开启说明

本文档说明如何启动本地视频监控（现场监控 / AI 检测）所需的流媒体服务。

## 一、整体链路

```
摄像头/测试视频 --ffmpeg 推流--> nginx-rtmp-win32(1935) --切片--> HLS(http://localhost:8068/hls/{推流码}/index.m3u8)
        ^                                                                    |
        |                                                                    +--> 前端 hls.js 播放
        |                                                                    +--> 后端 JavaCV 抽帧 AI 检测
```

- 流服务器：`D:\software\videoUtil\nginx-rtmp-win32-1.2.1`（带 RTMP/HLS 模块）
- 推流工具：`D:\ffmpeg\ffmpeg-2025-09-04-git-2611874a50-essentials_build\bin\ffmpeg.exe`
- 测试视频：`D:\software\videoUtil\nginx-rtmp-win32-1.2.1\video\大屏演示.mp4`

## 二、启动步骤

### 1. 启动 nginx 流服务器

打开 cmd，执行：

```cmd
cd /d D:\software\videoUtil\nginx-rtmp-win32-1.2.1
start nginx.exe
```

验证：浏览器访问 `http://localhost:8068/stat`，能出 RTMP 状态页即成功。
（`/stat` 页面报 `history.css / history.js 404` 属正常，是 nginx 自带测试页资源缺失，与视频流无关。）

### 2. 推流（四路监控）

打开 4 个 cmd 窗口，分别执行（推流码对应数据库 `device_camera.wvp_open_url` 的值）：

```cmd
D:\ffmpeg\ffmpeg-2025-09-04-git-2611874a50-essentials_build\bin\ffmpeg.exe -re -stream_loop -1 -i "D:\software\videoUtil\nginx-rtmp-win32-1.2.1\video\大屏演示.mp4" -c copy -f flv rtmp://localhost:1935/live/test1
```

```cmd
D:\ffmpeg\ffmpeg-2025-09-04-git-2611874a50-essentials_build\bin\ffmpeg.exe -re -stream_loop -1 -i "D:\software\videoUtil\nginx-rtmp-win32-1.2.1\video\大屏演示.mp4" -c copy -f flv rtmp://localhost:1935/live/live01
```

```cmd
D:\ffmpeg\ffmpeg-2025-09-04-git-2611874a50-essentials_build\bin\ffmpeg.exe -re -stream_loop -1 -i "D:\software\videoUtil\nginx-rtmp-win32-1.2.1\video\大屏演示.mp4" -c copy -f flv rtmp://localhost:1935/live/live02
```

```cmd
D:\ffmpeg\ffmpeg-2025-09-04-git-2611874a50-essentials_build\bin\ffmpeg.exe -re -stream_loop -1 -i "D:\software\videoUtil\nginx-rtmp-win32-1.2.1\video\大屏演示.mp4" -c copy -f flv rtmp://localhost:1935/live/live03
```

注意：
- 终端窗口会持续显示推流日志，属正常，不要关闭。
- 若 `-c copy` 报错（视频编码不是 H.264），改用重编码：
  `-c:v libx264 -preset veryfast -tune zerolatency -c:a aac`

### 3. 验证流

浏览器逐个访问，能看到 `#EXTM3U` 开头的内容即通：

```
http://localhost:8068/hls/test1/index.m3u8
http://localhost:8068/hls/live01/index.m3u8
http://localhost:8068/hls/live02/index.m3u8
http://localhost:8068/hls/live03/index.m3u8
```

### 4. 前端/后端联动

- 前端「施工场地 → 现场监控」页面四路画面亮起（流地址直接取数据库 `wvp_open_url`）。
- 后端每 30 秒自动重试失败摄像头（`reloadFailedCameras`），流一通摄像头自动上线并开始 AI 抽帧检测，无需重启后端。
- 播放地址拼装规则见前端 `ConsSafetySystem/src/components/FlvLivePlayer.vue`，后端 `camera.live.baseUrl` 配置见 `smartbuildings-backend/src/main/resources/application.yml`。

### 5. 停止服务

```cmd
cd /d D:\software\videoUtil\nginx-rtmp-win32-1.2.1
nginx.exe -s stop
```

## 三、接入真实监控

普通 IP 摄像头（RTSP）用 ffmpeg 拉流转推进现有 nginx（推流码对应摄像头记录）：

```cmd
D:\ffmpeg\ffmpeg-2025-09-04-git-2611874a50-essentials_build\bin\ffmpeg.exe -rtsp_transport tcp -i "rtsp://用户名:密码@摄像头IP:554/Streaming/Channels/101" -c:v libx264 -preset veryfast -tune zerolatency -c:a aac -f flv rtmp://localhost:1935/live/live03
```

- 海康常用路径：`/Streaming/Channels/101`（主码流）、`/102`（子码流）；大华：`/cam/realmonitor?channel=1&subtype=0`
- 换摄像头时同步更新数据库 `device_camera.wvp_open_url` 与推流码一致。
- GB28181 国标 / 已有现成流等场景见项目文档，架构为「GB设备 → wvp-pro → ZLMediaKit → HLS」。
