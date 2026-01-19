package com.skanga.mcp;

import com.skanga.mcp.config.CliUtils;
import com.skanga.mcp.config.ConfigParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.*;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for configuration loading and edge cases in McpServer
 */
class McpServerConfigurationTest {
    private PrintStream originalErr;

    @BeforeEach
    void setUp() {
        originalErr = System.err;
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errorOutput));
    }

    @AfterEach
    void tearDown() {
        System.setErr(originalErr);
        // Clear any system properties set during tests
        System.clearProperty("db.url");
        System.clearProperty("http.mode");
        System.clearProperty("http.port");
    }

    @Test
    void testConfigurationPrecedence_CliOverridesProp() throws IOException {
        // Test that CLI arguments override environment variables
        // This tests the getConfigValue method indirectly

        String[] args = {"--db_url=jdbc:test:cli"};

        // Set environment variable
        // Note: We can't actually set env vars in Java tests easily,
        // but we can test the system property precedence
        System.setProperty("db.url", "jdbc:test:sysprop");

        ConfigParams config = CliUtils.loadConfiguration(args);
        assertEquals("jdbc:test:cli", config.dbUrl());
    }

    @Test
    void testConfigurationPrecedence_SystemProperty() throws IOException {
        String[] args = {}; // No CLI args

        System.setProperty("db.url", "jdbc:test:sysprop");

        ConfigParams config = CliUtils.loadConfiguration(args);
        assertEquals("jdbc:test:sysprop", config.dbUrl());
    }

    @Test
    void testConfigurationPrecedence_DefaultValues() throws IOException {
        String[] args = {}; // No CLI args, no system properties

        ConfigParams config = CliUtils.loadConfiguration(args);
        assertEquals("jdbc:h2:mem:test", config.dbUrl());
        assertEquals("sa", config.dbUser());
        assertEquals("", config.dbPass());
        assertEquals("org.h2.Driver", config.dbDriver());
        assertTrue(config.selectOnly());
    }

    @Test
    void testParseArgs_ValidArguments() {
        String[] args = {
                "--db_url=jdbc:test:url",
                "--max_connections=20",
                "--select_only=false",
                "--some-flag", // Should not be ignored
                "not-an-arg", // Should be ignored
                "-h",
                "--version",
                "-xyz",
                "ppp"
        };

        Map<String, String> parsed = CliUtils.parseArgs(args);
        assertEquals("jdbc:test:url", parsed.get("DB_URL"));
        assertEquals("20", parsed.get("MAX_CONNECTIONS"));
        assertEquals("false", parsed.get("SELECT_ONLY"));
        assertTrue(parsed.containsKey("SOME-FLAG"));
        assertFalse(parsed.containsKey("NOT-AN-ARG"));
        assertTrue(parsed.containsKey("HELP"));
        assertTrue(parsed.containsKey("VERSION"));
        assertFalse(parsed.containsKey("XYZ"));
        assertFalse(parsed.containsKey("PPP"));
    }

    @Test
    void testParseArgs_EmptyArguments() {
        String[] args = {};
        Map<String, String> parsed = CliUtils.parseArgs(args);
        assertTrue(parsed.isEmpty());
    }

    @Test
    void testIsHttpMode_DefaultFalse() {
        String[] args = {};
        assertFalse(CliUtils.isHttpMode(args));
    }

    @Test
    void testIsHttpMode_CliArgument() {
        String[] args = {"--http_mode=true"};
        assertTrue(CliUtils.isHttpMode(args));
    }

    @Test
    void testIsHttpMode_SystemProperty() {
        String[] args = {};
        System.setProperty("http.mode", "true");
        assertTrue(CliUtils.isHttpMode(args));
    }

    @Test
    void testGetHttpPort_Default() {
        String[] args = {};
        assertEquals(8080, CliUtils.getHttpPort(args));
    }

    @Test
    void testGetHttpPort_CliArgument() {
        String[] args = {"--http_port=9090"};
        assertEquals(9090, CliUtils.getHttpPort(args));
    }

    @Test
    void testMainMethod_InvalidConfiguration() throws Exception {
        String[] args = {"--max_connections=invalid"};

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                CliUtils.loadConfiguration(args)
        );

        assertTrue(exception.getMessage().contains("configuration parameters") ||
                exception.getMessage().contains("invalid"));
    }

    @Test
    void testMainMethod_StdioModeSuccess() {
        String[] args = {"--http_mode=false", "--db_url=jdbc:h2:mem:testdb", "--db_driver=org.h2.Driver"};

        // Create input that sends a proper JSON-RPC request and then closes
        String initRequest = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-11-25\",\"capabilities\":{},\"clientInfo\":{\"name\":\"test\",\"version\":\"1.0\"}}}\n";
        ByteArrayInputStream testInput = new ByteArrayInputStream(initRequest.getBytes());

        // Capture output
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        InputStream originalIn = System.in;

        try {
            System.setIn(testInput);
            System.setOut(new PrintStream(capturedOutput));

            // Run in separate thread with timeout to prevent hanging
            Future<Void> future = Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    McpServer.main(args);
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // Wait for completion or timeout
            assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));

            // Verify output contains expected response
            String output = capturedOutput.toString();
            assertTrue(output.contains("\"jsonrpc\":\"2.0\""), "Should contain JSON-RPC response");
            assertTrue(output.contains("\"result\""), "Should contain successful result");

        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }

    @Test
    void testShutdownHook() {
        // Test that shutdown hook is properly registered
        ConfigParams config = new ConfigParams(
                "jdbc:h2:mem:testdb", "sa", "", "org.h2.Driver",
                10, 30000, 30, true, 10000, 10000, 600000, 1800000, 60000
        );

        McpServer server = new McpServer(config);

        // Create the shutdown hook manually to test it
        Thread shutdownHook = new Thread(server.databaseService::close);

        // Verify the hook runs without error
        assertDoesNotThrow(shutdownHook::run);
    }
}
