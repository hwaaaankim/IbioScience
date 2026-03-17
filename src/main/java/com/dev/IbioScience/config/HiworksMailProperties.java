package com.dev.IbioScience.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.mail.hiworks")
public class HiworksMailProperties {

    private String host;
    private Integer port = 465;
    private String username;
    private String password;
    private String fromAddress;
    private String fromName = "아이바이오사이언스";
    private boolean auth = true;
    private boolean sslEnable = true;
    private boolean debug = false;

    public String getResolvedFromAddress() {
        return StringUtils.hasText(fromAddress) ? fromAddress : username;
    }
}