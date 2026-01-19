package com.skanga.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skanga.mcp.config.ConfigParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@EnabledIf("isEnabled")
class McpServerContainerTest {
    static boolean isEnabled() {
        return isDockerAvailable() && isNotGitHubActions();
    }
    static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    static boolean isNotGitHubActions() {
        // Skip these tests in GitHub Actions if they're problematic
        return System.getenv("GITHUB_ACTIONS") == null;
    }

    @Container
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withStartupTimeout(Duration.ofMinutes(5)) // Increased timeout for CI
            .withConnectTimeoutSeconds(120)
            .withReuse(false)
            .withCommand("--default-authentication-plugin=mysql_native_password") // Fix auth issues
            .withEnv("MYSQL_ROOT_HOST", "%") // Allow connections from any host
            .withTmpFs(java.util.Map.of("/var/lib/mysql", "rw")) // Use tmpfs for faster I/O in CI
            .withLogConsumer(outputFrame -> {
                // Log container output for debugging
                System.out.print("[MYSQL] " + outputFrame.getUtf8String());
            });

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withStartupTimeout(Duration.ofMinutes(5))
            .withConnectTimeoutSeconds(120)
            .withReuse(false)
            .withTmpFs(java.util.Map.of("/var/lib/postgresql/data", "rw"))
            .withLogConsumer(outputFrame -> System.out.print("[POSTGRES] " + outputFrame.getUtf8String()));

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should work end-to-end with MySQL")
    void shouldWorkEndToEndWithMySQL() throws Exception {
        // Ensure container is started and ready
        if (!mysqlContainer.isRunning()) {
            mysqlContainer.start();
        }

        // Wait for container to be fully ready
        Thread.sleep(2000);
        
        ConfigParams config = ConfigParams.defaultConfig(
            mysqlContainer.getJdbcUrl(),
            mysqlContainer.getUsername(),
            mysqlContainer.getPassword(),
            "com.mysql.cj.jdbc.Driver"
        );

        setupMySQLTestData(config);
        McpServer server = new McpServer(config);

        // Test initialize
        JsonNode initRequest = objectMapper.readTree("""
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "initialize",
                "params": {
                    "protocolVersion": "2025-11-25",
                    "capabilities": {},
                    "clientInfo": {"name": "test-client", "version": "1.0.0"}
                }
            }
            """);

        JsonNode initResponse = server.handleRequest(initRequest);
        assertThat(initResponse.path("result").path("protocolVersion").asText()).isEqualTo("2025-11-25");

        // Test list tools
        JsonNode toolsRequest = objectMapper.readTree("""
            {
                "jsonrpc": "2.0",
                "id": 2,
                "method": "tools/list",
                "params": {}
            }
            """);

        JsonNode toolsResponse = server.handleRequest(toolsRequest);
        assertThat(toolsResponse.path("result").path("tools")).hasSize(1);
        assertThat(toolsResponse.path("result").path("tools").get(0).path("name").asText()).isEqualTo("run_sql");

        // Test list resources
        JsonNode resourcesRequest = objectMapper.readTree("""
            {
                "jsonrpc": "2.0",
                "id": 3,
                "method": "resources/list",
                "params": {}
            }
            """);

        JsonNode resourcesResponse = server.handleRequest(resourcesRequest);
        JsonNode resources = resourcesResponse.path("result").path("resources");
        assertThat(resources.size()).isGreaterThan(0);
        
        // Should have database info and test_users table
        boolean hasInfoResource = false;
        boolean hasUsersTable = false;
        
        for (JsonNode resource : resources) {
            String uri = resource.path("uri").asText();
            if (uri.equals("database://info")) hasInfoResource = true;
            if (uri.equals("database://table/test_users")) hasUsersTable = true;
        }
        
        assertThat(hasInfoResource).isTrue();
        assertThat(hasUsersTable).isTrue();

        // Test query execution
        JsonNode queryRequest = objectMapper.readTree("""
            {
                "jsonrpc": "2.0",
                "id": 4,
                "method": "tools/call",
                "params": {
                    "name": "run_sql",
                    "arguments": {
                        "sql": "SELECT * FROM test_users ORDER BY id",
                        "maxRows": 100
                    }
                }
            }
            """);

        JsonNode queryResponse = server.handleRequest(queryRequest);
        assertThat(queryResponse.path("result").path("isError").asBoolean()).isFalse();
        
        String resultText = queryResponse.path("result").path("content").get(0).path("text").asText();
        assertThat(resultText).contains("John Doe");
        assertThat(resultText).contains("Jane Smith");
        assertThat(resultText).contains("Rows returned: 2");

        // Test read table resource
        JsonNode readResourceRequest = objectMapper.readTree("""
            {
                "jsonrpc": "2.0",
                "id": 5,
                "method": "resources/read",
                "params": {
                    "uri": "database://table/test_users"
                }
            }
            """);

        JsonNode readResourceResponse = server.handleRequest(readResourceRequest);
        String tableContent = readResourceResponse.path("result").path("contents").get(0).path("text").asText();
        assertThat(tableContent).contains("Table: test_users");
        assertThat(tableContent).contains("Columns:");
        assertThat(tableContent).contains("id");
        assertThat(tableContent).contains("name");
    }

    @Test
    @DisplayName("Should work end-to-end with PostgreSQL")
    void shouldWorkEndToEndWithPostgreSQL() throws Exception {
        // Ensure container is started and ready
        if (!postgresContainer.isRunning()) {
            postgresContainer.start();
        }

        // Wait for container to be fully ready
        Thread.sleep(2000);
        
        ConfigParams config = ConfigParams.defaultConfig(
            postgresContainer.getJdbcUrl(),
            postgresContainer.getUsername(),
            postgresContainer.getPassword(),
            "org.postgresql.Driver"
        );

        setupPostgreSQLTestData(config);
        McpServer server = new McpServer(config);

        // Test query execution
        JsonNode queryRequest = objectMapper.readTree("""
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "tools/call",
                "params": {
                    "name": "run_sql",
                    "arguments": {
                        "sql": "SELECT COUNT(*) as user_count FROM test_users",
                        "maxRows": 1
                    }
                }
            }
            """);

        JsonNode queryResponse = server.handleRequest(queryRequest);
        assertThat(queryResponse.path("result").path("isError").asBoolean()).isFalse();
        
        String resultText = queryResponse.path("result").path("content").get(0).path("text").asText();
        assertThat(resultText).contains("user_count");
        assertThat(resultText).contains("2"); // Should show count of 2

        // Test database info resource
        JsonNode readInfoRequest = objectMapper.readTree("""
            {
                "jsonrpc": "2.0",
                "id": 2,
                "method": "resources/read",
                "params": {
                    "uri": "database://info"
                }
            }
            """);

        JsonNode readInfoResponse = server.handleRequest(readInfoRequest);
        String infoContent = readInfoResponse.path("result").path("contents").get(0).path("text").asText();
        assertThat(infoContent).contains("PostgreSQL");
        assertThat(infoContent).contains("Database Information");
    }

    @Test
    @DisplayName("Should handle H2 in-memory database")
    void shouldHandleH2InMemoryDatabase() throws Exception {
        // Given
        ConfigParams config = ConfigParams.defaultConfig(
            "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
            "sa",
            "",
            "org.h2.Driver"
        );

        setupH2TestData(config);
        McpServer server = new McpServer(config);

        // Test complex query with joins
        JsonNode queryRequest = objectMapper.readTree("""
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "tools/call",
                "params": {
                    "name": "run_sql",
                    "arguments": {
                        "sql": "SELECT u.name, o.order_date FROM test_users u LEFT JOIN test_orders o ON u.id = o.user_id ORDER BY u.id",
                        "maxRows": 100
                    }
                }
            }
            """);

        JsonNode queryResponse = server.handleRequest(queryRequest);
        assertThat(queryResponse.path("result").path("isError").asBoolean()).isFalse();
        
        String resultText = queryResponse.path("result").path("content").get(0).path("text").asText();
        assertThat(resultText).contains("John Doe");
        assertThat(resultText).contains("2024-01-01");
    }

    @Test
    @DisplayName("Should handle SQL errors gracefully")
    void shouldHandleSQLErrorsGracefully() throws Exception {
        // Given
        ConfigParams config = ConfigParams.defaultConfig(
            "jdbc:h2:mem:testdb2",
            "sa",
            "",
            "org.h2.Driver"
        );

        McpServer server = new McpServer(config);

        // Test invalid SQL
        JsonNode queryRequest = objectMapper.readTree("""
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "tools/call",
                "params": {
                    "name": "run_sql",
                    "arguments": {
                        "sql": "SELECT * FROM non_existent_table",
                        "maxRows": 100
                    }
                }
            }
            """);

        JsonNode queryResponse = server.handleRequest(queryRequest);
        assertThat(queryResponse.has("error")).isTrue();
        assertThat(queryResponse.path("error").path("code").asText()).isEqualTo("database_error");
        assertThat(queryResponse.path("error").path("message").asText()).contains("Database error");
    }

    @Test
    @DisplayName("Should handle large result sets with row limit")
    void shouldHandleLargeResultSetsWithRowLimit() throws Exception {
        // Given
        ConfigParams config = ConfigParams.defaultConfig(
            "jdbc:h2:mem:testdb3",
            "sa",
            "",
            "org.h2.Driver"
        );

        setupH2LargeDataset(config);
        McpServer server = new McpServer(config);

        // Test with small row limit
        JsonNode queryRequest = objectMapper.readTree("""
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "tools/call",
                "params": {
                    "name": "run_sql",
                    "arguments": {
                        "sql": "SELECT * FROM large_table ORDER BY id",
                        "maxRows": 5
                    }
                }
            }
            """);

        JsonNode queryResponse = server.handleRequest(queryRequest);
        assertThat(queryResponse.path("result").path("isError").asBoolean()).isFalse();
        
        String resultText = queryResponse.path("result").path("content").get(0).path("text").asText();
        assertThat(resultText).contains("Rows returned: 5");
        
        // Count the number of data rows in the table output
        String[] lines = resultText.split("\n");
        long dataRows = java.util.Arrays.stream(lines)
            .filter(line -> line.matches("\\d+\\s+\\|.*")) // Lines starting with numbers
            .count();
        assertThat(dataRows).isEqualTo(5);
    }

    private void setupMySQLTestData(ConfigParams config) throws Exception {
        // Retry connection setup with exponential backoff
        int maxRetries = 3;
        int retryDelay = 1000; // 1 second

        for (int i = 0; i < maxRetries; i++) {
            try (Connection conn = DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPass());
                 Statement stmt = conn.createStatement()) {

                stmt.execute("CREATE TABLE test_users (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255) NOT NULL, email VARCHAR(255))");
                stmt.execute("INSERT INTO test_users (name, email) VALUES ('John Doe', 'john@example.com'), ('Jane Smith', 'jane@example.com')");
                return; // Success, exit retry loop
            } catch (Exception e) {
                if (i == maxRetries - 1) {
                    throw e; // Last retry, rethrow exception
                }
                Thread.sleep(retryDelay * (i + 1)); // Exponential backoff
            }
        }
    }

    private void setupPostgreSQLTestData(ConfigParams config) throws Exception {
        // Retry connection setup with exponential backoff
        int maxRetries = 3;
        int retryDelay = 1000; // 1 second

        for (int i = 0; i < maxRetries; i++) {
            try (Connection conn = DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPass());
                 Statement stmt = conn.createStatement()) {

                stmt.execute("CREATE TABLE test_users (id SERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL, email VARCHAR(255))");
                stmt.execute("INSERT INTO test_users (name, email) VALUES ('John Doe', 'john@example.com'), ('Jane Smith', 'jane@example.com')");
                return; // Success, exit retry loop
            } catch (Exception e) {
                if (i == maxRetries - 1) {
                    throw e; // Last retry, rethrow exception
                }
                Thread.sleep(retryDelay * (i + 1)); // Exponential backoff
            }
        }
    }

    private void setupH2TestData(ConfigParams config) throws Exception {
        try (Connection conn = DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPass());
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE test_users (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255) NOT NULL, email VARCHAR(255))");
            stmt.execute("CREATE TABLE test_orders (id INT PRIMARY KEY AUTO_INCREMENT, user_id INT, order_date DATE, FOREIGN KEY (user_id) REFERENCES test_users(id))");
            
            stmt.execute("INSERT INTO test_users (name, email) VALUES ('John Doe', 'john@example.com'), ('Jane Smith', 'jane@example.com')");
            stmt.execute("INSERT INTO test_orders (user_id, order_date) VALUES (1, '2024-01-01'), (2, '2024-01-02')");
        }
    }

    private void setupH2LargeDataset(ConfigParams config) throws Exception {
        try (Connection conn = DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPass());
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE large_table (id INT PRIMARY KEY, value VARCHAR(50))");
            
            // Insert 100 rows
            for (int i = 1; i <= 100; i++) {
                stmt.execute(String.format("INSERT INTO large_table (id, value) VALUES (%d, 'Value %d')", i, i));
            }
        }
    }
}
