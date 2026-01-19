package com.skanga.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skanga.mcp.config.ConfigParams;
import com.skanga.mcp.db.DatabaseService;
import com.skanga.mcp.db.QueryResult;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Performance tests for the Database MCP Server focusing on OUR code performance,
 * not database performance. Tests memory usage, concurrency, and data processing efficiency.
 */
@ExtendWith(MockitoExtension.class)
class McpServerPerformanceTest {
    @Mock
    private Connection mockConnection;
    
    @Mock
    private PreparedStatement mockStatement;
    
    @Mock
    private ResultSet mockResultSet;
    
    @Mock
    private ResultSetMetaData mockMetaData;

    @Mock
    private HikariDataSource mockDataSource;

    @Mock
    private ResultSet mockValidationResultSet;

    private DatabaseService databaseService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws SQLException {
        ConfigParams config = ConfigParams.defaultConfig("jdbc:h2:mem:test", "sa", "", "org.h2.Driver");
        databaseService = new DatabaseService(config, mockDataSource);
        lenient().when(mockDataSource.getConnection()).thenReturn(mockConnection);
        lenient().when(mockStatement.executeQuery()).thenReturn(mockValidationResultSet);
        lenient().when(mockValidationResultSet.next()).thenReturn(true);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should handle memory efficiently when formatting large result sets")
    void shouldHandleMemoryEfficientlyWithLargeResultFormatting() throws SQLException {
        // Given - Create a large result set (1000 rows)
        int rowCount = 1000;
        String sql = "SELECT * FROM large_table";
        
        setupLargeResultSetMock(rowCount);

        // Measure memory before
        System.gc();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // When - Execute query and format results
        QueryResult result = databaseService.executeSql(sql, rowCount);
        String formattedTable = formatResultsAsTable(result);

        // Measure memory after
        System.gc();
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        // Then - Verify reasonable memory usage (less than 10MB for 1000 rows)
        assertThat(result.rowCount()).isEqualTo(rowCount);
        assertThat(formattedTable.length()).isGreaterThan(0);
        assertThat(memoryUsed).isLessThan(10 * 1024 * 1024); // 10MB limit
        
        System.out.printf("Memory used for %d rows: %.2f MB%n", rowCount, memoryUsed / (1024.0 * 1024.0));
    }

    @Test
    @DisplayName("Should handle concurrent query requests efficiently")
    void shouldHandleConcurrentQueriesEfficiently() throws Exception {
        // Given
        int numberOfThreads = 10;
        int queriesPerThread = 5;
        
        setupSimpleResultSetMock();
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        // When - Execute concurrent queries
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    for (int j = 0; j < queriesPerThread; j++) {
                        String sql = String.format("SELECT * FROM table_%d WHERE id = %d", threadId, j);
                        QueryResult result = databaseService.executeSql(sql, 100);
                        assertThat(result).isNotNull();
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        startLatch.countDown(); // Start all threads
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        
        executor.shutdown();

        // Then - Verify performance and correctness
        assertThat(completed).isTrue();
        assertThat(errorCount.get()).isZero();
        assertThat(successCount.get()).isEqualTo(numberOfThreads * queriesPerThread);
        assertThat(totalTime).isLessThan(5000); // Should complete within 5 seconds
        
        System.out.printf("Processed %d concurrent queries in %dms%n", 
                         successCount.get(), totalTime);
    }

    @Test
    @DisplayName("Should enforce max rows limit to prevent memory issues")
    @Timeout(value = 3)
    void shouldEnforceMaxRowsLimitEfficiently() throws SQLException {
        // Given - Mock unlimited result set
        int maxRows = 100;
        String sql = "SELECT * FROM unlimited_table";
        
        setupUnlimitedResultSetMock();

        // When - Execute with row limit
        long startTime = System.currentTimeMillis();
        QueryResult result = databaseService.executeSql(sql, maxRows);
        long executionTime = System.currentTimeMillis() - startTime;

        // Then - Verify limit is enforced efficiently
        assertThat(result.rowCount()).isEqualTo(maxRows);
        assertThat(result.allRows()).hasSize(maxRows);
        assertThat(executionTime).isLessThan(1000); // Should complete quickly
        
        // Verify we stopped processing after maxRows
        verify(mockResultSet, times(maxRows + 1)).next(); // +1 for the final check
    }

    @Test
    @DisplayName("Should handle JSON serialization efficiently for MCP responses")
    void shouldHandleJsonSerializationEfficiently() throws Exception {
        // Given - Large query result
        int rowCount = 500;
        setupLargeResultSetMock(rowCount);
        
        QueryResult result = databaseService.executeSql("SELECT * FROM test", rowCount);
        
        // Create MCP response structure
        Map<String, Object> mcpResponse = new HashMap<>();
        mcpResponse.put("jsonrpc", "2.0");
        mcpResponse.put("id", 1);
        
        Map<String, Object> toolResult = new HashMap<>();
        toolResult.put("isError", false);
        
        List<Map<String, Object>> content = new ArrayList<>();
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", formatResultsAsTable(result));
        content.add(textContent);
        
        toolResult.put("content", content);
        mcpResponse.put("result", toolResult);

        // When - Serialize to JSON
        long startTime = System.currentTimeMillis();
        String json = objectMapper.writeValueAsString(mcpResponse);
        long serializationTime = System.currentTimeMillis() - startTime;

        // Then - Verify efficient serialization
        assertThat(json).isNotEmpty();
        assertThat(json).contains("jsonrpc");
        assertThat(serializationTime).isLessThan(500); // Should serialize within 500ms
        
        System.out.printf("JSON serialization of %d rows took %dms, size: %d bytes%n", 
                         rowCount, serializationTime, json.length());
    }

    @Test
    @DisplayName("Should handle large column names and data efficiently")
    void shouldHandleLargeColumnNamesAndDataEfficiently() throws SQLException {
        // Given - Result set with very long column names and data
        String sql = "SELECT * FROM wide_table";
        
        setupWideResultSetMock();

        // When - Process result
        long startTime = System.currentTimeMillis();
        QueryResult result = databaseService.executeSql(sql, 10);
        String formatted = formatResultsAsTable(result);
        long processingTime = System.currentTimeMillis() - startTime;

        // Then - Verify efficient processing
        assertThat(result.allColumns()).hasSize(5);
        assertThat(result.allRows()).hasSize(10);
        assertThat(formatted).contains("very_long_column_name_that_exceeds_normal_limits");
        assertThat(processingTime).isLessThan(200); // Should process quickly
    }

    @Test
    @DisplayName("Should handle data type conversion efficiently")
    void shouldHandleDataTypeConversionEfficiently() throws SQLException {
        // Given - Result set with mixed data types
        String sql = "SELECT * FROM mixed_types_table";
        
        setupMixedTypesResultSetMock();

        // When - Process different data types
        long startTime = System.currentTimeMillis();
        QueryResult result = databaseService.executeSql(sql, 100);
        long processingTime = System.currentTimeMillis() - startTime;

        // Then - Verify efficient type handling
        assertThat(result.allRows()).hasSize(100);
        assertThat(processingTime).isLessThan(1000); // Should process reasonably quickly
        
        // Verify data types are preserved
        List<Object> firstRow = result.allRows().get(0);
        assertThat(firstRow.get(0)).isInstanceOf(Integer.class);
        assertThat(firstRow.get(1)).isInstanceOf(String.class);
        assertThat(firstRow.get(2)).isInstanceOf(Double.class);
        assertThat(firstRow.get(3)).isInstanceOf(Boolean.class);
    }

    @Test
    @DisplayName("Should handle string formatting performance for table output")
    void shouldHandleStringFormattingPerformanceForTableOutput() throws SQLException {
        // Given - Various sized result sets
        int[] rowCounts = {10, 100, 500};
        
        for (int rowCount : rowCounts) {
            setupLargeResultSetMock(rowCount);
            QueryResult result = databaseService.executeSql("SELECT * FROM test", rowCount);
            
            // When - Format as table
            long startTime = System.currentTimeMillis();
            String formatted = formatResultsAsTable(result);
            long formattingTime = System.currentTimeMillis() - startTime;
            
            // Then - Verify reasonable formatting performance
            assertThat(formatted).isNotEmpty();
            long maxAllowed = Math.max(50L, rowCount * 2L);
            assertThat(formattingTime).isLessThan(maxAllowed); // Should be roughly linear
            
            System.out.printf("Formatting %d rows took %dms%n", rowCount, formattingTime);
        }
    }

    // Helper methods for setting up mocks
    private void setupLargeResultSetMock(int rowCount) throws SQLException {
        when(mockMetaData.getColumnCount()).thenReturn(5);
        when(mockMetaData.getColumnName(1)).thenReturn("id");
        when(mockMetaData.getColumnName(2)).thenReturn("name");
        when(mockMetaData.getColumnName(3)).thenReturn("description");
        when(mockMetaData.getColumnName(4)).thenReturn("value");
        when(mockMetaData.getColumnName(5)).thenReturn("created_date");
        
        when(mockResultSet.getMetaData()).thenReturn(mockMetaData);
        
        // Mock the next() calls
        Boolean[] nextResults = new Boolean[rowCount + 1];
        Arrays.fill(nextResults, 0, rowCount, true);
        nextResults[rowCount] = false;
        when(mockResultSet.next()).thenReturn(true, nextResults);
        
        // Mock data for each column
        when(mockResultSet.getObject(1)).thenAnswer(inv -> {
            // Return sequential IDs
            return new Random().nextInt(10000);
        });
        when(mockResultSet.getObject(2)).thenReturn("Sample Name");
        when(mockResultSet.getObject(3)).thenReturn("This is a longer description field with more content");
        when(mockResultSet.getObject(4)).thenReturn(123.45);
        when(mockResultSet.getObject(5)).thenReturn(Date.valueOf("2024-01-01"));
        
        when(mockStatement.execute()).thenReturn(true);
        when(mockStatement.getResultSet()).thenReturn(mockResultSet);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
    }

    private void setupSimpleResultSetMock() throws SQLException {
        when(mockMetaData.getColumnCount()).thenReturn(2);
        when(mockMetaData.getColumnName(1)).thenReturn("id");
        when(mockMetaData.getColumnName(2)).thenReturn("name");
        
        when(mockResultSet.getMetaData()).thenReturn(mockMetaData);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getObject(1)).thenReturn(1);
        when(mockResultSet.getObject(2)).thenReturn("Test");
        
        when(mockStatement.execute()).thenReturn(true);
        when(mockStatement.getResultSet()).thenReturn(mockResultSet);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
    }

    private void setupUnlimitedResultSetMock() throws SQLException {
        when(mockMetaData.getColumnCount()).thenReturn(1);
        when(mockMetaData.getColumnName(1)).thenReturn("id");
        
        when(mockResultSet.getMetaData()).thenReturn(mockMetaData);
        when(mockResultSet.next()).thenReturn(true); // Always return true (unlimited)
        when(mockResultSet.getObject(1)).thenAnswer(inv -> new Random().nextInt());
        
        when(mockStatement.execute()).thenReturn(true);
        when(mockStatement.getResultSet()).thenReturn(mockResultSet);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
    }

    private void setupWideResultSetMock() throws SQLException {
        when(mockMetaData.getColumnCount()).thenReturn(5);
        when(mockMetaData.getColumnName(1)).thenReturn("very_long_column_name_that_exceeds_normal_limits_and_tests_formatting");
        when(mockMetaData.getColumnName(2)).thenReturn("another_extremely_long_column_name_for_testing_purposes");
        when(mockMetaData.getColumnName(3)).thenReturn("short");
        when(mockMetaData.getColumnName(4)).thenReturn("medium_length_column");
        when(mockMetaData.getColumnName(5)).thenReturn("col");
        
        when(mockResultSet.getMetaData()).thenReturn(mockMetaData);
        
        Boolean[] nextResults = new Boolean[11];
        Arrays.fill(nextResults, 0, 10, true);
        nextResults[10] = false;
        when(mockResultSet.next()).thenReturn(true, nextResults);
        
        when(mockResultSet.getObject(1)).thenReturn("Very long data content that might affect formatting performance and memory usage");
        when(mockResultSet.getObject(2)).thenReturn("Another long piece of data for testing");
        when(mockResultSet.getObject(3)).thenReturn("Short");
        when(mockResultSet.getObject(4)).thenReturn("Medium data");
        when(mockResultSet.getObject(5)).thenReturn("X");
        
        when(mockStatement.execute()).thenReturn(true);
        when(mockStatement.getResultSet()).thenReturn(mockResultSet);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
    }

    private void setupMixedTypesResultSetMock() throws SQLException {
        when(mockMetaData.getColumnCount()).thenReturn(4);
        when(mockMetaData.getColumnName(1)).thenReturn("int_col");
        when(mockMetaData.getColumnName(2)).thenReturn("string_col");
        when(mockMetaData.getColumnName(3)).thenReturn("double_col");
        when(mockMetaData.getColumnName(4)).thenReturn("boolean_col");
        
        when(mockResultSet.getMetaData()).thenReturn(mockMetaData);
        
        Boolean[] nextResults = new Boolean[101];
        Arrays.fill(nextResults, 0, 100, true);
        nextResults[100] = false;
        when(mockResultSet.next()).thenReturn(true, nextResults);
        
        when(mockResultSet.getObject(1)).thenAnswer(inv -> new Random().nextInt(1000));
        when(mockResultSet.getObject(2)).thenReturn("Sample String Data");
        when(mockResultSet.getObject(3)).thenAnswer(inv -> new Random().nextDouble() * 1000);
        when(mockResultSet.getObject(4)).thenAnswer(inv -> new Random().nextBoolean());
        
        when(mockStatement.execute()).thenReturn(true);
        when(mockStatement.getResultSet()).thenReturn(mockResultSet);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
    }

    // Copy of table formatting method for testing
    private String formatResultsAsTable(QueryResult result) {
        if (result.allRows().isEmpty()) {
            return "No data";
        }
        
        List<String> columns = result.allColumns();
        List<List<Object>> rows = result.allRows();
        
        // Calculate column widths
        int[] widths = new int[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            widths[i] = columns.get(i).length();
        }
        
        for (List<Object> row : rows) {
            for (int i = 0; i < row.size() && i < widths.length; i++) {
                Object value = row.get(i);
                String str = value != null ? value.toString() : "NULL";
                widths[i] = Math.max(widths[i], str.length());
            }
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Header
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sb.append(" | ");
            sb.append(String.format("%-" + widths[i] + "s", columns.get(i)));
        }
        sb.append("\n");
        
        // Separator
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sb.append("-+-");
            sb.append("-".repeat(widths[i]));
        }
        sb.append("\n");
        
        // Data rows
        for (List<Object> row : rows) {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) sb.append(" | ");
                Object value = i < row.size() ? row.get(i) : null;
                String str = value != null ? value.toString() : "NULL";
                sb.append(String.format("%-" + widths[i] + "s", str));
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }

}
