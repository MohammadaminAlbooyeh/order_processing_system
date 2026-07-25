package com.ecommerce.worker.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Builds the JDBC DataSource from DATABASE_URL in the same
 * "postgresql://user:password@host:port/db" shape the Python services'
 * shared/settings.py expects, so docker-compose does not need per-service
 * connection env vars.
 */
@Configuration
public class DataSourceConfig {

    @Value("${DATABASE_URL:postgresql://ecommerce:password@localhost:5432/orders}")
    private String databaseUrl;

    @Bean
    public DataSource dataSource() {
        URI uri = URI.create(databaseUrl);
        String[] userInfo = uri.getUserInfo().split(":", 2);
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(userInfo[0]);
        dataSource.setPassword(userInfo.length > 1 ? userInfo[1] : "");
        return dataSource;
    }
}
