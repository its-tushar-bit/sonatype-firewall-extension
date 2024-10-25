/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractThirdPartyScansSqlDAO;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.thirdpartyscans.ApiSbomApplicationsHistoryMetricDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class ThirdPartySbomMetadataDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartySbomMetadata>
{
  private static final String ACTIVE_STATUS = "ACTIVE";

  @Inject
  public ThirdPartySbomMetadataDAO(ThirdPartyScansDataStore thirdPartyScansDataStore) {
    super(thirdPartyScansDataStore);
  }

  @Override
  public ThirdPartySbomMetadata getById(String id) {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.id=?1";
    return get(sQuery, id);
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
    String sQuery = "DELETE FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.thirdPartyFileId=?1";
    createQuery(sQuery, thirdPartyFileId).executeUpdate(tx);
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
    return getList(sQuery, applicationId, ACTIVE_STATUS);
  }

  public ThirdPartySbomMetadata getLatestActiveByApplicationId(String applicationId) {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.applicationId=?1" +
        " AND entity.status=?2" +
        " ORDER BY entity.createdAt DESC";
    return createQuery(sQuery, applicationId, ACTIVE_STATUS).forceSingleResult().get();
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
      String status)
  {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " + //
        " WHERE entity.applicationId = ?1 AND entity.sbomVersion=?2 AND entity.status=?3";
    return get(sQuery, applicationId, sbomVersion, status);
  }

  public void deleteByApplicationId(TransactionContext tx, String applicationId) {
    String sQuery = "DELETE FROM ThirdPartySbomMetadata entity " //
        + " WHERE entity.applicationId=?1";
    createQuery(sQuery, applicationId).executeUpdate(tx);
  }

  public long getActiveSbomCount(String applicationId) {
    String sQuery = "SELECT COUNT(entity) FROM ThirdPartySbomMetadata entity"
        + " WHERE entity.applicationId=?1 AND entity.status=?2";
    return getSingle(Long.class, sQuery, applicationId, ACTIVE_STATUS);
  }

  public long getSbomCount() {
    String sQuery = "SELECT COUNT(entity) FROM ThirdPartySbomMetadata entity";
    return getSingle(Long.class, sQuery);
  }

  public long getActiveSbomCount() {
    String sQuery = "SELECT COUNT(entity) FROM ThirdPartySbomMetadata entity " //
        + "WHERE entity.status=?1";
    return getSingle(Long.class, sQuery, ACTIVE_STATUS);
  }

  public List<ThirdPartySbomMetadata> getPendingSbomsOlderThanDuration(Duration pendingTimeLimit) {
    Date limitDate = Timestamp.valueOf(LocalDateTime.now().minus(pendingTimeLimit));
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " //
        + "WHERE entity.status='PENDING' AND entity.createdAt <= ?1";
    return getList(sQuery, limitDate);
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
      javax.persistence.Query query = createNativeQuery(tx, sQuery, lastYear, lastMonth, lastWeek, ACTIVE_STATUS);
      Object[] result = (Object[]) query.getSingleResult();
      return new ApiSbomApplicationsHistoryMetricDTO(result);
    }
  }

  public List<ThirdPartySbomMetadata> getByApplicationIdAndStatus(
      final String applicationId,
      final String status,
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
      final String status,
      final int page,
      final int pageSize)
  {
    String sQuery = "SELECT entity FROM ThirdPartySbomMetadata entity " +
        "WHERE entity.applicationId = ?1 AND entity.status=?2 " +
        "ORDER BY entity.createdAt DESC";

    int offset = (page - 1) * pageSize;
    javax.persistence.Query paginationQuery = createPaginationQuery(tx, sQuery, offset, pageSize);
    paginationQuery.setParameter(1, applicationId);
    paginationQuery.setParameter(2, status);
    return paginationQuery.getResultList();
  }

  public void makeSbomActiveIfExist(String scanId) {
    ThirdPartySbomMetadata sbomMetadata = getByScanId(scanId);
    if (sbomMetadata != null) {
      sbomMetadata.setStatus("ACTIVE");
      update(sbomMetadata);
    }
  }
}
