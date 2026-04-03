# Database MCP Server - Project Structure

## Directory Layout

```
database-mcp-server/
├── pom.xml                                      # Maven project configuration
├── README.md                                    # Project documentation
├── test-mcp-protocol.py                         # MCP Protocol Test script
├── test-mcp-server.py                           # MCP Server Test script
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── skanga/
│   │   │           ├── DatabaseMcpServer.java   # Main server class
│   │   │           ├── DatabaseConfig.java      # Configuration
│   │   │           ├── DatabaseService.java     # Database operations
│   │   │           ├── DatabaseResource.java    # Resource representation
│   │   │           └── QueryResult.java         # Query results
│   │   └── resources/
│   │       └── application.properties           # Configuration file (optional)
│   └── test/
│       └── java/
│           └── com/
│               └── skanga/
│                   └── (test files)
└── target/                                      # Build output (created by Maven)
    └── dbchat-4.1.0.jar                         # Executable JAR
```

## Key Components

### 1. **DatabaseMcpServer.java** - Main Entry Point
- Implements MCP protocol (JSON-RPC over stdio)
- Handles all MCP methods: initialize, tools/list, tools/call, resources/list, resources/read
- Manages request/response processing
- Error handling and logging

### 2. **DatabaseService.java** - Database Operations
- JDBC connection pool & connenction management
- SQL query execution with result formatting
- Database metadata discovery (tables, views, schemas)
- Resource content generation
- Multi-database support through JDBC drivers

### 3. **DatabaseConfig.java** - Configuration Management
- Database connection parameters
- Timeout and pooling settings
- Database type detection
- Environment variable support

### 4. **DatabaseResource.java** - Resource Model
- Represents database objects as MCP resources
- URI-based resource identification
- Metadata and content management

### 5. **QueryResult.java** - Query Result Model
- Structured query results
- Column metadata and row data
- Execution timing information
- Formatted output support

