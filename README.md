# DBChat - Talk to Your Database using AI

Transform your database into an intelligent conversational partner. Ask questions in plain English, get instant answers, and create beautiful visualizations - all through Claude Desktop or any other MCP Client.

## 🌟 What is DBChat?

DBChat is a bridge that connects any MCP client like Claude Desktop, Gemini-CLI, etc to your database, enabling natural language database interactions. Instead of writing SQL queries, simply ask the chatbot questions about your data and get instant, intelligent responses.

**Before DBChat:**
```sql
SELECT c.name, COUNT(o.id) as order_count, SUM(o.total) as revenue 
FROM customers c 
LEFT JOIN orders o ON c.id = o.customer_id 
WHERE o.created_at >= '2024-01-01' 
GROUP BY c.id, c.name 
ORDER BY revenue DESC 
LIMIT 10;
```

**With DBChat:**
```
"Show me our top 10 customers by revenue this year"
```

## 🎯 Why Use DBChat?

### 🗣️ Natural Language Database Queries
- **Ask questions in plain English**: "How many customers signed up last month?"
- **Get conversational responses**: The LLM explains the data and provides businessInsights
- **No SQL knowledge required**: Perfect for business users and analysts

### 📊 Instant Data Visualizations
- **Automatic chart creation**: Claude generates beautiful charts from your data
- **Multiple chart types**: Line charts, bar charts, pie charts, scatter plots, and more
- **Interactive businessInsights**: Drill down into your data with follow-up questions

### 🔍 Smart Data Exploration
- **Database discovery**: "What tables do we have?" "Show me the customer table structure"
- **Relationship understanding**: The AI model explains how your tables connect
- **Data quality businessInsights**: Find duplicates, missing data, and anomalies

### 💼 Business Intelligence Made Easy
- **Executive dashboards**: "Create a sales summary for our board meeting"
- **Trend analysis**: "Show me user growth over the past 6 months"
- **Performance metrics**: "Which products are underperforming?"

## ✨ Advanced Features

DBChat now includes powerful features for interactive demos, onboarding, collaborative analysis, and sophisticated business intelligence workflows.

### 🚀 MCP Prompts Support
Revolutionary structured workflow system for guided database analysis and business intelligence.
- **Professional Business Scenarios**: Pre-built scenarios for Retail/E-commerce, Finance/Banking, and Logistics with realistic business contexts
- **Interactive Demo Templates**: Complete 15-20 minute guided workflows with:
  - Business narratives with protagonists and deadlines
  - Step-by-step analytical progression
  - Multiple choice decision points
  - Expected outcomes and insights for each step
- **Three Sophisticated Prompt Types**:
  - `mcp-demo` - Complete interactive demo with business scenarios
  - `business-intelligence` - Comprehensive BI analysis framework  
  - `database-analysis` - Enhanced database exploration workflow
- **MCP Protocol Integration**: Full compliance with MCP prompts protocol including `prompts/list` and `prompts/get` handlers
- **How to start**: Use any prompt-aware MCP client and select from available structured prompts for guided analysis

### 🎯 Interactive Multiple Choice Workflow System
Sophisticated workflow engine for structured data exploration and analysis.
- **Structured Progressions**: Step-by-step workflows with contextual multiple choice options
- **Scenario-Specific Content**: Tailored workflows for retail, finance, and logistics business domains
- **State Management**: Tracks user choices and workflow progression throughout analysis sessions
- **New MCP Tools**:
  - `start_workflow` - Initiates interactive analysis workflows with business context
  - `workflow_choice` - Processes user selections and advances workflow steps
- **MCP Resources**: `workflow://status` resource shows active workflow status and progress
- **Integration Ready**: Seamlessly works with demo data setup and insights collection systems

### 🛠️ Automatic Demo Data Setup
Intelligent database population system for demonstrations and onboarding.
- **Realistic Business Scenarios**: Choose from Retail, Finance, or Logistics domains
- **Database-Agnostic**: Works across all supported database types and configurations
- **Comprehensive Data Models**:
  - **Retail**: Customers, Products, Orders, Inventory with realistic relationships
  - **Finance**: Accounts, Transactions, Customers, Loans with financial data patterns
  - **Logistics**: Shipments, Routes, Warehouses, Deliveries with supply chain data
- **Instant Setup**: 2-3 related tables per scenario with 10-15 representative rows each
- **Sample Analytics**: Pre-built queries and analysis examples for each scenario
- **Cleanup Capabilities**: Automatic reset and cleanup functionality for fresh demonstrations

### 🧠 Enhanced Insights Collection System
Professional business intelligence capture and reporting system.
- **Structured Insight Model**: Comprehensive insights with categories, priorities, timestamps, and metadata
- **`append_insight` Tool**: Advanced tool for capturing structured business findings with:
  - Content validation and sanitization
  - Automatic categorization with visual indicators
  - Priority assignment (high/medium/low)
  - Security audit logging
- **Professional Memo Generation**:
  - **Comprehensive Memos**: Executive summaries, categorized findings, priority insights, timeline analysis
  - **Quick Summaries**: Fast consumption format for rapid business updates
  - **Multi-format Support**: Both detailed and summary formats available
- **MCP Resources Integration**:
  - `insights://memo` - Professional business intelligence report
  - `insights://summary` - Quick overview of collected insights
  - Dynamic resource discovery as insights are captured
- **Persistent Storage**: Auto-save to JSON with load/restore capabilities and export functionality
- **Real-time Statistics**: Live tracking of insight counts, categories, and analytical progress

## 🗃️ Supported Databases

DBChat works with virtually any database (as long as it has a JDBC driver)

### Popular Databases
- **MySQL** / **MariaDB** - Web applications and e-commerce
- **Oracle** - Enterprise applications
- **PostgreSQL** - Advanced applications and analytics
- **SQL Server** - Microsoft environments
- **H2** - Testing and development
- **SQLite** - Local applications and prototypes
- **HSQLDB** - Testing and development

### NoSQL & Caching
- **Redis** - In-memory data store
- **MongoDB** - Document-oriented database
- **Cassandra** - Wide-column store

### Analytics & Cloud
- **Snowflake** - Cloud data platform
- **Databricks** - Cloud data platform
- **Amazon Redshift** - AWS data warehouse
- **Google BigQuery** - Google analytics
- **ClickHouse** - Real-time analytics

### Flat-file Based Data
- **CSV Files** - Spreadsheet data and exports
- **Excel Files** - Can be exported to CSV and queried

*See [INSTALL.md](INSTALL.md) for the complete list and build options.*

## 🚀 Quick Start

### Step 1: Download DBChat

Download the latest release from [GitHub Releases](https://github.com/skanga/dbchat/releases):
- [dbchat-4.0.0.jar](https://github.com/skanga/dbchat/releases/download/v4.0.0/dbchat-4.0.0.jar) - Basic version (PostgreSQL, SQLite, H2, HSQLDB, CSV)   `<-- Start here`
- [dbchat-4.0.0.jar](https://github.com/skanga/dbchat/releases/download/v4.0.0/dbchat-4.0.0.jar) - Standard version (add MySQL, MariaDB, ClickHouse)
- [dbchat-4.0.0.jar](https://github.com/skanga/dbchat/releases/download/v4.0.0/dbchat-4.0.0.jar) - Enterprise version (add Oracle, SQL Server, DB2)
- [dbchat-4.0.0.jar](https://github.com/skanga/dbchat/releases/download/v4.0.0/dbchat-4.0.0.jar) - Cloud Analytics version (add Redshift, Snowflake, BigQuery)
- [dbchat-4.0.0.jar](https://github.com/skanga/dbchat/releases/download/v4.0.0/dbchat-4.0.0.jar) - All databases included (400MB+)

NOTE: Advanced users can also build a custom jar with only the drivers you need. See [INSTALL.md](INSTALL.md) for details.

IMPORTANT: Make sure that you are properly LICENSED to use any JDBC driver you install. The DBChat license does not cover any third party code or binaries.

### Step 2: Install Claude Desktop (similar setup for any other MCP client)

1. Download [Claude Desktop](https://claude.ai/download) (free)
2. Sign in with your Claude account
3. **Important**: The Claude website does not support MCP. For MCP only with Anthropic models you need to use Claude Desktop.

### Step 2 (alternate): 

If you are not using Claude Desktop but want to use another MCP client like Cursor, Windsurf, VS Code, Continue, etc then please refer to the [MCP Setup](MCP-SETUP.md) document for more details

### Step 3: Set Up Your Database Connection 

Create a configuration file `dbchat.conf`:

```properties
# Basic database connection
DB_URL=jdbc:mysql://localhost:3306/your_database
DB_USER=your_username
DB_PASSWORD=your_password
DB_DRIVER=com.mysql.cj.jdbc.Driver

# Optional: Enable web interface (not needed for Claude desktop)
HTTP_MODE=false
HTTP_PORT=8080
```

**Examples for Common/Popular Databases:**

**MySQL:**
```properties
DB_URL=jdbc:mysql://localhost:3306/your_database
DB_USER=your_username
DB_PASSWORD=your_password
DB_DRIVER=com.mysql.cj.jdbc.Driver
```

**PostgreSQL:**
```properties
DB_URL=jdbc:postgresql://localhost:5432/your_database
DB_USER=your_username
DB_PASSWORD=your_password
DB_DRIVER=org.postgresql.Driver
```

**SQLite:**
```properties
DB_URL=jdbc:sqlite:/path/to/your/database.db
DB_USER=
DB_PASSWORD=
DB_DRIVER=org.sqlite.JDBC
```

**H2 database (in memory - no database setup required):**
```properties
DB_URL=jdbc:h2:mem:testdb
DB_USER=sa
DB_PASSWORD=
DB_DRIVER=org.h2.Driver
```

**Oracle:**
```properties
DB_URL=jdbc:oracle:thin:@localhost:1521:xe
DB_USER=system
DB_PASSWORD=password
DB_DRIVER=oracle.jdbc.driver.OracleDriver
```

**Redis:**
```properties
DB_URL=jdbc:redis://localhost:6379
DB_USER=
DB_PASSWORD=your_redis_password
DB_DRIVER=com.dbvis.jdbc.redis.RedisDriver
```

### Step 4: Configure Claude Desktop

1. Open Claude Desktop
2. Go to **Settings** → **Developer** → **Edit Config**
3. Add your database server:

```json
{
  "mcpServers": {
    "database": {
      "command": "java",
      "args": [
        "-jar", 
        "/absolute/path/to/dbchat-4.0.0.jar",
        "--config_file=/absolute/path/to/dbchat.conf"
      ]
    }
  }
}
```

**Alternative without config file:**
```json
{
  "mcpServers": {
    "database": {
      "command": "java",
      "args": ["-jar", "/absolute/path/to/dbchat-4.0.0.jar"],
      "env": {
        "DB_URL": "jdbc:mysql://localhost:3306/your_database",
        "DB_USER": "your_username", 
        "DB_PASSWORD": "your_password",
        "DB_DRIVER": "com.mysql.cj.jdbc.Driver"
      }
    }
  }
}
```

**Windows Example:**
```json
{
  "mcpServers": {
    "database": {
      "command": "java",
      "args": [
        "-jar", 
        "C:/Users/YourName/Downloads/dbchat-4.0.0.jar",
        "--config_file=C:/Users/YourName/dbchat.conf"
      ]
    }
  }
}
```
NOTE: If java is not in your PATH then use the full path to java (JDK 17+) in the command.

### Step 5: Connect Multiple Databases

You can use many databases concurrently!
```json
{
  "mcpServers": {
    "production-db": {
      "command": "java",
      "args": ["-jar", "/path/to/dbchat-4.0.0.jar"],
      "env": {
        "DB_URL": "jdbc:mysql://prod-server:3306/production",
        "DB_USER": "readonly_user",
        "DB_PASSWORD": "secure_password",
        "DB_DRIVER": "com.mysql.cj.jdbc.Driver",
        "SELECT_ONLY": "true"
      }
    },
    "analytics-db": {
      "command": "java", 
      "args": ["-jar", "/path/to/dbchat-4.0.0.jar"],
      "env": {
        "DB_URL": "jdbc:postgresql://analytics:5432/warehouse",
        "DB_USER": "analyst",
        "DB_PASSWORD": "password",
        "DB_DRIVER": "org.postgresql.Driver"
      }
    }
  }
}
```

### Step 6: Restart Claude Desktop

Close and reopen Claude Desktop. You should see a database connection indicator in the chat input.

## 💬 Start Talking to Your Database

### Data Exploration
```
"What tables do we have in the database?"
"Show me the structure of the customers table"
"How many records are in each table?"
```

### Business Questions
```
"How many new customers did we get last month?"
"What are our top 5 selling products this quarter?"
"Show me revenue by month for the past year"
```

### Data Analysis
```
"Find customers who haven't ordered in 6 months"
"Are there any duplicate email addresses?"
"What's the average order value by customer segment?"
```

### Visualizations
```
"Create a chart showing monthly sales trends"
"Make a pie chart of orders by product category"
"Show me a bar chart of customer signups by region"
```

### Advanced Analytics
```
"Calculate customer lifetime value for each segment"
"Identify seasonal trends in our sales data"
"Find correlations between customer age and purchase behavior"
```

## 📱 More Real-World Examples

### E-commerce Analytics
```
"Show me our conversion funnel from visitors to purchases"
"Which products have the highest return rates?"
"Create a dashboard showing daily sales performance"
```

### Customer Success
```
"Find customers at risk of churning"
"Show me customer satisfaction trends"
"Identify our most valuable customer segments"
```

### Financial Reporting
```
"Generate a P&L summary for this quarter"
"Show cash flow trends over the past 12 months"
"Create an expense breakdown by department"
```

### Operations Management
```
"Monitor inventory levels across all warehouses"
"Show shipping performance by carrier"
"Identify bottlenecks in our fulfillment process"
```


## 📊 Data Visualization Examples

DBChat enables Claude to create stunning visualizations directly from your database:

### Sales Dashboard
- **Monthly Revenue Trends**: Line charts showing growth over time
- **Top Products**: Bar charts of bestsellers
- **Regional Performance**: Heat maps of sales by location
- **Customer Segments**: Pie charts of revenue distribution

### Analytics Reports
- **User Growth**: Area charts showing acquisition trends
- **Performance Metrics**: Multi-axis charts combining different KPIs
- **Comparative Analysis**: Side-by-side visualizations of different periods

### Operational Dashboards
- **Inventory Levels**: Real-time stock visualization
- **System Performance**: Time-series charts of key metrics
- **Quality Metrics**: Statistical charts showing trends and outliers

## 🛡️ Security Features

### Read-Only Mode
Protect your data with read-only access:
```properties
SELECT_ONLY=true
```

### Query Limits
Control resource usage:
```properties
MAX_ROWS_LIMIT=1000
QUERY_TIMEOUT_SECONDS=30
MAX_SQL_LENGTH=10000
```

### Local Processing
- All data stays on your machine
- No external API calls
- Encrypted environment variables
- Secure local communication

## 🌐 Web Interface (Optional)

Enable HTTP mode for web-based access:

```properties
# Turn on HTTP listener
HTTP_MODE=true
# Listen on port 8080
HTTP_PORT=8080
# Bind to all interfaces (allows external access)
BIND_ADDRESS=0.0.0.0
# NOTE: If no bind address is given we bind to localhost only (default, most secure)
```
Then access at `http://localhost:8080/`. For example try `http://localhost:8080/health` to check health status

For similar config via CLI args use:
```
# Bind to localhost only (default, most secure)
java -jar dbchat-4.0.0.jar --http_mode=true --http_port=8080

# Bind to all interfaces (allows external access)
java -jar dbchat-4.0.0.jar --http_mode=true --bind_address=0.0.0.0 --http_port=8080

# Bind to specific interface
java -jar dbchat-4.0.0.jar --http_mode=true --bind_address=192.168.1.100 --http_port=8080
```

## 🔧 Configuration Methods and Priority

For maximum flexibility, DBChat supports multiple configuration methods like CLI arguments, config file, Environment vars,
System Properties and Built-in Defaults. Understanding the priority order is crucial for troubleshooting and advanced setups.

### Configuration Priority Order (Highest to Lowest)

1. **Command Line Arguments** (Highest Priority)
2. **Configuration File**
3. **Environment Variables**
4. **System Properties**
5. **Built-in Defaults** (Lowest Priority)

This means command line arguments will always override config files, which override environment variables, and so on.

### Method 1: Command Line Arguments
**Format**: `--parameter_name=value`
**Use case**: Quick overrides, testing, one-time configurations

```bash
java -jar dbchat-4.0.0.jar \
  --db_url="jdbc:mysql://localhost:3306/mydb" \
  --db_user="username" \
  --db_password="password" \
  --db_driver="com.mysql.cj.jdbc.Driver" \
  --http_mode=true \
  --http_port=8080 \
  --select_only=true
```

**Available parameters:**
- `--config_file=/path/to/config.conf`
- `--db_url="jdbc:..."`
- `--db_user="username"`
- `--db_password="password"`
- `--db_driver="com.mysql.cj.jdbc.Driver"`
- `--http_mode=true`
- `--http_port=8080`
- `--max_connections=20`
- `--connection_timeout_ms=30000`
- `--query_timeout_seconds=60`
- `--select_only=true`
- `--max_sql_length=50000`
- `--max_rows_limit=10000`

### Method 2: Configuration Files
**Format**: `KEY=VALUE` (one per line)
**Use case**: Production environments, complex configurations, version control

Create a file (e.g., `dbchat.conf`):
```properties
# Database Connection
DB_URL=jdbc:postgresql://localhost:5432/myapp
DB_USER=dbuser
DB_PASSWORD=my secure password with spaces
DB_DRIVER=org.postgresql.Driver

# Connection Pool Settings
MAX_CONNECTIONS=20
CONNECTION_TIMEOUT_MS=60000
IDLE_TIMEOUT_MS=300000
MAX_LIFETIME_MS=1800000
LEAK_DETECTION_THRESHOLD_MS=60000

# Query Settings
QUERY_TIMEOUT_SECONDS=45
SELECT_ONLY=false
MAX_SQL_LENGTH=50000
MAX_ROWS_LIMIT=50000

# Server Settings
HTTP_MODE=true
HTTP_PORT=8080
```

**Usage:**
```bash
java -jar dbchat-4.0.0.jar --config_file=dbchat.conf
```

**Config file features:**
- Comments start with `#`
- Empty lines are ignored
- Values can be quoted: `DB_PASSWORD="password with spaces"`
- Keys are case-insensitive
- Supports all the same parameters as command line

### Method 3: Environment Variables
**Format**: `UPPERCASE_WITH_UNDERSCORES`
**Use case**: Docker, cloud deployment, CI/CD, secure credential management

```bash
export DB_URL="jdbc:mysql://localhost:3306/mydb"
export DB_USER="username"
export DB_PASSWORD="password"
export DB_DRIVER="com.mysql.cj.jdbc.Driver"
export HTTP_MODE="true"
export HTTP_PORT="8080"
export SELECT_ONLY="true"

java -jar dbchat-4.0.0.jar
```

**All environment variables:**
- `CONFIG_FILE` - Path to configuration file
- `DB_URL` - Database connection URL
- `DB_USER` - Database username
- `DB_PASSWORD` - Database password
- `DB_DRIVER` - JDBC driver class
- `HTTP_MODE` - Enable HTTP mode (true/false)
- `HTTP_PORT` - HTTP server port
- `MAX_CONNECTIONS` - Connection pool size
- `CONNECTION_TIMEOUT_MS` - Connection timeout
- `QUERY_TIMEOUT_SECONDS` - Query timeout
- `SELECT_ONLY` - Read-only mode (true/false)
- `MAX_SQL_LENGTH` - Maximum query length
- `MAX_ROWS_LIMIT` - Maximum result rows
- `IDLE_TIMEOUT_MS` - Connection idle timeout
- `MAX_LIFETIME_MS` - Connection max lifetime
- `LEAK_DETECTION_THRESHOLD_MS` - Leak detection threshold

### Method 4: System Properties
**Format**: `-Dparameter.name=value` (underscores become dots)
**Use case**: JVM-specific configuration, IDE run configurations

```bash
java -Ddb.url="jdbc:mysql://localhost:3306/mydb" \
     -Ddb.user="username" \
     -Ddb.password="password" \
     -Ddb.driver="com.mysql.cj.jdbc.Driver" \
     -Dhttp.mode="true" \
     -Dhttp.port="8080" \
     -jar dbchat-4.0.0.jar
```

**Property naming**: Environment variable `DB_URL` becomes system property `db.url`

### Method 5: Built-in Defaults
**When**: No configuration provided
**Values**: Safe defaults for development

```properties
DB_URL=jdbc:h2:mem:testdb
DB_USER=sa
DB_PASSWORD=
DB_DRIVER=org.h2.Driver
HTTP_MODE=false
HTTP_PORT=8080
MAX_CONNECTIONS=10
CONNECTION_TIMEOUT_MS=30000
QUERY_TIMEOUT_SECONDS=30
SELECT_ONLY=true
MAX_SQL_LENGTH=10000
MAX_ROWS_LIMIT=10000
```

### Configuration Examples

#### Example 1: Priority Override
```bash
# Config file has HTTP_PORT=8080
echo "HTTP_PORT=8080" > config.conf

# Environment variable sets different port
export HTTP_PORT=9090

# Command line overrides both
java -jar dbchat-4.0.0.jar --config_file=config.conf --http_port=7070

# Result: Uses port 7070 (command line wins)
```

#### Example 2: Mixed Configuration
```bash
# Use config file for database settings
echo "DB_URL=jdbc:mysql://localhost:3306/mydb" > prod.conf
echo "DB_USER=produser" >> prod.conf
echo "SELECT_ONLY=true" >> prod.conf

# Override password via environment (more secure)
export DB_PASSWORD="secure_password"

# Override port via command line (for this run only)
java -jar dbchat-4.0.0.jar --config_file=prod.conf --http_port=9090
```

#### Example 3: Claude Desktop Configuration
Note that most MCP clients use a similar configuration, but you'll need to refer to your MCP
client docs for details on how it can be configured. It is safest not to assume any PATH settings
and provide absolute paths for java, the dbchat jar and (optionally) the dbchat config file.
```json
{
  "mcpServers": {
    "database": {
      "command": "java",
      "args": [
        "-jar", "/path/to/dbchat-4.0.0.jar",
        "--config_file=/path/to/production.conf",
        "--select_only=true"
      ],
      "env": {
        "DB_PASSWORD": "secure_password_from_env"
      }
    }
  }
}
```

### Configuration Parameters Reference

#### Database Connection
- `DB_URL` - JDBC connection string (required)
- `DB_USER` - Database username
- `DB_PASSWORD` - Database password
- `DB_DRIVER` - JDBC driver class (required)

#### Connection Pool
- `MAX_CONNECTIONS=10` - Maximum concurrent connections
- `CONNECTION_TIMEOUT_MS=30000` - Connection acquisition timeout
- `IDLE_TIMEOUT_MS=600000` - Connection idle timeout (10 minutes)
- `MAX_LIFETIME_MS=1800000` - Connection max lifetime (30 minutes)
- `LEAK_DETECTION_THRESHOLD_MS=60000` - Connection leak detection (1 minute)

#### Query Settings
- `QUERY_TIMEOUT_SECONDS=30` - SQL query execution timeout
- `SELECT_ONLY=true` - Read-only mode (blocks INSERT/UPDATE/DELETE)
- `MAX_SQL_LENGTH=10000` - Maximum characters in SQL query
- `MAX_ROWS_LIMIT=10000` - Maximum rows returned per query

#### Server Settings
- `HTTP_MODE=false` - Enable HTTP web interface
- `HTTP_PORT=8080` - HTTP server port

### Security Best Practices

#### Credential Management
```bash
# Good: Use environment variables for passwords
export DB_PASSWORD="secure_password"
java -jar dbchat-4.0.0.jar --config_file=app.conf

# Good: Use config file with restricted permissions
chmod 600 secure.conf
java -jar dbchat-4.0.0.jar --config_file=secure.conf

# Avoid: Passwords in command line (visible in process list)
java -jar dbchat-4.0.0.jar --db_password="visible_password"
```

#### Configuration File Security
```bash
# Create secure config file
umask 077
cat > secure.conf << EOF
DB_PASSWORD=secure_password
EOF

# Verify permissions
ls -la secure.conf
# Should show: -rw------- (owner read/write only)
```

### Troubleshooting Configuration

#### Check Effective Configuration
Enable debug logging to see which values are being used:
```bash
java -Dlogging.level.root=DEBUG -jar dbchat-4.0.0.jar --config_file=myconfig.conf
```

#### Common Issues
1. **Config file not found**: Use absolute paths
2. **Permission denied**: Check file permissions
3. **Wrong values used**: Check priority order
4. **Environment variables not set**: Use `env | grep DB_` to verify

#### Validation Commands
```bash
# Test database connection with current config
java -jar dbchat-4.0.0.jar --help

# Verify config file syntax
grep -v "^#" myconfig.conf | grep -v "^$"

# Check environment variables
env | grep -E "(DB_|HTTP_|MAX_|SELECT_)"
```

## 🚫 Troubleshooting

### Claude Desktop Not Connecting
1. **Check paths**: Use absolute paths in configuration
2. **Java version**: Ensure Java 17+ is installed
3. **File permissions**: Verify JAR file is readable
4. **Restart Claude**: Close and reopen Claude Desktop

### Database Connection Issues
1. **Test connection**: Verify database is running
2. **Check credentials**: Ensure username/password are correct
3. **Network access**: Confirm database allows connections
4. **Driver support**: Use the correct JAR version for your database

### Performance Issues
1. **Limit results**: Use `MAX_ROWS_LIMIT=1000`
2. **Query timeout**: Set `QUERY_TIMEOUT_SECONDS=30`
3. **Connection pool**: Adjust `MAX_CONNECTIONS=10`

### Common Error Messages

**"ClassNotFoundException"**
- Download the correct JAR version for your database
- Check that the database driver is included

**"Connection refused"**
- Verify database server is running
- Check connection URL, username, and password
- Ensure database allows connections from your machine

**"Server not responding"**
- Check Claude Desktop configuration syntax
- Verify Java is accessible in PATH
- Review Claude Desktop logs

## 🎯 Best Practices

### Getting Started
1. **Start with read-only**: Use `SELECT_ONLY=true` initially
2. **Test with sample data**: Try the H2 database first
3. **Begin with simple questions**: Start with basic table exploration
4. **Build complexity gradually**: Move to advanced analytics over time

### Security
1. **Use dedicated database users**: Create read-only users for DBChat
2. **Limit access**: Only grant necessary table permissions
3. **Monitor usage**: Review query logs regularly
4. **Backup data**: Always maintain database backups

### Performance
1. **Set reasonable limits**: Use `MAX_ROWS_LIMIT` and timeouts
2. **Index important columns**: Ensure queries can run efficiently
3. **Monitor resources**: Watch CPU and memory usage
4. **Optimize queries**: Let Claude suggest query improvements

## 📚 Learn More

- **Model Context Protocol**: [modelcontextprotocol.io](https://modelcontextprotocol.io)
- **Claude Desktop**: [claude.ai/download](https://claude.ai/download)
- **Developer Guide**: See [INSTALL.md](INSTALL.md) for technical details
- **GitHub Repository**: [github.com/skanga/dbchat](https://github.com/skanga/dbchat)

## 🚀 Ready to Transform Your Data Experience?

1. **Download** the appropriate JAR file for your database(s)
2. **Install** an MCP Client like Claude Desktop (free)
3. **Configure** your database connection
4. **Add** DBChat to MCP Client settings
5. **Start asking** questions about your data!

Transform your relationship with data. No more complex SQL queries, no more waiting for reports. Just natural conversations with your database, powered by Claude's intelligence and DBChat's seamless integration.

**Get started today and discover what your data has been trying to tell you.**
