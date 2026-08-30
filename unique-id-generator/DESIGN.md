# Unique ID Generator (Snowflake) Design

Implements Chapter 7 of *System Design Interview* (Alex Xu): a distributed 64-bit unique ID
generator, built as a Twitter Snowflake variant in Java 21 & Maven.

## 1. Problem & scope

Requirements from the book: IDs must be unique, numeric, fit in 64 bits, be roughly ordered by
creation time, and support 10,000+ IDs/sec. The book finally picks the Snowflake layout (sign bit
+ timestamp + datacenter ID + machine ID + sequence) but does not solve two issues:

- **Clock rollback.** The book mentions clock synchronization as beyond scope, without
  saying what a generator should actually do if the local clock steps backwards. This
  is the main point we add on top of the book (§4).
- **Worker/datacenter ID assignment.** The book says these are "chosen at startup" without
  saying how. We ship the simplest possible answer (static configuration via
  environment variables). This is a distributed-coordination problem aside from what our ID generator
  needs to prove, and would be scope creep for this exercise. Instead, we define a `WorkerIdAssigner`
  (§5) so a smarter implementation could be dropped in later without touching
  the generator itself.
- **HTTP service wrapper.** We define the `IdGenerator.nextId()` method so a thin HTTP layer could call it.

## 2. Architecture overview

Three small interfaces define the parts that are likely to change and the parts that are the actual algorithm:

- **`IdGenerator`** (`long nextId()`) is the public surface. A future HTTP layer, or any other
  caller, depends only on this, not on anything Snowflake-specific.
- **`TimeSource`** (`long currentTimeMillis()`) — the generator's only way of knowing what time
  it is. It never calls `System.currentTimeMillis()` directly, which makes the
  clock-rollback and sequence-overflow logic testable: tests substitute a
  fake clock that returns whatever sequence of times a test wants (including "jump backward"),
  instead of needing to manipulate the real system clock or use `Thread.sleep`.
- **`WorkerIdAssigner`** (`WorkerId assign(SnowflakeConfig config)`) is how a machine learns its
  own datacenter/worker ID. One implementation ships (`EnvironmentWorkerIdAssigner`); the
  interface exists so that can be swapped later (§5).

Everything else is a concrete class with one job:

| Class | Responsibility |
|---|---|
| `SnowflakeConfig` | Validated, immutable bit-layout settings (record) |
| `WorkerId` | A (datacenterId, workerId) pair + range validation |
| `SnowflakeIdCodec` | Stateless bit-packing: `encode`/`decode` |
| `SnowflakeIdGenerator` | The only concurrency-sensitive state (last timestamp, sequence) |
| `EnvironmentWorkerIdAssigner` | Reads `SNOWFLAKE_DATACENTER_ID`/`SNOWFLAKE_WORKER_ID` |
| `SystemTimeSource` | Wraps `java.time.Clock` |
| `DecodedId` | The four fields an ID decodes into |
| `InvalidConfigurationException` / `ClockMovedBackwardsException` | The two failure modes |

## 3. Bit layout

A 64-bit `long`: `[ sign (1, unused) | timestamp (41) | datacenter | worker | sequence ]`,
where the last three fields are configurable but must sum to 22 bits (`64 - 1 - 41`). The
machine count is configurable; timestamp width is fixed at 41 bits so
the ~69-year usable window (from whatever epoch is chosen) is never accidentally shrunk by a
config change. `SnowflakeConfig.defaults()` uses the classic 5/5/12 split and a custom epoch of
2026-01-01T00:00:00Z (a round and recent date), chosen (like Twitter's own 2010 epoch) to push the
41-bit rollover as far into the future as possible from when this generator actually starts
being used, rather than defaulting to 1970 and wasting decades of possible timestamps.

The sign bit is explicitly left at 0 purely because the other four fields' widths add up to 63 bits, one short of 64,
following Twitter's implementation. `encode`/`decode` don't guard against the
41-bit timestamp field overflowing (~69 years from the epoch, or a caller passing a bad
timestamp); this is treated as a known and accepted limitation (§9).

## 4. Clock-rollback handling (the main addition over the book)

`SnowflakeIdGenerator.nextId()` compares each call's current time against the last timestamp
it used, inside a `synchronized` block:

- **Clock went backward, small drift** (≤ `maxBackwardDriftMillis`, default 10ms): drift is treated
  as self-healing and we wait (`TimeSource.waitForNextMillis`) until the clock passes the
  last timestamp, then proceed as normal. This covers routine causes: Network Time Protocol step corrections
  after ordinary hardware clock drift, a VM's clock pause or live migration, or a leap second.
- **Clock went backward, large drift**: fails with `ClockMovedBackwardsException` (carrying
  the observed drift in milliseconds) rather than blocking indefinitely on what's likely a real
  clock/host problem. This allows the caller to handle this potential problem.
- **Same millisecond as last call**: increment the sequence counter; if it overflows past the
  configured max, wait for the next millisecond and reset to 0.
- **Later millisecond**: reset the sequence counter to 0.

## 5. Worker/datacenter ID coordination

**Shipped:** `EnvironmentWorkerIdAssigner` reads `SNOWFLAKE_DATACENTER_ID` /
`SNOWFLAKE_WORKER_ID`, validates them against the configured bit widths, and fails
(`InvalidConfigurationException`) if they're missing, non-numeric, or out of range.

**Deferred, on purpose:** automatically assigning worker IDs instead of setting them by hand,
for example, having machines coordinate through ZooKeeper or leasing a row from a database table.
All of these solve a real problem regarding distributed coordination, not ID generation itself, which is a different exercise than this one. `WorkerIdAssigner` (§2) was created so that adding one of these later means writing one new class, not touching
`SnowflakeIdGenerator` or anything downstream of it.

## 6. Concurrency model

Many callers can ask for an ID from the same generator at the same time, so it has to stay
correct under that pressure. The entire `nextId()` method is marked `synchronized`, which means
only one caller can be running it at any given moment while everyone else must wait their turn.
The method itself is tiny (a few comparisons and updating two numbers), not something doing slow work.

To ensure this holds up under pressure (a practical check),
`SnowflakeIdGeneratorConcurrencyTest` throws 50,000 concurrent virtual threads
at one shared generator, all firing at once against the real system clock, and checks that not
a single duplicate ID comes out, and that every ID's timestamp looks sane.

We cannot claim that the order the IDs come out in matches the order the 50,000
threads were originally launched in. With that many threads racing each other, who
gets to go first is up to the scheduler, not launch order.

**Considered, not built:** a lock-free version using low-level atomic operations instead of
`synchronized`, which could handle heavier load faster. It's a valid technique but the
code needed to make it correct is meaningfully trickier to verify than a plain `synchronized`
block.

## 7. Testing strategy

JUnit 5 + AssertJ. The key object across almost every test is
`TimeSource`: `SnowflakeIdGeneratorTest` uses a small hand-written `FakeTimeSource` that
lets a test directly set "what time is it" and jump it forward/backward on command, including
overriding `waitForNextMillis` to skip straight to "time advanced by 1ms" instead of actually advancing.
That's what makes scenarios like sequence overflow and both clock-rollback branches
easy to test. `SnowflakeIdCodecTest` includes a value hand-computed independently of the encode/decode logic itself,
so the test proves the actual bit positions are correct rather than just that encode and decode undo each
other. `IdGeneratorIntegrationTest` wires every real component together (real config, real env-based
assigner, real system clock) rather than testing pieces in isolation.

## 8. How AI was used

This implementation was built using Claude Code's help across an incremental plan decided and written out before
any code was generated, including the specific scope decisions in §1. Code was then generated step by step, verified and reviewed by me,
each step compiled and tested before moving to the next. The decision on how to tackle the clock rollback issue was also discussed
with Claude Code.

Every non-trivial piece of code above was walked through and understood and can be properly explained; this design document has also been
manually reviewed and rewritten.

## 9. Known limitations & future work

- **41-bit timestamp overflow** (~69 years after the epoch, or a caller-supplied timestamp
  outside that range) is not guarded against at runtime, which is accepted as a known limitation
  matching the book's own explanation.
- **HTTP layer**: not built. `IdGenerator.nextId()` could be wrapped in
  `com.sun.net.httpserver` (or any framework) which would need no changes to the code above it.
- **Worker-ID auto-assignment** (something like ZooKeeper or a DB lease table): not built
  by choice, the clock reversal issue was prioritized. `WorkerIdAssigner` could hold a new implementation to solve this.
- **Lock-free generator variant**: used `synchronized` instead (see §6).
- **No caller-side handling for `ClockMovedBackwardsException`**: it's an unchecked exception
  by design, so nothing forces a caller to catch it, and nothing in this codebase currently
  does. Today it would simply crash whatever called `nextId()` (which could happen in `Main`). Choosing what to
  do about it (retry after a delay, alert on-call, fail just that one request) is a policy
  whoever calls the generator must decide on, which today is nobody, since the HTTP/service
  layer is deferred (see above).
- **No logging**: deliberately left out, rather than pulling in a logging framework for a
  library this small. If added later, JDK's built-in `System.Logger` (no new dependency) would
  be enough. It'd be worth logging when the small-backward-drift self-heal path fires (today it succeeds silently)
  and a note when the sequence counter overflows and the generator has to wait for
  the next millisecond (though this could fire often, so it may be better as counter/metric rather than a log line every time).
- **No metrics**: not built, same reasoning as logging above: no observability framework added
  for a library this size. Per-branch counters inside `nextId()`
  (IDs generated, self-heals, overflow waits, hard clock-rollback failures) would be a possible addition.
- **No defensive design (timeouts, connection pools, circuit breakers)**: not applicable here. This generator has no
  external I/O at all (no network calls, no database, no downstream service to time out on or circuit-break against). 
  Those concerns would only become relevant if a future `WorkerIdAssigner` implementation talked to something like
  ZooKeeper or a DB (§5).
