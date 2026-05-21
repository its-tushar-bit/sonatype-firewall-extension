/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.AddWaiverPage;
import com.sonatype.clm.testing.playwright.pages.AddWaiverPageAssertions;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.ViolationDetailsPage;
import com.sonatype.clm.testing.playwright.pages.ViolationDetailsPageAssertions;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

/**
 * Playwright smoke tests for the Add Waiver page.
 */
public class AddWaiverPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String PARENT_ORG_NAME = "Parent Org";

  private static final String ORG_NAME = "Org 1";

  private static final String APP_NAME = "App 1";

  private static final String APP_ID = "app1";

  private static final String POLICY1_NAME = "Policy 1";

  private static final int POLICY1_THREAT_LEVEL = 7;

  private static final String POLICY2_NAME = "Policy 2";

  private static final int POLICY2_THREAT_LEVEL = 8;

  private static final String COMPONENT1_GROUP_ID = "Group1";

  private static final String COMPONENT1_ARTIFACT_ID = "Artifact1";

  private static final String COMPONENT1_VERSION = "Version1";

  private static final String COMPONENT1_HASH = "hash1";

  private static final String COMPONENT1_CVE_ID = "sonatype-2017-0507";

  private static final String COMPONENT2_GROUP_ID = "Group2";

  private static final String COMPONENT2_ARTIFACT_ID = "Artifact2";

  private static final String COMPONENT2_VERSION = "Version2";

  private static final String COMPONENT2_HASH = "hash2";

  private static final String COMPONENT2_CVE_ID = "sonatype-2018-0777";

  private static final String CONSTRAINT_NAME = "Test Constraint";

  private static final String VULNERABILITY_DETAILS_LINK_TEXT = "See Security Vulnerability Details";

  private static final String COMMENT = "Some comments";

  private static final int EXPECTED_SCOPE_COUNT = 4;

  private static final int EXPECTED_COMPONENT_RADIO_COUNT = 3;

  private static final int EXPECTED_EXPIRY_OPTIONS_COUNT = 8;

  private static final int EXPECTED_WAIVER_REASON_OPTIONS_COUNT = 9;

  private static final String CREATED_BY_NAME = "Admin BuiltIn";

  private static final String SCOPE_APP = "Application - App 1";

  private static final String SCOPE_ORG = "Organization - Org 1";

  private static final String SCOPE_PARENT_ORG = "Organization - Parent Org";

  private static final String SCOPE_ROOT_ORG = "Organization - Root Organization";

  private Application application;

  private PolicyViolation primaryViolation;

  private PolicyViolation secondaryViolation;

  @Before
  public void setUp() {
    Organization parentOrg = tempEntity.newOrganization(PARENT_ORG_NAME);
    Organization org = tempEntity.newOrganization(ORG_NAME, parentOrg);
    application = tempEntity.newApplication(APP_NAME, APP_ID, org.getId());

    Policy policy1 =
        tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, POLICY1_NAME, POLICY1_THREAT_LEVEL);
    Policy policy2 =
        tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, POLICY2_NAME, POLICY2_THREAT_LEVEL);

    Date twoDaysAgo = Date.from(Instant.now().minus(2, ChronoUnit.DAYS));
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        application.getId(), StageTypes.BUILD.getId(), "scan1", false, false, twoDaysAgo);

    primaryViolation = tempEntity.newPolicyViolation(evaluation, policy1,
        COMPONENT1_GROUP_ID, COMPONENT1_ARTIFACT_ID, COMPONENT1_VERSION,
        COMPONENT1_HASH, COMPONENT1_CVE_ID);

    secondaryViolation = tempEntity.newPolicyViolation(evaluation, policy2,
        COMPONENT2_GROUP_ID, COMPONENT2_ARTIFACT_ID, COMPONENT2_VERSION,
        COMPONENT2_HASH, COMPONENT2_CVE_ID);

    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  @After
  public void tearDown() {
    reverseProxyServer.reset();
  }

  @Test
  @Category(SanityTest.class)
  public void testPageLayout() {
    playwrightRefreshOrOpen(AddWaiverPage.url(primaryViolation.getId()));

    String component1Coords =
        COMPONENT1_GROUP_ID + " : " + COMPONENT1_ARTIFACT_ID + " : " + COMPONENT1_VERSION;
    String allVersions = COMPONENT1_GROUP_ID + " : " + COMPONENT1_ARTIFACT_ID + " (all versions)";

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    AddWaiverPageAssertions addWaiverAssertions = new AddWaiverPageAssertions(addWaiverPage);
    addWaiverAssertions.shouldShowPageLayout(
        COMPONENT1_ARTIFACT_ID,
        component1Coords,
        POLICY1_NAME,
        CONSTRAINT_NAME,
        COMPONENT1_CVE_ID,
        VULNERABILITY_DETAILS_LINK_TEXT,
        EXPECTED_SCOPE_COUNT,
        EXPECTED_COMPONENT_RADIO_COUNT,
        EXPECTED_EXPIRY_OPTIONS_COUNT,
        EXPECTED_WAIVER_REASON_OPTIONS_COUNT,
        CREATED_BY_NAME);
    addWaiverAssertions.shouldShowScopeOptions(SCOPE_APP, SCOPE_ORG, SCOPE_PARENT_ORG, SCOPE_ROOT_ORG);
    addWaiverAssertions.shouldShowComponentRadioLabels(component1Coords, allVersions, "All Components");
  }

  @Test
  @Category(SanityTest.class)
  public void testSubmit_createsWaiver() {
    playwrightRefreshOrOpen(AddWaiverPage.url(primaryViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.selectScope(SCOPE_APP);
    addWaiverPage.selectComponentRadio(0);
    addWaiverPage.fillComment(COMMENT);
    addWaiverPage.submit();
    playwrightWaitUntilUrlContains("/violation/");

    playwrightRefreshOrOpen(ViolationDetailsPage.url(primaryViolation.getId()));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    new ViolationDetailsPageAssertions(violationDetailsPage).shouldBeVisible();
    assertThat(violationDetailsPage.applicableWaiversTab()).containsText("Applicable");
    violationDetailsPage.applicableWaiversTab().click();
    assertThat(violationDetailsPage.applicableWaiversTile()).isVisible();
    assertThat(violationDetailsPage.applicableWaiversTile()).containsText(POLICY1_NAME);
    assertThat(violationDetailsPage.applicableWaiversTile()).containsText(COMMENT);
  }

  @Test
  @Category(SanityTest.class)
  public void testSubmit_duplicateShowsError() {
    AddWaiverPage addWaiverPage = new AddWaiverPage();

    playwrightRefreshOrOpen(AddWaiverPage.url(secondaryViolation.getId()));
    addWaiverPage.fillComment(COMMENT);
    addWaiverPage.submit();
    waitForSubmitMask();
    new AddWaiverPageAssertions(addWaiverPage).shouldHaveNoSubmitError();

    playwrightRefreshOrOpen(AddWaiverPage.url(secondaryViolation.getId()));
    addWaiverPage.fillComment(COMMENT);
    addWaiverPage.submit();
    waitForSubmitMask();
    new AddWaiverPageAssertions(addWaiverPage).shouldShowSubmitError();
  }

  @Test
  @Category(SanityTest.class)
  public void testSubmit_navigatesBackToViolationDetails() {
    playwrightRefreshOrOpen(AddWaiverPage.url(primaryViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.selectScope(SCOPE_APP);
    addWaiverPage.selectComponentRadio(2);
    addWaiverPage.fillComment(COMMENT);
    addWaiverPage.submit();
    waitForSubmitMask();
    new AddWaiverPageAssertions(addWaiverPage).shouldHaveNoSubmitError();

    playwrightWaitUntilUrlContains("/violation/" + primaryViolation.getId());
    ViolationDetailsPage finalViolationPage = new ViolationDetailsPage();
    new ViolationDetailsPageAssertions(finalViolationPage).shouldBeVisible();
  }
}
