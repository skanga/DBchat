package com.skanga.demo;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseLoaderTest {

    @Test
    void setupDatabaseLoadsH2SchemaDataAndIndexes() throws Exception {
        String dbName = "sqltutorial_loader_" + System.nanoTime();
        String jdbcUrl = "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1";

        DatabaseLoader.setupDatabase(jdbcUrl, "sa", "", "sqltutorial.org");

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            assertThat(countRows(connection, "regions")).isEqualTo(4);
            assertThat(countRows(connection, "employees")).isGreaterThan(0);
            assertThat(indexExists(connection, "COUNTRIES", "IDX_COUNTRIES_REGION_ID")).isTrue();
            assertThat(indexExists(connection, "EMPLOYEES", "IDX_EMPLOYEES_JOB_ID")).isTrue();
        }
    }

    @Test
    void sqltutorialFixturesIncludeH2AndDerbyScripts() {
        assertThat(getFixture("sqltutorial.org/h2.sql")).isNotNull();
        assertThat(getFixture("sqltutorial.org/h2-data.sql")).isNotNull();
        assertThat(getFixture("sqltutorial.org/derby.sql")).isNotNull();
        assertThat(getFixture("sqltutorial.org/derby-data.sql")).isNotNull();
    }

    private int countRows(Connection connection, String tableName) throws Exception {
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getInt(1);
        }
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getIndexInfo(null, null, tableName, false, false)) {
            while (resultSet.next()) {
                String currentIndexName = resultSet.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(currentIndexName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private URL getFixture(String fixturePath) {
        return DatabaseLoader.class.getClassLoader().getResource(fixturePath);
    }
}
