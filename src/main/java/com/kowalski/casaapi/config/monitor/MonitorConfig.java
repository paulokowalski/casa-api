package com.kowalski.casaapi.config.monitor;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(MonitorProperties.class)
public class MonitorConfig {

    public static final String MONITOR = "monitor";

    @Bean(defaultCandidate = false)
    @Qualifier(MONITOR)
    public DataSource monitorDataSource(MonitorProperties props) {
        HikariDataSource ds = new HikariDataSource();
        ds.setPoolName("monitor");
        ds.setDriverClassName("org.mariadb.jdbc.Driver");
        ds.setJdbcUrl(props.datasource().url());
        ds.setUsername(props.datasource().username());
        ds.setPassword(props.datasource().password());
        ds.setMaximumPoolSize(3);
        ds.setMinimumIdle(0);
        ds.setConnectionTimeout(5_000);
        ds.setIdleTimeout(120_000);
        return ds;
    }
}
