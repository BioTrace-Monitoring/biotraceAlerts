package com.biotrace.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class DBConnectionProvider {
    private final DataSource dataSource;

    public DBConnectionProvider() {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(System.getenv("DB_URL"));
        basicDataSource.setUsername(System.getenv("DB_USER"));
        basicDataSource.setPassword(System.getenv("DB_PASS"));

        this.dataSource = basicDataSource;
    }

    public JdbcTemplate getConnection() {
        return new JdbcTemplate(dataSource);
    }
}
