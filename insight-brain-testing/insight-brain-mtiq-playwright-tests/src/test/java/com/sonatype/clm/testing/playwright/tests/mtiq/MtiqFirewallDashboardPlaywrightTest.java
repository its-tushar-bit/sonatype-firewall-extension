/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.FirewallDashboardRegressionAssertions;
import com.sonatype.clm.testing.playwright.pages.FirewallPage;
import com.sonatype.clm.testing.playwright.pages.FirewallRegressionPage;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.license.model.LicensedFeature;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Category(MtiqTest.class)
public class MtiqFirewallDashboardPlaywrightTest
    extends AbstractMtiqUiTest
{
  private static final Data DATA = TestDataManager.load("mtiq-firewall-dashboard", Data.class);

  private static final LocatorAssertions.HasCountOptions COUNT_OPTS =
      new LocatorAssertions.HasCountOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.HasCountOptions SLOW_COUNT_OPTS =
      new LocatorAssertions.HasCountOptions().setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS);

  private String containerWaiverPolicyName;

  private String filterAlphaArtifactId;

  private String filterBetaArtifactId;

  private String filterGammaArtifactId;

  private String filterAlphaPolicyName;

  private String filterAlphaActualRepoId;

  @Before
  public void enableFirewallFeaturesAndLoginAsAdmin() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
    setFeatures(
        LicensedFeature.FIREWALL,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE,
        LicensedFeature.RELEASE_INTEGRITY,
        LicensedFeature.DASHBOARD,
        LicensedFeature.WAIVERS_DASHBOARD,
        LicensedFeature.POLICY_WAIVERS,
        LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    playwrightLoginAdminAt(FirewallPage.url());
  }

  @After
  public void resetContainerImagesEvalFlag() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
  }

  @Test
  public void testMtiqFirewallDashboard_containerWaiverTable_nullExpiryDisplaysNever() {
    seedContainerWaivers();
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    // Full reload so the SPA re-fetches productFeatures with CONTAINER_IMAGES_EVAL_ENABLED.
    playwrightRefresh();
    playwrightNavigateTo(FirewallRegressionPage.waiversContainersApprovedUrl());

    FirewallRegressionPage firewallPage = new FirewallRegressionPage();
    Locator waiverRow = firewallPage.containerWaiverTableRows()
        .filter(new Locator.FilterOptions().setHasText(containerWaiverPolicyName));
    assertThat(waiverRow).hasCount(DATA.singleRowCount(), COUNT_OPTS);
    assertThat(firewallPage.containerWaiverExpiryCellIn(waiverRow)).containsText(DATA.waiverNeverExpiryText());
  }

  @Test
  public void testMtiqFirewallDashboard_quarantineTableFilters_eachFilterReducesVisibleRows() {
    seedQuarantineFilterComponents();
    // Full reload — FirewallPage's loadPolicies() runs in a mount-only useEffect, and hash-only
    // navigations (dashboard → quarantine tab) don't remount the SPA so cached empty state sticks.
    playwrightRefreshOrOpen(FirewallPage.quarantineTabUrl());
    playwrightRefresh();

    FirewallRegressionPage firewallPage = new FirewallRegressionPage();

    assertThat(firewallPage.quarantineTableRows()).hasCount(DATA.totalQuarantineRows(), SLOW_COUNT_OPTS);

    firewallPage.componentNameFilter().fill(filterAlphaArtifactId);
    assertThat(firewallPage.quarantineTableRows()).hasCount(DATA.quarantineFilteredRows(), SLOW_COUNT_OPTS);
    firewallPage.componentNameFilterClearButton().click();
    assertThat(firewallPage.quarantineTableRows()).hasCount(DATA.totalQuarantineRows(), SLOW_COUNT_OPTS);

    firewallPage.componentNameFilter().fill(filterBetaArtifactId);
    assertThat(firewallPage.quarantineTableRows()).hasCount(DATA.quarantineFilteredRows(), COUNT_OPTS);
    firewallPage.componentNameFilterClearButton().click();
    assertThat(firewallPage.quarantineTableRows()).hasCount(DATA.totalQuarantineRows(), COUNT_OPTS);

    firewallPage.componentNameFilter().fill(filterGammaArtifactId);
    assertThat(firewallPage.quarantineTableRows()).hasCount(DATA.quarantineFilteredRows(), COUNT_OPTS);
    firewallPage.componentNameFilterClearButton().click();
    assertThat(firewallPage.quarantineTableRows()).hasCount(DATA.totalQuarantineRows(), COUNT_OPTS);

    // Repository filter: alpha repo contains only the alpha component.
    firewallPage.repositoryFilter().fill(filterAlphaActualRepoId);
    assertThat(firewallPage.quarantineTableRows()).hasCount(DATA.quarantineFilteredRows(), COUNT_OPTS);
    firewallPage.repositoryFilterClearButton().click();
    assertThat(firewallPage.quarantineTableRows()).hasCount(DATA.totalQuarantineRows(), COUNT_OPTS);

    // "Past 30 days": beta was quarantined 200 days ago so it is excluded; alpha and gamma remain.
    firewallPage.quarantineTimeFilterToggle().click();
    firewallPage.quarantineTimeFilterOption(DATA.timeFilterPast30Days()).click();
    assertThat(firewallPage.quarantineTableRows()).hasCount(DATA.past30DaysQuarantineRows(), SLOW_COUNT_OPTS);
    firewallPage.quarantineTimeFilterToggle().click();
    firewallPage.quarantineTimeFilterOption(DATA.timeFilterAll()).click();
    assertThat(firewallPage.quarantineTableRows()).hasCount(DATA.totalQuarantineRows(), SLOW_COUNT_OPTS);

    firewallPage.componentNameFilter().fill(filterBetaArtifactId);
    assertThat(firewallPage.quarantineTableRows()).hasCount(DATA.quarantineFilteredRows(), COUNT_OPTS);
    firewallPage.componentNameFilterClearButton().click();
    assertThat(firewallPage.quarantineTableRows()).hasCount(DATA.totalQuarantineRows(), COUNT_OPTS);

    // Policy filter: alpha policy applies only to the alpha component.
    firewallPage.policyFilterToggle().click();
    firewallPage.policyFilterOption(filterAlphaPolicyName).click();
    assertThat(firewallPage.quarantineTableRows()).hasCount(DATA.quarantineFilteredRows(), COUNT_OPTS);
  }

  @Test
  public void testMtiqFirewallDashboard_limitedAccessUser_alertShownOnWaiversPage() {
    FirewallRegressionPage firewallPage = new FirewallRegressionPage();
    FirewallDashboardRegressionAssertions assertions = new FirewallDashboardRegressionAssertions(firewallPage);

    Organization childOrg = tempEntity.newOrganization();
    User limitedUser = tempEntity.newUser();
    tempEntity.newMembershipMapping(childOrg.getId(), Role.DEVELOPER_ROLE_ID, limitedUser.getUsername());
    playwrightLogout();
    // NEXUS-53680: LimitedFirewallAccessAlert is now on the Firewall Waivers page, not the dashboard.
    playwrightLoginAt(FirewallRegressionPage.waiversContainersApprovedUrl(), limitedUser.getUsername(),
        TemporaryEntity.USER_PASSWORD_CLEAR);
    playwrightRefresh();
    assertions.shouldShowLimitedAccessAlert();
  }

  private void seedContainerWaivers() {
    // NEXUS-53680: proxy repo → org.relatedRepositoryId → app; waiver needs isForContainerImage=true.
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository proxyRepo = tempEntity.newProxyRepository(
        repoManager, "cw-repo-" + TemporaryEntity.uuid(), DATA.dockerFormat(), true, true);
    Organization containerImageOrg = tempEntity.newOrganization();
    containerImageOrg.setRelatedRepositoryId(proxyRepo.getId());
    lookup(OrganizationDAO.class).update(containerImageOrg);
    Application containerImage = tempEntity.newApplication(containerImageOrg.getId());

    containerWaiverPolicyName = "cw-p1-" + TemporaryEntity.uuid();
    Policy policy = tempEntity.newPolicy(containerImage.getOrganizationId(), containerWaiverPolicyName);
    PolicyWaiver waiver = new PolicyWaiver(null, policy.getId(), containerImage.getId(), DATA.waiverComment());
    waiver.setForContainerImage(true);
    tempEntity.newWaiver(waiver);
  }

  private void seedQuarantineFilterComponents() {
    String uuid = TemporaryEntity.uuid();
    String suffix = "-" + uuid;
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repoAlpha = tempEntity.newRepository(repositoryManager, DATA.alphaRepoPublicId() + suffix, true, true);
    Repository repoBeta = tempEntity.newRepository(repositoryManager, DATA.betaRepoPublicId() + suffix, true, true);
    Repository repoGamma = tempEntity.newRepository(repositoryManager, DATA.gammaRepoPublicId() + suffix, true, true);

    filterAlphaActualRepoId = DATA.alphaRepoPublicId() + suffix;

    // Policies must be repo-scoped (not root org) to surface in /api/v2/policies for the admin.
    filterAlphaPolicyName = DATA.alphaPolicyName() + suffix;
    Policy policyAlpha = tempEntity.newPolicy(repoAlpha.getId(), filterAlphaPolicyName, DATA.violationThreatLevel());
    Policy policyBeta =
        tempEntity.newPolicy(repoBeta.getId(), DATA.betaPolicyName() + suffix, DATA.violationThreatLevel());
    Policy policyGamma =
        tempEntity.newPolicy(repoGamma.getId(), DATA.gammaPolicyName() + suffix, DATA.violationThreatLevel());

    Date now = Date.from(Instant.now());
    Date oldDate = Date.from(Instant.now().minus(DATA.oldQuarantineDays(), ChronoUnit.DAYS));

    filterAlphaArtifactId = DATA.alphaComponentNameFilter() + suffix;
    filterBetaArtifactId = DATA.betaComponentNameFilter() + suffix;
    filterGammaArtifactId = DATA.gammaComponentNameFilter() + suffix;

    ComponentIdentifier identifierAlpha =
        ComponentIdentifier.createMavenCoordinates(DATA.alphaComponentGroup(), filterAlphaArtifactId,
            DATA.componentVersion());
    ComponentIdentifier identifierBeta =
        ComponentIdentifier.createMavenCoordinates(DATA.betaComponentGroup(), filterBetaArtifactId,
            DATA.componentVersion());
    ComponentIdentifier identifierGamma =
        ComponentIdentifier.createMavenCoordinates(DATA.gammaComponentGroup(), filterGammaArtifactId,
            DATA.componentVersion());

    tempEntity.newRepositoryComponent(
        repoAlpha.getId(), MatchState.EXACT, "path-alpha" + suffix, "ha" + uuid.substring(0, 8), identifierAlpha, now,
        now);
    tempEntity.newRepositoryPolicyViolation(
        repoAlpha.getId(), DATA.violationThreatLevel(), "path-alpha" + suffix, false,
        FailActionType.ID, policyAlpha.getId(), filterAlphaPolicyName, identifierAlpha);

    tempEntity.newRepositoryComponent(
        repoBeta.getId(), MatchState.EXACT, "path-beta" + suffix, "hb" + uuid.substring(0, 8), identifierBeta, now,
        oldDate);
    tempEntity.newRepositoryPolicyViolation(
        repoBeta.getId(), DATA.violationThreatLevel(), "path-beta" + suffix, false,
        FailActionType.ID, policyBeta.getId(), DATA.betaPolicyName() + suffix, identifierBeta);

    tempEntity.newRepositoryComponent(
        repoGamma.getId(), MatchState.EXACT, "path-gamma" + suffix, "hg" + uuid.substring(0, 8), identifierGamma, now,
        now);
    tempEntity.newRepositoryPolicyViolation(
        repoGamma.getId(), DATA.violationThreatLevel(), "path-gamma" + suffix, false,
        FailActionType.ID, policyGamma.getId(), DATA.gammaPolicyName() + suffix, identifierGamma);
  }

  private record Data(
      String waiverNeverExpiryText,
      String waiverComment,
      String dockerFormat,
      String componentVersion,
      int singleRowCount,
      int quarantineFilteredRows,
      int violationThreatLevel,
      int oldQuarantineDays,
      String alphaRepoPublicId,
      String betaRepoPublicId,
      String alphaPolicyName,
      String betaPolicyName,
      String alphaComponentGroup,
      String alphaComponentNameFilter,
      String betaComponentGroup,
      String betaComponentNameFilter,
      String timeFilterPast30Days,
      String timeFilterAll,
      int totalQuarantineRows,
      int past30DaysQuarantineRows,
      String gammaRepoPublicId,
      String gammaPolicyName,
      String gammaComponentGroup,
      String gammaComponentNameFilter)
  {
  }
}
