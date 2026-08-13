/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.net.URI;
import java.util.Date;
import java.util.List;

import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.AddWaiverPage;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.FirewallComponentDetailsPage;
import com.sonatype.clm.testing.playwright.pages.ViolationDetailsPage;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.license.model.LicensedFeature;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.pages.ListWaiversTablePage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ViolationDetailsPlaywrightTest
    extends AbstractIqUiTest
{
  private static final ViolationDetailsData DATA =
      TestDataManager.load("violation-details", ViolationDetailsData.class);

  private PolicyViolation policyViolation;

  @Before
  public void seedViolationAndOpenAsAdmin() {
    seedOrgAppAndViolation(DATA);
    stubHdsVulnerabilityDetails();

    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  @Test
  @Category(SanityTest.class)
  public void testDetails() {
    ViolationDetailsPage detailsPage = openViolationDetails();

    assertThat(detailsPage.componentName()).containsText(DATA.expectedComponentDisplay());
    assertThat(detailsPage.policyName()).containsText(DATA.policyName());
  }

  @Test
  @Category(SanityTest.class)
  public void testPolicyViolationInfo() {
    ViolationDetailsPage detailsPage = openViolationDetails();

    assertThat(detailsPage.constraintSection()).isVisible();
    assertThat(detailsPage.conditionsSection()).isVisible();
  }

  @Test
  @Category(SanityTest.class)
  public void testGoDirectlyToAddWaiver() {
    ViolationDetailsPage detailsPage = openViolationDetails();
    assertThat(detailsPage.addWaiverButton()).isVisible();

    detailsPage.addWaiverButton().click();

    playwrightWaitUntilUrlContains("/addWaiver/" + violationId());
    assertThat(new AddWaiverPage().container()).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testInvalidViolationGuard_warningWhenViolationIdMissing() {
    playwrightRefreshOrOpen(DATA.violationPageUrlPrefix());
    playwrightRefresh();
    ViolationDetailsPage detailsPage = new ViolationDetailsPage();
    assertThat(detailsPage.container()).isVisible();
    assertThat(detailsPage.warningAlert()).isVisible();
    assertThat(detailsPage.warningAlert()).containsText(DATA.missingViolationIdMessage());
    assertThat(detailsPage.detailsTile()).not().isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testVulnerabilityDetailsTab_hiddenWhenNoVulnerabilityDetails() {
    String licenseViolationId = seedLicenseViolation();
    playwrightRefreshOrOpen(ViolationDetailsPage.url(licenseViolationId));
    playwrightRefresh();

    ViolationDetailsPage detailsPage = new ViolationDetailsPage();
    assertThat(detailsPage.container()).isVisible();
    assertThat(detailsPage.securityTab()).not().isVisible();
    assertThat(detailsPage.applicableWaiversTab()).isVisible();
    assertThat(detailsPage.similarWaiversTab()).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testVulnerabilityDetailsTab_presentWhenDetailsLoaded() {
    ViolationDetailsPage detailsPage = openViolationDetails();
    assertThat(detailsPage.applicableWaiversTab()).isVisible();
    assertThat(detailsPage.similarWaiversTab()).isVisible();
    assertThat(detailsPage.securityTab()).isVisible(
        new IsVisibleOptions().setTimeout(DATA.hdsWaitTimeoutMs()));
  }

  @Test
  @Category(RegressionTest.class)
  public void testWaiverCounterBadge_onApplicableWaiversTab() {
    String waiverViolationId = seedViolationWithWaivers();
    playwrightRefreshOrOpen(ViolationDetailsPage.url(waiverViolationId));
    playwrightRefresh();

    ViolationDetailsPage detailsPage = new ViolationDetailsPage();
    detailsPage.container()
        .waitFor(new Locator.WaitForOptions().setTimeout(DATA.hdsWaitTimeoutMs()));
    assertThat(detailsPage.container()).isVisible();
    assertThat(detailsPage.applicableWaiversBadge()).isVisible();
    assertThat(detailsPage.applicableWaiversBadge())
        .containsText(String.valueOf(DATA.waiverCount()));
  }

  @Test
  @Category(RegressionTest.class)
  public void testSimilarWaiversFilterDropdown_threeOptions() {
    ViolationDetailsPage detailsPage = openViolationDetails();
    detailsPage.openSimilarWaiversTab();

    assertThat(detailsPage.similarWaiversFilterDropdown()).isVisible();
    detailsPage.similarWaiversFilterToggle().click();
    assertThat(detailsPage.similarWaiversFilterOptions()).hasCount(DATA.similarWaiversFilterOptions().size());
    for (String option : DATA.similarWaiversFilterOptions()) {
      assertThat(detailsPage.similarWaiversFilterOptions()
          .filter(new Locator.FilterOptions().setHasText(option))).hasCount(1);
    }
  }

  @Test
  @Category(RegressionTest.class)
  public void testSimilarWaiversSubtitle_variesByVulnerability() {
    String licenseViolationId = seedLicenseViolation();
    playwrightRefreshOrOpen(ViolationDetailsPage.url(licenseViolationId));
    playwrightRefresh();
    playwrightWaitUntilUrlContains("/violation/" + licenseViolationId);

    ViolationDetailsPage detailsPage = new ViolationDetailsPage();
    assertThat(detailsPage.container()).isVisible();
    detailsPage.openSimilarWaiversTab();
    assertThat(detailsPage.similarWaiversSubtitle())
        .hasText(DATA.similarWaiversSubtitleNonSecurity());

    playwrightRefreshOrOpen(ViolationDetailsPage.url(violationId()));
    playwrightRefresh();
    playwrightWaitUntilUrlContains("/violation/" + violationId());

    detailsPage = new ViolationDetailsPage();
    assertThat(detailsPage.container()).isVisible();
    detailsPage.openSimilarWaiversTab();
    assertThat(detailsPage.similarWaiversSubtitle())
        .containsText(DATA.similarWaiversSubtitleSecurityPrefix());
    assertThat(detailsPage.similarWaiversSubtitle())
        .containsText(DATA.vulnerabilityRefId());
  }

  @Test
  @Category(RegressionTest.class)
  public void testFirewallContext_constraintViolationsRender() {
    setFeatures(
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE,
        LicensedFeature.RELEASE_INTEGRITY,
        LicensedFeature.DASHBOARD);

    ProxyRepositoryComponent component = seedFirewallViolation();
    playwrightRefreshOrOpen(
        FirewallComponentDetailsPage.urlViolationsTab(component));
    playwrightRefresh();

    FirewallComponentDetailsPage fwPage =
        new FirewallComponentDetailsPage();
    fwPage.container().waitFor();
    assertThat(fwPage.policyViolationsTable()).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testStandaloneMode_backButtonPresent() {
    ViolationDetailsPage detailsPage = openViolationDetails();
    assertThat(detailsPage.backButton()).isVisible();
    assertThat(detailsPage.popoverSection()).not().isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testApplicableWaiversBadge_clickOpensListWaiversTable() {
    String violationId = seedViolationWithWaivers();
    playwrightRefreshOrOpen(ViolationDetailsPage.url(violationId));
    playwrightRefresh();

    ViolationDetailsPage detailsPage = new ViolationDetailsPage();
    detailsPage.container()
        .waitFor(new Locator.WaitForOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(detailsPage.applicableWaiversBadge()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    detailsPage.applicableWaiversBadge().click();

    ListWaiversTablePage listPage = new ListWaiversTablePage();
    assertThat(listPage.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(listPage.activeWaiverRows()).hasCount(DATA.waiverCount());
  }

  private ViolationDetailsPage openViolationDetails() {
    String url = ViolationDetailsPage.url(violationId());
    playwrightRefreshOrOpen(url);
    playwrightRefresh();
    ViolationDetailsPage detailsPage = new ViolationDetailsPage();
    detailsPage.container()
        .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    return detailsPage;
  }

  private void stubHdsVulnerabilityDetails() {
    URI uri = UriBuilder.fromPath("rest/vulnerability/details/json/{refId}").build(DATA.vulnerabilityRefId());
    testCLMServer.getHdsServer()
        .respondWith(getClass().getClassLoader().getResource("vulnerabilityDetails/vulnerabilityDetails2.json"))
        .atUri(uri);
  }

  private void seedOrgAppAndViolation(ViolationDetailsData data) {
    String suffix = TemporaryEntity.uuid();
    String orgName = data.organizationNamePrefix() + "-" + suffix;
    String appName = data.applicationNamePrefix() + "-" + suffix;
    String appPublicId = data.applicationPublicIdPrefix() + "-" + suffix;

    Organization organization = tempEntity.newOrganization(orgName);
    Application application = tempEntity.newApplication(appName, appPublicId, organization.getId());

    Policy securityPolicy = tempEntity.newPolicy(
        Organization.ROOT_ORGANIZATION_ID, data.policyName(), data.policyThreatLevel());

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(), StageTypes.BUILD.getId(), data.scanId());

    policyViolation = tempEntity.newPolicyViolation(
        policyEvaluation,
        securityPolicy,
        data.componentGroup(),
        data.componentArtifact(),
        data.componentVersion(),
        data.componentHash(),
        data.vulnerabilityRefId());
  }

  private String violationId() {
    return policyViolation.getId();
  }

  private String seedLicenseViolation() {
    String suffix = TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization(DATA.organizationNamePrefix() + "-lic-" + suffix);
    Application app = tempEntity.newApplication(
        DATA.applicationNamePrefix() + "-lic-" + suffix,
        DATA.applicationPublicIdPrefix() + "-lic-" + suffix,
        org.getId());

    Policy licensePolicy = tempEntity.newPolicy(
        Organization.ROOT_ORGANIZATION_ID, DATA.licensePolicyName(), DATA.licensePolicyThreatLevel());

    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.BUILD.getId(), DATA.licenseScanIdPrefix() + "-" + suffix);

    PolicyViolation violation = tempEntity.newPolicyViolation(
        eval, licensePolicy, DATA.licensePolicyThreatLevel(), PolicyThreatCategory.LICENSE,
        DATA.componentGroup(), DATA.componentArtifact(), DATA.componentVersion(),
        DATA.componentHash(), null);
    return violation.getId();
  }

  private String seedViolationWithWaivers() {
    String suffix = TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization(DATA.organizationNamePrefix() + "-wv-" + suffix);
    Application app = tempEntity.newApplication(
        DATA.applicationNamePrefix() + "-wv-" + suffix,
        DATA.applicationPublicIdPrefix() + "-wv-" + suffix,
        org.getId());

    Policy policy = tempEntity.newPolicy(
        Organization.ROOT_ORGANIZATION_ID, DATA.policyName() + "-wv", DATA.policyThreatLevel());

    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.BUILD.getId(), DATA.scanId() + "-wv-" + suffix);

    PolicyViolation violation = tempEntity.newPolicyViolation(
        eval, policy,
        DATA.componentGroup(), DATA.componentArtifact(), DATA.componentVersion(),
        DATA.componentHash(), DATA.vulnerabilityRefId());

    List<ConstraintFact> violationConstraints = violation.getConstraintFacts();
    String[] ownerIds = {org.getId(), app.getId(), Organization.ROOT_ORGANIZATION_ID};
    for (int i = 0; i < DATA.waiverCount(); i++) {
      tempEntity.newWaiver(
          DATA.componentHash(),
          violation.getPolicyId(),
          ownerIds[i % ownerIds.length],
          violationConstraints,
          DATA.waiverComment() + "-" + i);
    }
    return violation.getId();
  }

  private ProxyRepositoryComponent seedFirewallViolation() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager(DATA.firewallRepositoryManagerInstanceId());
    Repository repository =
        tempEntity.newRepository(repositoryManager, DATA.firewallRepositoryPublicId(), true, false);

    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(
        repository.getId(), MatchState.EXACT,
        DATA.firewallComponentPathname(), DATA.componentHash(),
        com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates(
            DATA.componentGroup(), DATA.componentArtifact(), DATA.componentVersion()),
        new Date(), new Date());

    Policy fwPolicy = tempEntity.newPolicy(
        Organization.ROOT_ORGANIZATION_ID, "FW-Policy", DATA.firewallPolicyThreatLevel());

    ConstraintFact constraintFact = new ConstraintFact(
        TemporaryEntity.uuid(), DATA.firewallConstraintName(), LogicalOperator.AND.name());
    constraintFact.addConditionFact(
        new ConditionFact(MatchStateConditionType.ID, 0, "summary", DATA.firewallConstraintReason()));

    ProxyRepositoryPolicyViolation violation = new ProxyRepositoryPolicyViolation(
        component.getRepositoryId(), component.getPathname(), new Date(),
        fwPolicy.getId(), fwPolicy.getName(), DATA.firewallPolicyThreatLevel(),
        PolicyThreatCategory.SECURITY, component.getHash(), component.getComponentIdentifier(),
        List.of(constraintFact));
    violation.setActionTypeId(FailActionType.ID);
    tempEntity.newRepositoryPolicyViolation(violation);
    return component;
  }

  private record ViolationDetailsData(
      String organizationNamePrefix,
      String applicationNamePrefix,
      String applicationPublicIdPrefix,
      String policyName,
      int policyThreatLevel,
      String scanId,
      String componentGroup,
      String componentArtifact,
      String componentVersion,
      String componentHash,
      String vulnerabilityRefId,
      String expectedComponentDisplay,
      String missingViolationIdMessage,
      String licensePolicyName,
      int licensePolicyThreatLevel,
      String licenseScanIdPrefix,
      String waiverComment,
      int waiverCount,
      List<String> similarWaiversFilterOptions,
      String similarWaiversSubtitleNonSecurity,
      String similarWaiversSubtitleSecurityPrefix,
      String firewallRepositoryManagerInstanceId,
      String firewallRepositoryPublicId,
      String firewallComponentPathname,
      int firewallPolicyThreatLevel,
      String firewallConstraintName,
      String firewallConstraintReason,
      String violationPageUrlPrefix,
      String addWaiverUrlFragment,
      long hdsWaitTimeoutMs,
      String conditionFactSummary)
  {
  }
}
