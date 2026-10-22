package com.fafa.biz.media.infrastructure;

import com.fafa.biz.media.application.*;
import com.rabbitmq.client.Channel;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.Scheduled;

/** 发布确认后标记 Outbox；消费收据、作业租约与对账扫描共同覆盖重复与宕机窗口。 */
@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(name="fafa.media.messaging-enabled",havingValue="true")
public class MediaRabbitTransport {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(MediaRabbitTransport.class);
    private final MediaJobStore events;private final MediaJobService jobs;private final MediaWorker worker;private final Clock clock;
    private final RabbitTemplate template;private final String exchange,queue;
    public MediaRabbitTransport(MediaJobStore events,MediaJobService jobs,MediaWorker worker,Clock clock,
            ConnectionFactory factory,@Value("${fafa.media.exchange:fafa.media}") String exchange,
            @Value("${fafa.media.queue:fafa.media.process}") String queue) {
        this.events=events;this.jobs=jobs;this.worker=worker;this.clock=clock;this.exchange=exchange;this.queue=queue;
        if(!(factory instanceof CachingConnectionFactory caching)) throw new IllegalArgumentException("媒体消息需要缓存连接工厂");
        caching.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);caching.setPublisherReturns(true);
        template=new RabbitTemplate(factory);template.setMandatory(true);
    }
    @Bean
    public Declarables mediaTopology() {
        var main=QueueBuilder.durable(queue).deadLetterExchange(exchange+".dead").deadLetterRoutingKey(queue+".dead").build();
        var dead=QueueBuilder.durable(queue+".dead").build();
        var direct=new DirectExchange(exchange,true,false);var dlx=new DirectExchange(exchange+".dead",true,false);
        return new Declarables(main,dead,direct,dlx,BindingBuilder.bind(main).to(direct).with(queue),
            BindingBuilder.bind(dead).to(dlx).with(queue+".dead"));
    }
    @Scheduled(initialDelayString="${fafa.media.dispatch-delay-ms:2000}",fixedDelayString="${fafa.media.dispatch-delay-ms:2000}")
    public void dispatch() {
        try {
            for(var event:events.pending(clock.instant())) {
                try {
                    var properties=new MessageProperties();properties.setMessageId(event.id());properties.setContentType("text/plain");
                    properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    var correlation=new CorrelationData(event.id());
                    template.send(exchange,queue,new Message(event.jobId().getBytes(StandardCharsets.UTF_8),properties),correlation);
                    var confirm=correlation.getFuture().get(5,TimeUnit.SECONDS);
                    if(confirm.isAck()&&correlation.getReturned()==null) events.sent(event.id(),clock.instant());
                } catch(Exception e) { LOG.warn("媒体事件投递失败 eventId={} errorType={}",event.id(),e.getClass().getSimpleName()); }
            }
            for(String id:events.expired(clock.instant())) jobs.recover(id);
        } catch(RuntimeException e) { LOG.warn("媒体消息扫描失败 errorType={}",e.getClass().getSimpleName()); }
    }
    @RabbitListener(queues="${fafa.media.queue:fafa.media.process}",ackMode="MANUAL",concurrency="1")
    public void consume(Message message,Channel channel) throws java.io.IOException {
        long tag=message.getMessageProperties().getDeliveryTag();
        try {
            String eventId=UUID.fromString(message.getMessageProperties().getMessageId()).toString();
            String jobId=UUID.fromString(new String(message.getBody(),StandardCharsets.UTF_8)).toString();
            worker.run(eventId,jobId);
            channel.basicAck(tag,false);
        } catch(Exception e) {
            LOG.warn("媒体消息进入死信 errorType={}",e.getClass().getSimpleName());
            channel.basicReject(tag,false);
        }
    }
}
