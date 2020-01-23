package com.optum.ocr.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Configuration
public class DBMasterConfig {

    static DataSource staticDataSource;
    static EntityManagerFactory staticManagerFactory;

    @Autowired
    DataSource dataSource;

    @Autowired
    EntityManagerFactory emf;

    public static Connection getConnection() throws SQLException {
        return staticDataSource.getConnection();
    }

    public static DataSource getDataSource() {
        return staticDataSource;
    }

    public static EntityManagerFactory getEntityManager() {
        return staticManagerFactory;
    }

    @Bean
    public DBMasterConfig getDBConfig() {
        Logger.getGlobal().log(Level.INFO, "\n\n\n\nDATASOURCE======\n\n\n\n" + dataSource);
        staticDataSource = dataSource;
        staticManagerFactory = emf;
        return new DBMasterConfig();
    }
}
