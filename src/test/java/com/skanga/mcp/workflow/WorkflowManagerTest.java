package com.skanga.mcp.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowManagerTest {

    private WorkflowManager workflowManager;
    
    @BeforeEach
    void setUp() {
        workflowManager = new WorkflowManager("h2");
    }
    
    @Test
    @DisplayName("Should start retail workflow successfully")
    void testStartRetailWorkflow() {
        JsonNode result = workflowManager.startWorkflow("retail", "testUser");
        
        assertThat(result).isNotNull();
        assertThat(result.has("workflowId")).isTrue();
        assertThat(result.has("stepNumber")).isTrue();
        assertThat(result.has("stepType")).isTrue();
        assertThat(result.has("title")).isTrue();
        assertThat(result.has("description")).isTrue();
        assertThat(result.has("choices")).isTrue();
        assertThat(result.has("guidance")).isTrue();
        
        String workflowId = result.get("workflowId").asText();
        assertThat(workflowId).isNotEmpty();
        assertThat(workflowId).contains("wf_testUser_retail");
        
        assertThat(result.get("stepNumber").asInt()).isEqualTo(0);
        assertThat(result.get("stepType").asText()).isEqualTo("welcome");
        assertThat(result.get("title").asText()).isEqualTo("Welcome to E-commerce Analysis");
        assertThat(result.get("progress").asDouble()).isEqualTo(0.0);
        
        JsonNode choices = result.get("choices");
        assertThat(choices.isArray()).isTrue();
        assertThat(choices.size()).isEqualTo(3);
        
        // Check first choice is recommended
        JsonNode firstChoice = choices.get(0);
        assertThat(firstChoice.get("id").asText()).isEqualTo("explore_customers");
        assertThat(firstChoice.get("recommended").asBoolean()).isTrue();
        
        JsonNode guidance = result.get("guidance");
        assertThat(guidance.has("tip")).isTrue();
        assertThat(guidance.has("whatToExpect")).isTrue();
        assertThat(guidance.has("nextSteps")).isTrue();
    }
    
    @Test
    @DisplayName("Should start finance workflow successfully")
    void testStartFinanceWorkflow() {
        JsonNode result = workflowManager.startWorkflow("finance", "testUser");
        
        assertThat(result).isNotNull();
        assertThat(result.get("title").asText()).isEqualTo("Welcome to Banking Analysis");
        assertThat(result.get("stepType").asText()).isEqualTo("welcome");
        
        JsonNode choices = result.get("choices");
        assertThat(choices.size()).isEqualTo(3);
        
        JsonNode firstChoice = choices.get(0);
        assertThat(firstChoice.get("id").asText()).isEqualTo("explore_accounts");
        assertThat(firstChoice.get("recommended").asBoolean()).isTrue();
    }
    
    @Test
    @DisplayName("Should start logistics workflow successfully")
    void testStartLogisticsWorkflow() {
        JsonNode result = workflowManager.startWorkflow("logistics", "testUser");
        
        assertThat(result).isNotNull();
        assertThat(result.get("title").asText()).isEqualTo("Welcome to Supply Chain Analysis");
        assertThat(result.get("stepType").asText()).isEqualTo("welcome");
        
        JsonNode choices = result.get("choices");
        assertThat(choices.size()).isEqualTo(3);
        
        JsonNode firstChoice = choices.get(0);
        assertThat(firstChoice.get("id").asText()).isEqualTo("explore_shipments");
        assertThat(firstChoice.get("recommended").asBoolean()).isTrue();
    }
    
    @Test
    @DisplayName("Should start generic workflow successfully")
    void testStartGenericWorkflow() {
        JsonNode result = workflowManager.startWorkflow("generic", "testUser");
        
        assertThat(result).isNotNull();
        assertThat(result.get("title").asText()).isEqualTo("Welcome to Database Analysis");
        assertThat(result.get("stepType").asText()).isEqualTo("welcome");
        
        JsonNode choices = result.get("choices");
        assertThat(choices.size()).isEqualTo(3);
        
        JsonNode firstChoice = choices.get(0);
        assertThat(firstChoice.get("id").asText()).isEqualTo("explore_tables");
        assertThat(firstChoice.get("recommended").asBoolean()).isTrue();
    }
    
    @Test
    @DisplayName("Should process workflow choice and advance to next step")
    void testProcessChoice() {
        // Start workflow
        JsonNode startResult = workflowManager.startWorkflow("retail", "testUser");
        String workflowId = startResult.get("workflowId").asText();
        
        // Process first choice
        Map<String, String> additionalData = new HashMap<>();
        JsonNode choiceResult = workflowManager.processChoice(workflowId, "explore_customers", additionalData);
        
        assertThat(choiceResult).isNotNull();
        assertThat(choiceResult.get("workflowId").asText()).isEqualTo(workflowId);
        assertThat(choiceResult.get("stepNumber").asInt()).isEqualTo(1);
        
        // Should have progressed to step 1
        assertThat(choiceResult.get("stepType").asText()).isEqualTo("query_customers");
        assertThat(choiceResult.get("title").asText()).isEqualTo("Customer Data Exploration");
        assertThat(choiceResult.get("progress").asDouble()).isGreaterThan(0.0);
        
        // Should have suggested query
        assertThat(choiceResult.has("suggestedQuery")).isTrue();
        String suggestedQuery = choiceResult.get("suggestedQuery").asText();
        assertThat(suggestedQuery).contains("SELECT");
        assertThat(suggestedQuery).contains("customers");
        
        // Should have new choices
        JsonNode choices = choiceResult.get("choices");
        assertThat(choices.isArray()).isTrue();
        assertThat(choices.size()).isGreaterThan(0);
    }
    
    @Test
    @DisplayName("Should handle invalid workflow ID in process choice")
    void testProcessChoiceInvalidWorkflowId() {
        Map<String, String> additionalData = new HashMap<>();
        JsonNode result = workflowManager.processChoice("invalid_workflow", "test_choice", additionalData);
        
        assertThat(result).isNotNull();
        assertThat(result.has("success")).isTrue();
        assertThat(result.get("success").asBoolean()).isFalse();
        assertThat(result.get("error").asText()).contains("Workflow not found: invalid_workflow");
    }
    
    @Test
    @DisplayName("Should get workflow status for active workflow")
    void testGetWorkflowStatus() {
        // Start workflow
        JsonNode startResult = workflowManager.startWorkflow("retail", "testUser");
        String workflowId = startResult.get("workflowId").asText();
        
        // Get status
        JsonNode status = workflowManager.getWorkflowStatus(workflowId);
        
        assertThat(status).isNotNull();
        assertThat(status.get("workflowId").asText()).isEqualTo(workflowId);
        assertThat(status.get("scenarioType").asText()).isEqualTo("retail");
        assertThat(status.get("currentStep").asInt()).isEqualTo(0);
        assertThat(status.get("totalSteps").asInt()).isEqualTo(4);
        assertThat(status.get("progress").asDouble()).isEqualTo(0.0);
        assertThat(status.has("elapsedTime")).isTrue();
        assertThat(status.has("choices")).isTrue();
        
        JsonNode choices = status.get("choices");
        assertThat(choices.isArray()).isTrue();
        assertThat(choices.size()).isEqualTo(0); // No choices made yet
    }
    
    @Test
    @DisplayName("Should handle invalid workflow ID in get status")
    void testGetWorkflowStatusInvalidId() {
        JsonNode result = workflowManager.getWorkflowStatus("invalid_workflow");
        
        assertThat(result).isNotNull();
        assertThat(result.has("success")).isTrue();
        assertThat(result.get("success").asBoolean()).isFalse();
        assertThat(result.get("error").asText()).contains("Workflow not found: invalid_workflow");
    }
    
    @Test
    @DisplayName("Should list active workflows")
    void testListActiveWorkflows() {
        // Start multiple workflows
        workflowManager.startWorkflow("retail", "user1");
        workflowManager.startWorkflow("finance", "user2");
        
        JsonNode result = workflowManager.listActiveWorkflows();
        
        assertThat(result).isNotNull();
        assertThat(result.get("totalActive").asInt()).isEqualTo(2);
        assertThat(result.has("workflows")).isTrue();
        
        JsonNode workflows = result.get("workflows");
        assertThat(workflows.isArray()).isTrue();
        assertThat(workflows.size()).isEqualTo(2);
        
        // Check each workflow has required fields
        for (JsonNode workflow : workflows) {
            assertThat(workflow.has("workflowId")).isTrue();
            assertThat(workflow.has("scenarioType")).isTrue();
            assertThat(workflow.has("currentStep")).isTrue();
            assertThat(workflow.has("elapsedTime")).isTrue();
        }
    }
    
    @Test
    @DisplayName("Should complete workflow successfully")
    void testCompleteWorkflow() {
        // Start workflow
        JsonNode startResult = workflowManager.startWorkflow("retail", "testUser");
        String workflowId = startResult.get("workflowId").asText();
        
        // Complete workflow
        JsonNode completion = workflowManager.completeWorkflow(workflowId);
        
        assertThat(completion).isNotNull();
        assertThat(completion.get("workflowId").asText()).isEqualTo(workflowId);
        assertThat(completion.get("completed").asBoolean()).isTrue();
        assertThat(completion.get("scenarioType").asText()).isEqualTo("retail");
        assertThat(completion.has("totalSteps")).isTrue();
        assertThat(completion.has("duration")).isTrue();
        assertThat(completion.has("summary")).isTrue();
        
        // Workflow should be removed from active list
        JsonNode activeList = workflowManager.listActiveWorkflows();
        assertThat(activeList.get("totalActive").asInt()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("Should handle completion of invalid workflow")
    void testCompleteWorkflowInvalidId() {
        JsonNode result = workflowManager.completeWorkflow("invalid_workflow");
        
        assertThat(result).isNotNull();
        assertThat(result.has("success")).isTrue();
        assertThat(result.get("success").asBoolean()).isFalse();
        assertThat(result.get("error").asText()).contains("Workflow not found: invalid_workflow");
    }
    
    @Test
    @DisplayName("Should track workflow progression through multiple steps")
    void testWorkflowProgression() {
        // Start workflow
        JsonNode startResult = workflowManager.startWorkflow("retail", "testUser");
        String workflowId = startResult.get("workflowId").asText();
        
        // First choice
        Map<String, String> additionalData = new HashMap<>();
        JsonNode step1 = workflowManager.processChoice(workflowId, "explore_customers", additionalData);
        assertThat(step1.get("stepNumber").asInt()).isEqualTo(1);
        assertThat(step1.get("progress").asDouble()).isGreaterThan(0.0);
        
        // Second choice
        JsonNode step2 = workflowManager.processChoice(workflowId, "run_query", additionalData);
        assertThat(step2.get("stepNumber").asInt()).isEqualTo(2);
        assertThat(step2.get("progress").asDouble()).isGreaterThan(step1.get("progress").asDouble());
        
        // Check workflow status after progression
        JsonNode status = workflowManager.getWorkflowStatus(workflowId);
        assertThat(status.get("currentStep").asInt()).isEqualTo(2);
        
        JsonNode choices = status.get("choices");
        assertThat(choices.size()).isEqualTo(2); // Two choices made
        
        // Verify choice history
        JsonNode statusAfterChoices = workflowManager.getWorkflowStatus(workflowId);
        JsonNode choicesMade = statusAfterChoices.get("choices");
        assertThat(choicesMade.size()).isEqualTo(2);

        // The choices are not ordered, so we need to check for presence
        boolean foundExploreCustomers = false;
        boolean foundRunQuery = false;
        for (JsonNode choiceNode : choicesMade) {
            String choice = choiceNode.get("choice").asText();
            if (choice.equals("explore_customers")) {
                foundExploreCustomers = true;
            }
            if (choice.equals("run_query")) {
                foundRunQuery = true;
            }
        }
        assertThat(foundExploreCustomers).isTrue();
        assertThat(foundRunQuery).isTrue();
    }
    
    @Test
    @DisplayName("Should validate database type configuration")
    void testDatabaseTypeConfiguration() {
        // Should not throw exception and handle gracefully
        
        WorkflowManager managerWithValidType = new WorkflowManager("postgresql");
        JsonNode result = managerWithValidType.startWorkflow("retail", "testUser");
        assertThat(result).isNotNull();
    }
    
    @Test
    @DisplayName("Should handle concurrent workflow operations")
    void testConcurrentWorkflows() {
        // Start multiple workflows for same user
        JsonNode workflow1 = workflowManager.startWorkflow("retail", "testUser");
        JsonNode workflow2 = workflowManager.startWorkflow("finance", "testUser");
        
        String workflowId1 = workflow1.get("workflowId").asText();
        String workflowId2 = workflow2.get("workflowId").asText();
        
        assertThat(workflowId1).isNotEqualTo(workflowId2);
        
        // Process choices in different workflows
        Map<String, String> additionalData = new HashMap<>();
        JsonNode result1 = workflowManager.processChoice(workflowId1, "explore_customers", additionalData);
        JsonNode result2 = workflowManager.processChoice(workflowId2, "explore_accounts", additionalData);
        
        assertThat(result1.get("workflowId").asText()).isEqualTo(workflowId1);
        assertThat(result2.get("workflowId").asText()).isEqualTo(workflowId2);
        
        // List should show both active
        JsonNode activeList = workflowManager.listActiveWorkflows();
        assertThat(activeList.get("totalActive").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should generate unique workflow IDs when clock does not advance")
    void testWorkflowIdsRemainUniqueWhenClockDoesNotAdvance() {
        LongSupplier fixedClock = () -> 123456789L;
        WorkflowManager fixedClockManager = new WorkflowManager("h2", fixedClock);

        Set<String> workflowIds = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            JsonNode workflow = fixedClockManager.startWorkflow("retail", "testUser");
            workflowIds.add(workflow.get("workflowId").asText());
        }

        assertThat(workflowIds).hasSize(3);
        assertThat(fixedClockManager.listActiveWorkflows().get("totalActive").asInt()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should report finance workflow step count consistently")
    void testFinanceWorkflowStepCountIsConsistent() {
        JsonNode startResult = workflowManager.startWorkflow("finance", "testUser");
        String workflowId = startResult.get("workflowId").asText();

        JsonNode status = workflowManager.getWorkflowStatus(workflowId);
        assertThat(status.get("totalSteps").asInt()).isEqualTo(2);

        Map<String, String> additionalData = new HashMap<>();
        JsonNode nextStep = workflowManager.processChoice(workflowId, "explore_accounts", additionalData);
        assertThat(nextStep.get("progress").asDouble()).isEqualTo(50.0);

        JsonNode completion = workflowManager.processChoice(workflowId, "run_query", additionalData);
        assertThat(completion.get("completed").asBoolean()).isTrue();
        assertThat(completion.get("totalSteps").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should report logistics workflow step count consistently")
    void testLogisticsWorkflowStepCountIsConsistent() {
        JsonNode startResult = workflowManager.startWorkflow("logistics", "testUser");
        String workflowId = startResult.get("workflowId").asText();

        JsonNode status = workflowManager.getWorkflowStatus(workflowId);
        assertThat(status.get("totalSteps").asInt()).isEqualTo(1);

        Map<String, String> additionalData = new HashMap<>();
        JsonNode completion = workflowManager.processChoice(workflowId, "explore_shipments", additionalData);
        assertThat(completion.get("completed").asBoolean()).isTrue();
        assertThat(completion.get("totalSteps").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should report generic workflow step count consistently")
    void testGenericWorkflowStepCountIsConsistent() {
        JsonNode startResult = workflowManager.startWorkflow("generic", "testUser");
        String workflowId = startResult.get("workflowId").asText();

        JsonNode status = workflowManager.getWorkflowStatus(workflowId);
        assertThat(status.get("totalSteps").asInt()).isEqualTo(1);

        Map<String, String> additionalData = new HashMap<>();
        JsonNode completion = workflowManager.processChoice(workflowId, "explore_tables", additionalData);
        assertThat(completion.get("completed").asBoolean()).isTrue();
        assertThat(completion.get("totalSteps").asInt()).isEqualTo(1);
    }
}
