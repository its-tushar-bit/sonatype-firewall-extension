/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractThirdPartyScansSqlDAO;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.model.thirdpartyscans.RecentImportedSbomsDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.RecentVulnerabilitiesDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.VulnerabilitiesThreadLevelMetricDTO;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.InternalServerException;

import static com.sonatype.insight.brain.utils.CvssV3Severity.CRITICAL;
import static com.sonatype.insight.brain.utils.CvssV3Severity.HIGH;
import static com.sonatype.insight.brain.utils.CvssV3Severity.LOW;
import static com.sonatype.insight.brain.utils.CvssV3Severity.MEDIUM;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Named
@Singleton
public class ThirdPartyCoordinateSecurityDAO
    extends AbstractThirdPartyScansSqlDAO<ThirdPartyCoordinateSecurity>
{
  private final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO;

  @Inject
  public ThirdPartyCoordinateSecurityDAO(
      final ThirdPartyScansDataStore thirdPartyScansDataStore,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO)
  {
    super(thirdPartyScansDataStore);
    this.thirdPartyVulnerabilityExploitabilityExchangeDAO = thirdPartyVulnerabilityExploitabilityExchangeDAO;
  }

  @Override
  public ThirdPartyCoordinateSecurity getById(String id) {
    String sQuery = "SELECT entity FROM ThirdPartyCoordinateSecurity entity" + //
        " WHERE entity.id=?1";
    return get(sQuery, id);
  }

  public List<ThirdPartyCoordinateSecurity> getAll() {
    return getList("SELECT entity FROM ThirdPartyCoordinateSecurity entity");
  }

  public ThirdPartyCoordinateSecurity getByFileCoordinateIdAndRefId(String coordinateFileId, String refId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByFileCoordinateIdAndRefId(tx, coordinateFileId, refId);
    }
  }

  public ThirdPartyCoordinateSecurity getByFileCoordinateIdAndRefId(
      TransactionContext tx,
      String coordinateFileId,
      String refId)
  {
    String sQuery = "SELECT entity FROM ThirdPartyCoordinateSecurity entity" + //
        " WHERE entity.fileCoordinateId=?1 AND UPPER(entity.refId)=?2";
    return get(tx, sQuery, coordinateFileId, refId.toUpperCase());
  }

  public ThirdPartyCoordinateSecurity insertSafely(
      final TransactionContext tx,
      final ThirdPartyCoordinateSecurity entity)
  {
    ThirdPartyCoordinateSecurity existing =
        getByFileCoordinateIdAndRefId(tx, entity.getFileCoordinateId(), entity.getRefId());
    if (existing != null) {
      return existing;
    }
    insert(tx, entity);
    return entity;
  }

  public List<ThirdPartyCoordinateSecurity> getByFileCoordinateIds(List<String> fileCoordinateIdList) {
    String sQuery = "SELECT entity FROM ThirdPartyCoordinateSecurity entity" + //
        " WHERE entity.fileCoordinateId IN ?1";
    return getListWithSqlInClause(fileCoordinateIdList,
        inClauseValuesPartition -> getList(sQuery, inClauseValuesPartition));
  }

  public List<ThirdPartyCoordinateSecurity> getByFileCoordinateId(TransactionContext tx, String coordinateFileId) {
    String sQuery = "SELECT entity FROM ThirdPartyCoordinateSecurity entity" + //
        " WHERE entity.fileCoordinateId=?1";
    return getList(tx, sQuery, coordinateFileId);
  }

  public void deleteByFileCoordinateId(TransactionContext tx, String fileCoordinateId) {
    List<ThirdPartyCoordinateSecurity> coordinateSecurityFiles = getByFileCoordinateId(tx, fileCoordinateId);
    coordinateSecurityFiles.forEach(entity -> delete(tx, entity));
  }

  public List<ThirdPartyCoordinateSecurity> getByFileCoordinateId(final String fileCoordinateId) {
    String sQuery = "SELECT entity FROM ThirdPartyCoordinateSecurity entity" + //
        " WHERE entity.fileCoordinateId=?1";
    return getList(sQuery, fileCoordinateId);
  }

  @Override
  public void delete(TransactionContext tx, ThirdPartyCoordinateSecurity coordinateSecurity) {
    // cascade delete vulnerability exploitability exchanges records
    thirdPartyVulnerabilityExploitabilityExchangeDAO.deleteByCoordinateSecurityId(tx, coordinateSecurity.getId());

    // lastly delete this entity
    super.delete(tx, coordinateSecurity);
  }

  public List<RecentVulnerabilitiesDTO> getRecentHighPriorityVulnerabilities(Set<String> applicationIds) {
    String sQuery = "" + //
        "SELECT DISTINCT ref_id," + //
        " cs.severity," + //
        " CASE WHEN (cs.severity BETWEEN ?1 AND ?2) THEN 'high'" + //
        " WHEN (cs.severity BETWEEN ?3 AND ?4) THEN 'critical' END as severityStatus," + //
        " MAX(sm.created_at) AS created_at" + // Use MAX since we're interested in the most recent vulnerabilities
        " FROM " + getDatabaseSchema() + ".sbom_metadata sm" + //
        " JOIN " + getDatabaseSchema() + ".file_coordinate fc" + //
        " ON fc.third_party_file_id = sm.third_party_file_id" + //
        " JOIN " + getDatabaseSchema() + ".coordinate_security cs" + //
        " ON cs.file_coordinate_id = fc.file_coordinate_id" + //
        " WHERE cs.severity >= ?1" + //
        " AND sm.application_id = ANY(array[?5])" + //
        " AND sm.status = 'ACTIVE'" + //
        " GROUP BY ref_id, cs.severity" + //
        " ORDER BY created_at desc, cs.severity desc, ref_id desc" + //
        " LIMIT 10";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = createNativeQuery(tx, sQuery, HIGH.getStartScoreRange(),
          HIGH.getEndScoreRange(), CRITICAL.getStartScoreRange(), CRITICAL.getEndScoreRange(),
          createArrayOf(JDBCType.VARCHAR, applicationIds.toArray()));

      List<RecentVulnerabilitiesDTO> dtos = ((Stream<Object[]>) query.getResultStream())
          .map(RecentVulnerabilitiesDTO::new)
          .collect(Collectors.toList());

      return dtos;
    }
    catch (SQLException e) {
      throw new InternalServerException(e);
    }
  }

  public VulnerabilitiesThreadLevelMetricDTO getVulnerabilitiesByThreatLevel(Set<String> applicationIds) {
    String databaseSchema = getDatabaseSchema();
    String sQuery = "" + //
        "SELECT COUNT(CASE WHEN (cs.severity BETWEEN ?1 AND ?2) THEN 1 END) AS low," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?1 AND ?2 " + //
        "                  AND ve.vulnerability_exploitability_id IS NOT NULL)" + //
        "             THEN 1 END) AS low_annotated," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?3 AND ?4) THEN 1 END) AS medium," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?3 AND ?4" + //
        "                  AND ve.vulnerability_exploitability_id IS NOT NULL)" + //
        "             THEN 1 END) AS medium_annotated," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?5 AND ?6) THEN 1 END) AS high," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?5 AND ?6" + //
        "                  AND ve.vulnerability_exploitability_id IS NOT NULL)" + //
        "             THEN 1 END) AS high_annotated," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?7 AND ?8) THEN 1 END) AS critical, " + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?7 AND ?8" + //
        "                  AND ve.vulnerability_exploitability_id IS NOT NULL)" + //
        "             THEN 1 END) AS critical_annotated " + //
        " FROM " + databaseSchema + ".coordinate_security cs" + //
        "   LEFT JOIN " + databaseSchema + ".vulnerability_exploitability ve" + //
        "     ON cs.coordinate_security_id = ve.coordinate_security_id" + //
        "   INNER JOIN " + databaseSchema + ".sbom_metadata sm" + //
        "     ON cs.sbom_metadata_id = sm.sbom_metadata_id" + //
        " WHERE sm.status = ?9";

    if (isNotEmpty(applicationIds)) {
      sQuery += " AND sm.application_id = ANY(array[?10])";
    }

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = createNativeQuery(tx, sQuery, LOW.getStartScoreRange(), LOW.getEndScoreRange(),
          MEDIUM.getStartScoreRange(), MEDIUM.getEndScoreRange(), HIGH.getStartScoreRange(), HIGH.getEndScoreRange(),
          CRITICAL.getStartScoreRange(), CRITICAL.getEndScoreRange(), "ACTIVE");

      if (isNotEmpty(applicationIds)) {
        query.setParameter(10, createArrayOf(JDBCType.VARCHAR, applicationIds.toArray()));
      }

      Object[] result = (Object[]) query.getSingleResult();
      return new VulnerabilitiesThreadLevelMetricDTO(result);
    }
    catch (SQLException e) {
      throw new InternalServerException(e);
    }
  }

  public long getSbomReleaseStatusNeedsAttention(Set<String> applicationIds) {
    String sQuery = "" + //
        "SELECT COUNT(DISTINCT sm.third_party_file_id)" + //
        " FROM " + getDatabaseSchema() + ".sbom_metadata sm" + //
        " JOIN " + getDatabaseSchema() + ".coordinate_security cs" + //
        " ON sm.sbom_metadata_id = cs.sbom_metadata_id" + //
        " LEFT JOIN " + getDatabaseSchema() + ".vulnerability_exploitability vex" + //
        " ON cs.coordinate_security_id = vex.coordinate_security_id" + //
        " WHERE sm.application_id = ANY(array[?2])" + //
        " AND cs.severity >= ?1" + //
        " AND sm.status = 'ACTIVE'" + //
        " AND vex.vulnerability_exploitability_id IS NULL";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = createNativeQuery(tx, sQuery, HIGH.getStartScoreRange(),
          createArrayOf(JDBCType.VARCHAR, applicationIds.toArray()));
      return (long) query.getSingleResult();
    }
    catch (SQLException e) {
      throw new InternalServerException(e);
    }
  }

  public long getSbomReleaseStatusPartiallyReady(Set<String> applicationIds) {
    String sQuery = "" + //
        "SELECT COUNT(DISTINCT sm.third_party_file_id)" + //
        " FROM " + getDatabaseSchema() + ".sbom_metadata sm" + //
        " WHERE sm.application_id = ANY(array[?2])" + //
        " AND sm.status = 'ACTIVE'" + //
        " AND EXISTS (" + //
        "   SELECT cs.sbom_metadata_id" + //
        "   FROM " + getDatabaseSchema() + ".coordinate_security cs" + //
        "   LEFT JOIN " + getDatabaseSchema() + ".vulnerability_exploitability vex" +
        "   ON cs.coordinate_security_id = vex.coordinate_security_id" + //
        "   WHERE cs.severity >= ?1" +
        "   AND vex.vulnerability_exploitability_id IS NULL" + //
        "   AND cs.sbom_metadata_id = sm.sbom_metadata_id" + //
        " )" + //
        " AND EXISTS (" + //
        "    SELECT cs.sbom_metadata_id" + //
        "    FROM " + getDatabaseSchema() + ".coordinate_security cs" + //
        "    JOIN " + getDatabaseSchema() + ".vulnerability_exploitability vex" + //
        "    ON cs.coordinate_security_id = vex.coordinate_security_id" + //
        "    WHERE cs.severity >= ?1" + //
        "    AND cs.sbom_metadata_id = sm.sbom_metadata_id" + //
        " )";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = createNativeQuery(tx, sQuery, HIGH.getStartScoreRange(),
          createArrayOf(JDBCType.VARCHAR, applicationIds.toArray()));
      return (long) query.getSingleResult();
    }
    catch (SQLException e) {
      throw new InternalServerException(e);
    }
  }

  public long getSbomReleaseStatusReleaseReady(Set<String> applicationIds) {
    String sQuery = "" + //
        "SELECT COUNT(DISTINCT sm.third_party_file_id)" + //
        " FROM " + getDatabaseSchema() + ".sbom_metadata sm" + //
        " WHERE sm.status = 'ACTIVE'" + //
        " AND sm.application_id = ANY(array[?2])" + //
        " AND NOT EXISTS (" + //
        "   SELECT vulnerability.sbom_metadata_id" + //
        "   FROM " + getDatabaseSchema() + ".coordinate_security vulnerability " + //
        "   LEFT JOIN " + getDatabaseSchema() + ".vulnerability_exploitability vex " + //
        "   ON vulnerability.coordinate_security_id = vex.coordinate_security_id" + //
        "   WHERE vulnerability.severity > ?1 " + //
        "   AND vex.coordinate_security_id is NULL" + //
        "   AND vulnerability.sbom_metadata_id = sm.sbom_metadata_id" + //
        " )";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = createNativeQuery(tx, sQuery, HIGH.getStartScoreRange(),
          createArrayOf(JDBCType.VARCHAR, applicationIds.toArray()));

      return (long) query.getSingleResult();
    }
    catch (SQLException e) {
      throw new InternalServerException(e);
    }
  }

  public List<RecentImportedSbomsDTO> getRecentImportedSboms(Set<String> applicationIds) {
    String databaseSchema = getDatabaseSchema();
    String sQuery = "" + //
        "SELECT sm.application_id," + //
        "       sm.sbom_version," + //
        "       sm.spec," + //
        "       sm.created_at," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?1 AND ?2) THEN 1 END) as low," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?3 AND ?4) THEN 1 END) as medium," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?5 AND ?6) THEN 1 END) as high," + //
        "       COUNT(CASE WHEN (cs.severity BETWEEN ?7 AND ?8) THEN 1 END) as critical" + //
        " FROM " + databaseSchema + ".sbom_metadata sm" + //
        " JOIN " + databaseSchema + ".file_coordinate fc" + //
        " ON fc.third_party_file_id = sm.third_party_file_id" + //
        " LEFT JOIN " + databaseSchema + ".coordinate_security cs" + //
        " ON cs.file_coordinate_id = fc.file_coordinate_id" + //
        " WHERE sm.status = ?9" + //
        " AND sm.application_id = ANY(array[?10])" + //
        " GROUP BY sm.application_id, sm.sbom_version, sm.spec, sm.created_at" + //
        " ORDER BY sm.created_at DESC LIMIT 7";

    try (TransactionContext tx = createTransactionContext()) {
      jakarta.persistence.Query query = createNativeQuery(tx, sQuery, LOW.getStartScoreRange(),
          LOW.getEndScoreRange(), MEDIUM.getStartScoreRange(), MEDIUM.getEndScoreRange(), HIGH.getStartScoreRange(),
          HIGH.getEndScoreRange(), CRITICAL.getStartScoreRange(), CRITICAL.getEndScoreRange(), "ACTIVE",
          createArrayOf(JDBCType.VARCHAR, applicationIds.toArray()));

      return ((Stream<Object[]>) query.getResultStream())
          .map(RecentImportedSbomsDTO::new)
          .collect(Collectors.toList());
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
