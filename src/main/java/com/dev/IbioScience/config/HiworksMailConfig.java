package com.dev.IbioScience.config;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
@EnableConfigurationProperties(HiworksMailProperties.class)
public class HiworksMailConfig {

    @Bean(name = "hiworksEstimateMailSender")
    public JavaMailSender hiworksEstimateMailSender(HiworksMailProperties properties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.getHost());
        sender.setPort(properties.getPort());
        sender.setUsername(properties.getUsername());
        sender.setPassword(properties.getPassword());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties javaMailProps = sender.getJavaMailProperties();
        javaMailProps.put("mail.transport.protocol", "smtp");
        javaMailProps.put("mail.smtp.auth", String.valueOf(properties.isAuth()));
        javaMailProps.put("mail.smtp.ssl.enable", String.valueOf(properties.isSslEnable()));
        javaMailProps.put("mail.smtp.ssl.trust", properties.getHost());
        javaMailProps.put("mail.smtp.connectiontimeout", "10000");
        javaMailProps.put("mail.smtp.timeout", "10000");
        javaMailProps.put("mail.smtp.writetimeout", "10000");
        javaMailProps.put("mail.debug", String.valueOf(properties.isDebug()));

        return sender;
    }
}