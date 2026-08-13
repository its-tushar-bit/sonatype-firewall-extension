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
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.FirewallComponentDetailsPage;
import com.sonatype.clm.testing.playwright.pages.FirewallPage;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.utils.FirewallComponentDetailsHdsStub;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

/**
 * Playwright test for TC-31: verify the Policy Violations tab on the Firewall Component Details page.
 * <p>
 * Authoring rules: see {@code TestAuthourskill.md}. Backend setup is encapsulated in the nested
 * {@link FirewallPolicyViolationsSeeder} (§3c).
 */
public class FirewallPolicyViolationsPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String REPOSITORY_MANAGER_INSTANCE_ID = "1";

  private static final String REPOSITORY_PUBLIC_ID = "maven-central";

  private static final String COMPONENT_HASH = "abc123";

  private static final String COMPONENT_PATHNAME = "g/a/1/a-1.jar";

  private static final int HIGH_POLICY_THREAT_LEVEL = 8;

  private static final String HIGH_POLICY_CONSTRAINT_NAME = "High CVE";

  private static final String HIGH_POLICY_CONSTRAINT_REASON = "High CVSS score";

  private static final int LOW_POLICY_THREAT_LEVEL = 3;

  private static final String LOW_POLICY_CONSTRAINT_NAME = "Low CVE";

  private static final String LOW_POLICY_CONSTRAINT_REASON = "Low CVSS score";

  private static final int EXPECTED_VIOLATION_ROW_COUNT = 2;

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

  @Before
  @After
  public void resetContainerImagesEvalFlag() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
  }

  /**
   * Verify that the Policy Violations tab content area and violations table are visible
   * after navigating directly to the violations tab URL for a seeded quarantined component.
   */
  @Test
  @Category(SanityTest.class)
  public void testPolicyViolationsTabLoads() {
    ProxyRepositoryComponent component = seedComponentWithTwoViolations();
    playwrightRefreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));

    FirewallComponentDetailsPage detailsPage = new FirewallComponentDetailsPage();
    assertThat(detailsPage.container()).isVisible();
    assertThat(detailsPage.policyViolationsTable()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  /**
   * Verify that the Policy Violations table shows exactly the expected number of seeded rows.
   */
  @Test
  @Category(SanityTest.class)
  public void testPolicyViolationsTableRowCount() {
    ProxyRepositoryComponent component = seedComponentWithTwoViolations();
    playwrightRefreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));

    FirewallComponentDetailsPage detailsPage = new FirewallComponentDetailsPage();
    assertThat(detailsPage.policyViolationRows()).hasCount(EXPECTED_VIOLATION_ROW_COUNT);
  }

  private ProxyRepositoryComponent seedComponentWithTwoViolations() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager(REPOSITORY_MANAGER_INSTANCE_ID);
    Repository repository =
        tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true, false);

    ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    Date evalTime = Date.from(LocalDateTime.now().withDayOfMonth(1).toInstant(offset));
    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT,
        COMPONENT_PATHNAME,
        COMPONENT_HASH,
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"),
        evalTime,
        evalTime);

    Policy highPolicy = tempEntity.newPolicy();
    Policy lowPolicy = tempEntity.newPolicy();

    newPolicyViolation(component, highPolicy, FailActionType.ID, HIGH_POLICY_THREAT_LEVEL,
        HIGH_POLICY_CONSTRAINT_NAME, HIGH_POLICY_CONSTRAINT_REASON);
    newPolicyViolation(component, lowPolicy, WarnActionType.ID, LOW_POLICY_THREAT_LEVEL,
        LOW_POLICY_CONSTRAINT_NAME, LOW_POLICY_CONSTRAINT_REASON);

    FirewallComponentDetailsHdsStub.stubRepositoryComponentDetails(
        testCLMServer.getHdsServer(), component);
    return component;
  }

  private ProxyRepositoryPolicyViolation newPolicyViolation(
      ProxyRepositoryComponent component,
      Policy policy,
      String actionId,
      int threatLevel,
      String constraintName,
      String constraintReason)
  {
    ConstraintFact constraintFact =
        new ConstraintFact(java.util.UUID.randomUUID().toString(), constraintName, LogicalOperator.AND.name());
    constraintFact.addConditionFact(new ConditionFact(MatchStateConditionType.ID, 0, "summary", constraintReason));

    ProxyRepositoryPolicyViolation violation = new ProxyRepositoryPolicyViolation(component.getRepositoryId(),
        component.getPathname(), new Date(), policy.getId(), policy.getName(), threatLevel,
        PolicyThreatCategory.SECURITY, component.getHash(), component.getComponentIdentifier(),
        List.of(constraintFact));
    violation.setActionTypeId(actionId);
    tempEntity.newRepositoryPolicyViolation(violation);
    return violation;
  }
}
