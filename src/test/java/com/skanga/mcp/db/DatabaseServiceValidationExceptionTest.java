package com.skanga.mcp.db;

import com.skanga.mcp.TestUtils;
import com.skanga.mcp.config.ConfigParams;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests focused on SQL validation exception handling
 */
@ExtendWith(MockitoExtension.class)
class DatabaseServiceValidationExceptionTest {
    @Mock
    private HikariDataSource mockDataSource;
    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockStatement;

    @Mock
    private java.sql.ResultSet mockValidationResultSet;

    private DatabaseService databaseService;
    private DatabaseService nonSelectOnlyService;

    @BeforeEach
    void setUp() throws SQLException {
        // Service with SELECT_ONLY = true
        ConfigParams selectOnlyConfig = new ConfigParams(
            "jdbc:h2:mem:testdb", "sa", "", "org.h2.Driver",
            10, 30000, 30, true, 10000, 10000, 600000, 1800000, 60000
        );
        databaseService = new DatabaseService(selectOnlyConfig, mockDataSource);
        
        // Service with SELECT_ONLY = false
        ConfigParams nonSelectOnlyConfig = new ConfigParams(
            "jdbc:h2:mem:testdb", "sa", "", "org.h2.Driver",
            10, 30000, 30, false, 10000, 10000, 600000, 1800000, 60000
        );
        nonSelectOnlyService = new DatabaseService(nonSelectOnlyConfig, mockDataSource);
        lenient().when(mockDataSource.getConnection()).thenReturn(mockConnection);
        lenient().when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        lenient().when(mockStatement.executeQuery()).thenReturn(mockValidationResultSet);
        lenient().when(mockValidationResultSet.next()).thenReturn(true);
    }

    @Test
    void testValidateSqlQuery_NullQuery() {
        SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(null, 100));
        assertEquals("SQL query cannot be empty", exception.getMessage());
    }

    @Test
    void testValidateSqlQuery_EmptyString() {
        SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql("", 100));
        assertEquals("SQL query cannot be empty", exception.getMessage());
    }

    @Test
    void testValidateSqlQuery_WhitespaceOnly() {
        String[] whitespaceQueries = {
            "   ",
            "\t\t\t",
            "\n\n\n",
            " \t \n \r ",
            "  \t\n\r  "
        };

        for (String query : whitespaceQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertEquals("SQL query cannot be empty", exception.getMessage());
        }
    }

    @Test
    void testValidateSqlQuery_DropOperation() {
        String[] dropQueries = {
            "DROP TABLE test",
            "drop table test",
            "Drop Table test",
            "DROP VIEW test_view",
            "DROP INDEX test_idx",
            "DROP DATABASE testdb",
            "DROP SCHEMA test_schema",
            "DROP PROCEDURE test_proc",
            "DROP FUNCTION test_func",
            "DROP TRIGGER test_trigger",
            "DROP USER test_user",
            "DROP ROLE test_role"
        };

        for (String query : dropQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertTrue(exception.getMessage().contains("Operation not allowed: DROP"));
        }
    }

    @Test
    void testValidateSqlQuery_TruncateOperation() {
        String[] truncateQueries = {
            "TRUNCATE TABLE test",
            "truncate table test",
            "Truncate Table test",
            "TRUNCATE test"
        };

        for (String query : truncateQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertTrue(exception.getMessage().contains("Operation not allowed: TRUNCATE"));
        }
    }

    @Test
    void testValidateSqlQuery_DeleteOperation() {
        String[] deleteQueries = {
            "DELETE FROM test",
            "delete from test",
            "Delete From test",
            "DELETE FROM test WHERE id = 1",
            "DELETE test WHERE condition = true"
        };

        for (String query : deleteQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertTrue(exception.getMessage().contains("Operation not allowed: DELETE"));
        }
    }

    @Test
    void testValidateSqlQuery_UpdateOperation() {
        String[] updateQueries = {
            "UPDATE test SET col = 1",
            "update test set col = 1",
            "Update Test Set col = 1",
            "UPDATE test SET col1 = 1, col2 = 2 WHERE id = 1"
        };

        for (String query : updateQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertTrue(exception.getMessage().contains("Operation not allowed: UPDATE"));
        }
    }

    @Test
    void testValidateSqlQuery_InsertOperation() {
        String[] insertQueries = {
            "INSERT INTO test VALUES (1)",
            "insert into test values (1)",
            "Insert Into test Values (1)",
            "INSERT INTO test (col1, col2) VALUES (1, 2)",
            "INSERT INTO test SELECT * FROM other_table"
        };

        for (String query : insertQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertTrue(exception.getMessage().contains("Operation not allowed: INSERT"));
        }
    }

    @Test
    void testValidateSqlQuery_CreateOperation() {
        String[] createQueries = {
            "CREATE TABLE test (id INT)",
            "create table test (id int)",
            "Create Table test (id INT)",
            "CREATE VIEW test_view AS SELECT * FROM test",
            "CREATE INDEX idx_test ON test(id)",
            "CREATE PROCEDURE test_proc() BEGIN END",
            "CREATE FUNCTION test_func() RETURNS INT",
            "CREATE TRIGGER test_trigger BEFORE INSERT ON test"
        };

        for (String query : createQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertTrue(exception.getMessage().contains("Operation not allowed: CREATE"));
        }
    }

    @Test
    void testValidateSqlQuery_AlterOperation() {
        String[] alterQueries = {
            "ALTER TABLE test ADD COLUMN col2 VARCHAR(50)",
            "alter table test add column col2 varchar(50)",
            "Alter Table test Add Column col2 VARCHAR(50)",
            "ALTER TABLE test DROP COLUMN col1",
            "ALTER TABLE test MODIFY COLUMN col1 INT",
            "ALTER TABLE test RENAME TO new_test",
            "ALTER INDEX idx_test RENAME TO new_idx"
        };

        for (String query : alterQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertTrue(exception.getMessage().contains("Operation not allowed: ALTER"));
        }
    }

    @Test
    void testValidateSqlQuery_GrantOperation() {
        String[] grantQueries = {
            "GRANT SELECT ON test TO user",
            "grant select on test to user",
            "Grant Select On test To user",
            "GRANT ALL PRIVILEGES ON test TO user",
            "GRANT INSERT, UPDATE ON test TO user"
        };

        for (String query : grantQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertTrue(exception.getMessage().contains("Operation not allowed: GRANT"));
        }
    }

    @Test
    void testValidateSqlQuery_RevokeOperation() {
        String[] revokeQueries = {
            "REVOKE SELECT ON test FROM user",
            "revoke select on test from user",
            "Revoke Select On test From user",
            "REVOKE ALL PRIVILEGES ON test FROM user"
        };

        for (String query : revokeQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertTrue(exception.getMessage().contains("Operation not allowed: REVOKE"));
        }
    }

    @Test
    void testValidateSqlQuery_ExecOperation() {
        String[] execQueries = {
            "EXEC sp_test",
            "exec sp_test",
            "Exec sp_test",
            "EXEC sp_test @param1 = 1",
            "EXEC dbo.sp_test"
        };

        for (String query : execQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertTrue(exception.getMessage().contains("Operation not allowed: EXEC"));
        }
    }

    @Test
    void testValidateSqlQuery_ExecuteOperation() {
        String[] executeQueries = {
            "EXECUTE sp_test",
            "execute sp_test",
            "Execute sp_test",
            "EXECUTE sp_test @param1 = 1"
        };

        for (String query : executeQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertTrue(exception.getMessage().contains("Operation not allowed: EXECUTE"));
        }
    }

    @Test
    void testValidateSqlQuery_CallOperation() {
        String[] callQueries = {
            "CALL sp_test()",
            "call sp_test()",
            "Call sp_test()",
            "CALL sp_test(1, 2, 3)",
            "CALL schema.sp_test()"
        };

        for (String query : callQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertTrue(exception.getMessage().contains("Operation not allowed: CALL"));
        }
    }

    @Test
    void testValidateSqlQuery_SingleWordDangerousOperations() {
        String[] singleWordQueries = {
            "drop",
            "DROP",
            "truncate",
            "TRUNCATE",
            "delete",
            "DELETE",
            "update",
            "UPDATE",
            "insert",
            "INSERT",
            "create",
            "CREATE",
            "alter",
            "ALTER",
            "grant",
            "GRANT",
            "revoke",
            "REVOKE",
            "exec",
            "EXEC",
            "execute",
            "EXECUTE",
            "call",
            "CALL"
        };

        for (String query : singleWordQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertTrue(exception.getMessage().contains("Operation not allowed: " + query.toUpperCase()));
        }
    }

    @Test
    void testValidateSqlQuery_MultipleStatements() {
        // Use only SELECT statements (safe operations) to test multiple statement detection
        String[] multiStatementQueries = {
                "SELECT 1; SELECT 2",                           // No trailing semicolon
                "SELECT * FROM test; SELECT COUNT(*) FROM test", // No trailing semicolon
                "SELECT col1 FROM test; SELECT col2 FROM test"   // No trailing semicolon
        };

        for (String query : multiStatementQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));

            assertEquals("Multiple statements not allowed", exception.getMessage());
        }
    }

    @Test
    void testValidateSqlQuery_LineComments() {
        String[] commentQueries = {
            "SELECT * FROM test -- this is a comment",
            "SELECT * FROM test--comment",
            "-- SELECT * FROM test",
            "SELECT col1 FROM test -- comment here",
            "SELECT * FROM test WHERE id = 1 -- filter comment"
        };

        for (String query : commentQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertEquals("SQL comments not allowed", exception.getMessage());
        }
    }

    @Test
    void testValidateSqlQuery_BlockComments() {
        String[] commentQueries = {
            "SELECT * FROM test /* this is a comment */",
            "SELECT * FROM test/*comment*/",
            "/* SELECT * FROM test */",
            "SELECT col1 FROM test /* comment here */",
            "SELECT * /* comment */ FROM test",
            "SELECT * FROM test WHERE id = 1 /* filter comment */"
        };

        for (String query : commentQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertEquals("SQL comments not allowed", exception.getMessage());
        }
    }

    @Test
    void testValidateSqlQuery_ValidSelectWithSemicolon() throws SQLException {
        // Make the statement execution fail after validation passes
        doThrow(new SQLException("Table not found")).when(mockStatement).execute();

        // SELECT queries ending with semicolon should be allowed
        String[] validQueries = {
                "SELECT * FROM test;",
                "SELECT col1, col2 FROM test WHERE id = 1;",
                "SELECT COUNT(*) FROM test;"
        };

        // These should not throw validation exceptions (though they may fail for other reasons like missing connection)
        for (String query : validQueries) {
            // Since we're only testing validation, we expect SQLException but not validation-specific messages
            SQLException exception = TestUtils.withSuppressedLogging(() ->
                assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100))
            );

            // Should not be validation errors
            assertFalse(exception.getMessage().contains("Operation not allowed"));
            assertFalse(exception.getMessage().contains("Multiple statements not allowed"));
            assertFalse(exception.getMessage().contains("SQL comments not allowed"));
            assertFalse(exception.getMessage().contains("SQL query cannot be empty"));
        }
    }

    @Test
    void testValidateSqlQuery_SelectOnlyDisabled() throws SQLException {
        // When SELECT_ONLY is false, validation should be bypassed
        // Make execution fail to simulate database error after bypassing validation
        doThrow(new SQLException("Table not found")).when(mockStatement).execute();

        String[] dangerousQueries = {
                "DROP TABLE test",
                "DELETE FROM test",
                "UPDATE test SET col = 1"
        };

        for (String query : dangerousQueries) {
            // Since we're only testing validation, we expect SQLException but not validation-specific messages
            SQLException exception = TestUtils.withSuppressedLogging(() ->
                    assertThrows(SQLException.class, () -> nonSelectOnlyService.executeSql(query, 100))
            );

            // Should NOT be validation errors since validation is bypassed when selectOnly=false
            assertFalse(exception.getMessage().startsWith("Operation not allowed:"));

            // Should be execution error instead
            assertEquals("Table not found", exception.getMessage());
        }
    }

    @Test
    void testValidateSqlQuery_EdgeCaseSpacing() {
        String[] spacingQueries = {
            "  DROP  TABLE  test  ",
            "\tDELETE\tFROM\ttest\t",
            "\nUPDATE\ntest\nSET\ncol=1\n",
            " \t\n DROP \t\n TABLE \t\n test \t\n "
        };

        for (String query : spacingQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertTrue(exception.getMessage().startsWith("Operation not allowed:"));
        }
    }

    @Test
    void testValidateSqlQuery_CaseInsensitiveDangerousOperations() {
        String[] mixedCaseQueries = {
            "Drop Table test",
            "dRoP tAbLe test",
            "DELETE from test",
            "DeLeTe FrOm test",
            "Update test SET col=1",
            "UpDaTe test set col=1",
            "Insert Into test VALUES(1)",
            "InSeRt InTo test values(1)"
        };

        for (String query : mixedCaseQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            assertTrue(exception.getMessage().startsWith("Operation not allowed:"));
        }
    }

    @Test
    void testValidateSqlQuery_SqlInjectionPatterns() {
        String[] injectionQueries = {
            "SELECT * FROM test; DROP TABLE users; --",
            "SELECT * FROM test WHERE id = 1; DELETE FROM users; /*",
            "SELECT * FROM test/* comment */; UPDATE users SET password = 'hacked';",
            "SELECT * FROM test -- comment\n; TRUNCATE TABLE important_data;"
        };

        for (String query : injectionQueries) {
            SQLException exception = assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100));
            // Should fail on either multiple statements or comments
            assertTrue(exception.getMessage().equals("Multiple statements not allowed") ||
                      exception.getMessage().equals("SQL comments not allowed"));
        }
    }
    @Test
    void testValidateSqlQuery_ComplexValidQueries() {
        // Override the mock behavior for this specific test
        try {
            when(mockStatement.execute()).thenThrow(new SQLException("Table not found"));
        } catch (SQLException e) {
            // Handle checked exception from when() if needed
        }

        // These are complex but valid SELECT queries that should pass validation
        String[] complexValidQueries = {
                "SELECT t1.col1, t2.col2 FROM table1 t1 JOIN table2 t2 ON t1.id = t2.id",
                "SELECT COUNT(*) as count, AVG(price) as avg_price FROM products WHERE category = 'electronics'",
                "SELECT * FROM (SELECT id, name FROM users WHERE active = 1) AS active_users",
                "SELECT DISTINCT category FROM products ORDER BY category ASC",
                "SELECT * FROM test WHERE id IN (SELECT user_id FROM orders WHERE total > 100)"
        };

        for (String query : complexValidQueries) {
            // Since we're only testing validation, we expect SQLException but not validation-specific messages
            SQLException exception = TestUtils.withSuppressedLogging(() ->
                    assertThrows(SQLException.class, () -> databaseService.executeSql(query, 100))
            );

            // Should not be validation errors
            assertFalse(exception.getMessage().contains("Operation not allowed"));
            assertFalse(exception.getMessage().contains("Multiple statements not allowed"));
            assertFalse(exception.getMessage().contains("SQL comments not allowed"));
            assertFalse(exception.getMessage().contains("SQL query cannot be empty"));
        }
    }
}
