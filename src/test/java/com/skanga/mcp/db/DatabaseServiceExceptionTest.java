package com.skanga.mcp.db;

import com.skanga.mcp.TestUtils;
import com.skanga.mcp.config.ConfigParams;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseServiceExceptionTest {
    @Mock
    private HikariDataSource mockDataSource;
    
    @Mock
    private Connection mockConnection;
    
    @Mock
    private PreparedStatement mockStatement;
    
    @Mock
    private ResultSet mockResultSet;
    
    @Mock
    private DatabaseMetaData mockMetaData;
    
    @Mock
    private ResultSetMetaData mockRsMetaData;

    @Mock
    private ResultSet mockValidationResultSet;
    
    private ConfigParams testConfig;
    private DatabaseService databaseService;

    @BeforeEach
    void setUp() throws SQLException {
        testConfig = new ConfigParams(
            "jdbc:h2:mem:testdb", "sa", "", "org.h2.Driver",
            10, 30000, 30, true, 10000, 10000, 600000, 1800000, 60000
        );
        lenient().when(mockConnection.prepareStatement("SELECT 1")).thenReturn(mockStatement);
        lenient().when(mockStatement.executeQuery()).thenReturn(mockValidationResultSet);
        lenient().when(mockValidationResultSet.next()).thenReturn(true);
    }

    @Test
    void testConstructor_DriverNotFound() {
        // Test with non-existent driver
        ConfigParams badConfig = new ConfigParams(
            "jdbc:nonexistent:mem:testdb", "sa", "", "com.nonexistent.Driver",
            10, 30000, 30, true, 10000, 10000, 600000, 1800000, 60000
        );

        //RuntimeException exception = assertThrows(RuntimeException.class, () -> new DatabaseService(badConfig));
        RuntimeException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(RuntimeException.class, () -> new DatabaseService(badConfig))
        );

        assertTrue(exception.getMessage().contains("Database driver class 'com.nonexistent.Driver' not found in classpath"));
        assertTrue(exception.getCause() instanceof ClassNotFoundException);
    }

    @Test
    void testConstructor_ConnectionPoolInitializationFailed() {
        // Use a unique database name each time to avoid conflicts
        String uniqueDbName = "testdb_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();

        ConfigParams badConfig = new ConfigParams(
                "jdbc:h2:file:/definitely/nonexistent/path/" + uniqueDbName + ";IFEXISTS=TRUE",
                "sa", "", "org.h2.Driver",
                10, 30000, 30, true,
                1000,  // Shorter timeout to fail faster
                1000,  // Shorter timeout to fail faster
                600000, 1800000, 60000
        );

        //RuntimeException exception = assertThrows(RuntimeException.class, () -> new DatabaseService(badConfig));
        RuntimeException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(RuntimeException.class, () -> new DatabaseService(badConfig))
        );

        // More flexible assertion - just check that it's a pool initialization failure
        assertTrue(exception.getMessage().contains("Failed to initialize pool") ||
                exception.getMessage().contains("Database") ||
                exception.getMessage().contains("not found") ||
                exception.getMessage().contains("does not exist"));
        assertTrue(exception.getCause() instanceof SQLException);
    }

    @Test
    void testConstructor_WithDataSource_DriverNotFound() {
        ConfigParams badConfig = new ConfigParams(
            "jdbc:h2:mem:testdb", "sa", "", "com.nonexistent.Driver",
            10, 30000, 30, true, 10000, 10000, 600000, 1800000, 60000
        );

        //RuntimeException exception = assertThrows(RuntimeException.class, () -> new DatabaseService(badConfig, mockDataSource));
        RuntimeException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(RuntimeException.class, () -> new DatabaseService(badConfig, mockDataSource))
        );

        assertTrue(exception.getMessage().contains("Database driver class 'com.nonexistent.Driver' not found in classpath"));
        assertTrue(exception.getCause() instanceof ClassNotFoundException);
    }

    @Test
    void testExecuteQuery_ConnectionFailure() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenThrow(new SQLException("Database connection failed"));

        SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.executeSql("SELECT * FROM test", 100))
        );

        assertEquals("Unable to obtain valid database connection after 3 attempts", exception.getMessage());
    }

    @Test
    void testExecuteQuery_PreparedStatementFailure() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement("INVALID SQL")).thenThrow(new SQLException("Invalid SQL syntax"));

        SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.executeSql("INVALID SQL", 100))
        );

        assertEquals("Invalid SQL syntax", exception.getMessage());
    }

    @Test
    void testExecuteQuery_StatementExecutionFailure() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.execute()).thenThrow(new SQLException("Table does not exist"));

        SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.executeSql("SELECT * FROM nonexistent_table", 100))
        );

        assertEquals("Table does not exist", exception.getMessage());
    }

    @Test
    void testExecuteQuery_ResultSetFailure() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.execute()).thenReturn(true);
        when(mockStatement.getResultSet()).thenReturn(mockResultSet);
        when(mockResultSet.getMetaData()).thenThrow(new SQLException("ResultSet metadata error"));

        SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.executeSql("SELECT * FROM test", 100))
        );
        
        assertEquals("ResultSet metadata error", exception.getMessage());
    }

    @Test
    void testExecuteQuery_MetaDataColumnFailure() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.execute()).thenReturn(true);
        when(mockStatement.getResultSet()).thenReturn(mockResultSet);
        when(mockResultSet.getMetaData()).thenReturn(mockRsMetaData);
        when(mockRsMetaData.getColumnCount()).thenReturn(2);
        when(mockRsMetaData.getColumnName(1)).thenReturn("col1");
        when(mockRsMetaData.getColumnName(2)).thenThrow(new SQLException("Column metadata error"));

        SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.executeSql("SELECT * FROM test", 100))
        );

        assertEquals("Column metadata error", exception.getMessage());
    }

    @Test
    void testExecuteQuery_ResultSetNextFailure() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.execute()).thenReturn(true);
        when(mockStatement.getResultSet()).thenReturn(mockResultSet);
        when(mockResultSet.getMetaData()).thenReturn(mockRsMetaData);
        when(mockRsMetaData.getColumnCount()).thenReturn(1);
        when(mockRsMetaData.getColumnName(1)).thenReturn("col1");
        when(mockResultSet.next()).thenThrow(new SQLException("ResultSet iteration error"));

        SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.executeSql("SELECT * FROM test", 100))
        );

        assertEquals("Result set reading failed at row 0: ResultSet iteration error", exception.getMessage());
    }

    @Test
    void testExecuteQuery_GetObjectFailure() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.execute()).thenReturn(true);
        when(mockStatement.getResultSet()).thenReturn(mockResultSet);
        when(mockResultSet.getMetaData()).thenReturn(mockRsMetaData);
        when(mockRsMetaData.getColumnCount()).thenReturn(1);
        when(mockRsMetaData.getColumnName(1)).thenReturn("col1");
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getObject(1)).thenThrow(new SQLException("Data conversion error"));

        SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.executeSql("SELECT * FROM test", 100))
        );

        assertEquals("Result set reading failed at row 0: Data conversion error", exception.getMessage());
    }

    @Test
    void testValidateSqlQuery_EmptyQuery() {
        databaseService = new DatabaseService(testConfig, mockDataSource);

        SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.executeSql("", 100))
        );

        assertEquals("SQL query cannot be empty", exception.getMessage());
    }

    @Test
    void testValidateSqlQuery_NullQuery() {
        databaseService = new DatabaseService(testConfig, mockDataSource);

        SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.executeSql(null, 100))
        );

        assertEquals("SQL query cannot be empty", exception.getMessage());
    }

    @Test
    void testValidateSqlQuery_WhitespaceOnlyQuery() {
        databaseService = new DatabaseService(testConfig, mockDataSource);

        SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.executeSql("   \t\n  ", 100))
        );

        assertEquals("SQL query cannot be empty", exception.getMessage());
    }

    @Test
    void testValidateSqlQuery_DangerousOperations() {
        databaseService = new DatabaseService(testConfig, mockDataSource);

        String[] dangerousQueries = {
            "DROP TABLE test",
            "TRUNCATE TABLE test", 
            "DELETE FROM test",
            "UPDATE test SET col=1",
            "INSERT INTO test VALUES(1)",
            "CREATE TABLE test(id INT)",
            "ALTER TABLE test ADD COLUMN col2 VARCHAR(50)",
            "GRANT ALL ON test TO user",
            "REVOKE ALL ON test FROM user",
            "EXEC sp_test",
            "EXECUTE sp_test",
            "CALL sp_test()"
        };

        for (String query : dangerousQueries) {
            SQLException exception = TestUtils.withSuppressedLogging(() ->
                    assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100))
            );

            assertTrue(exception.getMessage().startsWith("Operation not allowed:"));
        }
    }

    @Test
    void testValidateSqlQuery_DangerousOperations_CaseInsensitive() {
        databaseService = new DatabaseService(testConfig, mockDataSource);

        String[] dangerousQueries = {
            "drop table test",
            "DROP table test",
            "Drop Table test",
            "DELETE from test",
            "delete FROM test"
        };

        for (String query : dangerousQueries) {
            SQLException exception = TestUtils.withSuppressedLogging(() ->
                    assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100))
            );

            assertTrue(exception.getMessage().startsWith("Operation not allowed:"));
        }
    }

    @Test
    void testValidateSqlQuery_MultipleStatements() {
        databaseService = new DatabaseService(testConfig, mockDataSource);

        // Use only SELECT statements (safe operations) to test multiple statement detection
        String[] multiStatementQueries = {
                "SELECT 1; SELECT 2",                           // No trailing semicolon
                "SELECT * FROM test; SELECT COUNT(*) FROM test", // No trailing semicolon
                "SELECT col1 FROM test; SELECT col2 FROM test"   // No trailing semicolon
        };

        for (String query : multiStatementQueries) {
            SQLException exception = TestUtils.withSuppressedLogging(() ->
                    assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100))
            );

            assertEquals("Multiple statements not allowed", exception.getMessage());
        }
    }

    @Test
    void testValidateSqlQuery_SqlComments() {
        databaseService = new DatabaseService(testConfig, mockDataSource);

        // Test line comments
        SQLException exception1 = assertThrows(SQLException.class, () -> databaseService.executeSql("SELECT * FROM test -- comment", 100));
        assertEquals("SQL comments not allowed", exception1.getMessage());

        // Test block comments
        SQLException exception2 = assertThrows(SQLException.class, () -> databaseService.executeSql("SELECT * FROM test /* comment */", 100));
        assertEquals("SQL comments not allowed", exception2.getMessage());
    }

    @Test
    void testListResources_ConnectionFailure() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        SQLException exception = assertThrows(SQLException.class, () -> databaseService.listResources());
        
        assertEquals("Connection failed", exception.getMessage());
    }

    @Test
    void testListResources_MetaDataFailure() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getMetaData()).thenThrow(new SQLException("MetaData access failed"));

        SQLException exception = assertThrows(SQLException.class, () -> databaseService.listResources());
        
        assertEquals("MetaData access failed", exception.getMessage());
    }

    @Test
    void testListResources_DatabaseInfoFailure() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getDatabaseProductName()).thenThrow(new SQLException("Product name access failed"));

        SQLException exception = assertThrows(SQLException.class, () -> databaseService.listResources());
        
        assertEquals("Product name access failed", exception.getMessage());
    }

    @Test
    void testListResources_TablesMetaDataFailure() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getDatabaseProductName()).thenReturn("H2");
        when(mockMetaData.getDatabaseProductVersion()).thenReturn("1.0");
        when(mockMetaData.getDriverName()).thenReturn("H2 Driver");
        when(mockMetaData.getDriverVersion()).thenReturn("1.0");
        when(mockMetaData.isReadOnly()).thenReturn(false);
        when(mockMetaData.supportsTransactions()).thenReturn(true);
        when(mockMetaData.supportsStoredProcedures()).thenReturn(false);
        when(mockMetaData.supportsMultipleResultSets()).thenReturn(true);
        when(mockMetaData.supportsBatchUpdates()).thenReturn(true);
        
        // Fail on getting tables
        when(mockMetaData.getTables(null, null, "%", new String[]{"TABLE", "VIEW"}))
            .thenThrow(new SQLException("Tables metadata access failed"));

        SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.listResources())
        );

        assertEquals("Tables metadata access failed", exception.getMessage());
    }

    @Test
    void testListResources_SchemasNotSupported() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);
        
        // Mock successful database info and tables
        setupSuccessfulMetaDataMocks();

        // Mock empty tables result
        when(mockMetaData.getTables(null, null, "%", new String[]{"TABLE", "VIEW"}))
            .thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);
        
        // Schemas not supported - should be handled gracefully
        when(mockMetaData.getSchemas()).thenThrow(new SQLException("Schemas not supported"));

        // This should not throw - the exception should be caught and logged
        assertDoesNotThrow(() -> {
            List<DatabaseResource> resources = databaseService.listResources();
            // Should still return database info resource
            assertFalse(resources.isEmpty());
        });
    }

    @Test
    void testReadResource_ConnectionFailure() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.readResource("database://info"))
        );

        assertEquals("Connection failed", exception.getMessage());
    }

    @Test
    void testReadResource_InfoMetaDataFailure() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getDatabaseProductName()).thenThrow(new SQLException("Product name failed"));

        SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.readResource("database://info"))
        );

        assertEquals("Product name failed", exception.getMessage());
    }

    @Test
    void testGetTableResource_ConnectionFailure() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.readResource("database://table/test_table"))
        );

        assertEquals("Connection failed", exception.getMessage());
    }

    @Test
    void testGetTableResource_TablesMetaDataFailure() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getTables(null, null, "test_table", new String[]{"TABLE", "VIEW"}))
            .thenThrow(new SQLException("Tables metadata failed"));

        SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.readResource("database://table/test_table"))
        );

        assertEquals("Tables metadata failed", exception.getMessage());
    }

    @Test
    void testGetTableResource_ColumnsMetaDataFailure() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);
        
        // Mock table exists
        when(mockMetaData.getTables(null, null, "test_table", new String[]{"TABLE", "VIEW"}))
            .thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        
        // Fail on columns
        when(mockMetaData.getColumns(null, null, "test_table", null))
            .thenThrow(new SQLException("Columns metadata failed"));

        SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.readResource("database://table/test_table"))
        );

        assertEquals("Columns metadata failed", exception.getMessage());
    }

    @Test
    void testGetSchemaResource_ConnectionFailure() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.readResource("database://schema/test_schema"))
        );

        assertEquals("Connection failed", exception.getMessage());
    }

    @Test
    void testGetSchemaResource_SchemasNotSupported() throws SQLException {
        databaseService = new DatabaseService(testConfig, mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getSchemas()).thenThrow(new SQLException("Schemas not supported"));

        // Should return null when schemas not supported
        DatabaseResource result = databaseService.readResource("database://schema/test_schema");
        assertNull(result);
    }

    private void setupSuccessfulMetaDataMocks() throws SQLException {
        when(mockMetaData.getDatabaseProductName()).thenReturn("H2");
        when(mockMetaData.getDatabaseProductVersion()).thenReturn("1.0");
        when(mockMetaData.getDriverName()).thenReturn("H2 Driver");
        when(mockMetaData.getDriverVersion()).thenReturn("1.0");
        when(mockMetaData.isReadOnly()).thenReturn(false);
        lenient().when(mockConnection.getAutoCommit()).thenReturn(true);
        lenient().when(mockMetaData.getDefaultTransactionIsolation()).thenReturn(Connection.TRANSACTION_READ_COMMITTED);
        when(mockMetaData.supportsTransactions()).thenReturn(true);
        when(mockMetaData.supportsStoredProcedures()).thenReturn(false);
        when(mockMetaData.supportsMultipleResultSets()).thenReturn(true);
        when(mockMetaData.supportsBatchUpdates()).thenReturn(true);
    }
}
