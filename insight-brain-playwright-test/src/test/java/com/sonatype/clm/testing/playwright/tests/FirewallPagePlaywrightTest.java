/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.FirewallComponentDetailsPage;
import com.sonatype.clm.testing.playwright.pages.FirewallPage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.utils.PlaywrightWaitUtils;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

/**
 * <p>
 * Authoring rules: see {@code TestAuthourskill.md}. Backend setup is encapsulated in the nested
 * {@link FirewallPageSeeder} (§3c).
 */
public class FirewallPagePlaywrightTest
    extends AbstractIqUiTest
{
  private static final String REPOSITORY_MANAGER_INSTANCE_ID = "1";

  private static final String REPOSITORY_PUBLIC_ID = "maven-central";

  private static final String POLICY_NAME = "policyName";

  private static final int VIOLATION_COUNT = 5;

  private static final String COMPONENT_COORDINATE_1 = "g:a:1";

  private static final String COMPONENT_COORDINATE_2 = "g:a:2";

  private static final String COMPONENT_COORDINATE_3 = "g:a:3";

  private static final String COMPONENT_COORDINATE_4 = "g:a:4";

  private static final String EXPECTED_REPOSITORIES_PROTECTED_TEXT = "0 of 1 repositories protected";

  private static final String EXPECTED_COMPONENTS_MONITORED_TEXT = "4 components monitored";

  @Before
  public void openFirewallAsAdmin() {
    setFeatures(
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.COMPONENT_EVALUATION,
        LicensedFeature.REPOSITORY_REPORTS,
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE,
        LicensedFeature.RELEASE_INTEGRITY,
        LicensedFeature.DASHBOARD,
        LicensedFeature.WAIVERS_DASHBOARD,
        LicensedFeature.CONTAINER_IMAGES_EVALUATION);

    playwrightRefreshOrOpen(FirewallPage.url());
    playwrightLogin();
  }

  /**
   * Reset the system-config feature flag mutated in {@link #openFirewallAsAdmin()}. Skill §7b
   * requires any system-config / feature-flag toggle to be paired with an {@code @After} that
   * restores it, so the next test in the same fork starts from a known state.
   */
  @Before
  @After
  public void resetContainerImagesEvalFlag() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
  }

  @Test
  @Category(SanityTest.class)
  public void testFirewallPage_StatusPartiallyProtected() {
    seedRepositoryWithFourComponents();
    playwrightRefreshOrOpen(FirewallPage.url());

    FirewallPage firewallPage = new FirewallPage();
    assertThat(firewallPage.container()).isVisible();
    assertThat(firewallPage.firewallStatus()).isVisible();
    assertThat(firewallPage.statusFullyProtected()).isHidden();
    assertThat(firewallPage.statusPartiallyProtected()).isVisible();
    assertThat(firewallPage.statusPartiallyProtected())
        .containsText(EXPECTED_REPOSITORIES_PROTECTED_TEXT);
    assertThat(firewallPage.componentsMonitored())
        .containsText(EXPECTED_COMPONENTS_MONITORED_TEXT);
  }

  // Note: the previous testFirewallPage_AutoUnquarantinePageLoads and
  // testFirewallQuarantineTable_TableBodyCount tests were removed from this class because they
  // duplicated coverage already provided by FirewallAutoQuarantinePlaywrightTest:
  // - testFirewallPage_AutoUnquarantinePageLoads → FirewallAutoQuarantinePlaywrightTest#testQuarantineTabLoads
  // (both assert container().isVisible() + quarantineTable().isVisible())
  // - testFirewallQuarantineTable_TableBodyCount → FirewallAutoQuarantinePlaywrightTest#testQuarantineTableRowCount
  // (both assert quarantineTableRows().hasCount(<count>))
  // Both classes seed the same shape (1 repo + 4 components, 2 quarantined). The firewall-dashboard
  // tests below stay because they cover firewall-page-specific flows: the partially-protected
  // status banner and the row-click navigation to component details.

  @Test
  @Category(SanityTest.class)
  public void testRedirectToComponentDetailsPage() {
    seedRepositoryWithFourComponents();
    playwrightRefreshOrOpen(FirewallPage.url());

    FirewallPage firewallPage = new FirewallPage();
    FirewallComponentDetailsPage detailsPage = new FirewallComponentDetailsPage();
    firewallPage.componentDetailsLink(0).click();
    PlaywrightWaitUtils.waitForVisible(detailsPage.container(), PlaywrightTiming.ELEMENT_TIMEOUT_MS,
        PlaywrightTiming.POLL_INTERVAL_MS);

    assertThat(detailsPage.container()).isVisible();
  }

  /**
   * Seed a repository with four components: two quarantined+unquarantined and two
   * still-quarantined, matching the violation shape expected by the firewall dashboard tests.
   */
  private void seedRepositoryWithFourComponents() {
    Policy policy = tempEntity.newPolicy();
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager(REPOSITORY_MANAGER_INSTANCE_ID);
    Repository repository =
        tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true, false);

    ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    Date date1 = Date.from(LocalDateTime.now().withDayOfMonth(1).toInstant(offset));
    Date date2 = Date.from(LocalDateTime.now().withDayOfMonth(2).toInstant(offset));

    ProxyRepositoryComponent c1 = tempEntity.newRepositoryComponent(repository.getId(),
        COMPONENT_COORDINATE_1, date1, date1, true);
    tempEntity.newRepositoryPolicyViolation(c1, policy.getId());

    ProxyRepositoryComponent c2 = tempEntity.newRepositoryComponent(repository.getId(),
        COMPONENT_COORDINATE_2, date2, date2, true);
    tempEntity.newRepositoryPolicyViolation(c2, policy.getId());

    ProxyRepositoryComponent c3 = tempEntity.newRepositoryComponent(repository.getId(),
        COMPONENT_COORDINATE_3, date1, null, false);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), VIOLATION_COUNT,
        c3.getPathname(), false, FailActionType.ID, policy.getId(), POLICY_NAME,
        c3.getComponentIdentifier());

    ProxyRepositoryComponent c4 = tempEntity.newRepositoryComponent(repository.getId(),
        COMPONENT_COORDINATE_4, date2, null, false);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), VIOLATION_COUNT,
        c4.getPathname(), false, FailActionType.ID, policy.getId(), POLICY_NAME,
        c4.getComponentIdentifier());
  }
}
