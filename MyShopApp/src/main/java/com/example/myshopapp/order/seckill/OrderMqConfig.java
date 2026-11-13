package com.example.myshopapp.order.seckill;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 下单排队队列：
 * 正常消息被消费后确认；被拒绝或过期的消息进入死信队列，便于排查与补偿。
 */
@Configuration
@ConditionalOnProperty(name = "myshop.order.async.enabled", havingValue = "true", matchIfMissing = true)
public class OrderMqConfig {

    public static final String CREATE_EXCHANGE = "myshop.order.exchange";
    public static final String CREATE_QUEUE = "myshop.order.create";
    public static final String CREATE_ROUTING_KEY = "order.create";
    public static final String DEAD_LETTER_QUEUE = "myshop.order.create.dlq";

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(CREATE_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCreateQueue() {
        return QueueBuilder.durable(CREATE_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Queue orderCreateDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding orderCreateBinding(@Qualifier("orderCreateQueue") Queue queue,
                                      @Qualifier("orderExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(CREATE_ROUTING_KEY);
    }

    /** 队列里传 JSON，消息体可读，也避免 Java 序列化的版本兼容问题 */
    @Bean
    public MessageConverter orderMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
