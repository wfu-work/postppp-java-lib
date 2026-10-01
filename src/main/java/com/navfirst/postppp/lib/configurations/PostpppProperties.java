package com.navfirst.postppp.lib.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** SDK 配置。创建默认服务时加载动态库。 */
@Data
@ConfigurationProperties(prefix = "ppp")
public class PostpppProperties {

    /** 是否注册默认 PostpppLibService。 */
    private boolean enabled = true;

}
