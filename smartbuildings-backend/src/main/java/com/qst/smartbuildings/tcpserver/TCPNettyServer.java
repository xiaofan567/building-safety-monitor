package com.qst.smartbuildings.tcpserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qst.smartbuildings.entity.data.DeviceReportDTO;
//import com.qst.smartbuildings.tcpserver.handle.EnvironmentDataProcessHandle;
import com.qst.smartbuildings.tcpserver.handle.EquipmentAssetsDataProcessHandle;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Netty TCP 服务器
 *
 * <p>接收设备通过 TCP 长连接上报的 JSON 数据，完成数据解析、存储与设备上下线处理。
 * 帧协议：[4字节长度][JSON数据]，通过 LengthFieldBasedFrameDecoder 解决粘包/拆包问题。</p>
 *
 * @author 智能建造团队
 */
@Slf4j
@Component
public class TCPNettyServer {

    // 配置参数
    private final int tcpPort;
    private final TCPClientPool tcpClientPool;
    private final ObjectMapper objectMapper;
    private final EquipmentAssetsDataProcessHandle equipmentAssetsDataProcessHandle;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Thread nettyServerThread;

    // 定义最大帧长度为1MB
    private static final int MAX_FRAME_LENGTH = 1024 * 1024;
    private static final int LENGTH_FIELD_OFFSET = 0;
    private static final int LENGTH_FIELD_LENGTH = 4;
    private static final int LENGTH_ADJUSTMENT = 0;
    private static final int INITIAL_BYTES_TO_STRIP = 4;

//    @Autowired
//    private EnvironmentDataProcessHandle environmentDataProcessHandle;
//    /**
//     * 服务器启动方法，使用@PostConstruct注解确保在当前Bean初始化完成后自动执行
//     */
//    @PostConstruct
//    public void startServer() {
//        handleList.add(towerDataProcessHandle);
//        handleList.add(elevatorInfoDataProcessHandle);
//        handleList.add(environmentDataProcessHandle);
//    }

    public TCPNettyServer(@Value("${device.tcp.port:8888}") int tcpPort,
                          TCPClientPool tcpClientPool,
                          ObjectMapper objectMapper,
                          EquipmentAssetsDataProcessHandle equipmentAssetsDataProcessHandle) {
        this.tcpPort = tcpPort;
        this.tcpClientPool = tcpClientPool;
        this.objectMapper = objectMapper;
        this.equipmentAssetsDataProcessHandle = equipmentAssetsDataProcessHandle;
    }

    /**
     * 服务器启动方法，使用@PostConstruct注解确保在当前Bean初始化完成后自动执行
     */
    @PostConstruct
    public void startServer() {
        // 创建独立线程用于启动Netty服务器，避免阻塞Spring容器初始化（因Netty启动含阻塞操作）
        nettyServerThread = new Thread(() -> {
            try {
                // 初始化boss线程组：负责接收客户端连接，此处指定1个线程（通常无需多个）
                bossGroup = new NioEventLoopGroup(1);
                // 初始化worker线程组：负责处理已连接客户端的IO操作，默认线程数为CPU核心数×2
                workerGroup = new NioEventLoopGroup();

                // 构建Netty服务端启动器
                ServerBootstrap bootstrap = new ServerBootstrap()
                        // 绑定boss和worker线程组
                        .group(bossGroup, workerGroup)
                        // 指定服务端通道类型为NIO（非阻塞IO，适合高并发场景）
                        .channel(NioServerSocketChannel.class)
                        // 设置TCP参数：SO_BACKLOG表示未完成三次握手的连接队列大小
                        .option(ChannelOption.SO_BACKLOG, 100)
                        // 设置客户端连接参数：SO_KEEPALIVE启用TCP心跳机制，检测死连接
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        // 配置新连接的处理器初始化器（每个客户端连接建立时都会执行initChannel）
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                // 获取当前通道的处理器流水线（责任链模式，按添加顺序执行）
                                ChannelPipeline pipeline = ch.pipeline();

                                // 添加长度字段解码器：解决TCP粘包/拆包问题
                                // 帧格式约定为 [4字节长度][JSON数据]
                                pipeline.addLast(new LengthFieldBasedFrameDecoder(
                                        MAX_FRAME_LENGTH,       // 最大帧长度（防止超大报文攻击）
                                        LENGTH_FIELD_OFFSET,    // 长度字段在帧中的起始偏移量
                                        LENGTH_FIELD_LENGTH,    // 长度字段占用的字节数（此处为4字节int）
                                        LENGTH_ADJUSTMENT,      // 长度调整值（通常为0，若长度包含自身则需修正）
                                        INITIAL_BYTES_TO_STRIP  // 解码后跳过的字节数（跳过长度字段，只保留数据）
                                ));

                                // 添加字符串解码器：将字节流转换为UTF-8编码的字符串
                                pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
                                // 添加字符串编码器：将字符串转换为UTF-8编码的字节流
                                pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));

                                // 添加自定义业务处理器，传入依赖的连接池、JSON工具和业务服务
                                pipeline.addLast(new TcpServerHandler(
                                        tcpClientPool,
                                        objectMapper,
                                        equipmentAssetsDataProcessHandle
                                ));
                            }
                        });

                // 绑定监听端口并同步等待绑定完成（阻塞操作，直到端口绑定成功）
                ChannelFuture future = bootstrap.bind(tcpPort).sync();
                log.info("TCP Netty Server 启动成功，监听端口：{}", tcpPort);
                // 阻塞等待服务器通道关闭（即保持服务器运行，直到主动关闭）
                future.channel().closeFuture().sync();

            } catch (InterruptedException e) {
                log.error("TCP Netty Server 启动异常或被中断", e);
                // 恢复线程中断状态，便于上层逻辑感知中断事件
                Thread.currentThread().interrupt();
            } finally {
                // 优雅关闭线程组，释放资源（无论启动成功与否均执行）
                if (bossGroup != null) bossGroup.shutdownGracefully();
                if (workerGroup != null) workerGroup.shutdownGracefully();
                // 服务器关闭时清理所有客户端连接，避免资源泄漏
                tcpClientPool.removeAll();
            }
        }, "Netty-TCP-Server-Thread"); // 线程命名，便于日志和监控识别

        // 启动Netty服务器线程
        nettyServerThread.start();
    }

    /**
     * 停止Netty服务器
     */
    @PreDestroy
    public void stopServer() {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        // 清理所有连接
        tcpClientPool.removeAll();
        log.info("TCP Netty Server 已停止");
    }

    /**
     * 自定义ChannelHandler：处理Netty的IO事件，使用TCPClientPool管理连接
     */
    public class TcpServerHandler extends SimpleChannelInboundHandler<String> {
        private final TCPClientPool tcpClientPool;
        private final ObjectMapper objectMapper;
        private final EquipmentAssetsDataProcessHandle equipmentAssetsDataProcessHandle;

        // 构造函数注入所有依赖
        public TcpServerHandler(TCPClientPool tcpClientPool,
                                ObjectMapper objectMapper,
                                EquipmentAssetsDataProcessHandle equipmentAssetsDataProcessHandle) {
            this.tcpClientPool = tcpClientPool;
            this.objectMapper = objectMapper;
            this.equipmentAssetsDataProcessHandle = equipmentAssetsDataProcessHandle;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            String clientId = getClientId(channel);
            // 使用TCPClientPool注册客户端
            tcpClientPool.register(clientId, channel);
            log.info("新客户端连接：{}，当前连接数：{}", clientId, tcpClientPool.getConnectionCount());
            equipmentAssetsDataProcessHandle.deviceConnect(clientId);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String receivedData) throws Exception {
            Channel channel = ctx.channel();
            String clientId = getClientId(channel);

            try {
                // 使用Jackson解析JSON数据
                DeviceReportDTO reportDTO = objectMapper.readValue(receivedData, DeviceReportDTO.class);
                // 数据校验
                validateReportData(reportDTO, clientId);
                // 处理设备数据
                equipmentAssetsDataProcessHandle.processEquipmentAssetsData(reportDTO, clientId);
                // 构建响应
                String response = String.format("{\"status\":\"success\", \"message\":\"数据已接收\", \"timestamp\":%d}",
                        System.currentTimeMillis());
                sendResponse(channel, response);
            } catch (JsonProcessingException e) {
                log.error("客户端[{}]数据解析失败：{}", clientId, receivedData, e);
                sendResponse(channel, "{\"status\":\"error\", \"message\":\"数据格式错误\"}");
            } catch (IllegalArgumentException e) {
                log.error("客户端[{}]数据验证失败：{}", clientId, e.getMessage());
                sendResponse(channel, String.format("{\"status\":\"error\", \"message\":\"%s\"}", e.getMessage()));
            } catch (Exception e) {
                log.error("客户端[{}]数据处理异常", clientId, e);
                sendResponse(channel, "{\"status\":\"error\", \"message\":\"服务器处理失败\"}");
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            String clientId = getClientId(channel);
            // 使用TCPClientPool移除客户端
            tcpClientPool.remove(clientId);
            log.info("客户端[{}]连接断开，当前连接数：{}", clientId, tcpClientPool.getConnectionCount());
            equipmentAssetsDataProcessHandle.deviceOffline(clientId);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            Channel channel = ctx.channel();
            String clientId = getClientId(channel);
            log.error("客户端[{}]连接异常", clientId, cause);
            // 移除异常客户端
            tcpClientPool.remove(clientId);
            equipmentAssetsDataProcessHandle.deviceOffline(clientId);
            ctx.close();
        }

        /**
         * 发送响应给客户端，添加长度前缀
         */
        private void sendResponse(Channel channel, String response) {
            try {
                byte[] responseBytes = response.getBytes(CharsetUtil.UTF_8);
                ByteBuf buffer = Unpooled.buffer(4 + responseBytes.length);
                buffer.writeInt(responseBytes.length);
                buffer.writeBytes(responseBytes);
                channel.writeAndFlush(buffer);
            } catch (Exception e) {
                log.error("发送响应失败", e);
            }
        }

        /**
         * 验证上报数据的合法性
         */
        private void validateReportData(DeviceReportDTO reportDTO, String clientId) {
            if (reportDTO.getId() == null || reportDTO.getId().trim().isEmpty()) {
                throw new IllegalArgumentException("设备ID不能为空");
            }
            if (reportDTO.getProperties() == null || reportDTO.getProperties().isEmpty()) {
                throw new IllegalArgumentException("设备属性不能为空");
            }
        }

        private String getClientId(Channel channel) {
            return channel.remoteAddress().toString().replace("/", "");
        }
    }
}
