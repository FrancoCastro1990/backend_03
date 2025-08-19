package com.bankxyz.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "bankxyz")
public class AppProperties {
    private String dataDir;

    public String getDataDir() {
        return dataDir;
    }
    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }
}
