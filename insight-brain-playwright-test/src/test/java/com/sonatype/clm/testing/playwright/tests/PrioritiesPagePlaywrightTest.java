/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.PrioritiesPage;
import com.sonatype.clm.testing.playwright.pages.PrioritiesPageAssertions;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

/** Regression tests for the Developer Priorities page (waiver indicators). */
public class PrioritiesPagePlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME_PREFIX = "AppReportTestOrg";

  private static final String APP_NAME_PREFIX = "AppReportTestApp";

  private static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private static final String REPORT_DIR = "/canned-reports/large-report";

  private Application app;

  private String appName;

  private TestReportEvaluator evaluator;

  @Before
  public void prepareApp() throws IOException {
    seedDb();
  }

  private void openPrioritiesPage() {
    playwrightRefreshOrOpen(PrioritiesPage.url(app.getPublicId(), SCAN_ID));
    playwrightLogin();
    new PrioritiesPageAssertions(new PrioritiesPage()).shouldBeVisible();
  }

  /**
   * Re-opens the priorities page on the same session, scoped to a single component via the
   * {@code componentNameFilter} route param so the seeded row is visible regardless of
   * sort/pagination. Use after the initial {@link #openPrioritiesPage()} + re-evaluation.
   */
  private PrioritiesPageAssertions reopenPrioritiesPageFilteredBy(String componentNameFilter) {
    playwrightRefreshOrOpen(PrioritiesPage.url(app.getPublicId(), SCAN_ID, componentNameFilter));
    PrioritiesPageAssertions assertions = new PrioritiesPageAssertions(new PrioritiesPage());
    assertions.shouldBeVisible();
    return assertions;
  }

  @Test
  @Category(RegressionTest.class)
  public void testPrioritiesPage_expiredWaiverShowsQuestionCircleIcon() throws IOException {
    evaluator.evaluatePolicy();
    openPrioritiesPage();

    PolicyViolation violation = firstSeedableViolation();

    Date pastExpiry = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
    tempEntity.newWaiver(
        violation.getHash(),
        violation.getPolicyId(),
        app.getId(),
        "Expired waiver regression check",
        pastExpiry);

    evaluator.reevaluatePolicy();

    String artifactId = violation.getComponentIdentifier().getCoordinates().get("artifactId");
    PrioritiesPage prioritiesPage = new PrioritiesPage();
    PrioritiesPageAssertions assertions = reopenPrioritiesPageFilteredBy(artifactId);

    assertions.shouldShowExpiredWaiverIconOnRow(prioritiesPage.rowByArtifactId(artifactId));
  }

  @Test
  @Category(RegressionTest.class)
  public void testPrioritiesPage_soonToExpireWaiverShowsWarningTriangleIcon() throws IOException {
    evaluator.evaluatePolicy();
    openPrioritiesPage();

    PolicyViolation representative = firstSeedableViolation();
    // Soon-to-expire icon requires isAllViolationsWaived=true. Waive every violation on the
    // component hash, dedup by policyId (waiver unique key is hash+policyId+ownerId).
    PolicyViolationDAO dao = lookup(PolicyViolationDAO.class);
    List<PolicyViolation> allViolations = dao.getByApplicationId(app.getId());
    Map<String, PolicyViolation> uniqueByPolicyId = allViolations.stream()
        .filter(v -> representative.getHash().equals(v.getHash()) && v.getPolicyId() != null)
        .collect(Collectors.toMap(PolicyViolation::getPolicyId, v -> v, (a, b) -> a));
    List<PolicyViolation> componentViolations = List.copyOf(uniqueByPolicyId.values());

    // Fail closed if the canned report drifts — otherwise zero seeded waivers would silently pass.
    assertThat(componentViolations).as("violations on the seeded component (precondition)").isNotEmpty();

    Date nearFutureExpiry = Date.from(Instant.now().plus(7, ChronoUnit.DAYS));
    for (PolicyViolation v : componentViolations) {
      tempEntity.newWaiver(
          v.getHash(),
          v.getPolicyId(),
          app.getId(),
          "Soon-to-expire waiver regression check",
          nearFutureExpiry);
    }

    evaluator.reevaluatePolicy();

    String artifactId = representative.getComponentIdentifier().getCoordinates().get("artifactId");
    PrioritiesPage prioritiesPage = new PrioritiesPage();
    PrioritiesPageAssertions assertions = reopenPrioritiesPageFilteredBy(artifactId);

    assertions.shouldShowSoonToExpireWaiverIconOnRow(prioritiesPage.rowByArtifactId(artifactId));
  }

  private PolicyViolation firstSeedableViolation() {
    PolicyViolationDAO dao = lookup(PolicyViolationDAO.class);
    List<PolicyViolation> violations = dao.getByApplicationId(app.getId());
    return violations.stream()
        .filter(v -> v.getHash() != null && v.getPolicyId() != null
            && v.getComponentIdentifier() != null
            && v.getComponentIdentifier().getCoordinates() != null
            && v.getComponentIdentifier().getCoordinates().get("artifactId") != null)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "No suitable PolicyViolation found after canned-report evaluation for app=" + app.getId()));
  }

  /**
   * Seeds org + policies + app + configured evaluator; does NOT call {@code evaluatePolicy()} —
   * each test runs the initial evaluation itself to avoid an {@code AutoPolicyWaiver}-induced
   * 404 race we hit when it was called from {@code @Before}. Do not lift the call back here
   * without re-testing that race.
   */
  private void seedDb() throws IOException {
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies =
        JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);

    String suffix = TemporaryEntity.uuid();
    String orgName = ORG_NAME_PREFIX + "-" + suffix;
    appName = APP_NAME_PREFIX + "-" + suffix;

    Organization org = tempEntity.newOrganization(orgName);
    lookup(PolicyImportExport.class).importOrganization(org, referencePolicies);
    // Reference policies have no actions — add fail so waiver icons render.
    PolicyDAO policyDAO = lookup(PolicyDAO.class);
    for (Policy policy : policyDAO.getByOwnerId(org.getId())) {
      policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
      policyDAO.update(policy);
    }
    app = tempEntity.newApplication(appName, appName, org.getId());

    URL zippedReport = ReportHelper.zipReport(REPORT_DIR, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest,
        work, Stage.ID_BUILD);
  }
}
