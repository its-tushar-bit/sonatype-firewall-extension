/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxThreatCounter;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationDetailPopover;
import com.sonatype.clm.testing.functional.pages.ComponentWaiversPopover;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage.DeleteWaiverModal;
import com.sonatype.clm.testing.functional.pages.RequestWaiveTransitiveViolationsPopover;
import com.sonatype.clm.testing.functional.pages.TransitiveViolationsPage;
import com.sonatype.clm.testing.functional.pages.TransitiveViolationsPage.ComponentDetailsHeader;
import com.sonatype.clm.testing.functional.pages.TransitiveViolationsPage.TransitiveViolationsRow;
import com.sonatype.clm.testing.functional.pages.TransitiveViolationsPage.TransitiveViolationsTable;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.clm.testing.functional.pages.WaiveTransitiveViolationsPopover;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.service.InsightWork;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.google.common.collect.ImmutableMap;
import org.apache.tools.ant.util.DateUtils;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;

public class TransitiveViolationsTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  private Organization rootOrganization;

  private Organization organization;

  private Application application;

  private List<Component> components;

  private Component component;

  private PolicyEvaluation policyEvaluation;

  private List<PolicyViolation> policyViolations;

  @Before
  public void before() throws Exception {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(ImmutableMap.of(Feature.INNER_SOURCE_TRANSITIVE_WAIVER.getFlag(), true));
    rootOrganization = new OrganizationDAO().getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    organization = tempEntity.newOrganization("Test Org 0af5aa00a2424db19b115f70b6f873d9");
    application = tempEntity.newApplication("Test App 56770d0ec3da47b0aa8eab53d874efdb",
        "56770d0ec3da47b0aa8eab53d874efdb", organization.getId());
    policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId",
        new Date(1623424315000L));
    PolicyViolation aPolicyViolation = tempEntity
        .newPolicyViolation(policyEvaluation, tempEntity.newPolicy(application.getId(), "aPolicyX"), 10,
            PolicyThreatCategory.SECURITY,
            ComponentIdentifier.createMavenCoordinates("g", "ZtransitiveY", "v", "", "e"), "hash3", Action.ID_FAIL);
    PolicyViolation zPolicyViolation = tempEntity
        .newPolicyViolation(policyEvaluation, tempEntity.newPolicy(application.getId(), "ZPolicy"), 5,
            PolicyThreatCategory.SECURITY, ComponentIdentifier.createMavenCoordinates("g", "btransitive", "v", "", "e"),
            "hash4", Action.ID_WARN);
    PolicyViolation bPolicyViolation = tempEntity
        .newPolicyViolation(policyEvaluation, tempEntity.newPolicy(application.getId(), "bPolicyx", 0),
            ComponentIdentifier.createMavenCoordinates("g", "atransitivey", "v", "", "e"), "hash2");
    policyViolations = Arrays.asList(aPolicyViolation, zPolicyViolation, bPolicyViolation);
    ReportTestUtils.createReportFile(application.getId(), policyEvaluation.getScanId(),
        zipReportDir("/TransitiveViolationsTest/report", tempDir),
        testCLMServer.getCLMServer().getInstance(InsightWork.class));
    ReportTestUtils.createPolicyThreats(application.getId(), policyEvaluation.getScanId(),
        testCLMServer.getCLMServer().getInstance(InsightWork.class), policyViolations);
    File reportFile = testCLMServer.getCLMServer().getInstance(InsightWork.class)
        .getReportFile(application.getId(), policyEvaluation.getScanId());
    ReportEntry reportEntry = Report.getEntry(reportFile, Report.BOM_JSON_FILENAME);
    components = new ComponentDAO(application).getAll(null, null, reportEntry.buf, null);
    component = components.stream().filter(c -> c.getHash().equals("hash1")).findFirst().orElse(null);
  }

  @Test
  public void testInitialState() {
    TransitiveViolationsPage transitiveViolationsPage = visitPage();
    ComponentDetailsHeader componentDetailsHeader = transitiveViolationsPage.title();
    componentDetailsHeader.title().shouldHave(Condition.text(component.getDisplayName()));
    ElementsCollection reportInformationElements = componentDetailsHeader.reportInformationElements();
    reportInformationElements.shouldHaveSize(3);
    reportInformationElements.get(0).shouldHave(Condition.text(organization.getName()));
    reportInformationElements.get(1).shouldHave(Condition.text(application.getName()));
    reportInformationElements.get(2).shouldHave(Condition
        .text(policyEvaluation.getStageTypeId() + " Report " + getExpectedDateTime(policyEvaluation.getTime())));
    componentDetailsHeader.getElement().$(".component-details-header__tags").should(Condition.exist);
    componentDetailsHeader.tags().shouldHaveSize(1);
    componentDetailsHeader.tags().get(0).shouldHave(Condition.text("InnerSource"));
    TransitiveViolationsTable transitiveViolationsTable = transitiveViolationsPage.transitiveViolationsTable();
    transitiveViolationsTable.threatHeader().shouldHave(Condition.attribute("aria-sort", "descending"));
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations("-threatLevel", null, null));
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testSortByThreatLevelAscending() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.threatHeader().click();
    transitiveViolationsTable.threatHeader().shouldHave(Condition.attribute("aria-sort", "ascending"));
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations("threatLevel", null, null));
  }

  @Test
  public void testSortByPolicyNameDescending() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.policyAndActionHeader().click();
    transitiveViolationsTable.policyAndActionHeader().shouldHave(Condition.attribute("aria-sort", "descending"));
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations("-policyName", null, null));
  }

  @Test
  public void testSortByPolicyNameAscending() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.policyAndActionHeader().click();
    transitiveViolationsTable.policyAndActionHeader().click();
    transitiveViolationsTable.policyAndActionHeader().shouldHave(Condition.attribute("aria-sort", "ascending"));
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations("policyName", null, null));
  }

  @Test
  public void testSortByComponentNameDescending() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.componentHeader().click();
    transitiveViolationsTable.componentHeader().shouldHave(Condition.attribute("aria-sort", "descending"));
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations("-componentName", null, null));
  }

  @Test
  public void testSortByComponentNameAscending() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.componentHeader().click();
    transitiveViolationsTable.componentHeader().click();
    transitiveViolationsTable.componentHeader().shouldHave(Condition.attribute("aria-sort", "ascending"));
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations("componentName", null, null));
  }

  @Test
  public void testFilterByPolicyName() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.policyNameFilter().sendKeys("Z");
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, "Z", null));
    transitiveViolationsTable.policyNameFilter().sendKeys(Keys.BACK_SPACE);
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, null, null));
  }

  @Test
  public void testFilterByPolicyName_Multiple() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.policyNameFilter().sendKeys("x");
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, "x", null));
    transitiveViolationsTable.policyNameFilter().sendKeys(Keys.BACK_SPACE);
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, null, null));
  }

  @Test
  public void testFilterByComponentName() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.componentNameFilter().sendKeys("Z");
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, null, "Z"));
    transitiveViolationsTable.componentNameFilter().sendKeys(Keys.BACK_SPACE);
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, null, null));
  }

  @Test
  public void testFilterByComponentName_Multiple() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.componentNameFilter().sendKeys("y");
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, null, "y"));
    transitiveViolationsTable.componentNameFilter().sendKeys(Keys.BACK_SPACE);
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, null, null));
  }

  @Test
  public void testFilterByPolicyNameAndComponentName() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.policyNameFilter().sendKeys("x");
    transitiveViolationsTable.componentNameFilter().sendKeys("z");
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, "x", "z"));
    transitiveViolationsTable.policyNameFilter().sendKeys(Keys.BACK_SPACE);
    transitiveViolationsTable.componentNameFilter().sendKeys(Keys.BACK_SPACE);
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, null, null));
  }

  @Test
  public void testFilterAndSort() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.policyNameFilter().sendKeys("x");
    transitiveViolationsTable.componentNameFilter().sendKeys("y");
    transitiveViolationsTable.threatHeader().click();
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations("threatLevel", "x", "y"));
  }

  @Test
  public void testFilterToEmpty() {
    TransitiveViolationsTable transitiveViolationsTable = visitPage().transitiveViolationsTable();
    transitiveViolationsTable.componentNameFilter().sendKeys("l");
    transitiveViolationsTable.rows().shouldHaveSize(1);
    transitiveViolationsTable.row(1).shouldHave(Condition.text("None"));
    transitiveViolationsTable.componentNameFilter().sendKeys(Keys.BACK_SPACE);
    assertRows(transitiveViolationsTable, getExpectedPolicyViolations(null, null, null));
  }

  @Test
  public void testNotInnerSource() {
    TransitiveViolationsPage transitiveViolationsPage = visitPage("hash2");
    transitiveViolationsPage.title().getElement().$(".component-details-header__tags").shouldNot(Condition.exist);
    TransitiveViolationsTable transitiveViolationsTable = transitiveViolationsPage.transitiveViolationsTable();
    transitiveViolationsTable.rows().shouldHaveSize(1);
    transitiveViolationsTable.row(1).shouldHave(Condition.text("None"));
  }

  @Test
  public void testRequestWaiveTransitiveViolations_InitialState() {
    RequestWaiveTransitiveViolationsPopover requestWaiveTransitiveViolationsPopover = visitRequestWaivePopover();
    requestWaiveTransitiveViolationsPopover.countsTitle()
        .shouldHave(Condition.text("3 total violations brought in by 3 components"));
    NxThreatCounter counts = requestWaiveTransitiveViolationsPopover.counts();
    counts.all().shouldHaveSize(3);
    counts.critical().text().shouldHave(Condition.text("Critical"));
    counts.critical().count().shouldHave(Condition.text("1"));
    counts.severe().text().shouldHave(Condition.text("Severe"));
    counts.severe().count().shouldHave(Condition.text("1"));
    counts.none().shouldHave(Condition.text("None"));
    counts.none().shouldHave(Condition.text("1"));
    requestWaiveTransitiveViolationsPopover.applicationPublicIdContainer().content()
        .shouldHave(Condition.text(application.getPublicId()));
    requestWaiveTransitiveViolationsPopover.reportIdContainer().content()
        .shouldHave(Condition.text(policyEvaluation.getScanId()));
    requestWaiveTransitiveViolationsPopover.componentHashContainer().content()
        .shouldHave(Condition.text(component.getHash()));
    String expectedCurlCommand =
        "curl -u admin:admin123 -X POST " + Configuration.baseUrl + "api/v2/policyWaivers/transitive/application/" +
            application.getPublicId() + "/" + policyEvaluation.getScanId() + "?hash=" + component.getHash();
    requestWaiveTransitiveViolationsPopover.curlExampleContainer().content()
        .shouldHave(Condition.text(expectedCurlCommand));
    eyesWatcher.eyesCheck();
    ScrollUtil.scrollIntoView(requestWaiveTransitiveViolationsPopover.curlExampleContainer().content());
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testRequestWaiveTransitiveViolations_Toggle() {
    RequestWaiveTransitiveViolationsPopover requestWaiveTransitiveViolationsPopover = visitRequestWaivePopover();
    requestWaiveTransitiveViolationsPopover.toggle().click();
    requestWaiveTransitiveViolationsPopover.shouldNotBe(Condition.visible);
  }

  @Test
  public void testWaiveTransitiveViolations_InitialState() {
    WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover = visitWaivePopover();
    waiveTransitiveViolationsPopover.countsTitle()
        .shouldHave(Condition.text("3 total violations brought in by 3 components"));
    NxThreatCounter counts = waiveTransitiveViolationsPopover.counts();
    counts.all().shouldHaveSize(3);
    counts.critical().text().shouldHave(Condition.text("Critical"));
    counts.critical().count().shouldHave(Condition.text("1"));
    counts.severe().text().shouldHave(Condition.text("Severe"));
    counts.severe().count().shouldHave(Condition.text("1"));
    counts.none().shouldHave(Condition.text("None"));
    counts.none().shouldHave(Condition.text("1"));
    waiveTransitiveViolationsPopover.scope().shouldHave(
        Condition.text(StringUtils.capitalise(application.getType().toString()) + " - " + application.getName()));
    waiveTransitiveViolationsPopover.expiryTimesSelect().getSelectedOption().shouldHave(text("Never"));
    waiveTransitiveViolationsPopover.expiryTimesOptions().shouldHaveSize(7);
    waiveTransitiveViolationsPopover.expiryTimesOptions().get(0).shouldHave(text("Never"));
    waiveTransitiveViolationsPopover.expiryTimesOptions().get(1).shouldHave(text("7 Days"));
    waiveTransitiveViolationsPopover.expiryTimesOptions().get(2).shouldHave(text("14 Days"));
    waiveTransitiveViolationsPopover.expiryTimesOptions().get(3).shouldHave(text("30 Days"));
    waiveTransitiveViolationsPopover.expiryTimesOptions().get(4).shouldHave(text("60 Days"));
    waiveTransitiveViolationsPopover.expiryTimesOptions().get(5).shouldHave(text("90 Days"));
    waiveTransitiveViolationsPopover.expiryTimesOptions().get(6).shouldHave(text("120 Days"));
    waiveTransitiveViolationsPopover.comments().shouldHave(Condition.text(""));
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testWaiveTransitiveViolations_Toggle() {
    WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover = visitWaivePopover();
    waiveTransitiveViolationsPopover.toggle().click();
    waiveTransitiveViolationsPopover.shouldNotBe(Condition.visible);
  }

  @Test
  public void testWaiveTransitiveViolations_Cancel() {
    WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover = visitWaivePopover();
    waiveTransitiveViolationsPopover.cancelButton().click();
    waiveTransitiveViolationsPopover.shouldNotBe(Condition.visible);
  }

  @Test
  public void testPolicyViolationDetails_InitialState() {
    visitPolicyViolationDetailsPopover();
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();

    tile.headerTitle().shouldHave(text("Violation of aPolicyX"));
    ElementsCollection elements = tile.headerSubtitle().findAll(".iq-violation-details__subtitle-part");
    elements.shouldHaveSize(3);
    elements.get(0).shouldHave(text("Test Org"));
    elements.get(1).shouldHave(text("Test App"));
    elements.get(2).shouldHave(text("g : ZtransitiveY : e : v"));

    tile.policyType().shouldHave(text("Security"));
    tile.threatLevel().shouldHave(text("10"));
    tile.policyOwnerLink().shouldHave(text("Test App"));

    tile.stages().shouldHaveSize(5);

    tile.stage(0).shouldHave(text("Source"));
    tile.stage(0).icon().should(exist);
    tile.stage(0).shouldBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(1).shouldHave(text("Build"));
    tile.stage(1).icon().should(exist);
    tile.stage(1).shouldNotBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(2).shouldHave(text("Stage"));
    tile.stage(2).icon().should(exist);
    tile.stage(2).shouldBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(3).shouldHave(text("Release"));
    tile.stage(3).icon().should(exist);
    tile.stage(3).shouldBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(4).shouldHave(text("Operate"));
    tile.stage(4).icon().should(exist);
    tile.stage(4).shouldBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testPolicyViolationDetails_Cancel() {
    PolicyViolationDetailPopover policyViolationDetailPopover = visitPolicyViolationDetailsPopover();
    policyViolationDetailPopover.getCloseButton().click();
    policyViolationDetailPopover.shouldNotBe(Condition.visible);
  }

  @Test
  public void testWaiveTransitiveViolations_Save() {
    WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover = visitWaivePopover();
    testWaiveTransitiveViolations_Save(waiveTransitiveViolationsPopover, null, null);
  }

  @Test
  public void testWaiveTransitiveViolations_SaveWithComment() {
    WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover = visitWaivePopover();
    String comment = "someComment";
    waiveTransitiveViolationsPopover.comments().sendKeys(comment);
    testWaiveTransitiveViolations_Save(waiveTransitiveViolationsPopover, null, comment);
  }

  @Test
  public void testWaiveTransitiveViolations_SaveWithExpiry() {
    WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover = visitWaivePopover();
    waiveTransitiveViolationsPopover.expiryTimesSelect().selectOption(1);
    testWaiveTransitiveViolations_Save(waiveTransitiveViolationsPopover, getExpectedExpiryDate(7), null);
  }

  private void testWaiveTransitiveViolations_Save(
      WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover,
      Date expectedExpiryTime,
      String expectedComment)
  {
    waiveTransitiveViolationsPopover.saveButton().click();
    waiveTransitiveViolationsPopover.shouldNotBe(Condition.visible);
    List<PolicyWaiver> policyWaivers = new PolicyWaiverDAO().getByOwnerId(application.getId());
    for (PolicyViolation policyViolation : policyViolations) {
      PolicyWaiver policyWaiver = findPolicyWaiver(policyWaivers, policyViolation);
      assertThat(policyWaiver.getExpiryTime()).isEqualTo(expectedExpiryTime);
      assertThat(policyWaiver.getComment()).isEqualTo(expectedComment);
    }
  }

  @Test
  public void testWaiveTransitiveViolations_SubmitError() throws Exception {
    WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover = visitWaivePopover();
    File reportFile = testCLMServer.getCLMServer().getInstance(InsightWork.class)
        .getReportFile(application.getId(), policyEvaluation.getScanId());
    new FileCleaner().delete(reportFile.getParentFile());
    waiveTransitiveViolationsPopover.saveButton().click();
    waiveTransitiveViolationsPopover.shouldBe(Condition.visible);
    waiveTransitiveViolationsPopover.submitError().shouldBe(Condition.visible);
    waiveTransitiveViolationsPopover.saveButton().shouldNotBe(Condition.visible);
    waiveTransitiveViolationsPopover.retryButton().shouldBe(Condition.visible).shouldHave(Condition.text("Retry"));

    ReportTestUtils.createReportFile(application.getId(), policyEvaluation.getScanId(),
        zipReportDir("/TransitiveViolationsTest/report", tempDir),
        testCLMServer.getCLMServer().getInstance(InsightWork.class));
    ReportTestUtils.createPolicyThreats(application.getId(), policyEvaluation.getScanId(),
        testCLMServer.getCLMServer().getInstance(InsightWork.class), policyViolations);
    waiveTransitiveViolationsPopover.retryButton().click();
    waiveTransitiveViolationsPopover.shouldNotBe(Condition.visible);
    new TransitiveViolationsPage().waiveTransitiveViolations().click();
    waiveTransitiveViolationsPopover.retryButton().shouldNotBe(Condition.visible);
    waiveTransitiveViolationsPopover.saveButton().shouldBe(Condition.visible).shouldHave(Condition.text("Save"));
  }

  @Test
  public void testViewTransitiveViolationWaivers() throws Exception {
    ComponentWaiversPopover componentWaiversPopover = visitViewWaiversPopover();
    componentWaiversPopover.title().shouldHave(Condition.text("Transitive Component Waivers"));
    componentWaiversPopover.componentWaiversPopoverTable().getRows().shouldHaveSize(1);
    componentWaiversPopover.componentWaiversPopoverTable().emptyTableMessage().shouldBe(Condition.visible);
    componentWaiversPopover.closePopoverButton().shouldBe(Condition.visible).click();

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
    Policy appPolicy = tempEntity.newPolicy(application.getId(), "appPolicy");
    Policy orgPolicy = tempEntity.newPolicy(organization.getId(), "orgPolicy");
    Policy rootOrgPolicy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "rootOrgPolicy");
    Date creationDate = DateUtils.parseIso8601Date("2000-05-01");
    PolicyWaiver appPolicyWaiver = tempEntity.newWaiver("hash2", appPolicy.getId(), application.getId(),
        getConstraintFacts(appPolicy), "comment", creationDate);
    PolicyWaiver orgPolicyWaiver = tempEntity.newWaiver("hash3", orgPolicy.getId(), organization.getId(),
        getConstraintFacts(orgPolicy), null, creationDate);
    PolicyWaiver rootOrgPolicyWaiver = tempEntity.newWaiver(null, rootOrgPolicy.getId(),
        Organization.ROOT_ORGANIZATION_ID, getConstraintFacts(rootOrgPolicy), null, creationDate);

    componentWaiversPopover = visitViewWaiversPopover();
    componentWaiversPopover.componentWaiversPopoverTable().getRows().shouldHaveSize(3);

    ElementsCollection appPolicyWaiverCells = componentWaiversPopover.componentWaiversPopoverTable().getRows()
        .find(Condition.text(application.getName())).findAll("td").shouldHaveSize(7);
    appPolicyWaiverCells.get(0).shouldHave(Condition.text(appPolicy.getName()));
    appPolicyWaiverCells.get(1)
        .shouldHave(Condition.text(appPolicyWaiver.getConstraintFacts().get(0).getConstraintName()));
    appPolicyWaiverCells.get(2).shouldHave(Condition.text(simpleDateFormat.format(appPolicyWaiver.getCreateTime())));
    appPolicyWaiverCells.get(3)
        .shouldHave(Condition.text(application.getType().name() + " - " + application.getName()));
    appPolicyWaiverCells.get(4).shouldHave(Condition.text("g : atransitivey : v"));
    appPolicyWaiverCells.get(5).shouldHave(Condition.text(appPolicyWaiver.getComment()));

    ElementsCollection orgPolicyWaiverCells = componentWaiversPopover.componentWaiversPopoverTable().getRows()
        .find(Condition.text(organization.getName())).findAll("td").shouldHaveSize(7);
    orgPolicyWaiverCells.get(0).shouldHave(Condition.text(orgPolicy.getName()));
    orgPolicyWaiverCells.get(1)
        .shouldHave(Condition.text(orgPolicyWaiver.getConstraintFacts().get(0).getConstraintName()));
    orgPolicyWaiverCells.get(2).shouldHave(Condition.text(simpleDateFormat.format(orgPolicyWaiver.getCreateTime())));
    orgPolicyWaiverCells.get(3)
        .shouldHave(Condition.text(organization.getType().name() + " - " + organization.getName()));
    orgPolicyWaiverCells.get(4).shouldHave(Condition.text("g : ZtransitiveY : v"));
    orgPolicyWaiverCells.get(5).shouldHave(Condition.text("- -"));

    ElementsCollection rootOrgPolicyWaiverCells = componentWaiversPopover.componentWaiversPopoverTable().getRows()
        .find(Condition.text(rootOrganization.getName())).findAll("td").shouldHaveSize(7);
    rootOrgPolicyWaiverCells.get(0).shouldHave(Condition.text(rootOrgPolicy.getName()));
    rootOrgPolicyWaiverCells.get(1)
        .shouldHave(Condition.text(rootOrgPolicyWaiver.getConstraintFacts().get(0).getConstraintName()));
    rootOrgPolicyWaiverCells.get(2)
        .shouldHave(Condition.text(simpleDateFormat.format(rootOrgPolicyWaiver.getCreateTime())));
    rootOrgPolicyWaiverCells.get(3).shouldHave(Condition.text(rootOrganization.getName()));
    rootOrgPolicyWaiverCells.get(4).shouldHave(Condition.text("All"));
    rootOrgPolicyWaiverCells.get(5).shouldHave(Condition.text("- -"));

    eyesWatcher.eyesCheck();

    orgPolicyWaiverCells.get(6).find(".nx-btn--delete-waiver").click();
    DeleteWaiverModal deleteWaiverModal = new DeleteWaiverModal();
    deleteWaiverModal.yesButton().click();
    componentWaiversPopover.componentWaiversPopoverTable().getRows().shouldHaveSize(2);
    componentWaiversPopover.componentWaiversPopoverTable().getRows().find(Condition.text(application.getName()))
        .shouldBe(visible);
    componentWaiversPopover.componentWaiversPopoverTable().getRows().find(Condition.text(rootOrganization.getName()))
        .shouldBe(visible);
  }

  private List<ConstraintFact> getConstraintFacts(Policy policy) {
    return policy.getConstraints().stream()
        .map(constraint ->
            new ConstraintFact(constraint.getId(), constraint.getName(), constraint.getOperator().name()))
        .collect(Collectors.toList());
  }

  private TransitiveViolationsPage visitPage() {
    return visitPage("hash1");
  }

  private TransitiveViolationsPage visitPage(String hash) {
    refreshOrOpen(TransitiveViolationsPage.url(application.getPublicId(), policyEvaluation.getScanId(), hash));
    TransitiveViolationsPage transitiveViolationsPage = new TransitiveViolationsPage();
    transitiveViolationsPage.shouldBe(Condition.visible);
    return transitiveViolationsPage;
  }

  private RequestWaiveTransitiveViolationsPopover visitRequestWaivePopover() {
    TransitiveViolationsPage transitiveViolationsPage = visitPage();
    transitiveViolationsPage.requestWaiveTransitiveViolations().click();
    RequestWaiveTransitiveViolationsPopover requestWaiveTransitiveViolationsPopover =
        new RequestWaiveTransitiveViolationsPopover();
    requestWaiveTransitiveViolationsPopover.shouldBe(Condition.visible);
    return requestWaiveTransitiveViolationsPopover;
  }

  private WaiveTransitiveViolationsPopover visitWaivePopover() {
    TransitiveViolationsPage transitiveViolationsPage = visitPage();
    transitiveViolationsPage.waiveTransitiveViolations().click();
    WaiveTransitiveViolationsPopover waiveTransitiveViolationsPopover = new WaiveTransitiveViolationsPopover();
    waiveTransitiveViolationsPopover.shouldBe(Condition.visible);
    return waiveTransitiveViolationsPopover;
  }

  private ComponentWaiversPopover visitViewWaiversPopover() {
    TransitiveViolationsPage transitiveViolationsPage = visitPage();
    transitiveViolationsPage.viewTransitiveViolationWaivers().click();
    ComponentWaiversPopover componentWaiversPopover = new ComponentWaiversPopover();
    componentWaiversPopover.shouldBe(Condition.visible);
    return componentWaiversPopover;
  }

  private PolicyViolationDetailPopover visitPolicyViolationDetailsPopover() {
    TransitiveViolationsPage transitiveViolationsPage = visitPage();
    transitiveViolationsPage.transitiveViolationsTable().row(1).click();
    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();
    policyViolationDetailPopover.shouldBe(Condition.visible);
    return policyViolationDetailPopover;
  }

  private String getExpectedDateTime(Date time) {
    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time);
  }

  private List<PolicyViolation> getExpectedPolicyViolations(
      String sortField,
      String policyNameFilter,
      String componentNameFilter)
  {
    List<PolicyViolation> result = policyViolations.stream()
        .filter(p -> policyNameFilter == null || p.getPolicyName().toLowerCase(Locale.ROOT)
            .contains(policyNameFilter.toLowerCase(Locale.ROOT)))
        .filter(p -> componentNameFilter == null || findComponent(p).getDisplayName().toLowerCase(Locale.ROOT)
            .contains(componentNameFilter.toLowerCase(Locale.ROOT)))
        .collect(Collectors.toList());
    switch (sortField == null ? "-threatLevel" : sortField) {
      case "threatLevel": {
        result.sort(Comparator.comparing(PolicyViolation::getThreatLevel));
        break;
      }
      case "-threatLevel": {
        result.sort(Comparator.comparing(PolicyViolation::getThreatLevel, Comparator.reverseOrder()));
        break;
      }
      case "policyName": {
        result.sort(Comparator.comparing(policyViolation -> policyViolation.getPolicyName().toLowerCase(Locale.ROOT)));
        break;
      }
      case "-policyName": {
        result.sort(Comparator.comparing(policyViolation -> policyViolation.getPolicyName().toLowerCase(Locale.ROOT),
            Comparator.reverseOrder()));
        break;
      }
      case "componentName": {
        result.sort(Comparator.comparing(p -> findComponent(p).getDisplayName().toLowerCase(Locale.ROOT)));
        break;
      }
      case "-componentName": {
        result.sort(Comparator.comparing(p -> findComponent(p).getDisplayName().toLowerCase(Locale.ROOT),
            Comparator.reverseOrder()));
        break;
      }
      default: {
        throw new RuntimeException("Unrecognized sortField " + sortField);
      }
    }
    return result;
  }

  private void assertRows(TransitiveViolationsTable transitiveViolationsTable, List<PolicyViolation> policyViolations) {
    transitiveViolationsTable.rows().shouldHaveSize(policyViolations.size());
    for (int row = 0; row < transitiveViolationsTable.rows().size(); row++) {
      assertRow(transitiveViolationsTable.row(row + 1), policyViolations.get(row));
    }
  }

  private void assertRow(TransitiveViolationsRow transitiveViolationsRow, PolicyViolation policyViolation) {
    transitiveViolationsRow.threat().shouldHave(Condition.text(String.valueOf(policyViolation.getThreatLevel())));
    transitiveViolationsRow.policyAndAction().shouldHave(Condition.text(policyViolation.getPolicyName()));
    if (Action.ID_FAIL.equals(policyViolation.getActionTypeId())) {
      transitiveViolationsRow.policyAndAction()
          .shouldHave(Condition.text("Failing " + policyViolation.getStageTypeId()));
    }
    else if (Action.ID_WARN.equals(policyViolation.getActionTypeId())) {
      transitiveViolationsRow.policyAndAction().shouldHave(Condition.text("Warning"));
    }
    Component component = findComponent(policyViolation);
    transitiveViolationsRow.component().shouldHave(Condition.text(component.getDisplayName()));
  }

  private Component findComponent(PolicyViolation policyViolation) {
    return components.stream().filter(c -> c.getHash().equals(policyViolation.getHash())).findFirst()
        .orElseThrow(() -> new RuntimeException("Component not found"));
  }

  private PolicyWaiver findPolicyWaiver(List<PolicyWaiver> policyWaivers, PolicyViolation policyViolation) {
    return policyWaivers.stream()
        .filter(policyWaiver -> policyWaiver.getHash() != null)
        .filter(policyWaiver -> policyWaiver.getHash().equals(policyViolation.getHash()))
        .filter(policyWaiver -> policyWaiver.getPolicyId().equals(policyViolation.getPolicyId()))
        .filter(policyWaiver -> policyWaiver.getConstraintFactsJson().equals(policyViolation.getConstraintFactsJson()))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Policy waiver not found"));
  }

  private Date getExpectedExpiryDate(int daysFromNow) {
    ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    return Date.from(LocalDateTime.now().plusDays(daysFromNow).withHour(23).withMinute(59).withSecond(59)
        .with(ChronoField.MILLI_OF_SECOND, 999).toInstant(offset));
  }
}
