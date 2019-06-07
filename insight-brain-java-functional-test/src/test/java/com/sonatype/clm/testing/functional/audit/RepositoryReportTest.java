/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.audit;

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
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.RepositoryReportPage;
import com.sonatype.clm.testing.functional.pages.RepositoryReportPage.Filter;
import com.sonatype.clm.testing.functional.pages.RepositoryReportPage.Row;
import com.sonatype.clm.testing.functional.pages.RepositoryReportPage.Table;
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

import com.codeborne.selenide.Condition;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryReportTest
    extends AbstractFunctionalTest
{
  private static final ComponentIdentifier CRITICAL_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("critical",
      "threat", "1.0");

  private Repository repo;

  private Policy extremelyBadPolicy;

  private Policy notInSummaryPolicy;

  private String criticalComponentHash;

  private InsightWork insightWork;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(ReportListPage.URL);
    loginAsAdmin();
  }

  @Before
  public void before() {
    // repositoryPublicId has a character requiring encoding
    repo = tempEntity.newRepository(tempEntity.newRepositoryManager(), "ce&ntral");

    extremelyBadPolicy = createPolicy(10, "Extremely Bad", MatchStateConditionType.ID, "is",
        MatchState.EXACT.toString());
    notInSummaryPolicy = createPolicy(9, "Not in summary", CoordinatesConditionType.ID, "match", "maven:critical:*");

    insightWork = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
  }

  @Test
  public void testSummary() throws Exception {
    tempEntity.newRepositoryComponent(repo.getId());
    tempEntity.newRepositoryComponent(repo.getId(), MatchState.UNKNOWN, null);

    tempEntity.newRepositoryPolicyViolation(repo.getId(), 3, "3", null);

    tempEntity.newRepositoryPolicyViolation(repo.getId(), 5, "5", null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 6, "6", null);

    tempEntity.newRepositoryPolicyViolation(repo.getId(), 8, "8", null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 9, "9", null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 10, "10", null);

    tempEntity.newRepositoryComponent(repo.getId(), "quarantined1", new Date(), null);
    tempEntity.newRepositoryComponent(repo.getId(), "quarantined2", new Date(), null);

    refreshOrOpen(RepositoryReportPage.url(repo.getId()));

    RepositoryReportPage.summary().shouldBe(visible);

    RepositoryReportPage.summary().moderateCount().shouldBe(visible).shouldHave(text("1"));
    RepositoryReportPage.summary().severeCount().shouldBe(visible).shouldHave(text("2"));
    RepositoryReportPage.summary().criticalCount().shouldBe(visible).shouldHave(text("3"));
    RepositoryReportPage.summary().violatingComponentsCount().shouldBe(visible).shouldHave(text("6"));
    RepositoryReportPage.summary().quarantinedCount().shouldBe(visible).shouldHave(text("2"));

    RepositoryReportPage.summary().identifiedCount().shouldBe(visible).shouldHave(text("3"));
    RepositoryReportPage.summary().identifiedPercent().shouldBe(visible).shouldHave(text("75"));
  }

  @Test
  public void testSummary_Empty() throws Exception {
    refreshOrOpen(RepositoryReportPage.url(repo.getId()));

    RepositoryReportPage.summary().shouldBe(visible);

    RepositoryReportPage.summary().noPolicyViolations().shouldBe(visible).shouldHave(text("0"));
    RepositoryReportPage.summary().identifiedCount().shouldBe(visible).shouldHave(text("0"));
    RepositoryReportPage.summary().identifiedPercent().shouldBe(visible).shouldHave(text("0"));
    RepositoryReportPage.summary().quarantinedCount().shouldBe(visible).shouldHave(text("0"));
  }

  @Test
  public void testUnquarantine() throws Exception {
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("severe", "threat", "1.0."), true);
    tempEntity.newRepositoryPolicyViolation(component, 6, false, "Really Bad", Action.ID_FAIL);

    component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("no", "threat", "1.0."), true);
    tempEntity.newRepositoryPolicyViolation(component, 3, false, "Whatever", null);
    setupHDSFirewallResponse(component.getHash());

    refreshOrOpen(RepositoryReportPage.url(repo.getId()));

    // Components with violations cannot be unquarantined
    openCip(0, "Policy");
    WaiverCip.row(0).actions().shouldHave(texts("Proxy fail"));
    WaiverCip.unquarantineButton().shouldBe(visible, CLM.DISABLED).click();
    UnquarantineDialog.releaseButton().shouldNot(appear);
    RepositoryReportPage.table().row(0).component().click(); // hide CIP

    // Unquarantine a component
    openCip(1, "Policy");
    WaiverCip.unquarantineButton().shouldBe(visible).shouldNotBe(CLM.DISABLED).click();
    UnquarantineDialog.releaseButton().should(appear).click();
    UnquarantineDialog.releaseButton().should(disappear);

    component = new RepositoryComponentDAO().getById(component.getId());
    assertThat(component.isQuarantined()).isFalse();
  }

  @Test
  public void testPage() {
    // one no violation, unknown
    RepositoryComponent component = tempEntity
        .newRepositoryComponent(repo.getId(), MatchState.UNKNOWN, "thePathname", null, false);
    unknown = new ExpectedRow(Table.NO_THREAT, "No violations", component.getPathname(), false, false);

    // one of each threat level
    component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("ignored", "threat", "1.0."));
    tempEntity.newRepositoryPolicyViolation(component, 1, false, "Meh", null);

    component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("moderate", "threat", "1.0."));
    tempEntity.newRepositoryPolicyViolation(component, 3, false, "Sorta Bad", null);

    component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("severe", "threat", "1.0."));
    tempEntity.newRepositoryPolicyViolation(component, 6, false, "Really Bad", null);

    component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT, CRITICAL_IDENTIFIER);

    tempEntity.newRepositoryPolicyViolation(component.getRepositoryId(), 10, component.getPathname(), false, true,
        extremelyBadPolicy.getId(), extremelyBadPolicy.getName(), component.getComponentIdentifier());
    criticalComponentHash = component.getHash();

    // one with multiple violations
    tempEntity.newRepositoryPolicyViolation(component, 9, false, "Not in summary", null);

    // one quarantined, that groups
    component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("quarantined", "component", "1.0."));
    component.setQuarantineTime(new Date());
    new RepositoryComponentDAO().update(component);
    tempEntity.newRepositoryPolicyViolation(component, 10, false, "Extremely Bad", null);

    // one waived
    component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("waived", "component", "1.0."));
    new RepositoryComponentDAO().update(component);
    tempEntity.newRepositoryPolicyViolation(component, 10, true, "Extremely Bad but its cool", null);

    // setup HDS
    setupHdsFirewallResponse();

    refreshOrOpen(RepositoryReportPage.url(repo.getId()));

    testReportSummary();
    eyesWatcher.eyesCheck("Repository report summary");

    // Default filter settings
    RepositoryReportPage.filter().allMatchState().shouldBe(Filter.ACTIVE);
    RepositoryReportPage.filter().summaryViolations().shouldBe(Filter.ACTIVE);

    assertRows(CRITICAL_ROW, QUARANTINED, SEVERE_ROW, MODERATE_ROW, IGNORED_ROW, unknown, NO_VIOLATION_ROW);

    testExactMatchesFilter();
    testUnknownMatchesFilter();

    testAllViolationsFilter();
    testQuarantinedFilter();
    testWaivedFilter();
  }

  private void setupHDSFirewallResponse(String hash) {
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = hash;
    componentEvaluationData.matchState = MatchState.EXACT.getId();
    componentEvaluationData.declaredLicenses = new HashSet<>();
    componentEvaluationData.observedLicenses = new HashSet<>();
    hdsResult.components.add(componentEvaluationData);
    testCLMServer.getHdsServer().setResponseForURI("/rest/component/details/firewall", hdsResult, 200);
  }

  private void setupHdsResponse() {
    testCLMServer.getHdsServer().setResponseForURI("rest/ci/componentDetails",
        getClass().getClassLoader().getResource("componentDetails/componentDetails.json"), 200);
    testCLMServer.getHdsServer().setResponseForURI("rest/ci/componentDetails/list",
        getClass().getClassLoader().getResource("componentDetails/componentDetailsList.json"), 200);
  }

  private void cipSetup() {
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        CRITICAL_IDENTIFIER);
    criticalComponentHash = component.getHash();

    createPolicyViolation(component, extremelyBadPolicy);
    createPolicyViolation(component, notInSummaryPolicy);

    setupHdsFirewallResponse();
    setupHdsResponse();
  }

  @Test
  public void testUnknownComponentCip() throws Exception {
    tempEntity.newRepositoryComponent(repo.getId(), MatchState.UNKNOWN, null);
    refreshOrOpen(RepositoryReportPage.url(repo.getId()));

    // Open CIP for unknown component
    RepositoryReportPage.table().row(0).openCip();

    RepositoryReportPage.table().cipTab("Component Info").shouldBe(visible);
    VersionsCIP.selectComponentMessage().shouldBe(hidden);
    VersionsCIP.unknownComponentMessage().shouldBe(visible);

    RepositoryReportPage.table().cipTab("Policy").shouldBe(visible);
    RepositoryReportPage.table().cipTab("Labels").shouldBe(hidden);
    RepositoryReportPage.table().cipTab("Licenses").shouldBe(hidden);
    RepositoryReportPage.table().cipTab("Vulnerabilities").shouldBe(hidden);
  }

  @Test
  public void testLicenseCip() throws Exception {
    cipSetup();
    refreshOrOpen(RepositoryReportPage.url(repo.getId()));
    tempEntity.newLicenseOverride(RepositoryContainer.REPOSITORY_CONTAINER_ID, CRITICAL_IDENTIFIER,
        LicenseOverrideStatus.ACKNOWLEDGED, (Set<String>) null);

    openCip(0, "License");

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
    eyesWatcher.eyesCheck("Repository report CIP");
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

    // Close CIP should disappear
    RepositoryReportPage.table().closeCipButton().shouldBe(visible).click();
  }

  @Test
  public void testVersionGraphCip() throws Exception {
    cipSetup();
    refreshOrOpen(RepositoryReportPage.url(repo.getId()));

    // open CIP
    RepositoryReportPage.table().row(0).openCip();

    RepositoryReportPage.table().cipTab("Component Info").click();
    VersionsCIP.groupId().shouldHave(text("critical"));
    VersionsCIP.artifactId().shouldHave(text("threat"));
    VersionsCIP.version().shouldHave(text("1.0"));
    VersionsCIP.declaredLicenses().shouldHave(texts("Apache-2.0"));
    VersionsCIP.observedLicenses().shouldHave(texts("GPL-2.0"));
    VersionsCIP.effectiveLicenses().shouldHave(texts("Apache-2.0", "GPL-2.0"));
    VersionsCIP.highestPolicyThreat().shouldHave(text(String.valueOf(extremelyBadPolicy.getThreatLevel())));
    VersionsCIP.highestSecurityThreat().shouldHave(text("9.1"));
    VersionsCIP.securityCount().shouldHave(text("3"));
    VersionsCIP.matchState().shouldHave(text("exact"));
    VersionsCIP.identificationSource().shouldHave(text("Sonatype"));
    VersionsCIP.componentCategory().shouldHave(text("Other"));
    VersionsCIP.recommendedVersionsHeader().shouldNotBe(visible);
    VersionsCIP.nextNoViolationVersionLink().shouldNotBe(visible);
    VersionsCIP.nextNoFailVersionLink().shouldNotBe(visible);
    eyesWatcher.eyesCheck("Repository report version graph");
    
    VersionsCIP.showDetailsLink().shouldBe(visible).click();
    VersionsCIP.hideDetailsLink().shouldBe(visible);

    eyesWatcher.eyesCheck("Repository report version graph with details");

    // close CIP
    RepositoryReportPage.table().row(0).component().click();
    RepositoryReportPage.table().cip().shouldBe(hidden);
  }

  @Test
  public void testLabelsCip() throws Exception {
    cipSetup();
    refreshOrOpen(RepositoryReportPage.url(repo.getId()));

    Label elJunko = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "El Junko", Color.dark_blue);
    Label elMagnifico = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "El Magnifico", Color.dark_red);
    tempEntity.newComponentLabel(Organization.ROOT_ORGANIZATION_ID, elJunko.getId(), criticalComponentHash);
    createPolicy(1, "Bad Label", LabelConditionType.ID, "is", elMagnifico.getId());

    // open CIP to labels
    openCip(0, "Labels");

    LabelsCIP.appliedLabels().shouldHaveSize(1);
    LabelsCIP.appliedLabel(1).shouldHave(text("El Junko"), LabelsCIP.Label.color(Color.dark_blue)).action()
        .should(exist);

    LabelsCIP.availableLabels().shouldHaveSize(1);
    LabelsCIP.availableLabel(1).shouldHave(text("El Magnifico"), LabelsCIP.Label.color(Color.dark_red)).action()
        .click();

    // Modal
    AddLabelModal.root().shouldBe(visible);
    AddLabelModal.scopes().shouldHaveSize(3);
    AddLabelModal.saveButton().click();

    AddLabelModal.root().shouldBe(hidden);
    RepositoryReportPage.waitForComponentUpdater();

    // label persisted
    assertThat(new ComponentLabelDAO().getByOwnerIdAndHash(repo.getId(), criticalComponentHash)).hasSize(2);

    // new table row for the policy violation
    RepositoryReportPage.filter().allViolations().click();
    assertRow(RepositoryReportPage.table().rowByName("Bad Label"), new ExpectedRow(Table.IGNORED_SCORE, "Bad Label",
        "critical : threat : 1.0", false, false), false);
    RepositoryReportPage.table().rows().shouldHaveSize(3);

    // re-open CIP
    openCip(0, "Labels");

    // Remove the label we added
    LabelsCIP.appliedLabels().shouldHaveSize(2);
    LabelsCIP.appliedLabel(2).shouldHave(text("El Magnifico")).action().click();

    // Confirmation modal
    RemoveLabelModal.root().should(appear);
    RemoveLabelModal.confirmButton().click();
    RemoveLabelModal.root().should(disappear);

    // backend check that it was removed
    List<ComponentLabel> appliedLabels = new ComponentLabelDAO().getByOwnerIdAndHash(repo.getId(),
        criticalComponentHash);
    assertThat(appliedLabels).extracting(ComponentLabel::getLabelId).containsExactlyInAnyOrder(elJunko.getId());

    // CIP should disappear
    RepositoryReportPage.table().cip().shouldBe(hidden);
    // Bad labels is gone
    RepositoryReportPage.table().rows().shouldHaveSize(2);

    // reset filter
    RepositoryReportPage.filter().summaryViolations().click();
  }

  @Test
  public void testPolicyCip() throws Exception {
    cipSetup();
    refreshOrOpen(RepositoryReportPage.url(repo.getId()));

    openCip(0, "Policy");

    // Check existing violations
    WaiverCip.rows().shouldHaveSize(2);
    WaiverCip.row(0).shouldBe("cip-policy-red", CRITICAL_ROW.policyName,
        new String[] { CRITICAL_ROW.policyName + " constraint" }, new String[] { "Match State was exact" });
    WaiverCip.row(1).shouldBe("cip-policy-red", CRITICAL_ROW_SECONDARY.policyName,
        new String[] { CRITICAL_ROW_SECONDARY.policyName + " constraint" },
        new String[] { "Coordinates were critical : threat : 1.0" });

    // check that there are no existing waivers
    WaiverCip.viewWaivers().shouldBe(visible).click();
    ViewWaiversDialog.rows().shouldHaveSize(0);
    ViewWaiversDialog.closeButton().click();

    // Waive first violation
    WaiverCip.row(0).waiveButton().shouldBe(visible).click();

    // Waive a policy violation
    AddWaiverDialog.waiveViolationOnly().shouldBe(visible, selected);
    AddWaiverDialog.scopeContainer().shouldBe(hidden);
    AddWaiverDialog.policyName().shouldHave(text(CRITICAL_ROW.policyName));
    AddWaiverDialog.policyName().shouldHave(cssClass("cip-policy-red"));
    AddWaiverDialog.constraintName().shouldHave(text(CRITICAL_ROW.policyName + " constraint"));
    AddWaiverDialog.waiverConditions().shouldHave(text("Match State was exact"));

    AddWaiverDialog.comment().setValue("TEST COMMENT");
    AddWaiverDialog.saveButton().shouldBe(visible, enabled).click();
    RepositoryReportPage.waitForComponentUpdater();

    // CIP closes as the row is hidden in the summary view
    AddWaiverDialog.root().should(disappear);
    ReportCip.policyTab().should(disappear);

    // Verify table has been updated and violation is waived
    RepositoryReportPage.table().rows().shouldHaveSize(1);
    RepositoryReportPage.filter().allViolations().click();
    RepositoryReportPage.table().row(0).waived().should(exist).click();

    // Verify repository policy violation and policy waiver both contain the correct content
    List<RepositoryPolicyViolation> repositoryPolicyViolations = new RepositoryPolicyViolationDAO()
        .getByRepositoryId(repo.getId());
    assertThat(repositoryPolicyViolations).hasSize(4);

    List<PolicyWaiver> policyWaivers = new PolicyWaiverDAO().getByOwnerId(repo.getId());
    assertThat(policyWaivers).hasSize(1);

    PolicyWaiver policyWaiver = policyWaivers.get(0);
    RepositoryPolicyViolation repositoryPolicyViolation = repositoryPolicyViolations.stream()
        .filter(violation -> policyWaiver.getConstraintFactsJson().equals(violation.getConstraintFactsJson()))
        .findFirst().get();

    assertThat(policyWaiver.getPolicyId()).isEqualTo(repositoryPolicyViolation.getPolicyId());
    assertThat(policyWaiver.getOwnerId()).isEqualTo(repositoryPolicyViolation.getRepositoryId());
    assertThat(policyWaiver.getHash()).isEqualTo(criticalComponentHash);

    // re-open CIP
    RepositoryReportPage.table().cipTab("Policy").click();

    // remove waiver
    WaiverCip.viewWaivers().should(appear).click();
    ViewWaiversDialog.rows().shouldHaveSize(1);
    ViewWaiversDialog.row(0).removeButton().click();

    WaiverCip.ConfirmRemoveWaiverDialog.removeButton().should(appear).click();
    WaiverCip.ConfirmRemoveWaiverDialog.removeButton().should(disappear);

    RepositoryReportPage.waitForComponentUpdater();
    ViewWaiversDialog.closeButton().should(appear).click();

    // re-open CIP
    openCip(0, "Policy");

    // Waive first violation
    WaiverCip.row(0).waiveButton().shouldBe(visible).click();

    // Waive a policy violation for all components
    AddWaiverDialog.waiveViolationOnly().shouldBe(visible, selected);
    AddWaiverDialog.scopeContainer().shouldBe(hidden);
    AddWaiverDialog.policyName().shouldHave(text(CRITICAL_ROW.policyName));
    AddWaiverDialog.policyName().shouldHave(cssClass("cip-policy-red"));
    AddWaiverDialog.constraintName().shouldHave(text(CRITICAL_ROW.policyName + " constraint"));
    AddWaiverDialog.waiverConditions().shouldHave(text("Match State was exact"));

    AddWaiverDialog.scopedWaiver().click();

    AddWaiverDialog.scopeContainer().shouldBe(visible);
    AddWaiverDialog.waiverOwner().shouldBe(visible);
    AddWaiverDialog.waiverOwner().shouldHave(text(repo.getPublicId()));

    AddWaiverDialog.allComponents().shouldBe(visible).shouldNotBe(selected).click();
    AddWaiverDialog.allComponents().shouldBe(selected);
    AddWaiverDialog.selectedComponent().shouldBe(visible).shouldNotBe(selected);

    AddWaiverDialog.comment().setValue("TEST COMMENT");
    AddWaiverDialog.saveButton().shouldBe(visible, enabled).click();

    // Warning about all component waivers should appear
    RepositoryReportPage.componentUpdater().shouldBe(visible);
    RepositoryReportPage.componentUpdater().dismissButton().click();

    // re-open CIP
    RepositoryReportPage.table().cipTab("Policy").click();

    // remove waiver
    WaiverCip.viewWaivers().should(appear).click();
    ViewWaiversDialog.rows().shouldHaveSize(1);
    ViewWaiversDialog.row(0).removeButton().click();

    WaiverCip.ConfirmRemoveWaiverDialog.removeButton().should(appear).click();
    WaiverCip.ConfirmRemoveWaiverDialog.removeButton().should(disappear);

    // Warning about all component waivers should appear
    RepositoryReportPage.componentUpdater().shouldBe(visible);
    RepositoryReportPage.componentUpdater().dismissButton().click();
    ViewWaiversDialog.closeButton().should(appear).click();

    // close CIP & reset filter
    RepositoryReportPage.filter().summaryViolations().click();
  }

  @Test
  public void testVulnerabilityCip() throws Exception {
    cipSetup();
    refreshOrOpen(RepositoryReportPage.url(repo.getId()));

    tempEntity.newSecurityVulnerabilityOverride(repo.getId(), criticalComponentHash, "cve", "CVE-1234-56789",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);
    openCip(0, "Vulnerabilities");

    VulnerabilityCIP.rows().shouldHaveSize(3);

    assertRow(VulnerabilityCIP.row(0), 9, "CVE-1234-56789");
    assertRow(VulnerabilityCIP.row(1), 4, "OSVDB-1234");
    assertRow(VulnerabilityCIP.row(2), null, "OSVDB-4321");

    String componentIdentifier = URLEncoder.encode(ComponentIdentifierAdapter.toJson(CRITICAL_IDENTIFIER), "UTF-8");

    testCLMServer.getHdsServer().setResponseForURI(
        "rest/vulnerability/details/cve/CVE-1234-56789?componentIdentifier=" + componentIdentifier + "&hash="
            + criticalComponentHash,
        getClass().getClassLoader().getResource("vulnerabilityDetails/vulnerabilityDetails.json"), 200);

    SVTableRow row = VulnerabilityCIP.row(0);
    row.info().click();

    SVDetailModal.root().shouldBe(Condition.visible);

    // html from the json response we send above
    $("#somedivfortest").shouldBe(Condition.visible);

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

    ArrayNode allLogJsonData = JsonUtils.read(new File(insightWork.getAuditDir(repo.getId()), "security.json"));
    assertThat(allLogJsonData).hasSize(1);
    assertThat(allLogJsonData.get(0).get("data").get("comment").asText()).isEqualTo("woot");

    // close CIP
    RepositoryReportPage.table().cipCloseButton().click();
    RepositoryReportPage.table().cip().shouldBe(hidden);
  }

  private static void assertRow(SVTableRow actualRow, Integer threatLevel, String identifier) {
    actualRow.identifier().shouldHave(text(identifier));
    actualRow.info().shouldBe(visible);
    actualRow.threatLevel().shouldHave(text(threatLevel == null ? "Unscored" : threatLevel.toString().substring(0, 1)),
        SVTableRow.color(threatLevel));
  }

  private void testReportSummary() {
    RepositoryReportPage.summary().shouldBe(visible);

    RepositoryReportPage.summary().moderateCount().shouldBe(visible).shouldHave(text("1"));
    RepositoryReportPage.summary().severeCount().shouldBe(visible).shouldHave(text("1"));
    RepositoryReportPage.summary().criticalCount().shouldBe(visible).shouldHave(text("2"));
    RepositoryReportPage.summary().violatingComponentsCount().shouldBe(visible).shouldHave(text("4"));

    RepositoryReportPage.summary().identifiedCount().shouldBe(visible).shouldHave(text("6"));
    RepositoryReportPage.summary().identifiedPercent().shouldBe(visible).shouldHave(text("86"));
  }

  private void testUnknownMatchesFilter() {
    RepositoryReportPage.filter().unknownMatchState().click().shouldBe(Filter.ACTIVE);

    assertRows(unknown);

    resetFilter();
  }

  private void testExactMatchesFilter() {
    RepositoryReportPage.filter().exactMatchState().click().shouldBe(Filter.ACTIVE);

    assertRows(CRITICAL_ROW, QUARANTINED, SEVERE_ROW, MODERATE_ROW, IGNORED_ROW, NO_VIOLATION_ROW);

    resetFilter();
  }

  private void testAllViolationsFilter() {
    RepositoryReportPage.filter().allViolations().click().shouldBe(Filter.ACTIVE);

    assertRows(CRITICAL_ROW, QUARANTINED, WAIVED_ROW, CRITICAL_ROW_SECONDARY, SEVERE_ROW, MODERATE_ROW, IGNORED_ROW,
        unknown);
    resetFilter();
  }

  private void testQuarantinedFilter() {
    RepositoryReportPage.filter().quarantinedViolations().click().shouldBe(Filter.ACTIVE);

    assertRows(QUARANTINED);
    resetFilter();
  }

  private void testWaivedFilter() {
    RepositoryReportPage.filter().waivedViolations().click().shouldBe(Filter.ACTIVE);

    assertRows(WAIVED_ROW);
    resetFilter();
  }

  private void resetFilter() {
    RepositoryReportPage.filter().allMatchState().click().shouldBe(Filter.ACTIVE);
    RepositoryReportPage.filter().summaryViolations().click().shouldBe(Filter.ACTIVE);
  }

  private void setupHdsFirewallResponse() {
    ComponentEvaluationData component = new ComponentEvaluationData();
    component.componentIdentifier = CRITICAL_IDENTIFIER;
    component.declaredLicenses = Collections.emptySet();
    component.observedLicenses = Collections.emptySet();
    component.hash = criticalComponentHash;
    component.matchState = MatchState.EXACT.toString();
    ComponentEvaluationDataList response = new ComponentEvaluationDataList();
    response.components.add(component);
    testCLMServer.getHdsServer().setResponseForURI("rest/component/details/firewall", response, 200);
  }

  private static void assertRows(ExpectedRow... expectedRows) {
    RepositoryReportPage.table().rows().shouldHaveSize(expectedRows.length);

    String previousPolicyName = null;
    for (int i = 0; i < expectedRows.length; i++) {
      assertRow(RepositoryReportPage.table().row(i), expectedRows[i],
          expectedRows[i].policyName.equals(previousPolicyName));
      previousPolicyName = expectedRows[i].policyName;
    }
  }

  private static void assertRow(Row actualRow, ExpectedRow expectedRow, boolean shouldBeGrouped) {
    actualRow.policy().shouldHave(expectedRow.threatLevel);
    if (shouldBeGrouped) {
      actualRow.policy().shouldHave(text(""));
    }
    else {
      actualRow.policy().shouldHave(text(expectedRow.policyName));
    }
    actualRow.component().shouldHave(text(expectedRow.componentName));

    if (expectedRow.waived) {
      actualRow.waived().shouldBe(exist);
    }
    else {
      actualRow.waived().shouldNotBe(exist);
    }

    if (expectedRow.quarantined) {
      actualRow.quarantined().shouldBe(exist);
    }
    else {
      actualRow.quarantined().shouldNotBe(exist);
    }
  }

  private static void openCip(int row, String tab) {
    RepositoryReportPage.table().row(row).openCip();
    RepositoryReportPage.table().cipTab(tab).click();
  }

  private Policy createPolicy(int threatLevel, String name, String conditionType, String op, String value) {
    Policy p = new Policy(null, name);
    p.setThreatLevel(threatLevel);
    p.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    Constraint constraint = new Constraint(null, name + " constraint", LogicalOperator.AND);
    com.sonatype.insight.brain.model.policy.Condition condition = new com.sonatype.insight.brain.model.policy.Condition(
        conditionType, op, value);
    constraint.setConditions(Collections.singletonList(condition));
    p.setConstraints(Collections.singletonList(constraint));
    return tempEntity.newPolicy(p);
  }

  private void createPolicyViolation(RepositoryComponent component, Policy policy) {
    Constraint constraint = policy.getConstraints().get(0);
    ConstraintFact constraintFact = new ConstraintFact(constraint.getId(), constraint.getName(),
        constraint.getOperator().name());

    Component c = new Component(component.getComponentIdentifier());
    c.setMatchState(MatchState.EXACT);
    int conditionIndex = 0;
    constraintFact.addConditionFact(ComponentPolicyEvaluator
        .createConditionFact(policy.getConstraints().get(0).getConditions().get(conditionIndex), new MatchFact(c,
            policy.getId(), policy.getConstraints().get(0).getId(), Collections.emptyList() /* conditionTriggers */)));

    RepositoryPolicyViolation violation = tempEntity.newRepositoryPolicyViolation(component.getRepositoryId(),
        policy.getThreatLevel(), component.getPathname(), false, true, policy.getId(), policy.getName(),
        component.getComponentIdentifier());

    violation.setConstraintFacts(Collections.singletonList(constraintFact));
    new RepositoryPolicyViolationDAO().update(violation);
  }

  private ExpectedRow unknown;

  private static final ExpectedRow QUARANTINED =
      new ExpectedRow(Table.CRITICAL_THREAT, "Extremely Bad", "quarantined : component : 1.0", false, true);

  private static final ExpectedRow CRITICAL_ROW =
      new ExpectedRow(Table.CRITICAL_THREAT, "Extremely Bad", "critical : threat : 1.0", false, false);

  private static final ExpectedRow CRITICAL_ROW_SECONDARY =
      new ExpectedRow(Table.CRITICAL_THREAT, "Not In Summary", "critical : threat : 1.0", false, false);

  private static final ExpectedRow SEVERE_ROW =
      new ExpectedRow(Table.SEVERE_THREAT, "Really Bad", "severe : threat : 1.0", false, false);

  private static final ExpectedRow MODERATE_ROW =
      new ExpectedRow(Table.MODERATE_THREAT, "Sorta Bad", "moderate : threat : 1.0", false, false);

  private static final ExpectedRow IGNORED_ROW =
      new ExpectedRow(Table.IGNORED_SCORE, "Meh", "ignored : threat : 1.0", false, false);

  private static final ExpectedRow WAIVED_ROW =
      new ExpectedRow(Table.CRITICAL_THREAT, "Extremely Bad but its cool", "waived : component : 1.0", true, false);

  private static final ExpectedRow NO_VIOLATION_ROW =
      new ExpectedRow(Table.NO_THREAT, "No violations", "waived : component : 1.0", false, false);

  private static class ExpectedRow
  {
    Condition threatLevel;

    String policyName;

    String componentName;

    boolean waived;

    boolean quarantined;

    ExpectedRow(Condition threatLevel, String policyName, String componentName, boolean waived, boolean quarantined) {
      this.threatLevel = threatLevel;
      this.policyName = policyName;
      this.componentName = componentName;
      this.waived = waived;
      this.quarantined = quarantined;
    }
  }
}
