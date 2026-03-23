/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Application.APPLICATION;
import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.CoordinateSecurity.COORDINATE_SECURITY;
import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.FileCoordinate.FILE_COORDINATE;
import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.SbomMetadata.SBOM_METADATA;
import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.ThirdPartyScan.THIRD_PARTY_SCAN;
import static com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.VulnerabilityExploitability.VULNERABILITY_EXPLOITABILITY;
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

  public ThirdPartySbomMetadata getByIdForUpdate(TransactionContext tx, String id) {
    // Note: We removed FOR UPDATE clause as it causes lock timeout issues in H2 during tests.
    // The status check in the calling code provides protection against invalid state transitions,
    // and the transaction context ensures atomicity of the read-check-update pattern.
    var sbomMetadata = tx.dsl()
        .selectFrom(SBOM_METADATA)
        .where(SBOM_METADATA.SBOM_METADATA_ID.eq(id))
        .fetchOneInto(ThirdPartySbomMetadata.class);
    if (sbomMetadata == null) {
      throw new NotFoundException(getEntityName() + " with ID " + id + " does not exist.");
    }
    return sbomMetadata;
  }

  public List<ThirdPartySbomMetadata> getByThirdPartyFileIds(List<String> thirdPartyFileIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SBOM_METADATA)
          .where(SBOM_METADATA.THIRD_PARTY_FILE_ID.in(thirdPartyFileIds))
          .fetchInto(ThirdPartySbomMetadata.class);
    }
  }

  public ThirdPartySbomMetadata getByThirdPartyFileId(String thirdPartyFileId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SBOM_METADATA)
          .where(SBOM_METADATA.THIRD_PARTY_FILE_ID.eq(thirdPartyFileId))
          .fetchOneInto(ThirdPartySbomMetadata.class);
    }
  }

  public void deleteByThirdPartyFileId(TransactionContext tx, String thirdPartyFileId) {
    List<ThirdPartySbomMetadata> sbomMetadataList = tx.dsl()
        .selectFrom(SBOM_METADATA)
        .where(SBOM_METADATA.THIRD_PARTY_FILE_ID.eq(thirdPartyFileId))
        .fetchInto(ThirdPartySbomMetadata.class);
    sbomMetadataList.forEach(sbomMetadata -> delete(tx, sbomMetadata));
  }

  public List<ThirdPartySbomMetadata> getByApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, applicationId);
    }
  }

  public List<ThirdPartySbomMetadata> getByApplicationId(TransactionContext tx, String applicationId) {
    return tx.dsl()
        .selectFrom(SBOM_METADATA)
        .where(SBOM_METADATA.APPLICATION_ID.eq(applicationId))
        .fetchInto(ThirdPartySbomMetadata.class);
  }

  public List<ThirdPartySbomMetadata> getActiveByApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SBOM_METADATA)
          .where(SBOM_METADATA.APPLICATION_ID.eq(applicationId)
              .and(SBOM_METADATA.STATUS.eq(ACTIVE.name())))
          .fetchInto(ThirdPartySbomMetadata.class);
    }
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
    int offset = (page - 1) * pageSize;
    return tx.dsl()
        .selectFrom(SBOM_METADATA)
        .where(SBOM_METADATA.APPLICATION_ID.eq(applicationId)
            .and(SBOM_METADATA.STATUS.eq(ACTIVE.name())))
        .orderBy(SBOM_METADATA.CREATED_AT, SBOM_METADATA.SBOM_METADATA_ID)
        .offset(offset)
        .limit(pageSize)
        .fetchInto(ThirdPartySbomMetadata.class);
  }

  public ThirdPartySbomMetadata getLatestActiveByApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SBOM_METADATA)
          .where(SBOM_METADATA.APPLICATION_ID.eq(applicationId)
              .and(SBOM_METADATA.STATUS.eq(ACTIVE.name())))
          .orderBy(SBOM_METADATA.CREATED_AT.desc())
          .limit(1)
          .fetchOneInto(ThirdPartySbomMetadata.class);
    }
  }

  public ThirdPartySbomMetadata getByApplicationIdAndSbomVersion(String applicationId, String sbomVersion) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SBOM_METADATA)
          .where(SBOM_METADATA.APPLICATION_ID.eq(applicationId)
              .and(SBOM_METADATA.SBOM_VERSION.eq(sbomVersion)))
          .fetchOneInto(ThirdPartySbomMetadata.class);
    }
  }

  public ThirdPartySbomMetadata getByScanId(String scanId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(SBOM_METADATA.fields())
          .from(SBOM_METADATA)
          .join(THIRD_PARTY_SCAN)
          .on(SBOM_METADATA.THIRD_PARTY_FILE_ID.eq(THIRD_PARTY_SCAN.THIRD_PARTY_FILE_ID))
          .where(THIRD_PARTY_SCAN.SCAN_ID.eq(scanId))
          .fetchOneInto(ThirdPartySbomMetadata.class);
    }
  }

  public ThirdPartySbomMetadata getByApplicationIdAndSbomVersionAndStatus(
      String applicationId,
      String sbomVersion,
      ThirdPartySbomMetadataStatus status)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SBOM_METADATA)
          .where(SBOM_METADATA.APPLICATION_ID.eq(applicationId)
              .and(SBOM_METADATA.SBOM_VERSION.eq(sbomVersion))
              .and(SBOM_METADATA.STATUS.eq(status.name())))
          .fetchOneInto(ThirdPartySbomMetadata.class);
    }
  }

  public long getActiveSbomCount(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectCount()
          .from(SBOM_METADATA)
          .where(SBOM_METADATA.APPLICATION_ID.eq(applicationId)
              .and(SBOM_METADATA.STATUS.eq(ACTIVE.name())))
          .fetchOne(0, Long.class);
    }
  }

  public long getSbomCount() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectCount()
          .from(SBOM_METADATA)
          .fetchOne(0, Long.class);
    }
  }

  public long getActiveSbomCount() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectCount()
          .from(SBOM_METADATA)
          .where(SBOM_METADATA.STATUS.eq(ACTIVE.name()))
          .fetchOne(0, Long.class);
    }
  }

  public List<ThirdPartySbomMetadata> getInactiveSbomsBeforeOrAt(Date date) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SBOM_METADATA)
          .where(SBOM_METADATA.STATUS.ne(ACTIVE.name())
              .and(SBOM_METADATA.CREATED_AT.le(date)))
          .fetchInto(ThirdPartySbomMetadata.class);
    }
  }

  public boolean hasSbomMetadata(String scanId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectCount()
          .from(SBOM_METADATA)
          .join(THIRD_PARTY_SCAN)
          .on(SBOM_METADATA.THIRD_PARTY_FILE_ID.eq(THIRD_PARTY_SCAN.THIRD_PARTY_FILE_ID))
          .where(THIRD_PARTY_SCAN.SCAN_ID.eq(scanId))
          .fetchOne(0, Long.class) != 0;
    }
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
    try (TransactionContext tx = createTransactionContext()) {
      var sm = SBOM_METADATA.as("sm");
      var fc = FILE_COORDINATE.as("fc");
      var cs = COORDINATE_SECURITY.as("cs");
      var ve = VULNERABILITY_EXPLOITABILITY.as("ve");

      LocalDate now = LocalDate.now();
      var lastWeek = Date.from(now.minusWeeks(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
      var lastMonth = Date.from(now.minusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
      var lastYear = Date.from(now.minusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

      var total = DSL.countDistinct(sm.APPLICATION_ID).as("total");
      var lastYearCount = DSL.countDistinct(
          DSL.when(
              sm.CREATED_AT.ge(lastYear)
                  .or(ve.CREATED_AT.ge(lastYear))
                  .or(ve.UPDATED_AT.ge(lastYear)),
              sm.APPLICATION_ID))
          .as("last_year");
      var lastMonthCount = DSL.countDistinct(
          DSL.when(
              sm.CREATED_AT.ge(lastMonth)
                  .or(ve.CREATED_AT.ge(lastMonth))
                  .or(ve.UPDATED_AT.ge(lastMonth)),
              sm.APPLICATION_ID))
          .as("last_month");
      var lastWeekCount = DSL.countDistinct(
          DSL.when(
              sm.CREATED_AT.ge(lastWeek)
                  .or(ve.CREATED_AT.ge(lastWeek))
                  .or(ve.UPDATED_AT.ge(lastWeek)),
              sm.APPLICATION_ID))
          .as("last_week");

      // Rewrite RIGHT JOIN as LEFT JOINs starting from sbom_metadata
      Object[] result = tx.dsl()
          .select(total, lastYearCount, lastMonthCount, lastWeekCount)
          .from(sm)
          .leftJoin(fc)
          .on(sm.THIRD_PARTY_FILE_ID.eq(fc.THIRD_PARTY_FILE_ID))
          .leftJoin(cs)
          .on(cs.FILE_COORDINATE_ID.eq(fc.FILE_COORDINATE_ID))
          .leftJoin(ve)
          .on(cs.COORDINATE_SECURITY_ID.eq(ve.COORDINATE_SECURITY_ID))
          .where(sm.STATUS.eq(ACTIVE.name()))
          .fetchOne()
          .intoArray();
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

  public List<ThirdPartySbomMetadata> getByApplicationIdAndStatus(
      final TransactionContext tx,
      final String applicationId,
      final ThirdPartySbomMetadataStatus status,
      final int page,
      final int pageSize)
  {
    int offset = (page - 1) * pageSize;
    return tx.dsl()
        .selectFrom(SBOM_METADATA)
        .where(SBOM_METADATA.APPLICATION_ID.eq(applicationId)
            .and(SBOM_METADATA.STATUS.eq(status.name())))
        .orderBy(SBOM_METADATA.CREATED_AT.desc())
        .offset(offset)
        .limit(pageSize)
        .fetchInto(ThirdPartySbomMetadata.class);
  }

  public SbomApplicationListSummaryDTO getSbomApplicationsWithRecentlyImportedSbomVersion(
      Set<String> applicationIds,
      SbomApplicationsSortableField sortBy,
      boolean asc,
      int page,
      int pageSize)
  {
    String tpsSchema = getDatabaseSchema();
    String odsSchema = operationalDataStore.getDatabaseSchema();

    String sQuery =
        "SELECT sm.application_id as applicationInternalId," +
            "  sm.sbom_version as sbomVersion," +
            "  sm.created_at as importDate," +
            "  app.public_id as applicationPublicId," +
            "  app.name as applicationName," +
            "  COUNT(CASE WHEN (cs.severity = ?) THEN 1 END) AS vulnerabilityNone," +
            "  COUNT(CASE WHEN (cs.severity BETWEEN ? AND ?) THEN 1 END) AS vulnerabilityLow," +
            "  COUNT(CASE WHEN (cs.severity BETWEEN ? AND ?) THEN 1 END) AS vulnerabilityMedium," +
            "  COUNT(CASE WHEN (cs.severity BETWEEN ? AND ?) THEN 1 END) AS vulnerabilityHigh," +
            "  COUNT(CASE WHEN (cs.severity BETWEEN ? AND ?) THEN 1 END) AS vulnerabilityCritical," +
            "  COUNT(*) OVER() AS full_count," +
            "  COALESCE(ROUND(" +
            "    COUNT(CASE WHEN (vex.coordinate_security_id IS NOT NULL AND cs.severity >= ?) THEN 1 END)" +
            "    * 100 / NULLIF(COUNT(CASE WHEN (cs.coordinate_security_id IS NOT NULL AND cs.severity >= ?) THEN 1 END)"
            +
            "    ::decimal, 0), 1), 100) AS releaseStatusPercentage" +
            " FROM " + tpsSchema + ".sbom_metadata sm" +
            " JOIN (" +
            "   SELECT application_id, max(created_at) as created_at" +
            "   FROM " + tpsSchema + ".sbom_metadata" +
            "   WHERE status = ?" +
            "   GROUP BY application_id) sm2" +
            " ON sm.application_id = sm2.application_id" +
            " AND sm.created_at = sm2.created_at" +
            " JOIN " + odsSchema + ".application app" +
            " ON app.application_id = sm.application_id" +
            " LEFT JOIN " + tpsSchema + ".coordinate_security cs" +
            " ON cs.sbom_metadata_id = sm.sbom_metadata_id" +
            " LEFT JOIN " + tpsSchema + ".vulnerability_exploitability vex" +
            " ON cs.coordinate_security_id = vex.coordinate_security_id" +
            " WHERE sm.status = ?";

    if (isNotEmpty(applicationIds)) {
      sQuery += " AND sm.application_id = ANY(?)";
    }

    sQuery += " GROUP BY sm.application_id, sm.sbom_version, sm.created_at, app.public_id, app.name" +
        generateOrderBySortFieldSelected(sortBy, asc) +
        " OFFSET ? LIMIT ?";

    int offset = (page - 1) * pageSize;

    try (TransactionContext tx = createTransactionContext()) {
      List<Object> params = new ArrayList<>();
      params.add((double) NONE.getStartScoreRange());
      params.add((double) LOW.getStartScoreRange());
      params.add((double) LOW.getEndScoreRange());
      params.add((double) MEDIUM.getStartScoreRange());
      params.add((double) MEDIUM.getEndScoreRange());
      params.add((double) HIGH.getStartScoreRange());
      params.add((double) HIGH.getEndScoreRange());
      params.add((double) CRITICAL.getStartScoreRange());
      params.add((double) CRITICAL.getEndScoreRange());
      params.add((double) HIGH.getStartScoreRange()); // releaseStatusPercentage numerator
      params.add((double) HIGH.getStartScoreRange()); // releaseStatusPercentage denominator
      params.add(ACTIVE.name()); // sm2 subquery status
      params.add(ACTIVE.name()); // WHERE sm.status

      if (isNotEmpty(applicationIds)) {
        params.add(applicationIds.toArray(new String[0]));
      }

      params.add(offset);
      params.add(pageSize);

      SbomApplicationListSummaryDTO result = new SbomApplicationListSummaryDTO();

      List<SbomApplicationSummaryDTO> applicationPageApplicationSummaryDTOList =
          tx.dsl()
              .resultQuery(sQuery, params.toArray())
              .fetchStream()
              .peek(record -> {
                if (result.getTotalCount() == 0) {
                  result.setTotalCount(record.get("full_count", Long.class).intValue());
                }
              })
              .map(record -> new SbomApplicationSummaryDTO(record.intoArray()))
              .collect(Collectors.toList());

      List<String> applicationIdsForPolicyViolation = applicationPageApplicationSummaryDTOList.stream()
          .map(SbomApplicationSummaryDTO::getApplicationInternalId)
          .collect(Collectors.toList());

      Map<String, SbomPolicyViolationSummaryDTO> policyViolationSummaryMap =
          policyViolationDAO.getSbomPolicyViolationSummaryForAnApplication(applicationIdsForPolicyViolation);

      for (SbomApplicationSummaryDTO applicationSummary : applicationPageApplicationSummaryDTOList) {
        applicationSummary.setPolicyViolationSummary(
            policyViolationSummaryMap.get(applicationSummary.getApplicationInternalId()));
      }

      result.setApplications(applicationPageApplicationSummaryDTOList);
      return result;
    }
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
        query.append(" ORDER BY vulnerabilityCritical ")
            .append(order)
            .append(" , vulnerabilityHigh ")
            .append(order)
            .append(" , vulnerabilityMedium ")
            .append(order)
            .append(" , vulnerabilityLow ")
            .append(order);
        break;
      default:
        break;
    }
    return query.toString();
  }

  @SuppressWarnings({"unchecked", "PMD.UnusedFormalParameter"}) // vulnerabilityNone reserved for future use
  private List<org.jooq.OrderField<?>> buildOrderByFields(
      SbomApplicationsSortableField sortBy,
      boolean asc,
      com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.SbomMetadata sm,
      org.jooq.Field<?> vulnerabilityNone,
      org.jooq.Field<?> vulnerabilityLow,
      org.jooq.Field<?> vulnerabilityMedium,
      org.jooq.Field<?> vulnerabilityHigh,
      org.jooq.Field<?> vulnerabilityCritical,
      org.jooq.Field<?> releaseStatusPercentage,
      org.jooq.Table<?> app)
  {
    List<org.jooq.OrderField<?>> orderFields = new ArrayList<>();
    if (sortBy == null) {
      return orderFields;
    }
    switch (sortBy) {
      case IMPORT_DATE:
        orderFields.add(asc ? sm.CREATED_AT.asc() : sm.CREATED_AT.desc());
        break;
      case RELEASE_STATUS_PERCENTAGE:
        orderFields.add(asc ? releaseStatusPercentage.asc() : releaseStatusPercentage.desc());
        break;
      case LATEST_SBOM_VERSION:
        orderFields.add(asc ? sm.SBOM_VERSION.asc() : sm.SBOM_VERSION.desc());
        break;
      case APPLICATION_NAME:
        orderFields.add(asc ? app.field(APPLICATION.NAME).asc() : app.field(APPLICATION.NAME).desc());
        break;
      case VULNERABILITY:
        orderFields.add(asc ? vulnerabilityCritical.asc() : vulnerabilityCritical.desc());
        orderFields.add(asc ? vulnerabilityHigh.asc() : vulnerabilityHigh.desc());
        orderFields.add(asc ? vulnerabilityMedium.asc() : vulnerabilityMedium.desc());
        orderFields.add(asc ? vulnerabilityLow.asc() : vulnerabilityLow.desc());
        break;
      default:
        break;
    }
    return orderFields;
  }

  public ThirdPartySbomMetadataSummaryListDTO getSbomApplicationVulnerabilities(
      String applicationId,
      int pageSize,
      int page,
      SbomVersionsApplicationSortableField sortBy,
      boolean asc)
  {
    try (TransactionContext tx = createTransactionContext()) {
      var sm = SBOM_METADATA.as("sm");
      var cs = COORDINATE_SECURITY.as("cs");
      var vex = VULNERABILITY_EXPLOITABILITY.as("vex");

      // Vulnerability count fields
      var vulnerabilityNone = DSL.count(
          DSL.when(cs.SEVERITY.eq((double) NONE.getStartScoreRange()), 1)).as("vulnerabilityNone");
      var vulnerabilityLow = DSL.count(
          DSL.when(cs.SEVERITY.between((double) LOW.getStartScoreRange(), (double) LOW.getEndScoreRange()), 1))
          .as("vulnerabilityLow");
      var vulnerabilityMedium = DSL.count(DSL.when(
          cs.SEVERITY.between((double) MEDIUM.getStartScoreRange(), (double) MEDIUM.getEndScoreRange()), 1))
          .as("vulnerabilityMedium");
      var vulnerabilityHigh = DSL.count(
          DSL.when(cs.SEVERITY.between((double) HIGH.getStartScoreRange(), (double) HIGH.getEndScoreRange()), 1))
          .as("vulnerabilityHigh");
      var vulnerabilityCritical = DSL.count(
          DSL.when(cs.SEVERITY.between(
              (double) CRITICAL.getStartScoreRange(), (double) CRITICAL.getEndScoreRange()), 1))
          .as("vulnerabilityCritical");

      // Window function for total count
      var fullCount = DSL.count().over().as("full_count");

      // Release status percentage calculation
      var annotatedCount = DSL.count(
          DSL.when(vex.COORDINATE_SECURITY_ID.isNotNull()
              .and(cs.SEVERITY.ge((double) HIGH.getStartScoreRange())), 1));
      var totalHighSeverity = DSL.count(
          DSL.when(cs.COORDINATE_SECURITY_ID.isNotNull()
              .and(cs.SEVERITY.ge((double) HIGH.getStartScoreRange())), 1));
      // Cast numerator to BigDecimal before multiplication to ensure floating-point division
      var releaseStatusPercentage = DSL.coalesce(
          DSL.round(
              annotatedCount.cast(java.math.BigDecimal.class)
                  .mul(100)
                  .div(
                      DSL.nullif(totalHighSeverity.cast(java.math.BigDecimal.class), java.math.BigDecimal.ZERO)),
              1),
          DSL.inline(java.math.BigDecimal.valueOf(100))).as("releaseStatusPercentage");

      // Build ORDER BY fields
      var orderFields = buildSbomVersionOrderByFields(sortBy, asc, sm, vulnerabilityLow,
          vulnerabilityMedium, vulnerabilityHigh, vulnerabilityCritical, releaseStatusPercentage);

      int offset = (page - 1) * pageSize;

      ThirdPartySbomMetadataSummaryListDTO result = new ThirdPartySbomMetadataSummaryListDTO();

      result.setResults(
          tx.dsl()
              .select(sm.SBOM_VERSION, sm.SPEC, sm.SPEC_VERSION, sm.CREATED_AT, sm.IS_VALID,
                  vulnerabilityNone, vulnerabilityLow, vulnerabilityMedium,
                  vulnerabilityHigh, vulnerabilityCritical,
                  fullCount, releaseStatusPercentage)
              .from(sm)
              .leftJoin(cs)
              .on(cs.SBOM_METADATA_ID.eq(sm.SBOM_METADATA_ID))
              .leftJoin(vex)
              .on(cs.COORDINATE_SECURITY_ID.eq(vex.COORDINATE_SECURITY_ID))
              .where(sm.APPLICATION_ID.eq(applicationId)
                  .and(sm.STATUS.eq(ACTIVE.name())))
              .groupBy(sm.SBOM_VERSION, sm.SPEC, sm.SPEC_VERSION, sm.CREATED_AT, sm.IS_VALID)
              .orderBy(orderFields)
              .offset(offset)
              .limit(pageSize)
              .fetchStream()
              .peek(record -> {
                if (result.getTotalResultsCount() == 0) {
                  result.setTotalResultsCount(record.get("full_count", Long.class).intValue());
                }
              })
              .map(record -> new ThirdPartySbomMetadataSummaryDTO(record.intoArray()))
              .collect(Collectors.toList()));
      return result;
    }
  }

  private List<org.jooq.OrderField<?>> buildSbomVersionOrderByFields(
      SbomVersionsApplicationSortableField sortBy,
      boolean asc,
      com.sonatype.insight.brain.jooq.generated.thirdpartyscans.tables.SbomMetadata sm,
      org.jooq.Field<?> vulnerabilityLow,
      org.jooq.Field<?> vulnerabilityMedium,
      org.jooq.Field<?> vulnerabilityHigh,
      org.jooq.Field<?> vulnerabilityCritical,
      org.jooq.Field<?> releaseStatusPercentage)
  {
    List<org.jooq.OrderField<?>> orderFields = new ArrayList<>();
    switch (sortBy) {
      case IMPORT_DATE:
        orderFields.add(asc ? sm.CREATED_AT.asc() : sm.CREATED_AT.desc());
        break;
      case RELEASE_STATUS:
        orderFields.add(asc ? releaseStatusPercentage.asc() : releaseStatusPercentage.desc());
        break;
      case VULNERABILITY:
        orderFields.add(asc ? vulnerabilityCritical.asc() : vulnerabilityCritical.desc());
        orderFields.add(asc ? vulnerabilityHigh.asc() : vulnerabilityHigh.desc());
        orderFields.add(asc ? vulnerabilityMedium.asc() : vulnerabilityMedium.desc());
        orderFields.add(asc ? vulnerabilityLow.asc() : vulnerabilityLow.desc());
        break;
      default:
        break;
    }
    return orderFields;
  }

  public void makeSbomActiveIfExist(String scanId) {
    ThirdPartySbomMetadata sbomMetadata = getByScanId(scanId);
    if (sbomMetadata != null) {
      sbomMetadata.setStatus(ACTIVE);
      update(sbomMetadata);
    }
  }

  @Override
  public org.jooq.Table<?> getJooqTable() {
    return SBOM_METADATA;
  }

  @Override
  public Class<ThirdPartySbomMetadata> getEntityClass() {
    return ThirdPartySbomMetadata.class;
  }
}
