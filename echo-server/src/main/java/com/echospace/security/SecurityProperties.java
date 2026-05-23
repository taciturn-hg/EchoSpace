package com.echospace.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @Author: taciturn-hg
 * @Date: 5/23/2026 2:49 下午
 * @Param:
 * @Return:
 * @Description:
 **/
@ConfigurationProperties(prefix = "security")
@Data
public class SecurityProperties {

    private List<String> whitelist = List.of();
}
