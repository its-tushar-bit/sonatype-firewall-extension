/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.ClusterLock;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.repository.FirewallFilterField.FirewallFilterableField;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

/**
 * @since 1.17
 */
public class RepositoryComponentDAO
    extends AbstractOperationalSqlDAO<RepositoryComponent>
{
  /*
    For queries on `quarantineTime` or `unquarantineTime`, if we query using `IS NOT NULL` the applicable indices
    are not used by H2. By changing this to `> {d EPOCH_START}` the queries return the same results but the applicable
    indices are also used.
  */
  private static final String EPOCH_START = new SimpleDateFormat("yyyy-MM-dd").format(Date.from(Instant.EPOCH));

  private final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO = new QuarantinedComponentAccessDAO();

  @Override
  public RepositoryComponent getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<RepositoryComponent> getByRepositoryId(String repositoryId) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1";
    return getList(sQuery, repositoryId);
  }

  public RepositoryComponent getByRepositoryIdAndPathname(String repositoryId, String pathname) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryIdAndPathname(tx, repositoryId, pathname);
    }
  }

  public RepositoryComponent getByRepositoryIdAndPathname(TransactionContext tx, String repositoryId, String pathname) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.pathname=?2";
    return get(tx, sQuery, repositoryId, pathname);
  }

  public List<RepositoryComponent> getByRepositoryIdAndHash(String repositoryId, String hash) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.hash=?2";
    return getList(sQuery, repositoryId, hash);
  }

  public int getComponentCountByRepositoryId(String repositoryId) {
    String sQuery = "SELECT COUNT(component.id) FROM RepositoryComponent component" + //
        " WHERE component.repositoryId=?1";

    return getSingle(Number.class, sQuery, repositoryId).intValue();
  }

  public int getKnownComponentCountByRepositoryId(String repositoryId) {
    String sQuery = "SELECT COUNT(component.id) FROM RepositoryComponent component" + //
        " WHERE component.repositoryId=?1 AND component.matchStateId <> ?2";

    return getSingle(Number.class, sQuery, repositoryId, MatchState.UNKNOWN.getId()).intValue();
  }

  public int getQuarantinedComponentCountByRepositoryId(String repositoryId) {
    String sQuery = "SELECT COUNT(component.id) FROM RepositoryComponent component" //
        + " WHERE component.repositoryId=?1"
        + " AND component.quarantineTime IS NOT NULL AND component.unquarantineTime IS NULL";

    return getSingle(Number.class, sQuery, repositoryId).intValue();
  }

  /**
   * @since 1.106
   */
  public long getQuarantinedComponentCount() {
    String sQuery = String.format("SELECT COUNT(component.id) FROM RepositoryComponent component" //
        + " WHERE component.quarantineTime > {d '%s'} AND component.unquarantineTime IS NULL", EPOCH_START);

    return getSingle(Long.class, sQuery);
  }

  public List<RepositoryComponent> getAllQuarantinedComponent() {
    return getList("SELECT qc FROM RepositoryComponent qc" +
        " WHERE qc.quarantineTime IS NOT NULL AND qc.unquarantineTime IS NULL");
  }

  public List<RepositoryComponent> getQuarantinedByRepositoryId(TransactionContext tx, String repositoryId) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1 AND entity.quarantineTime IS NOT NULL AND entity.unquarantineTime IS NULL";

    return getList(tx, sQuery, repositoryId);
  }

  public List<RepositoryComponent> getQuarantinedByRepositoryId(String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getQuarantinedByRepositoryId(tx, repositoryId);
    }
  }

  /**
   * @since 1.104
   */
  public List<RepositoryComponent> getQuarantinedByRepositoryIdAndDate(String repositoryId, Date date) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1 AND entity.quarantineTime>=?2 AND entity.unquarantineTime IS NULL";

    return getList(sQuery, repositoryId, date);
  }

  public Date getOldestComponentEvaluationTimeByRepositoryId(String repositoryId) {
    String sQuery = "SELECT MIN(entity.lastEvaluationTime) FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1";

    Date oldest = getSingle(Date.class, sQuery, repositoryId);

    // converting from a Timestamp to a Date object for happy comparisons
    return oldest != null ? new Date(oldest.getTime()) : null;
  }

  public List<RepositoryComponent> getUnquarantinedByRepositoryId(String repositoryId, Date sinceUtcTimestamp) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1 AND entity.unquarantineTime IS NOT NULL AND entity.unquarantineTime>=?2";
    return getList(sQuery, repositoryId, sinceUtcTimestamp);
  }

  public long getAutoReleaseQuarantinedCountByDate(Date date) {
    String sQuery = String.format("SELECT COUNT(component.id) FROM RepositoryComponent component" //
        + " WHERE component.quarantineTime > {d '%s'} AND component.unquarantineTime >=?1"
        + " AND component.autoUnquarantined = true", EPOCH_START);

    return getSingle(Number.class, sQuery, date).longValue();
  }

  public List<RepositoryComponent> getFirewallRepositoryComponents(FirewallRepositoryComponentFilter filter) {
    String baseQuery = getBaseFirewallComponentsQueryAndViolations(filter, "SELECT DISTINCT component");

    StringBuilder sQuery = new StringBuilder(baseQuery);

    // SORTING
    if (null != filter.sortableField) {
      sQuery.append(" ORDER BY component.").append(filter.sortableField.getColumn());
    }
    else {
      sQuery.append(" ORDER BY component.time");
    }

    if (filter.asc) {
      sQuery.append(" ASC");
    }
    else {
      sQuery.append(" DESC");
    }

    // PAGINATION
    int offset = (filter.page - 1) * filter.pageSize;
    int parameterIndex = 1;
    try (TransactionContext tx = createTransactionContext()) {
      final javax.persistence.Query paginationQuery =
          createPaginationQuery(tx, sQuery.toString(), offset, filter.pageSize);

      if (filter.getFilterFieldsMap().containsKey(FirewallFilterableField.POLICY_ID)) {
        paginationQuery.setParameter(parameterIndex++,
            filter.getFilterFieldsMap().get(FirewallFilterableField.POLICY_ID));
      }

      if (filter.getFilterFieldsMap().containsKey(FirewallFilterableField.COMPONENT_NAME)) {
        paginationQuery.setParameter(parameterIndex,
            "%" + StringUtils.lowerCase(filter.getFilterFieldsMap().get(FirewallFilterableField.COMPONENT_NAME)) + "%");
      }

      return paginationQuery.getResultList();
    }
  }

  public long getTotalFirewallRepositoryComponents(FirewallRepositoryComponentFilter filter) {
    String sQuery = getBaseFirewallComponentsQueryAndViolations(filter, "SELECT COUNT(DISTINCT component)");
    List<Object> parameters = new ArrayList<>();

    // FILTER
    if (filter.getFilterFieldsMap().containsKey(FirewallFilterableField.POLICY_ID)) {
      parameters.add(filter.getFilterFieldsMap().get(FirewallFilterableField.POLICY_ID));
    }

    if (filter.getFilterFieldsMap().containsKey(FirewallFilterableField.COMPONENT_NAME)) {
      parameters.add(
          "%" + StringUtils.lowerCase(filter.getFilterFieldsMap().get(FirewallFilterableField.COMPONENT_NAME)) + "%");
    }

    return getSingle(Long.class, sQuery, parameters.toArray());
  }

  public int getCountWithPolicyViolationInPolicyThreatLevelRange(
      String repositoryId,
      int minPolicyThreatLevel,
      int maxPolicyThreatLevel)
  {
    // Jan 19, 2023:
    // I tried this JPA query:
    // String sQuery = "SELECT COUNT(DISTINCT policyViolation.pathname)" + //
    // " FROM RepositoryPolicyViolation policyViolation" + //
    // " WHERE policyViolation.repositoryId=?1" + //
    // " AND policyViolation.active = true AND policyViolation.isWaived = false" + //
    // " AND policyViolation.threatLevel >= ?2 AND policyViolation.threatLevel <= ?3";
    // The native query below is about 2 times faster than the JPA query.
    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = "SELECT COUNT(*) AS component_count FROM " + //
          "(SELECT DISTINCT pathname" + //
          " FROM " + OperationalDataStoreProvider.getDatabaseSchema() + ".repository_policy_violation" + //
          " WHERE repository_id = ?1 AND active = true AND waived = false" + //
          " AND threat_level >= ?2 AND threat_level <= ?3) inner_select_alias";

      javax.persistence.Query query = tx.createNativeQuery(sQuery);
      query.setParameter(1, repositoryId);
      query.setParameter(2, minPolicyThreatLevel);
      query.setParameter(3, maxPolicyThreatLevel);

      return ((Long) query.getResultList().get(0)).intValue();
    }
  }

  private static String getBaseFirewallComponentsQueryAndViolations(
      FirewallRepositoryComponentFilter filter,
      String selectStatement)
  {
    validateFirewallRepositoryComponentFilter(filter);
    StringBuilder sQuery = new StringBuilder(selectStatement + " FROM RepositoryComponent component");
    MutableBoolean sQueryContainsWhereClause = new MutableBoolean();
    int parameterIndex = 1;

    if (queryRequiresPolicyViolations(filter)) {
      sQuery.append(" , RepositoryPolicyViolation policyViolation")
          .append(" WHERE component.repositoryId = policyViolation.repositoryId")
          .append(" AND component.pathname = policyViolation.pathname")
          .append(" AND policyViolation.actionTypeId = 'fail'")
          .append(" AND policyViolation.active = true")
          .append(" AND policyViolation.policyId=?")
          .append(parameterIndex++)
          .append(" AND policyViolation.isWaived = false");
      sQueryContainsWhereClause.setTrue();
    }

    sQuery.append(getFirewallComponentStateClause(sQueryContainsWhereClause, filter));

    if (queryRequiresComponentDisplayName(filter)) {
      if (sQueryContainsWhereClause.getValue()) {
        sQuery.append(" AND");
      }
      else {
        sQuery.append(" WHERE");
        sQueryContainsWhereClause.setTrue();
      }
      sQuery.append(" LOWER(component.displayName) LIKE ?").append(parameterIndex);
    }

    return sQuery.toString();
  }

  private static boolean queryRequiresPolicyViolations(FirewallRepositoryComponentFilter filter) {
    return filter.getFilterFieldsMap().containsKey(FirewallFilterableField.POLICY_ID);
  }

  private static boolean queryRequiresComponentDisplayName(FirewallRepositoryComponentFilter filter) {
    return filter.getFilterFieldsMap().containsKey(FirewallFilterableField.COMPONENT_NAME);
  }

  private static String getFirewallComponentStateClause(
      final MutableBoolean sQueryContainsWhereClause,
      final FirewallRepositoryComponentFilter filter)
  {
    String prefix = sQueryContainsWhereClause.getValue() ? "AND" : "WHERE";

    switch (filter.firewallComponentFilterState) {
      case AUDIT:
        sQueryContainsWhereClause.setTrue();
        return String.format(" %s (component.quarantineTime IS NULL)", prefix);
      case QUARANTINE:
        sQueryContainsWhereClause.setTrue();
        return String.format(" %s (component.quarantineTime > {d '%s'} AND component.unquarantineTime IS NULL)", prefix,
            EPOCH_START);
      case UNQUARANTINE_AUTO:
        sQueryContainsWhereClause.setTrue();
        return String.format(
            " %s (component.quarantineTime > {d '%s'} AND component.unquarantineTime > {d '%s'}" +
                " AND component.autoUnquarantined = true)", prefix, EPOCH_START, EPOCH_START);
      case UNQUARANTINE_MANUAL:
        sQueryContainsWhereClause.setTrue();
        return String.format(
            " %s (component.quarantineTime > {d '%s'} AND component.unquarantineTime > {d '%s'}" +
                " AND (component.autoUnquarantined = false OR component.autoUnquarantined IS NULL))",
            prefix, EPOCH_START, EPOCH_START);
      case UNQUARANTINE_ALL:
        sQueryContainsWhereClause.setTrue();
        return String
            .format(" %s (component.quarantineTime > {d '%s'} AND component.unquarantineTime > {d '%s'})", prefix,
                EPOCH_START, EPOCH_START);
      case ALL:
      default:
        return "";
    }
  }

  private static void validateFirewallRepositoryComponentFilter(final FirewallRepositoryComponentFilter filter) {
    if (filter.firewallComponentFilterState == null) {
      throw new BadRequestException("firewallComponentFilterState is required and cannot be null.");
    }

    if (filter.firewallComponentFilterState.equals(FirewallComponentFilterState.QUARANTINE) &&
        FirewallSortableField.RELEASE_QUARANTINE_TIME.equals(filter.sortableField)) {
      throw new BadRequestException(
          "Sortable field releaseQuarantineTime is not applicable to component state QUARANTINE");
    }

    if ((filter.firewallComponentFilterState.equals(FirewallComponentFilterState.AUDIT) ||
        filter.firewallComponentFilterState.equals(FirewallComponentFilterState.ALL)) &&
        filter.sortableField != null) {
      throw new BadRequestException(String
          .format("Sortable field cannot be specified for component state %s", filter.firewallComponentFilterState));
    }
  }

  public List<RepositoryComponent> getByRepositoryIdAndMatchStateId(String repositoryId, String matchStateId) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1 AND entity.matchStateId=?2";
    return getList(sQuery, repositoryId, matchStateId);
  }

  public long getCount() {
    String sQuery = "SELECT COUNT(entity) FROM RepositoryComponent entity";
    return getSingle(Long.class, sQuery);
  }

  @Override
  public final void delete(RepositoryComponent entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all components for a repository.
    // See https://issues.sonatype.org/browse/CLM-15648 for details
    super.delete(entity);
  }

  @Override
  public final void delete(TransactionContext tx, RepositoryComponent entity) {
    // WARNING: Be careful adding business logic to this method because, for performance reasons,
    // we bypass this method when deleting all components for a repository.
    // See https://issues.sonatype.org/browse/CLM-15648 for details
    ClusterLock.deleteForRepositoryComponent(tx, entity.getRepositoryId(), entity.getPathname());
    quarantinedComponentAccessDAO.deleteByRepositoryComponentId(tx, entity.getId());
    super.delete(tx, entity);
  }

  public void deleteByRepositoryId(TransactionContext tx, String repositoryId) {
    // For H2 locks would normally be deleted by calling delete > ClusterLock.deleteForRepositoryComponent
    // on each repository component, but there may be orphaned locks that were created without a corresponding
    // repository component, this will also delete those orphaned locks as well as the locks for postgres
    ClusterLock.deleteForRepository(tx, repositoryId);
    if (isDatabaseEmbedded()) {
      // We do not enroll the deletions in the transaction on purpose.
      // This improves performance and keeps db operations (including commits) reasonably short, which means other
      // concurrent db operations are blocked for shorter periods of time (H2 is single threaded).
      // See https://issues.sonatype.org/browse/CLM-15648 for details
      getByRepositoryId(repositoryId).forEach(this::delete);
    }
    else {
      // For performance reasons, we bypass the standard delete (per entity) method here.
      // We cannot do this for H2 until we upgrade to a multi-threaded H2 version.
      // See https://issues.sonatype.org/browse/CLM-15648 for details
      quarantinedComponentAccessDAO.deleteByRepositoryId(tx, repositoryId);
      String sQuery = "DELETE FROM RepositoryComponent entity WHERE entity.repositoryId=?1";
      createQuery(sQuery, repositoryId).executeUpdate(tx);
    }
  }

  public List<RepositoryComponent> getOtherVersionRepositoryComponentsByPathnameFilter(
      String repositoryId,
      String pathnamePrefix,
      String pathname)
  {
    String sQuery = "SELECT component FROM RepositoryComponent component" + //
        " WHERE component.repositoryId=?1" + //
        " AND component.pathname like ?2" + //
        " AND component.pathname <> ?3" + //
        " AND (component.quarantineTime IS NULL" + //
        " OR (component.quarantineTime IS NOT NULL AND component.unquarantineTime IS NOT NULL))";

    return getList(sQuery, repositoryId, pathnamePrefix + "%", pathname);
  }

  public RepositoryComponent getByRepositoryIdAndComponentIdentifier(
      String repositoryId,
      ComponentIdentifier componentIdentifier)
  {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1" + //
        " AND entity.componentIdFormat=?2 AND entity.componentIdCoordinatesJson=?3";
    return get(sQuery, repositoryId, componentIdentifier.getFormat(),
        ComponentIdentifierAdapter.toJson(componentIdentifier.getCoordinates()));
  }

  public List<RepositoryComponent> getByRepositoryIdAndDisplayName(String repositoryId, String displayName) {
    String sQuery = "SELECT entity FROM RepositoryComponent entity" + //
        " WHERE entity.repositoryId=?1 AND entity.displayName=?2";
    return getList(sQuery, repositoryId, displayName);
  }

  @Override
  public void insert(TransactionContext tx, RepositoryComponent entity) {
    fillDisplayName(entity);
    super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, RepositoryComponent entity) {
    fillDisplayName(entity);
    super.update(tx, entity);
  }

  private void fillDisplayName(RepositoryComponent entity) {
    if (entity.getComponentIdentifier() != null) {
      entity.setDisplayName(ComponentDisplayNameUtil.fromIdentifier(entity.getComponentIdentifier()).toString());
      return;
    }

    String pathname = entity.getPathname();
    if (pathname == null) {
      return;
    }

    entity.setDisplayName(pathname.substring(pathname.lastIndexOf('/') + 1) + " (" + pathname + ")");
  }
}
