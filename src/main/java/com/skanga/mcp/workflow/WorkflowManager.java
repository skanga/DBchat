package com.skanga.mcp.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Interactive Multiple Choice Workflow System - Manages guided database analysis workflows
 * with structured progressions, contextual choices, and state management.
 * Designed for non-technical users to navigate complex database analysis scenarios.
 */
public class WorkflowManager {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowManager.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Workflow state storage (in-memory for now)
    private final Map<String, WorkflowState> activeWorkflows = new ConcurrentHashMap<>();
    private final String databaseType;
    private final LongSupplier currentTimeSupplier;
    private final AtomicLong workflowSequence = new AtomicLong();

    public WorkflowManager(String databaseType) {
        this(databaseType, System::currentTimeMillis);
    }

    WorkflowManager(String databaseType, LongSupplier currentTimeSupplier) {
        this.databaseType = databaseType != null ? databaseType : "unknown";
        this.currentTimeSupplier = Objects.requireNonNull(currentTimeSupplier, "currentTimeSupplier");
        logger.info("WorkflowManager initialized for database type: {}", this.databaseType);
    }

    /**
     * Starts a new interactive workflow for the given scenario
     */
    public JsonNode startWorkflow(String scenarioType, String userId) {
        String workflowId = generateWorkflowId(userId, scenarioType);

        WorkflowState state = new WorkflowState();
        state.workflowId = workflowId;
        state.scenarioType = scenarioType;
        state.userId = userId;
        state.currentStep = 0;
        state.stepHistory = new ArrayList<>();
        state.userChoices = new HashMap<>();
        state.startTime = currentTimeSupplier.getAsLong();

        activeWorkflows.put(workflowId, state);

        logger.info("Started new workflow: {} for user: {} with scenario: {}", workflowId, userId, scenarioType);

        return generateStepResponse(state);
    }

    /**
     * Processes user choice and advances workflow to next step
     */
    public JsonNode processChoice(String workflowId, String choiceId, Map<String, String> additionalData) {
        WorkflowState state = activeWorkflows.get(workflowId);
        if (state == null) {
            return createErrorResponse("Workflow not found: " + workflowId);
        }

        // Record the choice
        state.userChoices.put("step_" + state.currentStep, choiceId);
        state.stepHistory.add(new StepRecord(state.currentStep, choiceId, currentTimeSupplier.getAsLong()));

        // Advance to next step
        state.currentStep++;

        logger.info("Processed choice '{}' for workflow: {}, advancing to step: {}",
                choiceId, workflowId, state.currentStep);

        return generateStepResponse(state);
    }

    /**
     * Gets current workflow status
     */
    public JsonNode getWorkflowStatus(String workflowId) {
        WorkflowState state = activeWorkflows.get(workflowId);
        if (state == null) {
            return createErrorResponse("Workflow not found: " + workflowId);
        }

        ObjectNode status = objectMapper.createObjectNode();
        status.put("workflowId", state.workflowId);
        status.put("scenarioType", state.scenarioType);
        status.put("currentStep", state.currentStep);
        status.put("totalSteps", getTotalSteps(state.scenarioType));
        status.put("progress", calculateProgress(state));
        status.put("elapsedTime", currentTimeSupplier.getAsLong() - state.startTime);

        ArrayNode choicesArray = objectMapper.createArrayNode();
        state.userChoices.forEach((step, choice) -> {
            ObjectNode choiceRecord = objectMapper.createObjectNode();
            choiceRecord.put("step", step);
            choiceRecord.put("choice", choice);
            choicesArray.add(choiceRecord);
        });
        status.set("choices", choicesArray);

        return status;
    }

    /**
     * Completes and cleans up workflow
     */
    public JsonNode completeWorkflow(String workflowId) {
        WorkflowState state = activeWorkflows.remove(workflowId);
        if (state == null) {
            return createErrorResponse("Workflow not found: " + workflowId);
        }

        long duration = currentTimeSupplier.getAsLong() - state.startTime;

        ObjectNode completion = objectMapper.createObjectNode();
        completion.put("workflowId", workflowId);
        completion.put("completed", true);
        completion.put("scenarioType", state.scenarioType);
        completion.put("totalSteps", state.currentStep);
        completion.put("duration", duration);
        completion.put("summary", generateWorkflowSummary(state));

        logger.info("Completed workflow: {} in {} ms with {} steps", workflowId, duration, state.currentStep);

        return completion;
    }

    /**
     * Lists all active workflows
     */
    public JsonNode listActiveWorkflows() {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("totalActive", activeWorkflows.size());

        ArrayNode workflows = objectMapper.createArrayNode();
        activeWorkflows.values().forEach(state -> {
            ObjectNode workflowInfo = objectMapper.createObjectNode();
            workflowInfo.put("workflowId", state.workflowId);
            workflowInfo.put("scenarioType", state.scenarioType);
            workflowInfo.put("currentStep", state.currentStep);
            workflowInfo.put("elapsedTime", currentTimeSupplier.getAsLong() - state.startTime);
            workflows.add(workflowInfo);
        });

        response.set("workflows", workflows);
        return response;
    }

    // Private helper methods
    private JsonNode generateStepResponse(WorkflowState state) {
        WorkflowStep step = getStepDefinition(state, state.scenarioType, state.currentStep);

        if (step == null) {
            // Workflow completed
            return completeWorkflow(state.workflowId);
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("workflowId", state.workflowId);
        response.put("stepNumber", state.currentStep);
        response.put("stepType", step.type);
        response.put("title", step.title);
        response.put("description", step.description);
        response.put("progress", calculateProgress(state));

        // Add context-specific content
        if (step.hasQuery) {
            response.put("suggestedQuery", step.suggestedQuery);
        }

        // Add multiple choice options
        ArrayNode choices = objectMapper.createArrayNode();
        for (WorkflowChoice choice : step.choices) {
            ObjectNode choiceNode = objectMapper.createObjectNode();
            choiceNode.put("id", choice.id);
            choiceNode.put("label", choice.label);
            choiceNode.put("description", choice.description);
            choiceNode.put("recommended", choice.recommended);
            choices.add(choiceNode);
        }
        response.set("choices", choices);

        // Add guidance and tips
        ObjectNode guidance = objectMapper.createObjectNode();
        guidance.put("tip", step.tip);
        guidance.put("whatToExpect", step.whatToExpect);
        guidance.put("nextSteps", step.nextSteps);
        response.set("guidance", guidance);

        return response;
    }

    private WorkflowStep getStepDefinition(WorkflowState state, String scenarioType, int stepNumber) {
        return switch (scenarioType.toLowerCase()) {
            case "retail" -> getRetailWorkflowStep(state, stepNumber);
            case "finance" -> getFinanceWorkflowStep(stepNumber);
            case "logistics" -> getLogisticsWorkflowStep(stepNumber);
            default -> getGenericWorkflowStep(stepNumber);
        };
    }

    private WorkflowStep getRetailWorkflowStep(WorkflowState state, int stepNumber) {
        return switch (stepNumber) {
            case 0 -> new WorkflowStep(
                    "welcome",
                    "Welcome to E-commerce Analysis",
                    "Let's explore TechnoMart's sales data to understand customer behavior and business performance.",
                    Arrays.asList(
                            new WorkflowChoice("explore_customers", "Start with Customer Analysis", "Understand who our customers are", true),
                            new WorkflowChoice("explore_products", "Start with Product Performance", "See which products sell best", false),
                            new WorkflowChoice("explore_orders", "Start with Order Patterns", "Analyze purchasing trends", false)
                    ),
                    false, null,
                    "Choose where to begin your analysis. Customer analysis is recommended for newcomers.",
                    "We'll examine data and build insights together.",
                    "After this choice, we'll run your first SQL query."
            );
            case 1 -> {
                String choice = getCurrentUserChoice(state, stepNumber - 1);
                yield switch (choice) {
                    case "explore_customers" -> new WorkflowStep(
                            "query_customers",
                            "Customer Data Exploration",
                            "Let's start by looking at our customer base to understand who shops with TechnoMart.",
                            Arrays.asList(
                                    new WorkflowChoice("run_query", "Run the Customer Query", "Execute: SELECT * FROM customers LIMIT 10", true),
                                    new WorkflowChoice("modify_query", "Modify the Query First", "I want to change what we select", false),
                                    new WorkflowChoice("skip_step", "Skip to Next Step", "I'm familiar with the data", false)
                            ),
                            true, "SELECT customer_id, first_name, last_name, email, customer_tier, total_spent FROM customers ORDER BY total_spent DESC LIMIT 10",
                            "This query shows our top customers by spending. Notice the customer tiers!",
                            "You'll see customer names, emails, their tier status, and spending amounts.",
                            "Next we'll analyze what insights we can draw from this data."
                    );
                    default -> getGenericQueryStep();
                };
            }
            case 2 -> new WorkflowStep(
                    "insight_capture",
                    "Capture Your First Insight",
                    "Based on the customer data you just saw, what stands out to you?",
                    Arrays.asList(
                            new WorkflowChoice("insight_tiers", "Customer Tiers Matter", "I notice different customer tiers with varying spending", true),
                            new WorkflowChoice("insight_spending", "Wide Spending Range", "There's a big difference between top and average customers", false),
                            new WorkflowChoice("insight_emails", "Email Patterns", "I see patterns in customer email addresses", false),
                            new WorkflowChoice("custom_insight", "Write My Own Insight", "I have a different observation", false)
                    ),
                    false, null,
                    "Look for patterns, surprises, or business implications in the data you just saw.",
                    "We'll use the append_insight tool to capture your observation.",
                    "Your insight will be saved and included in the final analysis report."
            );
            case 3 -> new WorkflowStep(
                    "next_analysis",
                    "What Should We Explore Next?",
                    "Great insight! Now let's dig deeper into the TechnoMart data.",
                    Arrays.asList(
                            new WorkflowChoice("analyze_products", "Product Performance", "See which products are selling best", true),
                            new WorkflowChoice("analyze_orders", "Order Patterns", "Understand when and how customers buy", false),
                            new WorkflowChoice("analyze_inventory", "Inventory Levels", "Check stock levels and turnover", false),
                            new WorkflowChoice("create_report", "Generate Analysis Report", "Summarize findings so far", false)
                    ),
                    false, null,
                    "Each area will reveal different aspects of the business performance.",
                    "We'll continue with guided queries and insight capture.",
                    "The analysis builds a comprehensive view of business operations."
            );
            default -> null; // End of workflow
        };
    }

    private WorkflowStep getFinanceWorkflowStep(int stepNumber) {
        return switch (stepNumber) {
            case 0 -> new WorkflowStep(
                    "welcome",
                    "Welcome to Banking Analysis",
                    "Let's explore FinanceFirst bank's customer data to understand financial patterns and risk profiles.",
                    Arrays.asList(
                            new WorkflowChoice("explore_accounts", "Start with Account Analysis", "Understand account types and balances", true),
                            new WorkflowChoice("explore_transactions", "Start with Transaction Patterns", "Analyze spending and deposit patterns", false),
                            new WorkflowChoice("explore_loans", "Start with Loan Portfolio", "Review loan performance and risk", false)
                    ),
                    false, null,
                    "Banking analysis requires careful attention to financial patterns and regulatory compliance.",
                    "We'll examine financial data while respecting privacy and security.",
                    "Each step builds understanding of customer financial behavior."
            );
            case 1 -> new WorkflowStep(
                    "query_accounts",
                    "Account Portfolio Overview",
                    "Let's examine the bank's account portfolio to understand customer relationships.",
                    Arrays.asList(
                            new WorkflowChoice("run_query", "Run the Account Query", "Execute: SELECT account_type, COUNT(*), AVG(balance) FROM accounts GROUP BY account_type", true),
                            new WorkflowChoice("modify_query", "Customize the Query", "I want to modify the analysis", false),
                            new WorkflowChoice("skip_step", "Skip to Transactions", "Move to transaction analysis", false)
                    ),
                    true, "SELECT account_type, COUNT(*) as account_count, AVG(balance) as avg_balance, SUM(balance) as total_balance FROM accounts GROUP BY account_type ORDER BY total_balance DESC",
                    "This shows account types, counts, and average balances across the portfolio.",
                    "You'll see how different account types contribute to the bank's deposits.",
                    "Next we'll explore what these numbers mean for customer segmentation."
            );
            default -> null;
        };
    }

    private WorkflowStep getLogisticsWorkflowStep(int stepNumber) {
        return switch (stepNumber) {
            case 0 -> new WorkflowStep(
                    "welcome",
                    "Welcome to Supply Chain Analysis",
                    "Let's explore GlobalShip's logistics data to understand delivery performance and operational efficiency.",
                    Arrays.asList(
                            new WorkflowChoice("explore_shipments", "Start with Shipment Analysis", "Understand delivery patterns and status", true),
                            new WorkflowChoice("explore_routes", "Start with Route Efficiency", "Analyze route performance and costs", false),
                            new WorkflowChoice("explore_warehouses", "Start with Warehouse Operations", "Review capacity and utilization", false)
                    ),
                    false, null,
                    "Logistics analysis focuses on efficiency, timing, and operational performance.",
                    "We'll examine supply chain data to identify optimization opportunities.",
                    "Each analysis area reveals different aspects of operational effectiveness."
            );
            default -> null;
        };
    }

    private WorkflowStep getGenericWorkflowStep(int stepNumber) {
        return switch (stepNumber) {
            case 0 -> new WorkflowStep(
                    "welcome",
                    "Welcome to Database Analysis",
                    "Let's explore your database to understand the data structure and business insights.",
                    Arrays.asList(
                            new WorkflowChoice("explore_tables", "Explore Database Tables", "See what tables and data are available", true),
                            new WorkflowChoice("run_queries", "Start with Queries", "Begin running SQL queries immediately", false),
                            new WorkflowChoice("guided_analysis", "Guided Analysis", "Let me guide you through the process", false)
                    ),
                    false, null,
                    "Start by understanding your data structure before diving into analysis.",
                    "We'll build understanding progressively through guided exploration.",
                    "Each step will build on previous discoveries."
            );
            default -> null;
        };
    }

    private WorkflowStep getGenericQueryStep() {
        return new WorkflowStep(
                "generic_query",
                "Database Exploration",
                "Let's run a query to explore your data.",
                Arrays.asList(
                        new WorkflowChoice("run_query", "Run Suggested Query", "Execute the recommended query", true),
                        new WorkflowChoice("modify_query", "Modify Query", "Change the query first", false),
                        new WorkflowChoice("skip_step", "Skip This Step", "Move to next step", false)
                ),
                true, "SELECT * FROM information_schema.tables LIMIT 10",
                "This will show you available tables in your database.",
                "You'll see table names and basic structure information.",
                "Next we'll dive deeper into specific tables."
        );
    }

    private String getCurrentUserChoice(WorkflowState state, int stepNumber) {
        return state.userChoices.get("step_" + stepNumber);
    }

    private int getTotalSteps(String scenarioType) {
        return switch (scenarioType.toLowerCase()) {
            case "retail" -> 4;
            case "finance" -> 2;
            case "logistics", "generic" -> 1;
            default -> 1;
        };
    }

    private double calculateProgress(WorkflowState state) {
        int totalSteps = getTotalSteps(state.scenarioType);
        return Math.min(100.0, (double) state.currentStep / totalSteps * 100.0);
    }

    private String generateWorkflowId(String userId, String scenarioType) {
        return String.format(
                "wf_%s_%s_%d_%d",
                userId,
                scenarioType,
                currentTimeSupplier.getAsLong(),
                workflowSequence.incrementAndGet()
        );
    }

    private String generateWorkflowSummary(WorkflowState state) {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Completed %s workflow with %d steps. ",
                state.scenarioType, state.currentStep));
        summary.append("User choices: ");
        state.userChoices.forEach((step, choice) ->
                summary.append(String.format("%s=%s; ", step, choice)));
        return summary.toString();
    }

    private JsonNode createErrorResponse(String message) {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("success", false);
        error.put("error", message);
        return error;
    }

    public static class WorkflowStep {
        final String type;
        final String title;
        final String description;
        final List<WorkflowChoice> choices;
        final boolean hasQuery;
        final String suggestedQuery;
        final String tip;
        final String whatToExpect;
        final String nextSteps;

        public WorkflowStep(String type, String title, String description,
                           List<WorkflowChoice> choices, boolean hasQuery, String suggestedQuery,
                           String tip, String whatToExpect, String nextSteps) {
            this.type = type;
            this.title = title;
            this.description = description;
            this.choices = choices;
            this.hasQuery = hasQuery;
            this.suggestedQuery = suggestedQuery;
            this.tip = tip;
            this.whatToExpect = whatToExpect;
            this.nextSteps = nextSteps;
        }
    }

    public static class WorkflowState {
        String workflowId;
        String scenarioType;
        String userId;
        int currentStep;
        List<StepRecord> stepHistory;
        Map<String, String> userChoices;
        long startTime;
    }

    public static class WorkflowChoice {
        final String id;
        final String label;
        final String description;
        final boolean recommended;

        public WorkflowChoice(String id, String label, String description, boolean recommended) {
            this.id = id;
            this.label = label;
            this.description = description;
            this.recommended = recommended;
        }
    }

    public static class StepRecord {
        final int stepNumber;
        final String choice;
        final long timestamp;

        public StepRecord(int stepNumber, String choice, long timestamp) {
            this.stepNumber = stepNumber;
            this.choice = choice;
            this.timestamp = timestamp;
        }
    }
}
