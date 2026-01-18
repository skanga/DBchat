# Repository Guidelines

## Project Structure & Module Organization
DBChat is a Maven-based Java 17 project. Key locations:

- `src/main/java/com/skanga/mcp`: MCP server, HTTP handler, config, DB services, prompts, workflows, insights.
- `src/main/java/com/skanga/demo` and `src/main/java/com/skanga/init`: demo data and client config helpers.
- `src/main/resources`: runtime resources (optional config).
- `src/test/java/com/skanga`: JUnit 5 tests, integration tests, and performance tests.
- `build.sh`, `release.sh`, `RELEASE.bat`: build/release helpers.
- `test-mcp-protocol.py`, `test-mcp-server.py`: protocol smoke tests.
- `target/`: Maven build output (generated).

## Build, Test, and Development Commands
Common workflows use Maven profiles for DB driver variants:

```bash
mvn clean package                    # default drivers
mvn clean package -P standard-databases
./build.sh standard --skip-tests     # scripted build variants
```

Run locally with a config file or env vars:

```bash
java -jar target/dbchat-4.0.0.jar --config_file=dbchat.conf
```

## Architecture Overview
DBChat is an MCP server that runs in stdio or HTTP mode. `McpServer` and `McpHttpHandler` accept JSON-RPC, route calls to services, and return typed responses. Core services include:

- `db` package for JDBC access and resource discovery.
- `prompts`, `workflow`, and `insights` for structured prompts, multi-step workflows, and captured analysis.
- `config` for CLI/env/config parsing and resource management.

## Coding Style & Naming Conventions
- Java 17, 4-space indentation, keep packages under `com.skanga`.
- Classes use `PascalCase`, methods/fields use `camelCase`.
- Tests end in `*Test.java`, integration tests use `*IntegrationTest.java`.
- Follow the Google Java Style guidance referenced in `INSTALL.md`.

## Testing Guidelines
Frameworks: JUnit 5, Mockito, AssertJ, Testcontainers, and JaCoCo coverage.

```bash
mvn test
mvn test -Dtest=McpServerTest
mvn verify -P standard-databases
```

Maintain the existing high coverage (documentation cites ~90% lines) and add tests for new behavior or bug fixes.

## Commit & Pull Request Guidelines
Recent history mixes short imperative subjects with long, multi-topic messages. Prefer the shorter pattern and split when possible:
- Use sentence-case imperatives, 50-72 chars target (e.g., "Fix links", "Update README.md").
- Use `Release version x.y.z` for release commits.
- Avoid bundling unrelated changes in a single commit.

PRs should include: a clear summary, linked issue (if any), test results (commands + outcomes), and docs updates when behavior/config changes. Include screenshots only when UI or visualization output changes.

## Security & Configuration Tips
- Do not commit credentials. Prefer env vars or a local `dbchat.conf`.
- Use `SELECT_ONLY=true` for demos or readonly environments.
- Keep local config files out of git unless explicitly intended.
