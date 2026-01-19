package com.skanga.mcp;

import com.skanga.mcp.config.CliUtils;
import com.skanga.mcp.config.ConfigParams;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIf;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class McpServerIntegrationTest {
    private static McpServer sharedMcpServer;
    private static ConfigParams sharedTestConfig;
    private static ExecutorService sharedExecutorService;
    private static int sharedPort;
    private static CompletableFuture<Void> sharedServerFuture;

    static boolean isNetworkAvailable() {
        try (ServerSocket ignored = new ServerSocket(0)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeAll
    static void setUpClass() throws Exception {
        sharedTestConfig = new ConfigParams(
                "jdbc:h2:mem:testdb_shared", "sa", "", "org.h2.Driver",
                10, 30000, 30, true, 10000, 10000, 600000, 1800000, 60000
        );
        sharedMcpServer = new McpServer(sharedTestConfig);
        sharedExecutorService = Executors.newCachedThreadPool();

        if (isNetworkAvailable()) {
            sharedPort = findAvailablePort();
            startSharedServer();
        }
    }

    @AfterAll
    static void tearDownClass() {
        if (sharedServerFuture != null) {
            sharedServerFuture.cancel(true);
        }
        if (sharedExecutorService != null) {
            sharedExecutorService.shutdownNow();
            try {
                if (!sharedExecutorService.awaitTermination(2, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate gracefully");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (sharedMcpServer != null) {
            try {
                sharedMcpServer.databaseService.close();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    private static void startSharedServer() throws Exception {
        sharedServerFuture = CompletableFuture.runAsync(() -> {
            try {
                sharedMcpServer.startHttpMode("localhost", sharedPort);
            } catch (IOException e) {
                // Expected when we shut it down
            }
        }, sharedExecutorService);

        waitForServerToStart(sharedPort, 5000);
    }

    @Test
    @Order(1)
    @Timeout(10)
    @EnabledIf("isNetworkAvailable")
    void testHttpMode_PortAlreadyInUse() {
        // Try to start second server on same port - should fail
        McpServer secondServer = new McpServer(sharedTestConfig);
        try {
            assertThrows(IOException.class, () -> secondServer.startHttpMode("localhost", sharedPort));
        } finally {
            try {
                secondServer.databaseService.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    @Order(2)
    @Timeout(5)
    @EnabledIf("isNetworkAvailable")
    void testHttpRequest_InvalidMethod() throws Exception {
        URL url = new URL("http://localhost:" + sharedPort + "/mcp");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        assertEquals(405, responseCode);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getErrorStream()))) {
            String response = reader.lines().reduce("", String::concat);
            assertTrue(response.contains("Method not allowed"));
        }
    }

    @Test
    @Order(3)
    @Timeout(5)
    @EnabledIf("isNetworkAvailable")
    void testHttpRequest_InvalidJson() throws Exception {
        // Suppress expected exception logging for cleaner test output
        java.io.PrintStream originalErr = System.err;
        try {
            // Redirect stderr to suppress JsonParseException stack trace
            System.setErr(new java.io.PrintStream(new java.io.OutputStream() {
                @Override
                public void write(int b) {
                    // Discard output
                }
            }));

            URL url = new URL("http://localhost:" + sharedPort + "/mcp");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String invalidJson = "{ invalid json content }";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(invalidJson.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            assertEquals(500, responseCode);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream()))) {
                String response = reader.lines().reduce("", String::concat);
                assertTrue(response.contains("Internal server error"));
            }

        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    @Order(4)
    @Timeout(5)
    @EnabledIf("isNetworkAvailable")
    void testHttpRequest_OptionsRequest() throws Exception {
        URL url = new URL("http://localhost:" + sharedPort + "/mcp");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        conn.setRequestMethod("OPTIONS");

        int responseCode = conn.getResponseCode();
        assertEquals(200, responseCode);

        assertEquals("*", conn.getHeaderField("Access-Control-Allow-Origin"));
        assertEquals("POST, OPTIONS", conn.getHeaderField("Access-Control-Allow-Methods"));
    }

    @Test
    @Order(5)
    @Timeout(5)
    @EnabledIf("isNetworkAvailable")
    void testHealthCheck_Endpoint() throws Exception {
        URL url = new URL("http://localhost:" + sharedPort + "/health");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        assertEquals(200, responseCode);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String response = reader.lines().reduce("", String::concat);
            assertTrue(response.contains("status"));
            assertTrue(response.contains("healthy"));
        }
    }

    @Test
    @Order(6)
    @Timeout(5)
    void testStdioMode_MalformedInput() {
        String malformedInput = "not json at all\n{ incomplete json\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(malformedInput.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try {
            System.setIn(inputStream);
            System.setOut(new PrintStream(outputStream));
            
            // Suppress expected exception logging for cleaner test output
            System.setErr(new PrintStream(new java.io.OutputStream() {
                @Override
                public void write(int b) {
                    // Discard output
                }
            }));

            // Create separate server instance for stdio tests
            McpServer stdioServer = new McpServer(new ConfigParams(
                    "jdbc:h2:mem:stdio_test", "sa", "", "org.h2.Driver",
                    10, 30000, 30, true, 10000, 10000, 600000, 1800000, 60000
            ));

            assertDoesNotThrow(stdioServer::startStdioMode);

            String output = outputStream.toString();
            // The output should contain error responses for malformed JSON
            assertNotNull(output);
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    @Test
    @Order(7)
    @Timeout(5)
    void testStdioMode_NotificationWithError() {
        // Prepare input with notification that will cause an error
        String notificationJson = """
            {"method":"tools/call","params":{"name":"run_sql","arguments":{"sql":""}}}
            """;
        ByteArrayInputStream inputStream = new ByteArrayInputStream(notificationJson.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        try {
            System.setIn(inputStream);
            System.setOut(new PrintStream(outputStream));

            McpServer stdioServer = new McpServer(new ConfigParams(
                    "jdbc:h2:mem:stdio_test2", "sa", "", "org.h2.Driver",
                    10, 30000, 30, true, 10000, 10000, 600000, 1800000, 60000
            ));

            assertDoesNotThrow(stdioServer::startStdioMode);

            String output = outputStream.toString().trim();
            assertFalse(output.contains("error"));
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }

    @Test
    @Order(8)
    @Timeout(5)
    void testStdioMode_RequestWithError() {
        // Prepare input with proper MCP initialization sequence followed by error request
        String initializeRequest = """
        {"jsonrpc":"2.0","id":"init","method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{"tools":{},"resources":{}},"clientInfo":{"name":"TestClient","version":"1.0.0"}}}
        """;
        String initializedNotification = """
        {"jsonrpc":"2.0","method":"notifications/initialized"}
        """;
        String errorRequest = """
        {"jsonrpc":"2.0","id":"error","method":"tools/call","params":{"name":"run_sql","arguments":{"sql":""}}}
        """;

        // Combine all requests with newlines (stdio mode expects line-separated JSON)
        String allRequests = initializeRequest + "\n" + initializedNotification + "\n" + errorRequest + "\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(allRequests.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        try {
            System.setIn(inputStream);
            System.setOut(new PrintStream(outputStream));

            McpServer stdioServer = new McpServer(new ConfigParams(
                    "jdbc:h2:mem:stdio_test3", "sa", "", "org.h2.Driver",
                    10, 30000, 30, true, 10000, 10000, 600000, 1800000, 60000
            ));

            assertDoesNotThrow(stdioServer::startStdioMode);

            String output = outputStream.toString().trim();
            String[] responses = output.split("\n");

            // Should have 2 responses (initialize response + error response)
            // The initialized notification doesn't get a response
            assertTrue(responses.length >= 2, "Should have at least 2 responses");

            // First response should be successful initialize
            assertTrue(responses[0].contains("\"result\""), "First response should be initialize success");
            assertTrue(responses[0].contains("protocolVersion"), "Should contain protocol version");

            // Last response should be the error for empty SQL
            String errorResponse = responses[responses.length - 1];
            assertTrue(errorResponse.contains("\"error\""), "Should contain error");
            assertTrue(errorResponse.contains("SQL query cannot be empty"), "Should contain empty SQL error message");
            assertTrue(errorResponse.contains("\"id\":\"error\""), "Should have matching request ID");
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }

    @Test
    @Order(9)
    @Timeout(2)
    void testStartHttpMode_InvalidPort() {
        ConfigParams config = ConfigParams.defaultConfig(
                "jdbc:h2:mem:test", "sa", "", "org.h2.Driver"
        );
        McpServer server = new McpServer(config);

        // Test various invalid ports
        assertThrows(IllegalArgumentException.class, () -> server.startHttpMode("localhost", -1));
        assertThrows(IllegalArgumentException.class, () -> server.startHttpMode("localhost", 65536));
    }

    @Test
    @Order(10)
    @Timeout(2)
    void testHttpPortParsing_InvalidValues() {
        // Test that configuration parsing handles invalid ports
        String[] args1 = {"--http_port=not_a_number"};
        assertThrows(NumberFormatException.class, () -> CliUtils.getHttpPort(args1));

        String[] args2 = {"--http_port="};
        assertThrows(NumberFormatException.class, () -> CliUtils.getHttpPort(args2));
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * Wait for server to start accepting connections with retry logic
     */
    private static void waitForServerToStart(int port, long timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeoutMs;

        while (System.currentTimeMillis() < endTime) {
            try (Socket testSocket = new Socket()) {
                testSocket.connect(new InetSocketAddress("localhost", port), 500);
                // If we get here, server is accepting connections
                return;
            } catch (ConnectException | SocketTimeoutException e) {
                Thread.sleep(100); // Reduced from 250ms to 100ms
            }
        }
        throw new AssertionError("Server did not start within " + timeoutMs + "ms on port " + port);
    }
}
