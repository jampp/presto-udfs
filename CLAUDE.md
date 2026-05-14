# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Purpose

A Trino plugin (`trino-jampp-udfs`) packaging the `json_sum` aggregation function: combines JSON values in a column by summing shared numeric keys and concatenating the rest, recursing into nested objects. Companion code for the Jampp Geeks blog post on writing custom Presto functions (the post predates the Presto → Trino rename; the underlying SPI shape is the same idea).

Targets **Trino 470**. The Maven module still lives under `presto-udfs/` (directory name kept for muscle memory), but the artifact coordinates are now `com.jampp.trino:trino-jampp-udfs:470` and packaging is `trino-plugin`. The project version intentionally matches Trino's major (`470`, not `0.470`) so that `${project.version}` inherited from the Trino parent's `<dependencyManagement>` resolves consistently with transitive `io.trino:*` deps — using `0.470` triggers an upper-bound-deps enforcer failure.

## Build & Test

All Maven commands run from the `presto-udfs/` subdirectory (the Maven module root), not the repo root. **JDK 23 is required** — Trino 470's server runs on Java 23 and the plugin compiles against the same target.

```bash
cd presto-udfs
mvn clean package              # build the plugin (.zip in target/, exploded dir under target/trino-jampp-udfs-470/)
mvn test                       # run TestNG tests
mvn test -Dtest=TestJSONAggregationUtils#bothJSONsDeep   # single test method
mvn checkstyle:check           # checkstyle is also wired to the `validate` phase, so it runs on every build
```

Local smoke-test against a real Trino via Docker (from the repo root):

```bash
docker run --name trino -p 8080:8080 \
  -v $PWD/presto-udfs/target/trino-jampp-udfs-470/:/usr/lib/trino/plugin/udfs \
  trinodb/trino:470
```

Then connect with the Trino CLI and run `SHOW FUNCTIONS;` — `json_sum` should appear once per supported input type (VARCHAR, JSON).

## Architecture

The plugin follows Trino's standard SPI layout. Four pieces compose one aggregation function:

1. **`UdfPlugin`** (entry point) — implements `io.trino.spi.Plugin#getFunctions()`. Add new functions by registering their class here. Discovered by Trino via the `trino-plugin` packaging type (the `trino-maven-plugin` generates the `META-INF/services` descriptor at build time).

2. **`JSONAggregation`** — `@AggregationFunction("json_sum")` declares the SQL-visible function. It defines two `@InputFunction` overloads (VARCHAR and JSON), a `@CombineFunction` (merges partial states across workers), and an `@OutputFunction` that writes the final VARCHAR. Each overload becomes a separate function signature in Trino.

3. **`JSONAggregationState`** — the per-group accumulator. The `@AccumulatorStateMetadata` annotation wires it to its factory and serializer. The state is just a `Map<String, Object>`.

4. **`JSONAggregationStateFactory` / `JSONAggregationStateSerializer`** — Trino requires both a `Single` and `Grouped` state implementation: `SingleJSONAggregationState` (used for simple aggregations) and `GroupedJSONAggregationState` (used under `GROUP BY`, backed by an `ObjectBigArray` indexed by `groupId`). The serializer round-trips the map through a VARCHAR via Jackson so partial states can be shuffled between workers.

Two Trino-470 SPI specifics worth flagging for the next person to edit this:

- **`AccumulatorStateFactory` only declares `createSingleState()` and `createGroupedState()`** — the older PrestoSQL interface also required `getSingleStateClass()` / `getGroupedStateClass()`, but those were removed. Don't re-add them.
- **`GroupedAccumulatorState.setGroupId(int)` and `ensureCapacity(int)` take `int`, not `long`.** This silently differs from PrestoSQL 306. Keep `@Override` so the compiler catches mismatches if Trino ever changes it back.

The merge math lives in `JSONAggregationUtils.merge` — recursive on nested maps, promotes to `double` if either side is a `Double`, otherwise sums as `long`. `mapAsJSONString` is a hand-rolled JSON serializer that only handles numbers and nested maps (no strings, arrays, or nulls in values) — this is intentional given the function's purpose.

## Version Coupling

Several versions are tied together and must move in lockstep when upgrading Trino:

- `pom.xml` parent `io.trino:trino-root` version
- `<trino.version>` property
- `<version>` of this artifact (matches the Trino major exactly, e.g. `470`)
- The target directory name in the Docker `-v` mount in the README (e.g. `trino-jampp-udfs-470`)
- The `trinodb/trino:NNN` Docker tag must be compatible with the SPI version compiled against
- `<air.java.version>` and the modernizer `<javaVersion>` must match the Trino server's required JDK (Trino 470 → JDK 23)

`trino-spi`, `slice`, `jackson-annotations`, and `jol-core` are `<scope>provided</scope>` — they're part of Trino's SPI surface, supplied by the runtime, and **must not** be shaded into the plugin jar. `trino-array` is in `lib/`, *not* the SPI, so it must NOT be `provided` — `trino-maven-plugin`'s `check-spi-dependencies` mojo enforces this at build time. It's pulled in at compile scope and bundled inside the plugin zip; Trino's per-plugin classloader loads it at runtime. A future major Trino bump may require swapping it for an inlined equivalent.

## Style & Conventions

- Java 23 target (`air.java.version=23.0.0`, compiled with `--release 23`).
- Style enforcement (checkstyle, modernizer, sortpom, enforcer rules) is **inherited from the Trino parent POM** — we don't pin our own versions. The Trino parent's checkstyle ruleset replaces what used to live at `src/checkstyle/checks.xml`; that file and `src/modernizer/violations.xml` are dead but kept on disk in case a future revert is needed. The parent's rules include: no tabs, LF line endings, no trailing whitespace, sorted POM elements, dependency upper-bound enforcement, banned imports (e.g. `org.testng.Assert.*` is banned — use `org.assertj.core.api.Assertions` in tests).
- The modernizer plugin executions read project-local violations files at `.mvn/modernizer/violations.xml` and `.mvn/modernizer/violations-production-code-only.xml`. Both are empty placeholders — add entries only if a specific API needs banning.
- The enforcer plugin requires a Temurin or Oracle JDK. Local builds must use Temurin 23 (e.g. `~/Library/Java/JavaVirtualMachines/jdk-23.0.2+7/Contents/Home`). Homebrew's `openjdk@23` is actually JDK 25 and will fail both the `requireJavaVendor` rule and the Surefire fork at test time (JDK 25 rejects `-Djava.security.manager=allow` that the parent passes unconditionally).
- Apache 2.0 license header on every Java file.
