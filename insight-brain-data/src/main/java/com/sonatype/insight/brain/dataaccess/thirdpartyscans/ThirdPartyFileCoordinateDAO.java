/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

import jakarta.persistence.NoResultException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;

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
  public void insert(TransactionContext tx, ThirdPartyFileCoordinate entity) {
    if (StringUtils.isBlank(entity.getDisplayName())) {
      entity.setDisplayName(
          FileCoordinateDisplayNameGenerator.generateDisplayName(entity.getPackageUrl(), entity.getFormat(),
              entity.getName(), entity.getVersion()));
    }
    super.insert(tx, entity);
  }

  @Override
  public ThirdPartyFileCoordinate getById(String id) {
    String sQuery = "SELECT entity FROM ThirdPartyFileCoordinate entity" + //
        " WHERE entity.id=?1";
    return get(sQuery, id);
  }

  public ThirdPartyFileCoordinate getByComponentRef(String componentRef, String thirdPartyFileId) {
    String sQuery = "SELECT entity FROM ThirdPartyFileCoordinate entity" + //
        " WHERE entity.componentRef=?1 AND entity.thirdPartyFileId=?2";
    return get(sQuery, componentRef, thirdPartyFileId);
  }

  public List<ThirdPartyFileCoordinate> getByThirdPartyFileId(String thirdPartyFileId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByThirdPartyFileId(tx, thirdPartyFileId);
    }
  }

  public List<ThirdPartyFileCoordinate> getByPackageUrlAndScanId(String purl, String scanId) {
    String sQuery = "SELECT TPF FROM ThirdPartyScan TPS," + //
        " ThirdPartyFileCoordinate TPF" + //
        " WHERE TPS.thirdPartyFileId=TPF.thirdPartyFileId AND TPF.packageUrl=?1 AND TPS.scanId=?2";
    return getList(sQuery, purl, scanId);
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
   *             does not guarantee unique records for new scans.
   *             keeping only for historical sbom records (for backward compatibility)
   *
   * @param purl
   * @param hash
   * @param scanId
   * @return
   */
  @Deprecated
  public ThirdPartyFileCoordinate getByPackageUrlAndHashAndScanId(String purl, String hash, String scanId) {
    String sQuery = "SELECT TPF FROM ThirdPartyScan TPS," + //
        " ThirdPartyFileCoordinate TPF" + //
        " WHERE TPS.thirdPartyFileId=TPF.thirdPartyFileId AND TPF.packageUrl=?1 AND TPF.hash=?2 AND TPS.scanId=?3";
    return get(sQuery, purl, hash, scanId);
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
   *             does not guarantee unique records for new scans.
   *             keeping only for historical sbom records (for backward compatibility)
   *
   * @param format
   * @param name
   * @param version
   * @param scanId
   * @return
   */
  @Deprecated
  public ThirdPartyFileCoordinate getByFormatNameVersionAndScanID(
      String format,
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
    String sQuery = "SELECT entity FROM ThirdPartyFileCoordinate entity, ThirdPartySbomMetadata sbomMetadata" +
        " WHERE sbomMetadata.id = ?1 AND entity.thirdPartyFileId = sbomMetadata.thirdPartyFileId" +
        " AND entity.hash = ?2";

    return get(tx, sQuery, sbomMetadataId, componentHash);
  }

  @SuppressWarnings("unchecked")
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
    Map<Integer, String> dependencyTypesParams = new LinkedHashMap<>();

    String sQuery = "" + //
        "SELECT fc.hash," + //
        "       fc.package_url," + //
        "       fc.name," + //
        "       fc.version," + //
        "       fc.format," + //
        "       fc.display_name," + //
        "       lic.licenses ::TEXT as licenses_json," + //
        "       COUNT(CASE WHEN (cs.severity = ?1) THEN 1 END) AS severity_none," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?2 AND ?3) THEN 1 END) AS severity_low," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?4 AND ?5) THEN 1 END) AS severity_medium," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?6 AND ?7) THEN 1 END) AS severity_high," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?8 AND ?9) THEN 1 END) AS severity_critical, " + //
        "       ROUND(COUNT(ve) * 100.0 / GREATEST(COUNT(cs), 1), 1) as percentage, " + //
        "       COALESCE(ROUND((COUNT(CASE WHEN (ve.coordinate_security_id IS NOT NULL" + //
        "           AND cs.severity >= ?6) THEN 1 END)) * 100 / NULLIF(COUNT(CASE WHEN " + //
        "           (cs.coordinate_security_id IS NOT NULL AND cs.severity >= ?6) THEN 1 END)" + //
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
        " WHERE fc.third_party_file_id = ?10";
    int indexForFilter = 11;
    MutableInt index = new MutableInt(indexForFilter);
    sQuery = applyFilterText(filterText, index, sQuery);
    sQuery += generateHavingByDependencyTypes(dependencyTypes, dependencyTypesParams, index.intValue()) + //
        " GROUP BY fc.hash, fc.package_url, fc.name, fc.version, fc.display_name, fc.format, licenses_json ,fc" +
        ".dependency_type, fc.filenames, fc.file_coordinate_id, fc.match_state_id" + //
        generateHavingByVulnerabilityThreatLevels(vulnerabilityThreatLevels) + //
        generateOrderBySortFieldSelected(sortBy, asc);

    int offset = (page - 1) * pageSize;

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query paginationQuery = createPaginationQueryWithScoreRangeParams(
          thirdPartyFileId, pageSize, sQuery, offset, tx);
      if (filterText != null && !filterText.isEmpty()) {
        filterText = filterText.trim();
        setFilterParameters(filterText, indexForFilter, paginationQuery);
      }
      dependencyTypesParams.forEach(paginationQuery::setParameter);

      SbomComponentListDTO result = new SbomComponentListDTO();

      List<SbomComponentDTO> dtos = ((Stream<Object[]>) paginationQuery.getResultStream())
          .peek(array -> {
            if (result.getTotalResultsCount() == 0) {
              result.setTotalResultsCount(((Long) array[19]).intValue());
            }
          })
          .map(SbomComponentDTO::new)
          .collect(Collectors.toList());

      result.setResults(dtos);
      return result;
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

  public BomPageSbomSummaryDTO getSbomVunerabilitySummaryForComponents(
      String applicationId,
      String version)
  {
    String sQuery = "" + //
        "SELECT COUNT(CASE WHEN (cs.severity = ?1) THEN 1 END) as none," + //
        " COUNT(CASE WHEN (cs.severity BETWEEN ?2 AND ?3) THEN 1 END) as low," + //
        " COUNT(CASE WHEN (cs.severity BETWEEN ?4 AND ?5) THEN 1 END) as medium," + //
        " COUNT(CASE WHEN (cs.severity BETWEEN ?6 AND ?7) THEN 1 END) as high," + //
        " COUNT(CASE WHEN (cs.severity BETWEEN ?8 AND ?9) THEN 1 END) as critical," + //
        " COALESCE(ROUND((COUNT(CASE WHEN (vex.coordinate_security_id IS NOT NULL AND cs.severity >= ?6) THEN 1 END))" +
        "* 100 / NULLIF(COUNT(CASE WHEN (cs.coordinate_security_id IS NOT NULL AND cs.severity >= ?6) THEN 1 END)" +
        "::decimal, 0), 1), 100) as releaseStatusPercentage" + //
        " FROM " + getDatabaseSchema() + ".sbom_metadata sm" + //
        " LEFT JOIN " + getDatabaseSchema() + ".coordinate_security cs" + //
        " ON cs.sbom_metadata_id = sm.sbom_metadata_id" + //
        " LEFT JOIN " + getDatabaseSchema() + ".vulnerability_exploitability vex" + //
        " ON cs.coordinate_security_id = vex.coordinate_security_id" + //
        " WHERE sm.application_id = ?10" + //
        " AND sm.sbom_version = ?11";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = createNativeQuery(tx, sQuery, NONE.getStartScoreRange(),
          LOW.getStartScoreRange(), LOW.getEndScoreRange(), MEDIUM.getStartScoreRange(), MEDIUM.getEndScoreRange(),
          HIGH.getStartScoreRange(), HIGH.getEndScoreRange(), CRITICAL.getStartScoreRange(),
          CRITICAL.getEndScoreRange(), applicationId, version);

      Object[] result = (Object[]) query.getSingleResult();

      return new BomPageSbomSummaryDTO(result);
    }
  }

  public SbomDependencyTypeDTO getSbomDependencyTypeSummaryForComponents(
      String applicationId,
      String version)
  {
    String sQuery = "" + //
        "SELECT COUNT(CASE WHEN (fc.dependency_type = 'D') THEN 1 END) as direct," + //
        "   COUNT(CASE WHEN (fc.dependency_type = 'T') THEN 1 END) as transitive," + //
        "   COUNT(CASE WHEN (fc.dependency_type is null) THEN 1 END) as unknown" + //
        "  FROM " + getDatabaseSchema() + ".sbom_metadata sm" + //
        "  JOIN " + getDatabaseSchema() + ".file_coordinate fc" + //
        "  ON fc.third_party_file_id = sm.third_party_file_id" + //
        "  WHERE sm.application_id = ?1" + //
        "  AND sm.sbom_version = ?2";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = createNativeQuery(tx, sQuery, applicationId, version);

      Object[] result = (Object[]) query.getSingleResult();

      return new SbomDependencyTypeDTO(result);
    }
    catch (NoResultException e) {
      return null;
    }
  }

  public long getNumberOfComponentsForSbom(
      String applicationId,
      String version)
  {
    String sQuery = "" + //
        "SELECT COUNT(fc.file_coordinate_id)" + //
        "  FROM " + getDatabaseSchema() + ".sbom_metadata sm" + //
        "  JOIN " + getDatabaseSchema() + ".file_coordinate fc" + //
        "  ON fc.third_party_file_id = sm.third_party_file_id" + //
        "  WHERE sm.application_id = ?1" + //
        "  AND sm.sbom_version = ?2";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = createNativeQuery(tx, sQuery, applicationId, version);

      long result = (long) query.getSingleResult();

      return result;
    }
  }

  public boolean hasNonNullComponentRefs(String thirdPartyFileId) {
    String sQuery = "SELECT COUNT(fc.component_ref)" + //
        "  FROM " + getDatabaseSchema() + ".file_coordinate fc" + //
        "  WHERE fc.third_party_file_id = ?1" + //
        "  AND fc.component_ref IS NOT NULL";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = createNativeQuery(tx, sQuery, thirdPartyFileId);
      return (long) query.getSingleResult() > 0;
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

  private String generateHavingByDependencyTypes(
      Set<ThirdPartyDependencyType> dependencyTypes,
      Map<Integer, String> params,
      int index)
  {
    if (CollectionUtils.isNotEmpty(dependencyTypes)) {
      String query = " AND (";

      for (ThirdPartyDependencyType dependencyType : dependencyTypes) {
        switch (dependencyType) {
          case DIRECT:
            query += "fc.dependency_type = ?" + index + " OR ";
            params.put(index++, ThirdPartyDependencyType.DIRECT.getValue());
            break;
          case TRANSITIVE:
            query += "fc.dependency_type = ?" + index + " OR ";
            params.put(index++, ThirdPartyDependencyType.TRANSITIVE.getValue());
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

  private String applyFilter(
      String filterText,
      int index)
  {
    if (!filterText.isEmpty()) {
      String query = " AND ((lower(fc.package_url) ~ lower(?" + index++ + "))" + //
          " OR (lower(fc.name) LIKE lower(?" + index + ") OR lower(fc.version) LIKE lower(?" + index + "))" +
          " OR (lower(lic.licenses::TEXT) LIKE lower(?" + index + ")))";

      return query;
    }
    return "";
  }

  private String applyFilterArray(
      String filterText,
      int index)
  {
    if (!filterText.isEmpty()) {
      return " (lower(fc.package_url) ~ lower(?" + index + ") OR lower(lic.licenses::TEXT) LIKE lower(?" + index + "))";
    }
    return "";
  }

  private String applyFilterText(String filterText, MutableInt index, String sQuery) {
    if (filterText != null && !filterText.isEmpty()) {
      if (filterText.contains(" : ") || filterText.contains(" ")) {
        long count = Arrays.stream(filterText.split("\\s+:\\s+|\\s+"))
            .filter(s -> !s.isEmpty())
            .count();
        int i = 0;
        sQuery += " AND (";
        while (i < count) {
          sQuery += applyFilterArray(filterText, index.intValue());
          index.increment();
          i++;
          if (i < count) {
            sQuery += " AND";
          }
        }
        sQuery += " ) ";
      }
      else {
        sQuery += applyFilter(filterText, index.intValue());
      }
    }
    return sQuery;
  }

  private void setFilterParameters(
      String filterText,
      int index,
      jakarta.persistence.Query paginationQuery)
  {
    if (StringUtils.containsAny(filterText, " : ", " ")) {
      String[] coordinates = Arrays.stream(filterText.split("\\s+:\\s+|\\s+")) //
          .filter(coordinate -> !coordinate.isEmpty()) //
          .map(this::urlEncodeCoordinate)
          .toArray(String[]::new);
      for (int i = 0; i < coordinates.length; i++) {
        String parameter = getCoordinateParameter(i, coordinates);
        paginationQuery.setParameter(index, parameter);
        index++;
      }
    }
    else {
      String filterTextQuoted = urlEncodeCoordinate(filterText);

      paginationQuery.setParameter(11, "((?<=\\/)(.*" + filterTextQuoted + ".*)(?=\\@))|((?<=@)(.*"
          + filterTextQuoted + "[^=]*)(?=(\\?|&|$)))");
      paginationQuery.setParameter(12, '%' + filterText + '%');
    }
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
   * should return a single record. But (unfortunately) we already have some customers that has SBOMs
   * (imported via binary scans) that have multiple records for the same hash.
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
    String sQuery = "SELECT entity FROM ThirdPartyFileCoordinate entity" + //
        " WHERE entity.thirdPartyFileId=?1 AND (entity.hash=?2 OR entity.componentRef=?3)";
    return getList(tx, sQuery, thirdPartyFileId, hash, componentRef);
  }

  public List<ThirdPartyFileCoordinate> getByComponentRefsAndThirdPartyFileId(
      final TransactionContext tx,
      final String thirdPartyFileId,
      final List<String> componentRef)
  {
    String sQuery = "SELECT entity FROM ThirdPartyFileCoordinate entity" + //
        " WHERE entity.thirdPartyFileId=?1 AND entity.componentRef IN ?2";
    return getList(tx, sQuery, thirdPartyFileId, componentRef);
  }
}
