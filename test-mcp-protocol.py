#!/usr/bin/env python3
"""
MCP Protocol Test Script
Tests both stdio and HTTP modes for the Database MCP Server
Includes comprehensive protocol compliance testing and comparison
"""

import json
import subprocess
import sys
import time
import os
import signal
import socket
import platform
import glob
from typing import Dict, Any, List, Optional, Tuple
from dataclasses import dataclass
from enum import Enum

# Try to import requests for HTTP testing
try:
    import requests
    HAS_REQUESTS = True
except ImportError:
    HAS_REQUESTS = False

class TestResult(Enum):
    PASS = "PASS"
    FAIL = "FAIL"
    ERROR = "ERROR"

class TransportMode(Enum):
    STDIO = "stdio"
    HTTP = "http"
    BOTH = "both"

@dataclass
class TestCase:
    name: str
    request: Dict[str, Any]
    expected_fields: List[str]
    is_notification: bool = False
    should_have_response: bool = True
    result: TestResult = TestResult.ERROR
    response: Optional[Dict[str, Any]] = None
    error_message: Optional[str] = None
    execution_time: float = 0.0

@dataclass
class ModeResults:
    mode: TransportMode
    total: int
    passed: int
    failed: int
    errors: int
    test_cases: List[TestCase]
    success_rate: float

class MCPTester:
    def __init__(self, jar_path: str, java_path: str = "java", port: int = 8080, debug: bool = False):
        self.jar_path = jar_path
        self.java_path = java_path
        self.port = port
        self.server_process = None
        self._http_drain_threads = []
        self.test_cases = []
        self.is_initialized = False
        self.debug = debug

    def print_header(self):
        """Print test header with system information"""
        print("=" * 70)
        print("MCP PROTOCOL TEST SUITE")
        print("=" * 70)
        print(f"Platform: {platform.system()} {platform.release()}")
        print(f"Python: {sys.version.split()[0]}")
        print(f"Java: {self.get_java_version()}")
        print(f"JAR Path: {self.jar_path}")
        print(f"HTTP Port: {self.port}")
        print(f"Requests Library: {'Available' if HAS_REQUESTS else 'Not Available (HTTP tests will be skipped)'}")
        print(f"Debug Mode: {'Enabled' if self.debug else 'Disabled'}")
        print()

    def get_java_version(self) -> str:
        """Get Java version"""
        try:
            result = subprocess.run([self.java_path, '-version'],
                                  capture_output=True, text=True, timeout=10)
            if result.stderr:
                return result.stderr.split('\n')[0]
            return "Unknown"
        except Exception:
            return "Not found"

    def check_prerequisites(self) -> bool:
        """Check if all prerequisites are met"""
        print("Checking prerequisites...")

        # Check if JAR file exists
        if not os.path.exists(self.jar_path):
            print(f"ERROR: Server JAR not found at {self.jar_path}")
            print("Please run 'mvn clean package' first")
            return False
        print(f"Server JAR found: {self.jar_path}")

        # Check Java installation
        try:
            subprocess.run([self.java_path, '-version'],
                         capture_output=True, timeout=10, check=True)
            print("Java is installed and accessible")
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired, FileNotFoundError):
            print("ERROR: Java is not installed or not in PATH")
            return False

        print()
        return True

    def setup_environment(self, mode: TransportMode):
        """Setup environment variables for the specified mode"""
        env = os.environ.copy()
        env.update({
            'DB_URL': 'jdbc:h2:mem:testdb',
            'DB_USER': 'sa',
            'DB_PASSWORD': '',
            'DB_DRIVER': 'org.h2.Driver'
        })

        if mode == TransportMode.HTTP:
            env['HTTP_MODE'] = 'true'
            env['HTTP_PORT'] = str(self.port)
        else:
            env.pop('HTTP_MODE', None)
            env.pop('HTTP_PORT', None)

        return env

    def check_port_availability(self) -> bool:
        """Check if the HTTP port is available"""
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                sock.bind(('localhost', self.port))
                return True
        except OSError:
            print(f"ERROR: Port {self.port} is already in use!")
            print("Solutions:")
            print(f"1. Use a different port: python {sys.argv[0]} {self.jar_path} --port 9090")
            print(f"2. Stop the service using port {self.port}")
            print("3. Find what's using the port:")

            if platform.system() == "Windows":
                print(f"   netstat -ano | findstr :{self.port}")
            else:
                print(f"   lsof -i :{self.port}")

            return False

    def start_http_server(self) -> bool:
        """Start the HTTP server in background"""
        try:
            env = self.setup_environment(TransportMode.HTTP)
            cmd = [self.java_path, "-jar", self.jar_path]

            self.server_process = subprocess.Popen(
                cmd,
                env=env,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True
            )
            self._start_http_drain()

            # Wait for server to start and check for immediate failures
            max_wait = 12
            for i in range(max_wait):
                # Check if process has exited (failed to start)
                if self.server_process.poll() is not None:
                    stdout, stderr = self.server_process.communicate()
                    print(f"ERROR: Server process exited with code {self.server_process.returncode}")
                    if stderr:
                        print("Server error output:")
                        print(stderr)

                    # Check for specific error messages
                    if "already in use" in stderr or "BindException" in stderr:
                        print(f"ERROR: Port {self.port} is already in use!")

                    return False

                # Try health endpoint
                try:
                    response = requests.get(f"http://localhost:{self.port}/health", timeout=1)
                    if response.status_code == 200:
                        print(f"HTTP server started on port {self.port}")
                        return True
                except requests.exceptions.RequestException:
                    pass

                time.sleep(1)

            print(f"ERROR: HTTP server failed to respond within {max_wait} seconds")
            self.stop_http_server()
            return False

        except Exception as e:
            print(f"ERROR: Failed to start HTTP server: {e}")
            return False

    def stop_http_server(self):
        """Stop the HTTP server"""
        if self.server_process:
            try:
                self.server_process.terminate()
                self.server_process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self.server_process.kill()
                self.server_process.wait()
            except Exception as e:
                print(f"Warning: Error stopping server: {e}")
            finally:
                self.server_process = None
                self._http_drain_threads = []

    def _start_http_drain(self):
        if not self.server_process:
            return

        def drain(stream):
            if not stream:
                return
            for _ in stream:
                pass

        import threading
        for stream in (self.server_process.stdout, self.server_process.stderr):
            thread = threading.Thread(target=drain, args=(stream,), daemon=True)
            thread.start()
            self._http_drain_threads.append(thread)

    def test_health_endpoint(self) -> bool:
        """Test the health endpoint (HTTP mode only)"""
        try:
            response = requests.get(f"http://localhost:{self.port}/health", timeout=5)
            if response.status_code == 200:
                health_data = response.json()
                print(f"Health check response: {health_data}")
                return health_data.get("status") == "healthy"
            return False
        except Exception as e:
            print(f"Health check failed: {e}")
            return False

    def print_debug_info(self, test_name: str, request: Dict[str, Any], response: Optional[Dict[str, Any]], mode: TransportMode):
        """Print debug information for request/response pairs"""
        if not self.debug:
            return

        print(f"\n{'='*60}")
        print(f"DEBUG: {test_name} ({mode.value.upper()} mode)")
        print(f"{'='*60}")

        print("REQUEST:")
        print(json.dumps(request, indent=2, sort_keys=True))

        print("\nRESPONSE:")
        if response is None:
            print("None (no response expected or received)")
        else:
            print(json.dumps(response, indent=2, sort_keys=True))

        print(f"{'='*60}\n")

    def send_http_request(self, request: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """Send MCP request via HTTP"""
        try:
            response = requests.post(
                f"http://localhost:{self.port}/mcp",
                json=request,
                headers={"Content-Type": "application/json"},
                timeout=30
            )

            if response.status_code == 204:
                # Notification - no response expected
                return None
            elif response.status_code == 200:
                return response.json()
            else:
                print(f"HTTP error: {response.status_code} - {response.text}")
                return None

        except requests.exceptions.RequestException as e:
            print(f"HTTP request failed: {e}")
            return None

    def send_stdio_request(self, request: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """Send MCP request via stdio"""
        try:
            input_line = json.dumps(request) + "\n"
            env = self.setup_environment(TransportMode.STDIO)

            cmd = [self.java_path, "-jar", self.jar_path]
            process = subprocess.Popen(
                cmd,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                env=env
            )

            stdout, stderr = process.communicate(input=input_line, timeout=30)

            # Check for process errors
            if process.returncode != 0:
                print(f"Server process failed with return code {process.returncode}")
                if stderr:
                    print(f"Server error: {stderr}")
                return None

            if not stdout.strip():
                return None

            return json.loads(stdout.strip().split('\n')[0])

        except Exception as e:
            print(f"Stdio request failed: {e}")
            return None

    def send_request(self, request: Dict[str, Any], mode: TransportMode) -> Optional[Dict[str, Any]]:
        """Send MCP request using the specified transport mode"""
        if mode == TransportMode.HTTP:
            return self.send_http_request(request)
        else:
            return self.send_stdio_request(request)

    def send_stdio_session_requests(self, requests: List[Dict[str, Any]]) -> List[Optional[Dict[str, Any]]]:
        """Send multiple MCP requests via stdio in a single session"""
        try:
            env = self.setup_environment(TransportMode.STDIO)
            cmd = [self.java_path, "-jar", self.jar_path]

            process = subprocess.Popen(
                cmd,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                env=env
            )

            def start_stderr_drain():
                if not process.stderr:
                    return

                def drain():
                    for _ in process.stderr:
                        pass

                import threading
                thread = threading.Thread(target=drain, daemon=True)
                thread.start()

            def read_with_timeout(timeout=15):
                if not process.stdout:
                    return None
                if sys.platform == "win32":
                    import threading
                    import queue

                    result_queue = queue.Queue()

                    def reader():
                        try:
                            line = process.stdout.readline()
                            result_queue.put(line)
                        except Exception:
                            result_queue.put(None)

                    thread = threading.Thread(target=reader, daemon=True)
                    thread.start()

                    try:
                        return result_queue.get(timeout=timeout)
                    except queue.Empty:
                        return None
                else:
                    import select
                    ready, _, _ = select.select([process.stdout], [], [], timeout)
                    if ready:
                        return process.stdout.readline()
                    return None

            start_stderr_drain()
            responses = []

            for request in requests:
                input_line = json.dumps(request) + "\n"
                process.stdin.write(input_line)
                process.stdin.flush()

                # For notifications, don't expect a response
                if "id" not in request:
                    responses.append(None)
                    continue

                # Read response
                try:
                    output_line = read_with_timeout()
                    if output_line and output_line.strip():
                        response = json.loads(output_line.strip())
                        responses.append(response)
                    else:
                        responses.append(None)
                except json.JSONDecodeError:
                    responses.append(None)

            # Close stdin to signal end of input
            process.stdin.close()
            process.wait(timeout=5)

            return responses

        except Exception as e:
            print(f"Stdio session failed: {e}")
            return [None] * len(requests)

    def create_test_cases(self) -> List[TestCase]:
        """Create comprehensive test cases in proper MCP lifecycle order"""
        return [
            # Step 1: Initialize the protocol
            TestCase(
                name="Initialize Protocol",
                request={
                    "jsonrpc": "2.0",
                    "id": 1,
                    "method": "initialize",
                    "params": {
                        "protocolVersion": "2025-11-25",
                        "capabilities": {
                            "tools": {},
                            "resources": {}
                        },
                        "clientInfo": {"name": "test-client", "version": "1.0.0"}
                    }
                },
                expected_fields=["jsonrpc", "id", "result", "result.protocolVersion", "result.capabilities", "result.serverInfo"]
            ),

            # Step 2: Send initialized notification
            TestCase(
                name="Send Initialized Notification",
                request={
                    "jsonrpc": "2.0",
                    "method": "notifications/initialized"
                },
                expected_fields=[],
                is_notification=True,
                should_have_response=False
            ),

            # Step 3: Test ping functionality (if supported)
            TestCase(
                name="Ping Test",
                request={
                    "jsonrpc": "2.0",
                    "id": 2,
                    "method": "ping"
                },
                expected_fields=["jsonrpc", "id", "result"]
            ),

            # Step 4: Now we can use other methods
            TestCase(
                name="List Tools",
                request={
                    "jsonrpc": "2.0",
                    "id": 3,
                    "method": "tools/list",
                    "params": {}
                },
                expected_fields=["jsonrpc", "id", "result", "result.tools"]
            ),
            TestCase(
                name="List Resources",
                request={
                    "jsonrpc": "2.0",
                    "id": 4,
                    "method": "resources/list",
                    "params": {}
                },
                expected_fields=["jsonrpc", "id", "result", "result.resources"]
            ),

            # Create a table using the 'run_sql' tool
            TestCase(
                name="Create Table (run_sql)",
                request={
                    "jsonrpc": "2.0",
                    "id": 3,
                    "method": "tools/call",
                    "params": {
                        "name": "run_sql",
                        "arguments": {"sql": "CREATE TABLE protocol_test (id INT, name VARCHAR(255))"}
                    }
                },
                expected_fields=["jsonrpc", "id", "result", "result.content"]
            ),

            TestCase(
                name="Execute Select (run_sql)",
                request={
                    "jsonrpc": "2.0",
                    "id": 5,
                    "method": "tools/call",
                    "params": {
                        "name": "run_sql",
                        "arguments": {
                            "sql": "SELECT 1 as test_value, 'Hello MCP' as message",
                            "maxRows": 10
                        }
                    }
                },
                expected_fields=["jsonrpc", "id", "result", "result.content"]
            ),

            # Describe the new table using the 'describe_table' tool
            TestCase(
                name="Describe Table (describe_table)",
                request={
                    "jsonrpc": "2.0",
                    "id": 4,
                    "method": "tools/call",
                    "params": {
                        "name": "describe_table",
                        "arguments": {"table_name": "protocol_test"}
                    }
                },
                expected_fields=["jsonrpc", "id", "result", "result.content"]
            ),

            # PARAMETERIZED QUERY TESTS
            # Insert with parameters (int, string)
            TestCase(
                name="Parameterized Insert (run_sql)",
                request={
                    "jsonrpc": "2.0",
                    "id": 11,
                    "method": "tools/call",
                    "params": {
                        "name": "run_sql",
                        "arguments": {
                            "sql": "INSERT INTO protocol_test VALUES (?, ?)",
                            "params": [1, "Alice Smith"]
                        }
                    }
                },
                expected_fields=["jsonrpc", "id", "result", "result.content"]
            ),

            # Insert with null parameter
            TestCase(
                name="Parameterized Insert with Null (run_sql)",
                request={
                    "jsonrpc": "2.0",
                    "id": 13,
                    "method": "tools/call",
                    "params": {
                        "name": "run_sql",
                        "arguments": {
                            "sql": "INSERT INTO protocol_test VALUES (?, ?)",
                            "params": [2, None]
                        }
                    }
                },
                expected_fields=["jsonrpc", "id", "result", "result.content"]
            ),

            # Mixed parameter types test
            TestCase(
                name="Mixed Parameter Types (run_sql)",
                request={
                    "jsonrpc": "2.0",
                    "id": 16,
                    "method": "tools/call",
                    "params": {
                        "name": "run_sql",
                        "arguments": {
                            "sql": "INSERT INTO protocol_test VALUES (?, ?)",
                            "params": [3, "Bob Jones"]
                        }
                    }
                },
                expected_fields=["jsonrpc", "id", "result", "result.content"]
            ),

            TestCase(
                name="Read Database Info Resource",
                request={
                    "jsonrpc": "2.0",
                    "id": 6,
                    "method": "resources/read",
                    "params": {"uri": "database://info"}
                },
                expected_fields=["jsonrpc", "id", "result", "result.contents"]
            ),

            # Error tests
            TestCase(
                name="Error Test - Invalid Method",
                request={
                    "jsonrpc": "2.0",
                    "id": 7,
                    "method": "invalid/method",
                    "params": {}
                },
                expected_fields=["jsonrpc", "id", "error", "error.code", "error.message"]
            ),
            TestCase(
                name="Error Test - Empty SQL",
                request={
                    "jsonrpc": "2.0",
                    "id": 8,
                    "method": "tools/call",
                    "params": {
                        "name": "run_sql",
                        "arguments": {"sql": "", "maxRows": 10}
                    }
                },
                expected_fields=["jsonrpc", "id", "error", "error.code", "error.message"]
            ),

            # ID format tests
            TestCase(
                name="ID Test - Null ID",
                request={
                    "jsonrpc": "2.0",
                    "id": None,
                    "method": "tools/list",
                    "params": {}
                },
                expected_fields=["jsonrpc", "id", "result"]
            ),
            TestCase(
                name="ID Test - String ID",
                request={
                    "jsonrpc": "2.0",
                    "id": "test-string-id",
                    "method": "tools/list",
                    "params": {}
                },
                expected_fields=["jsonrpc", "id", "result"]
            ),

            # Additional ping tests with different scenarios
            TestCase(
                name="Ping Test - String ID",
                request={
                    "jsonrpc": "2.0",
                    "id": "ping-test-string",
                    "method": "ping"
                },
                expected_fields=["jsonrpc", "id", "result"]
            ),
            TestCase(
                name="Ping Test - Null ID",
                request={
                    "jsonrpc": "2.0",
                    "id": None,
                    "method": "ping"
                },
                expected_fields=["jsonrpc", "id", "result"]
            )
        ]

    def validate_json_rpc(self, response: Dict[str, Any], expected_id: Any = None) -> List[str]:
        """Validate JSON-RPC 2.0 compliance"""
        errors = []

        # Check required fields
        if response.get("jsonrpc") != "2.0":
            errors.append("Missing or invalid 'jsonrpc' field")

        # Validate ID field - must exactly match what was sent
        if "id" in response:
            actual_id = response["id"]
            if expected_id != actual_id:
                errors.append(f"ID mismatch: expected {repr(expected_id)}, got {repr(actual_id)}")
        else:
            if expected_id is not None:
                errors.append(f"Missing 'id' field, expected {repr(expected_id)}")

        # Must have either result or error, but not both
        has_result = "result" in response
        has_error = "error" in response

        if not has_result and not has_error:
            errors.append("Response must have either 'result' or 'error'")
        elif has_result and has_error:
            errors.append("Response cannot have both 'result' and 'error'")

        # Validate error structure if present
        if has_error:
            error = response["error"]
            if not isinstance(error, dict):
                errors.append("Error field must be an object")
            else:
                if "code" not in error or not isinstance(error["code"], int):
                    errors.append("Error object missing or invalid 'code' field")
                if "message" not in error or not isinstance(error["message"], str):
                    errors.append("Error object missing or invalid 'message' field")

        # Check for unexpected extra fields in root response
        allowed_root_fields = {"jsonrpc", "id", "result", "error"}
        extra_fields = set(response.keys()) - allowed_root_fields
        if extra_fields:
            errors.append(f"Unexpected extra fields in response: {', '.join(sorted(extra_fields))}")

        return errors

    def validate_expected_fields(self, response: Dict[str, Any], expected_fields: List[str]) -> List[str]:
        """Validate that expected fields are present in the response"""
        errors = []

        for field_path in expected_fields:
            current = response
            parts = field_path.split('.')

            for part in parts:
                if isinstance(current, dict) and part in current:
                    current = current[part]
                else:
                    errors.append(f"Missing expected field: {field_path}")
                    break

        return errors

    def is_vendor_extension(self, field_name: str) -> bool:
        """Check if a field is a valid vendor extension (x- prefix)"""
        return field_name.startswith("x-")

    def validate_mcp_response_structure(self, response: Dict[str, Any], method: str) -> List[str]:
        """Validate MCP-specific response structure based on method"""
        errors = []

        if "error" in response:
            # Error responses are handled by validate_json_rpc
            return errors

        if "result" not in response:
            return errors

        result = response["result"]

        # Method-specific validation
        if method == "initialize":
            required_fields = {"protocolVersion", "capabilities", "serverInfo"}
            missing = required_fields - set(result.keys())
            if missing:
                errors.append(f"Initialize result missing required fields: {', '.join(missing)}")

            # Check for unexpected fields in initialize result (allow x- extensions)
            allowed_fields = {"protocolVersion", "capabilities", "serverInfo"}
            extra_fields = {f for f in result.keys() if f not in allowed_fields and not self.is_vendor_extension(f)}
            if extra_fields:
                errors.append(f"Initialize result has unexpected fields: {', '.join(sorted(extra_fields))}")

        elif method == "tools/list":
            if not isinstance(result.get("tools"), list):
                errors.append("tools/list result must contain 'tools' array")
            # Check for unexpected fields (allow x- extensions)
            allowed_fields = {"tools"}
            extra_fields = {f for f in result.keys() if f not in allowed_fields and not self.is_vendor_extension(f)}
            if extra_fields:
                errors.append(f"tools/list result has unexpected fields: {', '.join(sorted(extra_fields))}")

        elif method == "resources/list":
            if not isinstance(result.get("resources"), list):
                errors.append("resources/list result must contain 'resources' array")
            # Check for unexpected fields (allow x- extensions)
            allowed_fields = {"resources"}
            extra_fields = {f for f in result.keys() if f not in allowed_fields and not self.is_vendor_extension(f)}
            if extra_fields:
                errors.append(f"resources/list result has unexpected fields: {', '.join(sorted(extra_fields))}")

        elif method == "tools/call":
            if "content" not in result:
                errors.append("tools/call result must contain 'content' field")
            # Check for unexpected fields (allow x- extensions)
            allowed_fields = {"content", "isError"}  # isError is optional
            extra_fields = {f for f in result.keys() if f not in allowed_fields and not self.is_vendor_extension(f)}
            if extra_fields:
                errors.append(f"tools/call result has unexpected fields: {', '.join(sorted(extra_fields))}")

        elif method == "resources/read":
            if "contents" not in result:
                errors.append("resources/read result must contain 'contents' field")
            # Check for unexpected fields (allow x- extensions)
            allowed_fields = {"contents"}
            extra_fields = {f for f in result.keys() if f not in allowed_fields and not self.is_vendor_extension(f)}
            if extra_fields:
                errors.append(f"resources/read result has unexpected fields: {', '.join(sorted(extra_fields))}")

        elif method == "ping":
            # Ping can have vendor extensions but no standard fields
            allowed_fields = set()  # No standard fields for ping
            extra_fields = {f for f in result.keys() if f not in allowed_fields and not self.is_vendor_extension(f)}
            if extra_fields:
                errors.append(f"ping result has unexpected standard fields: {', '.join(sorted(extra_fields))}")

        return errors

    def run_test_case(self, test_case: TestCase, mode: TransportMode) -> TestCase:
        """Run a single test case and validate the response"""
        start_time = time.time()
        result_case = TestCase(
            name=test_case.name,
            request=test_case.request,
            expected_fields=test_case.expected_fields,
            is_notification=test_case.is_notification,
            should_have_response=test_case.should_have_response
        )

        try:
            response = self.send_request(test_case.request, mode)
            result_case.execution_time = time.time() - start_time

            # Print debug info if enabled
            self.print_debug_info(test_case.name, test_case.request, response, mode)

            # Handle notifications (should have NO response)
            if test_case.is_notification and not test_case.should_have_response:
                if response is None:
                    result_case.result = TestResult.PASS
                    result_case.response = None
                    return result_case
                else:
                    result_case.result = TestResult.FAIL
                    result_case.error_message = f"Unexpected response for notification"
                    result_case.response = response
                    return result_case

            # Handle regular requests (should have response)
            if response is None:
                result_case.result = TestResult.ERROR
                result_case.error_message = "No response from server"
                return result_case

            result_case.response = response

            # Validate response
            errors = []
            expected_id = test_case.request.get("id") if "id" in test_case.request else None

            errors.extend(self.validate_json_rpc(response, expected_id))
            errors.extend(self.validate_expected_fields(response, test_case.expected_fields))

            # MCP-specific structure validation
            method = test_case.request.get("method", "")
            errors.extend(self.validate_mcp_response_structure(response, method))

            if errors:
                result_case.result = TestResult.FAIL
                result_case.error_message = "; ".join(errors)
            else:
                result_case.result = TestResult.PASS

        except Exception as e:
            result_case.result = TestResult.ERROR
            result_case.error_message = f"Unexpected error: {str(e)}"
            result_case.execution_time = time.time() - start_time

        return result_case

    def run_mode_tests(self, mode: TransportMode) -> ModeResults:
        """Run all tests for a specific transport mode"""
        print(f"\n{'='*60}")
        print(f"TESTING {mode.value.upper()} MODE")
        print(f"{'='*60}")

        if mode == TransportMode.HTTP and not HAS_REQUESTS:
            print("❌ Skipping HTTP tests - requests library not available")
            print("Install with: pip install requests")
            return ModeResults(mode, 0, 0, 0, 0, [], 0.0)

        # Pre-flight checks for HTTP
        if mode == TransportMode.HTTP:
            if not self.check_port_availability():
                return ModeResults(mode, 0, 0, 0, 1, [], 0.0)

            if not self.start_http_server():
                return ModeResults(mode, 0, 0, 0, 1, [], 0.0)

            # Test health endpoint
            print("Testing health endpoint...")
            if self.test_health_endpoint():
                print("✅ Health endpoint working")
            else:
                print("❌ Health endpoint failed")
                self.stop_http_server()
                return ModeResults(mode, 0, 0, 0, 1, [], 0.0)

        try:
            test_cases = self.create_test_cases()
            results = []
            passed = failed = errors = 0

            print(f"\nRunning {len(test_cases)} test cases...")
            print("-" * 40)

            if mode == TransportMode.STDIO:
                # For stdio, run all requests in a single session to maintain state
                requests = [tc.request for tc in test_cases]
                responses = self.send_stdio_session_requests(requests)

                for i, (test_case, response) in enumerate(zip(test_cases, responses), 1):
                    print(f"Test {i:2d}: {test_case.name}...", end=" ")

                    # Print debug info if enabled
                    self.print_debug_info(test_case.name, test_case.request, response, mode)

                    result = self.validate_test_response(test_case, response)
                    results.append(result)

                    if result.result == TestResult.PASS:
                        passed += 1
                        print(f"✅ PASS ({result.execution_time:.2f}s)")
                    elif result.result == TestResult.FAIL:
                        failed += 1
                        print(f"❌ FAIL ({result.execution_time:.2f}s)")
                        print(f"    Error: {result.error_message}")
                    else:
                        errors += 1
                        print(f"🔥 ERROR ({result.execution_time:.2f}s)")
                        print(f"    Error: {result.error_message}")
            else:
                # For HTTP, run each test individually (server maintains state)
                for i, test_case in enumerate(test_cases, 1):
                    print(f"Test {i:2d}: {test_case.name}...", end=" ")

                    result = self.run_test_case(test_case, mode)
                    results.append(result)

                    if result.result == TestResult.PASS:
                        passed += 1
                        print(f"✅ PASS ({result.execution_time:.2f}s)")
                    elif result.result == TestResult.FAIL:
                        failed += 1
                        print(f"❌ FAIL ({result.execution_time:.2f}s)")
                        print(f"    Error: {result.error_message}")
                    else:
                        errors += 1
                        print(f"🔥 ERROR ({result.execution_time:.2f}s)")
                        print(f"    Error: {result.error_message}")

            success_rate = (passed / len(test_cases)) * 100 if test_cases else 0
            return ModeResults(mode, len(test_cases), passed, failed, errors, results, success_rate)

        finally:
            if mode == TransportMode.HTTP:
                self.stop_http_server()

    def validate_test_response(self, test_case: TestCase, response: Optional[Dict[str, Any]]) -> TestCase:
        """Validate a test response and return result"""
        result_case = TestCase(
            name=test_case.name,
            request=test_case.request,
            expected_fields=test_case.expected_fields,
            is_notification=test_case.is_notification,
            should_have_response=test_case.should_have_response
        )

        # Handle notifications (should have NO response)
        if test_case.is_notification and not test_case.should_have_response:
            if response is None:
                result_case.result = TestResult.PASS
                result_case.response = None
                return result_case
            else:
                result_case.result = TestResult.FAIL
                result_case.error_message = f"Unexpected response for notification"
                result_case.response = response
                return result_case

        # Handle regular requests (should have response)
        if response is None:
            result_case.result = TestResult.ERROR
            result_case.error_message = "No response from server"
            return result_case

        result_case.response = response

        # Validate response
        errors = []
        expected_id = test_case.request.get("id") if "id" in test_case.request else None

        errors.extend(self.validate_json_rpc(response, expected_id))
        errors.extend(self.validate_expected_fields(response, test_case.expected_fields))

        # MCP-specific structure validation
        method = test_case.request.get("method", "")
        errors.extend(self.validate_mcp_response_structure(response, method))

        if errors:
            result_case.result = TestResult.FAIL
            result_case.error_message = "; ".join(errors)
        else:
            result_case.result = TestResult.PASS

        return result_case

    def print_mode_summary(self, results: ModeResults):
        """Print summary for a specific mode"""
        print(f"\n{results.mode.value.upper()} MODE SUMMARY:")
        print("-" * 30)
        print(f"Total Tests: {results.total}")
        print(f"✅ Passed: {results.passed}")
        print(f"❌ Failed: {results.failed}")
        print(f"🔥 Errors: {results.errors}")
        print(f"Success Rate: {results.success_rate:.1f}%")

    def print_overall_summary(self, stdio_results: ModeResults, http_results: ModeResults):
        """Print overall test summary and comparison"""
        print(f"\n{'='*70}")
        print("OVERALL SUMMARY")
        print(f"{'='*70}")

        # Mode comparison table
        print(f"{'Mode':<8} {'Total':<6} {'Passed':<7} {'Failed':<7} {'Errors':<7} {'Success Rate':<12}")
        print("-" * 60)
        print(f"{'STDIO':<8} {stdio_results.total:<6} {stdio_results.passed:<7} {stdio_results.failed:<7} {stdio_results.errors:<7} {stdio_results.success_rate:<11.1f}%")

        if http_results.total > 0:
            print(f"{'HTTP':<8} {http_results.total:<6} {http_results.passed:<7} {http_results.failed:<7} {http_results.errors:<7} {http_results.success_rate:<11.1f}%")
        else:
            print(f"{'HTTP':<8} {'SKIPPED (missing requests library)'}")

        # Overall status
        print()
        stdio_ok = stdio_results.success_rate == 100.0
        http_ok = http_results.total == 0 or http_results.success_rate == 100.0

        if stdio_ok and http_ok:
            print("🎉 ALL TESTS PASSED! Both transport modes are working correctly.")
        elif stdio_ok and http_results.total == 0:
            print("✅ STDIO mode working. HTTP tests skipped (install 'requests' to test HTTP mode).")
        elif stdio_ok:
            print("⚠️  STDIO mode working, but HTTP mode has issues.")
        elif http_ok and http_results.total > 0:
            print("⚠️  HTTP mode working, but STDIO mode has issues.")
        else:
            print("❌ BOTH modes have issues that need to be addressed.")

        # Show detailed failures
        all_failures = []
        for result in stdio_results.test_cases + http_results.test_cases:
            if result.result != TestResult.PASS:
                all_failures.append((result.name, result.error_message))

        if all_failures:
            print(f"\nFailed Test Details:")
            print("-" * 30)
            for name, error in all_failures:
                print(f"❌ {name}: {error}")

    def run_all_tests(self, mode: TransportMode = TransportMode.BOTH) -> Tuple[ModeResults, ModeResults]:
        """Run tests for specified mode(s)"""
        self.print_header()

        if not self.check_prerequisites():
            return ModeResults(TransportMode.STDIO, 0, 0, 0, 1, [], 0.0), ModeResults(TransportMode.HTTP, 0, 0, 0, 1, [], 0.0)

        stdio_results = ModeResults(TransportMode.STDIO, 0, 0, 0, 0, [], 0.0)
        http_results = ModeResults(TransportMode.HTTP, 0, 0, 0, 0, [], 0.0)

        # Run tests based on mode selection
        if mode in [TransportMode.STDIO, TransportMode.BOTH]:
            stdio_results = self.run_mode_tests(TransportMode.STDIO)
            self.print_mode_summary(stdio_results)

        if mode in [TransportMode.HTTP, TransportMode.BOTH]:
            http_results = self.run_mode_tests(TransportMode.HTTP)
            self.print_mode_summary(http_results)

        if mode == TransportMode.BOTH:
            self.print_overall_summary(stdio_results, http_results)

        return stdio_results, http_results

def main():
   """Main function with comprehensive argument parsing"""

   jar_path = None
   mode = TransportMode.BOTH
   java_path = "java"
   port = 8080
   debug = False

   # Parse args, allowing jar_path to be optional
   args = sys.argv[1:]
   if args and not args[0].startswith("--"):
       jar_path = args[0]
       args = args[1:]
   else:
       jars = sorted(glob.glob("target/dbchat-*.jar"), key=os.path.getmtime, reverse=True)
       if jars:
           jar_path = jars[0]
           print(f"Auto-detected JAR: {jar_path}")
       else:
           print("Error: No dbchat JAR found in target/. Please run 'mvn clean package'.")
           sys.exit(1)

   i = 0
   while i < len(args):
       arg = args[i]
       if arg == "--help":
           print("Usage: python test-mcp-protocol.py [jar_path] [options]")
           print()
           print("Options:")
           print("  --mode <stdio|http|both>     Test mode (default: both)")
           print("  --java <path>                Java executable path (default: java)")
           print("  --port <number>              HTTP port for testing (default: 8080)")
           print("  --debug                      Enable debug mode (show request/response)")
           print("  --help                       Show this help message")
           print()
           print("Examples:")
           print("  python test-mcp-protocol.py target/dbchat-2.0.0.jar")
           print("  python test-mcp-protocol.py --mode http --port 9090")
           print("  python test-mcp-protocol.py --debug --mode stdio")
           sys.exit(0)
       elif arg == "--mode" and i + 1 < len(args):
           try:
               mode_str = args[i + 1].lower()
               mode = TransportMode(mode_str) if mode_str != "both" else TransportMode.BOTH
           except ValueError:
               print(f"Invalid mode: {args[i + 1]}. Use stdio, http, or both")
               sys.exit(1)
           i += 2
       elif arg == "--java" and i + 1 < len(args):
           java_path = args[i + 1]
           i += 2
       elif arg == "--port" and i + 1 < len(args):
           try:
               port = int(args[i + 1])
           except ValueError:
               print(f"Invalid port: {args[i + 1]}")
               sys.exit(1)
           i += 2
       elif arg == "--debug":
           debug = True
           i += 1
       else:
           print(f"Unknown argument: {arg}")
           sys.exit(1)

   # Validate JAR file
   if not os.path.exists(jar_path):
       print(f"Error: JAR file not found: {jar_path}")
       sys.exit(1)

   # Check for requests library if HTTP testing requested
   if mode in [TransportMode.HTTP, TransportMode.BOTH] and not HAS_REQUESTS:
       print("Warning: HTTP testing requires 'requests' library")
       print("Install with: pip install requests")
       if mode == TransportMode.HTTP:
           print("Cannot run HTTP-only tests without requests library")
           sys.exit(1)

   # Create tester and run tests
   tester = MCPTester(jar_path=jar_path, java_path=java_path, port=port, debug=debug)

   # Handle Ctrl+C gracefully
   def signal_handler(sig, frame):
       print("\n\nTest interrupted by user")
       tester.stop_http_server()
       sys.exit(1)

   signal.signal(signal.SIGINT, signal_handler)

   try:
       stdio_results, http_results = tester.run_all_tests(mode)

       # Determine exit code
       stdio_ok = stdio_results.total == 0 or stdio_results.success_rate == 100.0
       http_ok = http_results.total == 0 or http_results.success_rate == 100.0

       if stdio_ok and http_ok:
           sys.exit(0)
       else:
           sys.exit(1)

   except KeyboardInterrupt:
       print("\n\nTest interrupted by user")
       tester.stop_http_server()
       sys.exit(1)
   except Exception as e:
       print(f"\n\nUnexpected error: {e}")
       tester.stop_http_server()
       sys.exit(1)

if __name__ == "__main__":
   main()
