# Development Best Practices

This document contains important development best practices and guidelines for the insight-brain codebase.

## jOOQ Batch Operations

### Overview
Unlike OpenJPA, jOOQ does **not** automatically batch consecutive insert/update calls. Each `insert()` or
`update()` call executes an individual SQL statement as a separate JDBC round trip. For high-volume operations,
you must explicitly use jOOQ's batch API to reduce database round trips.

### When to Use Batch API
Use `dsl.batch()` when inserting or updating many rows of the same type in a loop. For single-row operations
or low-volume code paths (e.g., configuration saves), individual `insert()`/`update()` calls are fine.

### Implementation Patterns
```java
// Individual inserts — each is a separate SQL round trip (fine for low volume)
public void insertComponents(TransactionContext tx, List<Component> components) {
    for (Component component : components) {
        componentDAO.insert(tx, component);
    }
}

// Batch inserts — sends multiple statements in a single JDBC batch (use for high volume)
public void batchInsertComponents(TransactionContext tx, List<Component> components) {
    var inserts = components.stream()
        .map(c -> tx.dsl().insertInto(COMPONENT)
            .set(COMPONENT.ID, c.getId())
            .set(COMPONENT.NAME, c.getName()))
        .collect(toList());
    tx.dsl().batch(inserts).execute();
}
```

### Performance Guidelines
- **N+1 queries**: Watch for loops that fetch related entities one at a time — use joins or batch fetches
- **Transaction scope**: Keep related operations within the same transaction to avoid unnecessary commit overhead
- **Batch size**: For very large datasets (10k+ rows), consider chunking batch operations to avoid excessive
  memory usage from building the full query list

### Reference Documentation
- jOOQ Batch Operations: https://www.jooq.org/doc/latest/manual/sql-execution/batch-execution/

### Testing Requirements
- Use `TemporaryEntity` rule for proper test cleanup
- Verify performance improvements with realistic data volumes when optimizing batch operations
