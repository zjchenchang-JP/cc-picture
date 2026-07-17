package com.zjcc.ccpicturebackend.config;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import com.zjcc.ccpicturebackend.manager.websocket.disruptor.PictureEditEvent;
import com.zjcc.ccpicturebackend.manager.websocket.disruptor.PictureEditEventWorkHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 *  Disruptor 配置类，将自定义的事件及处理器关联到 Disruptor 实例中
 *
 * Disruptor 核心概念与工作流程
 *
 * 先了解 Disruptor 的核心概念：
 * RingBuffer（环形缓冲区）：固定大小的循环数组，用于存储数据项，生产者和消费者共享该数据结构。
 * Event（事件）：存储在 RingBuffer 中的数据对象，用于表示要传递的消息或数据。
 * Producer（生产者）：负责向 RingBuffer 写入数据的角色。
 * Consumer（消费者）：从 RingBuffer 中读取并处理数据的角色。
 * Sequencer（序列器）：管理生产者与消费者的索引，确保并发安全的序列管理。
 * SequenceBarrier（序列屏障）：控制消费者等待数据可用的机制，确保数据完整性。
 * WaitStrategy（等待策略）：定义消费者如何等待新的数据（如自旋、自适应等待等）。
 * EventProcessor（事件处理器）：集成了 Consumer 和 SequenceBarrier，用于更高级的消费控制。
 * 而 Disruptor 是封装了 RingBuffer、Producer 和 Consumer 的核心管理类，用于协调所有组件的运行。
 *
 * 举例来说明 Disruptor 的工作流程：
 * 环形队列初始化：创建一个固定大小为 8 的 RingBuffer（索引范围 0-7），每个格子存储一个可复用的事件对象，序号初始为 0。
 * 生产者写入数据：生产者申请索引 0（序号 0），将数据 "A" 写入事件对象，提交后序号递增为 1，下一个写入索引变为 1。
 * 消费者读取数据：消费者检查索引 0（序号 0），读取数据 "A"，处理后提交，序号递增为 1，下一个读取索引变为 1。
 * 环形队列循环使用：当生产者写入到索引 7（序号 7）后，索引回到 0（序号 8），形成循环存储，但序号会持续自增以区分数据的先后顺序。
 * 防止数据覆盖：如果生产者追上消费者，消费者尚未处理完数据，生产者会等待，确保数据不被覆盖
 */
@Configuration
public class PictureEditEventDisruptorConfig {

    @Resource
    private PictureEditEventWorkHandler pictureEditEventWorkHandler;

    @Bean("pictureEditEventDisruptor")
    public Disruptor<PictureEditEvent> messageModelRingBuffer() {
        // ringBuffer 的大小
        int bufferSize = 1024 * 256;
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(
                PictureEditEvent::new,
                bufferSize,
                ThreadFactoryBuilder.create().setNamePrefix("pictureEditEventDisruptor").build()
        );
        // 设置消费者
        disruptor.handleEventsWithWorkerPool(pictureEditEventWorkHandler);
        // 开启 disruptor
        disruptor.start();
        return disruptor;
    }
}
