package com.project.savingbee.productAlert.channel.sender.sms;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(SmsProperties.class)
@ConditionalOnProperty(prefix = "sms", name = "enabled", havingValue = "true")
public class SmsConfig {
  @Bean
  WebClient sensWebClient(SmsProperties smsProperties) {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 연결 타임아웃
        .responseTimeout(Duration.ofSeconds(5)) // 응답 타임아웃
        .doOnConnected(c ->
            c.addHandlerLast(new ReadTimeoutHandler(5)) // 읽기 타임아웃
                .addHandlerLast(new WriteTimeoutHandler(5))); // 쓰기 타임아웃

    return WebClient.builder()
        .baseUrl(smsProperties.getBaseUrl())
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
        .build();
  }
}


