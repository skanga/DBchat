package com.skanga.mcp.config;

import com.skanga.mcp.McpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.*;

/**
 * Utility class for handling command line interface operations.
 * Provides argument parsing, help display, version information, and JDBC driver detection.
 */
public class CliUtils {
    private static final Logger logger = LoggerFactory.getLogger(CliUtils.class);
    public static final String SERVER_NAME = "DBChat";
    public static final String SERVER_VERSION = "4.1.0";
    public static final String SERVER_DESCRIPTION = "Secure MCP server for database operations";

    static String DEFAULT_DB_URL = "jdbc:h2:mem:test";
    static String DEFAULT_DB_USER = "sa";
    static String DEFAULT_DB_PASSWORD = "";
    static String DEFAULT_DB_DRIVER = "org.h2.Driver";

    /**
     * Maps short form arguments to their long form equivalents.
     *
     * @return Map of short form to long form argument names
     */
    static Map<String, String> getShortFormMapping() {
        Map<String, String> shortToLong = new HashMap<>();

        // Help and version
        shortToLong.put("h", "help");
        shortToLong.put("v", "version");

        // Server mode and connection
        shortToLong.put("c", "config_file");
        shortToLong.put("m", "http_mode");
        shortToLong.put("b", "bind_address");
        shortToLong.put("p", "http_port");

        // Database connection
        shortToLong.put("u", "db_url");
        shortToLong.put("U", "db_user");
        shortToLong.put("P", "db_password");
        shortToLong.put("d", "db_driver");

        // Connection pool settings
        shortToLong.put("C", "max_connections");
        shortToLong.put("t", "connection_timeout_ms");
        shortToLong.put("i", "idle_timeout_ms");
        shortToLong.put("l", "max_lifetime_ms");
        shortToLong.put("L", "leak_detection_threshold_ms");

        // Query settings
        shortToLong.put("q", "query_timeout_seconds");
        shortToLong.put("s", "select_only");
        shortToLong.put("M", "max_sql");
        shortToLong.put("r", "max_rows_limit");

        return shortToLong;
    }

    /**
     * Parses command line arguments into a key-value map.
     * Supports both short form (-h) and long form (--help) arguments.
     * Handles both key=value and key value formats for both forms.
     * Converts keys to uppercase for consistent lookup.
     *
     * @param args Command line arguments array
     * @return Map of uppercase keys to values
     */
    public static Map<String, String> parseArgs(String[] args) {
        Map<String, String> argsMap = new HashMap<>();
        Map<String, String> shortToLong = getShortFormMapping();

        for (int i = 0; i < args.length; i++) {
            String currArg = args[i];
            String argKey = null;
            String argValue = null;

            if (currArg.startsWith("--")) {
                // Long form: --key=value or --key value or --key (for flags)
                String argWithoutPrefix = currArg.substring(2);

                if (argWithoutPrefix.contains("=")) {
                    // Format: --key=value
                    String[] argParts = argWithoutPrefix.split("=", 2);
                    argKey = argParts[0];
                    argValue = argParts[1];
                } else {
                    // Format: --key value or --key (flag)
                    argKey = argWithoutPrefix;

                    // Check if next argument is a value (doesn't start with -)
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        argValue = args[i + 1];
                        i++; // Skip the next argument since we consumed it as a value
                    } else {
                        argValue = "true"; // Flag without value
                    }
                }
            } else if (currArg.startsWith("-") && currArg.length() > 1) {
                // Short form: -k=value or -k value or -k (for flags)
                String shortArg = currArg.substring(1);

                if (shortArg.contains("=")) {
                    // Format: -k=value
                    String[] argParts = shortArg.split("=", 2);
                    String shortForm = argParts[0];
                    argKey = shortToLong.get(shortForm);
                    argValue = argParts[1];
                } else {
                    // Format: -k value or -k (flag)
                    argKey = shortToLong.get(shortArg);

                    // Check if next argument is a value (doesn't start with -)
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        argValue = args[i + 1];
                        i++; // Skip the next argument since we consumed it as a value
                    } else {
                        argValue = "true"; // Flag without value
                    }
                }
            }

            if (argKey != null) {
                // Convert to uppercase for consistent lookup
                argsMap.put(argKey.toUpperCase(), argValue);
            }
        }

        return argsMap;
    }

    /**
     * Checks for help and version arguments and handles them.
     *
     * @param args Command line arguments
     * @return true if help or version was displayed (caller should exit), false otherwise
     */
    public static boolean handleHelpAndVersion(String[] args) {
        for (String arg : args) {
            if ("--help".equals(arg) || "-h".equals(arg)) {
                displayHelp();
                return true;
            }
            if ("--version".equals(arg) || "-v".equals(arg)) {
                displayVersion();
                return true;
            }
        }
        return false;
    }

    /**
     * Displays help information for command line usage.
     */
    static void displayHelp() {
        System.out.println(SERVER_NAME + " v" + SERVER_VERSION);
        System.out.println("Usage: java -jar dbchat-" + SERVER_VERSION + ".jar [OPTIONS]");
        System.out.println();
        System.out.println("ARGUMENT FORMATS:");
        System.out.println("  Both short (-k) and long (--key) forms support:");
        System.out.println("    -k=value  or  --key=value     (no spaces around =)");
        System.out.println("    -k value  or  --key value     (space-separated)");
        System.out.println("    -k        or  --key           (flags, defaults to true)");
        System.out.println();
        System.out.println("OPTIONS:");
        System.out.println("  -h, --help                     Show this help message and exit");
        System.out.println("  -v, --version                  Show version information and exit");
        System.out.println("  -c, --config_file=<path>       Load configuration from file");
        System.out.println("  -m, --http_mode=<true|false>   Run in HTTP mode (default: false, uses stdio)");
        System.out.println("  -b, --bind_address=<address>   HTTP bind address (default: localhost)");
        System.out.println("  -p, --http_port=<port>         HTTP port number (default: 8080)");
        System.out.println();
        System.out.println("DATABASE CONFIGURATION:");
        System.out.println("  -u, --db_url=<url>             Database JDBC URL (default: jdbc:h2:mem:test)");
        System.out.println("  -U, --db_user=<username>       Database username (default: sa)");
        System.out.println("  -P, --db_password=<password>   Database password (default: empty)");
        System.out.println("  -d, --db_driver=<class>        JDBC driver class (default: org.h2.Driver)");
        System.out.println();
        System.out.println("CONNECTION POOL SETTINGS:");
        System.out.println("  -C, --max_connections=<num>    Maximum connections (default: 10)");
        System.out.println("  -t, --connection_timeout_ms=<ms>  Connection timeout (default: 30000)");
        System.out.println("  -i, --idle_timeout_ms=<ms>     Idle timeout (default: 600000)");
        System.out.println("  -l, --max_lifetime_ms=<ms>     Max connection lifetime (default: 1800000)");
        System.out.println("  -L, --leak_detection_threshold_ms=<ms>  Leak detection (default: 20000)");
        System.out.println();
        System.out.println("QUERY SETTINGS:");
        System.out.println("  -q, --query_timeout_seconds=<sec>  Query timeout (default: 30)");
        System.out.println("  -s, --select_only=<true|false>     Allow only SELECT queries (default: true)");
        System.out.println("  -M, --max_sql=<chars>              Max SQL query length (default: 10000)");
        System.out.println("  -r, --max_rows_limit=<num>         Max rows returned (default: 10000)");
        System.out.println();
        System.out.println("EXAMPLES:");
        System.out.println("  # Different argument formats (all equivalent):");
        System.out.println("  java -jar dbchat-" + SERVER_VERSION + ".jar -p=8080");
        System.out.println("  java -jar dbchat-" + SERVER_VERSION + ".jar -p 8080");
        System.out.println("  java -jar dbchat-" + SERVER_VERSION + ".jar --http_port=8080");
        System.out.println("  java -jar dbchat-" + SERVER_VERSION + ".jar --http_port 8080");
        System.out.println();
        System.out.println("  # Mixed formats work together:");
        System.out.println("  java -jar dbchat-" + SERVER_VERSION + ".jar -u jdbc:postgresql://localhost/db --db_user=admin -p=9090");
        System.out.println();
        System.out.println("  # Boolean flags:");
        System.out.println("  java -jar dbchat-" + SERVER_VERSION + ".jar -m          # Same as -m=true");
        System.out.println("  java -jar dbchat-" + SERVER_VERSION + ".jar --http_mode # Same as --http_mode=true");
    }

    /**
     * Displays version information including available JDBC drivers.
     */
    static void displayVersion() {
        System.out.println(SERVER_NAME + " v" + SERVER_VERSION);
        System.out.println(SERVER_DESCRIPTION);
        System.out.println("MCP Protocol Version: " + McpServer.DEFAULT_PROTOCOL_VERSION);
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Java Vendor: " + System.getProperty("java.vendor"));
        System.out.println();
        showJdbcDrivers();
        System.out.println("\nFeatures:");
        System.out.println(" - Model Context Protocol (MCP) compliance");
        System.out.println(" - Security-hardened query execution");
        System.out.println(" - Connection pooling and resource management");
        System.out.println(" - Both stdio and HTTP transport modes");
        System.out.println(" - Configurable access controls and limits");
        System.out.println(" - Comprehensive audit logging");
    }

    /**
     * Displays available JDBC drivers found in the classpath.
     * Uses DriverManager to enumerate registered drivers and attempts to load
     * common JDBC drivers to check availability.
     */
    private static void showJdbcDrivers() {
        System.out.println("Available JDBC Drivers:");

        // Get registered drivers from DriverManager first
        Map<String, String> foundDrivers = new LinkedHashMap<>(); // Preserve order and avoid duplicates
        try {
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                String driverClass = driver.getClass().getName().trim();
                String driverInfo = String.format("%s v%d.%d",
                    driverClass, driver.getMajorVersion(), driver.getMinorVersion());
                foundDrivers.put(driverClass, driverInfo);
            }
        } catch (Exception e) {
            logger.debug("Error enumerating registered drivers", e);
        }

        // Check for common JDBC drivers in classpath (only if not already found)
        Map<String, String> commonDrivers = getCommonJdbcDrivers();

        for (Map.Entry<String, String> driverEntry : commonDrivers.entrySet()) {
            String driverClass = driverEntry.getKey();
            String databaseType = driverEntry.getValue();

            // Skip if we already found this driver through DriverManager
            if (foundDrivers.containsKey(driverClass)) {
                // Update the description to include database type if it's missing
                String existingInfo = foundDrivers.get(driverClass);
                if (!existingInfo.contains(driverClass)) {
                    // Replace the existing entry with enhanced info
                    String enhancedInfo = String.format("%s -> %s", databaseType, existingInfo);
                    foundDrivers.put(driverClass, enhancedInfo);
                }
                continue;
            }

            try {
                Class.forName(driverClass);
                // Driver is available - get version info if possible
                String versionInfo = getDriverVersionInfo(driverClass).trim();
                String driverInfo = String.format("%s -> %s %s", databaseType, driverClass, versionInfo);
                foundDrivers.put(driverClass, driverInfo);
            } catch (ClassNotFoundException e) {
                // Driver not available - we'll note this below
            } catch (Exception e) {
                // Other error loading driver
                logger.debug("Error loading driver {}: {}", driverClass, e.getMessage());
            }
        }

        if (foundDrivers.isEmpty()) {
            System.out.println("  No JDBC drivers found in classpath");
            System.out.println("  Note: Add JDBC driver JARs to classpath to enable database connectivity");
        } else {
            // Sort drivers for consistent output
            List<String> sortedDrivers = new ArrayList<>(foundDrivers.values());
            sortedDrivers.sort(String.CASE_INSENSITIVE_ORDER);

            for (String driverInfo : sortedDrivers) {
                System.out.println(" - " + driverInfo);
            }
        }
    }

    /**
     * Returns a map of common JDBC driver classes and their associated database types.
     * This list is synchronized with all Maven profiles in pom.xml.
     */
    static Map<String, String> getCommonJdbcDrivers() {
        Map<String, String> allDrivers = new LinkedHashMap<>(); // LinkedHashMap to preserve order

        // Basic drivers (default profile)
        allDrivers.put("org.h2.Driver", "H2 Database");
        allDrivers.put("org.hsqldb.jdbc.JDBCDriver", "HSQLDB HyperSQL");
        allDrivers.put("org.postgresql.Driver", "PostgreSQL");
        allDrivers.put("org.sqlite.JDBC", "SQLite");
        allDrivers.put("org.relique.jdbc.csv.CsvDriver", "CSV JDBC Driver");

        // Standard databases profile
        allDrivers.put("com.mysql.cj.jdbc.Driver", "MySQL 8.0+");
        allDrivers.put("org.mariadb.jdbc.Driver", "MariaDB");
        allDrivers.put("com.clickhouse.jdbc.ClickHouseDriver", "ClickHouse");

        // Enterprise databases profile
        allDrivers.put("oracle.jdbc.OracleDriver", "Oracle Database");
        allDrivers.put("com.microsoft.sqlserver.jdbc.SQLServerDriver", "SQL Server");
        allDrivers.put("com.ibm.db2.jcc.DB2Driver", "IBM DB2");

        // Cloud analytics profile
        allDrivers.put("com.amazon.redshift.jdbc.Driver", "Amazon Redshift");
        allDrivers.put("com.databricks.client.jdbc.Driver", "Databricks");
        allDrivers.put("net.snowflake.client.jdbc.SnowflakeDriver", "Snowflake");
        allDrivers.put("com.google.cloud.bigquery.jdbc.Driver", "Google BigQuery");
        allDrivers.put("org.duckdb.DuckDBDriver", "DuckDB");

        // Big data profile
        allDrivers.put("org.apache.hive.jdbc.HiveDriver", "Apache Hive");
        allDrivers.put("com.dbvis.redis.jdbc.RedisDriver", "Redis");
        allDrivers.put("com.mongodb.jdbc.MongoDriver", "MongoDB");
        allDrivers.put("com.github.adejanovski.cassandra.jdbc.CassandraDriver", "Apache Cassandra");

        return allDrivers;
    }

    /**
     * Attempts to get version information for a JDBC driver.
     */
    static String getDriverVersionInfo(String driverName) {
        try {
            Driver driverClass = (Driver) Class.forName(driverName).getDeclaredConstructor().newInstance();
            int majorVersion = driverClass.getMajorVersion();
            int minorVersion = driverClass.getMinorVersion();
            return String.format(" v%d.%d", majorVersion, minorVersion);
        } catch (Exception e) {
            return ""; // Return empty string if version can't be determined
        }
    }

    /**
     * Loads configuration from command line arguments, config file, environment variables, and system properties.
     * Uses priority order: CLI args (--db_url) > config file > environment variables (DB_URL) > system properties (-Ddb.url=) > hard coded defaults.
     * Keys are case-insensitive for args but uppercase for environment variables (as per convention).
     *
     * @param args Command line arguments in --key=value format
     * @return Configured ConfigParams instance
     * @throws IOException if config file cannot be read
     * @throws NumberFormatException if numeric parameters cannot be parsed
     */
    public static ConfigParams loadConfiguration(String[] args) throws IOException {
        Map<String, String> cliArgs = parseArgs(args);

        // Load config file if specified
        Map<String, String> fileConfig = null;
        String configFile = getConfigValue("CONFIG_FILE", null, cliArgs, null); // No file config for CONFIG_FILE itself
        if (configFile != null) {
            try {
                fileConfig = loadConfigFile(configFile);
                logger.info("Configuration file loaded: {}", configFile);
            } catch (IOException e) {
                logger.error("Failed to load configuration file: {}", configFile, e);
                throw new IOException("Failed to load configuration file: " + configFile, e);
            }
        }

        // Load configuration values with proper priority order
        String dbUrl = getConfigValue("DB_URL", DEFAULT_DB_URL, cliArgs, fileConfig);
        String dbUser = getConfigValue("DB_USER", DEFAULT_DB_USER, cliArgs, fileConfig);
        String dbPassword = getConfigValue("DB_PASSWORD", DEFAULT_DB_PASSWORD, cliArgs, fileConfig);
        String dbDriver = getConfigValue("DB_DRIVER", DEFAULT_DB_DRIVER, cliArgs, fileConfig);
        String maxConnections = getConfigValue("MAX_CONNECTIONS", "10", cliArgs, fileConfig);
        String connectionTimeoutMs = getConfigValue("CONNECTION_TIMEOUT_MS", "30000", cliArgs, fileConfig);
        String queryTimeoutSeconds = getConfigValue("QUERY_TIMEOUT_SECONDS", "30", cliArgs, fileConfig);
        String selectOnly = getConfigValue("SELECT_ONLY", "true", cliArgs, fileConfig);
        String maxSql = getConfigValue("MAX_SQL", "10000", cliArgs, fileConfig);
        String maxRowsLimit = getConfigValue("MAX_ROWS_LIMIT", "10000", cliArgs, fileConfig);
        String idleTimeoutMs = getConfigValue("IDLE_TIMEOUT_MS", "600000", cliArgs, fileConfig);
        String maxLifetimeMs = getConfigValue("MAX_LIFETIME_MS", "1800000", cliArgs, fileConfig);
        String leakDetectionThresholdMs = getConfigValue("LEAK_DETECTION_THRESHOLD_MS", "20000", cliArgs, fileConfig);

        try {
            return new ConfigParams(dbUrl, dbUser, dbPassword, dbDriver,
                    parseIntegerConfig("MAX_CONNECTIONS", maxConnections),
                    parseIntegerConfig("CONNECTION_TIMEOUT_MS", connectionTimeoutMs),
                    parseIntegerConfig("QUERY_TIMEOUT_SECONDS", queryTimeoutSeconds),
                    parseBooleanConfig("SELECT_ONLY", selectOnly),
                    parseIntegerConfig("MAX_SQL", maxSql),
                    parseIntegerConfig("MAX_ROWS_LIMIT", maxRowsLimit),
                    parseIntegerConfig("IDLE_TIMEOUT_MS", idleTimeoutMs),
                    parseIntegerConfig("MAX_LIFETIME_MS", maxLifetimeMs),
                    parseIntegerConfig("LEAK_DETECTION_THRESHOLD_MS", leakDetectionThresholdMs));
        } catch (IllegalArgumentException e) {
            // Re-throw with additional context about configuration loading
            throw new IllegalArgumentException(
                ResourceManager.getErrorMessage("config.validation.failed", "configuration parameters", e.getMessage()), e);
        }
    }

    /**
     * Determines if the server should run in HTTP mode based on configuration.
     *
     * @param args Command line arguments
     * @return true if HTTP mode is enabled, false for stdio mode
     */
    public static boolean isHttpMode(String[] args) {
        Map<String, String> cliArgs = parseArgs(args);
        String httpMode = getConfigValue("HTTP_MODE", "false", cliArgs, null);
        return Boolean.parseBoolean(httpMode);
    }

    /**
     * Gets the bind address from configuration.
     *
     * @param args Command line arguments
     * @return Bind address for HTTP mode (default: "localhost")
     */
    public static String getBindAddress(String[] args) {
        Map<String, String> cliArgs = parseArgs(args);
        return getConfigValue("BIND_ADDRESS", "localhost", cliArgs, null);
    }

    /**
     * Gets the HTTP port number from configuration.
     *
     * @param args Command line arguments
     * @return Port number for HTTP mode (default: 8080)
     */
    public static int getHttpPort(String[] args) {
        Map<String, String> cliArgs = parseArgs(args);
        String httpPort = getConfigValue("HTTP_PORT", "8080", cliArgs, null);
        return Integer.parseInt(httpPort);
    }
    /**
     * Gets a configuration value using the priority order:
     * CLI args > config file > env vars > system properties > default.
     * Config file parameter is optional - if null, it's skipped in the priority chain.
     *
     * @param varName Config parameter name (uppercase)
     * @param defaultValue Default value if not found in any source
     * @param cliArgs Parsed command line arguments
     * @param fileConfig Configuration from file (can be null if no config file)
     * @return The configuration value from the highest priority source
     */
    private static String getConfigValue(String varName, String defaultValue, Map<String, String> cliArgs, Map<String, String> fileConfig) {
        // 1. CLI arguments (highest priority)
        String cliValue = cliArgs.get(varName.toUpperCase());
        if (cliValue != null) {
            return cliValue;
        }

        // 2. Config file (if provided)
        if (fileConfig != null) {
            String fileValue = fileConfig.get(varName.toUpperCase());
            if (fileValue != null) {
                return fileValue;
            }
        }

        // 3. Environment variable
        String envValue = System.getenv(varName);
        if (envValue != null) {
            return envValue;
        }

        // 4. System property (envVar.lower().replace('_', '.'))
        String propValue = System.getProperty(varName.toLowerCase().replace('_', '.'));
        if (propValue != null) {
            return propValue;
        }

        // 5. Default
        return defaultValue;
    }

    /**
     * Loads configuration parameters from a file.
     * Each line should be in KEY=VALUE format. Lines starting with # are treated as comments.
     * Empty lines are ignored.
     *
     * @param configFilePath Path to the configuration file
     * @return Map of configuration key-value pairs
     * @throws IOException if the file cannot be read
     */
    public static Map<String, String> loadConfigFile(String configFilePath) throws IOException {
        Map<String, String> configMap = new HashMap<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(configFilePath))) {
            String currLine;
            int lineNumber = 0;

            while ((currLine = bufferedReader.readLine()) != null) {
                lineNumber++;
                currLine = currLine.trim();

                // Skip empty lines and comments
                if (currLine.isEmpty() || currLine.startsWith("#")) {
                    continue;
                }

                // Parse KEY=VALUE format
                String[] lineParts = currLine.split("=", 2);
                if (lineParts.length != 2) {
                    logger.warn("Invalid config line {} in file {}: {}", lineNumber, configFilePath, currLine);
                    continue;
                }

                String paramKey = lineParts[0].trim().toUpperCase();
                String paramValue = lineParts[1].trim();

                // Validate that key is not empty after trimming
                if (paramKey.isEmpty()) {
                    logger.warn("Key cannot be empty. Invalid config on line {} in file {}:\n{}", lineNumber, configFilePath, currLine);
                    continue;
                }

                // Remove quotes if present
                if (paramValue.startsWith("\"") && paramValue.endsWith("\"")) {
                    paramValue = paramValue.substring(1, paramValue.length() - 1);
                } else if (paramValue.startsWith("'") && paramValue.endsWith("'")) {
                    paramValue = paramValue.substring(1, paramValue.length() - 1);
                }

                configMap.put(paramKey, paramValue);
                logger.debug("Loaded config: {} = {}", paramKey, paramKey.contains("PASSWORD") ? "***" : paramValue);
            }
        }

        logger.info("Loaded {} configuration parameters from file: {}", configMap.size(), configFilePath);
        return configMap;
    }

    /**
     * Parses an integer configuration value with detailed error context.
     *
     * @param paramName The parameter name for error reporting
     * @param value The string value to parse
     * @return Parsed integer value
     * @throws IllegalArgumentException if the value cannot be parsed as an integer
     */
    private static int parseIntegerConfig(String paramName, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                ResourceManager.getErrorMessage("config.parse.integer.failed", paramName, value), e);
        }
    }

    /**
     * Parses a boolean configuration value with detailed error context.
     *
     * @param paramName The parameter name for error reporting
     * @param value The string value to parse
     * @return Parsed boolean value
     * @throws IllegalArgumentException if the value is not a valid boolean
     */
    private static boolean parseBooleanConfig(String paramName, String value) {
        if (value == null) {
            throw new IllegalArgumentException(
                ResourceManager.getErrorMessage("config.parse.boolean.failed", paramName, "null"));
        }
        
        String lowerValue = value.toLowerCase().trim();
        if ("true".equals(lowerValue) || "false".equals(lowerValue)) {
            return Boolean.parseBoolean(lowerValue);
        } else {
            throw new IllegalArgumentException(
                ResourceManager.getErrorMessage("config.parse.boolean.failed", paramName, value));
        }
    }
}
