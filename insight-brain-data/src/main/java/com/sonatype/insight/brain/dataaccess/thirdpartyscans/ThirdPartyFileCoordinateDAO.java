/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractThirdPartyScansSqlDAO;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.model.thirdpartyscans.BomPageSbomSummaryDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentListDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomDependencyTypeDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.InternalServerException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.CoordinateSecurity.COORDINATE_SECURITY;
import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.FileCoordinate.FILE_COORDINATE;
import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.SbomMetadata.SBOM_METADATA;
import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.ThirdPartyScan.THIRD_PARTY_SCAN;
import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.VulnerabilityExploitability.VULNERABILITY_EXPLOITABILITY;
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
  public void insert(TransactionContext tx, ThirdPartyFileCoordinate entity) {
    if (StringUtils.isBlank(entity.getDisplayName())) {
      entity.setDisplayName(
          FileCoordinateDisplayNameGenerator.generateDisplayName(entity.getPackageUrl(), entity.getFormat(),
              entity.getName(), entity.getVersion()));
    }
    if (entity.getId() == null) {
      entity.setId(UUID.randomUUID().toString());
    }
    tx.dsl()
        .insertInto(FILE_COORDINATE)
        .set(FILE_COORDINATE.FILE_COORDINATE_ID, entity.getId())
        .set(FILE_COORDINATE.HASH, entity.getHash())
        .set(FILE_COORDINATE.SOURCE, entity.getSource())
        .set(FILE_COORDINATE.PACKAGE_URL, entity.getPackageUrl())
        .set(FILE_COORDINATE.FORMAT, entity.getFormat())
        .set(FILE_COORDINATE.NAME, entity.getName())
        .set(FILE_COORDINATE.VERSION, entity.getVersion())
        .set(FILE_COORDINATE.THIRD_PARTY_FILE_ID, entity.getThirdPartyFileId())
        .set(FILE_COORDINATE.COMPONENT_REF, entity.getComponentRef())
        .set(FILE_COORDINATE.CPE, entity.getCpe())
        .set(FILE_COORDINATE.SWID, entity.getSwid())
        .set(FILE_COORDINATE.IDENTIFICATION_SOURCES, entity.getIdentificationSources())
        .set(FILE_COORDINATE.DEPENDENCY_TYPE, entity.getDependencyType())
        .set(FILE_COORDINATE.MATCH_STATE_ID, entity.getMatchStateId())
        .set(FILE_COORDINATE.OCCURRENCES, entity.getOccurrencesList() != null && !entity.getOccurrencesList().isEmpty()
            ? String.join(",", entity.getOccurrencesList())
            : null)
        .set(FILE_COORDINATE.FILENAMES, entity.getFilenamesList() != null && !entity.getFilenamesList().isEmpty()
            ? String.join(",", entity.getFilenamesList())
            : null)
        .set(FILE_COORDINATE.DISPLAY_NAME, entity.getDisplayName())
        .execute();
  }

  @Override
  public void delete(TransactionContext tx, ThirdPartyFileCoordinate fileCoordinate) {
    // cascade delete coordinate security records
    thirdPartyCoordinateSecurityDAO.deleteByFileCoordinateId(tx, fileCoordinate.getId());

    // cascade delete coordinate license records
    thirdPartyCoordinateLicenseDAO.deleteByFileCoordinateId(tx, fileCoordinate.getId());

    tx.dsl()
        .deleteFrom(FILE_COORDINATE)
        .where(FILE_COORDINATE.FILE_COORDINATE_ID.eq(fileCoordinate.getId()))
        .execute();
  }

  public ThirdPartyFileCoordinate getByComponentRef(String componentRef, String thirdPartyFileId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(FILE_COORDINATE)
          .where(FILE_COORDINATE.COMPONENT_REF.eq(componentRef)
              .and(FILE_COORDINATE.THIRD_PARTY_FILE_ID.eq(thirdPartyFileId)))
          .fetchOneInto(ThirdPartyFileCoordinate.class);
    }
  }

  public List<ThirdPartyFileCoordinate> getByThirdPartyFileId(String thirdPartyFileId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByThirdPartyFileId(tx, thirdPartyFileId);
    }
  }

  public List<ThirdPartyFileCoordinate> getByPackageUrlAndScanId(String purl, String scanId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(FILE_COORDINATE.fields())
          .from(FILE_COORDINATE)
          .join(THIRD_PARTY_SCAN)
          .on(THIRD_PARTY_SCAN.THIRD_PARTY_FILE_ID.eq(FILE_COORDINATE.THIRD_PARTY_FILE_ID))
          .where(FILE_COORDINATE.PACKAGE_URL.eq(purl)
              .and(THIRD_PARTY_SCAN.SCAN_ID.eq(scanId)))
          .fetchInto(ThirdPartyFileCoordinate.class);
    }
  }

  /**
   * @param purl
   * @param hash
   * @param scanId
   * @return
   * @deprecated instead use either
   *             <ul>
   *             <li>
   *             {@link #getByHashOrComponentRefForThirdPartyFileId(String, String, String)} (String, String)} or
   *             </li>
   *             <li>
   *             {@link #getByPackageUrlAndScanId(String, String)}
   *             </li>
   *             </ul>
   *             does not guarantee unique records for new scans.
   *             keeping only for historical sbom records (for backward compatibility)
   */
  @Deprecated
  public ThirdPartyFileCoordinate getByPackageUrlAndHashAndScanId(String purl, String hash, String scanId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(FILE_COORDINATE.fields())
          .from(FILE_COORDINATE)
          .join(THIRD_PARTY_SCAN)
          .on(THIRD_PARTY_SCAN.THIRD_PARTY_FILE_ID.eq(FILE_COORDINATE.THIRD_PARTY_FILE_ID))
          .where(FILE_COORDINATE.PACKAGE_URL.eq(purl)
              .and(FILE_COORDINATE.HASH.eq(hash))
              .and(THIRD_PARTY_SCAN.SCAN_ID.eq(scanId)))
          .fetchOneInto(ThirdPartyFileCoordinate.class);
    }
  }

  /**
   * @param format
   * @param name
   * @param version
   * @param scanId
   * @return
   * @deprecated instead use either
   *             <ul>
   *             <li>
   *             {@link #getByHashOrComponentRefForThirdPartyFileId(String, String, String)} (String, String)} or
   *             </li>
   *             <li>
   *             {@link #getByPackageUrlAndScanId(String, String)}
   *             </li>
   *             </ul>
   *             does not guarantee unique records for new scans.
   *             keeping only for historical sbom records (for backward compatibility)
   */
  @Deprecated
  public ThirdPartyFileCoordinate getByFormatNameVersionAndScanID(
      String format,
      String name,
      String version,
      String scanId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(FILE_COORDINATE.fields())
          .from(FILE_COORDINATE)
          .join(THIRD_PARTY_SCAN)
          .on(THIRD_PARTY_SCAN.THIRD_PARTY_FILE_ID.eq(FILE_COORDINATE.THIRD_PARTY_FILE_ID))
          .where(FILE_COORDINATE.FORMAT.eq(format)
              .and(FILE_COORDINATE.NAME.eq(name))
              .and(FILE_COORDINATE.VERSION.eq(version))
              .and(THIRD_PARTY_SCAN.SCAN_ID.eq(scanId)))
          .fetchOneInto(ThirdPartyFileCoordinate.class);
    }
  }

  public List<ThirdPartyFileCoordinate> getByScanId(String scanId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(FILE_COORDINATE.fields())
          .from(FILE_COORDINATE)
          .join(THIRD_PARTY_SCAN)
          .on(THIRD_PARTY_SCAN.THIRD_PARTY_FILE_ID.eq(FILE_COORDINATE.THIRD_PARTY_FILE_ID))
          .where(THIRD_PARTY_SCAN.SCAN_ID.eq(scanId))
          .fetchInto(ThirdPartyFileCoordinate.class);
    }
  }

  public List<ThirdPartyFileCoordinate> getByThirdPartyFileId(TransactionContext tx, String thirdPartyFileId) {
    return tx.dsl()
        .selectFrom(FILE_COORDINATE)
        .where(FILE_COORDINATE.THIRD_PARTY_FILE_ID.eq(thirdPartyFileId))
        .fetchInto(ThirdPartyFileCoordinate.class);
  }

  /**
   * @deprecated instead use either
   *             <ul>
   *             <li>
   *             {@link #getByHashOrComponentRefForThirdPartyFileId(String, String, String)} (String, String)} or
   *             </li>
   *             <li>
   *             {@link #getByPackageUrlAndScanId(String, String)}
   *             </li>
   *             </ul>
   *             for guarantee unique records for new scans.
   */
  @Deprecated
  public ThirdPartyFileCoordinate getByThirdPartyFileIdAndPackageUrl(
      final String thirdPartyFileId,
      final String purl)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(FILE_COORDINATE)
          .where(FILE_COORDINATE.THIRD_PARTY_FILE_ID.eq(thirdPartyFileId)
              .and(FILE_COORDINATE.PACKAGE_URL.eq(purl)))
          .fetchOneInto(ThirdPartyFileCoordinate.class);
    }
  }

  public List<ThirdPartyFileCoordinate> getBySbomMetadataId(String sbomMetadataId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getBySbomMetadataId(tx, sbomMetadataId);
    }
  }

  public List<ThirdPartyFileCoordinate> getBySbomMetadataId(TransactionContext tx, String sbomMetadataId) {
    return tx.dsl()
        .select(FILE_COORDINATE.fields())
        .from(FILE_COORDINATE)
        .join(SBOM_METADATA)
        .on(FILE_COORDINATE.THIRD_PARTY_FILE_ID.eq(SBOM_METADATA.THIRD_PARTY_FILE_ID))
        .where(SBOM_METADATA.SBOM_METADATA_ID.eq(sbomMetadataId))
        .fetchInto(ThirdPartyFileCoordinate.class);
  }

  public ThirdPartyFileCoordinate getBySbomMetadataIdAndComponentHash(
      String sbomMetadataId,
      String componentHash)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getBySbomMetadataIdAndComponentHash(tx, sbomMetadataId, componentHash);
    }
  }

  public ThirdPartyFileCoordinate getBySbomMetadataIdAndComponentHash(
      TransactionContext tx,
      String sbomMetadataId,
      String componentHash)
  {
    return tx.dsl()
        .select(FILE_COORDINATE.fields())
        .from(FILE_COORDINATE)
        .join(SBOM_METADATA)
        .on(FILE_COORDINATE.THIRD_PARTY_FILE_ID.eq(SBOM_METADATA.THIRD_PARTY_FILE_ID))
        .where(SBOM_METADATA.SBOM_METADATA_ID.eq(sbomMetadataId)
            .and(FILE_COORDINATE.HASH.eq(componentHash)))
        .fetchOneInto(ThirdPartyFileCoordinate.class);
  }

  public SbomComponentListDTO getSbomComponentsByThirdPartyFileId(
      String thirdPartyFileId,
      Set<CvssV3Severity> vulnerabilityThreatLevels,
      Set<ThirdPartyDependencyType> dependencyTypes,
      String filterText,
      SbomComponentSortableField sortBy,
      boolean asc,
      int pageSize,
      int page)
  {
    List<Object> params = new ArrayList<>();
    List<String> dependencyTypesParamValues = new ArrayList<>();

    String sQuery = "" + //
        "SELECT fc.hash," + //
        "       fc.package_url," + //
        "       fc.name," + //
        "       fc.version," + //
        "       fc.format," + //
        "       fc.display_name," + //
        "       lic.licenses ::TEXT as licenses_json," + //
        "       COUNT(CASE WHEN (cs.severity = ?) THEN 1 END) AS severity_none," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ? AND ?) THEN 1 END) AS severity_low," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ? AND ?) THEN 1 END) AS severity_medium," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ? AND ?) THEN 1 END) AS severity_high," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ? AND ?) THEN 1 END) AS severity_critical, " + //
        "       ROUND(COUNT(ve) * 100.0 / GREATEST(COUNT(cs), 1), 1) as percentage, " + //
        "       COALESCE(ROUND((COUNT(CASE WHEN (ve.coordinate_security_id IS NOT NULL" + //
        "           AND cs.severity >= ?) THEN 1 END)) * 100 / NULLIF(COUNT(CASE WHEN " + //
        "           (cs.coordinate_security_id IS NOT NULL AND cs.severity >= ?) THEN 1 END)" + //
        "           ::decimal, 0), 1), 100) as release_status_percentage, " + //
        "       fc.dependency_type," + //
        "       fc.file_coordinate_id," + //
        "       fc.filenames," + //
        "       fc.match_state_id," + //
        "       fc.component_ref," + //
        "       COUNT(*) OVER() AS full_count" + //
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
        "  LEFT JOIN " + getDatabaseSchema() + ".vulnerability_exploitability ve" + //
        "    ON cs.coordinate_security_id = ve.coordinate_security_id" + //
        " WHERE fc.third_party_file_id = ?";

    // Add severity range parameters
    params.add(NONE.getStartScoreRange());
    params.add(LOW.getStartScoreRange());
    params.add(LOW.getEndScoreRange());
    params.add(MEDIUM.getStartScoreRange());
    params.add(MEDIUM.getEndScoreRange());
    params.add(HIGH.getStartScoreRange());
    params.add(HIGH.getEndScoreRange());
    params.add(CRITICAL.getStartScoreRange());
    params.add(CRITICAL.getEndScoreRange());
    params.add(HIGH.getStartScoreRange()); // for release_status_percentage >= HIGH
    params.add(HIGH.getStartScoreRange()); // for release_status_percentage >= HIGH
    params.add(thirdPartyFileId);

    sQuery = applyFilterTextJooq(filterText, params, sQuery);
    sQuery += generateHavingByDependencyTypesJooq(dependencyTypes, dependencyTypesParamValues) + //
        " GROUP BY fc.hash, fc.package_url, fc.name, fc.version, fc.display_name, fc.format, licenses_json ,fc" +
        ".dependency_type, fc.filenames, fc.file_coordinate_id, fc.match_state_id" + //
        generateHavingByVulnerabilityThreatLevelsJooq(vulnerabilityThreatLevels) + //
        generateOrderBySortFieldSelected(sortBy, asc);

    // Add dependency type parameters
    params.addAll(dependencyTypesParamValues);

    int offset = (page - 1) * pageSize;
    sQuery += " OFFSET ? LIMIT ?";
    params.add(offset);
    params.add(pageSize);

    try (TransactionContext tx = createTransactionContext()) {
      SbomComponentListDTO result = new SbomComponentListDTO();

      List<SbomComponentDTO> dtos = tx.dsl()
          .resultQuery(sQuery, params.toArray())
          .fetchStream()
          .peek(record -> {
            if (result.getTotalResultsCount() == 0) {
              result.setTotalResultsCount(record.get(19, Long.class).intValue());
            }
          })
          .map(record -> new SbomComponentDTO(record.intoArray()))
          .collect(Collectors.toList());

      result.setResults(dtos);
      return result;
    }
  }

  public void deleteByThirdPartyFileId(TransactionContext tx, String thirdPartyFileId) {
    List<ThirdPartyFileCoordinate> coordinateFiles = getByThirdPartyFileId(tx, thirdPartyFileId);
    coordinateFiles.forEach(entity -> delete(tx, entity));
  }

  public BomPageSbomSummaryDTO getSbomVunerabilitySummaryForComponents(
      String applicationId,
      String version)
  {
    try (TransactionContext tx = createTransactionContext()) {
      // Severity range values - cast to double for jOOQ compatibility
      double noneStart = NONE.getStartScoreRange();
      double lowStart = LOW.getStartScoreRange();
      double lowEnd = LOW.getEndScoreRange();
      double mediumStart = MEDIUM.getStartScoreRange();
      double mediumEnd = MEDIUM.getEndScoreRange();
      double highStart = HIGH.getStartScoreRange();
      double highEnd = HIGH.getEndScoreRange();
      double criticalStart = CRITICAL.getStartScoreRange();
      double criticalEnd = CRITICAL.getEndScoreRange();

      // Count expressions for severity levels
      var noneCount = DSL.count(
          DSL.when(COORDINATE_SECURITY.SEVERITY.eq(noneStart), 1)).as("none");
      var lowCount = DSL.count(
          DSL.when(COORDINATE_SECURITY.SEVERITY.between(lowStart, lowEnd), 1)).as("low");
      var mediumCount = DSL.count(
          DSL.when(COORDINATE_SECURITY.SEVERITY.between(mediumStart, mediumEnd), 1)).as("medium");
      var highCount = DSL.count(
          DSL.when(COORDINATE_SECURITY.SEVERITY.between(highStart, highEnd), 1)).as("high");
      var criticalCount = DSL.count(
          DSL.when(COORDINATE_SECURITY.SEVERITY.between(criticalStart, criticalEnd), 1)).as("critical");

      // Complex release status percentage calculation
      var vexWithHighSeverity = DSL.count(
          DSL.when(VULNERABILITY_EXPLOITABILITY.COORDINATE_SECURITY_ID.isNotNull()
              .and(COORDINATE_SECURITY.SEVERITY.ge(highStart)), 1));
      var csWithHighSeverity = DSL.count(
          DSL.when(COORDINATE_SECURITY.COORDINATE_SECURITY_ID.isNotNull()
              .and(COORDINATE_SECURITY.SEVERITY.ge(highStart)), 1));
      var releaseStatusPercentage = DSL.coalesce(
          DSL.round(
              vexWithHighSeverity.mul(100)
                  .div(
                      DSL.nullif(csWithHighSeverity.cast(java.math.BigDecimal.class), java.math.BigDecimal.ZERO)),
              1),
          DSL.inline(java.math.BigDecimal.valueOf(100))).as("releaseStatusPercentage");

      Object[] result = tx.dsl()
          .select(
              noneCount,
              lowCount,
              mediumCount,
              highCount,
              criticalCount,
              releaseStatusPercentage)
          .from(SBOM_METADATA)
          .leftJoin(COORDINATE_SECURITY)
          .on(COORDINATE_SECURITY.SBOM_METADATA_ID.eq(SBOM_METADATA.SBOM_METADATA_ID))
          .leftJoin(VULNERABILITY_EXPLOITABILITY)
          .on(COORDINATE_SECURITY.COORDINATE_SECURITY_ID.eq(VULNERABILITY_EXPLOITABILITY.COORDINATE_SECURITY_ID))
          .where(SBOM_METADATA.APPLICATION_ID.eq(applicationId)
              .and(SBOM_METADATA.SBOM_VERSION.eq(version)))
          .fetchOne()
          .intoArray();

      return new BomPageSbomSummaryDTO(result);
    }
  }

  public SbomDependencyTypeDTO getSbomDependencyTypeSummaryForComponents(
      String applicationId,
      String version)
  {
    try (TransactionContext tx = createTransactionContext()) {
      var record = tx.dsl()
          .select(
              DSL.count(
                  DSL.when(FILE_COORDINATE.DEPENDENCY_TYPE.eq("D"), 1)).as("direct"),
              DSL.count(
                  DSL.when(FILE_COORDINATE.DEPENDENCY_TYPE.eq("T"), 1)).as("transitive"),
              DSL.count(
                  DSL.when(FILE_COORDINATE.DEPENDENCY_TYPE.isNull(), 1)).as("unknown"))
          .from(SBOM_METADATA)
          .join(FILE_COORDINATE)
          .on(FILE_COORDINATE.THIRD_PARTY_FILE_ID.eq(SBOM_METADATA.THIRD_PARTY_FILE_ID))
          .where(SBOM_METADATA.APPLICATION_ID.eq(applicationId)
              .and(SBOM_METADATA.SBOM_VERSION.eq(version)))
          .fetchOne();
      if (record == null) {
        return null;
      }
      return new SbomDependencyTypeDTO(record.intoArray());
    }
  }

  public long getNumberOfComponentsForSbom(
      String applicationId,
      String version)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectCount()
          .from(SBOM_METADATA)
          .join(FILE_COORDINATE)
          .on(FILE_COORDINATE.THIRD_PARTY_FILE_ID.eq(SBOM_METADATA.THIRD_PARTY_FILE_ID))
          .where(SBOM_METADATA.APPLICATION_ID.eq(applicationId)
              .and(SBOM_METADATA.SBOM_VERSION.eq(version)))
          .fetchOne(0, Long.class);
    }
  }

  public boolean hasNonNullComponentRefs(String thirdPartyFileId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectCount()
          .from(FILE_COORDINATE)
          .where(FILE_COORDINATE.THIRD_PARTY_FILE_ID.eq(thirdPartyFileId)
              .and(FILE_COORDINATE.COMPONENT_REF.isNotNull()))
          .fetchOne(0, Long.class) > 0;
    }
  }

  private String generateOrderBySortFieldSelected(SbomComponentSortableField sortBy, boolean asc) {
    if (sortBy == null) {
      return "";
    }
    String query;
    String order = asc ? "ASC" : "DESC";
    String tieBreaker =
        ", fc.name " + order + ", fc.version " + order + ", fc.package_url " + order + ", fc.hash " + order;
    switch (sortBy) {
      case TYPE:
        query = " ORDER BY fc.dependency_type " + order + tieBreaker;
        break;
      case PERCENTAGE_ANNOTATED:
        query = " ORDER BY percentage " + order + tieBreaker;
        break;
      case RELEASE_STATUS_PERCENTAGE:
        query = " ORDER BY release_status_percentage " + order + tieBreaker;
        break;
      case DISPLAY_NAME:
        query = " ORDER BY fc.display_name " + order + tieBreaker;
        break;
      case VULNERABILITIES:
      default:
        query = " ORDER BY severity_critical " + order +
            " , severity_high " + order +
            " , severity_medium " + order +
            " , severity_low " + order +
            tieBreaker;
    }
    return query;
  }

  private String generateHavingByDependencyTypesJooq(
      Set<ThirdPartyDependencyType> dependencyTypes,
      List<String> params)
  {
    if (CollectionUtils.isNotEmpty(dependencyTypes)) {
      String query = " AND (";

      for (ThirdPartyDependencyType dependencyType : dependencyTypes) {
        switch (dependencyType) {
          case DIRECT:
            query += "fc.dependency_type = ? OR ";
            params.add(ThirdPartyDependencyType.DIRECT.getValue());
            break;
          case TRANSITIVE:
            query += "fc.dependency_type = ? OR ";
            params.add(ThirdPartyDependencyType.TRANSITIVE.getValue());
            break;
          case UNSPECIFIED:
            query += "fc.dependency_type IS NULL OR ";
            break;
          default:
            query += "";
        }
      }

      return StringUtils.removeEnd(query, "OR ") + ")";
    }

    return "";
  }

  private String applyFilterJooq(String filterText, List<Object> params) {
    if (!filterText.isEmpty()) {
      String filterTextQuoted = urlEncodeCoordinate(filterText);
      String query = " AND ((lower(fc.package_url) ~ lower(?))" + //
          " OR (lower(fc.name) LIKE lower(?) OR lower(fc.version) LIKE lower(?))" +
          " OR (lower(lic.licenses::TEXT) LIKE lower(?)))";

      params.add("((?<=\\/)(.*" + filterTextQuoted + ".*)(?=\\@))|((?<=@)(.*"
          + filterTextQuoted + "[^=]*)(?=(\\?|&|$)))");
      params.add('%' + filterText + '%');
      params.add('%' + filterText + '%');
      params.add('%' + filterText + '%');

      return query;
    }
    return "";
  }

  private String applyFilterArrayJooq(List<Object> params, String coordinate) {
    if (!coordinate.isEmpty()) {
      String parameter = getCoordinateParameterJooq(coordinate);
      params.add(parameter);
      params.add('%' + coordinate + '%');
      return " (lower(fc.package_url) ~ lower(?) OR lower(lic.licenses::TEXT) LIKE lower(?))";
    }
    return "";
  }

  private String applyFilterTextJooq(String filterText, List<Object> params, String sQuery) {
    if (filterText != null && !filterText.isEmpty()) {
      filterText = filterText.trim();
      if (filterText.contains(" : ") || filterText.contains(" ")) {
        String[] coordinates = Arrays.stream(filterText.split("\\s+:\\s+|\\s+")) //
            .filter(s -> !s.isEmpty()) //
            .map(this::urlEncodeCoordinate)
            .toArray(String[]::new);
        sQuery += " AND (";
        for (int i = 0; i < coordinates.length; i++) {
          sQuery += applyFilterArrayJooq(params, coordinates[i]);
          if (i < coordinates.length - 1) {
            sQuery += " AND";
          }
        }
        sQuery += " ) ";
      }
      else {
        sQuery += applyFilterJooq(filterText, params);
      }
    }
    return sQuery;
  }

  private String getCoordinateParameterJooq(String coordinate) {
    return "(^.*" + coordinate.trim() + ".*)(?=\\@)|((?<=\\/)(.*"
        + coordinate.trim() + ".*)(?=\\@))|((?<=@)(.*"
        + coordinate.trim() + "[^=]*)(?=(\\?|&|$)))";
  }

  private String generateHavingByVulnerabilityThreatLevelsJooq(Set<CvssV3Severity> vulnerabilityThreatLevels) {
    if (CollectionUtils.isNotEmpty(vulnerabilityThreatLevels)) {
      String query = " HAVING ";

      for (CvssV3Severity vulnerabilityThreatLevel : vulnerabilityThreatLevels) {
        switch (vulnerabilityThreatLevel) {
          case NONE:
            query += "COUNT(CASE WHEN (cs.severity = " + NONE.getStartScoreRange() + ") THEN 1 END) > 0 OR ";
            break;
          case LOW:
            query +=
                "COUNT(CASE WHEN (cs.severity BETWEEN " + LOW.getStartScoreRange() + " AND " + LOW.getEndScoreRange() +
                    ") THEN 1 END) > 0 OR ";
            break;
          case MEDIUM:
            query += "COUNT(CASE WHEN (cs.severity BETWEEN " + MEDIUM.getStartScoreRange() + " AND " +
                MEDIUM.getEndScoreRange() + ") THEN 1 END) > 0 OR ";
            break;
          case HIGH:
            query += "COUNT(CASE WHEN (cs.severity BETWEEN " + HIGH.getStartScoreRange() + " AND " +
                HIGH.getEndScoreRange() + ") THEN 1 END) > 0 OR ";
            break;
          case CRITICAL:
            query += "COUNT(CASE WHEN (cs.severity BETWEEN " + CRITICAL.getStartScoreRange() + " AND " +
                CRITICAL.getEndScoreRange() + ") THEN 1 END) > 0 OR ";
            break;
          default:
            query += "";
        }
      }

      return StringUtils.removeEnd(query, "OR ");
    }

    return "";
  }

  private String urlEncodeCoordinate(String coordinate) {
    try {
      return URLEncoder.encode(coordinate, StandardCharsets.UTF_8.toString())
          .replace("%2F", "(/|%2F)")
          .replace(".",
              "\\.");
    }
    catch (UnsupportedEncodingException e) {
      throw new InternalServerException(e);
    }
  }

  private static @NotNull String getCoordinateParameter(int i, String[] coordinates) {
    String parameter;
    if (i == 0 || i == coordinates.length - 1) {
      parameter = "(^.*" + coordinates[i].trim() + ".*)(?=\\@)|((?<=\\/)(.*"
          + coordinates[i].trim() + ".*)(?=\\@))|((?<=@)(.*"
          + coordinates[i].trim() + "[^=]*)(?=(\\?|&|$)))";
    }
    else {
      parameter = "(^.*" + coordinates[i].trim() + ")(?=\\@)|((?<=\\/)(^"
          + coordinates[i].trim() + ")(?=\\@))|((?<=@)(^"
          + coordinates[i].trim() + ")(?=(\\?|&|$)))";
    }
    return parameter;
  }

  private String generateHavingByVulnerabilityThreatLevels(Set<CvssV3Severity> vulnerabilityThreatLevels) {
    if (CollectionUtils.isNotEmpty(vulnerabilityThreatLevels)) {
      String query = " HAVING ";

      for (CvssV3Severity vulnerabilityThreatLevel : vulnerabilityThreatLevels) {
        switch (vulnerabilityThreatLevel) {
          case NONE:
            query += "COUNT(CASE WHEN (cs.severity = ?1) THEN 1 END) > 0 OR ";
            break;
          case LOW:
            query += "COUNT(CASE WHEN (cs.severity BETWEEN ?2 AND ?3) THEN 1 END) > 0 OR ";
            break;
          case MEDIUM:
            query += "COUNT(CASE WHEN (cs.severity BETWEEN ?4 AND ?5) THEN 1 END) > 0 OR ";
            break;
          case HIGH:
            query += "COUNT(CASE WHEN (cs.severity BETWEEN ?6 AND ?7) THEN 1 END) > 0 OR ";
            break;
          case CRITICAL:
            query += "COUNT(CASE WHEN (cs.severity BETWEEN ?8 AND ?9) THEN 1 END) > 0 OR ";
            break;
          default:
            query += "";
        }
      }

      return StringUtils.removeEnd(query, "OR ");
    }

    return "";
  }

  /**
   * Get ThirdPartyFileCoordinate hash or componentRef for a given thirdPartyFileId (a.k.a a single sbom)
   * <p>
   * In theory, there should be only one record for a given hash or componentRef for a given thirdPartyFileId so this
   * should return a single record. But (unfortunately) we already have some customers that has SBOMs (imported via
   * binary scans) that have multiple records for the same hash.
   * </p>
   *
   * @param thirdPartyFileId
   * @param hash
   * @param componentRef
   * @return - list of ThirdPartyFileCoordinate for a given thirdPartyFileId, hash or componentRef
   */
  public List<ThirdPartyFileCoordinate> getByHashOrComponentRefForThirdPartyFileId(
      final String thirdPartyFileId,
      final String hash,
      final String componentRef)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByHashOrComponentRefForThirdPartyFileId(tx, thirdPartyFileId, hash, componentRef);
    }
  }

  /**
   * This method is identical to the one above
   * {@link #getByHashOrComponentRefForThirdPartyFileId(String, String, String)} but it uses a transaction context.
   *
   * @return
   */
  public List<ThirdPartyFileCoordinate> getByHashOrComponentRefForThirdPartyFileId(
      final TransactionContext tx,
      final String thirdPartyFileId,
      final String hash,
      final String componentRef)
  {
    return tx.dsl()
        .selectFrom(FILE_COORDINATE)
        .where(FILE_COORDINATE.THIRD_PARTY_FILE_ID.eq(thirdPartyFileId)
            .and(FILE_COORDINATE.HASH.eq(hash).or(FILE_COORDINATE.COMPONENT_REF.eq(componentRef))))
        .fetchInto(ThirdPartyFileCoordinate.class);
  }

  public List<ThirdPartyFileCoordinate> getByComponentRefsAndThirdPartyFileId(
      final TransactionContext tx,
      final String thirdPartyFileId,
      final List<String> componentRef)
  {
    return tx.dsl()
        .selectFrom(FILE_COORDINATE)
        .where(FILE_COORDINATE.THIRD_PARTY_FILE_ID.eq(thirdPartyFileId)
            .and(FILE_COORDINATE.COMPONENT_REF.in(componentRef)))
        .fetchInto(ThirdPartyFileCoordinate.class);
  }

  @Override
  public org.jooq.Table<?> getJooqTable() {
    return FILE_COORDINATE;
  }

  @Override
  public Class<ThirdPartyFileCoordinate> getEntityClass() {
    return ThirdPartyFileCoordinate.class;
  }
}
