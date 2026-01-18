package com.skanga.demo;

import java.io.*;
import java.sql.*;
import java.util.*;

public class DatabaseLoader {

    // Database type detection
    public enum DatabaseType {
        HSQLDB("hsqldb"),
        MYSQL("mysql"),
        ORACLE("oracle"),
        SQLITE("sqlite"),
        H2("h2"),
        DERBY("derby"),
        POSTGRESQL("postgresql"),
        UNKNOWN("unknown");

        private final String filePrefix;

        DatabaseType(String filePrefix) {
            this.filePrefix = filePrefix;
        }

        public String getFilePrefix() {
            return filePrefix;
        }
    }

    private static final String DROP_TABLES_FILE = "drop-tables.sql";
    private static final String CREATE_TABLES_SUFFIX = ".sql";
    private static final String DATA_SUFFIX = "-data.sql";

    public static void main(String[] args) throws SQLException, IOException {
        //String jdbcUrl = "jdbc:h2:mem:testdb";    // In-Memory Database (Recommended for Testing)
        //String jdbcUrl = "jdbc:h2:C:/Users/skanga/mydb";
        //String jdbcUrl = "jdbc:sqlite:memory";      // In-Memory Database (Recommended for Testing)
        String jdbcUrl = "jdbc:hsqldb:file:~/DB/testdb.db";
        String user = "sa";
        String password = "";
        //String schema = "invoices"; // Default schema name - Load *.sql from src/test/resources/<schema>
        String schema = "sqltutorial.org";

        // Parse command line arguments for schema
        if (args.length > 0) {
            schema = args[0];
        }

        setupDatabase(jdbcUrl, user, password, schema);
    }

    /**
     * Overloaded main method that accepts schema parameter
     */
    public static void setupDatabase(String jdbcUrl, String user, String password, String schema) throws SQLException, IOException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
            DatabaseType dbType = detectDatabaseType(conn);
            System.out.println("Detected database: " + dbType);
            System.out.println("Using schema: " + schema);

            conn.setAutoCommit(false);
            try {
                dropTables(conn, schema);
                createTables(conn, dbType, schema);
                populateData(conn, dbType, schema);
                conn.commit();
                System.out.println("Database setup complete for schema: " + schema);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static DatabaseType detectDatabaseType(Connection conn) throws SQLException {
        String url = conn.getMetaData().getURL().toLowerCase();
        String productName = conn.getMetaData().getDatabaseProductName().toLowerCase();

        if (url.contains("mysql") || productName.contains("mysql")) {
            return DatabaseType.MYSQL;
        } else if (url.contains("oracle") || productName.contains("oracle")) {
            return DatabaseType.ORACLE;
        } else if (url.contains("sqlite") || productName.contains("sqlite")) {
            return DatabaseType.SQLITE;
        } else if (url.contains("h2") || productName.contains("h2")) {
            return DatabaseType.H2;
        } else if (url.contains("hsqldb") || productName.contains("hsqldb")) {
            return DatabaseType.HSQLDB;
        } else if (url.contains("derby") || productName.contains("derby")) {
            return DatabaseType.DERBY;
        } else if (url.contains("postgresql") || productName.contains("postgresql")) {
            return DatabaseType.POSTGRESQL;
        }
        return DatabaseType.UNKNOWN;
    }

    private static void dropTables(Connection conn, String schema) throws SQLException, IOException {
        String dropFileName = schema + "/" + DROP_TABLES_FILE;
        System.out.println("Reading drop statements from: " + dropFileName);

        List<String> dropStatements = readSqlStatementsFromClasspath(dropFileName);
        if (dropStatements.isEmpty()) {
            System.out.println("No drop statements found. Skipping drop operation.");
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            for (String dropSql : dropStatements) {
                if (!dropSql.trim().isEmpty()) {
                    try {
                        System.out.println("Executing: " + dropSql);
                        stmt.executeUpdate(dropSql);
                    } catch (SQLException e) {
                        // Log but continue - table might not exist
                        System.out.println("Warning: " + e.getMessage());
                    }
                }
            }
        }
    }

    private static void createTables(Connection conn, DatabaseType dbType, String schema) throws SQLException, IOException {
        String createFileName = schema + "/" + dbType.getFilePrefix() + CREATE_TABLES_SUFFIX;
        System.out.println("Reading create statements from: " + createFileName);

        List<String> createStatements = readSqlStatementsFromClasspath(createFileName);
        if (createStatements.isEmpty()) {
            throw new FileNotFoundException("Create tables file not found or empty: " + createFileName);
        }

        try (Statement stmt = conn.createStatement()) {
            for (String createSql : createStatements) {
                if (!createSql.trim().isEmpty()) {
                    System.out.println("Executing: " + createSql.substring(0, Math.min(50, createSql.length())) + "...");
                    stmt.execute(createSql);
                }
            }
        }
    }

    private static void populateData(Connection conn, DatabaseType dbType, String schema) throws SQLException, IOException {
        String dataFileName = schema + "/" + dbType.getFilePrefix() + DATA_SUFFIX;
        System.out.println("Reading data statements from: " + dataFileName);

        List<String> dataStatements = readSqlStatementsFromClasspath(dataFileName);
        if (dataStatements.isEmpty()) {
            System.out.println("Data file not found or empty: " + dataFileName + ". Skipping data population.");
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            for (String dataSql : dataStatements) {
                if (!dataSql.trim().isEmpty()) {
                    System.out.println("Executing: " + dataSql.substring(0, Math.min(50, dataSql.length())) + "...");
                    stmt.executeUpdate(dataSql);
                }
            }
        }
    }

    private static List<String> readSqlStatementsFromClasspath(String fileName) throws IOException {
        ClassLoader classLoader = DatabaseLoader.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(fileName)) {
            if (inputStream == null) {
                // Return empty list instead of throwing exception for optional files
                System.out.println("File not found in classpath: " + fileName);
                return new ArrayList<>();
            }

            String content = new String(inputStream.readAllBytes());
            content = removeComments(content);
            String[] rawStatements = splitSqlStatements(content);

            List<String> statements = new ArrayList<>();
            for (String statement : rawStatements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    statements.add(trimmed);
                }
            }
            return statements;
        }
    }

    private static String removeComments(String sql) {
        // Remove single-line comments (-- comment)
        sql = sql.replaceAll("--.*(?=\\r?\\n|$)", "");

        // Remove multi-line comments (/* comment */)
        sql = sql.replaceAll("/\\*[\\s\\S]*?\\*/", "");

        return sql;
    }

    private static String[] splitSqlStatements(String content) {
        // Simple split by semicolon - this could be enhanced to handle
        // semicolons within quoted strings more robustly
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        char stringDelimiter = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (!inString && (c == '\'' || c == '"')) {
                inString = true;
                stringDelimiter = c;
                current.append(c);
            } else if (inString && c == stringDelimiter) {
                // Check for escaped quotes
                if (i + 1 < content.length() && content.charAt(i + 1) == stringDelimiter) {
                    current.append(c).append(stringDelimiter);
                    i++; // Skip the next character
                } else {
                    inString = false;
                    current.append(c);
                }
            } else if (!inString && c == ';') {
                statements.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // Add the last statement if it doesn't end with semicolon
        if (!current.isEmpty()) {
            statements.add(current.toString().trim());
        }

        return statements.toArray(new String[0]);
    }
}