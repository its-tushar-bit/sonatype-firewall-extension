/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractThirdPartyScansSqlDAO;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.dataaccess.TransactionContext;

import static com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO.createPaginationNativeQuery;
import static com.sonatype.insight.brain.utils.CvssV3Severity.CRITICAL;
import static com.sonatype.insight.brain.utils.CvssV3Severity.HIGH;
import static com.sonatype.insight.brain.utils.CvssV3Severity.LOW;
import static com.sonatype.insight.brain.utils.CvssV3Severity.MEDIUM;
import static com.sonatype.insight.brain.utils.CvssV3Severity.NONE;

@Named
@Singleton
public class ThirdPartyFileCoordinateDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartyFileCoordinate>
{
  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  @Inject
  public ThirdPartyFileCoordinateDAO(
      final ThirdPartyScansDataStore thirdPartyScansDataStore,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO)
  {
    super(thirdPartyScansDataStore);
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.thirdPartyCoordinateLicenseDAO = thirdPartyCoordinateLicenseDAO;
  }

  @Override
  public ThirdPartyFileCoordinate getById(String id) {
    String sQuery = "SELECT entity FROM ThirdPartyFileCoordinate entity" + //
        " WHERE entity.id=?1";
    return get(sQuery, id);
  }

  public List<ThirdPartyFileCoordinate> getBySourceFormatNameVersionAndThirdPartyFileId(
      String source,
      String format,
      String name,
      String version,
      String thirdPartyFileId)
  {
    String sQuery = "SELECT entity FROM ThirdPartyFileCoordinate entity" + //
        " WHERE entity.source=?1 AND entity.format=?2 AND entity.name=?3" + //
        " AND entity.version=?4 AND entity.thirdPartyFileId=?5";
    return getList(sQuery, source, format, name, version, thirdPartyFileId);
  }

  public List<ThirdPartyFileCoordinate> getAll() {
    return getList("SELECT entity FROM ThirdPartyFileCoordinate entity");
  }

  public List<ThirdPartyFileCoordinate> getByThirdPartyFileId(String thirdPartyFileId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByThirdPartyFileId(tx, thirdPartyFileId);
    }
  }

  public ThirdPartyFileCoordinate getByPackageUrlAndScanId(String purl, String scanId) {
    String sQuery = "SELECT TPF FROM ThirdPartyScan TPS," + //
        " ThirdPartyFileCoordinate TPF" + //
        " WHERE TPS.thirdPartyFileId=TPF.thirdPartyFileId AND TPF.packageUrl=?1 AND TPS.scanId=?2";
    return get(sQuery, purl, scanId);
  }

  public ThirdPartyFileCoordinate getByFormatNameVersionAndScanID(String format,
                                                                 String name,
                                                                 String version,
                                                                 String scanId)
  {
    String sQuery = "SELECT TPF FROM ThirdPartyScan TPS," + //
        " ThirdPartyFileCoordinate TPF" + //
        " WHERE TPS.thirdPartyFileId=TPF.thirdPartyFileId" + //
        " AND TPF.format=?1 AND TPF.name=?2 AND TPF.version=?3 AND TPS.scanId=?4";
    return get(sQuery, format, name, version, scanId);
  }

  public List<ThirdPartyFileCoordinate> getByHashAndScanId(String hash, String scanId) {
    String sQuery = "SELECT TPF FROM ThirdPartyScan TPS," + //
        " ThirdPartyFileCoordinate TPF" + //
        " WHERE TPS.thirdPartyFileId=TPF.thirdPartyFileId AND TPF.hash=?1 AND TPS.scanId=?2";
    return getList(sQuery, hash, scanId);
  }

  public List<ThirdPartyFileCoordinate> getByScanId(String scanId) {
    String sQuery = "SELECT TPF FROM ThirdPartyScan TPS," + //
        " ThirdPartyFileCoordinate TPF" + //
        " WHERE TPS.thirdPartyFileId=TPF.thirdPartyFileId AND TPS.scanId=?1";
    return getList(sQuery, scanId);
  }

  public List<ThirdPartyFileCoordinate> getByThirdPartyFileId(TransactionContext tx, String thirdPartyFileId) {
    String sQuery = "SELECT entity FROM ThirdPartyFileCoordinate entity" + //
        " WHERE entity.thirdPartyFileId=?1";
    return getList(tx, sQuery, thirdPartyFileId);
  }

  public ThirdPartyFileCoordinate getByThirdPartyFileIdAndPackageUrl(
      final String thirdPartyFileId,
      final String purl)
  {
    String sQuery = "SELECT entity FROM ThirdPartyFileCoordinate entity" + //
        " WHERE entity.thirdPartyFileId=?1 AND entity.packageUrl=?2";
    return get(sQuery, thirdPartyFileId, purl);
  }

  public List<ThirdPartyFileCoordinate> getBySbomMetadataId(String sbomMetadataId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getBySbomMetadataId(tx, sbomMetadataId);
    }
  }

  public List<ThirdPartyFileCoordinate> getBySbomMetadataId(TransactionContext tx, String sbomMetadataId) {
    String sQuery = "SELECT entity FROM ThirdPartyFileCoordinate entity, ThirdPartySbomMetadata sbomMetadata" +
        " WHERE sbomMetadata.id = ?1 AND entity.thirdPartyFileId = sbomMetadata.thirdPartyFileId";

    return getList(tx, sQuery, sbomMetadataId);
  }

  @SuppressWarnings("unchecked")
  public List<SbomComponentDTO> getSbomComponentsByThirdPartyFileId(String thirdPartyFileId) {
    String sQuery = "" + //
        "SELECT fc.hash," + //
        "       fc.package_url," + //
        "       fc.name," + //
        "       fc.version," + //
        "       lic.licenses ::TEXT as licenses_json," + //
        "       COUNT(CASE WHEN (cs.severity = ?1) THEN 1 END) AS severity_none," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?2 AND ?3) THEN 1 END) AS severity_low," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?4 AND ?5) THEN 1 END) AS severity_medium," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?6 AND ?7) THEN 1 END) AS severity_high," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?8 AND ?9) THEN 1 END) AS severity_critical" + //
        " FROM " + getDatabaseSchema() + ".file_coordinate fc" + //
        "  LEFT JOIN " + getDatabaseSchema() + ".coordinate_security cs" + //
        "    ON cs.file_coordinate_id = fc.file_coordinate_id" + //
        "  LEFT JOIN LATERAL (" + //
        "       SELECT cl.file_coordinate_id," + //
        "              JSON_AGG(JSON_BUILD_OBJECT('licenseId', cl.license_id, 'licenseName', cl.name)) AS licenses" + //
        "         FROM " + getDatabaseSchema() + ".coordinate_license cl" + //
        "         WHERE cl.file_coordinate_id = fc.file_coordinate_id" + //
        "         GROUP BY cl.file_coordinate_id) lic" + //
        "    ON lic.file_coordinate_id = fc.file_coordinate_id" + //
        " WHERE fc.third_party_file_id = ?10" + //
        " GROUP BY fc.hash, fc.package_url, fc.name, fc.version, licenses_json";

    try (TransactionContext tx = createTransactionContext()) {
      javax.persistence.Query query = createNativeQuery(tx, sQuery, NONE.getStartScoreRange(), LOW.getStartScoreRange(),
          LOW.getEndScoreRange(), MEDIUM.getStartScoreRange(), MEDIUM.getEndScoreRange(), HIGH.getStartScoreRange(),
          HIGH.getEndScoreRange(), CRITICAL.getStartScoreRange(), CRITICAL.getEndScoreRange(), thirdPartyFileId);

      return ((Stream<Object[]>) query.getResultStream())
          .map(SbomComponentDTO::new)
          .collect(Collectors.toList());
    }
  }

  public void deleteByThirdPartyFileId(TransactionContext tx, String thirdPartyFileId) {
    List<ThirdPartyFileCoordinate> coordinateFiles = getByThirdPartyFileId(tx, thirdPartyFileId);
    coordinateFiles.forEach(entity -> delete(tx, entity));
  }

  @Override
  public void delete(TransactionContext tx, ThirdPartyFileCoordinate fileCoordinate) {
    // cascade delete coordinate security records
    thirdPartyCoordinateSecurityDAO.deleteByFileCoordinateId(tx, fileCoordinate.getId());

    // cascade delete coordinate license records
    thirdPartyCoordinateLicenseDAO.deleteByFileCoordinateId(tx, fileCoordinate.getId());

    super.delete(tx, fileCoordinate);
  }

  @SuppressWarnings("unchecked")
  public ThirdPartySbomMetadataSummaryListDTO getSbomApplicationVulnerabilities(
      String applicationId,
      String sortByDate,
      int pageSize,
      int page)
  {
    String sQuery = "" + //
        "SELECT sm.sbom_version," + //
        "       sm.spec," + //
        "       sm.spec_version," + //
        "       sm.created_at," + //
        "       COUNT(CASE WHEN (cs.severity = ?1) THEN 1 END)," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?2 AND ?3) THEN 1 END)," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?4 AND ?5) THEN 1 END)," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?6 AND ?7) THEN 1 END)," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?8 AND ?9) THEN 1 END)," + //
        "       COUNT(*) OVER() AS full_count" + //
        " FROM " + getDatabaseSchema() + ".sbom_metadata sm" + //
        "  LEFT JOIN " + getDatabaseSchema() + ".file_coordinate fc" + //
        "    ON fc.third_party_file_id = sm.third_party_file_id" + //
        "  LEFT JOIN " + getDatabaseSchema() + ".coordinate_security cs" + //
        "    ON cs.file_coordinate_id = fc.file_coordinate_id" + //
        " WHERE sm.application_id = ?10" + //
        "   AND sm.status = ?11" + //
        " GROUP BY sm.sbom_version, sm.spec, sm.spec_version, sm.created_at" + //
        " ORDER BY sm.created_at " + (sortByDate.equalsIgnoreCase("asc") ? "ASC " : "DESC ");

    int offset = (page - 1) * pageSize;
    ThirdPartySbomMetadataSummaryListDTO result = new ThirdPartySbomMetadataSummaryListDTO();

    try (TransactionContext tx = createTransactionContext()) {
      javax.persistence.Query paginationQuery = createPaginationNativeQuery(tx, sQuery, offset, pageSize);
      paginationQuery.setParameter(1, NONE.getStartScoreRange());
      paginationQuery.setParameter(2, LOW.getStartScoreRange());
      paginationQuery.setParameter(3, LOW.getEndScoreRange());
      paginationQuery.setParameter(4, MEDIUM.getStartScoreRange());
      paginationQuery.setParameter(5, MEDIUM.getEndScoreRange());
      paginationQuery.setParameter(6, HIGH.getStartScoreRange());
      paginationQuery.setParameter(7, HIGH.getEndScoreRange());
      paginationQuery.setParameter(8, CRITICAL.getStartScoreRange());
      paginationQuery.setParameter(9, CRITICAL.getEndScoreRange());
      paginationQuery.setParameter(10, applicationId);
      paginationQuery.setParameter(11, "ACTIVE");

      List<ThirdPartySbomMetadataSummaryDTO> dtos = ((Stream<Object[]>) paginationQuery.getResultStream())
          .peek(array -> {
            if (result.getTotalResultsCount() == 0) {
              result.setTotalResultsCount(((Long) array[9]).intValue());
            }
          })
          .map(ThirdPartySbomMetadataSummaryDTO::new)
          .collect(Collectors.toList());

      result.setResults(dtos);
      return result;
    }
  }
}
