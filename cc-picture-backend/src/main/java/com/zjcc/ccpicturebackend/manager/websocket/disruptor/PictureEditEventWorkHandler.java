package com.zjcc.ccpicturebackend.manager.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.lmax.disruptor.WorkHandler;
import com.zjcc.ccpicturebackend.manager.websocket.PictureEditHandler;
import com.zjcc.ccpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.zjcc.ccpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.zjcc.ccpicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * 定义事件处理器（消费者)
 */
@Slf4j
@Component
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    private PictureEditHandler pictureEditHandler;
    @Resource
    private UserService userService;


    @Override
    public void onEvent(PictureEditEvent event) throws Exception {
        PictureEditRequestMessage editRequestMessage = event.getPictureEditRequestMessage();
        WebSocketSession session = event.getSession();
        User user = event.getUser();
        Long pictureId = event.getPictureId();
        // 获取到消息类别
        String type = editRequestMessage.getType();
        PictureEditMessageTypeEnum typeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        // 调用对应的消息处理方法
        switch (typeEnum) {
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(editRequestMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(editRequestMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(editRequestMessage, session, user, pictureId);
                break;
            default:
                PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
                responseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                responseMessage.setMessage("消息类型错误");
                responseMessage.setUser(userService.getUserVO(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(responseMessage)));
        }
    }



}