package com.skanga.mcp.demo;

import com.skanga.mcp.db.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;

/**
 * Automatic Demo Data Setup - Generates realistic business scenarios with synthetic data.
 * Supports multiple business domains with proper relationships and representative datasets.
 * Designed for database-agnostic operation with security-first principles.
 */
public class DemoDataGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DemoDataGenerator.class);
    
    private final DatabaseService databaseService;
    private final Random random = new Random(42); // Fixed seed for reproducible data
    
    // Demo scenario definitions
    private static final Map<String, DemoScenario> DEMO_SCENARIOS = new HashMap<>();
    
    static {
        // Retail E-commerce Scenario
        DEMO_SCENARIOS.put("retail", new DemoScenario(
            "retail",
            "E-commerce Retail Analysis",
            "Complete online retail dataset with customers, products, orders, and inventory for TechnoMart electronics store",
            Arrays.asList("customers", "products", "orders", "order_items", "inventory")
        ));
        
        // Finance Banking Scenario
        DEMO_SCENARIOS.put("finance", new DemoScenario(
            "finance",
            "Banking & Finance Analysis", 
            "Banking operations dataset with accounts, transactions, customers, and loans for FinanceFirst bank",
            Arrays.asList("customers", "accounts", "transactions", "loans", "credit_scores")
        ));
        
        // Supply Chain Logistics Scenario
        DEMO_SCENARIOS.put("logistics", new DemoScenario(
            "logistics",
            "Supply Chain & Logistics Analysis",
            "Logistics operations with shipments, routes, warehouses, and deliveries for GlobalShip logistics",
            Arrays.asList("warehouses", "products", "shipments", "routes", "deliveries")
        ));
    }
    
    public DemoDataGenerator(DatabaseService databaseService, String databaseType) {
        this.databaseService = databaseService;
        String databaseType1 = databaseType != null ? databaseType.toLowerCase() : "unknown";
        logger.info("DemoDataGenerator initialized for database type: {}", databaseType1);
    }
    
    /**
     * Gets all available demo scenarios
     */
    public static Set<String> getAvailableScenarios() {
        return DEMO_SCENARIOS.keySet();
    }
    
    /**
     * Gets scenario information
     */
    public static DemoScenario getScenarioInfo(String scenarioName) {
        return DEMO_SCENARIOS.get(scenarioName.toLowerCase());
    }
    
    /**
     * Sets up complete demo scenario with all tables and data
     */
    public boolean setupDemoScenario(String scenarioName) {
        String normalizedScenario = scenarioName.toLowerCase();
        DemoScenario scenario = DEMO_SCENARIOS.get(normalizedScenario);
        
        if (scenario == null) {
            logger.warn("Unknown demo scenario: {}", scenarioName);
            return false;
        }
        
        logger.info("Setting up demo scenario: {} - {}", scenario.name, scenario.description);
        
        try {
            // Clean up existing demo tables first
            cleanupDemoTables(scenario);
            
            // Create tables and populate data based on scenario
            switch (normalizedScenario) {
                case "retail" -> setupRetailScenario();
                case "finance" -> setupFinanceScenario(); 
                case "logistics" -> setupLogisticsScenario();
                default -> {
                    logger.error("Implementation missing for scenario: {}", scenarioName);
                    return false;
                }
            }
            
            logger.info("Successfully set up demo scenario: {}", scenario.name);
            return true;
            
        } catch (Exception e) {
            logSetupFailure(scenarioName, e);
            return false;
        }
    }
    
    /**
     * Cleans up all demo tables for a scenario
     */
    public boolean cleanupDemoScenario(String scenarioName) {
        String normalizedScenario = scenarioName.toLowerCase();
        DemoScenario scenario = DEMO_SCENARIOS.get(normalizedScenario);
        
        if (scenario == null) {
            logger.warn("Unknown demo scenario for cleanup: {}", scenarioName);
            return false;
        }
        
        try {
            cleanupDemoTables(scenario);
            logger.info("Successfully cleaned up demo scenario: {}", scenario.name);
            return true;
        } catch (Exception e) {
            logCleanupFailure(scenarioName, e);
            return false;
        }
    }
    
    /**
     * Checks if demo scenario tables exist
     */
    public boolean isDemoScenarioActive(String scenarioName) {
        String normalizedScenario = scenarioName.toLowerCase();
        DemoScenario scenario = DEMO_SCENARIOS.get(normalizedScenario);
        
        if (scenario == null) {
            return false;
        }
        
        try (Connection conn = getRequiredConnection()) {
            // Check if first table in scenario exists
            String firstTable = scenario.tables.get(0);
            String checkQuery = "SELECT COUNT(*) FROM " + firstTable + " LIMIT 1";
            
            try (Statement stmt = conn.createStatement()) {
                stmt.executeQuery(checkQuery);
                return true; // If query succeeds, table exists
            }
        } catch (SQLException e) {
            return false; // Table doesn't exist or is inaccessible
        }
    }
    
    // Private implementation methods
    
    private void cleanupDemoTables(DemoScenario scenario) throws SQLException {
        try (Connection conn = getRequiredConnection()) {
            // Drop tables in reverse order to handle foreign key constraints
            List<String> reverseTables = new ArrayList<>(scenario.tables);
            Collections.reverse(reverseTables);
            
            for (String tableName : reverseTables) {
                try (Statement stmt = conn.createStatement()) {
                    String dropSql = "DROP TABLE IF EXISTS " + tableName;
                    stmt.executeUpdate(dropSql);
                    logger.debug("Dropped demo table: {}", tableName);
                } catch (SQLException e) {
                    logger.debug("Could not drop table {} (may not exist): {}", tableName, e.getMessage());
                }
            }
        }
    }
    
    private void setupRetailScenario() throws SQLException {
        try (Connection conn = getRequiredConnection()) {
            // Create customers table
            executeUpdate(conn, """
                CREATE TABLE customers (
                    customer_id INTEGER PRIMARY KEY,
                    first_name VARCHAR(50) NOT NULL,
                    last_name VARCHAR(50) NOT NULL,
                    email VARCHAR(100) UNIQUE NOT NULL,
                    phone VARCHAR(20),
                    registration_date DATE NOT NULL,
                    customer_tier VARCHAR(20) DEFAULT 'Bronze',
                    total_spent DECIMAL(10,2) DEFAULT 0.00
                )
                """);
            
            // Create products table
            executeUpdate(conn, """
                CREATE TABLE products (
                    product_id INTEGER PRIMARY KEY,
                    product_name VARCHAR(100) NOT NULL,
                    category VARCHAR(50) NOT NULL,
                    brand VARCHAR(50) NOT NULL,
                    price DECIMAL(10,2) NOT NULL,
                    cost DECIMAL(10,2) NOT NULL,
                    description TEXT,
                    in_stock BOOLEAN DEFAULT true
                )
                """);
            
            // Create orders table
            executeUpdate(conn, """
                CREATE TABLE orders (
                    order_id INTEGER PRIMARY KEY,
                    customer_id INTEGER NOT NULL,
                    order_date DATE NOT NULL,
                    status VARCHAR(20) DEFAULT 'pending',
                    total_amount DECIMAL(10,2) NOT NULL,
                    shipping_address VARCHAR(200),
                    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
                )
                """);
            
            // Create order_items table
            executeUpdate(conn, """
                CREATE TABLE order_items (
                    item_id INTEGER PRIMARY KEY,
                    order_id INTEGER NOT NULL,
                    product_id INTEGER NOT NULL,
                    quantity INTEGER NOT NULL,
                    unit_price DECIMAL(10,2) NOT NULL,
                    total_price DECIMAL(10,2) NOT NULL,
                    FOREIGN KEY (order_id) REFERENCES orders(order_id),
                    FOREIGN KEY (product_id) REFERENCES products(product_id)
                )
                """);
            
            // Create inventory table
            executeUpdate(conn, """
                CREATE TABLE inventory (
                    inventory_id INTEGER PRIMARY KEY,
                    product_id INTEGER NOT NULL,
                    warehouse_location VARCHAR(50) NOT NULL,
                    quantity_on_hand INTEGER NOT NULL,
                    reorder_level INTEGER DEFAULT 10,
                    last_updated DATE NOT NULL,
                    FOREIGN KEY (product_id) REFERENCES products(product_id)
                )
                """);
            
            // Populate with realistic data
            populateRetailData(conn);
        }
    }
    
    private void setupFinanceScenario() throws SQLException {
        try (Connection conn = getRequiredConnection()) {
            // Create customers table
            executeUpdate(conn, """
                CREATE TABLE customers (
                    customer_id INTEGER PRIMARY KEY,
                    first_name VARCHAR(50) NOT NULL,
                    last_name VARCHAR(50) NOT NULL,
                    email VARCHAR(100) UNIQUE NOT NULL,
                    phone VARCHAR(20),
                    date_of_birth DATE,
                    ssn_last_four VARCHAR(4),
                    customer_since DATE NOT NULL,
                    risk_profile VARCHAR(20) DEFAULT 'Medium'
                )
                """);
            
            // Create accounts table
            executeUpdate(conn, """
                CREATE TABLE accounts (
                    account_id INTEGER PRIMARY KEY,
                    customer_id INTEGER NOT NULL,
                    account_number VARCHAR(20) UNIQUE NOT NULL,
                    account_type VARCHAR(30) NOT NULL,
                    balance DECIMAL(15,2) NOT NULL,
                    opened_date DATE NOT NULL,
                    status VARCHAR(20) DEFAULT 'Active',
                    interest_rate DECIMAL(5,4) DEFAULT 0.0000,
                    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
                )
                """);
            
            // Create transactions table
            executeUpdate(conn, """
                CREATE TABLE transactions (
                    transaction_id INTEGER PRIMARY KEY,
                    account_id INTEGER NOT NULL,
                    transaction_date DATE NOT NULL,
                    transaction_type VARCHAR(30) NOT NULL,
                    amount DECIMAL(15,2) NOT NULL,
                    description VARCHAR(200),
                    merchant_category VARCHAR(50),
                    balance_after DECIMAL(15,2) NOT NULL,
                    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
                )
                """);
            
            // Create loans table
            executeUpdate(conn, """
                CREATE TABLE loans (
                    loan_id INTEGER PRIMARY KEY,
                    customer_id INTEGER NOT NULL,
                    loan_type VARCHAR(30) NOT NULL,
                    principal_amount DECIMAL(15,2) NOT NULL,
                    outstanding_balance DECIMAL(15,2) NOT NULL,
                    interest_rate DECIMAL(5,4) NOT NULL,
                    term_months INTEGER NOT NULL,
                    monthly_payment DECIMAL(10,2) NOT NULL,
                    origination_date DATE NOT NULL,
                    status VARCHAR(20) DEFAULT 'Current',
                    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
                )
                """);
            
            // Create credit_scores table
            executeUpdate(conn, """
                CREATE TABLE credit_scores (
                    score_id INTEGER PRIMARY KEY,
                    customer_id INTEGER NOT NULL,
                    credit_score INTEGER NOT NULL,
                    score_date DATE NOT NULL,
                    reporting_agency VARCHAR(30) NOT NULL,
                    score_factors VARCHAR(200),
                    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
                )
                """);
            
            populateFinanceData(conn);
        }
    }
    
    private void setupLogisticsScenario() throws SQLException {
        try (Connection conn = getRequiredConnection()) {
            // Create warehouses table
            executeUpdate(conn, """
                CREATE TABLE warehouses (
                    warehouse_id INTEGER PRIMARY KEY,
                    warehouse_name VARCHAR(100) NOT NULL,
                    location VARCHAR(100) NOT NULL,
                    capacity INTEGER NOT NULL,
                    current_utilization DECIMAL(5,2) DEFAULT 0.00,
                    manager_name VARCHAR(100)
                )
                """);
            
            // Create products table (logistics version)
            executeUpdate(conn, """
                CREATE TABLE products (
                    product_id INTEGER PRIMARY KEY,
                    product_code VARCHAR(50) UNIQUE NOT NULL,
                    product_name VARCHAR(100) NOT NULL,
                    category VARCHAR(50) NOT NULL,
                    weight_kg DECIMAL(8,2) NOT NULL,
                    dimensions VARCHAR(50),
                    fragile BOOLEAN DEFAULT false,
                    hazardous BOOLEAN DEFAULT false
                )
                """);
            
            // Create shipments table
            executeUpdate(conn, """
                CREATE TABLE shipments (
                    shipment_id INTEGER PRIMARY KEY,
                    origin_warehouse INTEGER NOT NULL,
                    destination VARCHAR(100) NOT NULL,
                    shipment_date DATE NOT NULL,
                    expected_delivery DATE NOT NULL,
                    status VARCHAR(30) DEFAULT 'in_transit',
                    total_weight DECIMAL(10,2) NOT NULL,
                    carrier VARCHAR(50),
                    tracking_number VARCHAR(100),
                    FOREIGN KEY (origin_warehouse) REFERENCES warehouses(warehouse_id)
                )
                """);
            
            // Create routes table
            executeUpdate(conn, """
                CREATE TABLE routes (
                    route_id INTEGER PRIMARY KEY,
                    route_name VARCHAR(100) NOT NULL,
                    origin VARCHAR(100) NOT NULL,
                    destination VARCHAR(100) NOT NULL,
                    distance_km INTEGER NOT NULL,
                    estimated_hours DECIMAL(4,2) NOT NULL,
                    toll_cost DECIMAL(8,2) DEFAULT 0.00,
                    preferred_carrier VARCHAR(50)
                )
                """);
            
            // Create deliveries table
            executeUpdate(conn, """
                CREATE TABLE deliveries (
                    delivery_id INTEGER PRIMARY KEY,
                    shipment_id INTEGER NOT NULL,
                    delivery_date DATE,
                    delivery_status VARCHAR(30) DEFAULT 'scheduled',
                    recipient_name VARCHAR(100),
                    delivery_notes TEXT,
                    signature_required BOOLEAN DEFAULT false,
                    delivered_by VARCHAR(100),
                    FOREIGN KEY (shipment_id) REFERENCES shipments(shipment_id)
                )
                """);
            
            populateLogisticsData(conn);
        }
    }
    
    private void populateRetailData(Connection conn) throws SQLException {
        // Sample customer data
        String[] firstNames = {"John", "Sarah", "Michael", "Emma", "David", "Lisa", "James", "Anna", "Robert", "Maria", "William", "Jennifer", "Christopher", "Michelle", "Daniel", "Amanda"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas"};
        String[] tiers = {"Bronze", "Silver", "Gold", "Platinum"};
        
        // Insert customers
        String customerSql = "INSERT INTO customers (customer_id, first_name, last_name, email, phone, registration_date, customer_tier, total_spent) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(customerSql)) {
            for (int i = 1; i <= 15; i++) {
                String firstName = firstNames[random.nextInt(firstNames.length)];
                String lastName = lastNames[random.nextInt(lastNames.length)];
                stmt.setInt(1, i);
                stmt.setString(2, firstName);
                stmt.setString(3, lastName);
                stmt.setString(4, firstName.toLowerCase() + "." + lastName.toLowerCase() + "@email.com");
                stmt.setString(5, String.format("555-0%03d", 100 + i));
                stmt.setString(6, LocalDate.now().minusDays(random.nextInt(365)).toString());
                stmt.setString(7, tiers[random.nextInt(tiers.length)]);
                stmt.setBigDecimal(8, java.math.BigDecimal.valueOf(random.nextDouble() * 5000 + 100).setScale(2, java.math.RoundingMode.HALF_UP));
                stmt.executeUpdate();
            }
        }
        
        // Sample product data
        String[][] products = {
            {"1", "iPhone 15 Pro", "Electronics", "Apple", "999.00", "750.00", "Latest iPhone with advanced camera system", "true"},
            {"2", "Samsung Galaxy S24", "Electronics", "Samsung", "849.00", "650.00", "Premium Android smartphone with AI features", "true"},
            {"3", "MacBook Air M3", "Computers", "Apple", "1299.00", "950.00", "Lightweight laptop with M3 chip", "true"},
            {"4", "Dell XPS 13", "Computers", "Dell", "1199.00", "900.00", "Ultra-portable Windows laptop", "true"},
            {"5", "Sony WH-1000XM5", "Audio", "Sony", "399.00", "280.00", "Noise-canceling wireless headphones", "true"},
            {"6", "AirPods Pro", "Audio", "Apple", "249.00", "180.00", "Premium wireless earbuds with ANC", "true"},
            {"7", "iPad Pro 12.9\"", "Tablets", "Apple", "1099.00", "800.00", "Professional tablet for creative work", "true"},
            {"8", "Surface Pro 9", "Tablets", "Microsoft", "999.00", "750.00", "2-in-1 tablet and laptop", "true"},
            {"9", "Nintendo Switch", "Gaming", "Nintendo", "299.00", "220.00", "Portable gaming console", "true"},
            {"10", "PlayStation 5", "Gaming", "Sony", "499.00", "400.00", "Next-gen gaming console", "false"},
            {"11", "Logitech MX Master 3", "Accessories", "Logitech", "99.00", "65.00", "Professional wireless mouse", "true"},
            {"12", "Mechanical Keyboard", "Accessories", "Keychron", "149.00", "95.00", "Premium mechanical keyboard", "true"}
        };
        
        String productSql = "INSERT INTO products (product_id, product_name, category, brand, price, cost, description, in_stock) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(productSql)) {
            for (String[] product : products) {
                stmt.setInt(1, Integer.parseInt(product[0]));
                stmt.setString(2, product[1]);
                stmt.setString(3, product[2]);
                stmt.setString(4, product[3]);
                stmt.setBigDecimal(5, new java.math.BigDecimal(product[4]));
                stmt.setBigDecimal(6, new java.math.BigDecimal(product[5]));
                stmt.setString(7, product[6]);
                stmt.setBoolean(8, Boolean.parseBoolean(product[7]));
                stmt.executeUpdate();
            }
        }
        
        // Generate orders and order items
        generateRetailOrdersAndItems(conn);
        
        // Generate inventory data
        generateInventoryData(conn);
    }
    
    private void populateFinanceData(Connection conn) throws SQLException {
        // Sample customer data for finance
        String[] firstNames = {"John", "Sarah", "Michael", "Emma", "David", "Lisa", "James", "Anna", "Robert", "Maria"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"};
        String[] riskProfiles = {"Low", "Medium", "High"};
        
        // Insert customers
        String customerSql = "INSERT INTO customers (customer_id, first_name, last_name, email, phone, date_of_birth, ssn_last_four, customer_since, risk_profile) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(customerSql)) {
            for (int i = 1; i <= 12; i++) {
                String firstName = firstNames[random.nextInt(firstNames.length)];
                String lastName = lastNames[random.nextInt(lastNames.length)];
                stmt.setInt(1, i);
                stmt.setString(2, firstName);
                stmt.setString(3, lastName);
                stmt.setString(4, firstName.toLowerCase() + "." + lastName.toLowerCase() + "@email.com");
                stmt.setString(5, String.format("555-0%03d", 200 + i));
                stmt.setString(6, LocalDate.now().minusYears(random.nextInt(40) + 18).toString());
                stmt.setString(7, String.format("%04d", random.nextInt(10000)));
                stmt.setString(8, LocalDate.now().minusYears(random.nextInt(10) + 1).toString());
                stmt.setString(9, riskProfiles[random.nextInt(riskProfiles.length)]);
                stmt.executeUpdate();
            }
        }
        
        generateFinanceAccountsAndTransactions(conn);
        generateLoansAndCreditScores(conn);
    }
    
    private void populateLogisticsData(Connection conn) throws SQLException {
        // Sample warehouse data
        String[][] warehouses = {
            {"1", "Central Distribution Hub", "Los Angeles, CA", "50000", "75.50", "Sarah Johnson"},
            {"2", "East Coast Facility", "Atlanta, GA", "35000", "82.30", "Michael Brown"},
            {"3", "West Coast Hub", "Seattle, WA", "40000", "68.75", "Jennifer Davis"},
            {"4", "Midwest Center", "Chicago, IL", "45000", "71.20", "David Wilson"}
        };
        
        String warehouseSql = "INSERT INTO warehouses (warehouse_id, warehouse_name, location, capacity, current_utilization, manager_name) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(warehouseSql)) {
            for (String[] warehouse : warehouses) {
                stmt.setInt(1, Integer.parseInt(warehouse[0]));
                stmt.setString(2, warehouse[1]);
                stmt.setString(3, warehouse[2]);
                stmt.setInt(4, Integer.parseInt(warehouse[3]));
                stmt.setBigDecimal(5, new java.math.BigDecimal(warehouse[4]));
                stmt.setString(6, warehouse[5]);
                stmt.executeUpdate();
            }
        }
        
        generateLogisticsProductsAndShipments(conn);
        generateRoutesAndDeliveries(conn);
    }
    
    // Helper methods for generating related data
    private void generateRetailOrdersAndItems(Connection conn) throws SQLException {
        String[] statuses = {"pending", "processing", "shipped", "delivered", "cancelled"};
        String[] addresses = {
            "123 Main St, Anytown, CA 90210",
            "456 Oak Ave, Springfield, IL 62701", 
            "789 Pine Rd, Austin, TX 73301",
            "321 Elm St, Denver, CO 80202",
            "654 Maple Dr, Portland, OR 97201"
        };
        
        String orderSql = "INSERT INTO orders (order_id, customer_id, order_date, status, total_amount, shipping_address) VALUES (?, ?, ?, ?, ?, ?)";
        String itemSql = "INSERT INTO order_items (item_id, order_id, product_id, quantity, unit_price, total_price) VALUES (?, ?, ?, ?, ?, ?)";
        
        int itemCounter = 1;
        
        try (PreparedStatement orderStmt = conn.prepareStatement(orderSql);
             PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
            
            for (int orderId = 1; orderId <= 20; orderId++) {
                int customerId = random.nextInt(15) + 1;
                String status = statuses[random.nextInt(statuses.length)];
                LocalDate orderDate = LocalDate.now().minusDays(random.nextInt(90));
                String address = addresses[random.nextInt(addresses.length)];
                
                // Generate 1-4 items per order
                int itemCount = random.nextInt(4) + 1;
                double orderTotal = 0.0;
                
                for (int i = 0; i < itemCount; i++) {
                    int productId = random.nextInt(12) + 1;
                    int quantity = random.nextInt(3) + 1;
                    double unitPrice = 99.0 + random.nextDouble() * 1200; // Price range $99-$1299
                    double totalPrice = unitPrice * quantity;
                    orderTotal += totalPrice;
                    
                    itemStmt.setInt(1, itemCounter++);
                    itemStmt.setInt(2, orderId);
                    itemStmt.setInt(3, productId);
                    itemStmt.setInt(4, quantity);
                    itemStmt.setBigDecimal(5, java.math.BigDecimal.valueOf(unitPrice).setScale(2, java.math.RoundingMode.HALF_UP));
                    itemStmt.setBigDecimal(6, java.math.BigDecimal.valueOf(totalPrice).setScale(2, java.math.RoundingMode.HALF_UP));
                    itemStmt.executeUpdate();
                }
                
                orderStmt.setInt(1, orderId);
                orderStmt.setInt(2, customerId);
                orderStmt.setString(3, orderDate.toString());
                orderStmt.setString(4, status);
                orderStmt.setBigDecimal(5, java.math.BigDecimal.valueOf(orderTotal).setScale(2, java.math.RoundingMode.HALF_UP));
                orderStmt.setString(6, address);
                orderStmt.executeUpdate();
            }
        }
    }
    
    private void generateInventoryData(Connection conn) throws SQLException {
        String[] locations = {"Main Warehouse", "Overflow Storage", "Distribution Center", "Retail Store"};
        
        String inventorySql = "INSERT INTO inventory (inventory_id, product_id, warehouse_location, quantity_on_hand, reorder_level, last_updated) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(inventorySql)) {
            int inventoryId = 1;
            for (int productId = 1; productId <= 12; productId++) {
                // Create 1-2 inventory records per product
                int locationCount = random.nextInt(2) + 1;
                for (int i = 0; i < locationCount; i++) {
                    stmt.setInt(1, inventoryId++);
                    stmt.setInt(2, productId);
                    stmt.setString(3, locations[random.nextInt(locations.length)]);
                    stmt.setInt(4, random.nextInt(100) + 5); // 5-104 units
                    stmt.setInt(5, random.nextInt(20) + 5); // 5-24 reorder level
                    stmt.setString(6, LocalDate.now().minusDays(random.nextInt(30)).toString());
                    stmt.executeUpdate();
                }
            }
        }
    }
    
    // Additional helper methods for finance and logistics data generation
    private void generateFinanceAccountsAndTransactions(Connection conn) throws SQLException {
        String[] accountTypes = {"Checking", "Savings", "Money Market", "Certificate of Deposit"};
        String[] transactionTypes = {"Deposit", "Withdrawal", "Transfer", "Fee", "Interest"};
        String[] merchantCategories = {"Grocery", "Gas", "Restaurant", "Online", "ATM", "Transfer"};
        
        // Generate accounts
        String accountSql = "INSERT INTO accounts (account_id, customer_id, account_number, account_type, balance, opened_date, status, interest_rate) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(accountSql)) {
            for (int i = 1; i <= 18; i++) {
                int customerId = random.nextInt(12) + 1;
                String accountType = accountTypes[random.nextInt(accountTypes.length)];
                double interestRate = accountType.equals("Savings") ? random.nextDouble() * 0.05 : 0.0;
                
                stmt.setInt(1, i);
                stmt.setInt(2, customerId);
                stmt.setString(3, String.format("ACC%08d", 10000000 + i));
                stmt.setString(4, accountType);
                stmt.setBigDecimal(5, java.math.BigDecimal.valueOf(random.nextDouble() * 50000 + 1000).setScale(2, java.math.RoundingMode.HALF_UP));
                stmt.setString(6, LocalDate.now().minusYears(random.nextInt(5) + 1).toString());
                stmt.setString(7, "Active");
                stmt.setBigDecimal(8, java.math.BigDecimal.valueOf(interestRate).setScale(4, java.math.RoundingMode.HALF_UP));
                stmt.executeUpdate();
            }
        }
        
        // Generate transactions
        String transactionSql = "INSERT INTO transactions (transaction_id, account_id, transaction_date, transaction_type, amount, description, merchant_category, balance_after) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(transactionSql)) {
            for (int i = 1; i <= 50; i++) {
                int accountId = random.nextInt(18) + 1;
                String transactionType = transactionTypes[random.nextInt(transactionTypes.length)];
                double amount = (random.nextDouble() * 2000 + 10) * (transactionType.equals("Withdrawal") || transactionType.equals("Fee") ? -1 : 1);
                String category = merchantCategories[random.nextInt(merchantCategories.length)];
                
                stmt.setInt(1, i);
                stmt.setInt(2, accountId);
                stmt.setString(3, LocalDate.now().minusDays(random.nextInt(60)).toString());
                stmt.setString(4, transactionType);
                stmt.setBigDecimal(5, java.math.BigDecimal.valueOf(amount).setScale(2, java.math.RoundingMode.HALF_UP));
                stmt.setString(6, String.format("%s transaction - %s", transactionType, category));
                stmt.setString(7, category);
                stmt.setBigDecimal(8, java.math.BigDecimal.valueOf(random.nextDouble() * 40000 + 500).setScale(2, java.math.RoundingMode.HALF_UP));
                stmt.executeUpdate();
            }
        }
    }
    
    private void generateLoansAndCreditScores(Connection conn) throws SQLException {
        String[] loanTypes = {"Personal", "Auto", "Mortgage", "Student", "Home Equity"};
        String[] loanStatuses = {"Current", "Late", "Paid Off", "In Default"};
        String[] agencies = {"Experian", "Equifax", "TransUnion"};
        
        // Generate loans
        String loanSql = "INSERT INTO loans (loan_id, customer_id, loan_type, principal_amount, outstanding_balance, interest_rate, term_months, monthly_payment, origination_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(loanSql)) {
            for (int i = 1; i <= 15; i++) {
                int customerId = random.nextInt(12) + 1;
                String loanType = loanTypes[random.nextInt(loanTypes.length)];
                double principal = random.nextDouble() * 200000 + 5000;
                double outstandingBalance = principal * (0.2 + random.nextDouble() * 0.8); // 20-100% of principal
                double interestRate = random.nextDouble() * 0.12 + 0.03; // 3-15%
                int termMonths = (random.nextInt(20) + 1) * 12; // 12-240 months
                double monthlyPayment = outstandingBalance / termMonths * 1.1; // Approximate
                
                stmt.setInt(1, i);
                stmt.setInt(2, customerId);
                stmt.setString(3, loanType);
                stmt.setBigDecimal(4, java.math.BigDecimal.valueOf(principal).setScale(2, java.math.RoundingMode.HALF_UP));
                stmt.setBigDecimal(5, java.math.BigDecimal.valueOf(outstandingBalance).setScale(2, java.math.RoundingMode.HALF_UP));
                stmt.setBigDecimal(6, java.math.BigDecimal.valueOf(interestRate).setScale(4, java.math.RoundingMode.HALF_UP));
                stmt.setInt(7, termMonths);
                stmt.setBigDecimal(8, java.math.BigDecimal.valueOf(monthlyPayment).setScale(2, java.math.RoundingMode.HALF_UP));
                stmt.setString(9, LocalDate.now().minusYears(random.nextInt(5) + 1).toString());
                stmt.setString(10, loanStatuses[random.nextInt(loanStatuses.length)]);
                stmt.executeUpdate();
            }
        }
        
        // Generate credit scores
        String scoreSql = "INSERT INTO credit_scores (score_id, customer_id, credit_score, score_date, reporting_agency, score_factors) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(scoreSql)) {
            for (int i = 1; i <= 20; i++) {
                int customerId = random.nextInt(12) + 1;
                int creditScore = random.nextInt(350) + 300; // 300-850 range
                String agency = agencies[random.nextInt(agencies.length)];
                
                stmt.setInt(1, i);
                stmt.setInt(2, customerId);
                stmt.setInt(3, creditScore);
                stmt.setString(4, LocalDate.now().minusMonths(random.nextInt(24)).toString());
                stmt.setString(5, agency);
                stmt.setString(6, "Payment history, Credit utilization, Length of credit history");
                stmt.executeUpdate();
            }
        }
    }
    
    private void generateLogisticsProductsAndShipments(Connection conn) throws SQLException {
        // Generate logistics products
        String[][] logisticsProducts = {
            {"1", "ELEC001", "Laptop Computers", "Electronics", "2.5", "35x25x3 cm", "false", "false"},
            {"2", "FURN002", "Office Chairs", "Furniture", "15.0", "80x60x120 cm", "false", "false"},
            {"3", "CHEM003", "Industrial Cleaners", "Chemicals", "5.0", "30x20x25 cm", "false", "true"},
            {"4", "FRAG004", "Glass Displays", "Electronics", "8.0", "50x30x5 cm", "true", "false"},
            {"5", "BULK005", "Paper Supplies", "Office", "20.0", "40x30x30 cm", "false", "false"},
            {"6", "AUTO006", "Car Parts", "Automotive", "12.0", "Various", "false", "false"}
        };
        
        String productSql = "INSERT INTO products (product_id, product_code, product_name, category, weight_kg, dimensions, fragile, hazardous) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(productSql)) {
            for (String[] product : logisticsProducts) {
                stmt.setInt(1, Integer.parseInt(product[0]));
                stmt.setString(2, product[1]);
                stmt.setString(3, product[2]);
                stmt.setString(4, product[3]);
                stmt.setBigDecimal(5, new java.math.BigDecimal(product[4]));
                stmt.setString(6, product[5]);
                stmt.setBoolean(7, Boolean.parseBoolean(product[6]));
                stmt.setBoolean(8, Boolean.parseBoolean(product[7]));
                stmt.executeUpdate();
            }
        }
        
        // Generate shipments
        String[] destinations = {"New York, NY", "Miami, FL", "Houston, TX", "Denver, CO", "Phoenix, AZ", "Portland, OR"};
        String[] statuses = {"scheduled", "in_transit", "delivered", "delayed"};
        String[] carriers = {"FedEx", "UPS", "DHL", "USPS"};
        
        String shipmentSql = "INSERT INTO shipments (shipment_id, origin_warehouse, destination, shipment_date, expected_delivery, status, total_weight, carrier, tracking_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(shipmentSql)) {
            for (int i = 1; i <= 25; i++) {
                int originWarehouse = random.nextInt(4) + 1;
                String destination = destinations[random.nextInt(destinations.length)];
                LocalDate shipmentDate = LocalDate.now().minusDays(random.nextInt(30));
                LocalDate expectedDelivery = shipmentDate.plusDays(random.nextInt(7) + 1);
                String status = statuses[random.nextInt(statuses.length)];
                String carrier = carriers[random.nextInt(carriers.length)];
                
                stmt.setInt(1, i);
                stmt.setInt(2, originWarehouse);
                stmt.setString(3, destination);
                stmt.setString(4, shipmentDate.toString());
                stmt.setString(5, expectedDelivery.toString());
                stmt.setString(6, status);
                stmt.setBigDecimal(7, java.math.BigDecimal.valueOf(random.nextDouble() * 500 + 10).setScale(2, java.math.RoundingMode.HALF_UP));
                stmt.setString(8, carrier);
                stmt.setString(9, String.format("%s%09d", carrier.substring(0, 2).toUpperCase(), random.nextInt(1000000000)));
                stmt.executeUpdate();
            }
        }
    }
    
    private void generateRoutesAndDeliveries(Connection conn) throws SQLException {
        // Generate routes
        String[][] routeData = {
            {"1", "LA to Vegas Route", "Los Angeles, CA", "Las Vegas, NV", "270", "4.5", "15.50", "TruckCorp"},
            {"2", "Atlanta to Miami Express", "Atlanta, GA", "Miami, FL", "650", "10.0", "45.00", "FastShip"},
            {"3", "Seattle to Portland", "Seattle, WA", "Portland, OR", "280", "4.0", "12.00", "QuickMove"},
            {"4", "Chicago Hub Circuit", "Chicago, IL", "Milwaukee, WI", "150", "2.5", "8.00", "LocalExpress"}
        };
        
        String routeSql = "INSERT INTO routes (route_id, route_name, origin, destination, distance_km, estimated_hours, toll_cost, preferred_carrier) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(routeSql)) {
            for (String[] route : routeData) {
                stmt.setInt(1, Integer.parseInt(route[0]));
                stmt.setString(2, route[1]);
                stmt.setString(3, route[2]);
                stmt.setString(4, route[3]);
                stmt.setInt(5, Integer.parseInt(route[4]));
                stmt.setBigDecimal(6, new java.math.BigDecimal(route[5]));
                stmt.setBigDecimal(7, new java.math.BigDecimal(route[6]));
                stmt.setString(8, route[7]);
                stmt.executeUpdate();
            }
        }
        
        // Generate deliveries
        String[] deliveryStatuses = {"scheduled", "in_progress", "delivered", "failed", "rescheduled"};
        String[] deliveryPersons = {"Mike Johnson", "Sarah Davis", "Alex Rodriguez", "Lisa Chen", "James Wilson"};
        
        String deliverySql = "INSERT INTO deliveries (delivery_id, shipment_id, delivery_date, delivery_status, recipient_name, delivery_notes, signature_required, delivered_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(deliverySql)) {
            for (int i = 1; i <= 20; i++) {
                int shipmentId = random.nextInt(25) + 1;
                String status = deliveryStatuses[random.nextInt(deliveryStatuses.length)];
                LocalDate deliveryDate = status.equals("delivered") ? LocalDate.now().minusDays(random.nextInt(15)) : null;
                String deliveryPerson = deliveryPersons[random.nextInt(deliveryPersons.length)];
                
                stmt.setInt(1, i);
                stmt.setInt(2, shipmentId);
                stmt.setString(3, deliveryDate != null ? deliveryDate.toString() : null);
                stmt.setString(4, status);
                stmt.setString(5, "Customer " + i);
                stmt.setString(6, status.equals("failed") ? "Address not found" : "Standard delivery");
                stmt.setBoolean(7, random.nextBoolean());
                stmt.setString(8, deliveryPerson);
                stmt.executeUpdate();
            }
        }
    }
    
    private void executeUpdate(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private Connection getRequiredConnection() throws SQLException {
        Connection conn = databaseService.getConnection();
        if (conn == null) {
            throw new SQLException("Database connection is null");
        }
        return conn;
    }

    private void logCleanupFailure(String scenarioName, Exception e) {
        if (isNullConnectionException(e)) {
            logger.warn("Cleanup failed for scenario: {} (no database connection)", scenarioName);
            return;
        }
        logger.error("Failed to cleanup demo scenario: {}", scenarioName, e);
    }

    private void logSetupFailure(String scenarioName, Exception e) {
        if (isNullConnectionException(e)) {
            logger.warn("Setup failed for scenario: {} (no database connection)", scenarioName);
            return;
        }
        logger.error("Failed to setup demo scenario: {}", scenarioName, e);
    }

    private boolean isNullConnectionException(Exception e) {
        if (!(e instanceof SQLException)) {
            return false;
        }
        String message = e.getMessage();
        return message != null && message.equals("Database connection is null");
    }
    
    /**
     * Demo scenario definition class
     */
    public static class DemoScenario {
        public final String name;
        public final String displayName;
        public final String description;
        public final List<String> tables;
        
        public DemoScenario(String name, String displayName, String description, List<String> tables) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.tables = new ArrayList<>(tables);
        }
        
        @Override
        public String toString() {
            return String.format("%s: %s (%d tables)", displayName, description, tables.size());
        }
    }
}
