package com.skanga.mcp.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skanga.mcp.McpServer;
import com.skanga.mcp.TestUtils;
import com.skanga.mcp.config.ConfigParams;
import com.skanga.mcp.db.DatabaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class McpServerWorkflowIntegrationTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Mock
    private DatabaseService mockDatabaseService;
    
    @Mock
    private ConfigParams mockConfigParams;
    
    private McpServer mcpServer;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(mockDatabaseService.getDatabaseConfig()).thenReturn(mockConfigParams);
        when(mockConfigParams.getDatabaseType()).thenReturn("h2");
        
        mcpServer = new McpServer(mockDatabaseService);
    }
    
    @Test
    @DisplayName("Should initialize server and list workflow tools")
    void testServerInitializationWithWorkflowTools() {
        // Initialize server
        TestUtils.initializeServer(mcpServer, objectMapper);
        
        // Create tools/list request
        ObjectNode listRequest = objectMapper.createObjectNode();
        listRequest.put("method", "tools/list");
        listRequest.put("id", 1);
        
        JsonNode response = mcpServer.handleRequest(listRequest);
        
        assertThat(response).isNotNull();
        assertThat(response.has("result")).isTrue();
        
        JsonNode result = response.get("result");
        assertThat(result.has("tools")).isTrue();
        
        JsonNode tools = result.get("tools");
        assertThat(tools.isArray()).isTrue();
        assertThat(tools.size()).isEqualTo(6);
        
        // Verify workflow tools are present
        boolean hasStartWorkflow = false;
        boolean hasWorkflowChoice = false;
        
        for (JsonNode tool : tools) {
            String toolName = tool.get("name").asText();
            if ("start_workflow".equals(toolName)) {
                hasStartWorkflow = true;
                assertThat(tool.get("description").asText()).contains("INTERACTIVE WORKFLOW");
            } else if ("workflow_choice".equals(toolName)) {
                hasWorkflowChoice = true;
                assertThat(tool.get("description").asText()).contains("WORKFLOW PROGRESSION");
            }
        }
        
        assertThat(hasStartWorkflow).isTrue();
        assertThat(hasWorkflowChoice).isTrue();
    }
    
    @Test
    @DisplayName("Should list workflow resources")
    void testServerListWorkflowResources() throws Exception {
        // Initialize server
        TestUtils.initializeServer(mcpServer, objectMapper);
        
        // Mock database resources
        when(mockDatabaseService.listResources()).thenReturn(java.util.List.of());
        
        // Create resources/list request
        ObjectNode listRequest = objectMapper.createObjectNode();
        listRequest.put("method", "resources/list");
        listRequest.put("id", 1);
        
        JsonNode response = mcpServer.handleRequest(listRequest);
        
        assertThat(response).isNotNull();
        assertThat(response.has("result")).isTrue();
        
        JsonNode result = response.get("result");
        assertThat(result.has("resources")).isTrue();
        
        JsonNode resources = result.get("resources");
        assertThat(resources.isArray()).isTrue();
        assertThat(resources.size()).isGreaterThanOrEqualTo(3); // demo + workflow resources
        
        // Verify workflow status resource is present
        boolean hasWorkflowStatus = false;
        
        for (JsonNode resource : resources) {
            String uri = resource.get("uri").asText();
            if ("workflow://status".equals(uri)) {
                hasWorkflowStatus = true;
                assertThat(resource.get("name").asText()).isEqualTo("Active Workflow Status");
                assertThat(resource.get("mimeType").asText()).isEqualTo("application/json");
            }
        }
        
        assertThat(hasWorkflowStatus).isTrue();
    }
    
    @Test
    @DisplayName("Should handle invalid start_workflow parameters")
    void testExecuteStartWorkflowToolInvalidParams() {
        // Initialize server
        TestUtils.initializeServer(mcpServer, objectMapper);
        
        // Create start_workflow tool call with invalid scenario
        ObjectNode toolCall = objectMapper.createObjectNode();
        toolCall.put("method", "tools/call");
        toolCall.put("id", 1);
        
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "start_workflow");
        
        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("scenario", "invalid_scenario");
        params.set("arguments", arguments);
        
        toolCall.set("params", params);
        
        JsonNode response = mcpServer.handleRequest(toolCall);
        
        assertThat(response).isNotNull();
        assertThat(response.has("result")).isTrue();
        
        JsonNode result = response.get("result");
        JsonNode content = result.get("content");
        String text = content.get(0).get("text").asText();
        
        assertThat(text).contains("WORKFLOW ERROR");
        assertThat(text).contains("Invalid scenario: invalid_scenario");
    }
    
    @Test
    @DisplayName("Should handle missing start_workflow parameters")
    void testExecuteStartWorkflowToolMissingParams() {
        // Initialize server
        TestUtils.initializeServer(mcpServer, objectMapper);
        
        // Create start_workflow tool call without scenario
        ObjectNode toolCall = objectMapper.createObjectNode();
        toolCall.put("method", "tools/call");
        toolCall.put("id", 1);
        
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "start_workflow");
        
        ObjectNode arguments = objectMapper.createObjectNode();
        // Missing scenario parameter
        params.set("arguments", arguments);
        
        toolCall.set("params", params);
        
        JsonNode response = mcpServer.handleRequest(toolCall);
        
        assertThat(response).isNotNull();
        assertThat(response.has("error")).isTrue();
        
        JsonNode error = response.get("error");
        assertThat(error.get("message").asText()).contains("Missing required parameter: scenario");
    }

    @Test
    @DisplayName("Should handle missing workflow_choice parameters")
    void testExecuteWorkflowChoiceToolMissingParams() {
        // Initialize server
        TestUtils.initializeServer(mcpServer, objectMapper);
        
        // Create workflow_choice tool call without required parameters
        ObjectNode toolCall = objectMapper.createObjectNode();
        toolCall.put("method", "tools/call");
        toolCall.put("id", 1);
        
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "workflow_choice");
        
        ObjectNode arguments = objectMapper.createObjectNode();
        // Missing workflowId and choiceId parameters
        params.set("arguments", arguments);
        
        toolCall.set("params", params);
        
        JsonNode response = mcpServer.handleRequest(toolCall);
        
        assertThat(response).isNotNull();
        assertThat(response.has("error")).isTrue();
        
        JsonNode error = response.get("error");
        assertThat(error.get("message").asText()).contains("Missing required parameter: workflowId");
    }
    
    @Test
    @DisplayName("Should read workflow status resource")
    void testReadWorkflowStatusResource() throws Exception {
        // Initialize server
        TestUtils.initializeServer(mcpServer, objectMapper);
        
        // Create resources/read request for workflow status
        ObjectNode readRequest = objectMapper.createObjectNode();
        readRequest.put("method", "resources/read");
        readRequest.put("id", 1);
        
        ObjectNode params = objectMapper.createObjectNode();
        params.put("uri", "workflow://status");
        readRequest.set("params", params);
        
        JsonNode response = mcpServer.handleRequest(readRequest);
        
        assertThat(response).isNotNull();
        assertThat(response.has("result")).isTrue();
        
        JsonNode result = response.get("result");
        assertThat(result.has("contents")).isTrue();
        
        JsonNode contents = result.get("contents");
        assertThat(contents.isArray()).isTrue();
        assertThat(contents.size()).isEqualTo(1);
        
        JsonNode content = contents.get(0);
        assertThat(content.get("uri").asText()).isEqualTo("workflow://status");
        assertThat(content.get("mimeType").asText()).isEqualTo("application/json");
        assertThat(content.has("text")).isTrue();
        
        // Parse the JSON content
        String contentText = content.get("text").asText();
        JsonNode statusJson = objectMapper.readTree(contentText);
        
        assertThat(statusJson.get("title").asText()).isEqualTo("Active Workflow Status");
        assertThat(statusJson.get("databaseType").asText()).isEqualTo("H2");
        assertThat(statusJson.has("timestamp")).isTrue();
        assertThat(statusJson.get("totalActiveWorkflows").asInt()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("Should handle invalid workflow resource URI")
    void testReadInvalidWorkflowResource() {
        // Initialize server
        TestUtils.initializeServer(mcpServer, objectMapper);
        
        // Create resources/read request for invalid workflow resource
        ObjectNode readRequest = objectMapper.createObjectNode();
        readRequest.put("method", "resources/read");
        readRequest.put("id", 1);
        
        ObjectNode params = objectMapper.createObjectNode();
        params.put("uri", "workflow://invalid");
        readRequest.set("params", params);
        
        JsonNode response = mcpServer.handleRequest(readRequest);
        
        assertThat(response).isNotNull();
        assertThat(response.has("error")).isTrue();
        
        JsonNode error = response.get("error");
        assertThat(error.get("message").asText()).contains("Unknown workflow resource: workflow://invalid");
    }
    
    @Test
    @DisplayName("Should execute setup_demo_scenario tool")
    void testExecuteSetupDemoScenarioTool() throws Exception {
        // Initialize server
        TestUtils.initializeServer(mcpServer, objectMapper);

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(mockDatabaseService.getConnection()).thenReturn(connection);

        // Create setup_demo_scenario tool call
        ObjectNode toolCall = objectMapper.createObjectNode();
        toolCall.put("method", "tools/call");
        toolCall.put("id", 1);
        
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "setup_demo_scenario");
        
        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("scenario", "retail");
        arguments.put("reset", true);
        params.set("arguments", arguments);
        
        toolCall.set("params", params);
        
        JsonNode response = mcpServer.handleRequest(toolCall);
        
        assertThat(response).isNotNull();
        assertThat(response.has("result")).isTrue();
        
        JsonNode result = response.get("result");
        assertThat(result.has("content")).isTrue();
        
        String text = result.get("content").get(0).get("text").asText();
        assertThat(text).contains("DEMO SCENARIO SETUP");
    }
    
    @Test
    @DisplayName("Should read demo scenarios resource")
    void testReadDemoScenariosResource() throws Exception {
        // Initialize server
        TestUtils.initializeServer(mcpServer, objectMapper);
        
        // Mock database connection to avoid NPE in this integration test
        when(mockDatabaseService.getConnection()).thenThrow(new java.sql.SQLException("Mock connection not available"));
        
        // Create resources/read request for demo scenarios
        ObjectNode readRequest = objectMapper.createObjectNode();
        readRequest.put("method", "resources/read");
        readRequest.put("id", 1);
        
        ObjectNode params = objectMapper.createObjectNode();
        params.put("uri", "demo://scenarios");
        readRequest.set("params", params);
        
        JsonNode response = mcpServer.handleRequest(readRequest);
        
        assertThat(response).isNotNull();
        
        // Check for errors first
        if (response.has("error")) {
            System.out.println("Error in demo scenarios resource: " + response.get("error"));
        }
        
        assertThat(response.has("result")).isTrue();

        JsonNode result = response.get("result");
        JsonNode contents = result.get("contents");
        JsonNode content = contents.get(0);
        
        assertThat(content.get("uri").asText()).isEqualTo("demo://scenarios");
        assertThat(content.get("mimeType").asText()).isEqualTo("application/json");
        
        String contentText = content.get("text").asText();
        JsonNode scenariosJson = objectMapper.readTree(contentText);
        
        assertThat(scenariosJson.get("title").asText()).isEqualTo("Available Demo Scenarios");
        assertThat(scenariosJson.has("scenarios")).isTrue();
        assertThat(scenariosJson.get("scenarios").size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should execute start_workflow tool successfully")
    void testExecuteStartWorkflowTool() {
        // Initialize server
        TestUtils.initializeServer(mcpServer, objectMapper);

        // Create start_workflow tool call
        ObjectNode toolCall = objectMapper.createObjectNode();
        toolCall.put("method", "tools/call");
        toolCall.put("id", 1);

        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "start_workflow");

        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("scenario", "retail");
        arguments.put("userId", "testUser");
        params.set("arguments", arguments);

        toolCall.set("params", params);

        JsonNode response = mcpServer.handleRequest(toolCall);

        assertThat(response).isNotNull();
        assertThat(response.has("result")).isTrue();

        JsonNode result = response.get("result");
        assertThat(result.has("content")).isTrue();

        JsonNode content = result.get("content");
        assertThat(content.isArray()).isTrue();
        assertThat(content.size()).isEqualTo(1);

        JsonNode textContent = content.get(0);
        assertThat(textContent.get("type").asText()).isEqualTo("text");

        String text = textContent.get("text").asText();
        assertThat(text).contains("INTERACTIVE WORKFLOW STARTED");
        assertThat(text).contains("RETAIL");
        assertThat(text).contains("workflow_choice");
    }

    @Test
    @DisplayName("Should execute workflow_choice tool with error for invalid workflow")
    void testExecuteWorkflowChoiceToolInvalidWorkflow() {
        // Initialize server
        TestUtils.initializeServer(mcpServer, objectMapper);

        // Create workflow_choice tool call with invalid workflow ID
        ObjectNode toolCall = objectMapper.createObjectNode();
        toolCall.put("method", "tools/call");
        toolCall.put("id", 1);

        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", "workflow_choice");

        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("workflowId", "invalid_workflow_id");
        arguments.put("choiceId", "test_choice");
        params.set("arguments", arguments);

        toolCall.set("params", params);

        JsonNode response = mcpServer.handleRequest(toolCall);

        assertThat(response).isNotNull();
        assertThat(response.has("result")).isTrue();

        JsonNode result = response.get("result");
        JsonNode content = result.get("content");
        String text = content.get(0).get("text").asText();

        assertThat(text).contains("WORKFLOW ERROR");
        assertThat(text).contains("Workflow not found: invalid_workflow_id");
    }
}
