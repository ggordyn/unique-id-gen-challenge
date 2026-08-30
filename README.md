# System Design Implementation Challenge

Solution to the system design implementation challenge, based on *System Design Interview –
An Insider's Guide* (Vol 1) by Alex Xu.

See [`unique-id-generator/`](unique-id-generator/) for the chosen problem (Chapter 7: Design a
Unique ID Generator in Distributed Systems) and its `DESIGN.md`.

## Build & run

Requires Java 21 and Maven.

```
cd unique-id-generator
mvn test                                                         # run all tests
mvn compile exec:java -Dexec.mainClass=com.challenge.idgen.Main  # run the demo
```
