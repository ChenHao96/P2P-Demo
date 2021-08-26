package top.sclab.java.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.sclab.java.handler.UDPBaseMessageHandler;
import top.sclab.java.service.MessageHandler;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

@Configuration
public class BeansConfig implements InitializingBean {

    @Bean
    @ConditionalOnMissingBean
    public MessageHandler messageHandler() {
        return new UDPBaseMessageHandler() {
            @Override
            protected void processData(InetSocketAddress current, ByteBuffer byteBuffer) {
            }
        };
    }

    @Override
    public void afterPropertiesSet() {

    }
}
