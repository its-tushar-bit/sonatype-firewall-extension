/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.time.LocalDate;
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

  public long getSbomCount() {
    String sQuery = "SELECT COUNT(entity) FROM ThirdPartySbomMetadata entity";
    return getSingle(Long.class, sQuery);
  }

  public long getActiveSbomCount() {
    String sQuery = "SELECT COUNT(entity) FROM ThirdPartySbomMetadata entity " //
        + "WHERE entity.status='ACTIVE'";
    return getSingle(Long.class, sQuery);
  }

  /**
   * This allows service-layer code to create a SearchIndexChanges for insert or update at the appropriate times.
   * It also implements the search index change for deletions.
   */
  @Override
  public SearchIndexChange newSearchIndexChange(ThirdPartySbomMetadata sbomMetadata) {
    return new SearchIndexChange(ChangeType.SBOM,
        String.format("%s:%s", sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion()));
  }

  /**
   * Search indexing for these records should not occur automatically, as child records need to be in place
   * before the indexing is done, and those records are outside the scope of this DAO
   */
  @Override
  protected SearchIndexChange newSearchIndexChangeForInsert(ThirdPartySbomMetadata sbomMetadata) {
    return null;
  }

  public ApiSbomApplicationsHistoryMetricDTO getSbomsHistoryMetrics() {
    String sQuery = "" + //
        "SELECT COUNT(1) AS total," + //
        "       COUNT(CASE WHEN (sm.created_at >= ?1) THEN 1 END) AS last_year," + //
        "       COUNT(CASE WHEN (sm.created_at >= ?2) THEN 1 END) AS last_month," + //
        "       COUNT(CASE WHEN (sm.created_at >= ?3) THEN 1 END) AS last_week" + //
        "  FROM " + getDatabaseSchema() + ".sbom_metadata sm" + //
        " WHERE sm.status = ?4";

    LocalDate now = LocalDate.now();
    LocalDate lastWeek = now.minusWeeks(1);
    LocalDate lastMonth = now.minusMonths(1);
    LocalDate lastYear = now.minusYears(1);

    try (TransactionContext tx = createTransactionContext()) {
      javax.persistence.Query query = createNativeQuery(tx, sQuery, lastYear, lastMonth, lastWeek, "ACTIVE");
      Object[] result = (Object[]) query.getSingleResult();
      return new ApiSbomApplicationsHistoryMetricDTO(result);
    }
  }
}
