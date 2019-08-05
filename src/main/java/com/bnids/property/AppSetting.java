package com.bnids.property;

import com.bnids.core.annotations.Comment;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="env.sever")
@Getter @Setter
public class AppSetting {
    private String localServer;
    private String signageInterfaceServer;
    private String gateControlServer;
    private String pushServer;
    private String homenetInterfaceServer;
}
