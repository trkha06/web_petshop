package com.utepetshop.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("mysql")
public class OrderStatusSchemaMigration {
  @Bean
  ApplicationRunner orderStatusColumnMigration(JdbcTemplate jdbcTemplate) {
    return arguments -> {
      migrateEnumColumn(jdbcTemplate, "orders", "status", "VARCHAR(32) NULL");
      migrateEnumColumn(jdbcTemplate, "inventory_transactions", "type", "VARCHAR(32) NOT NULL");
    };
  }

  private void migrateEnumColumn(
      JdbcTemplate jdbcTemplate, String tableName, String columnName, String targetDefinition) {
    String dataType =
        jdbcTemplate.queryForObject(
            "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
            String.class,
            tableName,
            columnName);
    if ("enum".equalsIgnoreCase(dataType)) {
      jdbcTemplate.execute(
          "ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + targetDefinition);
    }
  }
}
