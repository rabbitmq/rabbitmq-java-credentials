# Known Issues

These issues come from the code this library was extracted from (the RabbitMQ AMQP 1.0 Java client and the RabbitMQ stream Java client).
They are not fixed yet, so that the extraction keeps the behaviour identical and the fixes stay separate changes.

## Data race on the registrations in `TokenCredentialsManager#getToken()`

Location: `TokenCredentialsManager#getToken()`, the two `LOGGER.debug` calls that use `registrationSummary(this.registrations.values())`.

Problem: `getToken()` runs on the `executorService` (it is dispatched by `requestToken()`), but `registrations` is a plain `HashMap` confined to the serial executor (`loop`).
With debug logging on, the iteration races with `onRegister`/`onUnregister`/`onClose` on the loop: it can see stale or inconsistent content, or throw a `ConcurrentModificationException`.
That exception is caught in `requestToken()` and reported through `onTokenFailure`, so a logging problem turns into a failed token request (failed `connect` calls, refresh retry).

Suggested fix: compute the registration summary on the loop in `requestToken()` and pass it to `getToken(String summary)`, or remove the registration summary from these two log statements.

Test: enable debug logging for `TokenCredentialsManager` in a test, then register and close registrations concurrently while refreshes run, and assert no token request fails.
The race is timing-dependent: loop many times, or check the fix by review.

## `TokenCredentialsManager#ratioRefreshDelayStrategy(float)` accepts a ratio of 0

Location: `TokenCredentialsManager#ratioRefreshDelayStrategy(float)`, the ratio validation.

Problem: the check is `ratio < 0 || ratio > 1`, while the message says "Ratio should be > 0 and <= 1".
A ratio of `0` is accepted and gives a zero refresh delay, so the token is refreshed again immediately after every refresh (a busy loop against the token endpoint).
`NaN` passes both comparisons and is accepted as well.

Suggested fix: reject `ratio <= 0` and `NaN`, e.g. `if (!(ratio > 0 && ratio <= 1))`.

Test: `ratioRefreshDelayStrategy(0f)` and `ratioRefreshDelayStrategy(Float.NaN)` throw `IllegalArgumentException`, `ratioRefreshDelayStrategy(1f)` does not.

## Ineffective condition in `TokenCredentialsManager#dispatchUpdates(Token)`

Location: `TokenCredentialsManager#dispatchUpdates(Token)`, `if (debug() || dispatchedCount > 0) { LOGGER.debug(...); }`.

Problem: the `dispatchedCount > 0` part has no effect, since `LOGGER.debug` logs nothing when debug is off.
The intent was probably to log at debug level only when there is something to report, or to log at a higher level when registrations were updated.

Suggested fix: decide the intent, then either use `if (debug() && dispatchedCount > 0)` (or just `if (debug())`), or use `LOGGER.info` when `dispatchedCount > 0`.

Test: none needed (logging only).
