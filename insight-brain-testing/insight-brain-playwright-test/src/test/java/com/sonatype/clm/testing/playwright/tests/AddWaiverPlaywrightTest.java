/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.AddWaiverPage;
import com.sonatype.clm.testing.playwright.pages.AddWaiverPageAssertions;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.ViolationDetailsPage;
import com.sonatype.clm.testing.playwright.pages.ViolationDetailsPageAssertions;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.license.model.LicensedFeature;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

public class AddWaiverPlaywrightTest
    extends AbstractIqUiTest
{
  private static final Data DATA = TestDataManager.load("add-waiver", Data.class);

  private Application application;

  private PolicyViolation primaryViolation;

  private PolicyViolation secondaryViolation;

  private PolicyViolation noComponentIdViolation;

  private String scopeAppLabel;

  private String scopeOrgLabel;

  private String scopeParentOrgLabel;

  @Before
  public void setUp() {
    String suffix = TemporaryEntity.uuid();
    String parentOrgName = DATA.parentOrgNamePrefix() + "-" + suffix;
    String orgName = DATA.orgNamePrefix() + "-" + suffix;
    String appName = DATA.appNamePrefix() + "-" + suffix;

    Organization parentOrg = tempEntity.newOrganization(parentOrgName);
    Organization org = tempEntity.newOrganization(orgName, parentOrg);
    application = tempEntity.newApplication(appName, DATA.appIdPrefix() + "-" + suffix, org.getId());

    scopeAppLabel = "Application - " + appName;
    scopeOrgLabel = "Organization - " + orgName;
    scopeParentOrgLabel = "Organization - " + parentOrgName;

    Policy policy1 =
        tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, DATA.policy1Name(), DATA.policy1ThreatLevel());
    Policy policy2 =
        tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, DATA.policy2Name(), DATA.policy2ThreatLevel());

    Date twoDaysAgo = Date.from(Instant.now().minus(2, ChronoUnit.DAYS));
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        application.getId(), StageTypes.BUILD.getId(), DATA.scanId(), false, false, twoDaysAgo);

    primaryViolation = tempEntity.newPolicyViolation(evaluation, policy1,
        DATA.component1GroupId(), DATA.component1ArtifactId(), DATA.component1Version(),
        DATA.component1Hash(), DATA.component1CveId());

    secondaryViolation = tempEntity.newPolicyViolation(evaluation, policy2,
        DATA.component2GroupId(), DATA.component2ArtifactId(), DATA.component2Version(),
        DATA.component2Hash(), (String) null);

    noComponentIdViolation = tempEntity.newPolicyViolation(
        evaluation, policy1, DATA.policy1ThreatLevel(), PolicyThreatCategory.SECURITY);

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
        DATA.component1GroupId() + " : " + DATA.component1ArtifactId() + " : " + DATA.component1Version();
    String allVersions = DATA.component1GroupId() + " : " + DATA.component1ArtifactId() + " (all versions)";

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    AddWaiverPageAssertions addWaiverAssertions = new AddWaiverPageAssertions(addWaiverPage);
    addWaiverAssertions.shouldShowPageLayout(
        DATA.component1ArtifactId(),
        component1Coords,
        DATA.policy1Name(),
        DATA.constraintName(),
        DATA.component1CveId(),
        DATA.vulnerabilityDetailsLinkText(),
        DATA.expectedScopeCount(),
        DATA.expectedComponentRadioCount(),
        DATA.expectedExpiryOptionsCount(),
        DATA.expectedWaiverReasonOptionsCount(),
        DATA.createdByName());
    addWaiverAssertions.shouldShowScopeOptions(scopeAppLabel, scopeOrgLabel, scopeParentOrgLabel,
        DATA.scopeRootOrg());
    addWaiverAssertions.shouldShowComponentRadioLabels(component1Coords, allVersions, "All Components");
  }

  @Test
  @Category(SanityTest.class)
  public void testSubmit_createsWaiver() {
    playwrightRefreshOrOpen(AddWaiverPage.url(primaryViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.container().waitFor();
    addWaiverPage.selectScope(scopeAppLabel);
    addWaiverPage.selectComponentRadio(0);
    addWaiverPage.fillComment(DATA.comment());
    addWaiverPage.submit();
    playwrightWaitUntilUrlContains("/violation/");

    playwrightRefreshOrOpen(ViolationDetailsPage.url(primaryViolation.getId()));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    new ViolationDetailsPageAssertions(violationDetailsPage).shouldBeVisible();
    assertThat(violationDetailsPage.applicableWaiversTab()).containsText("Applicable");
    violationDetailsPage.applicableWaiversTab().click();
    assertThat(violationDetailsPage.applicableWaiversTile()).isVisible();
    assertThat(violationDetailsPage.applicableWaiversTile()).containsText(DATA.policy1Name());
    assertThat(violationDetailsPage.applicableWaiversTile()).containsText(DATA.comment());
  }

  @Test
  @Category(SanityTest.class)
  public void testSubmit_duplicateShowsError() {
    AddWaiverPage addWaiverPage = new AddWaiverPage();

    playwrightRefreshOrOpen(AddWaiverPage.url(secondaryViolation.getId()));
    addWaiverPage.fillComment(DATA.comment());
    addWaiverPage.submit();
    waitForSubmitMask();
    new AddWaiverPageAssertions(addWaiverPage).shouldHaveNoSubmitError();

    playwrightRefreshOrOpen(AddWaiverPage.url(secondaryViolation.getId()));
    addWaiverPage.fillComment(DATA.comment());
    addWaiverPage.submit();
    waitForSubmitMask();
    new AddWaiverPageAssertions(addWaiverPage).shouldShowSubmitError();
  }

  @Test
  @Category(SanityTest.class)
  public void testSubmit_navigatesBackToViolationDetails() {
    playwrightRefreshOrOpen(AddWaiverPage.url(primaryViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.selectScope(scopeAppLabel);
    addWaiverPage.selectComponentRadio(2);
    addWaiverPage.fillComment(DATA.comment());
    addWaiverPage.submit();
    waitForSubmitMask();
    new AddWaiverPageAssertions(addWaiverPage).shouldHaveNoSubmitError();

    playwrightWaitUntilUrlContains("/violation/" + primaryViolation.getId());
    ViolationDetailsPage finalViolationPage = new ViolationDetailsPage();
    new ViolationDetailsPageAssertions(finalViolationPage).shouldBeVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testCustomExpirationDatePicker() {
    playwrightRefreshOrOpen(AddWaiverPage.url(primaryViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    AddWaiverPageAssertions assertions = new AddWaiverPageAssertions(addWaiverPage);
    addWaiverPage.expiryTimeSelect().waitFor();
    addWaiverPage.selectExpiryTime(DATA.customExpiryOptionLabel());
    assertions.shouldShowCustomExpiryDateInput();
    String futureDate = java.time.LocalDate.now().plusDays(30).toString();
    addWaiverPage.fillCustomExpiryDate(futureDate);

    assertThat(addWaiverPage.expiryTimeMessage()).containsText("This waiver will expire in");
  }

  @Test
  @Category(RegressionTest.class)
  public void testComments_maxLengthEnforced() {
    playwrightRefreshOrOpen(AddWaiverPage.url(primaryViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.container().waitFor();
    String longComment = "x".repeat(DATA.commentsMaxLength() + 100);
    addWaiverPage.fillComment(longComment);
    assertThat(addWaiverPage.comments()).hasValue(longComment.substring(0, DATA.commentsMaxLength()));
  }

  @Test
  @Category(RegressionTest.class)
  public void testVulnerabilityDetailsLink_hiddenForNonSecurityViolation() {
    playwrightRefreshOrOpen(AddWaiverPage.url(secondaryViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.container().waitFor();
    new AddWaiverPageAssertions(addWaiverPage).shouldNotShowVulnerabilityDetailsLink();
  }

  @Test
  @Category(RegressionTest.class)
  public void testCancelButton_navigatesBackWithoutCreatingWaiver() {
    playwrightRefreshOrOpen(AddWaiverPage.url(primaryViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.container().waitFor();
    addWaiverPage.clickCancel();
    playwrightWaitUntilUrlContains("/violation/" + primaryViolation.getId());

    ViolationDetailsPage violationPage = new ViolationDetailsPage();
    assertThat(violationPage.container()).isVisible();
    assertThat(addWaiverPage.container()).not().isVisible();

    PolicyWaiverDAO waiverDAO = lookup(PolicyWaiverDAO.class);
    List<PolicyWaiver> waivers =
        waiverDAO.getApplicableToComponent(application.getId(), DATA.component1Hash());
    Assertions.assertThat(waivers).isEmpty();
  }

  @Test
  @Category(RegressionTest.class)
  public void testScopeDropdown_requiredAndPreSelected() {
    playwrightRefreshOrOpen(AddWaiverPage.url(primaryViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    AddWaiverPageAssertions assertions = new AddWaiverPageAssertions(addWaiverPage);
    addWaiverPage.container().waitFor();
    assertions.shouldShowScopeAsRequired();
    assertions.shouldHaveScopePreSelected();
  }

  @Test
  @Category(RegressionTest.class)
  public void testComponentRadio_allVersionsDisabledWhenComponentIdentifierNull() {
    playwrightRefreshOrOpen(AddWaiverPage.url(noComponentIdViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.container().waitFor();
    addWaiverPage.allVersionsRadio().waitFor();
    new AddWaiverPageAssertions(addWaiverPage).shouldShowAllVersionsRadioDisabled();
  }

  /** Guards against accidentally adding a tier gate to manual waiver creation. */
  @Test
  @Category(RegressionTest.class)
  public void testSubmit_createsWaiverUnderProLicense() {
    setMissingFeature(LicensedFeature.AUTO_WAIVER_MANAGEMENT);
    playwrightRefreshOrOpen(AddWaiverPage.url(primaryViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.container().waitFor();
    addWaiverPage.selectScope(scopeAppLabel);
    addWaiverPage.selectComponentRadio(0);
    addWaiverPage.fillComment(DATA.comment());
    addWaiverPage.submit();
    playwrightWaitUntilUrlContains("/violation/");

    playwrightRefreshOrOpen(ViolationDetailsPage.url(primaryViolation.getId()));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    new ViolationDetailsPageAssertions(violationDetailsPage).shouldBeVisible();
    violationDetailsPage.applicableWaiversTab().click();
    assertThat(violationDetailsPage.applicableWaiversTile()).containsText(DATA.policy1Name());
    assertThat(violationDetailsPage.applicableWaiversTile()).containsText(DATA.comment());
  }

  private record Data(
      String parentOrgName,
      String parentOrgNamePrefix,
      String orgName,
      String orgNamePrefix,
      String appName,
      String appNamePrefix,
      String appId,
      String appIdPrefix,

      String policy1Name,
      int policy1ThreatLevel,
      String policy2Name,
      int policy2ThreatLevel,

      String component1GroupId,
      String component1ArtifactId,
      String component1Version,
      String component1Hash,
      String component1CveId,

      String component2GroupId,
      String component2ArtifactId,
      String component2Version,
      String component2Hash,
      String component2CveId,

      String licenseComponent3GroupId,
      String licenseComponent3ArtifactId,
      String licenseComponent3Version,
      String licenseComponent3Hash,
      String licensePolicyName,
      int licensePolicyThreatLevel,

      String unknownComponentHash,
      String unknownComponentCveId,
      String unknownComponentAllVersionsLabel,

      String constraintName,
      String vulnerabilityDetailsLinkText,
      String comment,
      String createdByName,
      String submitSuccessComment,
      String scanId,

      int expectedScopeCount,
      int expectedComponentRadioCount,
      int expectedExpiryOptionsCount,
      int expectedWaiverReasonOptionsCount,

      String scopeApp,
      String scopeOrg,
      String scopeParentOrg,
      String scopeRootOrg,
      String scopeFieldLabel,
      String scopeRequiredIndicator,
      String rootOrganizationScopeLabel,

      String customExpiryOptionLabel,
      String customExpiryOptionValue,
      String pastDate,
      int commentsMaxLength,

      String allComponentsRadioLabel,
      String allVersionsDisabledTooltip,
      String dashboardWaiversPolicyColumn)
  {
  }
}
