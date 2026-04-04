package com.zhiyan.redbookbackend.config;

import com.zhiyan.redbookbackend.util.RabbitMqConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class LikeMqConfig {

    private final RedbookProperties redbookProperties;

    @Bean
    public Declarables likeDeclarables() {
        List<Declarable> ds = new ArrayList<>();
        DirectExchange userEx = new DirectExchange(RabbitMqConstants.LIKE_USER_EXCHANGE, true, false);
        DirectExchange noteEx = new DirectExchange(RabbitMqConstants.LIKE_NOTE_EXCHANGE, true, false);
        ds.add(userEx);
        ds.add(noteEx);
        for (int i = 0; i < redbookProperties.getLikeUserShards(); i++) {
            Queue q = new Queue(RabbitMqConstants.LIKE_USER_QUEUE_PREFIX + i, true);
            ds.add(q);
            ds.add(BindingBuilder.bind(q).to(userEx).with(String.valueOf(i)));
        }
        for (int i = 0; i < redbookProperties.getLikeNoteShards(); i++) {
            Queue q = new Queue(RabbitMqConstants.LIKE_NOTE_QUEUE_PREFIX + i, true);
            ds.add(q);
            ds.add(BindingBuilder.bind(q).to(noteEx).with(String.valueOf(i)));
        }
        return new Declarables(ds);
    }

    @Bean
    public String[] likeUserQueueNames() {
        int n = redbookProperties.getLikeUserShards();
        String[] arr = new String[n];
        for (int i = 0; i < n; i++) {
            arr[i] = RabbitMqConstants.LIKE_USER_QUEUE_PREFIX + i;
        }
        return arr;
    }

    @Bean
    public String[] likeNoteQueueNames() {
        int n = redbookProperties.getLikeNoteShards();
        String[] arr = new String[n];
        for (int i = 0; i < n; i++) {
            arr[i] = RabbitMqConstants.LIKE_NOTE_QUEUE_PREFIX + i;
        }
        return arr;
    }
}
