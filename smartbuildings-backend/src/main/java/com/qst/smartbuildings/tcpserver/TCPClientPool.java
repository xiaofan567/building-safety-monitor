package com.qst.smartbuildings.tcpserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TCP 客户端连接池
 * 管理所有与设备建立的 TCP 连接（Channel）
 *
 * @author 智能建造团队
 */
@Slf4j
@Component
public class TCPClientPool {

    // 存储客户端连接信息
    private final Map<String, Channel> clientMap = new ConcurrentHashMap<>();
    private final Map<String, ClientInfo> clientInfoMap = new ConcurrentHashMap<>();

    /**
     * 注册客户端连接
     */
    public void register(String clientId, Channel channel) {
        clientMap.put(clientId, channel);
        clientInfoMap.put(clientId, new ClientInfo(clientId, channel, System.currentTimeMillis()));
        log.info("客户端[{}]注册成功，当前连接数：{}", clientId, clientMap.size());
    }

    /**
     * 获取客户端连接
     */
    public Channel getClient(String clientId) {
        return clientMap.get(clientId);
    }

    /**
     * 移除客户端连接
     */
    public void remove(String clientId) {
        Channel channel = clientMap.remove(clientId);
        clientInfoMap.remove(clientId);
        if (channel != null && channel.isActive()) {
            channel.close();
        }
        log.info("客户端[{}]移除成功，当前连接数：{}", clientId, clientMap.size());
    }

    /**
     * 移除所有客户端连接
     */
    public void removeAll() {
        log.info("开始移除所有客户端连接，总数：{}", clientMap.size());
        for (String clientId : new ArrayList<>(clientMap.keySet())) {
            remove(clientId);
        }
        clientMap.clear();
        clientInfoMap.clear();
    }

    /**
     * 获取所有客户端ID
     */
    public Set<String> getAllClientIds() {
        return new HashSet<>(clientMap.keySet());
    }

    /**
     * 获取连接数量
     */
    public int getConnectionCount() {
        return clientMap.size();
    }

    /**
     * 发送消息到指定客户端
     */
    public boolean sendMessage(String clientId, String message) {
        Channel channel = clientMap.get(clientId);
        if (channel != null && channel.isActive()) {
            try {
                // 添加长度前缀：[4字节长度][消息内容]
                byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
                ByteBuf buffer = Unpooled.buffer(4 + messageBytes.length);
                buffer.writeInt(messageBytes.length);
                buffer.writeBytes(messageBytes);

                channel.writeAndFlush(buffer);
                log.debug("消息发送成功到客户端[{}]", clientId);
                return true;
            } catch (Exception e) {
                log.error("发送消息到客户端[{}]失败", clientId, e);
                remove(clientId);
            }
        }
        return false;
    }

    /**
     * 广播消息到所有客户端
     */
    public void broadcast(String message) {
        for (String clientId : getAllClientIds()) {
            sendMessage(clientId, message);
        }
    }

    /**
     * 获取客户端连接信息
     */
    public Map<String, Object> getClientInfo(String clientId) {
        ClientInfo info = clientInfoMap.get(clientId);
        if (info != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("clientId", info.getClientId());
            result.put("connectTime", new Date(info.getConnectTime()));
            result.put("active", info.getChannel() != null && info.getChannel().isActive());
            result.put("remoteAddress", info.getChannel() != null ?
                    info.getChannel().remoteAddress().toString() : "未知");
            return result;
        }
        return Collections.emptyMap();
    }

    /**
     * 获取所有客户端信息
     */
    public Map<String, Map<String, Object>> getAllClientInfos() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (String clientId : clientInfoMap.keySet()) {
            result.put(clientId, getClientInfo(clientId));
        }
        return result;
    }

    /**
     * 客户端信息内部类
     */
    private static class ClientInfo {
        private final String clientId;
        private final Channel channel;
        private final long connectTime;

        public ClientInfo(String clientId, Channel channel, long connectTime) {
            this.clientId = clientId;
            this.channel = channel;
            this.connectTime = connectTime;
        }

        public String getClientId() { return clientId; }
        public Channel getChannel() { return channel; }
        public long getConnectTime() { return connectTime; }
    }
}
