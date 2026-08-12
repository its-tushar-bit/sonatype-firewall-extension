/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.repository.ManagerType;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.db.jooq.DialectHelper.POSTGRES_UNIQUE_CONSTRAINT_VIOLATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerAncestor.OWNER_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Repository.REPOSITORY;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryManager.REPOSITORY_MANAGER;

@Named
@Singleton
public class RepositoryManagerDAO
    extends AbstractOperationalSqlDAO<RepositoryManager>
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryManagerDAO.class);

  // Unique-index names on repository_manager. Both constants are defined here so any rename of a
  // constraint is a single-file change that a reviewer can catch. extractConstraintName pairs each
  // name with the exception it should map to; the two names share the "repository_manager_" prefix
  // but neither is a substring of the other, so their check order is not load-bearing.
  static final String REPOSITORY_MANAGER_NAME_UK = "repository_manager_name_uk";

  static final String REPOSITORY_MANAGER_UK = "repository_manager_uk";

  private final Provider<OwnerDAO> ownerDAOProvider;

  @Inject
  public RepositoryManagerDAO(
      final OperationalDataStore operationalDataStore,
      final Provider<OwnerDAO> ownerDAOProvider)
  {
    super(operationalDataStore);
    this.ownerDAOProvider = ownerDAOProvider;
  }

  @Override
  public Table<?> getJooqTable() {
    return REPOSITORY_MANAGER;
  }

  public RepositoryManager getByInstanceId(String instanceId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByInstanceId(tx, instanceId);
    }
  }

  private RepositoryManager getByInstanceId(TransactionContext tx, String instanceId) {
    return toEntity(tx.dsl()
        .selectFrom(REPOSITORY_MANAGER)
        .where(REPOSITORY_MANAGER.INSTANCE_ID.eq(instanceId))
        .fetchOne());
  }

  /**
   * @since 1.161
   */
  public RepositoryManager getByInstanceIdNotNull(String instanceId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByInstanceIdNotNull(tx, instanceId);
    }
  }

  public RepositoryManager getByInstanceIdNotNull(TransactionContext tx, String instanceId) {
    RepositoryManager repositoryManager = getByInstanceId(tx, instanceId);
    if (repositoryManager == null) {
      throw new NotFoundException("Cannot find a repository manager with instance ID " + instanceId + ".");
    }
    return repositoryManager;
  }

  private void validateName(RepositoryManager repositoryManager) {
    // We default the name to the instance ID if the name was not set.
    // So we need to accept any instance ID as name, and only validate the name if it is set to something different from
    // the instance ID.
    if (!Objects.equals(repositoryManager.getInstanceId(), repositoryManager.getName())) {
      // The display name is a user-facing friendly label — only enforce non-blank and length;
      // character-set restrictions (NameHelper.validate) are intentionally skipped here so that
      // names like "My NXRM (Production)" or "Dev & QA" are accepted.
      String name = repositoryManager.getName();
      if (name.trim().isEmpty()) {
        throw new InvalidNameException("Name is required.");
      }
      if (name.length() > NameHelper.MAX_NAME_LENGTH_APP_ORG) {
        throw new InvalidNameException("Name must be " + NameHelper.MAX_NAME_LENGTH_APP_ORG + " characters or less.");
      }
    }
  }

  /**
   * Validates that instanceId is not null or blank for non-virtual repository managers.
   * Virtual managers have their instanceId generated by the server, so null is valid initially.
   */
  private void validateInstanceId(RepositoryManager repositoryManager) {
    Objects.requireNonNull(repositoryManager, "repositoryManager");
    // Virtual managers have instanceId generated server-side, so skip validation
    if (repositoryManager.getManagerType() == ManagerType.VIRTUAL) {
      return;
    }
    if (StringUtils.isBlank(repositoryManager.getInstanceId())) {
      throw new InvalidRepositoryManagerException("The repository manager instance ID cannot be null or empty.");
    }
  }

  public RepositoryManager getByRelatedOrganizationId(String organizationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRelatedOrganizationId(tx, organizationId);
    }
  }

  private RepositoryManager getByRelatedOrganizationId(TransactionContext tx, String organizationId) {
    return toEntity(tx.dsl()
        .selectFrom(REPOSITORY_MANAGER)
        .where(REPOSITORY_MANAGER.RELATED_ORGANIZATION_ID.eq(organizationId))
        .fetchOne());
  }

  @Override
  public int insert(TransactionContext tx, RepositoryManager repositoryManager) {
    normalizeManagerType(repositoryManager);
    validateInstanceId(repositoryManager);
    validateName(repositoryManager);

    if (getByInstanceId(tx, repositoryManager.getInstanceId()) != null) {
      throw new InvalidRepositoryManagerException("There is already a repository manager with instance ID "
          + repositoryManager.getInstanceId() + ".");
    }

    if (repositoryManager.getName() != null
        && getByNameAndManagerType(tx, repositoryManager.getName(), repositoryManager.getManagerType()) != null)
    {
      throw new InvalidNameException(repositoryManager.getName() + " is already used as a name.");
    }

    try {
      return super.insert(tx, repositoryManager);
    }
    catch (DataAccessException e) {
      if (isUniqueConstraintViolation(e)) {
        throw translateConstraintViolation(repositoryManager, e);
      }
      throw e;
    }
  }

  @Override
  public int update(TransactionContext tx, RepositoryManager repositoryManager) {
    normalizeManagerType(repositoryManager);
    validateInstanceId(repositoryManager);
    validateName(repositoryManager);

    RepositoryManager existingRepositoryManager = getByInstanceId(tx, repositoryManager.getInstanceId());
    if (existingRepositoryManager != null && !existingRepositoryManager.getId().equals(repositoryManager.getId())) {
      throw new InvalidRepositoryManagerException("There is already a repository manager with instance ID "
          + repositoryManager.getInstanceId() + ".");
    }

    if (repositoryManager.getRawName() != null) {
      RepositoryManager foundByNameRepositoryManager = getByNameAndManagerType(tx,
          repositoryManager.getRawName(), repositoryManager.getManagerType());
      if (foundByNameRepositoryManager != null && !repositoryManager.getId()
          .equals(foundByNameRepositoryManager.getId()))
      {
        throw new InvalidNameException(
            repositoryManager.getName() + " is already used as a name.");
      }
    }

    try {
      return super.update(tx, repositoryManager);
    }
    catch (DataAccessException e) {
      if (isUniqueConstraintViolation(e)) {
        throw translateConstraintViolation(repositoryManager, e);
      }
      throw e;
    }
  }

  /**
   * Mirrors the {@code manager_type} column default in memory so the entity handed back to
   * callers (and any subsequent read-back or serialization within the same request) reflects
   * what was persisted. The DB column is {@code NOT NULL DEFAULT 'TRADITIONAL'}; applying the
   * same default on the write paths keeps the in-memory entity and the stored row in sync.
   *
   * <p>
   * {@code null} is not a preservable value on this DAO: a caller that passes a {@code null}
   * {@code managerType} is coerced to {@link ManagerType#TRADITIONAL}. Rows loaded from the
   * database are never null (the column is {@code NOT NULL} after migration 0481), so this
   * normalization only fires when a caller supplies a null value — in practice, when an
   * older client POSTs a repository-manager registration whose body omits {@code managerType}.
   * Logged at {@code DEBUG}: the trigger is caller-driven and can be per-request on the
   * pre-flag-migration REST path, so an {@code INFO} entry would be chatty. {@code instanceId}
   * is deliberately not in the log message — it is unvalidated client input and the log
   * pattern does not escape newlines (CWE-117 CRLF).
   */
  private static void normalizeManagerType(final RepositoryManager repositoryManager) {
    if (repositoryManager.getManagerType() == null) {
      log.debug("Normalizing null managerType to TRADITIONAL on repository_manager id={}",
          repositoryManager.getId());
      repositoryManager.setManagerType(ManagerType.TRADITIONAL);
    }
  }

  /**
   * Translates a DB-level unique-constraint violation into the corresponding app-level
   * exception, matching what the pre-check would have thrown. This closes the check-then-act
   * race where two concurrent writers both pass the pre-check before either commits — the
   * loser hits the unique index at commit time and must produce a clean caller-facing error
   * rather than a raw jOOQ exception.
   *
   * <p>
   * Inspects the exception's message chain to identify which constraint fired so that the
   * translation is correct: an {@code instance_id} race maps to
   * {@link InvalidRepositoryManagerException}, a name-uniqueness race maps to
   * {@link InvalidNameException}. Unknown constraint violations propagate as-is.
   */
  // package-private for direct unit testing of the exception-translation branching — the
  // check-then-act race that reaches this method requires two concurrent writers and is not
  // reproducible in a single-threaded integration test.
  RuntimeException translateConstraintViolation(
      final RepositoryManager repositoryManager,
      final DataAccessException cause)
  {
    final String constraint = extractConstraintName(cause);
    if (REPOSITORY_MANAGER_NAME_UK.equals(constraint)) {
      return new InvalidNameException(repositoryManager.getName() + " is already used as a name.", cause);
    }
    if (REPOSITORY_MANAGER_UK.equals(constraint)) {
      return new InvalidRepositoryManagerException("There is already a repository manager with instance ID "
          + repositoryManager.getInstanceId() + ".", cause);
    }
    // Unknown constraint or unrecognized message shape: surface with enough context to diagnose
    // (driver upgrade, new constraint on the table, or lost server-error metadata) rather than
    // silently propagating a raw jOOQ exception that reaches the caller as a 500.
    log.warn("Unique-constraint violation on repository_manager could not be mapped to a caller-facing exception "
        + "(constraint={}); rethrowing raw jOOQ exception.", constraint, cause);
    return cause;
  }

  /**
   * jOOQ does not reliably surface {@link IntegrityConstraintViolationException} at the top
   * level — some violations only appear at commit or batch flush and reach the caller as a
   * plain {@link DataAccessException} wrapping a driver exception. Walk the entire cause chain
   * so a violation nested more than one level down (batch flush, pooling wrapper, driver
   * upgrade adding a layer) still routes through {@code translateConstraintViolation} instead
   * of surfacing as a raw 500. Kept symmetric with {@link #extractConstraintName}, which walks
   * the same chain for the same reason.
   */
  // package-private for direct unit testing of the SQLState / wrapper / nested-cause check.
  static boolean isUniqueConstraintViolation(final DataAccessException e) {
    for (Throwable current = e; current != null; current = current.getCause()) {
      if (current instanceof IntegrityConstraintViolationException) {
        return true;
      }
      if (current instanceof PSQLException psqlEx
          && POSTGRES_UNIQUE_CONSTRAINT_VIOLATION.equals(psqlEx.getSQLState()))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Extracts the violated constraint name from a jOOQ-wrapped DB exception.
   *
   * <p>
   * Two passes over the cause chain rather than one interleaved loop: jOOQ's
   * {@link IntegrityConstraintViolationException} wrapper copies the driver's message into its
   * own {@code getMessage()} (recipe {@code "SQL [" + sql + "]; " + driverException.getMessage()}),
   * so a single interleaved loop matches the substring on the wrapper before ever reaching the
   * underlying {@link PSQLException} — the structured accessor would never run in production.
   *
   * <p>
   * Pass 1 walks the chain looking for a {@link PSQLException} with a structured constraint name
   * via {@link org.postgresql.util.ServerErrorMessage#getConstraint()} — preferred where available
   * since it doesn't depend on driver message wording.
   *
   * <p>
   * Pass 2 falls back to substring-matching the message. H2 embeds the conflicting row's column
   * values inside the message after a {@code " VALUES "} token, and PostgreSQL's {@code Detail:}
   * line carries the same shape — so the search is bounded to the segment before that token to
   * prevent user-controlled data (e.g. an {@code instance_id} containing the literal
   * {@code repository_manager_name_uk}) from misrouting a constraint match. The message is
   * lowercased first so the fallback stays correct even if a future H2 config drops
   * {@code DATABASE_TO_UPPER=FALSE}. Neither constraint name is a substring of the other, so the
   * order in which they are checked does not affect correctness.
   *
   * @return the constraint name, or {@code null} if it could not be determined
   */
  // package-private for direct unit testing of the exception-introspection logic — the
  // try/catch that invokes this only fires on a pre-check/commit race that is not
  // reproducible in a single-threaded integration test.
  static String extractConstraintName(final Throwable t) {
    for (Throwable current = t; current != null; current = current.getCause()) {
      if (current instanceof PSQLException psqlEx && psqlEx.getServerErrorMessage() != null) {
        String constraint = psqlEx.getServerErrorMessage().getConstraint();
        if (constraint != null) {
          return constraint;
        }
      }
    }
    for (Throwable current = t; current != null; current = current.getCause()) {
      String message = current.getMessage();
      if (message == null) {
        continue;
      }
      // Locale.ENGLISH matches NameHelper.normalize in this module — on a Turkish-locale JVM the
      // default toLowerCase() maps 'I' to dotless 'ı' (U+0131), which turns "REPOSITORY" into
      // "reposıtory" and prevents the constraint-name substring match below from ever firing.
      String lower = message.toLowerCase(Locale.ENGLISH);
      // Both H2 and PostgreSQL embed the conflicting row's column values further into the
      // message (H2 after " values ", PostgreSQL after " detail: "). Bound the search to the
      // segment before whichever appears first so a user-supplied instance_id containing the
      // literal constraint name can't misroute the match.
      int values = lower.indexOf(" values ");
      int detail = lower.indexOf(" detail: ");
      int cutoff = -1;
      if (values >= 0 && detail >= 0) {
        cutoff = Math.min(values, detail);
      }
      else if (values >= 0) {
        cutoff = values;
      }
      else if (detail >= 0) {
        cutoff = detail;
      }
      String searchable = cutoff >= 0 ? lower.substring(0, cutoff) : lower;
      if (searchable.contains(REPOSITORY_MANAGER_NAME_UK)) {
        return REPOSITORY_MANAGER_NAME_UK;
      }
      if (searchable.contains(REPOSITORY_MANAGER_UK)) {
        return REPOSITORY_MANAGER_UK;
      }
    }
    return null;
  }

  /**
   * Looks up a repository manager by name within the given {@code managerType} bucket. Since
   * {@code repository_manager_name_uk} is unique <em>per bucket</em>, the same name can
   * legitimately exist in the TRADITIONAL and VIRTUAL buckets simultaneously — callers must
   * therefore state which bucket they mean. Both {@code name} and {@code managerType} are
   * required.
   */
  public RepositoryManager getByNameAndManagerType(
      final TransactionContext tx,
      final String name,
      final ManagerType managerType)
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(managerType, "managerType");
    final String normalized = NameHelper.normalize(name);
    return toEntity(tx.dsl()
        .selectFrom(REPOSITORY_MANAGER)
        .where(REPOSITORY_MANAGER.NAME_LOWERCASE_NO_WHITESPACE.eq(normalized))
        .and(REPOSITORY_MANAGER.MANAGER_TYPE.eq(managerType.name()))
        .fetchOne());
  }

  @Override
  public void delete(TransactionContext tx, RepositoryManager repositoryManager) {
    long start = System.currentTimeMillis();

    // Cascade to owned entities
    ownerDAOProvider.get().cascadeDelete(tx, repositoryManager);

    super.delete(tx, repositoryManager);

    long duration = System.currentTimeMillis() - start;
    if (duration > 500) {
      log.debug("Deleted repository manager with id {} in {} ms.", repositoryManager.getId(), duration);
    }
  }

  /**
   * @since 1.160
   */
  public List<RepositoryManager> getUnconfigured() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(REPOSITORY_MANAGER)
          .where(REPOSITORY_MANAGER.CONFIGURED.eq(false))
          .fetch(this::toEntity);
    }
  }

  /**
   * @see #getByNameAndManagerType(TransactionContext, String, ManagerType)
   */
  public RepositoryManager getByNameAndManagerType(final String name, final ManagerType managerType) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByNameAndManagerType(tx, name, managerType);
    }
  }

  /**
   * @return the RepositoryManager that either has the specified id or is the parent of a repository with that id
   */
  public RepositoryManager getByIdOrRepositoryId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdOrRepositoryId(tx, ownerId);
    }
  }

  public RepositoryManager getByIdOrRepositoryId(TransactionContext tx, String ownerId) {
    Record record = tx.dsl()
        .select(REPOSITORY_MANAGER.fields())
        .from(REPOSITORY_MANAGER)
        .join(OWNER_ANCESTOR)
        .on(REPOSITORY_MANAGER.REPOSITORY_MANAGER_ID.eq(OWNER_ANCESTOR.ANCESTOR_ID))
        .where(OWNER_ANCESTOR.OWNER_ID.eq(ownerId)
            .and(OWNER_ANCESTOR.OWNER_TYPE.in("REPOSITORY", "REPOSITORY_MANAGER")))
        .fetchOne();
    return record != null ? toEntity(record.into(REPOSITORY_MANAGER)) : null;
  }

  public List<RepositoryManager> getByRepositoryIds(Set<String> repositoryIds) {
    return getListWithSqlInClause(repositoryIds, inClauseValuesPartition -> {
      try (TransactionContext tx = createTransactionContext()) {
        return tx.dsl()
            .select(REPOSITORY_MANAGER.fields())
            .from(REPOSITORY_MANAGER)
            .join(REPOSITORY)
            .on(REPOSITORY_MANAGER.REPOSITORY_MANAGER_ID.eq(REPOSITORY.REPOSITORY_MANAGER_ID))
            .where(REPOSITORY.REPOSITORY_ID.in(inClauseValuesPartition))
            .fetch(r -> toEntity(r.into(REPOSITORY_MANAGER)));
      }
    });
  }

  public List<RepositoryManager> getByIds(Set<String> repositoryManagerIds) {
    return getListWithSqlInClause(repositoryManagerIds, inClauseValuesPartition -> {
      try (TransactionContext tx = createTransactionContext()) {
        return tx.dsl()
            .selectFrom(REPOSITORY_MANAGER)
            .where(REPOSITORY_MANAGER.REPOSITORY_MANAGER_ID.in(inClauseValuesPartition))
            .fetch(this::toEntity);
      }
    });
  }

  @Override
  public Class<RepositoryManager> getEntityClass() {
    return RepositoryManager.class;
  }
}
