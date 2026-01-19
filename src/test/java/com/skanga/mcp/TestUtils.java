package com.skanga.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skanga.mcp.config.ConfigParams;
import com.skanga.mcp.db.DatabaseResource;
import com.skanga.mcp.db.QueryResult;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test utilities and configuration for database MCP server tests
 */
public class TestUtils {
    private static final AtomicInteger dbCounter = new AtomicInteger(0);

    /**
     * Creates a unique H2 in-memory database configuration for testing
     */
    public static ConfigParams createTestH2Config() {
        String dbName = "testdb" + dbCounter.incrementAndGet();
        return ConfigParams.customConfig(
            "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "sa",
            "",
            "org.h2.Driver",
                false, 10000
        );
    }

    /**
     * Creates a test database with sample data
     */
    public static void setupTestDatabase(ConfigParams config) throws SQLException {
        try (Connection conn = DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPass());
             Statement stmt = conn.createStatement()) {
            
            // Create users table
            stmt.execute("""
                CREATE TABLE users (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(255) NOT NULL,
                    email VARCHAR(255) UNIQUE,
                    age INT,
                    created_date DATE DEFAULT CURRENT_DATE,
                    is_active BOOLEAN DEFAULT TRUE
                )
                """);

            // Create orders table
            stmt.execute("""
                CREATE TABLE orders (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    user_id INT,
                    product_name VARCHAR(255),
                    amount DECIMAL(10,2),
                    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """);

            // Create view
            stmt.execute("""
                CREATE VIEW active_users AS
                SELECT id, name, email, age
                FROM users
                WHERE is_active = TRUE
                """);

            // Insert test data
            stmt.execute("""
                INSERT INTO users (name, email, age, is_active) VALUES
                ('John Doe', 'john@example.com', 30, TRUE),
                ('Jane Smith', 'jane@example.com', 25, TRUE),
                ('Bob Johnson', 'bob@example.com', 35, FALSE),
                ('Alice Brown', 'alice@example.com', 28, TRUE)
                """);

            stmt.execute("""
                INSERT INTO orders (user_id, product_name, amount) VALUES
                (1, 'Laptop', 999.99),
                (1, 'Mouse', 29.99),
                (2, 'Keyboard', 79.99),
                (4, 'Monitor', 299.99)
                """);
        }
    }

    /**
     * Creates a minimal database with just one table for simple tests
     */
    public static void setupMinimalDatabase(ConfigParams config) throws SQLException {
        try (Connection conn = DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPass());
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE simple_table (id INT PRIMARY KEY, value VARCHAR(50))");
            stmt.execute("INSERT INTO simple_table VALUES (1, 'test'), (2, 'data')");
        }
    }

    /**
     * Creates a database with various data types for testing
     */
    public static void setupDataTypesDatabase(ConfigParams config) throws SQLException {
        try (Connection conn = DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPass());
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("""
                CREATE TABLE data_types_test (
                    id INT PRIMARY KEY,
                    text_field VARCHAR(255),
                    number_field DECIMAL(10,2),
                    date_field DATE,
                    timestamp_field TIMESTAMP,
                    boolean_field BOOLEAN,
                    null_field VARCHAR(50)
                )
                """);

            stmt.execute("""
                INSERT INTO data_types_test VALUES
                (1, 'Sample Text', 123.45, '2024-01-15', '2024-01-15 10:30:00', TRUE, NULL),
                (2, 'Another Text', 67.89, '2024-02-20', '2024-02-20 14:45:30', FALSE, 'Not Null'),
                (3, NULL, NULL, NULL, NULL, NULL, NULL)
                """);
        }
    }

    /**
     * Checks if a database connection is valid
     */
    public static boolean isConnectionValid(ConfigParams config) {
        try (Connection conn = DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPass())) {
            return conn.isValid(5); // 5 second timeout
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Cleans up a test database by dropping all tables
     */
    public static void cleanupDatabase(ConfigParams config) {
        try (Connection conn = DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPass());
             Statement stmt = conn.createStatement()) {
            
            // Drop tables in correct order (considering foreign keys)
            try { stmt.execute("DROP VIEW IF EXISTS active_users"); } catch (SQLException ignored) {}
            try { stmt.execute("DROP TABLE IF EXISTS orders"); } catch (SQLException ignored) {}
            try { stmt.execute("DROP TABLE IF EXISTS users"); } catch (SQLException ignored) {}
            try { stmt.execute("DROP TABLE IF EXISTS simple_table"); } catch (SQLException ignored) {}
            try { stmt.execute("DROP TABLE IF EXISTS data_types_test"); } catch (SQLException ignored) {}
            try { stmt.execute("DROP TABLE IF EXISTS large_table"); } catch (SQLException ignored) {}
            
        } catch (SQLException e) {
            // Ignore cleanup errors
        }
    }

    /**
     * Creates a large dataset for performance testing
     */
    public static void setupLargeDataset(ConfigParams config, int rowCount) throws SQLException {
        try (Connection conn = DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPass());
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("""
                CREATE TABLE large_table (
                    id INT PRIMARY KEY,
                    name VARCHAR(100),
                    description TEXT,
                    value DECIMAL(10,2),
                    created_date DATE
                )
                """);

            // Insert data in batches for better performance
            conn.setAutoCommit(false);
            for (int i = 1; i <= rowCount; i++) {
                stmt.addBatch(String.format("""
                    INSERT INTO large_table VALUES (
                        %d,
                        'Name %d',
                        'Description for record %d with some longer text content',
                        %.2f,
                        '2024-01-01'
                    )
                    """, i, i, i, i * 10.5));
                
                if (i % 1000 == 0) {
                    stmt.executeBatch();
                    conn.commit();
                }
            }
            stmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        }
    }

    /**
     * Executes code with suppressed logging for expected exception tests
     */
    public static <T> T withSuppressedLogging(java.util.function.Supplier<T> operation) {
        // For slf4j-simple, we can't programmatically change log levels easily
        // But we can redirect System.err temporarily to suppress stack traces
        java.io.PrintStream originalErr = System.err;
        try {
            // Redirect stderr to a null stream to suppress exception stack traces
            System.setErr(new java.io.PrintStream(new java.io.OutputStream() {
                @Override
                public void write(int b) {
                    // Discard output
                }
            }));
            return operation.get();
        } finally {
            System.setErr(originalErr);
        }
    }

    /**
     * Executes code with suppressed logging for expected exception tests (void version)
     */
    public static void withSuppressedLogging(Runnable operation) {
        withSuppressedLogging(() -> {
            operation.run();
            return null;
        });
    }

    /**
     * Helper method to initialize the server for tests
     */
    public static void initializeServer(McpServer mcpServer, ObjectMapper objectMapper) {
        // Step 1: Send initialize request
        ObjectNode initRequest = objectMapper.createObjectNode();
        initRequest.put("id", 1);
        initRequest.put("method", "initialize");

        ObjectNode initParams = objectMapper.createObjectNode();
        initParams.put("protocolVersion", "2025-11-25");
        ObjectNode capabilities = objectMapper.createObjectNode();
        ObjectNode toolsCap = objectMapper.createObjectNode();
        toolsCap.put("listChanged", false);
        capabilities.set("tools", toolsCap);
        ObjectNode resourcesCap = objectMapper.createObjectNode();
        resourcesCap.put("subscribe", false);
        resourcesCap.put("listChanged", false);
        capabilities.set("resources", resourcesCap);
        initParams.set("capabilities", capabilities);
        initRequest.set("params", initParams);

        JsonNode initResponse = mcpServer.handleRequest(initRequest);
        assertNotNull(initResponse);
        assertTrue(initResponse.has("result"));

        // Step 2: Send initialized notification
        ObjectNode initializedRequest = objectMapper.createObjectNode();
        initializedRequest.put("method", "notifications/initialized");
        // No id field for notifications

        JsonNode initializedResponse = mcpServer.handleRequest(initializedRequest);
        assertNull(initializedResponse); // Notifications don't return responses
    }

    /**
     * Creates a standard JSON-RPC request for the 'tools/call' MCP method.
     *
     * @param toolName  The name of the tool to be called (e.g., "run_sql", "describe_table").
     * @param arguments The JSON object containing the arguments for the tool.
     * @return An ObjectNode representing the complete 'tools/call' request.
     */
    public static ObjectNode createToolCallRequest(String toolName, ObjectNode arguments, ObjectMapper objectMapper) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", "test-req-1"); // A standard ID for testing
        request.put("method", "tools/call");

        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", toolName);
        params.set("arguments", arguments);

        request.set("params", params);
        return request;
    }

    /**
     * Test data factory for creating sample database resources
     */
    public static class TestDataFactory {
        
        public static DatabaseResource createTableResource(String tableName) {
            return new DatabaseResource(
                "database://table/" + tableName,
                tableName,
                "TABLE: Test table " + tableName,
                "text/plain",
                String.format("Table: %s\n\nColumns:\n  - id (INT) NOT NULL\n  - name (VARCHAR)", tableName)
            );
        }
        
        public static DatabaseResource createViewResource(String viewName) {
            return new DatabaseResource(
                "database://table/" + viewName,
                viewName,
                "VIEW: Test view " + viewName,
                "text/plain",
                String.format("View: %s\n\nColumns:\n  - id (INT)\n  - name (VARCHAR)", viewName)
            );
        }
        
        public static DatabaseResource createSchemaResource(String schemaName) {
            return new DatabaseResource(
                "database://schema/" + schemaName,
                schemaName,
                "Database schema: " + schemaName,
                "text/plain",
                String.format("Schema: %s\n\nTables in this schema:\n  - users\n  - orders", schemaName)
            );
        }
        
        public static QueryResult createSampleQueryResult() {
            return new QueryResult(
                java.util.Arrays.asList("id", "name", "email"),
                java.util.Arrays.asList(
                    java.util.Arrays.asList(1, "John Doe", "john@example.com"),
                    java.util.Arrays.asList(2, "Jane Smith", "jane@example.com")
                ),
                2,
                150L
            );
        }
        
        public static QueryResult createEmptyQueryResult() {
            return new QueryResult(
                java.util.Arrays.asList("id", "name"),
                java.util.Collections.emptyList(),
                0,
                25L
            );
        }
        
        public static QueryResult createUpdateQueryResult(int affectedRows) {
            return new QueryResult(
                    List.of("affected_rows"),
                    List.of(List.of(affectedRows)),
                affectedRows,
                75L
            );
        }
    }

    /**
     * Custom assertion helpers for testing database operations
     */
    public static class DatabaseAssertions {
        
        public static void assertValidDatabaseConfig(ConfigParams config) {
            org.assertj.core.api.Assertions.assertThat(config.dbUrl()).isNotBlank();
            org.assertj.core.api.Assertions.assertThat(config.dbDriver()).isNotBlank();
            org.assertj.core.api.Assertions.assertThat(config.getDatabaseType()).isNotEqualTo("unknown");
        }
        
        public static void assertValidQueryResult(QueryResult result) {
            org.assertj.core.api.Assertions.assertThat(result).isNotNull();
            org.assertj.core.api.Assertions.assertThat(result.allColumns()).isNotNull();
            org.assertj.core.api.Assertions.assertThat(result.allRows()).isNotNull();
            org.assertj.core.api.Assertions.assertThat(result.executionTimeMs()).isGreaterThanOrEqualTo(0);
        }
        
        public static void assertValidDatabaseResource(DatabaseResource resource) {
            org.assertj.core.api.Assertions.assertThat(resource).isNotNull();
            org.assertj.core.api.Assertions.assertThat(resource.uri()).isNotBlank();
            org.assertj.core.api.Assertions.assertThat(resource.name()).isNotBlank();
            org.assertj.core.api.Assertions.assertThat(resource.mimeType()).isNotBlank();
        }
        
        public static void assertMcpResponse(com.fasterxml.jackson.databind.JsonNode response) {
            org.assertj.core.api.Assertions.assertThat(response).isNotNull();
            org.assertj.core.api.Assertions.assertThat(response.path("jsonrpc").asText()).isEqualTo("2.0");
            org.assertj.core.api.Assertions.assertThat(response.has("result") || response.has("error")).isTrue();
        }
        
        public static void assertMcpSuccessResponse(com.fasterxml.jackson.databind.JsonNode response) {
            assertMcpResponse(response);
            org.assertj.core.api.Assertions.assertThat(response.has("result")).isTrue();
            org.assertj.core.api.Assertions.assertThat(response.has("error")).isFalse();
        }
        
        public static void assertMcpErrorResponse(com.fasterxml.jackson.databind.JsonNode response, String expectedErrorCode) {
            assertMcpResponse(response);
            org.assertj.core.api.Assertions.assertThat(response.has("error")).isTrue();
            org.assertj.core.api.Assertions.assertThat(response.has("result")).isFalse();
            org.assertj.core.api.Assertions.assertThat(response.path("error").path("code").asText()).isEqualTo(expectedErrorCode);
        }
    }

    /**
     * JUnit 5 extension for providing test database configurations
     */
    public static class DatabaseConfigExtension implements ParameterResolver {
        
        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
            return parameterContext.getParameter().getType().equals(ConfigParams.class);
        }
        
        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
            return createTestH2Config();
        }
    }

    /**
     * Test performance helper
     */
    public static class PerformanceHelper {
        
        public static long measureExecutionTime(Runnable operation) {
            long startTime = System.currentTimeMillis();
            operation.run();
            return System.currentTimeMillis() - startTime;
        }
        
        public static <T> TimedResult<T> measureExecution(java.util.function.Supplier<T> operation) {
            long startTime = System.currentTimeMillis();
            T result = operation.get();
            long executionTime = System.currentTimeMillis() - startTime;
            return new TimedResult<>(result, executionTime);
        }
        
        public static class TimedResult<T> {
            private final T result;
            private final long executionTimeMs;
            
            public TimedResult(T result, long executionTimeMs) {
                this.result = result;
                this.executionTimeMs = executionTimeMs;
            }
            
            public T getResult() { return result; }
            public long getExecutionTimeMs() { return executionTimeMs; }
        }
    }

    /**
     * SQL template builder for testing
     */
    public static class SqlTemplates {
        
        public static String createTable(String tableName, String... columns) {
            return String.format("CREATE TABLE %s (%s)", tableName, String.join(", ", columns));
        }
        
        public static String insertInto(String tableName, Object... values) {
            String valuesList = java.util.Arrays.stream(values)
                .map(v -> v instanceof String ? "'" + v + "'" : String.valueOf(v))
                .collect(java.util.stream.Collectors.joining(", "));
            return String.format("INSERT INTO %s VALUES (%s)", tableName, valuesList);
        }
        
        public static String selectAll(String tableName) {
            return "SELECT * FROM " + tableName;
        }
        
        public static String selectCount(String tableName) {
            return "SELECT COUNT(*) as count FROM " + tableName;
        }
        
        public static String selectWithLimit(String tableName, int limit) {
            return String.format("SELECT * FROM %s LIMIT %d", tableName, limit);
        }
    }
}
