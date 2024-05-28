/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.legal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.componentdetails.EditLicensesPopover;
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.text;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class EditLicensesTest
    extends AbstractFunctionalTest
{
  private Application app;

  private Repository repository;

  private LicenseOverrideDAO licenseOverrideDAO;

  private RepositoryManagerDAO repositoryManagerDAO;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    licenseOverrideDAO = lookup(LicenseOverrideDAO.class);
    repositoryManagerDAO = lookup(RepositoryManagerDAO.class);

    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);
  }

  private void init(String hash, ComponentIdentifier componentIdentifier, String testFileSuffix) throws IOException {
    app = tempEntity.newApplicationWithParent(EditLicensesTest.class.getSimpleName(), "app", "org");
    repository = tempEntity.newRepository();

    ApplicationComponent applicationComponent =
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash, componentIdentifier);
    tempEntity.newApplicationComponentLicense(applicationComponent.getId(), "MIT");

    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalLicenseMetadataHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/license/metadata");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalCommentHdsResponse" + testFileSuffix + ".json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/comment");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/file");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/source-link");

    testCLMServer.getHdsServer()
        .respondWith(
            IOUtils.toString(this.getClass().getResourceAsStream("/legal/componentDetails" + testFileSuffix + ".json"),
            StandardCharsets.UTF_8))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils.toString(this.getClass().getResourceAsStream("/legal/componentDetailsList.json"),
            StandardCharsets.UTF_8))
        .atUri("rest/ci/componentDetails/list");

  }

  @After
  public void after() {
    licenseOverrideDAO.getByOwnerId(ROOT_ORGANIZATION_ID).forEach(licenseOverrideDAO::delete);
  }

  @Test
  public void testEditLicenseByHash() throws IOException {
    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");
    init("033e7a20b23ea284d474", componentId, "");
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));

    ComponentLegalOverviewPage.editLicensesButton().click();
    EditLicensesPopover editLicensesPopover = new EditLicensesPopover();

    SelenideElement firstScope = editLicensesPopover.scope(0);
    SelenideElement secondScope = editLicensesPopover.scope(1);
    SelenideElement thirdScope = editLicensesPopover.scope(2);
    editLicensesPopover.should(Condition.appear);
    assertThat(editLicensesPopover.popoverTitle().getText()).isEqualTo("Edit Licenses");
    assertThat(editLicensesPopover.status().getText()).isEqualTo("Open");
    assertThat(editLicensesPopover.comment().getText()).isEmpty();
    firstScope.shouldHave(text("Application - app"));
    secondScope.shouldHave(text("Organization - org"));
    thirdScope.shouldHave(text("Organization - Root Organization"));
    editLicensesPopover.statuses()
        .shouldHave(texts("Open", "Acknowledged", "Overridden", "Selected", "Confirmed", "Inherit Status (Open)"));
    editLicensesPopover.selectedLicensesCheckBoxElements().shouldHave(CollectionCondition.size(0));

    editLicensesPopover.saveButton().click();
    FormUtils.getAlertElement(editLicensesPopover).shouldHave(text("There are no changes to update."));
    eyesWatcher.eyesCheck();

    editLicensesPopover.cancelButton().click();
    editLicensesPopover.shouldNotBe(Condition.visible);
  }

  @Test
  public void testEditLicenseByComponentIdentifier() throws IOException {
    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "jar");
    init("02744a3ac66344569f0b", componentId, "2");
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentId, repository.getId()));

    ComponentLegalOverviewPage.editLicensesButton().click();
    EditLicensesPopover editLicensesPopover = new EditLicensesPopover();

    SelenideElement firstScope = editLicensesPopover.scope(0);
    SelenideElement secondScope = editLicensesPopover.scope(1);
    SelenideElement thirdScope = editLicensesPopover.scope(2);
    SelenideElement fourthScope = editLicensesPopover.scope(3);
    editLicensesPopover.should(Condition.appear);
    assertThat(editLicensesPopover.popoverTitle().getText()).isEqualTo("Edit Licenses");
    assertThat(editLicensesPopover.status().getText()).isEqualTo("Open");
    assertThat(editLicensesPopover.comment().getText()).isEmpty();
    RepositoryManager repositoryManager = repositoryManagerDAO.getById(repository.getRepositoryManagerId());
    firstScope.shouldHave(text("Repository - " + repository.getName()));
    secondScope.shouldHave(text("Repository Manager - " + repositoryManager.getName()));
    thirdScope.shouldHave(text("Repository Managers"));
    fourthScope.shouldHave(text("Organization - Root Organization"));
    editLicensesPopover.statuses().shouldHave(
        texts("Open", "Acknowledged", "Overridden", "Selected", "Confirmed", "Inherit Status (Open)"));
    editLicensesPopover.selectedLicensesCheckBoxElements().shouldHave(CollectionCondition.size(0));

    editLicensesPopover.saveButton().click();
    FormUtils.getAlertElement(editLicensesPopover)
        .shouldHave(text("There are no changes to update."));
    eyesWatcher.eyesCheck();

    editLicensesPopover.cancelButton().click();
    editLicensesPopover.shouldNotBe(Condition.visible);
  }
}
