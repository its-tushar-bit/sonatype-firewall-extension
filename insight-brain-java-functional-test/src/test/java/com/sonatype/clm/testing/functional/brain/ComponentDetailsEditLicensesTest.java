
/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.NxTransferList;
import com.sonatype.clm.testing.functional.elements.componentdetails.EditLicensesPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.LicenseDetectionsTile;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentDetailsEditLicensesTest
    extends AbstractFunctionalTest
{
  private static final ComponentIdentifier JAVANCSS_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("javancss",
      "javancss", "29.50", "", "jar");

  private static final String JAVANCSS_HASH = "9aba4af169a1a3baa67f";

  private static final String SCAN_ID = "306e0a923df34c64b836358182b1b902";

  private LicenseOverrideDAO licenseOverrideDAO;

  private Application app;

  private TestReportEvaluator evaluator;

  private Organization parentOrg;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    licenseOverrideDAO = lookup(LicenseOverrideDAO.class);

    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);
    parentOrg = tempEntity.newOrganization("ParentApplicationReportTest");
    Organization org = tempEntity.newOrganization("ApplicationReportTest", parentOrg);
    app = tempEntity.newApplication("ApplicationReportTest",
        "ApplicationReportTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/small-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work);
    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
  }

  @Test
  public void testLegalTab_editLicensesPopover() throws Exception {
    mockHdsResponseForFirstComponent();
    refreshOrOpen(ComponentDetailsPage.urlToLegal(app, SCAN_ID, JAVANCSS_HASH));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTabContent().shouldBe(visible);

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);
    licenseDetectionsTile.editLicenseButton().click();

    // License Info Section
    EditLicensesPopover editLicensesPopover = new EditLicensesPopover();
    editLicensesPopover.shouldBe(visible);
    editLicensesPopover.popoverTitle().shouldHave(text("Edit Licenses"));

    ElementsCollection declaredLicenses = editLicensesPopover.getItems(editLicensesPopover.declaredLicenses());
    declaredLicenses.shouldHave(size(1));
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection observedLicenses = editLicensesPopover.getItems(editLicensesPopover.observedLicenses());
    observedLicenses.shouldHave(size(1));
    observedLicenses.first().shouldHave(text("GPL-2.0"));

    ElementsCollection effectiveLicenses = editLicensesPopover.getItems(editLicensesPopover.effectiveLicenses());
    effectiveLicenses.shouldHave(size(2));
    effectiveLicenses.first().shouldHave(text("Apache-2.0"));
    effectiveLicenses.last().shouldHave(text("GPL-2.0"));

    SelenideElement firstScope = editLicensesPopover.scope(0);
    SelenideElement secondScope = editLicensesPopover.scope(1);
    SelenideElement thirdScope = editLicensesPopover.scope(2);
    SelenideElement statusSelect = editLicensesPopover.status();
    Button saveButton = editLicensesPopover.saveButton();

    // Default states
    editLicensesPopover.availableScopes()
        .shouldHave(texts("Application - ApplicationReportTest", "Organization - ApplicationReportTest",
            "Organization - ParentApplicationReportTest", "Organization - Root Organization"));
    secondScope.shouldHave(text("Organization - ApplicationReportTest"));
    thirdScope.shouldHave(text("Organization - ParentApplicationReportTest"));
    editLicensesPopover.statuses()
        .shouldHave(
            texts("Open", "Acknowledged", "Overridden", "Selected", "Confirmed", "Inherit Status (Open)"));
    statusSelect.getSelectedOption().shouldHave(value("Open"));
    editLicensesPopover.selectedLicensesCheckBoxElements().shouldHave(size(0));

    // Update to 'Acknowledged' status for ParentApplicationReportTest Organization
    thirdScope.click();
    statusSelect.selectOptionContainingText("Acknowledged");
    editLicensesPopover.comment().setValue("Some comments");
    saveButton.shouldBe(enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();

    // Check UI for 'Acknowledged' override
    thirdScope.shouldBe(selected);
    thirdScope.shouldHave(text("Organization - ParentApplicationReportTest (Acknowledged)"));
    statusSelect.getSelectedOption().shouldHave(value("Acknowledged"));

    // Check backend for 'Acknowledged' override
    LicenseOverride override =
        licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(parentOrg.getId(),
            JAVANCSS_IDENTIFIER);
    assertThat(override.getStatus()).isEqualTo(LicenseOverrideStatus.ACKNOWLEDGED);

    // Update to 'Overridden' status for ParentApplicationReportTest Organization
    statusSelect.selectOptionContainingText("Overridden");
    NxTransferList overriddenField = editLicensesPopover.overriddenField();
    overriddenField.shouldBe(visible);
    overriddenField.transferredItems().shouldHave(size(0));
    overriddenField.availableItems().first().click();
    overriddenField.transferredItems().shouldHave(size(1));
    saveButton.shouldBe(enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();

    // Check UI for 'Overridden' override
    thirdScope.shouldBe(selected);
    thirdScope.shouldHave(text("Organization - ParentApplicationReportTest (Overridden)"));
    statusSelect.getSelectedOption().shouldHave(value("Overridden"));
    editLicensesPopover.declaredLicenses().scrollTo();
    effectiveLicenses.shouldHave(size(1));
    effectiveLicenses.first().shouldHave(text("0BSD"));

    // Check backend for 'Overridden' override
    override =
        licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(parentOrg.getId(), JAVANCSS_IDENTIFIER);
    assertThat(override.getStatus()).isEqualTo(LicenseOverrideStatus.OVERRIDDEN);
    assertThat(override.getLicenseIds().size()).isEqualTo(1);
    assertThat(override.getLicenseIds()).contains("0BSD");

    // Update to 'Acknowledged' status for ApplicationReportTest Organization
    secondScope.click();
    statusSelect.selectOptionContainingText("Acknowledged");
    editLicensesPopover.comment().setValue("Some comments");
    saveButton.shouldBe(enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();

    // Check UI for 'Acknowledged' override
    secondScope.shouldBe(selected);
    secondScope.shouldHave(text("Organization - ApplicationReportTest (Acknowledged)"));
    statusSelect.getSelectedOption().shouldHave(value("Acknowledged"));

    // Check backend for 'Acknowledged' override
    override =
        licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(app.getOrganizationId(),
            JAVANCSS_IDENTIFIER);
    assertThat(override.getStatus()).isEqualTo(LicenseOverrideStatus.ACKNOWLEDGED);

    // Update to 'Overridden' status for ApplicationReportTest Organization
    statusSelect.selectOptionContainingText("Overridden");
    overriddenField = editLicensesPopover.overriddenField();
    overriddenField.shouldBe(visible);
    overriddenField.transferredItems().shouldHave(size(0));
    overriddenField.availableItems().first().click();
    overriddenField.transferredItems().shouldHave(size(1));
    saveButton.shouldBe(enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();

    // Check UI for 'Overridden' override
    secondScope.shouldBe(selected);
    secondScope.shouldHave(text("Organization - ApplicationReportTest (Overridden)"));
    statusSelect.getSelectedOption().shouldHave(value("Overridden"));
    editLicensesPopover.declaredLicenses().scrollTo();
    effectiveLicenses.shouldHave(size(1));
    effectiveLicenses.first().shouldHave(text("0BSD"));

    // Check backend for 'Overridden' override
    override =
        licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(app.getParentOwnerId(), JAVANCSS_IDENTIFIER);
    assertThat(override.getStatus()).isEqualTo(LicenseOverrideStatus.OVERRIDDEN);
    assertThat(override.getLicenseIds().size()).isEqualTo(1);
    assertThat(override.getLicenseIds()).contains("0BSD");

    // update to 'Selected' status for Application
    firstScope.click();
    statusSelect.selectOptionContainingText("Selected");
    List<NxCheckbox> selectedLicensesCheckboxes = editLicensesPopover.selectedLicensesCheckbox();
    editLicensesPopover.selectedLicensesCheckBoxElements().shouldHave(size(2));

    NxCheckbox firstCheckbox = selectedLicensesCheckboxes.get(0);
    firstCheckbox.label().shouldBe(visible).shouldHave(text("Apache-2.0"));
    firstCheckbox.shouldNotBe(selected);

    NxCheckbox secondCheckbox = selectedLicensesCheckboxes.get(1);
    secondCheckbox.label().shouldBe(visible).shouldHave(text("GPL-2.0"));
    secondCheckbox.shouldNotBe(selected);

    firstCheckbox.label().click();
    firstCheckbox.shouldBe(selected);

    saveButton.shouldBe(enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();

    // Check UI for Application 'Selected' Override
    firstScope.shouldBe(selected);
    firstScope.shouldHave(text("Application - ApplicationReportTest (Selected)"));
    statusSelect.getSelectedOption().shouldHave(value("Selected"));
    editLicensesPopover.selectedLicensesCheckBoxElements().shouldHave(size(2));
    firstCheckbox.shouldBe(selected);
    effectiveLicenses.shouldHave(size(1));
    effectiveLicenses.first().shouldHave(text("Apache-2.0"));

    // Check backend for Application 'SELECTED' override
    override =
        licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(app.getId(), JAVANCSS_IDENTIFIER);
    assertThat(override.getStatus()).isEqualTo(LicenseOverrideStatus.SELECTED);
    assertThat(override.getLicenseIds().size()).isEqualTo(1);
    assertThat(override.getLicenseIds()).contains("Apache-2.0");

    // Check comments being rendered
    secondScope.click();
    editLicensesPopover.comment().shouldHave(value("Some comments"));

    // Remove Override from Application
    firstScope.click();
    statusSelect.selectOptionContainingText("Inherit Status (Overridden)");
    saveButton.shouldBe(enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();

    // Verify Application override has been removed on backend
    override =
        licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(app.getId(), JAVANCSS_IDENTIFIER);
    assertThat(override).isNull();

    eyesWatcher.eyesCheck("component details legal tab edit licenses popover");

    editLicensesPopover.getCloseButton().click();
    editLicensesPopover.shouldNotBe(visible);
  }

  private void testDefaultValuesAfterCloseWithoutSaving(
      LicenseDetectionsTile licenseDetectionsTile,
      EditLicensesPopover editLicensesPopover)
  {
    editLicensesPopover.getCloseButton().click();
    // CHECK FOR CONFIRMATION MODAL
    editLicensesPopover.unsavedModal().shouldBe(visible);
    editLicensesPopover.unsavedModalContinueButton().click();
    licenseDetectionsTile.shouldBe(visible);
    licenseDetectionsTile.editLicenseButton().click();

    ElementsCollection declaredLicenses = editLicensesPopover.getItems(editLicensesPopover.declaredLicenses());
    declaredLicenses.shouldHave(size(1));
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection observedLicenses = editLicensesPopover.getItems(editLicensesPopover.observedLicenses());
    observedLicenses.shouldHave(size(1));
    observedLicenses.first().shouldHave(text("GPL-2.0"));

    ElementsCollection effectiveLicenses = editLicensesPopover.getItems(editLicensesPopover.effectiveLicenses());
    effectiveLicenses.shouldHave(size(2));
    effectiveLicenses.first().shouldHave(text("Apache-2.0"));
    effectiveLicenses.last().shouldHave(text("GPL-2.0"));

    SelenideElement secondScope = editLicensesPopover.scope(1);
    SelenideElement statusSelect = editLicensesPopover.status();

    editLicensesPopover.comment().shouldBe(empty);

    // Default states
    editLicensesPopover.availableScopes()
        .shouldHave(texts("Application - ApplicationReportTest", "Organization - ApplicationReportTest",
            "Organization - ParentApplicationReportTest", "Organization - Root Organization"));
    secondScope.shouldHave(text("Organization - ApplicationReportTest"));
    editLicensesPopover.statuses()
        .shouldHave(
            texts("Open", "Acknowledged", "Overridden", "Selected", "Confirmed", "Inherit Status (Open)"));
    statusSelect.getSelectedOption().shouldHave(value("Open"));
    editLicensesPopover.selectedLicensesCheckBoxElements().shouldHave(size(0));
  }

  @Test
  public void testLegalTab_editAndCloseLicensesPopover() throws Exception {
    mockHdsResponseForFirstComponent();
    refreshOrOpen(ComponentDetailsPage.urlToLegal(app, SCAN_ID, JAVANCSS_HASH));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTabContent().shouldBe(visible);

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);
    licenseDetectionsTile.editLicenseButton().click();

    // License Info Section
    EditLicensesPopover editLicensesPopover = new EditLicensesPopover();
    editLicensesPopover.shouldBe(visible);
    editLicensesPopover.popoverTitle().shouldHave(text("Edit Licenses"));

    ElementsCollection declaredLicenses = editLicensesPopover.getItems(editLicensesPopover.declaredLicenses());
    declaredLicenses.shouldHave(size(1));
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection observedLicenses = editLicensesPopover.getItems(editLicensesPopover.observedLicenses());
    observedLicenses.shouldHave(size(1));
    observedLicenses.first().shouldHave(text("GPL-2.0"));

    ElementsCollection effectiveLicenses = editLicensesPopover.getItems(editLicensesPopover.effectiveLicenses());
    effectiveLicenses.shouldHave(size(2));
    effectiveLicenses.first().shouldHave(text("Apache-2.0"));
    effectiveLicenses.last().shouldHave(text("GPL-2.0"));

    SelenideElement firstScope = editLicensesPopover.scope(0);
    SelenideElement secondScope = editLicensesPopover.scope(1);
    SelenideElement statusSelect = editLicensesPopover.status();

    // Default states
    editLicensesPopover.availableScopes()
        .shouldHave(texts("Application - ApplicationReportTest", "Organization - ApplicationReportTest",
            "Organization - ParentApplicationReportTest", "Organization - Root Organization"));
    secondScope.shouldHave(text("Organization - ApplicationReportTest"));
    editLicensesPopover.statuses()
        .shouldHave(
            texts("Open", "Acknowledged", "Overridden", "Selected", "Confirmed", "Inherit Status (Open)"));
    statusSelect.getSelectedOption().shouldHave(value("Open"));
    editLicensesPopover.selectedLicensesCheckBoxElements().shouldHave(size(0));

    // Update to 'Acknowledged' status for ApplicationReportTest Organization
    secondScope.click();
    statusSelect.selectOptionContainingText("Acknowledged");
    editLicensesPopover.comment().setValue("Some comments");
    testDefaultValuesAfterCloseWithoutSaving(licenseDetectionsTile, editLicensesPopover);

    // Check backend for 'Acknowledged' override
    LicenseOverride override =
        licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(app.getOrganizationId(),
            JAVANCSS_IDENTIFIER);

    // Update to 'Overridden' status for ApplicationReportTest Organization
    statusSelect.selectOptionContainingText("Overridden");
    NxTransferList overriddenField = editLicensesPopover.overriddenField();
    overriddenField.shouldBe(visible);
    overriddenField.transferredItems().shouldHave(size(0));
    overriddenField.availableItems().first().click();
    overriddenField.transferredItems().shouldHave(size(1));
    testDefaultValuesAfterCloseWithoutSaving(licenseDetectionsTile, editLicensesPopover);

    // update to 'Selected' status for Application
    firstScope.click();
    statusSelect.selectOptionContainingText("Selected");
    List<NxCheckbox> selectedLicensesCheckboxes = editLicensesPopover.selectedLicensesCheckbox();
    editLicensesPopover.selectedLicensesCheckBoxElements().shouldHave(size(2));

    NxCheckbox firstCheckbox = selectedLicensesCheckboxes.get(0);
    firstCheckbox.label().shouldBe(visible).shouldHave(text("Apache-2.0"));
    firstCheckbox.shouldNotBe(selected);

    NxCheckbox secondCheckbox = selectedLicensesCheckboxes.get(1);
    secondCheckbox.label().shouldBe(visible).shouldHave(text("GPL-2.0"));
    secondCheckbox.shouldNotBe(selected);

    firstCheckbox.label().click();
    firstCheckbox.shouldBe(selected);
    testDefaultValuesAfterCloseWithoutSaving(licenseDetectionsTile, editLicensesPopover);

    // Verify Application override has been removed on backend
    override =
        licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(app.getId(), JAVANCSS_IDENTIFIER);
    assertThat(override).isNull();

    editLicensesPopover.getCloseButton().click();
    editLicensesPopover.shouldNotBe(visible);
  }

  private void mockHdsResponseForFirstComponent() throws Exception {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/javancssComponentDetails-29.50.json"))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(new ComponentDependenciesDTO(Collections.emptyMap(), Collections.emptyMap()))
        .atUri("rest/component/dependencies");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(Objects.requireNonNull(
                this.getClass().getResourceAsStream("/legal/legalLicenseMetadataHdsResponse.json")),
                StandardCharsets.UTF_8))
        .atUri("/rest/license/metadata");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream("/legal/legalCommentHdsResponse.json")),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/comment");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(Objects.requireNonNull(this.getClass()
                .getResourceAsStream("/legal/ApplicationAttributionReportTest-legalFileHdsResponse.json")),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/file");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/source-link");
  }
}
