/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.sql.JDBCType;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractThirdPartyScansSqlDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.thirdpartyscans.ApiSbomApplicationsHistoryMetricDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomPolicyViolationSummaryDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.persistence.LockModeType;

import static com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO.createPaginationNativeQuery;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.utils.CvssV3Severity.CRITICAL;
import static com.sonatype.insight.brain.utils.CvssV3Severity.HIGH;
import static com.sonatype.insight.brain.utils.CvssV3Severity.LOW;
import static com.sonatype.insight.brain.utils.CvssV3Severity.MEDIUM;
import static com.sonatype.insight.brain.utils.CvssV3Severity.NONE;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Named
@Singleton
public class ThirdPartySbomMetadataDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartySbomMetadata>
{
  private final OperationalDataStore operationalDataStore;

  private final PolicyViolationDAO policyViolationDAO;

  @Inject
  public ThirdPartySbomMetadataDAO(
      ThirdPartyScansDataStore thirdPartyScansDataStore,
      OperationalDataStore operationalDataStore,
      PolicyViolationDAO policyViolationDAO,
      SearchIndexManager searchIndexManager)
  {
    super(thirdPartyScansDataStore, searchIndexManager);
    this.policyViolationDAO = policyViolationDAO;
    this.operationalDataStore = operationalDataStore;
  }

  @Override
  public ThirdPartySbomMetadata getById(String id) {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.id=?1";
    return get(sQuery, id);
  }

  public ThirdPartySbomMetadata getByIdForUpdate(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity WHERE entity.id=?1";

    var sbomMetadata = get(tx, sQuery, LockModeType.PESSIMISTIC_WRITE, id);
    if (sbomMetadata == null) {
      throw new NotFoundException(getEntityName() + " with ID " + id + " does not exist.");
    }
    return sbomMetadata;
  }

  public List<ThirdPartySbomMetadata> getAll() {
    return getList("SELECT entity FROM ThirdPartySbomMetadata entity");
  }

  public List<ThirdPartySbomMetadata> getByThirdPartyFileIds(List<String> thirdPartyFileIds) {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.thirdPartyFileId IN ?1";
    return getList(sQuery, thirdPartyFileIds);
  }

  public ThirdPartySbomMetadata getByThirdPartyFileId(String thirdPartyFileId) {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.thirdPartyFileId=?1";
    return get(sQuery, thirdPartyFileId);
  }

  public void deleteByThirdPartyFileId(TransactionContext tx, String thirdPartyFileId) {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.thirdPartyFileId=?1";
    getList(tx, sQuery, thirdPartyFileId).forEach(sbomMetadata -> delete(tx, sbomMetadata));
  }

  public List<ThirdPartySbomMetadata> getByApplicationId(String applicationId) {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.applicationId=?1";
    return getList(sQuery, applicationId);
  }

  public List<ThirdPartySbomMetadata> getByApplicationId(TransactionContext tx, String applicationId) {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.applicationId=?1";
    return getList(tx, sQuery, applicationId);
  }

  public List<ThirdPartySbomMetadata> getActiveByApplicationId(String applicationId) {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.applicationId=?1" + //
        " AND entity.status=?2";
    return getList(sQuery, applicationId, ACTIVE);
  }

  public List<ThirdPartySbomMetadata> getActiveByApplicationId(
      final String applicationId,
      final int page,
      final int pageSize)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getActiveByApplicationId(tx, applicationId, page, pageSize);
    }
  }

  public List<ThirdPartySbomMetadata> getActiveByApplicationId(
      final TransactionContext tx,
      final String applicationId,
      final int page,
      final int pageSize)
  {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " +
        " WHERE entity.applicationId=?1" +
        " AND entity.status=?2" +
        " ORDER BY entity.createdAt, entity.id";
    int offset = (page - 1) * pageSize;
    jakarta.persistence.Query paginationQuery = createPaginationQuery(tx, sQuery, offset, pageSize);
    paginationQuery.setParameter(1, applicationId);
    paginationQuery.setParameter(2, ACTIVE);
    return paginationQuery.getResultList();
  }

  public ThirdPartySbomMetadata getLatestActiveByApplicationId(String applicationId) {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.applicationId=?1" +
        " AND entity.status=?2" +
        " ORDER BY entity.createdAt DESC";
    return createQuery(sQuery, applicationId, ACTIVE).forceSingleResult().get();
  }

  public ThirdPartySbomMetadata getByApplicationIdAndSbomVersion(String applicationId, String sbomVersion) {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.applicationId = ?1 AND entity.sbomVersion=?2";
    return get(sQuery, applicationId, sbomVersion);
  }

  public ThirdPartySbomMetadata getByScanId(String scanId) {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity, ThirdPartyScan scan" +
        " WHERE entity.thirdPartyFileId = scan.thirdPartyFileId AND scan.scanId = ?1";
    return get(sQuery, scanId);
  }

  public ThirdPartySbomMetadata getByApplicationIdAndSbomVersionAndStatus(
      String applicationId,
      String sbomVersion,
      ThirdPartySbomMetadataStatus status)
  {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.applicationId = ?1 AND entity.sbomVersion=?2 AND entity.status=?3";
    return get(sQuery, applicationId, sbomVersion, status);
  }

  public long getActiveSbomCount(String applicationId) {
    String sQuery = "SELECT COUNT(entity) FROM ThirdPartySbomMetadata entity"
        + " WHERE entity.applicationId=?1 AND entity.status=?2";
    return getSingle(Long.class, sQuery, applicationId, ACTIVE);
  }

  public long getSbomCount() {
    String sQuery = "SELECT COUNT(entity) FROM ThirdPartySbomMetadata entity";
    return getSingle(Long.class, sQuery);
  }

  public long getActiveSbomCount() {
    String sQuery = "SELECT COUNT(entity) FROM ThirdPartySbomMetadata entity " //
        + "WHERE entity.status=?1";
    return getSingle(Long.class, sQuery, ACTIVE);
  }

  public List<ThirdPartySbomMetadata> getInactiveSbomsBeforeOrAt(Date date) {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity"
        + " WHERE entity.status <> com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE"
        + " AND entity.createdAt <= ?1";
    return getList(sQuery, date);
  }

  public boolean hasSbomMetadata(String scanId) {
    String sQuery = "SELECT count(entity) FROM ThirdPartySbomMetadata entity, ThirdPartyScan scan" +
        " WHERE entity.thirdPartyFileId = scan.thirdPartyFileId AND scan.scanId = ?1";
    return getSingle(Long.class, sQuery, scanId) != 0;
  }

  /**
   * This allows service-layer code to create a SearchIndexChanges for insert or update at the appropriate times. It
   * also implements the search index change for deletions.
   */
  @Override
  public SearchIndexChange newSearchIndexChange(ThirdPartySbomMetadata sbomMetadata) {
    return new SearchIndexChange(ChangeType.SBOM,
        String.format("%s:%s", sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion()));
  }

  @Override
  protected SearchIndexChange newSearchIndexChangeForUpdate(ThirdPartySbomMetadata sbomMetadata) {
    return null;
  }

  /**
   * Search indexing for these records should not occur automatically, as child records need to be in place before the
   * indexing is done, and those records are outside the scope of this DAO
   */
  @Override
  protected SearchIndexChange newSearchIndexChangeForInsert(ThirdPartySbomMetadata sbomMetadata) {
    return null;
  }

  public ApiSbomApplicationsHistoryMetricDTO getSbomsHistoryMetrics() {
    String databaseSchema = getDatabaseSchema();
    String sQuery = "" + //
        "SELECT COUNT(DISTINCT sm.application_id) AS total," + //
        "       COUNT(DISTINCT CASE WHEN (sm.created_at >= ?1 OR ve.created_at >= ?1 OR ve.updated_at >= ?1)" +
        " THEN sm.application_id END) AS last_year," + //
        "       COUNT(DISTINCT CASE WHEN (sm.created_at >= ?2 OR ve.created_at >= ?2 OR ve.updated_at >= ?2)" +
        " THEN sm.application_id END) AS last_month," + //
        "       COUNT(DISTINCT CASE WHEN (sm.created_at >= ?3 OR ve.created_at >= ?3 OR ve.updated_at >= ?3)" +
        " THEN sm.application_id END) AS last_week " + //
        "FROM " +
        "  " + databaseSchema + ".vulnerability_exploitability ve " +
        "  INNER JOIN " + databaseSchema + ".coordinate_security cs " +
        "    ON cs.coordinate_security_id = ve.coordinate_security_id " +
        "  INNER JOIN " + databaseSchema + ".file_coordinate fc ON cs.file_coordinate_id = fc.file_coordinate_id " +
        "  RIGHT JOIN " + databaseSchema + ".sbom_metadata sm ON sm.third_party_file_id = fc.third_party_file_id " +
        "WHERE sm.status = ?4";

    LocalDate now = LocalDate.now();
    LocalDate lastWeek = now.minusWeeks(1);
    LocalDate lastMonth = now.minusMonths(1);
    LocalDate lastYear = now.minusYears(1);

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = createNativeQuery(tx, sQuery, lastYear, lastMonth, lastWeek, ACTIVE.name());
      Object[] result = (Object[]) query.getSingleResult();
      return new ApiSbomApplicationsHistoryMetricDTO(result);
    }
  }

  public List<ThirdPartySbomMetadata> getByApplicationIdAndStatus(
      final String applicationId,
      final ThirdPartySbomMetadataStatus status,
      final int page,
      final int pageSize)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationIdAndStatus(tx, applicationId, status, page, pageSize);
    }
  }

  @SuppressWarnings("unchecked")
  public List<ThirdPartySbomMetadata> getByApplicationIdAndStatus(
      final TransactionContext tx,
      final String applicationId,
      final ThirdPartySbomMetadataStatus status,
      final int page,
      final int pageSize)
  {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " +
        "WHERE entity.applicationId = ?1 AND entity.status=?2 " +
        "ORDER BY entity.createdAt DESC";

    int offset = (page - 1) * pageSize;
    jakarta.persistence.Query paginationQuery = createPaginationQuery(tx, sQuery, offset, pageSize);
    paginationQuery.setParameter(1, applicationId);
    paginationQuery.setParameter(2, status);
    return paginationQuery.getResultList();
  }

  public SbomApplicationListSummaryDTO getSbomApplicationsWithRecentlyImportedSbomVersion(
      Set<String> applicationIds,
      SbomApplicationsSortableField sortBy,
      boolean asc,
      int page,
      int pageSize)
  {
    String databaseSchema = getDatabaseSchema();

    String sQuery = "" + //
        "SELECT sm.application_id as applicationInternalId," + //
        "  sm.sbom_version as sbomVersion, " + //
        "  sm.created_at as importDate, " + //
        "  app.public_id as applicationPublicId, " + //
        "  app.name as applicationName, " + //
        "  COUNT(CASE WHEN (cs.severity = ?1) THEN 1 END) AS vulnerabilityNone, " + //
        "  COUNT(CASE WHEN (cs.severity BETWEEN ?2 AND ?3) THEN 1 END) AS vulnerabilityLow, " + //
        "  COUNT(CASE WHEN (cs.severity BETWEEN ?4 AND ?5) THEN 1 END) AS vulnerabilityMedium, " + //
        "  COUNT(CASE WHEN (cs.severity BETWEEN ?6 AND ?7) THEN 1 END) AS vulnerabilityHigh, " + //
        "  COUNT(CASE WHEN (cs.severity BETWEEN ?8 AND ?9) THEN 1 END) AS vulnerabilityCritical, " + //
        " COUNT(*) OVER() AS full_count," + //
        " COALESCE(ROUND((COUNT(CASE WHEN (vex.coordinate_security_id IS NOT NULL AND cs.severity >= ?6) THEN 1 END))" +
        "* 100 / NULLIF(COUNT(CASE WHEN (cs.coordinate_security_id IS NOT NULL AND cs.severity >= ?6) THEN 1 END)" +
        "::decimal, 0), 1), 100) as releaseStatusPercentage" + //
        " FROM " +  databaseSchema + ".sbom_metadata sm " + //
        " JOIN " + //
        "  (SELECT  application_id, max(created_at) as created_at " + //
        "   FROM " +  databaseSchema + ".sbom_metadata " + //
        "   WHERE status = ?10 " + //
        "   GROUP BY application_id) sm2 " + //
        " ON sm.application_id = sm2.application_id " + //
        " AND sm.created_at = sm2.created_at " + //
        " JOIN " + operationalDataStore.getDatabaseSchema() + ".application app" + //
        " ON app.application_id = sm.application_id" + //
        " LEFT JOIN " +  databaseSchema + ".coordinate_security cs" + //
        " ON cs.sbom_metadata_id = sm.sbom_metadata_id" + //
        " LEFT JOIN " +  databaseSchema + ".vulnerability_exploitability vex" + //
        " ON cs.coordinate_security_id = vex.coordinate_security_id" + //
        " WHERE sm.status = ?10";

    if (isNotEmpty(applicationIds)) {
      sQuery += " AND sm.application_id = ANY(array[?11])";
    }
    sQuery += " GROUP BY sm.application_id, sm.sbom_version, sm.created_at, app.public_id, app.name" +
    generateOrderBySortFieldSelected(sortBy, asc);

    int offset = (page - 1) * pageSize;

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query paginationQuery =
          createPaginationQueryWithScoreRangeParams(pageSize, sQuery, offset, tx);
      if (isNotEmpty(applicationIds)) {
        paginationQuery.setParameter(11, createArrayOf(JDBCType.VARCHAR, applicationIds.toArray()));
      }
      SbomApplicationListSummaryDTO result = new SbomApplicationListSummaryDTO();

      List<SbomApplicationSummaryDTO> applicationPageApplicationSummaryDTOList =
          ((Stream<Object[]>) paginationQuery.getResultStream())
              .peek(array -> {
                if (result.getTotalCount() == 0) {
                  result.setTotalCount(( (Long) array[10]).intValue());
                }
              })
              .map(SbomApplicationSummaryDTO::new)
          .collect(Collectors.toList());
      List<String> applicationIdsForPolicyViolation = applicationPageApplicationSummaryDTOList.stream()
          .map(SbomApplicationSummaryDTO::getApplicationInternalId)
          .collect(Collectors.toList());

      //policy violation query results
      Map<String, SbomPolicyViolationSummaryDTO> policyViolationSummaryMap;
      policyViolationSummaryMap = policyViolationDAO
          .getSbomPolicyViolationSummaryForAnApplication(applicationIdsForPolicyViolation);

      //combine results
      for (SbomApplicationSummaryDTO applicationSummary: applicationPageApplicationSummaryDTOList) {
        applicationSummary.setPolicyViolationSummary(
            policyViolationSummaryMap.get(applicationSummary.getApplicationInternalId()));
      }

      result.setApplications(applicationPageApplicationSummaryDTOList);
      return result;
    }
    catch (SQLException e) {
      throw new InternalServerException(e);
    }
  }

  private jakarta.persistence.Query createPaginationQueryWithScoreRangeParams(
      final int pageSize,
      final String sQuery,
      final int offset,
      final TransactionContext tx)
  {
    jakarta.persistence.Query paginationQuery = createPaginationNativeQuery(tx, sQuery, offset, pageSize);
    paginationQuery.setParameter(1, NONE.getStartScoreRange());
    paginationQuery.setParameter(2, LOW.getStartScoreRange());
    paginationQuery.setParameter(3, LOW.getEndScoreRange());
    paginationQuery.setParameter(4, MEDIUM.getStartScoreRange());
    paginationQuery.setParameter(5, MEDIUM.getEndScoreRange());
    paginationQuery.setParameter(6, HIGH.getStartScoreRange());
    paginationQuery.setParameter(7, HIGH.getEndScoreRange());
    paginationQuery.setParameter(8, CRITICAL.getStartScoreRange());
    paginationQuery.setParameter(9, CRITICAL.getEndScoreRange());
    paginationQuery.setParameter(10, "ACTIVE");
    return paginationQuery;
  }

  private String generateOrderBySortFieldSelected(SbomApplicationsSortableField sortBy, boolean asc) {
    if (sortBy == null) {
      return "";
    }
    StringBuilder query = new StringBuilder();
    String order = asc ? "ASC" : "DESC";
    switch (sortBy) {
      case IMPORT_DATE:
        query.append(" ORDER BY importDate " + order);
        break;
      case PERCENTAGE_ANNOTATED:
        query.append(" ORDER BY annotatedPercentage " + order);
        break;
      case RELEASE_STATUS_PERCENTAGE:
        query.append(" ORDER BY releaseStatusPercentage " + order);
        break;
      case LATEST_SBOM_VERSION:
        query.append(" ORDER BY sbomVersion " + order);
        break;
      case APPLICATION_NAME:
        query.append(" ORDER BY applicationName " + order);
        break;
      case VULNERABILITY:
        query.append(" ORDER BY vulnerabilityCritical ").append(order)
            .append(" , vulnerabilityHigh ").append(order)
            .append(" , vulnerabilityMedium ").append(order)
            .append(" , vulnerabilityLow ").append(order);
        break;
      default:
        break;
    }
    return query.toString();
  }

  public ThirdPartySbomMetadataSummaryListDTO getSbomApplicationVulnerabilities(
      String applicationId,
      int pageSize,
      int page,
      SbomVersionsApplicationSortableField sortBy,
      boolean asc)
  {
    String sQuery = "" + //
        "SELECT sm.sbom_version," + //
        "       sm.spec," + //
        "       sm.spec_version," + //
        "       sm.created_at," + //
        "       sm.is_valid," + //
        "       COUNT(CASE WHEN (cs.severity = ?1) THEN 1 END) AS vulnerabilityNone," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?2 AND ?3) THEN 1 END) AS vulnerabilityLow," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?4 AND ?5) THEN 1 END) AS vulnerabilityMedium," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?6 AND ?7) THEN 1 END) AS vulnerabilityHigh," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?8 AND ?9) THEN 1 END) AS vulnerabilityCritical," + //
        "       COUNT(*) OVER() AS full_count," + //
        "       COALESCE(ROUND((COUNT(CASE WHEN (vex.coordinate_security_id IS NOT NULL" + //
        "           AND cs.severity >= ?6) THEN 1 END)) * 100 / NULLIF(COUNT(CASE WHEN " + //
        "           (cs.coordinate_security_id IS NOT NULL AND cs.severity >= ?6) THEN 1 END)"  + //
        "           ::decimal, 0), 1), 100) as releaseStatusPercentage" + //
        " FROM " + getDatabaseSchema() + ".sbom_metadata sm" + //
        "  LEFT JOIN " + getDatabaseSchema() + ".coordinate_security cs" + //
        "    ON cs.sbom_metadata_id = sm.sbom_metadata_id" + //
        "  LEFT JOIN " +  getDatabaseSchema() + ".vulnerability_exploitability vex" + //
        "    ON cs.coordinate_security_id = vex.coordinate_security_id" + //
        " WHERE sm.application_id = ?10" + //
        "   AND sm.status = ?11" + //
        " GROUP BY sm.sbom_version, sm.spec, sm.spec_version, sm.created_at, sm.is_valid";
    sQuery += generateOrderBySortFieldSelectedSbomsByApplication(sortBy , asc);

    int offset = (page - 1) * pageSize;
    ThirdPartySbomMetadataSummaryListDTO result = new ThirdPartySbomMetadataSummaryListDTO();

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query paginationQuery = createPaginationQueryWithScoreRangeParams(
          applicationId, pageSize, sQuery, offset, tx);
      paginationQuery.setParameter(11, ThirdPartySbomMetadataStatus.ACTIVE.name());

      try (Stream<Object[]> resultsStream = paginationQuery.getResultStream()) {
        result.setResults(resultsStream.peek(array -> {
          if (result.getTotalResultsCount() == 0) {
            result.setTotalResultsCount(((Long) array[10]).intValue());
          }
        }).map(ThirdPartySbomMetadataSummaryDTO::new).collect(Collectors.toList()));
      }
      return result;
    }
  }

  private jakarta.persistence.Query createPaginationQueryWithScoreRangeParams(
      final String searchParam,
      final int pageSize,
      final String sQuery,
      final int offset,
      final TransactionContext tx)
  {
    jakarta.persistence.Query paginationQuery = createPaginationNativeQuery(tx, sQuery, offset, pageSize);
    paginationQuery.setParameter(1, NONE.getStartScoreRange());
    paginationQuery.setParameter(2, LOW.getStartScoreRange());
    paginationQuery.setParameter(3, LOW.getEndScoreRange());
    paginationQuery.setParameter(4, MEDIUM.getStartScoreRange());
    paginationQuery.setParameter(5, MEDIUM.getEndScoreRange());
    paginationQuery.setParameter(6, HIGH.getStartScoreRange());
    paginationQuery.setParameter(7, HIGH.getEndScoreRange());
    paginationQuery.setParameter(8, CRITICAL.getStartScoreRange());
    paginationQuery.setParameter(9, CRITICAL.getEndScoreRange());
    paginationQuery.setParameter(10, searchParam);
    return paginationQuery;
  }

  private String generateOrderBySortFieldSelectedSbomsByApplication(
      SbomVersionsApplicationSortableField sortBy,
      boolean asc)
  {
    StringBuilder query = new StringBuilder();
    String order = asc ? "ASC" : "DESC";
    switch (sortBy) {
      case IMPORT_DATE:
        query.append(" ORDER BY sm.created_at " + order);
        break;
      case RELEASE_STATUS:
        query.append(" ORDER BY releaseStatusPercentage " + order);
        break;
      case VULNERABILITY:
        query.append(" ORDER BY vulnerabilityCritical ").append(order)
            .append(" , vulnerabilityHigh ").append(order)
            .append(" , vulnerabilityMedium ").append(order)
            .append(" , vulnerabilityLow ").append(order);
        break;
      default:
        break;
    }
    return query.toString();
  }

  public void makeSbomActiveIfExist(String scanId) {
    ThirdPartySbomMetadata sbomMetadata = getByScanId(scanId);
    if (sbomMetadata != null) {
      sbomMetadata.setStatus(ACTIVE);
      update(sbomMetadata);
    }
  }
}
