# Cleanup Test Log/Warning Messages

## Goal
Clean up warnings and unnecessary log messages during test runs.

## Issues Identified

### 1. Compiler Warnings (Pattern Match Exhaustivity)
- **ElapsedTime.scala:82, 101** - TimeUnit match may not be exhaustive
- **LogLevel.scala:61** - java.util.logging.Level match may not be exhaustive

### 2. Test Log Messages to Suppress

| Source | Type | Message | Fix Strategy |
|--------|------|---------|--------------|
| SingletonTest:84 | info | Surface class info | Remove debug log |
| Retry.scala:109 | warn | Retry logging | Use `.noRetryLogging` in test |
| ToolEnabledChatSession:114 | warn | No tool executor configured | Use `.suppressWarnings` or change to debug |
| LocalToolExecutor:72,80 | error/warn | Tool execution error, tool not found | Use `.suppressWarnings` in test |
| CircuitBreaker:78 | info | State change logging | Use `.onStateChange(_ => ())` in tests |
| DITest | warn | Self-cycle/cyclic dependency tests | These are intentional for testing |
| LifeCycleManager | info | Lifecycle start/stop | Tests already use `noLifeCycleLogging` |
| NettyHttpServer:100,105 | info | Server start/stop | Change to debug level |
| BedrockChat:128 | warn | Ignoring top-k parameter | Change to debug level |
| SpinnerTest | stderr | Override, Test, Done, Error | Redirect stream to suppress |
| LogLevel:51 | stderr | Unknown log level | This is intentional test output |

### 3. Console Output
- SpinnerTest outputs to stderr (Override, Test, ✔ Done, ✖ Error)

## Implementation Plan

### Phase 1: Fix Compiler Warnings

1. **ElapsedTime.scala** - Add wildcard case or explicit cases for all TimeUnit values
2. **LogLevel.scala** - Add wildcard case for unknown java.util.logging.Level values

### Phase 2: Reduce Log Verbosity in Production Code

1. **NettyHttpServer.scala:100,105** - Change `info` to `debug` for server start/stop
2. **BedrockChat.scala:128** - Change `warn` to `debug` for top-k parameter warning
3. **ToolEnabledChatSession.scala:114** - Change `warn` to `debug` for no tool executor
4. **LocalToolExecutor.scala:72,80** - Keep error/warn but add `trace` for normal debug

### Phase 3: Suppress Logs in Tests

1. **CircuitBreakerTest.scala** - Use `.onStateChange(_ => ())` to suppress state change logs
2. **RetryTest.scala** - Ensure tests use `.noRetryLogging` where retry logging is not needed
3. **LocalToolExecutorTest.scala** - Wrap tests with `Logger.of[LocalToolExecutor].suppressWarnings`
4. **ToolEnabledChatSessionTest.scala** - Wrap tests with log suppression
5. **SpinnerTest.scala** - Use custom PrintStream to suppress output during tests
6. **SingletonTest.scala:84** - Remove or change `info` to `debug`

## Files to Modify

### Source Files
- `uni/src/main/scala/wvlet/uni/util/ElapsedTime.scala`
- `uni-core/src/main/scala/wvlet/uni/log/LogLevel.scala`
- `uni-netty/src/main/scala/wvlet/uni/http/netty/NettyHttpServer.scala`
- `uni-agent-bedrock/src/main/scala/wvlet/uni/agent/chat/bedrock/BedrockChat.scala`
- `uni-agent/src/main/scala/wvlet/uni/agent/chat/ToolEnabledChatSession.scala`
- `uni-agent/src/main/scala/wvlet/uni/agent/tool/LocalToolExecutor.scala`

### Test Files
- `uni/src/test/scala/wvlet/uni/control/CircuitBreakerTest.scala`
- `uni/src/test/scala/wvlet/uni/control/RetryTest.scala`
- `uni-agent/src/test/scala/wvlet/uni/agent/tool/LocalToolExecutorTest.scala`
- `uni-agent/src/test/scala/wvlet/uni/agent/chat/ToolEnabledChatSessionTest.scala`
- `uni/src/test/scala/wvlet/uni/cli/SpinnerTest.scala`
- `uni/src/test/scala/wvlet/uni/design/SingletonTest.scala`

## Verification

1. Run `./sbt compile` - should have no warnings
2. Run `./sbt test 2>&1 | grep -E "(warn|error|info.*\[0m)" | head -50` - should show minimal log output
3. Run `./sbt test` - all tests should pass
