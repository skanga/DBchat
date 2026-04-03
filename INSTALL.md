# DBChat - Developer Installation Guide
## Building and Deployment Documentation for Database MCP Server

This guide is for developers who need to build DBChat from source, understand build profiles, configure advanced deployment scenarios, or contribute to the project.

**End users**: See [README.md](README.md) for simple setup instructions using pre-built JAR files.

## 📋 Prerequisites

### Required Software
- **Java 17 or higher** - [OpenJDK](https://openjdk.org/) or [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
- **Maven 3.6 or higher** - [Apache Maven](https://maven.apache.org/download.cgi)
- **Git** - [git-scm.com](https://git-scm.com/)

### Verify Installation
```bash
java -version    # Must show 17+
mvn -version     # Must show 3.6+
git --version
```

## 🏗️ Building from Source

### Clone Repository
```bash
git clone https://github.com/skanga/dbchat.git
cd dbchat
```

### Understanding Build Profiles

DBChat uses Maven profiles to manage JDBC driver dependencies. This approach keeps JAR sizes manageable while supporting a wide range of databases.

#### Default Profile (No additional flags)
**Included drivers:**
- H2 Database Engine - In-memory and file-based database
- SQLite JDBC Driver - Lightweight file-based database
- PostgreSQL JDBC Driver - Advanced open-source database
- CSV JDBC Driver - Query CSV files as database tables

**Build command:**
```bash
mvn clean package
```

**Resulting JAR:** `target/dbchat-4.1.0.jar` (~15MB)

#### Standard Databases Profile (`-P standard-databases`)
**Additional drivers:**
- MySQL Connector/J - Popular open-source database
- MariaDB Connector/J - MySQL-compatible database
- ClickHouse JDBC Driver - Column-oriented analytics database

**Build command:**
```bash
mvn clean package -P standard-databases
```

**Resulting JAR:** `target/dbchat-4.1.0.jar` (~25MB)

#### Enterprise Databases Profile (`-P enterprise-databases`)
**Additional drivers:**
- Oracle JDBC Driver (ojdbc11) - Enterprise database system
- Microsoft SQL Server JDBC Driver - Microsoft database system
- IBM DB2 JDBC Driver - IBM enterprise database

**Build command:**
```bash
mvn clean package -P standard-databases,enterprise-databases
```

**Resulting JAR:** `target/dbchat-4.1.0.jar` (~60MB)

**Note:** Enterprise drivers require accepting additional licenses. Ensure compliance with vendor licensing terms.

#### Cloud Analytics Profile (`-P cloud-analytics`)
**Additional drivers:**
- Amazon Redshift JDBC Driver - AWS data warehouse
- Snowflake JDBC Driver - Cloud data platform
- Databricks JDBC Driver - Cloud data platform
- Google BigQuery JDBC Driver - Google's analytics database

**Build command:**
```bash
mvn clean package -P standard-databases,cloud-analytics
```

**Resulting JAR:** `target/dbchat-4.1.0.jar` (~80MB)

#### Big Data Profile (`-P big-data`)
**Additional drivers:**
- Apache Hive JDBC Driver - Data warehouse software
- MongoDB JDBC Driver - Document database (experimental)
- Apache Cassandra JDBC Driver - NoSQL database
- Apache Spark SQL JDBC Driver - Unified analytics engine

**Build command:**
```bash
mvn clean package -P standard-databases,big-data
```

**Resulting JAR:** `target/dbchat-4.1.0.jar` (~120MB)

#### Complete Build (All Profiles)
```bash
mvn clean package -P standard-databases,enterprise-databases,cloud-analytics,big-data
```

**Resulting JAR:** `target/dbchat-4.1.0.jar` (~400MB)

### Custom Profile Combinations

You can combine profiles as needed:

```bash
# Standard + Enterprise (no Cloud)
mvn clean package -P standard-databases,enterprise-databases

# Analytics databases only
mvn clean package -P cloud-analytics

# Standard + Cloud (no enterprise)
mvn clean package -P standard-databases,cloud-analytics
```

### Maven Profile Details

#### Profile Dependencies Matrix

| Database | Default | standard-databases | enterprise-databases | cloud-analytics | big-data |
|----------|---------|-------------------|---------------------|----------------|----------|
| H2 | ✓ | ✓ | ✓ | ✓ | ✓ |
| SQLite | ✓ | ✓ | ✓ | ✓ | ✓ |
| PostgreSQL | ✓ | ✓ | ✓ | ✓ | ✓ |
| CSV Files | ✓ | ✓ | ✓ | ✓ | ✓ |
| MySQL | | ✓ | ✓ | ✓ | ✓ |
| MariaDB | | ✓ | ✓ | ✓ | ✓ |
| ClickHouse | | ✓ | ✓ | ✓ | ✓ |
| Oracle | | | ✓ | | |
| SQL Server | | | ✓ | | |
| IBM DB2 | | | ✓ | | |
| Redshift | | | | ✓ | |
| Snowflake | | | | ✓ | |
| Databricks | | | | ✓ | |
| BigQuery | | | | ✓ | |
| Hive | | | | | ✓ |
| MongoDB | | | | | ✓ |
| Cassandra | | | | | ✓ |
| Redis | | | | | ✓ |
| Spark SQL | | | | | ✓ |

### Build Verification

```bash
# Check that the JAR file was created
ls -la target/dbchat-4.1.0.jar

# Check included drivers
jar tf target/dbchat-4.1.0.jar | grep -E "\.(jar|class)" | grep -E "(mysql|postgres|oracle)"

# Quick test to see if it starts
java -jar target/dbchat-4.1.0.jar

Ctrl-C to stop it
```

## 🗄️ Database-Specific Setup

### In-Memory Databases (Development/Testing)

#### H2 In-Memory Database
```bash
# No setup required - embedded
export DB_URL="jdbc:h2:mem:testdb"
export DB_USER="sa"
export DB_PASSWORD=""
export DB_DRIVER="org.h2.Driver"
```

#### H2 File-Based Database
```bash
export DB_URL="jdbc:h2:file:./data/testdb"
export DB_USER="sa"
export DB_PASSWORD=""
export DB_DRIVER="org.h2.Driver"
```

### File-Based Databases

#### SQLite
```bash
export DB_URL="jdbc:sqlite:/absolute/path/to/database.db"
export DB_USER=""
export DB_PASSWORD=""
export DB_DRIVER="org.sqlite.JDBC"
```

#### CSV Files
```bash
# Single CSV file
export DB_URL="jdbc:relique:csv:/path/to/file.csv"
export DB_USER=""
export DB_PASSWORD=""
export DB_DRIVER="org.relique.jdbc.csv.CsvDriver"

# Directory of CSV files
export DB_URL="jdbc:relique:csv:/path/to/csv/directory"
export DB_USER=""
export DB_PASSWORD=""
export DB_DRIVER="org.relique.jdbc.csv.CsvDriver"

# ZIP file containing CSVs
export DB_URL="jdbc:relique:csv:zip:/path/to/archive.zip"
export DB_USER=""
export DB_PASSWORD=""
export DB_DRIVER="org.relique.jdbc.csv.CsvDriver"

# Custom separator and file extension
export DB_URL="jdbc:relique:csv:/path/to/data?separator=;&fileExtension=.txt"
export DB_USER=""
export DB_PASSWORD=""
export DB_DRIVER="org.relique.jdbc.csv.CsvDriver"
```

### Standard Databases

#### MySQL
```bash
# Docker setup
docker run -d --name mysql-test \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=testdb \
  -e MYSQL_USER=mcpuser \
  -e MYSQL_PASSWORD=mcppassword \
  mysql:8.0

# Connection configuration
export DB_URL="jdbc:mysql://localhost:3306/testdb?useSSL=false&serverTimezone=UTC"
export DB_USER="mcpuser"
export DB_PASSWORD="mcppassword"
export DB_DRIVER="com.mysql.cj.jdbc.Driver"
```

#### MariaDB
```bash
# Docker setup
docker run -d --name mariadb-test \
  -p 3307:3306 \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=testdb \
  -e MYSQL_USER=mcpuser \
  -e MYSQL_PASSWORD=mcppassword \
  mariadb:10.6

# Connection configuration
export DB_URL="jdbc:mariadb://localhost:3307/testdb"
export DB_USER="mcpuser"
export DB_PASSWORD="mcppassword"
export DB_DRIVER="org.mariadb.jdbc.Driver"
```

#### PostgreSQL
```bash
# Docker setup
docker run -d --name postgres-test \
  -p 5432:5432 \
  -e POSTGRES_DB=testdb \
  -e POSTGRES_USER=mcpuser \
  -e POSTGRES_PASSWORD=mcppassword \
  postgres:15

# Connection configuration
export DB_URL="jdbc:postgresql://localhost:5432/testdb"
export DB_USER="mcpuser"
export DB_PASSWORD="mcppassword"
export DB_DRIVER="org.postgresql.Driver"
```

#### ClickHouse
```bash
# Docker setup
docker run -d --name clickhouse-test \
  -p 8123:8123 \
  -p 9000:9000 \
  clickhouse/clickhouse-server

# Connection configuration
export DB_URL="jdbc:clickhouse://localhost:8123/default"
export DB_USER="default"
export DB_PASSWORD=""
export DB_DRIVER="com.clickhouse.jdbc.ClickHouseDriver"
```

### Enterprise Databases

#### Oracle Database
```bash
# Docker setup (requires Oracle account)
docker run -d --name oracle-test \
  -p 1521:1521 \
  -e ORACLE_PASSWORD=password \
  container-registry.oracle.com/database/express:latest

# Connection configuration
export DB_URL="jdbc:oracle:thin:@localhost:1521:xe"
export DB_USER="system"
export DB_PASSWORD="password"
export DB_DRIVER="oracle.jdbc.driver.OracleDriver"
```

#### SQL Server
```bash
# Docker setup
docker run -d --name sqlserver-test \
  -e "ACCEPT_EULA=Y" \
  -e "SA_PASSWORD=StrongPassword123!" \
  -p 1433:1433 \
  mcr.microsoft.com/mssql/server:2019-latest

# Connection configuration
export DB_URL="jdbc:sqlserver://localhost:1433;databaseName=master;encrypt=false"
export DB_USER="sa"
export DB_PASSWORD="StrongPassword123!"
export DB_DRIVER="com.microsoft.sqlserver.jdbc.SQLServerDriver"
```

#### IBM DB2
```bash
# Docker setup
docker run -d --name db2-test \
  -e LICENSE=accept \
  -e DB2INST1_PASSWORD=password \
  -e DBNAME=testdb \
  -p 50000:50000 \
  ibmcom/db2

# Connection configuration
export DB_URL="jdbc:db2://localhost:50000/testdb"
export DB_USER="db2inst1"
export DB_PASSWORD="password"
export DB_DRIVER="com.ibm.db2.jcc.DB2Driver"
```

### Cloud Analytics Databases

#### Amazon Redshift
```bash
export DB_URL="jdbc:redshift://your-cluster.region.redshift.amazonaws.com:5439/database"
export DB_USER="username"
export DB_PASSWORD="password"
export DB_DRIVER="com.amazon.redshift.jdbc42.Driver"
```

#### Snowflake
```bash
export DB_URL="jdbc:snowflake://account.snowflakecomputing.com/?warehouse=warehouse&db=database&schema=schema"
export DB_USER="username"
export DB_PASSWORD="password"
export DB_DRIVER="net.snowflake.client.jdbc.SnowflakeDriver"
```

#### Databricks
```bash
export DB_URL="jdbc:spark://your-workspace.cloud.databricks.com:443/default;transportMode=http;ssl=1;httpPath=/sql/1.0/warehouses/warehouse-id"
export DB_USER="token"
export DB_PASSWORD="your-personal-access-token"
export DB_DRIVER="com.simba.spark.jdbc.Driver"
```

#### Google BigQuery
```bash
export DB_URL="jdbc:bigquery://https://www.googleapis.com/bigquery/v2:443;ProjectId=project-id;OAuthType=0;OAuthServiceAcctEmail=service@project.iam.gserviceaccount.com;OAuthPvtKeyPath=/path/to/key.json"
export DB_USER=""
export DB_PASSWORD=""
export DB_DRIVER="com.simba.googlebigquery.jdbc42.Driver"
```

### Big Data Databases

#### Apache Hive
```bash
export DB_URL="jdbc:hive2://localhost:10000/default"
export DB_USER="hive"
export DB_PASSWORD=""
export DB_DRIVER="org.apache.hive.jdbc.HiveDriver"
```

#### MongoDB (via JDBC)
```bash
export DB_URL="jdbc:mongodb://localhost:27017/database"
export DB_USER="username"
export DB_PASSWORD="password"
export DB_DRIVER="com.mongodb.jdbc.MongoDriver"
```

#### Redis (via JDBC)
```bash
# Docker setup
docker run -d --name redis-test -p 6379:6379 redis

# Connection configuration
export DB_URL="jdbc:redis://localhost:6379"
export DB_USER=""
export DB_PASSWORD="" # Set if your Redis instance requires a password
export DB_DRIVER="com.dbvis.jdbc.redis.RedisDriver"
```

## 🚀 Transport Modes and Configuration

### Transport Mode Architecture

DBChat supports two transport modes for different deployment scenarios:

#### stdio mode (Default)
- **Use case**: MCP Clients like Claude Desktop, Gemini-CLI, etc & integration with command-line tools
- **Protocol**: JSON-RPC 2.0 over stdin/stdout
- **Security**: Local process communication only
- **Performance**: Direct process communication (fastest)

#### HTTP mode
- **Use case**: Web applications, remote access, API integration
- **Protocol**: JSON-RPC 2.0 over HTTP POST
- **Security**: HTTP with CORS support, local binding recommended, remote binding supports
- **Performance**: HTTP overhead, suitable for web integration

### Development Server Modes

#### stdio Development Mode
```bash
# Basic stdio mode
java -jar target/dbchat-4.1.0.jar

# Debug mode with detailed logging
java -Dlogging.level.root=DEBUG -jar target/dbchat-4.1.0.jar

# Test with manual input
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}' | java -jar target/dbchat-4.1.0.jar
```

#### HTTP Development Mode
```bash
# Basic HTTP mode
java -jar target/dbchat-4.1.0.jar --http_mode=true

# Custom port with debug logging
java -Dlogging.level.root=DEBUG -jar target/dbchat-4.1.0.jar --http_mode=true --http_port=9090

# Production-like settings
java -jar target/dbchat-4.1.0.jar \
  --http_mode=true \
  --http_port=8080 \
  --max_connections=50 \
  --query_timeout_seconds=120
```

### Configuration File Support

Create advanced configuration files for complex deployments:

#### Example: `production.conf`
```properties
# Database Connection
DB_URL=jdbc:postgresql://prod-db:5432/analytics
DB_USER=readonly_user
DB_PASSWORD="complex password with spaces"
DB_DRIVER=org.postgresql.Driver

# Connection Pool Settings
MAX_CONNECTIONS=50
CONNECTION_TIMEOUT_MS=60000
IDLE_TIMEOUT_MS=300000
MAX_LIFETIME_MS=1800000
LEAK_DETECTION_THRESHOLD_MS=60000

# Query Settings
QUERY_TIMEOUT_SECONDS=120
SELECT_ONLY=true
MAX_SQL_LENGTH=100000
MAX_ROWS_LIMIT=50000

# Transport Settings
HTTP_MODE=true
HTTP_PORT=8080
```

#### Example: `development.conf`
```properties
# Development H2 Database
DB_URL=jdbc:h2:mem:devdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
DB_USER=sa
DB_PASSWORD=
DB_DRIVER=org.h2.Driver

# Relaxed settings for development
MAX_CONNECTIONS=5
CONNECTION_TIMEOUT_MS=30000
QUERY_TIMEOUT_SECONDS=60
SELECT_ONLY=false
MAX_SQL_LENGTH=50000
MAX_ROWS_LIMIT=10000

# HTTP mode for web development
HTTP_MODE=true
HTTP_PORT=3001
```

#### Usage:
```bash
# Use specific config file
java -jar target/dbchat-4.1.0.jar --config_file=production.conf

# Override specific settings
java -jar target/dbchat-4.1.0.jar --config_file=production.conf --http_port=9090

# Multiple config files for different environments
java -jar target/dbchat-4.1.0.jar --config_file=base.conf --config_file=env-specific.conf
```

### Advanced Configuration Options

#### Connection Pool Tuning
```
# High-throughput settings
MAX_CONNECTIONS=100
CONNECTION_TIMEOUT_MS=10000
IDLE_TIMEOUT_MS=120000
MAX_LIFETIME_MS=900000
LEAK_DETECTION_THRESHOLD_MS=30000

# Conservative settings
MAX_CONNECTIONS=5
CONNECTION_TIMEOUT_MS=60000
IDLE_TIMEOUT_MS=600000
MAX_LIFETIME_MS=1800000
LEAK_DETECTION_THRESHOLD_MS=120000
```

#### Security Configuration
```
# Maximum security
SELECT_ONLY=true
MAX_SQL_LENGTH=5000
MAX_ROWS_LIMIT=100
QUERY_TIMEOUT_SECONDS=15

# Development flexibility
SELECT_ONLY=false
MAX_SQL_LENGTH=100000
MAX_ROWS_LIMIT=50000
QUERY_TIMEOUT_SECONDS=300
```

#### Performance Tuning
```
# High-performance settings
MAX_CONNECTIONS=50
CONNECTION_TIMEOUT_MS=5000
QUERY_TIMEOUT_SECONDS=300
MAX_ROWS_LIMIT=100000

# Memory-constrained settings
MAX_CONNECTIONS=3
CONNECTION_TIMEOUT_MS=30000
QUERY_TIMEOUT_SECONDS=30
MAX_ROWS_LIMIT=1000
```

## 🧪 Testing and Validation

Heavy unit testing has been implemented with almost 300 tests and coverage of over 90% of all lines of code.

### Unit Testing
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=McpServerTest

# Run tests with specific profile
mvn test -P standard-databases

# Generate test reports
mvn test jacoco:report
```

### Integration Testing
```bash
# Full integration test suite
mvn verify

# Test with specific database profiles
mvn verify -P standard-databases
mvn verify -P enterprise-databases

# Test with Docker databases
mvn verify -P integration-tests
```

### Manual Protocol Testing

#### stdio Mode Testing
```bash
# Initialize protocol
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}' | java -jar target/dbchat-4.1.0.jar

# Send initialized notification
echo '{"jsonrpc":"2.0","method":"notifications/initialized"}' | java -jar target/dbchat-4.1.0.jar

# List tools
echo '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}' | java -jar target/dbchat-4.1.0.jar

# Execute query
echo '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"query","arguments":{"sql":"SELECT 1 as test","maxRows":1}}}' | java -jar target/dbchat-4.1.0.jar

# List resources
echo '{"jsonrpc":"2.0","id":4,"method":"resources/list","params":{}}' | java -jar target/dbchat-4.1.0.jar
```

#### HTTP Mode Testing
```bash
# Start server
java -jar target/dbchat-4.1.0.jar --http_mode=true &
SERVER_PID=$!

# Wait for startup
sleep 2

# Health check
curl -s http://localhost:8080/health | jq

# Initialize protocol
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}' | jq

# Send initialized notification
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"notifications/initialized"}'

# List tools
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}' | jq

# Execute query
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"query","arguments":{"sql":"SELECT 1 as test","maxRows":1}}}' | jq

# List resources
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":4,"method":"resources/list","params":{}}' | jq

# Cleanup
kill $SERVER_PID
```

### Automated Test Scripts

#### Python Testing Script (`test_mcp.py`)
```python
#!/usr/bin/env python3
import json
import subprocess
import sys
import time
import requests

def test_stdio_mode():
    """Test stdio mode functionality"""
    print("Testing stdio mode...")
    
    # Test initialize
    init_request = {
        "jsonrpc": "2.0",
        "id": 1,
        "method": "initialize",
        "params": {
            "protocolVersion": "2025-03-26",
            "capabilities": {},
            "clientInfo": {"name": "test", "version": "1.0"}
        }
    }
    
    result = subprocess.run(
        ["java", "-jar", "target/dbchat-4.1.0.jar"],
        input=json.dumps(init_request),
        text=True,
        capture_output=True
    )
    
    if result.returncode == 0:
        response = json.loads(result.stdout)
        assert response["jsonrpc"] == "2.0"
        assert "result" in response
        print("✓ stdio mode initialize test passed")
    else:
        print(f"✗ stdio mode test failed: {result.stderr}")
        sys.exit(1)

def test_http_mode():
    """Test HTTP mode functionality"""
    print("Testing HTTP mode...")
    
    # Start server
    process = subprocess.Popen([
        "java", "-jar", "target/dbchat-4.1.0.jar", 
        "--http_mode=true", "--http_port=8081"
    ])
    
    time.sleep(3)  # Wait for startup
    
    try:
        # Health check
        response = requests.get("http://localhost:8081/health")
        assert response.status_code == 200
        print("✓ HTTP health check passed")
        
        # MCP initialize
        init_request = {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {
                "protocolVersion": "2025-03-26",
                "capabilities": {},
                "clientInfo": {"name": "test", "version": "1.0"}
            }
        }
        
        response = requests.post(
            "http://localhost:8081/mcp",
            json=init_request
        )
        assert response.status_code == 200
        assert response.json()["jsonrpc"] == "2.0"
        print("✓ HTTP MCP initialize test passed")
        
    finally:
        process.terminate()
        process.wait()

if __name__ == "__main__":
    test_stdio_mode()
    test_http_mode()
    print("All tests passed!")
```

#### Bash Testing Script (`test_build.sh`)
```bash
#!/bin/bash
set -e

echo "Testing DBChat build and functionality..."

# Test basic build
echo "Testing basic build..."
mvn clean package -q
test -f target/dbchat-4.1.0.jar || (echo "Build failed" && exit 1)
echo "✓ Basic build successful"

# Test standard databases build
echo "Testing standard databases build..."
mvn clean package -P standard-databases -q
test -f target/dbchat-4.1.0.jar || (echo "Standard build failed" && exit 1)
echo "✓ Standard databases build successful"

# Test enterprise databases build (if available)
echo "Testing enterprise databases build..."
if mvn clean package -P standard-databases,enterprise-databases -q 2>/dev/null; then
    echo "✓ Enterprise databases build successful"
else
    echo "⚠ Enterprise databases build skipped (drivers may not be available)"
fi

# Test basic functionality
echo "Testing basic functionality..."
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}' | \
    timeout 10s java -jar target/dbchat-4.1.0.jar > /dev/null || (echo "Functionality test failed" && exit 1)
echo "✓ Basic functionality test passed"

echo "All tests completed successfully!"
```

## 🐳 Docker Deployment

### Development Docker Setup

#### Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy built JAR
COPY target/dbchat-4.1.0.jar

# Create non-root user
RUN useradd -m -u 1000 dbchat
USER dbchat

# Expose HTTP port
EXPOSE 8080

# Default command
CMD ["java", "-jar", "dbchat.jar", "--http_mode=true", "--http_port=8080"]
```

#### Docker Compose for Development
```yaml
version: '3.8'
services:
  dbchat:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DB_URL=jdbc:postgresql://postgres:5432/testdb
      - DB_USER=mcpuser
      - DB_PASSWORD=mcppassword
      - DB_DRIVER=org.postgresql.Driver
      - HTTP_MODE=true
      - HTTP_PORT=8080
    depends_on:
      - postgres
    networks:
      - dbchat-network

  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=testdb
      - POSTGRES_USER=mcpuser
      - POSTGRES_PASSWORD=mcppassword
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - dbchat-network

volumes:
  postgres_data:

networks:
  dbchat-network:
```

#### Usage
```bash
# Build and start services
docker-compose up --build

# Test the setup
curl http://localhost:8080/health

# Stop services
docker-compose down
```

### Production Docker Setup

#### Multi-stage Production Dockerfile
```dockerfile
# Build stage
FROM maven:3.8.6-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -P standard-databases -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim AS runtime
WORKDIR /app

# Install security updates
RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN useradd -m -u 1000 dbchat

# Copy JAR from build stage
COPY --from=build /app/target/dbchat-4.1.0.jar

# Change ownership
RUN chown dbchat:dbchat dbchat.jar

USER dbchat

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

EXPOSE 8080

CMD ["java", "-jar", "dbchat.jar", "--http_mode=true", "--http_port=8080"]
```

## 🔧 IDE Configuration

### IntelliJ IDEA Setup

#### Import Project
1. **Open IntelliJ IDEA**
2. **Import Project** → Select `pom.xml`
3. **Import as Maven project**
4. **Set Project SDK** → Java 17+

#### Run Configurations

**stdio Mode Configuration:**
- **Main class**: `com.skanga.mcp.McpServer`
- **Program arguments**: (none for default stdio mode)
- **VM options**: `-Dlogging.level.root=DEBUG`
- **Environment variables**: `DB_URL=jdbc:h2:mem:testdb;DB_USER=sa;DB_PASSWORD=;DB_DRIVER=org.h2.Driver`

**HTTP Mode Configuration:**
- **Main class**: `com.skanga.mcp.McpServer`
- **Program arguments**: `--http_mode=true --http_port=8080`
- **VM options**: `-Dlogging.level.root=DEBUG`
- **Environment variables**: `DB_URL=jdbc:h2:mem:testdb;DB_USER=sa;DB_PASSWORD=;DB_DRIVER=org.h2.Driver`

#### Build Configurations
Create separate run configurations for different profiles:
- **Default Build**: `mvn clean package`
- **Standard Build**: `mvn clean package -P standard-databases`
- **Enterprise Build**: `mvn clean package -P standard-databases,enterprise-databases`

### VS Code Setup

#### Extensions
- **Extension Pack for Java**
- **Spring Boot Tools**
- **Maven for Java**

#### launch.json
```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Launch McpServer (stdio)",
            "request": "launch",
            "mainClass": "com.skanga.mcp.McpServer",
            "projectName": "dbchat",
            "env": {
                "DB_URL": "jdbc:h2:mem:testdb",
                "DB_USER": "sa",
                "DB_PASSWORD": "",
                "DB_DRIVER": "org.h2.Driver"
            },
            "vmArgs": "-Dlogging.level.root=DEBUG"
        },
        {
            "type": "java",
            "name": "Launch McpServer (HTTP)",
            "request": "launch",
            "mainClass": "com.skanga.mcp.McpServer",
            "projectName": "dbchat",
            "args": ["--http_mode=true", "--http_port=8080"],
            "env": {
                "DB_URL": "jdbc:h2:mem:testdb",
                "DB_USER": "sa", 
                "DB_PASSWORD": "",
                "DB_DRIVER": "org.h2.Driver"
            },
            "vmArgs": "-Dlogging.level.root=DEBUG"
        }
    ]
}
```

#### tasks.json
```json
{
    "version": "2.0.0",
    "tasks": [
        {
            "label": "maven-build-default",
            "type": "shell",
            "command": "mvn",
            "args": ["clean", "package"],
            "group": "build",
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "panel": "shared"
            }
        },
        {
            "label": "maven-build-standard",
            "type": "shell", 
            "command": "mvn",
            "args": ["clean", "package", "-P", "standard-databases"],
            "group": "build"
        },
        {
            "label": "maven-test",
            "type": "shell",
            "command": "mvn",
            "args": ["test"],
            "group": "test"
        }
    ]
}
```

## 🚀 Deployment Strategies

### Local Development Deployment
```bash
# Quick development setup
java -jar target/dbchat-4.1.0.jar \
  --config_file=dev.conf \
  --http_mode=true \
  --http_port=3001
```

### Staging Environment Deployment
```bash
# Staging with PostgreSQL
java -jar target/dbchat-4.1.0.jar \
  --config_file=staging.conf \
  --http_mode=true \
  --http_port=8080 \
  --max_connections=20 \
  --select_only=true
```

### Production Environment Deployment

#### Systemd Service (`/etc/systemd/system/dbchat.service`)
```ini
[Unit]
Description=DBChat Database MCP Server
After=network.target postgresql.service

[Service]
Type=simple
User=dbchat
Group=dbchat
WorkingDirectory=/opt/dbchat
ExecStart=/usr/bin/java -jar /opt/dbchat/dbchat-4.1.0.jar --config_file=/etc/dbchat/production.conf
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=dbchat

# Security settings
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/log/dbchat

[Install]
WantedBy=multi-user.target
```

#### Installation Script (`install-production.sh`)
```bash
#!/bin/bash
set -e

# Create user
sudo useradd -r -s /bin/false dbchat

# Create directories
sudo mkdir -p /opt/dbchat
sudo mkdir -p /etc/dbchat
sudo mkdir -p /var/log/dbchat

# Copy files
sudo cp target/dbchat-4.1.0.jar /opt/dbchat/
sudo cp production.conf /etc/dbchat/
sudo cp dbchat.service /etc/systemd/system/

# Set permissions
sudo chown -R dbchat:dbchat /opt/dbchat
sudo chown -R dbchat:dbchat /var/log/dbchat
sudo chmod 600 /etc/dbchat/production.conf

# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable dbchat
sudo systemctl start dbchat

echo "DBChat production service installed and started"
```

### Cloud Deployment (AWS)

#### ECS Task Definition
```json
{
  "family": "dbchat",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "512",
  "executionRoleArn": "arn:aws:iam::account:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::account:role/ecsTaskRole",
  "containerDefinitions": [
    {
      "name": "dbchat",
      "image": "your-account.dkr.ecr.region.amazonaws.com/dbchat:2.0.0",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "HTTP_MODE",
          "value": "true"
        },
        {
          "name": "HTTP_PORT", 
          "value": "8080"
        }
      ],
      "secrets": [
        {
          "name": "DB_URL",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:dbchat/db-url"
        },
        {
          "name": "DB_USER",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:dbchat/db-user"
        },
        {
          "name": "DB_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:dbchat/db-password"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/dbchat",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8080/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

## 🔍 Troubleshooting Development Issues

### Build Issues

#### Maven Profile Problems
```bash
# Verify available profiles
mvn help:all-profiles

# Debug profile activation
mvn help:active-profiles -P standard-databases

# Check effective POM
mvn help:effective-pom -P standard-databases
```

#### Dependency Issues
```bash
# Analyze dependencies
mvn dependency:tree -P standard-databases

# Resolve version conflicts
mvn dependency:analyze

# Force update snapshots
mvn clean install -U
```

#### JDBC Driver Issues
```bash
# Verify driver in JAR
jar tf target/dbchat-4.1.0.jar | grep -E "(mysql|postgres|oracle)"

# Test driver loading
java -cp target/dbchat-4.1.0.jar -e "Class.forName('com.mysql.cj.jdbc.Driver')"

# Check driver versions
mvn dependency:list | grep -E "(mysql|postgres|oracle)"
```

### Runtime Issues

#### Connection Problems
```bash
# Test database connectivity
java -cp target/dbchat-4.1.0.jar -e "
import java.sql.*;
Connection conn = DriverManager.getConnection('$DB_URL', '$DB_USER', '$DB_PASSWORD');
System.out.println('Connection successful');
conn.close();
"

# Verify JDBC URL format
java -jar target/dbchat-4.1.0.jar --db_url="$DB_URL" --help
```

#### Memory Issues
```bash
# Monitor memory usage
java -Xmx512m -XX:+PrintGCDetails -jar target/dbchat-4.1.0.jar

# Profile memory allocation
java -XX:+UseG1GC -XX:+PrintGCApplicationStoppedTime -jar target/dbchat-4.1.0.jar

# Enable heap dump on OOM
java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -jar target/dbchat-4.1.0.jar
```

#### Performance Debugging
```bash
# Enable JMX monitoring
java -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9999 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -jar target/dbchat-4.1.0.jar

# Profile with async-profiler
java -jar target/dbchat-4.1.0.jar &
PID=$!
./profiler.sh -d 30 -f profile.html $PID
```

## 📚 Additional Resources for Developers

### Documentation
- **MCP Specification**: [modelcontextprotocol.io](https://modelcontextprotocol.io)
- **JDBC Tutorial**: [Oracle JDBC Tutorial](https://docs.oracle.com/javase/tutorial/jdbc/)
- **HikariCP Documentation**: [HikariCP GitHub](https://github.com/brettwooldridge/HikariCP)
- **Maven Profiles**: [Maven Profile Documentation](https://maven.apache.org/guides/introduction/introduction-to-profiles.html)

### Development Tools
- **MCP Inspector**: Test MCP protocol compliance
- **Database Tools**: DBeaver, DataGrip for database management
- **API Testing**: Postman, Insomnia for HTTP mode testing
- **Performance Monitoring**: JProfiler, VisualVM for performance analysis

### Contributing Guidelines
1. **Fork repository** and create feature branch
2. **Follow coding standards** (Google Java Style)
3. **Add tests** for new functionality
4. **Update documentation** for API changes
5. **Submit pull request** with clear description

### License Compliance
- **Apache 2.0**: Main project license
- **JDBC Drivers**: Each driver has its own license requirements
- **Enterprise Drivers**: May require commercial licenses
- **Review vendor terms** before distribution

---

This developer guide provides comprehensive information for building, testing, and deploying DBChat. For end-user setup instructions, see [README.md](README.md).
