/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.LabelsCIP;
import com.sonatype.clm.testing.functional.elements.LabelsCIP.AddLabelModal;
import com.sonatype.clm.testing.functional.elements.LabelsCIP.RemoveLabelModal;
import com.sonatype.clm.testing.functional.elements.ReportCip;
import com.sonatype.clm.testing.functional.elements.VersionsCIP;
import com.sonatype.clm.testing.functional.elements.VulnerabilityCIP;
import com.sonatype.clm.testing.functional.elements.VulnerabilityCIP.SVDetailModal;
import com.sonatype.clm.testing.functional.elements.VulnerabilityCIP.SVTableRow;
import com.sonatype.clm.testing.functional.elements.reports.LicenseCIP;
import com.sonatype.clm.testing.functional.pages.FirewallAutoUnquarantinePage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents;
import com.sonatype.clm.testing.functional.pages.RepositoryReportPage;
import com.sonatype.clm.testing.functional.pages.WaiverCip;
import com.sonatype.clm.testing.functional.pages.WaiverCip.AddWaiverDialog;
import com.sonatype.clm.testing.functional.pages.WaiverCip.UnquarantineDialog;
import com.sonatype.clm.testing.functional.pages.WaiverCip.ViewWaiversDialog;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codeborne.selenide.Condition;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.*;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallCipTest
    extends AbstractFunctionalTest
{
  private static final ComponentIdentifier CRITICAL_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("critical",
      "threat", "1.0");

  private static final String MATCH_STATE_POLICY_NAME = "Match State Policy";

  private static final String COORDINATES_POLICY_NAME = "Coordinates Policy";

  private Repository repo;

  private Policy matchStatePolicy;

  private Policy coordinatesPolicy;

  private String criticalComponentHash;

  private InsightWork insightWork;

  private final FirewallPage firewallPage = new FirewallPage();

  private final FirewallAutoUnquarantinePage firewallAutoUnquarantinePage = new FirewallAutoUnquarantinePage();

  private final RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  private static int timeIncrementCounter;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(FirewallPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    setFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE, LicensedFeature.RELEASE_INTEGRITY);
    repo = tempEntity.newRepository("testRepo");

    matchStatePolicy = createPolicy(10, MATCH_STATE_POLICY_NAME, MatchStateConditionType.ID, "is",
        MatchState.EXACT.toString());
    coordinatesPolicy =
        createPolicy(9, COORDINATES_POLICY_NAME, CoordinatesConditionType.ID, "match", "maven:critical:*");

    insightWork = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
  }

  @Test
  public void testUnquarantine() {
    createComponentWithViolation(false, "severe", "threat", "1.0", "Really Bad", false, Action.ID_FAIL);
    final String componentId =
        createComponentWithViolation(false, "no", "threat", "1.0", "Whatever", true, Action.ID_FAIL);

    refreshOrOpen(FirewallPage.url());

    // Components with violations cannot be unquarantined
    openCip(0, 2);

    WaiverCip.row(0).actions().shouldHave(texts("Proxy fail"));
    WaiverCip.unquarantineButton().shouldBe(visible, CLM.DISABLED).click();
    UnquarantineDialog.releaseButton().shouldNot(appear);

    FirewallPageComponents.cipModal().closeButton().click();

    // Unquarantine a component
    openCip(1, 2);
    WaiverCip.unquarantineButton().shouldBe(visible).shouldNotBe(CLM.DISABLED).click();
    UnquarantineDialog.releaseButton().should(appear).click();
    UnquarantineDialog.releaseButton().should(disappear);

    final RepositoryComponent component = new RepositoryComponentDAO().getById(componentId);
    assertThat(component.isQuarantined()).isFalse();
  }

  @Test
  public void testUnknownComponentCip() {
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.UNKNOWN,
        ComponentIdentifier.createMavenCoordinates("unknown", "unknown", "unknown"), true);
    tempEntity.newRepositoryPolicyViolation(component, 6, false, "Really Bad", Action.ID_FAIL);
    refreshOrOpen(FirewallPage.url());

    // Open CIP for unknown component
    openCip(0, 1);

    VersionsCIP.selectComponentMessage().shouldBe(hidden);
    VersionsCIP.unknownComponentMessage().shouldBe(visible);
  }

  @Test
  public void testLicenseCip() {
    cipSetup();
    refreshOrOpen(FirewallPage.url());
    tempEntity.newLicenseOverride(RepositoryContainer.REPOSITORY_CONTAINER_ID, CRITICAL_IDENTIFIER,
        LicenseOverrideStatus.ACKNOWLEDGED, (Set<String>) null);

    openCip(0, 4);

    eyesWatcher.eyesCheck("FirewallPage License CIP");

    // License sidebar
    LicenseCIP.declaredLicenses().shouldHave(LicenseCIP.licenseThreats(0), texts("Apache-2.0"));
    LicenseCIP.observedLicenses().shouldHave(LicenseCIP.licenseThreats(9), texts("GPL-2.0"));
    LicenseCIP.effectiveLicenses().shouldHave(LicenseCIP.licenseThreats(0, 9), texts("Apache-2.0", "GPL-2.0"));

    // Editor default state
    LicenseCIP.scopes().shouldHave(texts(repo.getName(), "All Repositories", "Root Organization"));
    LicenseCIP.scope().shouldHave(value("string:" + RepositoryContainer.REPOSITORY_CONTAINER_ID));
    LicenseCIP.statuses().shouldHave(
        texts("Open", "Acknowledged", "Overridden", "Selected", "Confirmed", "Inherit Status (Open)"));
    LicenseCIP.status().shouldHave(value("ACKNOWLEDGED"));
    LicenseCIP.licenseSelector().shouldNot(exist);
    LicenseCIP.updateButton().shouldNotBe(enabled);

    // Update to Selected state
    LicenseCIP.scope().selectOption(repo.getName());

    LicenseCIP.statuses().shouldHave(
        texts("Open", "Acknowledged", "Overridden", "Selected", "Confirmed", "Inherit Status (Acknowledged)"));
    LicenseCIP.status().selectOption("Selected");

    // choose a license
    LicenseCIP.licenseSelector().button().shouldBe(visible).click();
    LicenseCIP.licenseSelector().entries().shouldHave(texts("Apache-2.0", "GPL-2.0"));

    LicenseCIP.licenseSelector().entry(0).click();
    LicenseCIP.licenseSelector().button().click();
    LicenseCIP.comment().setValue("not bad");
    LicenseCIP.updateButton().shouldBe(enabled).click();

    RepositoryReportPage.waitForComponentUpdater();

    // Check for our override
    LicenseCIP.effectiveLicenses().shouldHave(texts("Apache-2.0"));
    LicenseCIP.scope().shouldHave(value("string:" + repo.getId()));
    LicenseCIP.status().shouldHave(value("SELECTED"));
    LicenseCIP.licenseSelector().should(exist);
    LicenseCIP.updateButton().shouldNotBe(enabled);

    // Verify override on backend
    final LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    LicenseOverride override = licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(repo.getId(), CRITICAL_IDENTIFIER);
    assertThat(override.getStatus()).isEqualTo(LicenseOverrideStatus.SELECTED);
    assertThat(override.getLicenseIds()).isEqualTo(Collections.singleton("Apache-2.0"));

    // remove
    LicenseCIP.status().selectOption("Inherit Status (Acknowledged)");
    LicenseCIP.updateButton().shouldBe(enabled).click();

    RepositoryReportPage.waitForComponentUpdater();

    LicenseCIP.updateButton().shouldBe(disabled);
    override = licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(repo.getId(), CRITICAL_IDENTIFIER);
    assertThat(override).isNull();
  }

  @Test
  public void testVersionGraphCip() {
    cipSetup();
    refreshOrOpen(FirewallPage.url());

    // open CIP
    openCip(0, 1);

    VersionsCIP.componentType().shouldHave(text("maven"));
    VersionsCIP.groupId().shouldHave(text("critical"));
    VersionsCIP.artifactId().shouldHave(text("threat"));
    VersionsCIP.version().shouldHave(text("1.0"));
    VersionsCIP.declaredLicenses().shouldHave(texts("Apache-2.0"));
    VersionsCIP.observedLicenses().shouldHave(texts("GPL-2.0"));
    VersionsCIP.effectiveLicenses().shouldHave(texts("Apache-2.0", "GPL-2.0"));
    VersionsCIP.highestPolicyThreat().shouldHave(text(String.valueOf(matchStatePolicy.getThreatLevel())));
    VersionsCIP.highestSecurityThreat().shouldHave(text("9.1"));
    VersionsCIP.securityCount().shouldHave(text("3"));
    VersionsCIP.hygieneRating().shouldHave(text("Laggard"));
    VersionsCIP.integrityRating().shouldHave(text("Malicious"));
    VersionsCIP.integrityRating().shouldHave(cssClass("cip-color-suspicious"));
    VersionsCIP.matchState().shouldHave(text("exact"));
    VersionsCIP.identificationSource().shouldHave(text("Sonatype"));
    VersionsCIP.componentCategory().shouldHave(text("Other"));
    VersionsCIP.recommendedVersionsHeader().shouldNotBe(visible);
    VersionsCIP.nextNoViolationVersionLink().shouldNotBe(visible);
    VersionsCIP.nextNoFailVersionLink().shouldNotBe(visible);

    VersionsCIP.showDetailsLink().shouldBe(visible).click();
    VersionsCIP.hideDetailsLink().shouldBe(visible);

    eyesWatcher.eyesCheck("FirewallPage version graph with details");
  }

  @Test
  public void testLabelsCip() {
    cipSetup();
    refreshOrOpen(FirewallPage.url());

    final Label elJunko = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "El Junko", Color.dark_blue);
    final Label elMagnifico = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "El Magnifico", Color.dark_red);
    tempEntity.newComponentLabel(Organization.ROOT_ORGANIZATION_ID, elJunko.getId(), criticalComponentHash);
    createPolicy(1, "Bad Label", LabelConditionType.ID, "is", elMagnifico.getId());

    // open CIP to labels
    openCip(0, 6);

    LabelsCIP.appliedLabels().shouldHaveSize(1);
    LabelsCIP.appliedLabel(1).shouldHave(text("El Junko"), LabelsCIP.Label.color(Color.dark_blue)).action()
        .should(exist);

    LabelsCIP.availableLabels().shouldHaveSize(1);
    LabelsCIP.availableLabel(1).shouldHave(text("El Magnifico"), LabelsCIP.Label.color(Color.dark_red)).action()
        .click();

    eyesWatcher.eyesCheck("FirewallPage Labels CIP");

    // Modal
    AddLabelModal.root().shouldBe(visible);
    AddLabelModal.scopes().shouldHaveSize(3);
    AddLabelModal.saveButton().click();

    AddLabelModal.root().shouldBe(hidden);

    RepositoryReportPage.waitForComponentUpdater();

    // label persisted
    assertThat(new ComponentLabelDAO().getByOwnerIdAndHashWithHierarchy(repo.getId(), criticalComponentHash))
        .hasSize(2);

    //close CIP
    FirewallPageComponents.cipModal().closeButton().click();

    // re-open CIP
    openCip(0, 6);

    // Remove the label we added
    LabelsCIP.appliedLabels().shouldHaveSize(2);
    LabelsCIP.appliedLabel(2).shouldHave(text("El Magnifico")).action().click();

    // Confirmation modal
    RemoveLabelModal.root().should(appear);
    RemoveLabelModal.confirmButton().click();
    RemoveLabelModal.root().should(disappear);

    // backend check that it was removed
    final List<ComponentLabel> appliedLabels = new ComponentLabelDAO().getByOwnerIdAndHashWithHierarchy(repo.getId(),
        criticalComponentHash);
    assertThat(appliedLabels).extracting(ComponentLabel::getLabelId).containsExactlyInAnyOrder(elJunko.getId());
  }

  @Test
  public void testPolicyCip() {
    cipSetup();
    refreshOrOpen(FirewallPage.url());

    openCip(0, 2);

    // Check existing violations
    WaiverCip.rows().shouldHaveSize(2);

    WaiverCip.row(0).shouldBe("cip-policy-red", MATCH_STATE_POLICY_NAME,
        new String[]{MATCH_STATE_POLICY_NAME + " constraint"}, new String[]{"Match state was 'Exact'"});
    WaiverCip.row(1).shouldBe("cip-policy-red", COORDINATES_POLICY_NAME,
        new String[]{COORDINATES_POLICY_NAME + " constraint"},
        new String[]{"Coordinates were critical : threat : 1.0"});

    eyesWatcher.eyesCheck("FirewallPage Policy CIP");

    // check that there are no existing waivers
    WaiverCip.viewWaivers().shouldBe(visible).click();
    ViewWaiversDialog.rows().shouldHaveSize(0);
    ViewWaiversDialog.closeButton().click();

    // Request waiver button should be hidden
    WaiverCip.row(0).requestWaiverButton().shouldNotBe(visible);

    // Waive first violation
    WaiverCip.row(0).waiveButton().shouldBe(visible).click();

    // Waive a policy violation
    AddWaiverDialog.waiveViolationOnly().shouldBe(visible, selected);
    AddWaiverDialog.scopeContainer().shouldBe(hidden);

    AddWaiverDialog.policyName().shouldHave(text(MATCH_STATE_POLICY_NAME));
    AddWaiverDialog.policyName().shouldHave(cssClass("cip-policy-red"));

    AddWaiverDialog.constraintName().shouldHave(text(MATCH_STATE_POLICY_NAME + " constraint"));
    AddWaiverDialog.waiverConditions().shouldHave(text("Match state was 'Exact'"));

    AddWaiverDialog.comment().setValue("TEST COMMENT");
    eyesWatcher.eyesCheck("FirewallPage Add Waiver Dialog");

    AddWaiverDialog.saveButton().shouldBe(visible, enabled).click();

    RepositoryReportPage.waitForComponentUpdater();

    // CIP closes as the row is hidden in the summary view
    AddWaiverDialog.root().should(disappear);
    ReportCip.policyTab().should(disappear);

    // Verify repository policy violation and policy waiver both contain the correct content
    final List<RepositoryPolicyViolation> repositoryPolicyViolations = new RepositoryPolicyViolationDAO()
        .getByRepositoryId(repo.getId());
    assertThat(repositoryPolicyViolations).hasSize(2);

    final List<PolicyWaiver> policyWaivers = new PolicyWaiverDAO().getActiveByOwnerId(repo.getId());
    assertThat(policyWaivers).hasSize(1);

    final PolicyWaiver policyWaiver = policyWaivers.get(0);
    final RepositoryPolicyViolation repositoryPolicyViolation = repositoryPolicyViolations.stream()
        .filter(violation -> policyWaiver.getConstraintFactsJson().equals(violation.getConstraintFactsJson()))
        .findFirst().get();

    assertThat(policyWaiver.getPolicyId()).isEqualTo(repositoryPolicyViolation.getPolicyId());
    assertThat(policyWaiver.getOwnerId()).isEqualTo(repositoryPolicyViolation.getRepositoryId());
    assertThat(policyWaiver.getHash()).isEqualTo(criticalComponentHash);

    //close CIP
    FirewallPageComponents.cipModal().closeButton().click();

    // re-open CIP
    openCip(0, 2);

    // Waive first violation
    WaiverCip.row(0).waiveButton().shouldBe(visible).click();

    // Waive a policy violation for all components
    AddWaiverDialog.waiveViolationOnly().shouldBe(visible, selected);
    AddWaiverDialog.scopeContainer().shouldBe(hidden);

    AddWaiverDialog.policyName().shouldHave(text(COORDINATES_POLICY_NAME));
    AddWaiverDialog.policyName().shouldHave(cssClass("cip-policy-red"));

    AddWaiverDialog.constraintName().shouldHave(text(COORDINATES_POLICY_NAME + " constraint"));
    AddWaiverDialog.waiverConditions().shouldHave(text("Coordinates were critical : threat : 1.0"));

    AddWaiverDialog.scopedWaiver().click();

    AddWaiverDialog.scopeContainer().shouldBe(visible);
    AddWaiverDialog.waiverOwner().shouldBe(visible);
    AddWaiverDialog.waiverOwner().shouldHave(text(repo.getPublicId()));

    AddWaiverDialog.allComponents().shouldBe(visible).shouldNotBe(selected).click();
    AddWaiverDialog.allComponents().shouldBe(selected);
    AddWaiverDialog.selectedComponent().shouldBe(visible).shouldNotBe(selected);

    AddWaiverDialog.comment().setValue("TEST COMMENT");
    AddWaiverDialog.saveButton().shouldBe(visible, enabled).click();
  }

  @Test
  public void testVulnerabilityCip() throws Exception {
    cipSetup();
    refreshOrOpen(FirewallPage.url());

    tempEntity.newSecurityVulnerabilityOverride(repo.getId(), criticalComponentHash, "cve", "CVE-1234-56789",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);
    openCip(0, 5);

    VulnerabilityCIP.rows().shouldHaveSize(3);

    assertRow(VulnerabilityCIP.row(0), 9, "CVE-1234-56789");
    assertRow(VulnerabilityCIP.row(1), 4, "OSVDB-1234");
    assertRow(VulnerabilityCIP.row(2), null, "OSVDB-4321");

    final String componentIdentifier =
        URLEncoder.encode(ComponentIdentifierAdapter.toJson(CRITICAL_IDENTIFIER), "UTF-8");

    testCLMServer.getHdsServer()
        .respondWith(getClass().getClassLoader().getResource("vulnerabilityDetails/vulnerabilityDetails.json"))
        .atUri("rest/vulnerability/details/json/CVE-1234-56789?componentIdentifier=" + componentIdentifier);

    final SVTableRow row = VulnerabilityCIP.row(0);
    row.info().click();

    SVDetailModal.root().shouldBe(Condition.visible);
    SVDetailModal.contents().shouldHave(text("CVE-1234-56789"));
    SVDetailModal.closeButton().shouldBe(Condition.enabled).click();

    row.identifier().click();
    row.shouldBe(SVTableRow.ROW_SELECTED);

    VulnerabilityCIP.Editor.status().shouldBe(visible).shouldHave(text("Acknowledged"))
        .selectOption(SecurityVulnerabilityOverrideStatus.OPEN.getName());
    VulnerabilityCIP.Editor.comment().val("woot");
    VulnerabilityCIP.Editor.saveButton().click();

    RepositoryReportPage.waitForComponentUpdater();

    row.status().shouldHave(text("Open"));
    assertThat(new SecurityVulnerabilityOverrideDAO().getByOwnerIdHashSourceAndReferenceId(repo.getId(),
        criticalComponentHash, "cve", "CVE-1234-56789")).isNull();

    eyesWatcher.eyesCheck("FirewallPage Vulnerability CIP");

    final ArrayNode allLogJsonData = JsonUtils.read(new File(insightWork.getAuditDir(repo.getId()), "security.json"));
    assertThat(allLogJsonData).hasSize(1);
    assertThat(allLogJsonData.get(0).get("data").get("comment").asText()).isEqualTo("woot");
  }

  @Test
  public void testPreviousNextButtons_FirewallPage() {
    createComponentWithViolation(false, "severe", "threat", "1.0", "Really Bad", false, Action.ID_FAIL);
    createComponentWithViolation(false, "another", "threat", "1.0", "Really Bad", false, Action.ID_FAIL);
    createComponentWithViolation(false, "more", "threats", "1.0", "Really Bad", false, Action.ID_FAIL);
    createComponentWithViolation(false, "last", "threat", "1.0", "Really Bad", false, Action.ID_FAIL);

    refreshOrOpen(FirewallPage.url());

    openCip(0, 1);

    eyesWatcher.eyesCheck("FirewallPage CIP");

    FirewallPageComponents.cipModal().previousButton().shouldNotBe(enabled);
    FirewallPageComponents.cipModal().nextButton().shouldBe(enabled);
    FirewallPageComponents.cipModal().header().shouldHave(text("severe : threat : 1.0"));
    FirewallPageComponents.cipModal().nextButton().click();

    FirewallPageComponents.cipModal().previousButton().shouldBe(enabled);
    FirewallPageComponents.cipModal().nextButton().shouldBe(enabled);
    FirewallPageComponents.cipModal().header().shouldHave(text("another : threat : 1.0"));
    FirewallPageComponents.cipModal().nextButton().click();

    FirewallPageComponents.cipModal().previousButton().shouldBe(enabled);
    FirewallPageComponents.cipModal().nextButton().shouldBe(enabled);
    FirewallPageComponents.cipModal().header().shouldHave(text("more : threats : 1.0"));
    FirewallPageComponents.cipModal().nextButton().click();

    FirewallPageComponents.cipModal().previousButton().shouldBe(enabled);
    FirewallPageComponents.cipModal().nextButton().shouldNotBe(enabled);
    FirewallPageComponents.cipModal().header().shouldHave(text("last : threat : 1.0"));
    FirewallPageComponents.cipModal().previousButton().click();

    FirewallPageComponents.cipModal().previousButton().shouldBe(enabled);
    FirewallPageComponents.cipModal().nextButton().shouldBe(enabled);
    FirewallPageComponents.cipModal().header().shouldHave(text("more : threats : 1.0"));
  }

  @Test
  public void testPreviousNextButtons_FirewallAutoUnquarantinePage() {
    createComponentWithViolation(true, "severe", "threat", "1.0", "Really Bad", false, Action.ID_FAIL);
    createComponentWithViolation(true, "another", "threat", "1.0", "Really Bad", false, Action.ID_FAIL);
    createComponentWithViolation(true, "more", "threats", "1.0", "Really Bad", false, Action.ID_FAIL);
    createComponentWithViolation(true, "last", "threat", "1.0", "Really Bad", false, Action.ID_FAIL);

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    firewallAutoUnquarantinePage.firewallUnquarantineTable().tableBodyRows().get(0).click();

    eyesWatcher.eyesCheck("FirewallAutoUnquarantinePage CIP");

    FirewallPageComponents.cipModal().previousButton().shouldNotBe(enabled);
    FirewallPageComponents.cipModal().nextButton().shouldBe(enabled);
    FirewallPageComponents.cipModal().header().shouldHave(text("severe : threat : 1.0"));
    FirewallPageComponents.cipModal().nextButton().click();

    FirewallPageComponents.cipModal().previousButton().shouldBe(enabled);
    FirewallPageComponents.cipModal().nextButton().shouldBe(enabled);
    FirewallPageComponents.cipModal().header().shouldHave(text("another : threat : 1.0"));
    FirewallPageComponents.cipModal().nextButton().click();

    FirewallPageComponents.cipModal().previousButton().shouldBe(enabled);
    FirewallPageComponents.cipModal().nextButton().shouldBe(enabled);
    FirewallPageComponents.cipModal().header().shouldHave(text("more : threats : 1.0"));
    FirewallPageComponents.cipModal().nextButton().click();

    FirewallPageComponents.cipModal().previousButton().shouldBe(enabled);
    FirewallPageComponents.cipModal().nextButton().shouldNotBe(enabled);
    FirewallPageComponents.cipModal().header().shouldHave(text("last : threat : 1.0"));
    FirewallPageComponents.cipModal().previousButton().click();

    FirewallPageComponents.cipModal().previousButton().shouldBe(enabled);
    FirewallPageComponents.cipModal().nextButton().shouldBe(enabled);
    FirewallPageComponents.cipModal().header().shouldHave(text("more : threats : 1.0"));
  }

  private void setupHDSFirewallResponse(final String hash) {
    final ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    final ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = hash;
    componentEvaluationData.matchState = MatchState.EXACT.getId();
    componentEvaluationData.declaredLicenses = new HashSet<>();
    componentEvaluationData.observedLicenses = new HashSet<>();
    hdsResult.components.add(componentEvaluationData);
    testCLMServer.getHdsServer().respondWith(hdsResult).atUri("/rest/component/details/firewall");
  }

  private void setupHdsResponse() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getClassLoader().getResource("componentDetails/componentDetails.json"))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(getClass().getClassLoader().getResource("componentDetails/componentDetailsList.json"))
        .atUri("rest/ci/componentDetails/list");
  }

  private void cipSetup() {
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        CRITICAL_IDENTIFIER, true);
    criticalComponentHash = component.getHash();

    createPolicyViolation(component, matchStatePolicy);
    createPolicyViolation(component, coordinatesPolicy);

    setupHdsFirewallResponse();
    setupHdsResponse();
  }

  private static void assertRow(final SVTableRow actualRow, final Integer threatLevel, final String identifier) {
    actualRow.identifier().shouldHave(text(identifier));
    actualRow.info().shouldBe(visible);
    actualRow.threatLevel().shouldHave(text(threatLevel == null ? "Unscored" : threatLevel.toString().substring(0, 1)),
        SVTableRow.color(threatLevel));
  }

  private void setupHdsFirewallResponse() {
    final ComponentEvaluationData component = new ComponentEvaluationData();
    component.componentIdentifier = CRITICAL_IDENTIFIER;
    component.declaredLicenses = Collections.emptySet();
    component.observedLicenses = Collections.emptySet();
    component.hash = criticalComponentHash;
    component.matchState = MatchState.EXACT.toString();
    final ComponentEvaluationDataList response = new ComponentEvaluationDataList();
    response.components.add(component);
    testCLMServer.getHdsServer().respondWith(response).atUri("rest/component/details/firewall");
  }

  private void openCip(final int row, final int tab) {
    firewallPage.firewallQuarantineTable().tableBodyRows().get(row).click();
    FirewallPageComponents.cipModal().tabLink(tab).click();
  }

  private Policy createPolicy(
      final int threatLevel,
      final String name,
      final String conditionType,
      final String op,
      final String value)
  {
    final Policy p = new Policy(null, name);
    p.setThreatLevel(threatLevel);
    p.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    final Constraint constraint = new Constraint(null, name + " constraint", LogicalOperator.AND);
    final com.sonatype.insight.brain.model.policy.Condition condition =
        new com.sonatype.insight.brain.model.policy.Condition(
            conditionType, op, value);
    constraint.setConditions(Collections.singletonList(condition));
    p.setConstraints(Collections.singletonList(constraint));
    return tempEntity.newPolicy(p);
  }

  private void createPolicyViolation(final RepositoryComponent component, final Policy policy) {
    final Constraint constraint = policy.getConstraints().get(0);
    final ConstraintFact constraintFact = new ConstraintFact(constraint.getId(), constraint.getName(),
        constraint.getOperator().name());

    final Component c = new Component(component.getComponentIdentifier());
    c.setMatchState(MatchState.EXACT);
    final int conditionIndex = 0;
    constraintFact.addConditionFact(ComponentPolicyEvaluator
        .createConditionFact(policy.getConstraints().get(0).getConditions().get(conditionIndex), new MatchFact(c,
            policy.getId(), policy.getConstraints().get(0).getId(), Collections.emptyList() /* conditionTriggers */)));

    final RepositoryPolicyViolation violation = tempEntity.newRepositoryPolicyViolation(component.getRepositoryId(),
        policy.getThreatLevel(), component.getPathname(), false, Action.ID_FAIL, policy.getId(), policy.getName(),
        component.getComponentIdentifier());

    violation.setConstraintFacts(Collections.singletonList(constraintFact));
    new RepositoryPolicyViolationDAO().update(violation);
  }

  private String createComponentWithViolation(
      final boolean isAutoUnquarantined,
      final String group,
      final String artifact,
      final String version,
      final String policyName,
      final boolean isWaived,
      final String actionId)
  {
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates(group, artifact, version), true);
    tempEntity.newRepositoryPolicyViolation(repositoryComponent, 3, isWaived, policyName, actionId);
    repositoryComponent.setQuarantineTime(DateUtils.addSeconds(new Date(), ++timeIncrementCounter));
    if (isAutoUnquarantined) {
      repositoryComponent.setUnquarantineTimeForMonitoring(DateUtils.addSeconds(new Date(), timeIncrementCounter));
    }
    repositoryComponentDAO.update(repositoryComponent);
    setupHDSFirewallResponse(repositoryComponent.getHash());
    return repositoryComponent.getId();
  }
}
