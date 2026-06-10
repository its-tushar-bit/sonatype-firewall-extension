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
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.pages.FirewallComponentDetailsPage;
import com.sonatype.clm.testing.playwright.pages.FirewallPage;
import com.sonatype.clm.testing.playwright.utils.FirewallComponentDetailsHdsStub;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.utils.PlaywrightWaitUtils;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.license.model.LicensedFeature;

import com.microsoft.playwright.assertions.LocatorAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class FirewallComponentDetailsPlaywrightTest
    extends AbstractIqUiTest
{

  private static final String REPOSITORY_MANAGER_INSTANCE_ID = "1";

  private static final String REPOSITORY_PUBLIC_ID = "maven-central";

  private static final String POLICY_NAME = "policyName";

  private static final int VIOLATION_COUNT = 5;

  private static final String COMPONENT_HASH = "pwfwcmpdetailshash01";

  private static final String COMPONENT_PATHNAME = "g/a/v/g-a-v.jar";

  private static final String MAVEN_FORMAT = "maven";

  private static final String MAVEN_GROUP_ID = "g";

  private static final String MAVEN_ARTIFACT_ID = "a";

  private static final String MAVEN_VERSION = "v";

  private static final String LABEL_MATCH_STATE = "Match State";

  private static final String LABEL_IDENTIFICATION_SOURCE = "Identification Source";

  private static final String LABEL_CATEGORY = "Category";

  private static final String LABEL_TYPE = "Type";

  private static final String LABEL_GROUP_ID = "Group";

  private static final String LABEL_ARTIFACT_ID = "Artifact";

  private static final String LABEL_VERSION = "Version";

  private static final String EXPECTED_MATCH_STATE = "Exact";

  private static final String EXPECTED_IDENTIFICATION_SOURCE = "Sonatype";

  private static final String EXPECTED_WEBSITE_LINK_TEXT = "Visit Project Website";

  private static final String EXPECTED_CATEGORY = "Other";

  private static final String EXPECTED_TITLE_CONTAINS = "a";

  private static final String HIGH_SEVERITY_CVE_ID = "sonatype-2017-0507";

  private static final String LOW_SEVERITY_CVE_ID = "CVE-1234-56789";

  private static final float HIGH_SEVERITY = 9.1f;

  private static final float LOW_SEVERITY = 4.3f;

  private static final String HIGH_SEVERITY_VULNERABILITY_FIXTURE =
      "/vulnerabilityDetails/vulnerabilityDetails2.json";

  private static final String LOW_SEVERITY_VULNERABILITY_FIXTURE =
      "/vulnerabilityDetails/vulnerabilityDetails_CVE-1234-56789.json";

  private static final int HIGH_SEVERITY_POLICY_THREAT_LEVEL = 10;

  private static final String HIGH_SEVERITY_POLICY_CONSTRAINT_NAME = "Security-High constraint";

  private static final String HIGH_SEVERITY_POLICY_CONSTRAINT_REASON =
      "security vulnerability severity >= 9.1";

  private static final int LOW_SEVERITY_POLICY_THREAT_LEVEL = 6;

  private static final String LOW_SEVERITY_POLICY_CONSTRAINT_NAME = "Security-Low constraint";

  private static final String LOW_SEVERITY_POLICY_CONSTRAINT_REASON =
      "security vulnerability severity >= 4.3";

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
   * Verify dashboard → component-details navigation and the "Component Information" tile.
   * Mirrors two legacy Selenide tests in a single drill-in (skill §2: one scenario, multiple
   * coherent assertions): {@code testTitle} and
   * {@code testComponentOverviewTileFromFirewallDashboard}.
   * <p>
   * Asserts:
   * <ul>
   * <li>Click on the quarantined-row navigates to {@code firewall.componentDetailsPage}
   * and mounts the page container.</li>
   * <li>Page title renders and contains the seeded artifact's coordinates fragment (the
   * title is built from {@code componentDetails.displayName.parts} —
   * {@code FirewallComponentDetailsPage.jsx:77}).</li>
   * <li>Identification info read-only list values:
   * <ol>
   * <li>Match State = {@code "Exact"}</li>
   * <li>Identification Source = {@code "Sonatype"}</li>
   * <li>Website = "Visit Project Website" link (rendered when HDS componentDetails
   * returns a non-empty {@code website})</li>
   * <li>Category = {@code "Other"} (default in
   * {@code FirewallOverviewComponentInformation.jsx:72} when no
   * {@code componentCategories} are returned)</li>
   * </ol>
   * </li>
   * <li>"View Coordinates" button opens the popover; the popover shows the maven format
   * and coordinates {@code groupId / artifactId / version}; the popover's close
   * button hides it.</li>
   * </ul>
   * Requires the HDS {@code componentDetails} stub: those fields are loaded via
   * {@code firewallActions.loadComponentDetails} which proxies through
   * {@code /rest/ci/componentDetails/repository/{ownerId}} to HDS.
   */
  @Test
  @Category(SanityTest.class)
  public void testComponentInformationTile_fromDashboard() {
    seedComponentWithComponentInformationStubs();
    playwrightRefreshOrOpen(FirewallPage.url());

    FirewallPage firewallPage = new FirewallPage();
    FirewallComponentDetailsPage detailsPage = new FirewallComponentDetailsPage();
    firewallPage.openFirstQuarantinedComponent();
    PlaywrightWaitUtils.waitForVisible(detailsPage.container(), PlaywrightTiming.ELEMENT_TIMEOUT_MS,
        PlaywrightTiming.POLL_INTERVAL_MS);
    PlaywrightWaitUtils.waitForVisible(detailsPage.componentInfoTile(), PlaywrightTiming.ELEMENT_TIMEOUT_MS,
        PlaywrightTiming.POLL_INTERVAL_MS);

    assertThat(detailsPage.container()).isVisible();

    // Page title (legacy `testTitle`): asserts the title renders and contains a fragment of
    // the seeded maven coordinates. The title text is the IQ-derived `displayName.parts`
    // joined together (FirewallComponentDetailsPage.jsx:77), so it should include the
    // artifactId. We use containsText (not exact match) to stay decoupled from the exact
    // separator the backend chooses (" : " vs ":" vs " ") and from any localisation.
    assertThat(detailsPage.title()).isVisible();
    assertThat(detailsPage.title()).containsText(EXPECTED_TITLE_CONTAINS);

    assertThat(detailsPage.componentInfoValueByLabel(LABEL_MATCH_STATE)).hasText(EXPECTED_MATCH_STATE);
    assertThat(detailsPage.componentInfoValueByLabel(LABEL_IDENTIFICATION_SOURCE))
        .hasText(EXPECTED_IDENTIFICATION_SOURCE);
    assertThat(detailsPage.componentInfoWebsiteLink()).isVisible();
    assertThat(detailsPage.componentInfoWebsiteLink()).hasText(EXPECTED_WEBSITE_LINK_TEXT);
    // Category is the 4th read-only item on the tile (legacy `testComponentOverviewTile`
    // index 3). FirewallOverviewComponentInformation.jsx:72 falls back to "Other" when no
    // componentCategories are returned by HDS, which matches our minimal HDS stub.
    assertThat(detailsPage.componentInfoValueByLabel(LABEL_CATEGORY)).hasText(EXPECTED_CATEGORY);

    detailsPage.openCoordinatesPopover();
    assertThat(detailsPage.coordinatesPopover()).isVisible();
    assertThat(detailsPage.coordinatesPopoverValueByLabel(LABEL_TYPE)).hasText(MAVEN_FORMAT);
    assertThat(detailsPage.coordinatesPopoverValueByLabel(LABEL_GROUP_ID)).hasText(MAVEN_GROUP_ID);
    assertThat(detailsPage.coordinatesPopoverValueByLabel(LABEL_ARTIFACT_ID)).hasText(MAVEN_ARTIFACT_ID);
    assertThat(detailsPage.coordinatesPopoverValueByLabel(LABEL_VERSION)).hasText(MAVEN_VERSION);

    detailsPage.closeCoordinatesPopover();
    assertThat(detailsPage.coordinatesPopover()).isHidden();
  }

  @Test
  @Category(SanityTest.class)
  public void testSecurityTab_policyViolationsTable_renders() {
    RepositoryComponent component = seedComponentWithSecurityViolations();
    playwrightRefreshOrOpen(FirewallComponentDetailsPage.urlSecurityTab(component));

    FirewallComponentDetailsPage detailsPage = new FirewallComponentDetailsPage();
    PlaywrightWaitUtils.waitForVisible(detailsPage.container(), PlaywrightTiming.ELEMENT_TIMEOUT_MS,
        PlaywrightTiming.POLL_INTERVAL_MS);
    PlaywrightWaitUtils.waitForVisible(detailsPage.securityTabContent(), PlaywrightTiming.ELEMENT_TIMEOUT_MS,
        PlaywrightTiming.POLL_INTERVAL_MS);

    // Tab + panel sanity (formerly testSecurityTab_isVisible): folded in here so we don't
    // re-execute the same dashboard navigation + tab click just to assert the panel mounts.
    assertThat(detailsPage.securityTab()).isVisible();
    assertThat(detailsPage.securityTabContent()).isVisible();

    assertThat(detailsPage.securityTabPolicyViolationsTable()).isVisible();
    assertThat(detailsPage.securityTabPolicyViolationRows()).hasCount(2);
  }

  @Test
  @Category(SanityTest.class)
  public void testSecurityTab_vulnerabilityDetailsPopover_opens() {
    RepositoryComponent component = seedComponentWithSecurityViolationsAndVulnerabilityStubs();
    playwrightRefreshOrOpen(FirewallComponentDetailsPage.urlSecurityTab(component));

    FirewallComponentDetailsPage detailsPage = new FirewallComponentDetailsPage();
    assertThat(detailsPage.container())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));

    detailsPage.openVulnerabilityDetailsPopover(1);

    assertThat(detailsPage.vulnerabilityDetailsPopover()).isVisible();
    assertThat(detailsPage.vulnerabilityDetailsPopoverTitle()).containsText(HIGH_SEVERITY_CVE_ID);
  }

  private RepositoryComponent seedComponentWithSecurityViolations() {
    Repository repository = newRepositoryForSecurityScenario();
    Policy securityHighPolicy = tempEntity.newPolicy();
    Policy securityLowPolicy = tempEntity.newPolicy();
    RepositoryComponent component = newQuarantinedComponentForSecurityScenario(repository);

    newSecurityViolation(component, securityHighPolicy, FailActionType.ID, HIGH_SEVERITY_POLICY_THREAT_LEVEL,
        HIGH_SEVERITY_POLICY_CONSTRAINT_NAME, HIGH_SEVERITY_POLICY_CONSTRAINT_REASON);
    newSecurityViolation(component, securityLowPolicy, WarnActionType.ID, LOW_SEVERITY_POLICY_THREAT_LEVEL,
        LOW_SEVERITY_POLICY_CONSTRAINT_NAME, LOW_SEVERITY_POLICY_CONSTRAINT_REASON);
    FirewallComponentDetailsHdsStub.stubRepositoryComponentDetails(
        testCLMServer.getHdsServer(), component);
    return component;
  }

  private RepositoryComponent seedComponentWithSecurityViolationsAndVulnerabilityStubs() {
    RepositoryComponent component = seedComponentWithSecurityViolations();
    stubComponentDetailsHds(component, /* withVulnerabilities */ true);

    // Vulnerability detail fixtures used when a vuln-row is clicked.
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource(HIGH_SEVERITY_VULNERABILITY_FIXTURE))
        .atUri("rest/vulnerability/details/json/" + HIGH_SEVERITY_CVE_ID);
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource(LOW_SEVERITY_VULNERABILITY_FIXTURE))
        .atUri("rest/vulnerability/details/json/" + LOW_SEVERITY_CVE_ID);
    return component;
  }

  private RepositoryComponent seedComponentWithComponentInformationStubs() {
    Repository repository = newRepositoryForSecurityScenario();
    Policy quarantinePolicy = tempEntity.newPolicy();
    RepositoryComponent component = newQuarantinedComponentForSecurityScenario(repository);
    tempEntity.newRepositoryPolicyViolation(component.getRepositoryId(), VIOLATION_COUNT,
        component.getPathname(), false, FailActionType.ID, quarantinePolicy.getId(),
        POLICY_NAME, component.getComponentIdentifier());
    stubComponentDetailsHds(component, /* withVulnerabilities */ false);
    return component;
  }

  private void stubComponentDetailsHds(RepositoryComponent component, boolean withVulnerabilities) {
    if (withVulnerabilities) {
      FirewallComponentDetailsHdsStub.stubRepositoryComponentDetailsWithVulnerabilities(
          testCLMServer.getHdsServer(), component,
          HIGH_SEVERITY_CVE_ID, HIGH_SEVERITY, LOW_SEVERITY_CVE_ID, LOW_SEVERITY);
    }
    else {
      FirewallComponentDetailsHdsStub.stubRepositoryComponentDetails(
          testCLMServer.getHdsServer(), component);
    }
  }

  private Repository newRepositoryForSecurityScenario() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPOSITORY_MANAGER_INSTANCE_ID);
    return tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true, false);
  }

  private RepositoryComponent newQuarantinedComponentForSecurityScenario(Repository repository) {
    ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    Date evalTime = Date.from(LocalDateTime.now().withDayOfMonth(1).toInstant(offset));
    return tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        COMPONENT_PATHNAME, COMPONENT_HASH,
        ComponentIdentifier.createMavenCoordinates(
            MAVEN_GROUP_ID, MAVEN_ARTIFACT_ID, MAVEN_VERSION),
        evalTime, evalTime);
  }

  private RepositoryPolicyViolation newSecurityViolation(
      RepositoryComponent component,
      Policy policy,
      String actionId,
      int threatLevel,
      String constraintName,
      String constraintReason)
  {
    ConstraintFact constraintFact = new ConstraintFact(uuid(), constraintName, LogicalOperator.AND.name());
    constraintFact.addConditionFact(new ConditionFact(MatchStateConditionType.ID, 0, "summary", constraintReason));

    RepositoryPolicyViolation violation = new RepositoryPolicyViolation(component.getRepositoryId(),
        component.getPathname(), new Date(), policy.getId(), policy.getName(), threatLevel,
        PolicyThreatCategory.SECURITY, component.getHash(), component.getComponentIdentifier(),
        List.of(constraintFact));
    violation.setActionTypeId(actionId);
    tempEntity.newRepositoryPolicyViolation(violation);
    return violation;
  }

  private String uuid() {
    return UUID.randomUUID().toString();
  }
}
