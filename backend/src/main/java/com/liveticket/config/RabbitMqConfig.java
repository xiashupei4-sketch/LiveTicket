package com.liveticket.config;

import com.liveticket.common.constant.RabbitConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(RabbitConstants.ORDER_EXCHANGE, true, false);
    }

    /**
     * 主队列绑定 DLX，消费端 basicReject(requeue=false) 时消息进入死信队列
     */
    @Bean
    public Queue orderCreateQueue() {
        return QueueBuilder.durable(RabbitConstants.ORDER_CREATE_QUEUE)
                .deadLetterExchange(RabbitConstants.ORDER_DLX_EXCHANGE)
                .deadLetterRoutingKey(RabbitConstants.ORDER_DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding orderCreateBinding() {
        return BindingBuilder.bind(orderCreateQueue()).to(orderExchange())
                .with(RabbitConstants.ORDER_CREATE_ROUTING_KEY);
    }

    @Bean
    public DirectExchange orderDlxExchange() {
        return new DirectExchange(RabbitConstants.ORDER_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderDlxQueue() {
        return QueueBuilder.durable(RabbitConstants.ORDER_DLX_QUEUE).build();
    }

    @Bean
    public Binding orderDlxBinding() {
        return BindingBuilder.bind(orderDlxQueue()).to(orderDlxExchange())
                .with(RabbitConstants.ORDER_DLX_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
