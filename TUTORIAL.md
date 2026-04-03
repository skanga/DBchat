# DBchat Tutorial: A Complete Hands-On Tour

Welcome to DBchat! This tutorial walks you through every major feature of the DBchat MCP server using a single, coherent story: you're a new analyst at **Luminary Goods**, a fictional online retailer, and you need to get up to speed on the business fast — with no existing data and no SQL expertise required.

By the end you will have:
- Spun up an in-memory H2 database (zero setup)
- Built a realistic retail dataset from scratch using natural language
- Explored the schema, queried data, and caught anomalies
- Generated charts and an interactive dashboard
- Used the guided workflow and insights systems
- Exported a polished executive memo

---

## Prerequisites

You need:
- **Java 17+** installed (`java -version` to verify)
- **DBchat basic JAR** downloaded from the [releases page](https://github.com/skanga/dbchat/releases) (the `basic` variant includes H2)
- **Claude Desktop** (free, from [claude.ai/download](https://claude.ai/download)) or any MCP-compatible client

> **Note:** This entire tutorial uses the built-in H2 in-memory database. No database server, no credentials — just the JAR.

---

## Part 0 — Setup (5 minutes)

### 0.1 Configure Claude Desktop

Open Claude Desktop → **Settings → Developer → Edit Config** and add:

```json
{
  "mcpServers": {
    "luminary-db": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/dbchat-4.2.1-basic.jar"
      ]
    }
  }
}
```

No `dbchat.conf` needed — DBchat's built-in defaults use H2 in-memory mode:

```
DB_URL=jdbc:h2:mem:testdb
DB_USER=sa
DB_PASSWORD=
DB_DRIVER=org.h2.Driver
SELECT_ONLY=false   ← write access is on by default for H2
```

Restart Claude Desktop. You should see the database connection indicator (a small plug icon) appear in the chat input area.

> **First-time users:** The first time DBchat tries to read or write to your database, Claude Desktop will ask you to grant permission to the tool. You'll see a dialog along the lines of *"luminary-db wants to run a query"* — click **Allow** (or **Allow for this chat** to avoid repeated prompts). This is normal and expected; it's Claude Desktop's safety mechanism ensuring you're always in control of what runs against your database.

### 0.2 Verify the connection

Type this first prompt to confirm everything is working:

```
What database am I connected to, and what tables exist right now?
```

**Expected result:** DBchat reports an H2 in-memory database with no tables yet. You're starting from a clean slate.

---

## Part 1 — Building the Dataset (10 minutes)

We'll create the Luminary Goods schema table by table, populating realistic data as we go. Each step demonstrates a different DDL/DML capability.

### 1.1 Create the customers table

```
I need to keep track of our customers. Each customer has a name,
email address, the city and country they live in, the date they
signed up, and a loyalty tier — either Bronze, Silver, Gold, or
Platinum. Can you create a table for this and add 30 realistic
customers from the US, UK, Canada, and Australia? Spread the
signup dates across 2022 to 2024, and make sure a handful of
customers share cities so we can do some geographic grouping later.
```

> **What's happening:** DBchat translates your plain-English description into `CREATE TABLE` and `INSERT` statements and runs them against H2. You never touch SQL.

### 1.2 Create the products table

```
We also need a product catalog. Each product has a name, a category
(we sell Electronics, Apparel, Home & Garden, and Sports gear), a
price we sell it at, what it costs us to buy, and how many we have
in stock. Can you create that table and add about 20 products?
Prices should range from around $10 to $500. Leave 3 of the products
with zero stock — I want to test some inventory alerts later.
```

### 1.3 Create the orders and order_items tables

```
Now I need to track customer orders. An order belongs to a customer,
has a date, a status (Completed, Shipped, Processing, Cancelled, or
Refunded), the city it's shipping to, and a total dollar amount.
Each order can contain multiple products, so we also need a line-items
table that records which product, how many, and the price at the time
of purchase.

Please create both tables and fill them with about 120 realistic orders
from 2023 and 2024. Most should be Completed or Shipped, with a
realistic sprinkling of Cancellations and Refunds. Link them to the
customers and products we already created.
```

### 1.4 Create a reviews table with an intentional data quality issue

```
Last thing — customers can leave reviews on products. Each review
has a star rating from 1 to 5, some review text, and the date it
was written. Please create that table and add around 50 reviews.

One thing I want to test later is data quality checking, so
intentionally leave the star rating blank on two of the reviews,
and add one review that references a customer ID that doesn't
actually exist in our system.
```

### 1.5 Confirm the full schema

```
Show me all the tables in the database, with a row count for each one.
```

You should see: `customers`, `products`, `orders`, `order_items`, `product_reviews` — all populated.

---

## Part 2 — Schema Exploration (Feature: Database Discovery)

DBchat knows how to describe the shape of your database, not just its data.

### 2.1 Describe the full schema

```
Describe the complete schema of this database: all tables, their
columns with data types, primary keys, and foreign key relationships.
Explain how the tables relate to each other in plain English.
```

> **What to notice:** DBchat introspects the JDBC metadata and gives you a human-readable entity-relationship summary. This is the `describe_table` capability under the hood.

### 2.2 Generate an ERD

```
Now that you know the full schema, draw me an entity-relationship
diagram showing all the tables and how they connect to each other.
```

> **What to notice:** Claude generates a visual ERD as an artifact — boxes for each table, lines showing the foreign-key relationships between them. This is a great way to get a bird's-eye view of any unfamiliar database at a glance.

### 2.3 Identify join paths

```
If I want to find out which cities generate the most revenue,
which tables do I need to join and how?
```

DBchat will trace the path: `customers → orders` (via `customer_id`), filtering on `city`, summing `total_amount`. It explains the join logic before executing.

### 2.3 Spot potential design issues

```
Are there any columns in this schema that look like they might be
good candidates for an index? Which queries would benefit most?
```

---

## Part 3 — Natural Language Queries (Feature: Plain-English SQL)

This is the core DBchat superpower. No SQL required.

### 3.1 Simple aggregation

```
How many customers do we have in each country?
```

### 3.2 Business question with joins

```
Who are our top 5 customers by total spend? Show their name, email,
loyalty tier, and total amount spent.
```

### 3.3 Time-based filtering

```
How many orders were placed each month in 2024? Show the trend
month by month.
```

### 3.4 Status breakdown

```
What percentage of orders ended up Cancelled or Refunded?
Break it down by year.
```

### 3.5 Product performance

```
Which 3 product categories generate the most revenue?
For each category, also tell me the average order size.
```

### 3.6 A complex multi-step question

```
Find customers who signed up in 2022 but placed no orders in 2024.
These are our at-risk churners. List their names, emails, and
loyalty tiers, sorted by tier descending.
```

> **Notice:** DBchat handles the multi-table join, the date filtering, and the anti-join (customers with no matching 2024 orders) — all from a single natural language sentence.

---

## Part 4 — Data Quality Checks (Feature: Anomaly Detection)

### 4.1 Find missing data

```
Check all tables for NULL values in important columns.
Which columns have missing data, and how many rows are affected?
```

DBchat should surface the two NULL ratings in `product_reviews`.

### 4.2 Find orphan records

```
Are there any rows in product_reviews where the customer_id doesn't
match any customer in the customers table?
```

This catches the intentional orphan record from Step 1.4.

### 4.3 Check for duplicate emails

```
Are there any duplicate email addresses in the customers table?
```

### 4.4 Inventory alert

```
Which products are currently out of stock? Are any of those
products still being ordered (i.e., do they appear in order_items)?
```

### 4.5 Price sanity check

```
Are there any products where the unit_price is lower than the
cost_price? This would indicate we're selling at a loss.
```

---

## Part 5 — Visualizations (Feature: Charts & Dashboards)

DBchat + Claude Desktop can turn query results into interactive charts without leaving the chat.

### 5.1 Monthly revenue line chart

```
Query the orders table and create a line chart showing total monthly
revenue for 2023 and 2024 on the same chart, so I can compare
year-over-year growth.
```

> **What to notice:** Claude generates an interactive artifact (React/Recharts). Hover over points for exact values.

### 5.2 Category revenue bar chart

```
Create a horizontal bar chart showing revenue by product category.
Sort from highest to lowest. Include the exact revenue figure
as a label on each bar.
```

### 5.3 Order status pie chart

```
Make a pie chart of orders broken down by status.
Use a professional color scheme.
```

### 5.4 Customer geography

```
Create a bar chart showing the number of customers per country,
and a separate chart showing average order value per country.
Display both side by side.
```

### 5.5 Full executive dashboard

```
Build a complete sales dashboard showing:
1. Four KPI cards: Total Revenue, Total Orders, Average Order Value,
   Active Customers (those with at least one completed order)
2. A line chart: monthly revenue trend for 2024
3. A bar chart: top 5 products by revenue
4. A pie chart: orders by status
5. A table: top 5 customers by spend (name, tier, total)

Make it look professional, with a clean layout and consistent colors.
```

> **Pro tip:** You can ask Claude to refine the dashboard iteratively — "make the KPI cards larger", "add a trend arrow to revenue", etc.

---

## Part 6 — Guided Workflow (Feature: Interactive Workflows)

DBchat includes a structured workflow engine that guides you through a business analysis step by step, with multiple-choice decision points.

### 6.1 Start a workflow

```
Start a workflow for me. I want to do a retail business analysis
of the Luminary Goods data.
```

> DBchat invokes `start_workflow`, presenting you with an initial business question and a set of choices for where to dig first (e.g., "Revenue trends", "Customer segments", "Product performance").

### 6.2 Follow the choices

Select options as they appear. The workflow engine (`workflow_choice`) tracks your path and tailors each subsequent step based on what you chose before. It might take you through:

- High-level revenue summary → month-by-month drill-down → anomaly investigation
- Customer segmentation → cohort analysis → churn risk scoring
- Product ranking → margin analysis → reorder recommendations

### 6.3 Check workflow status

```
Show me the current workflow status and what step we're on.
```

This reads the `workflow://status` MCP resource, showing your progression through the analysis.

---

## Part 7 — Insights Collection (Feature: Business Intelligence Capture)

As you discover interesting findings, DBchat can capture them into a structured intelligence log.

### 7.1 Record a finding manually

```
Save this insight: "Customers in the Gold and Platinum tiers account
for 68% of revenue despite being only 22% of our customer base.
Priority: High. Category: Customer Segmentation."
```

> This uses the `append_insight` tool, which validates, categorizes, and timestamps the finding.

### 7.2 Ask DBchat to proactively capture insights

```
Analyze our order cancellation rate by month. If you find anything
noteworthy, save it as an insight automatically.
```

### 7.3 Add more findings as you explore

```
Check whether customers who leave reviews have a higher repeat
purchase rate than those who don't. Save any significant finding
as a high-priority insight.
```

```
Compare the average margin (unit_price - cost_price) across product
categories. Save the category with the highest and lowest margins
as insights.
```

### 7.4 View the insights memo

```
Show me the full insights memo.
```

> This reads the `insights://memo` MCP resource — a formatted executive report of everything captured, organized by category and priority, with timestamps.

### 7.5 Get the quick summary

```
Give me the quick insights summary.
```

> This reads `insights://summary` — a condensed version suitable for a stand-up meeting.

---

## Part 8 — Security & Configuration Features

### 8.1 Test read-only mode

To experience the `SELECT_ONLY` safeguard, temporarily update your Claude Desktop config to add:

```json
"env": {
  "SELECT_ONLY": "true"
}
```

Restart Claude Desktop, then try:

```
Delete all orders with status 'Cancelled'.
```

> DBchat will refuse with a clear error explaining that write operations are disabled. This is the safety net for production databases.

### 8.2 Test row limits

Add `MAX_ROWS_LIMIT=5` to env, restart, then ask:

```
Show me all customers.
```

> DBchat returns only 5 rows and explains that the result was truncated. Remove the limit afterward.

### 8.3 Query timeout behavior

```
What would happen if I ran a query that took 60 seconds?
How is the timeout configured, and what's the current value?
```

DBchat will explain the `QUERY_TIMEOUT_SECONDS` setting and its current default (30 seconds).

---

## Part 9 — MCP Prompts (Feature: Structured Prompt Templates)

DBchat exposes pre-built prompt templates via the MCP `prompts/list` protocol. In a prompt-aware client, you can browse and invoke these directly.

### 9.1 List available prompts

```
What structured prompts or analysis templates does DBchat provide?
```

> DBchat lists its three built-in prompt types: `mcp-demo`, `business-intelligence`, and `database-analysis`.

### 9.2 Run a business intelligence prompt

```
Run the business-intelligence prompt on this database.
```

> This triggers a comprehensive BI framework: executive summary, trend analysis, segment performance, and anomaly report — all in one structured response.

### 9.3 Run the database analysis prompt

```
Run the database-analysis prompt. Treat this as a new database
I've inherited and need to understand quickly.
```

> Produces a full database health report: schema summary, row counts, data quality score, relationship map, and suggested indexes.

---

## Part 10 — Multi-Database Setup (Bonus Feature)

If you want to see DBchat connect to multiple databases simultaneously, add a second server entry to your Claude Desktop config pointing to a SQLite file:

```json
{
  "mcpServers": {
    "luminary-db": {
      "command": "java",
      "args": ["-jar", "/path/to/dbchat-4.2.1-basic.jar"],
      "env": { "DB_URL": "jdbc:h2:mem:testdb", "DB_USER": "sa", "DB_DRIVER": "org.h2.Driver" }
    },
    "luminary-archive": {
      "command": "java",
      "args": ["-jar", "/path/to/dbchat-4.2.1-basic.jar"],
      "env": { "DB_URL": "jdbc:sqlite:/tmp/luminary-archive.db", "DB_DRIVER": "org.sqlite.JDBC", "SELECT_ONLY": "true" }
    }
  }
}
```

Then ask:

```
I have two databases connected. Compare the schema of luminary-db
and luminary-archive. Are the table structures compatible?
```

---

## Recap: Features Covered

| Feature | Where You Used It |
|---|---|
| In-memory H2 database | Parts 0–1 |
| Natural language DDL/DML | Part 1 |
| Schema introspection | Part 2 |
| Plain-English SQL queries | Part 3 |
| Data quality / anomaly detection | Part 4 |
| Chart generation (line, bar, pie) | Part 5.1–5.4 |
| Multi-panel executive dashboard | Part 5.5 |
| Interactive guided workflows | Part 6 |
| Insights capture & memo export | Part 7 |
| SELECT_ONLY read-only mode | Part 8.1 |
| Row limits & query timeouts | Part 8.2–8.3 |
| MCP prompt templates | Part 9 |
| Multi-database configuration | Part 10 |

---

## What's Next

- Connect to a real database (PostgreSQL, MySQL, SQLite file) by updating `DB_URL` and using the appropriate JAR variant
- Try the `standard` JAR for MySQL/MariaDB, or `enterprise` for Oracle/SQL Server
- Enable the HTTP web interface (`HTTP_MODE=true`) to access DBchat from a browser
- Explore the [VISUALIZATION.md](VISUALIZATION.md) file for advanced charting techniques
- Read [MCP-SETUP.md](MCP-SETUP.md) to configure DBchat with Cursor, VS Code, or other MCP clients

---

*Tutorial based on DBchat v4.2.1 · [github.com/skanga/DBchat](https://github.com/skanga/DBchat)*
