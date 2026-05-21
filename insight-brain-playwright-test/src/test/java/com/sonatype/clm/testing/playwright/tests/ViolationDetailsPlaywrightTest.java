/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.AddWaiverPage;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.ViolationDetailsPage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import org.junit.Before;
import org.junit.Test;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

/**
 * Playwright test for the Violation Details page.
 * <p>
 * Each test follows a Given/When/Then shape:
 * <ul>
 * <li>{@link #seedViolationAndOpenAsAdmin()} seeds a per-test {@link Organization} +
 * {@link Application} (names UUID-suffixed for parallel-fork safety), a security
 * {@link Policy} on the root organization, a {@link PolicyEvaluation} for the seeded app
 * at BUILD stage, and a single {@link PolicyViolation} referencing a CVE so the
 * Vulnerability Details tab is guaranteed to render.</li>
 * <li>The test body navigates straight to {@link ViolationDetailsPage#url(String)} and
 * exercises the page via {@link ViolationDetailsPage} locators.</li>
 * </ul>
 *
 * <p>
 * Selectors live in {@link ViolationDetailsPage} (and {@link AddWaiverPage} for the destination
 * page reached by clicking "Add Waiver").
 */
public class ViolationDetailsPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORGANIZATION_NAME_PREFIX = "ViolationDetailsOrg";

  private static final String APPLICATION_NAME_PREFIX = "ViolationDetailsApp";

  private static final String APPLICATION_PUBLIC_ID_PREFIX = "violationDetailsApp";

  private static final String POLICY_NAME = "Security Policy";

  private static final int POLICY_THREAT_LEVEL = 8;

  private static final String SCAN_ID = "scan1";

  private static final String COMPONENT_GROUP = "com.example";

  private static final String COMPONENT_ARTIFACT = "test-lib";

  private static final String COMPONENT_VERSION = "1.0.0";

  private static final String COMPONENT_HASH = "hash123";

  private static final String VULNERABILITY_REF_ID = "sonatype-2017-0507";

  private static final String EXPECTED_COMPONENT_DISPLAY =
      COMPONENT_GROUP + " : " + COMPONENT_ARTIFACT + " : " + COMPONENT_VERSION;

  private PolicyViolation policyViolation;

  // --------------- @Before ---------------

  @Before
  public void seedViolationAndOpenAsAdmin() {
    seedOrgAppAndViolation();

    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  // --------------- @Test methods ---------------

  @Test
  @Category(SanityTest.class)
  public void testDetails() {
    // Given: navigate directly to the seeded violation's details page.
    ViolationDetailsPage detailsPage = openViolationDetails();

    // Then: the details tile renders the seeded component coordinates and policy name.
    assertThat(detailsPage.componentName()).containsText(EXPECTED_COMPONENT_DISPLAY);
    assertThat(detailsPage.policyName()).containsText(POLICY_NAME);
  }

  @Test
  @Category(SanityTest.class)
  public void testPolicyViolationInfo() {
    // Given: navigate directly to the seeded violation's details page.
    ViolationDetailsPage detailsPage = openViolationDetails();

    // Then: the constraint info tile and its conditions list render.
    assertThat(detailsPage.constraintSection()).isVisible();
    assertThat(detailsPage.conditionsSection()).isVisible();
  }

  @Test
  @Category(SanityTest.class)
  public void testGoDirectlyToAddWaiver() {
    // Given: admin user on the seeded violation's details page — Add Waiver MUST be available.
    ViolationDetailsPage detailsPage = openViolationDetails();
    assertThat(detailsPage.addWaiverButton()).isVisible();

    // When: click Add Waiver.
    detailsPage.addWaiverButton().click();

    // Then: navigation lands on the Add Waiver page for the seeded violation.
    playwrightWaitUntilUrlContains("/addWaiver/" + violationId());
    assertThat(new AddWaiverPage().container()).isVisible();
  }

  // --------------- Helpers ---------------

  private ViolationDetailsPage openViolationDetails() {
    String url = ViolationDetailsPage.url(violationId());
    // Hash-only deep links can race the SPA router on a freshly logged-in page; a reload is the
    // same defensive pattern AddWaiverPlaywrightTest.testOpenPageDirectly... uses.
    playwrightRefreshOrOpen(url);
    playwrightRefresh();
    ViolationDetailsPage detailsPage = new ViolationDetailsPage();
    assertThat(detailsPage.container()).isVisible();
    return detailsPage;
  }

  // --------------- Backend seed methods ---------------

  private void seedOrgAppAndViolation() {
    String suffix = TemporaryEntity.uuid();
    String orgName = ORGANIZATION_NAME_PREFIX + "-" + suffix;
    String appName = APPLICATION_NAME_PREFIX + "-" + suffix;
    String appPublicId = APPLICATION_PUBLIC_ID_PREFIX + "-" + suffix;

    Organization organization = tempEntity.newOrganization(orgName);
    Application application = tempEntity.newApplication(appName, appPublicId, organization.getId());

    // Policy is created on the root org so it's visible to any descendant application.
    Policy securityPolicy = tempEntity.newPolicy(
        Organization.ROOT_ORGANIZATION_ID, POLICY_NAME, POLICY_THREAT_LEVEL);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(), StageTypes.BUILD.getId(), SCAN_ID);

    policyViolation = tempEntity.newPolicyViolation(
        policyEvaluation,
        securityPolicy,
        COMPONENT_GROUP,
        COMPONENT_ARTIFACT,
        COMPONENT_VERSION,
        COMPONENT_HASH,
        VULNERABILITY_REF_ID);
  }

  private String violationId() {
    return policyViolation.getId();
  }
}
