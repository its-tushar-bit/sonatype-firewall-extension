/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractThirdPartyScansSqlDAO;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.model.thirdpartyscans.RecentImportedSbomsDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.RecentVulnerabilitiesDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.VulnerabilitiesThreadLevelMetricDTO;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.CoordinateSecurity.COORDINATE_SECURITY;
import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.FileCoordinate.FILE_COORDINATE;
import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.SbomMetadata.SBOM_METADATA;
import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.VulnerabilityExploitability.VULNERABILITY_EXPLOITABILITY;
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
    return tx.dsl()
        .selectFrom(COORDINATE_SECURITY)
        .where(COORDINATE_SECURITY.FILE_COORDINATE_ID.eq(coordinateFileId)
            .and(DSL.upper(COORDINATE_SECURITY.REF_ID).eq(refId.toUpperCase())))
        .fetchOneInto(ThirdPartyCoordinateSecurity.class);
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
    return getListWithSqlInClause(fileCoordinateIdList, inClauseValuesPartition -> {
      try (TransactionContext tx = createTransactionContext()) {
        return tx.dsl()
            .selectFrom(COORDINATE_SECURITY)
            .where(COORDINATE_SECURITY.FILE_COORDINATE_ID.in(inClauseValuesPartition))
            .fetchInto(ThirdPartyCoordinateSecurity.class);
      }
    });
  }

  public List<ThirdPartyCoordinateSecurity> getByFileCoordinateId(TransactionContext tx, String coordinateFileId) {
    return tx.dsl()
        .selectFrom(COORDINATE_SECURITY)
        .where(COORDINATE_SECURITY.FILE_COORDINATE_ID.eq(coordinateFileId))
        .fetchInto(ThirdPartyCoordinateSecurity.class);
  }

  public void deleteByFileCoordinateId(TransactionContext tx, String fileCoordinateId) {
    List<ThirdPartyCoordinateSecurity> coordinateSecurityFiles = getByFileCoordinateId(tx, fileCoordinateId);
    coordinateSecurityFiles.forEach(entity -> delete(tx, entity));
  }

  public List<ThirdPartyCoordinateSecurity> getByFileCoordinateId(final String fileCoordinateId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByFileCoordinateId(tx, fileCoordinateId);
    }
  }

  @Override
  public void delete(TransactionContext tx, ThirdPartyCoordinateSecurity coordinateSecurity) {
    // cascade delete vulnerability exploitability exchanges records
    thirdPartyVulnerabilityExploitabilityExchangeDAO.deleteByCoordinateSecurityId(tx, coordinateSecurity.getId());

    // delete this entity
    tx.dsl()
        .deleteFrom(COORDINATE_SECURITY)
        .where(COORDINATE_SECURITY.COORDINATE_SECURITY_ID.eq(coordinateSecurity.getId()))
        .execute();

    // handle search index changes
    super.delete(tx, coordinateSecurity);
  }

  public List<RecentVulnerabilitiesDTO> getRecentHighPriorityVulnerabilities(Set<String> applicationIds) {
    try (TransactionContext tx = createTransactionContext()) {
      var cs = COORDINATE_SECURITY.as("cs");
      var sm = SBOM_METADATA.as("sm");
      var fc = FILE_COORDINATE.as("fc");

      var severityStatus = DSL.when(
          cs.SEVERITY.between((double) HIGH.getStartScoreRange(), (double) HIGH.getEndScoreRange()),
          DSL.inline("high"))
          .when(cs.SEVERITY.between((double) CRITICAL.getStartScoreRange(), (double) CRITICAL.getEndScoreRange()),
              DSL.inline("critical"))
          .as("severityStatus");

      var createdAt = DSL.max(sm.CREATED_AT).as("created_at");

      try (var stream = tx.dsl()
          .selectDistinct(cs.REF_ID, cs.SEVERITY, severityStatus, createdAt)
          .from(sm)
          .join(fc)
          .on(fc.THIRD_PARTY_FILE_ID.eq(sm.THIRD_PARTY_FILE_ID))
          .join(cs)
          .on(cs.FILE_COORDINATE_ID.eq(fc.FILE_COORDINATE_ID))
          .where(cs.SEVERITY.ge((double) HIGH.getStartScoreRange())
              .and(sm.APPLICATION_ID.in(applicationIds))
              .and(sm.STATUS.eq("ACTIVE")))
          .groupBy(cs.REF_ID, cs.SEVERITY)
          .orderBy(createdAt.desc(), cs.SEVERITY.desc(), cs.REF_ID.desc())
          .limit(10)
          .fetchStream()
          .map(record -> new RecentVulnerabilitiesDTO(record.intoArray())))
      {
        return stream.collect(Collectors.toList());
      }
    }
  }

  public VulnerabilitiesThreadLevelMetricDTO getVulnerabilitiesByThreatLevel(Set<String> applicationIds) {
    try (TransactionContext tx = createTransactionContext()) {
      var cs = COORDINATE_SECURITY.as("cs");
      var ve = VULNERABILITY_EXPLOITABILITY.as("ve");
      var sm = SBOM_METADATA.as("sm");

      // Count fields for each severity level
      var lowCount = DSL.count(
          DSL.when(cs.SEVERITY.between((double) LOW.getStartScoreRange(), (double) LOW.getEndScoreRange()), 1))
          .as("low");
      var lowAnnotated = DSL.count(
          DSL.when(cs.SEVERITY.between((double) LOW.getStartScoreRange(), (double) LOW.getEndScoreRange())
              .and(ve.VULNERABILITY_EXPLOITABILITY_ID.isNotNull()), 1))
          .as("low_annotated");
      var mediumCount = DSL.count(
          DSL.when(cs.SEVERITY.between((double) MEDIUM.getStartScoreRange(),
              (double) MEDIUM.getEndScoreRange()), 1))
          .as("medium");
      var mediumAnnotated = DSL.count(
          DSL.when(cs.SEVERITY.between((double) MEDIUM.getStartScoreRange(), (double) MEDIUM.getEndScoreRange())
              .and(ve.VULNERABILITY_EXPLOITABILITY_ID.isNotNull()), 1))
          .as("medium_annotated");
      var highCount = DSL.count(
          DSL.when(cs.SEVERITY.between((double) HIGH.getStartScoreRange(), (double) HIGH.getEndScoreRange()), 1))
          .as("high");
      var highAnnotated = DSL.count(
          DSL.when(cs.SEVERITY.between((double) HIGH.getStartScoreRange(), (double) HIGH.getEndScoreRange())
              .and(ve.VULNERABILITY_EXPLOITABILITY_ID.isNotNull()), 1))
          .as("high_annotated");
      var criticalCount = DSL.count(DSL.when(
          cs.SEVERITY.between((double) CRITICAL.getStartScoreRange(), (double) CRITICAL.getEndScoreRange()), 1))
          .as("critical");
      var criticalAnnotated = DSL.count(
          DSL.when(cs.SEVERITY.between((double) CRITICAL.getStartScoreRange(), (double) CRITICAL.getEndScoreRange())
              .and(ve.VULNERABILITY_EXPLOITABILITY_ID.isNotNull()), 1))
          .as("critical_annotated");

      var baseCondition = sm.STATUS.eq("ACTIVE");
      var finalCondition = isNotEmpty(applicationIds)
          ? baseCondition.and(sm.APPLICATION_ID.in(applicationIds))
          : baseCondition;

      Object[] result = tx.dsl()
          .select(lowCount, lowAnnotated, mediumCount, mediumAnnotated,
              highCount, highAnnotated, criticalCount, criticalAnnotated)
          .from(cs)
          .leftJoin(ve)
          .on(cs.COORDINATE_SECURITY_ID.eq(ve.COORDINATE_SECURITY_ID))
          .join(sm)
          .on(cs.SBOM_METADATA_ID.eq(sm.SBOM_METADATA_ID))
          .where(finalCondition)
          .fetchOne()
          .intoArray();
      return new VulnerabilitiesThreadLevelMetricDTO(result);
    }
  }

  public long getSbomReleaseStatusNeedsAttention(Set<String> applicationIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(DSL.countDistinct(SBOM_METADATA.THIRD_PARTY_FILE_ID))
          .from(SBOM_METADATA)
          .join(COORDINATE_SECURITY)
          .on(SBOM_METADATA.SBOM_METADATA_ID.eq(COORDINATE_SECURITY.SBOM_METADATA_ID))
          .leftJoin(VULNERABILITY_EXPLOITABILITY)
          .on(COORDINATE_SECURITY.COORDINATE_SECURITY_ID.eq(VULNERABILITY_EXPLOITABILITY.COORDINATE_SECURITY_ID))
          .where(SBOM_METADATA.APPLICATION_ID.in(applicationIds)
              .and(COORDINATE_SECURITY.SEVERITY.ge((double) HIGH.getStartScoreRange()))
              .and(SBOM_METADATA.STATUS.eq("ACTIVE"))
              .and(VULNERABILITY_EXPLOITABILITY.VULNERABILITY_EXPLOITABILITY_ID.isNull()))
          .fetchOne(0, Long.class);
    }
  }

  public long getSbomReleaseStatusPartiallyReady(Set<String> applicationIds) {
    try (TransactionContext tx = createTransactionContext()) {
      var cs = COORDINATE_SECURITY.as("cs");
      var vex = VULNERABILITY_EXPLOITABILITY.as("vex");

      // EXISTS: has unannotated high-severity vulnerabilities
      var existsUnannotated = DSL.exists(
          DSL.selectOne()
              .from(cs)
              .leftJoin(vex)
              .on(cs.COORDINATE_SECURITY_ID.eq(vex.COORDINATE_SECURITY_ID))
              .where(cs.SEVERITY.ge((double) HIGH.getStartScoreRange())
                  .and(vex.VULNERABILITY_EXPLOITABILITY_ID.isNull())
                  .and(cs.SBOM_METADATA_ID.eq(SBOM_METADATA.SBOM_METADATA_ID))));

      // EXISTS: has annotated high-severity vulnerabilities
      var cs2 = COORDINATE_SECURITY.as("cs2");
      var vex2 = VULNERABILITY_EXPLOITABILITY.as("vex2");
      var existsAnnotated = DSL.exists(
          DSL.selectOne()
              .from(cs2)
              .join(vex2)
              .on(cs2.COORDINATE_SECURITY_ID.eq(vex2.COORDINATE_SECURITY_ID))
              .where(cs2.SEVERITY.ge((double) HIGH.getStartScoreRange())
                  .and(cs2.SBOM_METADATA_ID.eq(SBOM_METADATA.SBOM_METADATA_ID))));

      return tx.dsl()
          .select(DSL.countDistinct(SBOM_METADATA.THIRD_PARTY_FILE_ID))
          .from(SBOM_METADATA)
          .where(SBOM_METADATA.APPLICATION_ID.in(applicationIds)
              .and(SBOM_METADATA.STATUS.eq("ACTIVE"))
              .and(existsUnannotated)
              .and(existsAnnotated))
          .fetchOne(0, Long.class);
    }
  }

  public long getSbomReleaseStatusReleaseReady(Set<String> applicationIds) {
    try (TransactionContext tx = createTransactionContext()) {
      var vuln = COORDINATE_SECURITY.as("vulnerability");
      var vex = VULNERABILITY_EXPLOITABILITY.as("vex");

      // NOT EXISTS: no unannotated high-severity vulnerabilities
      var notExistsUnannotated = DSL.notExists(
          DSL.selectOne()
              .from(vuln)
              .leftJoin(vex)
              .on(vuln.COORDINATE_SECURITY_ID.eq(vex.COORDINATE_SECURITY_ID))
              .where(vuln.SEVERITY.gt((double) HIGH.getStartScoreRange())
                  .and(vex.COORDINATE_SECURITY_ID.isNull())
                  .and(vuln.SBOM_METADATA_ID.eq(SBOM_METADATA.SBOM_METADATA_ID))));

      return tx.dsl()
          .select(DSL.countDistinct(SBOM_METADATA.THIRD_PARTY_FILE_ID))
          .from(SBOM_METADATA)
          .where(SBOM_METADATA.STATUS.eq("ACTIVE")
              .and(SBOM_METADATA.APPLICATION_ID.in(applicationIds))
              .and(notExistsUnannotated))
          .fetchOne(0, Long.class);
    }
  }

  public List<RecentImportedSbomsDTO> getRecentImportedSboms(Set<String> applicationIds) {
    try (TransactionContext tx = createTransactionContext()) {
      var sm = SBOM_METADATA.as("sm");
      var fc = FILE_COORDINATE.as("fc");
      var cs = COORDINATE_SECURITY.as("cs");

      var lowCount = DSL.count(
          DSL.when(cs.SEVERITY.between((double) LOW.getStartScoreRange(), (double) LOW.getEndScoreRange()), 1))
          .as("low");
      var mediumCount = DSL.count(
          DSL.when(cs.SEVERITY.between((double) MEDIUM.getStartScoreRange(),
              (double) MEDIUM.getEndScoreRange()), 1))
          .as("medium");
      var highCount = DSL.count(
          DSL.when(cs.SEVERITY.between((double) HIGH.getStartScoreRange(), (double) HIGH.getEndScoreRange()), 1))
          .as("high");
      var criticalCount = DSL.count(DSL.when(
          cs.SEVERITY.between((double) CRITICAL.getStartScoreRange(), (double) CRITICAL.getEndScoreRange()), 1))
          .as("critical");

      try (var stream = tx.dsl()
          .select(sm.APPLICATION_ID, sm.SBOM_VERSION, sm.SPEC, sm.CREATED_AT,
              lowCount, mediumCount, highCount, criticalCount)
          .from(sm)
          .join(fc)
          .on(fc.THIRD_PARTY_FILE_ID.eq(sm.THIRD_PARTY_FILE_ID))
          .leftJoin(cs)
          .on(cs.FILE_COORDINATE_ID.eq(fc.FILE_COORDINATE_ID))
          .where(sm.STATUS.eq("ACTIVE")
              .and(sm.APPLICATION_ID.in(applicationIds)))
          .groupBy(sm.APPLICATION_ID, sm.SBOM_VERSION, sm.SPEC, sm.CREATED_AT)
          .orderBy(sm.CREATED_AT.desc())
          .limit(7)
          .fetchStream()
          .map(record -> new RecentImportedSbomsDTO(record.intoArray())))
      {
        return stream.collect(Collectors.toList());
      }
    }
  }

  @Override
  public org.jooq.Table<?> getJooqTable() {
    return COORDINATE_SECURITY;
  }

  @Override
  public Class<ThirdPartyCoordinateSecurity> getEntityClass() {
    return ThirdPartyCoordinateSecurity.class;
  }
}
