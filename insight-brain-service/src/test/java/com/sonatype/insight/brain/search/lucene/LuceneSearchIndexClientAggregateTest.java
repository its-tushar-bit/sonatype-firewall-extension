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

import java.util.Map;
import java.util.concurrent.ExecutorService;

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
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.tenancy.TenantReference;

import jakarta.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for the RBAC-scoped {@link LuceneSearchIndexClient#count(String)} and
 * {@link LuceneSearchIndexClient#aggregateCountByField(String, String, Map)} primitives (CLM-40927).
 */
public class LuceneSearchIndexClientAggregateTest
    extends AbstractComponentTest
{
  @Inject
  private LuceneSearchIndexClient luceneSearchIndexClient;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Mock
  private com.sonatype.insight.brain.search.index.VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Before
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
    ReportTestUtils.createReportFile(policyEval.getApplicationId(), policyEval.getScanId(),
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
}
