package com.navfirst.postppp.lib.configurations;

import com.navfirst.postppp.lib.services.PostpppLibService;
import com.navfirst.postppp.lib.services.impl.PostpppLibServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(PostpppProperties.class)
@ConditionalOnProperty(prefix = "ppp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PostpppAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PostpppLibService.class)
    public PostpppLibService postpppLibService() {
        return new PostpppLibServiceImpl();
    }

}
