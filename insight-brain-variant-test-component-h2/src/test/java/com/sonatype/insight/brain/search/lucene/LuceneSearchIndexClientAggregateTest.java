/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.RankedGroup;
import com.sonatype.insight.brain.search.index.RankedGroupsResult;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.search.ConversionHelper;

import jakarta.inject.Inject;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for the RBAC-scoped {@link LuceneSearchIndexClient#count(String)} and
 * {@link LuceneSearchIndexClient#aggregateCountByField(String, String, Map)} primitives (CLM-40927).
 */
@ComponentH2Test
public class LuceneSearchIndexClientAggregateTest
    extends AbstractComponentH2Test
{
  @Inject
  private LuceneSearchIndexClient luceneSearchIndexClient;

  @Inject
  private LuceneIndexWriterOwner indexWriterOwner;

  @Inject
  private OwnerDAO ownerDAO;

  @Inject
  private ConversionHelper conversionHelper;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Mock
  private com.sonatype.insight.brain.search.index.VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @BeforeEach
  public void setUpClient() {
    // Mirror LuceneIndexSearchingTest: swap in a mocked shutdown handler and reset the lazily-created
    // indexing executors so repeated full re-indexes within this test class behave deterministically.
    applyBeanFieldOverride(AbstractSearchIndexClient.class, "shutdownHandler", mockShutdownHandler);
    applyBeanFieldOverride(DocumentBuilderHelper.class, "shutdownHandler", mockShutdownHandler);
    resetTenantExecutor(lookup(AbstractSearchIndexClient.class), "indexingExecutors");
    resetTenantExecutor(lookup(DocumentBuilderHelper.class), "evalExecutors");
    resetTenantExecutor(lookup(DocumentBuilderHelper.class), "componentExecutors");

    lenient().when(vulnerabilityDescriptionFetcher.getVulnerabilityDescription(anyString())).thenReturn("");
  }

  private void resetTenantExecutor(Object bean, String fieldName) {
    @SuppressWarnings("unchecked")
    TenantReference<ExecutorService> executors =
        (TenantReference<ExecutorService>) ReflectionTestUtils.getField(bean, fieldName);
    if (executors == null) {
      return;
    }
    ExecutorService oldExecutor = executors.remove();
    if (oldExecutor != null) {
      oldExecutor.shutdownNow();
    }
  }

  private void actAsUser(String username) {
    when(subject.getPrincipal()).thenReturn(new UserPrincipal(username, username, InternalRealm.ID));
  }

  private PolicyEvaluation newAppReport(
      String appId,
      String stageId,
      String scanId,
      String reportResourceName) throws Exception
  {
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(appId, stageId, scanId);
    ReportTestUtils.createReportFile(policyEval.getOwnerId(), policyEval.getScanId(),
        ReportTestUtils.zipReportDir(reportResourceName, tempDir), insightWork);
    return policyEval;
  }

  @Test
  public void testCount_ApplicationItemType_GlobalUserCountsAllReadableApplications() {
    // Default test user (testuser) has GLOBAL read permission, so every APPLICATION doc is counted.
    Organization org = tempEntity.newOrganization();
    tempEntity.newApplication(org.getId());
    tempEntity.newApplication(org.getId());
    tempEntity.newApplication(org.getId());

    luceneSearchIndexClient.populateIndex();

    long count = luceneSearchIndexClient.count("itemType:" + ItemType.APPLICATION.searchFieldName());

    assertThat(count).isEqualTo(3);
  }

  @Test
  public void testCount_FailsClosed_UserWithNoReadContextsCountsZero() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newApplication(org.getId());
    tempEntity.newApplication(org.getId());

    luceneSearchIndexClient.populateIndex();

    // A user with no membership mappings has no readable contexts: the count must fail closed to 0,
    // never leaking an unscoped total even though APPLICATION docs exist in the index.
    actAsUser("user-with-no-permissions");

    long count = luceneSearchIndexClient.count("itemType:" + ItemType.APPLICATION.searchFieldName());

    assertThat(count).isZero();
  }

  @Test
  public void testCount_RbacFilter_MatchesLowercasedIndexedContextIdRegardlessOfCase() {
    // Context ids are indexed via a lowercasing analyzer; the programmatic RBAC TermInSetQuery does NOT
    // analyze its terms, so the implementation must lowercase the allowed context ids. Mixed-case ids
    // here prove the match is case-correct (and that the count is scoped to the single readable app).
    Organization org = tempEntity.newOrganizationWithSpecificId("2FAB4462f587401299AC3728ee21ADDc", "Mixed Case Org");
    Application readableApp = tempEntity.newApplicationWithSpecificId(
        "9CDe1234F567890123ABcdef45678901", "Readable App", "readable-app", org.getId());
    tempEntity.newApplication(org.getId());

    String readerUsername = "scoped-reader";
    tempEntity.newUser(readerUsername);
    Role readRole = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(readableApp.getId(), readRole.getId(), readerUsername);

    luceneSearchIndexClient.populateIndex();

    actAsUser(readerUsername);

    long count = luceneSearchIndexClient.count("itemType:" + ItemType.APPLICATION.searchFieldName());

    assertThat(count).isEqualTo(1);
  }

  @Test
  public void testAggregateCountByField_PolicyViolationThreatLevelBand() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation evaluation =
        newAppReport(app.getId(), Stage.ID_BUILD, "aggPvtReport", "/IndexSearchingTest/policyViolationReport");

    Policy pCritA = tempEntity.newPolicy(org.getId(), "Security - Critical A");
    Policy pCritB = tempEntity.newPolicy(org.getId(), "Security - Critical B");
    Policy pLow = tempEntity.newPolicy(org.getId(), "Quality - Standards");
    Policy pMed = tempEntity.newPolicy(org.getId(), "Legal - Non-Standard");

    tempEntity.newPolicyViolation(evaluation, pCritA, 8, PolicyThreatCategory.SECURITY,
        "com.crit", "critA", "1.0", "hashCritA0000000000");
    tempEntity.newPolicyViolation(evaluation, pCritB, 10, PolicyThreatCategory.SECURITY,
        "com.crit", "critB", "1.0", "hashCritB0000000000");
    tempEntity.newPolicyViolation(evaluation, pLow, 3, PolicyThreatCategory.QUALITY,
        "com.low", "low", "1.0", "hashLow00000000000");
    tempEntity.newPolicyViolation(evaluation, pMed, 6, PolicyThreatCategory.LICENSE,
        "com.med", "med", "1.0", "hashMed00000000000");

    luceneSearchIndexClient.populateIndex();

    MetricAggregationResult result = luceneSearchIndexClient.aggregateCountByField(
        "itemType:" + ItemType.POLICY_VIOLATION.searchFieldName(),
        FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
        Map.of("critical", new int[]{8, 10}));

    assertThat(result.total).isEqualTo(4);
    assertThat(result.buckets).containsEntry("critical", 2L);
  }

  @Test
  public void testCountDistinctGroupedByBands_PerComponentPerSeverityCounts() throws Exception {
    // Two components. compA has a critical (10) + a severe (5) violation; compB has a low (1) violation.
    // Grouped by componentHash, bucketed into the ThreatLevel severity bands, counting distinct
    // policyViolationId, the per-component per-band counts must be: compA {critical:1, severe:1},
    // compB {low:1}. This backs the Components leg per-severity badge (C1).
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation evaluation = newAppReport(app.getId(), Stage.ID_BUILD, "groupedBandsReport",
        "/IndexSearchingTest/policyViolationReport");
    Policy pCrit = tempEntity.newPolicy(org.getId(), "Security - Critical");
    Policy pSev = tempEntity.newPolicy(org.getId(), "Security - Severe");
    Policy pLow = tempEntity.newPolicy(org.getId(), "Quality - Low");
    tempEntity.newPolicyViolation(evaluation, pCrit, 10, PolicyThreatCategory.SECURITY,
        "com.a", "a", "1.0", "hashA00000000000000");
    tempEntity.newPolicyViolation(evaluation, pSev, 5, PolicyThreatCategory.SECURITY,
        "com.a", "a", "1.0", "hashA00000000000000");
    tempEntity.newPolicyViolation(evaluation, pLow, 1, PolicyThreatCategory.QUALITY,
        "com.b", "b", "1.0", "hashB00000000000000");

    luceneSearchIndexClient.populateIndex();

    Map<String, Map<String, Long>> byHash = luceneSearchIndexClient.countDistinctGroupedByBands(
        "itemType:" + ItemType.POLICY_VIOLATION.searchFieldName(),
        FieldIdentifier.COMPONENT_HASH.label,
        FieldIdentifier.POLICY_VIOLATION_ID.label,
        java.util.Set.of("hashA00000000000000", "hashB00000000000000"),
        FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
        ThreatLevel.searchAggregationBands());

    // Result keyed by lowercased group value (keyword lowercase normalizer).
    assertThat(byHash.get("hasha00000000000000")).containsEntry("critical", 1L).containsEntry("severe", 1L);
    assertThat(byHash.get("hasha00000000000000")).doesNotContainKey("low");
    assertThat(byHash.get("hashb00000000000000")).containsEntry("low", 1L);
    assertThat(byHash.get("hashb00000000000000")).doesNotContainKey("critical");
  }

  @Test
  public void testCountDistinctGroupedByBands_FailsClosed_UserWithNoReadContextsGetsEmpty() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation evaluation = newAppReport(app.getId(), Stage.ID_BUILD, "groupedBandsFailClosed",
        "/IndexSearchingTest/policyViolationReport");
    Policy pCrit = tempEntity.newPolicy(org.getId(), "Security - Critical");
    tempEntity.newPolicyViolation(evaluation, pCrit, 10, PolicyThreatCategory.SECURITY,
        "com.a", "a", "1.0", "hashA00000000000000");
    luceneSearchIndexClient.populateIndex();

    actAsUser("user-with-no-permissions");

    Map<String, Map<String, Long>> byHash = luceneSearchIndexClient.countDistinctGroupedByBands(
        "itemType:" + ItemType.POLICY_VIOLATION.searchFieldName(),
        FieldIdentifier.COMPONENT_HASH.label,
        FieldIdentifier.POLICY_VIOLATION_ID.label,
        java.util.Set.of("hashA00000000000000"),
        FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
        ThreatLevel.searchAggregationBands());

    assertThat(byHash).isEmpty();
  }

  @Test
  public void testCountDistinct_VulnerableComponentWithMultipleCves_CountsComponentOnce() throws Exception {
    // A vulnerable component is indexed as one SECURITY_VULNERABILITY doc per CVE.
    // The componentsMetricReport has 3 vulnerable components (compA has 2 CVEs => 4 SV docs total). A naive
    // count() over-counts (4); countDistinct on (applicationId, componentHash) must count each component once (3).
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    newAppReport(app.getId(), Stage.ID_BUILD, "componentsDistinctReport",
        "/IndexSearchingTest/componentsMetricReport");

    luceneSearchIndexClient.populateIndex();

    long rawCount = luceneSearchIndexClient.count("itemType:" + ItemType.SECURITY_VULNERABILITY.name());
    assertThat(rawCount).isEqualTo(4);

    long distinct = luceneSearchIndexClient.countDistinct(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        List.of(FieldIdentifier.APPLICATION_ID.label, FieldIdentifier.COMPONENT_HASH.label));

    assertThat(distinct).isEqualTo(3);
  }

  @Test
  public void testCountDistinct_CompositeKeyIncludesApplicationId() throws Exception {
    // The same component hashes appear in two applications. The composite key (applicationId, componentHash)
    // keeps the per-application components distinct: 3 components x 2 apps = 6, even though only 3 hashes exist.
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication(org.getId());
    Application app2 = tempEntity.newApplication(org.getId());
    newAppReport(app1.getId(), Stage.ID_BUILD, "compDistinctApp1", "/IndexSearchingTest/componentsMetricReport");
    newAppReport(app2.getId(), Stage.ID_BUILD, "compDistinctApp2", "/IndexSearchingTest/componentsMetricReport");

    luceneSearchIndexClient.populateIndex();

    long distinct = luceneSearchIndexClient.countDistinct(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        List.of(FieldIdentifier.APPLICATION_ID.label, FieldIdentifier.COMPONENT_HASH.label));

    assertThat(distinct).isEqualTo(6);
  }

  @Test
  public void testCountDistinct_FailsClosed_UserWithNoReadContextsCountsZero() throws Exception {
    // A user with no readable contexts must get 0 distinct components, never an unscoped distinct count.
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    newAppReport(app.getId(), Stage.ID_BUILD, "componentsDistinctFailClosed",
        "/IndexSearchingTest/componentsMetricReport");

    luceneSearchIndexClient.populateIndex();

    actAsUser("user-with-no-permissions");

    long distinct = luceneSearchIndexClient.countDistinct(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        List.of(FieldIdentifier.APPLICATION_ID.label, FieldIdentifier.COMPONENT_HASH.label));

    assertThat(distinct).isZero();
  }

  @Test
  public void testAggregateCountByField_PolicyViolationAllThreatLevelBandsInSingleReader() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation evaluation =
        newAppReport(app.getId(), Stage.ID_BUILD, "aggAllBandsReport", "/IndexSearchingTest/policyViolationReport");

    Policy pCrit = tempEntity.newPolicy(org.getId(), "Security - Critical");
    Policy pSev = tempEntity.newPolicy(org.getId(), "Security - Severe");
    Policy pMod = tempEntity.newPolicy(org.getId(), "Legal - Moderate");
    Policy pLow = tempEntity.newPolicy(org.getId(), "Quality - Low");

    tempEntity.newPolicyViolation(evaluation, pCrit, 10, PolicyThreatCategory.SECURITY,
        "com.crit", "crit10", "1.0", "hashCrit10000000000");
    tempEntity.newPolicyViolation(evaluation, pCrit, 8, PolicyThreatCategory.SECURITY,
        "com.crit", "crit8", "1.0", "hashCrit800000000000");
    tempEntity.newPolicyViolation(evaluation, pSev, 5, PolicyThreatCategory.SECURITY,
        "com.sev", "sev", "1.0", "hashSev000000000000");
    tempEntity.newPolicyViolation(evaluation, pMod, 3, PolicyThreatCategory.LICENSE,
        "com.mod", "mod", "1.0", "hashMod000000000000");
    tempEntity.newPolicyViolation(evaluation, pLow, 1, PolicyThreatCategory.QUALITY,
        "com.low", "low", "1.0", "hashLow000000000000");

    luceneSearchIndexClient.populateIndex();

    MetricAggregationResult result = luceneSearchIndexClient.aggregateCountByField(
        "itemType:" + ItemType.POLICY_VIOLATION.searchFieldName(),
        FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
        ThreatLevel.searchAggregationBands());

    assertThat(result.total).isEqualTo(5);
    assertThat(result.buckets.keySet()).containsExactlyElementsOf(ThreatLevel.searchAggregationBands().keySet());
    assertThat(result.buckets).containsEntry("critical", 2L);
    assertThat(result.buckets).containsEntry("severe", 1L);
    assertThat(result.buckets).containsEntry("moderate", 1L);
    assertThat(result.buckets).containsEntry("low", 1L);
  }

  @Test
  public void testAggregateCountByField_PolicyViolationEmptyResult() throws Exception {
    luceneSearchIndexClient.populateIndex();

    MetricAggregationResult result = luceneSearchIndexClient.aggregateCountByField(
        "itemType:" + ItemType.POLICY_VIOLATION.searchFieldName(),
        FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
        ThreatLevel.searchAggregationBands());

    assertThat(result.total).isZero();
    assertThat(result.buckets.keySet()).containsExactlyElementsOf(ThreatLevel.searchAggregationBands().keySet());
    assertThat(result.buckets.values()).containsOnly(0L);
  }

  @Test
  public void testAggregateCountByField_PolicyViolationPartialBands() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation evaluation =
        newAppReport(app.getId(), Stage.ID_BUILD, "aggPartialBandsReport", "/IndexSearchingTest/policyViolationReport");

    Policy pCrit = tempEntity.newPolicy(org.getId(), "Security - Critical");
    Policy pLow = tempEntity.newPolicy(org.getId(), "Quality - Low");

    tempEntity.newPolicyViolation(evaluation, pCrit, 10, PolicyThreatCategory.SECURITY,
        "com.crit", "crit", "1.0", "hashCrit00000000000");
    tempEntity.newPolicyViolation(evaluation, pLow, 0, PolicyThreatCategory.QUALITY,
        "com.low", "low", "1.0", "hashLow000000000000");

    luceneSearchIndexClient.populateIndex();

    MetricAggregationResult result = luceneSearchIndexClient.aggregateCountByField(
        "itemType:" + ItemType.POLICY_VIOLATION.searchFieldName(),
        FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
        ThreatLevel.searchAggregationBands());

    assertThat(result.total).isEqualTo(2);
    assertThat(result.buckets).containsEntry("critical", 1L);
    assertThat(result.buckets).containsEntry("low", 1L);
    assertThat(result.buckets).containsEntry("moderate", 0L);
    assertThat(result.buckets).containsEntry("severe", 0L);
  }

  @Test
  public void testAggregateCountByField_PolicyViolationOutOfRangeThreatLevelIncludedInTotal() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation evaluation =
        newAppReport(app.getId(), Stage.ID_BUILD, "aggOutOfRangeReport", "/IndexSearchingTest/policyViolationReport");

    Policy policy = tempEntity.newPolicy(org.getId(), "Security - Critical");
    tempEntity.newPolicyViolation(evaluation, policy, 15, PolicyThreatCategory.SECURITY,
        "com.crit", "crit", "1.0", "hCritOutOfRange15");

    luceneSearchIndexClient.populateIndex();

    MetricAggregationResult result = luceneSearchIndexClient.aggregateCountByField(
        "itemType:" + ItemType.POLICY_VIOLATION.searchFieldName(),
        FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
        ThreatLevel.searchAggregationBands());

    assertThat(result.total).isEqualTo(1);
    assertThat(result.buckets).containsEntry("critical", 1L);
    assertThat(result.buckets.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(result.total);
  }

  @Test
  public void testAggregateCountByField_rejectsMalformedRangeBounds() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> luceneSearchIndexClient.aggregateCountByField(
            "itemType:" + ItemType.POLICY_VIOLATION.searchFieldName(),
            FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
            Map.of("critical", new int[]{8})))
        .withMessageContaining("critical");
  }

  @Test
  public void testAggregateCountByField_rejectsInvertedRangeBounds() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> luceneSearchIndexClient.aggregateCountByField(
            "itemType:" + ItemType.POLICY_VIOLATION.searchFieldName(),
            FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label,
            Map.of("inverted", new int[]{10, 8})))
        .withMessageContaining("inverted");
  }

  /**
   * Writes SECURITY_VULNERABILITY docs with exact FloatPoint {@code vulnerabilitySeverity} scores
   * through the client's own writer. This pins the CVSS value precisely (a value the report pipeline
   * would round), so the band-boundary assertions test the exact float-range aggregation arithmetic.
   * Documents are added via {@link LuceneIndexingContext#addDocuments} (which contributes the same
   * sort doc-values the production path does) and {@code runWithWriter} commits + refreshes the NRT
   * searcher. The default global test user reads every doc (no RBAC filter), so no context ids are set.
   */
  private void indexVulnDocsWithSeverities(final float... severities) throws Exception {
    // populateIndex() creates the index/writer so runWithWriter can open it; add our docs on top.
    luceneSearchIndexClient.populateIndex();
    final List<Document> docs = new ArrayList<>();
    for (int i = 0; i < severities.length; i++) {
      docs.add(new DocumentBuilder(ItemType.SECURITY_VULNERABILITY)
          .setVulnerabilityId("CVE-BOUNDARY-" + i)
          .setVulnerabilitySeverity(severities[i])
          .build());
    }
    indexWriterOwner
        .runWithWriter(writer -> new LuceneIndexingContext(ownerDAO, writer, conversionHelper).addDocuments(docs));
  }

  /**
   * Writes one SECURITY_VULNERABILITY doc per (vulnerabilityId, severity) pair. A CVE that recurs across
   * several docs (e.g. per-app-per-stage) is the exact shape the distinct-per-band facet must count once.
   */
  private void indexVulnDocs(final String[] vulnIds, final float[] severities) throws Exception {
    luceneSearchIndexClient.populateIndex();
    final List<Document> docs = new ArrayList<>();
    for (int i = 0; i < vulnIds.length; i++) {
      docs.add(new DocumentBuilder(ItemType.SECURITY_VULNERABILITY)
          .setVulnerabilityId(vulnIds[i])
          .setVulnerabilitySeverity(severities[i])
          .build());
    }
    indexWriterOwner
        .runWithWriter(writer -> new LuceneIndexingContext(ownerDAO, writer, conversionHelper).addDocuments(docs));
  }

  @Test
  public void testAggregateCountByFloatField_DistinctField_CountsEachCveOncePerBand() throws Exception {
    // CVE-A recurs across 3 docs all scored High, CVE-B once High, CVE-C twice Critical. The distinct-per-band
    // count must be distinct CVEs (high=2, critical=1), not raw docs (high=4, critical=2). total stays raw.
    indexVulnDocs(
        new String[]{"CVE-A", "CVE-A", "CVE-A", "CVE-B", "CVE-C", "CVE-C"},
        new float[]{7.5f, 7.5f, 8.0f, 8.9f, 9.5f, 9.5f});

    MetricAggregationResult result = luceneSearchIndexClient.aggregateCountByFloatField(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.searchFieldName(),
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        CvssV3Severity.halfOpenScoreBands(),
        FieldIdentifier.VULNERABILITY_ID.label);

    assertThat(result.total).isEqualTo(6); // raw docs, unaffected by distinctField
    assertThat(result.buckets).containsEntry("none", 0L);
    assertThat(result.buckets).containsEntry("low", 0L);
    assertThat(result.buckets).containsEntry("medium", 0L);
    assertThat(result.buckets).containsEntry("high", 2L); // distinct CVE-A, CVE-B (not 4 docs)
    assertThat(result.buckets).containsEntry("critical", 1L); // distinct CVE-C (not 2 docs)
  }

  @Test
  public void testAggregateCountByFloatField_DistinctField_MatchesPerBandCountDistinctLoop() throws Exception {
    // The distinct-per-band aggregation pass must produce the exact same per-band numbers as the old loop
    // of one countDistinct(query AND half-open-band-clause, [vulnerabilityId]) per band it replaced.
    final String[] cves = {"CVE-1", "CVE-1", "CVE-2", "CVE-3", "CVE-3", "CVE-4", "CVE-5"};
    final float[] scores = {0.0f, 0.0f, 2.0f, 5.0f, 5.0f, 7.5f, 9.9f};
    indexVulnDocs(cves, scores);
    final String svType = "itemType:" + ItemType.SECURITY_VULNERABILITY.searchFieldName();
    final String severity = FieldIdentifier.VULNERABILITY_SEVERITY.label;
    final List<String> byCve = List.of(FieldIdentifier.VULNERABILITY_ID.label);

    MetricAggregationResult result = luceneSearchIndexClient.aggregateCountByFloatField(
        svType, severity, CvssV3Severity.halfOpenScoreBands(), FieldIdentifier.VULNERABILITY_ID.label);

    // Old per-band countDistinct loop, band by band, must equal the single-pass aggregation buckets.
    assertThat(result.buckets.get("none"))
        .isEqualTo(luceneSearchIndexClient.countDistinct(svType + " AND " + severity + ":[0.0 TO 0.0]", byCve));
    assertThat(result.buckets.get("low"))
        .isEqualTo(luceneSearchIndexClient.countDistinct(svType + " AND " + severity + ":[0.1 TO 4.0}", byCve));
    assertThat(result.buckets.get("medium"))
        .isEqualTo(luceneSearchIndexClient.countDistinct(svType + " AND " + severity + ":[4.0 TO 7.0}", byCve));
    assertThat(result.buckets.get("high"))
        .isEqualTo(luceneSearchIndexClient.countDistinct(svType + " AND " + severity + ":[7.0 TO 9.0}", byCve));
    assertThat(result.buckets.get("critical"))
        .isEqualTo(luceneSearchIndexClient.countDistinct(svType + " AND " + severity + ":[9.0 TO 10.0]", byCve));
    // Concretely: none=1 (CVE-1), low=1 (CVE-2), medium=1 (CVE-3), high=1 (CVE-4), critical=1 (CVE-5).
    assertThat(result.buckets.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(5);
  }

  @Test
  public void testAggregateCountByFloatField_CvssBandBoundariesLandInExactlyOneBand() throws Exception {
    // One score at every band boundary (and interior). Half-open bands must place each in exactly one
    // band: 0.0->none, 0.1/3.9->low, 4.0/6.9->medium, 7.0/8.9->high, 9.0/10.0->critical.
    indexVulnDocsWithSeverities(0.0f, 0.1f, 3.9f, 4.0f, 6.9f, 7.0f, 8.9f, 9.0f, 10.0f);

    MetricAggregationResult result = luceneSearchIndexClient.aggregateCountByFloatField(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.searchFieldName(),
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        CvssV3Severity.halfOpenScoreBands());

    assertThat(result.total).isEqualTo(9);
    assertThat(result.buckets).containsEntry("none", 1L); // 0.0
    assertThat(result.buckets).containsEntry("low", 2L); // 0.1, 3.9
    assertThat(result.buckets).containsEntry("medium", 2L); // 4.0, 6.9
    assertThat(result.buckets).containsEntry("high", 2L); // 7.0, 8.9
    assertThat(result.buckets).containsEntry("critical", 2L); // 9.0, 10.0
    // No double-counting: every doc lands in exactly one band, so the bands sum to the total.
    assertThat(result.buckets.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(result.total);
  }

  @Test
  public void testAggregateCountByFloatField_BoundaryValueSevenIsHighNotMedium() throws Exception {
    // The canonical footgun: a CVSS 7.0 must be High, never Medium (Medium is [4.0, 7.0), High is [7.0, 9.0)).
    indexVulnDocsWithSeverities(7.0f);

    MetricAggregationResult result = luceneSearchIndexClient.aggregateCountByFloatField(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.searchFieldName(),
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        CvssV3Severity.halfOpenScoreBands());

    assertThat(result.buckets).containsEntry("high", 1L);
    assertThat(result.buckets).containsEntry("medium", 0L);
  }

  @Test
  public void testAggregateCountByFloatField_ZeroIsNoneAndTenIsCritical() throws Exception {
    indexVulnDocsWithSeverities(0.0f, 10.0f);

    MetricAggregationResult result = luceneSearchIndexClient.aggregateCountByFloatField(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.searchFieldName(),
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        CvssV3Severity.halfOpenScoreBands());

    assertThat(result.buckets).containsEntry("none", 1L);
    assertThat(result.buckets).containsEntry("critical", 1L);
    assertThat(result.buckets).containsEntry("low", 0L);
  }

  @Test
  public void testCountDistinct_HalfOpenSeverityBandClause_BoundarySevenIsHighNotMedium() throws Exception {
    // The catalog severity facet counts distinct CVEs per band using a Lucene range STRING clause built
    // from CvssV3Severity.halfOpenScoreBands() (the same source of truth the float-range primitive uses).
    // This proves the string path — parsed by StandardQueryParser — honors the exclusive-upper } bracket
    // identically to the primitive's programmatic FloatPoint.newRangeQuery(lo, nextDown(hi)): a boundary
    // 7.0 lands in High [7.0 TO 9.0} and never in Medium [4.0 TO 7.0}.
    indexVulnDocsWithSeverities(7.0f);
    final String svType = "itemType:" + ItemType.SECURITY_VULNERABILITY.searchFieldName();
    final String severity = FieldIdentifier.VULNERABILITY_SEVERITY.label;
    final List<String> byCve = List.of(FieldIdentifier.VULNERABILITY_ID.label);

    assertThat(luceneSearchIndexClient.countDistinct(svType + " AND " + severity + ":[7.0 TO 9.0}", byCve))
        .isEqualTo(1);
    assertThat(luceneSearchIndexClient.countDistinct(svType + " AND " + severity + ":[4.0 TO 7.0}", byCve))
        .isZero();
  }

  @Test
  public void testCountDistinct_HalfOpenSeverityBandClauses_EveryBoundaryLandsInExactlyOneBand() throws Exception {
    // Mirror the float-primitive boundary test through the facet's countDistinct string clauses: 0.0->none,
    // 0.1/3.9->low, 4.0/6.9->medium, 7.0/8.9->high, 9.0/10.0->critical, each CVE distinct. None (single
    // point 0.0) and Critical (inclusive top 10.0) close inclusive ]; the others close exclusive }.
    indexVulnDocsWithSeverities(0.0f, 0.1f, 3.9f, 4.0f, 6.9f, 7.0f, 8.9f, 9.0f, 10.0f);
    final String svType = "itemType:" + ItemType.SECURITY_VULNERABILITY.searchFieldName();
    final String severity = FieldIdentifier.VULNERABILITY_SEVERITY.label;
    final List<String> byCve = List.of(FieldIdentifier.VULNERABILITY_ID.label);

    assertThat(luceneSearchIndexClient.countDistinct(svType + " AND " + severity + ":[0.0 TO 0.0]", byCve))
        .isEqualTo(1); // 0.0
    assertThat(luceneSearchIndexClient.countDistinct(svType + " AND " + severity + ":[0.1 TO 4.0}", byCve))
        .isEqualTo(2); // 0.1, 3.9
    assertThat(luceneSearchIndexClient.countDistinct(svType + " AND " + severity + ":[4.0 TO 7.0}", byCve))
        .isEqualTo(2); // 4.0, 6.9
    assertThat(luceneSearchIndexClient.countDistinct(svType + " AND " + severity + ":[7.0 TO 9.0}", byCve))
        .isEqualTo(2); // 7.0, 8.9
    assertThat(luceneSearchIndexClient.countDistinct(svType + " AND " + severity + ":[9.0 TO 10.0]", byCve))
        .isEqualTo(2); // 9.0, 10.0
  }

  @Test
  public void testAggregateCountByFloatField_FailsClosed_UserWithNoReadContextsCountsZero() throws Exception {
    indexVulnDocsWithSeverities(2.0f, 5.0f, 9.5f);

    actAsUser("user-with-no-permissions");

    MetricAggregationResult result = luceneSearchIndexClient.aggregateCountByFloatField(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.searchFieldName(),
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        CvssV3Severity.halfOpenScoreBands());

    assertThat(result.total).isZero();
    assertThat(result.buckets.values()).containsOnly(0L);
  }

  @Test
  public void testAggregateCountByFloatField_rejectsMalformedRangeBounds() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> luceneSearchIndexClient.aggregateCountByFloatField(
            "itemType:" + ItemType.SECURITY_VULNERABILITY.searchFieldName(),
            FieldIdentifier.VULNERABILITY_SEVERITY.label,
            Map.of("high", new float[]{7.0f})))
        .withMessageContaining("high");
  }

  @Test
  public void testAggregateCountByFloatField_rejectsInvertedRangeBounds() {
    Map<String, float[]> inverted = new LinkedHashMap<>();
    inverted.put("inverted", new float[]{9.0f, 4.0f});
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> luceneSearchIndexClient.aggregateCountByFloatField(
            "itemType:" + ItemType.SECURITY_VULNERABILITY.searchFieldName(),
            FieldIdentifier.VULNERABILITY_SEVERITY.label,
            inverted))
        .withMessageContaining("inverted");
  }

  @Test
  public void testRankGroupsByMaxMetric_RanksDistinctVulnerabilitiesByHighestCvss() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    newAppReport(app.getId(), Stage.ID_BUILD, "rankGroupsReport", "/IndexSearchingTest/componentsMetricReport");

    luceneSearchIndexClient.populateIndex();

    RankedGroupsResult result = luceneSearchIndexClient.rankGroupsByMaxMetric(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        25,
        false,
        CvssV3Severity.halfOpenScoreBands());

    // Four SECURITY_VULNERABILITY docs, but rows are distinct vulnerabilities.
    assertThat(result.distinctGroupCount()).isEqualTo(result.groups().size());
    assertThat(result.distinctGroupCountExact()).isTrue();
    assertThat(result.groups()).isNotEmpty();
    assertThat(result.groups()).extracting(RankedGroup::groupValue).doesNotHaveDuplicates();
    assertThat(result.groups()).extracting(RankedGroup::groupValue)
        .allSatisfy(id -> assertThat(id).isEqualTo(id.toLowerCase(Locale.ROOT)));

    // Descending CVSS, nulls last. This fixture happens to score every vulnerability, so the
    // trailing "nulls" slice is empty; allMatch (unlike containsOnlyNulls) holds vacuously for an
    // empty slice, so the assertion still expresses "any unscored group sorts after every scored one"
    // without assuming this fixture has an unscored vulnerability.
    List<Float> scores = result.groups().stream().map(RankedGroup::metricValue).toList();
    List<Float> scored = scores.stream().filter(java.util.Objects::nonNull).toList();
    assertThat(scored).isSortedAccordingTo(Comparator.reverseOrder());
    assertThat(scores.subList(scored.size(), scores.size())).allMatch(java.util.Objects::isNull);
  }

  @Test
  public void testRankGroupsByMaxMetric_BandCountsAndUnbandedSumToDistinctTotal() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    newAppReport(app.getId(), Stage.ID_BUILD, "rankGroupsBandsReport",
        "/IndexSearchingTest/componentsMetricReport");

    luceneSearchIndexClient.populateIndex();

    RankedGroupsResult result = luceneSearchIndexClient.rankGroupsByMaxMetric(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        25,
        false,
        CvssV3Severity.halfOpenScoreBands());

    assertThat(result.bandCounts().keySet())
        .containsExactlyElementsOf(CvssV3Severity.halfOpenScoreBands().keySet());
    long banded = result.bandCounts().values().stream().mapToLong(Long::longValue).sum();
    assertThat(banded + result.unbandedGroupCount()).isEqualTo(result.distinctGroupCount());
  }

  @Test
  public void testRankGroupsByMaxMetric_LimitBoundsGroupsButNotDistinctCount() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    newAppReport(app.getId(), Stage.ID_BUILD, "rankGroupsLimitReport",
        "/IndexSearchingTest/componentsMetricReport");

    luceneSearchIndexClient.populateIndex();

    RankedGroupsResult unlimited = luceneSearchIndexClient.rankGroupsByMaxMetric(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        25, false, CvssV3Severity.halfOpenScoreBands());
    RankedGroupsResult limited = luceneSearchIndexClient.rankGroupsByMaxMetric(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        1, false, CvssV3Severity.halfOpenScoreBands());

    assertThat(limited.groups()).hasSize(1);
    assertThat(limited.distinctGroupCount()).isEqualTo(unlimited.distinctGroupCount());
    assertThat(limited.groups().get(0)).isEqualTo(unlimited.groups().get(0));
  }

  @Test
  public void testRankGroupsByMaxMetric_FailsClosed_UserWithNoReadContextsGetsEmpty() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    newAppReport(app.getId(), Stage.ID_BUILD, "rankGroupsFailClosed",
        "/IndexSearchingTest/componentsMetricReport");

    luceneSearchIndexClient.populateIndex();

    actAsUser("user-with-no-permissions");

    RankedGroupsResult result = luceneSearchIndexClient.rankGroupsByMaxMetric(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        25, false, CvssV3Severity.halfOpenScoreBands());

    assertThat(result.groups()).isEmpty();
    assertThat(result.distinctGroupCount()).isZero();
  }

  /**
   * Loads the dedicated {@code rankGroupsEdgeCasesReport} fixture: 4 distinct CVEs across 4
   * components — one with no CVSS score, two ({@code CVE-RANK-TIE-A}, {@code CVE-RANK-TIE-B})
   * tied at exactly 6.5 (medium band), and one ({@code CVE-RANK-BOUNDARY}) at exactly 7.0, the
   * medium/high band boundary. Unlike {@code componentsMetricReport} (whose 4 CVEs are all scored
   * and land in 4 different, non-adjacent bands), this fixture exercises the NaN-sentinel,
   * unbanded-count, tie-break, and half-open-boundary paths that {@code componentsMetricReport}
   * cannot. {@code security.json}'s unscored entry omits the {@code score} key entirely;
   * {@code ComponentLoader} reads it via {@code JsonUtils.getNullableFloat}, which returns
   * {@code null} for a missing key, so the report pipeline (not just the low-level
   * {@code DocumentBuilder} test helpers) can produce a CVE with no severity.
   */
  private RankedGroupsResult rankGroupsEdgeCases(final int limit, final boolean ascending) throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    newAppReport(app.getId(), Stage.ID_BUILD, "rankGroupsEdgeCases-" + System.nanoTime(),
        "/IndexSearchingTest/rankGroupsEdgeCasesReport");

    luceneSearchIndexClient.populateIndex();

    return luceneSearchIndexClient.rankGroupsByMaxMetric(
        "itemType:" + ItemType.SECURITY_VULNERABILITY.name(),
        FieldIdentifier.VULNERABILITY_ID.label,
        FieldIdentifier.VULNERABILITY_SEVERITY.label,
        limit,
        ascending,
        CvssV3Severity.halfOpenScoreBands());
  }

  @Test
  public void testRankGroupsByMaxMetric_UnscoredVulnerabilitySortsLastWithNullMetric() throws Exception {
    RankedGroupsResult result = rankGroupsEdgeCases(25, false);

    RankedGroup last = result.groups().get(result.groups().size() - 1);
    assertThat(last.groupValue()).isEqualTo("cve-rank-unscored");
    assertThat(last.metricValue()).isNull();
    // Every other group is scored, so the unscored CVE is the unique null and it sorts last.
    assertThat(result.groups()).filteredOn(g -> g.metricValue() == null).containsExactly(last);
  }

  @Test
  public void testRankGroupsByMaxMetric_UnbandedCountMatchesUnscoredAndSumsToDistinctTotal() throws Exception {
    RankedGroupsResult result = rankGroupsEdgeCases(25, false);

    assertThat(result.distinctGroupCount()).isEqualTo(4);
    // Exactly one unscored CVE (CVE-RANK-UNSCORED) is unbanded; the other three are all scored.
    assertThat(result.unbandedGroupCount()).isEqualTo(1);
    long banded = result.bandCounts().values().stream().mapToLong(Long::longValue).sum();
    assertThat(banded + result.unbandedGroupCount()).isEqualTo(result.distinctGroupCount());
  }

  @Test
  public void testRankGroupsByMaxMetric_TiedMetricsBreakTieByAscendingVulnerabilityId() throws Exception {
    RankedGroupsResult result = rankGroupsEdgeCases(25, false);

    // Descending metric: CVE-RANK-BOUNDARY (7.0) first, then the two 6.5-tied CVEs, then the
    // unscored CVE last. The tie between TIE-A and TIE-B must break on ascending global ordinal,
    // i.e. ascending lower-cased vulnerability id: "cve-rank-tie-a" before "cve-rank-tie-b".
    assertThat(result.groups()).extracting(RankedGroup::groupValue)
        .containsExactly("cve-rank-boundary", "cve-rank-tie-a", "cve-rank-tie-b", "cve-rank-unscored");
  }

  @Test
  public void testRankGroupsByMaxMetric_BoundaryScoreLandsInHighBandNotMedium() throws Exception {
    RankedGroupsResult result = rankGroupsEdgeCases(25, false);

    // CVE-RANK-BOUNDARY is scored exactly 7.0, the medium/high boundary: high=[7.0,9.0), so it
    // must land in high, never medium. CVE-RANK-TIE-A/B at 6.5 are the only other scored CVEs and
    // both fall in medium=[4.0,7.0), so a high count of 1 and a medium count of 2 is only possible
    // if the boundary CVE is counted in high, not medium.
    assertThat(result.bandCounts()).containsEntry("high", 1L);
    assertThat(result.bandCounts()).containsEntry("medium", 2L);
    assertThat(result.groups()).filteredOn(g -> "cve-rank-boundary".equals(g.groupValue()))
        .extracting(RankedGroup::metricValue)
        .containsExactly(7.0f);
  }

  /**
   * Pins sequential {@code groupDocuments} behavior with a controlled document order (CLM-29232).
   * Non-adjacent same keys must not merge; adjacent same keys must.
   */
  @Test
  public void groupDocuments_mergesOnlySequentialSameGroupByKeys() {
    List<Document> docs = List.of(
        new DocumentBuilder(ItemType.SECURITY_VULNERABILITY).setVulnerabilityId("CVE-A").build(),
        new DocumentBuilder(ItemType.SECURITY_VULNERABILITY).setVulnerabilityId("CVE-A").build(),
        new DocumentBuilder(ItemType.SECURITY_VULNERABILITY).setVulnerabilityId("CVE-B").build(),
        new DocumentBuilder(ItemType.SECURITY_VULNERABILITY).setVulnerabilityId("CVE-A").build());
    Iterator<Document> iterator = docs.iterator();
    Supplier<Document> supplier = () -> iterator.hasNext() ? iterator.next() : null;

    SearchResultDTO result = new SearchResultDTO();
    Map<String, String> groupFields =
        Map.of(ItemType.SECURITY_VULNERABILITY.name(), FieldIdentifier.VULNERABILITY_ID.label);

    ReflectionTestUtils.invokeMethod(
        luceneSearchIndexClient,
        "groupDocuments",
        1,
        25,
        supplier,
        result,
        groupFields);

    assertThat(result.groupingByDTOS).hasSize(3);
    assertThat(result.groupingByDTOS.get(0).groupBy).isEqualTo("CVE-A");
    assertThat(result.groupingByDTOS.get(0).searchResultItemDTOS).hasSize(2);
    assertThat(result.groupingByDTOS.get(0).searchResultItemDTOS)
        .extracting(item -> item.resultIndex)
        .containsExactly(1, 2);
    assertThat(result.groupingByDTOS.get(1).groupBy).isEqualTo("CVE-B");
    assertThat(result.groupingByDTOS.get(1).searchResultItemDTOS).hasSize(1);
    assertThat(result.groupingByDTOS.get(1).searchResultItemDTOS.get(0).resultIndex).isEqualTo(3);
    assertThat(result.groupingByDTOS.get(2).groupBy).isEqualTo("CVE-A");
    assertThat(result.groupingByDTOS.get(2).searchResultItemDTOS).hasSize(1);
    assertThat(result.groupingByDTOS.get(2).searchResultItemDTOS.get(0).resultIndex).isEqualTo(4);
  }
}
