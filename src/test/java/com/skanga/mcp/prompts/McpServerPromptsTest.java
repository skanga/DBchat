package com.skanga.mcp.prompts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skanga.mcp.McpServer;
import com.skanga.mcp.config.ConfigParams;
import com.skanga.mcp.db.DatabaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class McpServerPromptsTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Mock
    private DatabaseService mockDatabaseService;
    
    @Mock
    private ConfigParams mockConfigParams;
    
    private McpServer mcpServer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock the database service to return a database type
        when(mockDatabaseService.getDatabaseConfig()).thenReturn(mockConfigParams);
        when(mockConfigParams.getDatabaseType()).thenReturn("h2");
        
        mcpServer = new McpServer(mockDatabaseService);
    }

    @Test
    @DisplayName("Should declare prompts capability in server capabilities")
    void testPromptsCapabilityDeclared() {
        // Initialize the server
        ObjectNode initRequest = objectMapper.createObjectNode();
        initRequest.put("method", "initialize");
        initRequest.put("id", 1);
        
        ObjectNode params = objectMapper.createObjectNode();
        params.put("protocolVersion", "2025-11-25");
        initRequest.set("params", params);
        
        JsonNode response = mcpServer.handleRequest(initRequest);
        
        assertThat(response).isNotNull();
        assertThat(response.has("result")).isTrue();
        
        JsonNode result = response.get("result");
        assertThat(result.has("capabilities")).isTrue();
        
        JsonNode capabilities = result.get("capabilities");
        assertThat(capabilities.has("prompts")).isTrue();
        
        JsonNode promptsCapability = capabilities.get("prompts");
        assertThat(promptsCapability.get("listChanged").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("Should handle prompts/list request")
    void testHandlePromptsList() {
        // First initialize the server
        initializeServer();
        
        // Create prompts/list request
        ObjectNode listRequest = objectMapper.createObjectNode();
        listRequest.put("method", "prompts/list");
        listRequest.put("id", 2);
        
        JsonNode response = mcpServer.handleRequest(listRequest);
        
        assertThat(response).isNotNull();
        assertThat(response.has("result")).isTrue();
        
        JsonNode result = response.get("result");
        assertThat(result.has("prompts")).isTrue();
        assertThat(result.get("prompts").isArray()).isTrue();
        assertThat(result.get("prompts").size()).isEqualTo(3);
        
        // Check that all expected prompts are present
        String[] expectedPrompts = {"mcp-demo", "business-intelligence", "database-analysis"};
        JsonNode prompts = result.get("prompts");
        
        for (String expectedPrompt : expectedPrompts) {
            boolean found = false;
            for (JsonNode prompt : prompts) {
                if (expectedPrompt.equals(prompt.get("name").asText())) {
                    found = true;
                    assertThat(prompt.has("description")).isTrue();
                    assertThat(prompt.has("arguments")).isTrue();
                    break;
                }
            }
            assertThat(found).as("Expected prompt %s not found", expectedPrompt).isTrue();
        }
    }

    @Test
    @DisplayName("Should handle prompts/get request for mcp-demo")
    void testHandlePromptsGetMcpDemo() {
        // First initialize the server
        initializeServer();
        
        // Create prompts/get request
        ObjectNode getRequest = objectMapper.createObjectNode();
        getRequest.put("method", "prompts/get");
        getRequest.put("id", 3);
        
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "mcp-demo");
        
        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("topic", "retail");
        params.set("arguments", arguments);
        
        getRequest.set("params", params);
        
        JsonNode response = mcpServer.handleRequest(getRequest);
        
        assertThat(response).isNotNull();
        assertThat(response.has("result")).isTrue();
        
        JsonNode result = response.get("result");
        assertThat(result.get("name").asText()).isEqualTo("mcp-demo");
        assertThat(result.has("description")).isTrue();
        assertThat(result.has("messages")).isTrue();
        assertThat(result.get("messages").isArray()).isTrue();
        
        JsonNode message = result.get("messages").get(0);
        assertThat(message.get("role").asText()).isEqualTo("user");
        assertThat(message.has("content")).isTrue();
        
        String contentText = message.get("content").get("text").asText();
        assertThat(contentText).contains("INTERACTIVE DATABASE ANALYSIS DEMO");
        assertThat(contentText).contains("TechnoMart");
    }

    @Test
    @DisplayName("Should handle prompts/get request for business-intelligence")
    void testHandlePromptsGetBusinessIntelligence() {
        // First initialize the server
        initializeServer();
        
        // Create prompts/get request
        ObjectNode getRequest = objectMapper.createObjectNode();
        getRequest.put("method", "prompts/get");
        getRequest.put("id", 4);
        
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "business-intelligence");
        
        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("focus_area", "sales");
        params.set("arguments", arguments);
        
        getRequest.set("params", params);
        
        JsonNode response = mcpServer.handleRequest(getRequest);
        
        assertThat(response).isNotNull();
        assertThat(response.has("result")).isTrue();
        
        JsonNode result = response.get("result");
        assertThat(result.get("name").asText()).isEqualTo("business-intelligence");
        
        String contentText = result.get("messages").get(0).get("content").get("text").asText();
        assertThat(contentText).contains("BUSINESS INTELLIGENCE ANALYSIS");
        assertThat(contentText).contains("Focus Area: sales");
    }

    @Test
    @DisplayName("Should handle prompts/get request for database-analysis")
    void testHandlePromptsGetDatabaseAnalysis() {
        // First initialize the server
        initializeServer();
        
        // Create prompts/get request
        ObjectNode getRequest = objectMapper.createObjectNode();
        getRequest.put("method", "prompts/get");
        getRequest.put("id", 5);
        
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "database-analysis");
        
        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("focus_area", "customers");
        params.set("arguments", arguments);
        
        getRequest.set("params", params);
        
        JsonNode response = mcpServer.handleRequest(getRequest);
        
        assertThat(response).isNotNull();
        assertThat(response.has("result")).isTrue();
        
        JsonNode result = response.get("result");
        assertThat(result.get("name").asText()).isEqualTo("database-analysis");
        
        String contentText = result.get("messages").get(0).get("content").get("text").asText();
        assertThat(contentText).contains("DATABASE ANALYSIS WORKFLOW");
        assertThat(contentText).contains("Focus Area: customers");
        assertThat(contentText).contains("H2"); // Database type should be included
    }

    @Test
    @DisplayName("Should return error for unknown prompt")
    void testHandlePromptsGetUnknownPrompt() {
        // First initialize the server
        initializeServer();
        
        // Create prompts/get request for unknown prompt
        ObjectNode getRequest = objectMapper.createObjectNode();
        getRequest.put("method", "prompts/get");
        getRequest.put("id", 6);
        
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "unknown-prompt");
        getRequest.set("params", params);
        
        JsonNode response = mcpServer.handleRequest(getRequest);
        
        assertThat(response).isNotNull();
        assertThat(response.has("error")).isTrue();
        
        JsonNode error = response.get("error");
        assertThat(error.get("message").asText()).contains("Invalid prompt request");
    }

    @Test
    @DisplayName("Should return error for prompts/get without name parameter")
    void testHandlePromptsGetWithoutName() {
        // First initialize the server
        initializeServer();
        
        // Create prompts/get request without name
        ObjectNode getRequest = objectMapper.createObjectNode();
        getRequest.put("method", "prompts/get");
        getRequest.put("id", 7);
        
        ObjectNode params = objectMapper.createObjectNode();
        // Missing name parameter
        getRequest.set("params", params);
        
        JsonNode response = mcpServer.handleRequest(getRequest);
        
        assertThat(response).isNotNull();
        assertThat(response.has("error")).isTrue();
        
        JsonNode error = response.get("error");
        assertThat(error.get("message").asText()).contains("Missing required parameter: name");
    }

    @Test
    @DisplayName("Should return error for database-analysis without focus_area")
    void testHandlePromptsGetDatabaseAnalysisWithoutFocusArea() {
        // First initialize the server
        initializeServer();
        
        // Create prompts/get request for database-analysis without focus_area
        ObjectNode getRequest = objectMapper.createObjectNode();
        getRequest.put("method", "prompts/get");
        getRequest.put("id", 8);
        
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "database-analysis");
        // Missing arguments with focus_area
        getRequest.set("params", params);
        
        JsonNode response = mcpServer.handleRequest(getRequest);
        
        assertThat(response).isNotNull();
        assertThat(response.has("error")).isTrue();
        
        JsonNode error = response.get("error");
        assertThat(error.get("message").asText()).contains("Missing required argument: focus_area");
    }
    @Test
    @DisplayName("Should handle prompts/list before initialization")
    void testHandlePromptsListBeforeInitialization() {
        // Create prompts/list request without initializing
        ObjectNode listRequest = objectMapper.createObjectNode();
        listRequest.put("method", "prompts/list");
        listRequest.put("id", 9);
        
        JsonNode response = mcpServer.handleRequest(listRequest);
        
        assertThat(response).isNotNull();
        assertThat(response.has("error")).isTrue();
        
        JsonNode error = response.get("error");
        assertThat(error.get("message").asText()).contains("not initialized");
    }

    private void initializeServer() {
        ObjectNode initRequest = objectMapper.createObjectNode();
        initRequest.put("method", "initialize");
        initRequest.put("id", 1);
        
        ObjectNode params = objectMapper.createObjectNode();
        params.put("protocolVersion", "2025-11-25");
        initRequest.set("params", params);
        
        mcpServer.handleRequest(initRequest);
        
        // Send initialized notification
        ObjectNode initNotification = objectMapper.createObjectNode();
        initNotification.put("method", "notifications/initialized");
        // No id for notifications
        
        mcpServer.handleRequest(initNotification);
    }
}
