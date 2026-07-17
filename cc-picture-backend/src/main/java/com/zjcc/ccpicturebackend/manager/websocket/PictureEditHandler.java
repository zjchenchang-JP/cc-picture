package com.zjcc.ccpicturebackend.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.zjcc.ccpicturebackend.manager.websocket.disruptor.PictureEditEventProducer;
import com.zjcc.ccpicturebackend.manager.websocket.model.PictureEditActionEnum;
import com.zjcc.ccpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.zjcc.ccpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.zjcc.ccpicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 定义 WebSocket 处理器类
 * 在连接成功、连接关闭、接收到客户端消息时进行相应的处理。
 * 实现 TextWebSocketHandler 接口，这样就能以字符串的方式发送和接受消息了
 */
@Slf4j
@Component
public class PictureEditHandler extends TextWebSocketHandler {

    /**
     * 可能同时有多个 WebSocket 客户端建立连接和发送消息，集合要使用并发包（JUC）中的 ConcurrentHashMap，保证线程安全
     * final: 保证引用不可变，防止在多线程环境下被意外重新赋值
     */
    // 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户ID
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    // 保存所有连接的会话，key: pictureId, value: 订阅了该图片的所有 WebSocket 会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    @Resource
    private UserService userService;


    /**
     * 编写一个 广播消息 的方法
     * 消息都需要传递给所有协作者，根据 pictureId，将响应消息发送给编辑该图片的所有会话
     *
     * @param pictureId                  图片ID
     * @param pictureEditResponseMessage 图片编辑响应消息
     * @param excludeSession             排除掉向某个会话发送消息 可能会有消息不需要发送给编辑者本人的情况
     */
    private void broadcastToPicture(Long pictureId,
                                    PictureEditResponseMessage pictureEditResponseMessage,
                                    WebSocketSession excludeSession) throws Exception {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            // 创建 ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            // 配置序列化：将 Long 类型转为 String，解决丢失精度问题
            // Long 类型的 id 超过了前端 JS number 的最大值导致前端进度丢失的问题，把返回的数据类型改成 String
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance); // 支持 long 基本类型
            objectMapper.registerModule(module);
            // 序列化为 JSON 字符串
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessionSet) {
                // 排除session不广播消息
                if (excludeSession != null && excludeSession.equals(session)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    // 全部广播
    private void broadcastToPicture(Long pictureId,
                                    PictureEditResponseMessage pictureEditResponseMessage) throws Exception {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }

    /**
     * 连接建立成功后执行: 保存会话到集合中，并且给其他会话发送消息
     *
     * @param session session
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        User user = (User) session.getAttributes().get("user");
        // ConcurrentHashMap.newKeySet(): 创建一个线程安全的 Set
        // 不能使用普通的 HashSet。因为外层 Map 虽然线程安全，但内层的 Set 会被多个线程同时读写（如用户加入/离开房间）
        //  newKeySet() 本质上是一个以 Boolean.TRUE 为值的 ConcurrentHashMap 视图，保证了内层集合本身的线程安全性
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        // 这一步：多线程可以同时执行，拿到同一个 Set 引用
        // Set<WebSocketSession> sessions = pictureSessions.get(pictureId);
        pictureSessions.get(pictureId).add(session); //  这一步：如果 sessions 是普通HashSet，多线程同时 add 就会出问题

        // // 推荐写法：一步完成“不存在则创建”并直接获取引用
        // Set<WebSocketSession> sessions = pictureSessions.computeIfAbsent(
        //     pictureId,
        //     k -> ConcurrentHashMap.newKeySet()
        // );
        // sessions.add(session); // 安全地添加会话

        // 构造响应
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("%s加入编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        // 广播给操作同一张图片的其他用户
        broadcastToPicture(pictureId, pictureEditResponseMessage, session);
    }

    /**
     * 接收客户端消息的方法，根据消息类别执行不同的处理
     * 原始方法 - 废弃
     * @param session session
     * @param message message
     */
    // @Override
    // protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    //     // message.getPayload() 拿到的是前端发过来的一段 JSON 字符串(纯文本),内容长什么样完全由后端规定。
    //     // 后端定义了 PictureEditRequestMessage:
    //     // 前端必须按这个结构发 JSON,比如 {"type":"ENTER_EDIT","editAction":"xxx"}
    //     // 等价于 HTTP 接口的请求 DTO。前端照着发,后端照着接
    //     PictureEditRequestMessage requestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
    //
    //     Map<String, Object> attributes = session.getAttributes();
    //     User user = (User) attributes.get("user");
    //     Long pictureId = (Long) attributes.get("pictureId");
    //
    //     String type = requestMessage.getType();
    //     PictureEditMessageTypeEnum enumByValue = PictureEditMessageTypeEnum.getEnumByValue(type);
    //     String editAction = requestMessage.getEditAction();
    //
    //     // 调用对应的消息处理方法
    //     // TODO 将每个处理器封装为单独的类（设计模式中 - 策略模式），根据消息类别调用不同的处理器类。
    //     switch (enumByValue) {
    //         case ENTER_EDIT:
    //             handleEnterEditMessage(requestMessage,session,user,pictureId);
    //             break;
    //         case EXIT_EDIT:
    //             handleExitEditMessage(requestMessage, session, user, pictureId);
    //             break;
    //         case EDIT_ACTION:
    //             handleEditActionMessage(requestMessage, session, user, pictureId);
    //             break;
    //         default:
    //             PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
    //             pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
    //             pictureEditResponseMessage.setMessage("消息类型错误");
    //             pictureEditResponseMessage.setUser(userService.getUserVO(user));
    //             session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
    //     }
    //
    // }

    @Resource
    private PictureEditEventProducer pictureEditEventProducer;

    /**
     * 接收客户端消息的方法，根据消息类别执行不同的处理
     * 引入 Disruptor 后新方法
     * @param session session
     * @param message message
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 将消息解析为 PictureEditMessage
        PictureEditRequestMessage requestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        // 从 Session 属性中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        // 生产消息
        // TODO 1、为防止消息丢失 + 2.分布式 WebSocket
        // 1、为防止消息丢失，可以使用 Redis 等高性能存储保存执行的操作记录。
        // 目前如果图片已经被编辑了，新用户加入编辑时没办法查看到已编辑的状态，可以利用 Redis 保存操作记录来解决，新用户加入编辑时读取 Redis 的操作记录即可。
        // 
        // 2、支持分布式 WebSocket。实现思路很简单，只需要保证要编辑同一图片的用户连接的是相同的服务器即可，和游戏分服务器大区、聊天室分房间是类似的原理。
        pictureEditEventProducer.publishEvent(requestMessage, session, user, pictureId);
    }


    // 用户进入编辑状态
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage,
                                       WebSocketSession session, User user, Long pictureId) throws Exception {
        // 没有用户正在编辑该图片，才能进入编辑
        if (!pictureEditingUsers.containsKey(pictureId)) {
            // 设置当前用户为编辑用户
            pictureEditingUsers.put(pictureId, user.getId());
            // 构造响应
            PictureEditResponseMessage editResponseMessage = new PictureEditResponseMessage();
            editResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("%s开始编辑图片", user.getUserName());
            editResponseMessage.setMessage(message);
            editResponseMessage.setUser(userService.getUserVO(user));
            broadcastToPicture(pictureId, editResponseMessage);
        }
    }

    // 用户执行编辑操作
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage,
                                        WebSocketSession session, User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            return;
        }
        // 确认是当前编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("%s执行%s", user.getUserName(), actionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            // 广播给除了当前用户之外的其他用户，否则会造成重复编辑
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }

    // 用户退出编辑操作
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage,
                                      WebSocketSession session, User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 移除当前用户的编辑状态
            pictureEditingUsers.remove(pictureId);
            // 构造响应，发送退出编辑的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("%s退出编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * WebSocket 连接关闭时，需要移除当前用户的编辑状态、并且从集合中删除当前会话
     *
     * @param session session
     * @param status  status
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long pictureId = (Long) attributes.get("pictureId");
        User user = (User) attributes.get("user");
        // 退出当前用户编辑状态
        handleExitEditMessage(null, session, user, pictureId);
        // 移除会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }

        // 给其他用户发消息
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("%s离开编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        broadcastToPicture(pictureId, pictureEditResponseMessage);

    }


}
