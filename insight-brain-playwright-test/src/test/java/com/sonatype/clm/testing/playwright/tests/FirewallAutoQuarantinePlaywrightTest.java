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
import com.sonatype.clm.testing.playwright.pages.FirewallPage;
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
 * Playwright test for TC-26: verify the quarantine sub-tab of the Firewall dashboard.
 * <p>
 * Authoring rules: see {@code TestAuthourskill.md}. Backend setup is encapsulated in the nested
 * {@link FirewallAutoQuarantineSeeder} (§3c).
 */
public class FirewallAutoQuarantinePlaywrightTest
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

  private static final int EXPECTED_QUARANTINE_ROW_COUNT = 2;

  private static final String EXPECTED_FIRST_ROW_TEXT = "maven-central";

  @Before
  public void openQuarantineTabAsAdmin() {
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
        LicensedFeature.CONTAINER_IMAGES_EVALUATION);

    seedRepositoryWithFourComponents();

    playwrightRefreshOrOpen(FirewallPage.quarantineTabUrl());
    playwrightLogin();
  }

  /**
   * Reset the system-config flag mutated in {@link #openQuarantineTabAsAdmin()}.
   * Skill §7b: system-config toggles must be paired with an {@code @After} reset.
   */
  @Before
  @After
  public void resetContainerImagesEvalFlag() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
  }

  @Test
  @Category(SanityTest.class)
  public void testQuarantineTabLoads() {
    FirewallPage firewallPage = new FirewallPage();

    assertThat(firewallPage.container()).isVisible();
    assertThat(firewallPage.quarantineTable()).isVisible();
  }

  @Test
  @Category(SanityTest.class)
  public void testQuarantineTableRowCount() {
    FirewallPage firewallPage = new FirewallPage();

    assertThat(firewallPage.quarantineTableRows()).hasCount(EXPECTED_QUARANTINE_ROW_COUNT);
  }

  @Test
  @Category(SanityTest.class)
  public void testQuarantineTableRowContent() {
    FirewallPage firewallPage = new FirewallPage();

    assertThat(firewallPage.quarantineTableRow(0))
        .containsText(EXPECTED_FIRST_ROW_TEXT);
  }

  /**
   * Seed 1 repository with 4 components: 2 quarantined (quarantineTime set, no unquarantineTime)
   * and 2 non-quarantined (no quarantineTime at all). The quarantine dashboard tab only counts
   * components where quarantine_time IS NOT NULL AND unquarantine_time IS NULL.
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

    // Quarantined: quarantineTime set, unquarantineTime null → appears in the quarantine table.
    // The quarantine list query requires action_type_id = 'fail', so FailActionType.ID is mandatory.
    ProxyRepositoryComponent c1 = tempEntity.newRepositoryComponent(repository.getId(),
        COMPONENT_COORDINATE_1, date1, null, false);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), VIOLATION_COUNT,
        c1.getPathname(), false, FailActionType.ID, policy.getId(), POLICY_NAME,
        c1.getComponentIdentifier());

    ProxyRepositoryComponent c2 = tempEntity.newRepositoryComponent(repository.getId(),
        COMPONENT_COORDINATE_2, date2, null, false);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), VIOLATION_COUNT,
        c2.getPathname(), false, FailActionType.ID, policy.getId(), POLICY_NAME,
        c2.getComponentIdentifier());

    // Not quarantined: quarantineTime = null → must NOT appear in the quarantine table.
    ProxyRepositoryComponent c3 = tempEntity.newRepositoryComponent(repository.getId(),
        COMPONENT_COORDINATE_3);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), VIOLATION_COUNT,
        c3.getPathname(), false, FailActionType.ID, policy.getId(), POLICY_NAME,
        c3.getComponentIdentifier());

    ProxyRepositoryComponent c4 = tempEntity.newRepositoryComponent(repository.getId(),
        COMPONENT_COORDINATE_4);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), VIOLATION_COUNT,
        c4.getPathname(), false, FailActionType.ID, policy.getId(), POLICY_NAME,
        c4.getComponentIdentifier());
  }
}
