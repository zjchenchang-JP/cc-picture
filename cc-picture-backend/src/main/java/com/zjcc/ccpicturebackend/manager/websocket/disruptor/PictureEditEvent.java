package com.zjcc.ccpicturebackend.manager.websocket.disruptor;

import com.zjcc.ccpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.zjcc.ccpicturebackend.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 定义 Disruptor 核心事件
 * 充当上下文容器，所有处理消息所需的数据都封装其中
 */
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long pictureId;

}
