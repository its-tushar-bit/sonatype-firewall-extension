/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.elements.AccessTile;
import com.sonatype.clm.testing.functional.elements.AccessTile.InheritedAccessList;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.CategoryTile;
import com.sonatype.clm.testing.functional.elements.ChangeApplicationIdDialog;
import com.sonatype.clm.testing.functional.elements.EvaluateApplicationModal;
import com.sonatype.clm.testing.functional.elements.EvaluationStatusModal;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.LabelTile;
import com.sonatype.clm.testing.functional.elements.LabelTile.InheritedLabelsList;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupSummaryTile;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupSummaryTile.ApplicableLicenseThreatGroupSection;
import com.sonatype.clm.testing.functional.elements.MoveOwnerDialog;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.elements.NxList;
import com.sonatype.clm.testing.functional.elements.NxToast;
import com.sonatype.clm.testing.functional.elements.OrgsAndPoliciesSidebar;
import com.sonatype.clm.testing.functional.elements.OwnerEditorDialog;
import com.sonatype.clm.testing.functional.elements.SelectContactModal;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.elements.SourceControlTile;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.utils.InputUtils;
import com.sonatype.clm.testing.functional.utils.NxColor;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.scan.ScanService;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.focused;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.textCaseSensitive;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationSummaryViewTest
    extends AbstractSummaryViewTest
{
  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  private final SourceControlDAO sourceControlDAO = new SourceControlDAO();

  private final OrganizationDAO organizationDAO = new OrganizationDAO();

  private Application application;

  private Application newApplication;

  private Organization rootOrganization;

  private Organization organization;

  @Rule
  public LogOutput logOutput = new LogOutput(ScanService.log.getName());

  @Before
  public void init() {
    //note the ȧ being used to force a character to be encoded
    application = tempEntity.newApplicationWithParent(getClass().getSimpleName() + "ȧpp", YE_OLE_APPLICATION,
        YE_OLE_ORGANIZATION);
    rootOrganization = organizationDAO.getByIdNotNull(ROOT_ORGANIZATION_ID);
    organization = tempEntity.newOrganization("An Org With A Very Very Very Very Very Very Very Very " +
        "Very Very Very Very Very Very Very Very Very Very Very Very Very Very Very Very Very V Very Very Very Very " +
        "Very Very Very Very Very Very Long Name");
    String id = "bfc6c69a39b94e81a777edf9727e01ce";
    newApplication = tempEntity.newApplication("Test App " + id, id, organization.getId());

    super.init(application);
  }

  @Test
  public void testApplicationContact() {
    User tempUser = tempEntity.newUser();
    OwnerSummaryPage.summaryTile().contact().shouldNotHave(text(tempUser.calculateDisplayName()));
    // open and close the contact modal
    ActionDropDown.actionButton().click();
    ActionDropDown.selectContact().shouldBe(visible).click();
    SelectContactModal.body().shouldBe(visible);
    SelectContactModal.header().shouldHave(SelectContactModal.headerText());
    SelectContactModal.users().shouldHaveSize(0);
    SelectContactModal.cancelButton().shouldBe(visible).click();
    SelectContactModal.body().shouldBe(hidden);
    OwnerSummaryPage.summaryTile().contact().shouldNotHave(text(tempUser.calculateDisplayName()));
    // wildcard search returns all users
    ActionDropDown.actionButton().click();
    ActionDropDown.selectContact().click();
    SelectContactModal.searchBox().shouldBe(visible).val("*");
    SelectContactModal.users().shouldHaveSize(2).shouldHave(texts("Admin Builtin", tempUser.calculateDisplayName()));

    // wildcard suffix search narrows search results
    SelectContactModal.searchBox().val(tempUser.getFirstName() + "*");
    SelectContactModal.users().shouldHaveSize(1).shouldHave(texts(tempUser.calculateDisplayName()));
    // update contact
    SelectContactModal.userContact(tempUser.calculateDisplayName()).click();
    SelectContactModal.updateButton().click();
    SelectContactModal.body().shouldBe(hidden);
    OwnerSummaryPage.summaryTile().contact().shouldHave(text(tempUser.calculateDisplayName()));
    // updated contact is retained upon page refresh
    refresh();
    OwnerSummaryPage.summaryTile().contact().shouldHave(text(tempUser.calculateDisplayName()));

    // remove contact
    ActionDropDown.actionButton().click();
    ActionDropDown.selectContact().shouldBe(visible).click();
    SelectContactModal.searchBox().click();
    SelectContactModal.searchBox().shouldBe(focused);
    InputUtils.clearInput(SelectContactModal.searchBox());
    SelectContactModal.updateButton().click();
    FormMask.seeAndWaitForDismissal();
    SelectContactModal.body().shouldBe(hidden);
    OwnerSummaryPage.summaryTile().contact().shouldNotHave(text(tempUser.calculateDisplayName()));
  }

  @Test
  public void testApplicationContact_withEditApplicationName() {
    User tempUser = tempEntity.newUser();
    OwnerSummaryPage.summaryTile().name().shouldHave(text(YE_OLE_APPLICATION));

    // open the contact modal
    ActionDropDown.actionButton().click();
    ActionDropDown.selectContact().shouldBe(visible).click();
    SelectContactModal.body().shouldBe(visible);
    SelectContactModal.searchBox().shouldBe(focused).val(tempUser.getFirstName() + "*");
    SelectContactModal.users().shouldHaveSize(1).shouldHave(texts(tempUser.calculateDisplayName()));
    // update contact
    SelectContactModal.userContact(tempUser.calculateDisplayName()).click();
    SelectContactModal.updateButton().click();
    SelectContactModal.body().shouldBe(hidden);
    OwnerSummaryPage.summaryTile().contact().shouldHave(text(tempUser.calculateDisplayName()));

    // edit the application name
    String shortTypeName = "App";
    String newAppName = "New Name";
    ActionDropDown.actionButton().click();
    OwnerEditorDialog.root().shouldBe(hidden);
    ActionDropDown.editOwner().shouldHave(text(shortTypeName)).click();
    OwnerEditorDialog.root().shouldBe(visible);
    OwnerEditorDialog.title().shouldHave(text(OwnerType.APPLICATION.toString()));
    OwnerEditorDialog.name().val(newAppName);
    OwnerEditorDialog.saveButton().shouldNotBe(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    OwnerSummaryPage.summaryTile().name().shouldHave(text(newAppName));
    OwnerSummaryPage.summaryTile().contact().shouldBe(visible).shouldHave(text(tempUser.calculateDisplayName()));
  }

  @Test
  public void testCopyApplicationIdToClipboard() {
    // open the action dropdown
    ActionDropDown.actionButton().click();
    ActionDropDown.copyAppIdButton().shouldBe(visible).click();
    NxToast toast = new NxToast("success");
    toast.shouldBe(visible);
    toast.shouldHave(text("Copied!"));
    toast.closeButton().shouldBe(visible).click();
    toast.shouldNotBe(visible);
  }

  @Test
  public void testActionsDropdownOptions() {
    ActionDropDown.actionButton().click();
    ActionDropDown.copyAppIdButton().shouldBe(visible);
    ActionDropDown.selectContact().shouldBe(visible);
    ActionDropDown.editOwner().shouldBe(visible);

    ActionDropDown.changeApplicationId().shouldBe(visible);
    ActionDropDown.moveOwner().shouldBe(visible);
    ActionDropDown.deleteOwnerButton().shouldBe(visible);
    ActionDropDown.legacyViolation().shouldBe(visible);
    ActionDropDown.revokeLegacyViolation().shouldBe(visible);
    ActionDropDown.evaluateFile().shouldBe(visible);

    ActionDropDown.actions().shouldHaveSize(9);
  }

  @Override
  @Test
  public void testReportLinks() {
    List<StageType> stages = new ArrayList<>();
    stages.add(StageTypes.SOURCE);
    stages.add(StageTypes.BUILD);
    stages.add(StageTypes.STAGE_RELEASE);
    stages.add(StageTypes.RELEASE);
    stages.add(StageTypes.OPERATE);

    final int stagesSize = stages.size();

    ActionDropDown.actionButton().click();
    ActionDropDown.reportLinks().shouldHaveSize(stagesSize);

    for (int i = 0; i < stagesSize; i++) {
      ActionDropDown.reportLink(i).shouldBe(visible, CLM.DISABLED)
          .shouldHave(ActionDropDown.reportLinkText(stages.get(i).getName()));
    }

    List<PolicyEvaluation> policyEvaluations = new ArrayList<>();

    for (StageType stage : stages) {
      policyEvaluations.add(tempEntity.newPolicyEvaluation(application.getId(), stage.getId(), stage.getId()
          + "FakeScanID"));
    }

    refresh();

    final int policyEvaluationsSize = policyEvaluations.size();
    ActionDropDown.actionButton().click();
    ActionDropDown.reportLinks().shouldHaveSize(policyEvaluationsSize);

    eyesWatcher.eyesCheck("Summary report links");

    for (int i = 0; i < policyEvaluationsSize; i++) {
      ActionDropDown.reportLink(i).shouldBe(visible).shouldNotBe(CLM.DISABLED)
          .shouldHave(ActionDropDown.reportLinkText(stages.get(i).getName()));

      ActionDropDown.reportLink(i).click();
      Selenide.switchTo().window(1);

      waitUntilUrl(ApplicationReportPage.url(application, policyEvaluations.get(i).getScanId()));

      WebDriverRunner.getWebDriver().close();
      Selenide.switchTo().window(0);

      waitUntilUrl(OwnerSummaryPage.url(application));

      ActionDropDown.actionButton().click();
    }
  }

  @Test
  public void testLTGTile_NoLocal() {
    LicenseThreatGroupSummaryTile ltgTile = OwnerSummaryPage.licenseThreatGroupSummaryTile();
    ScrollUtil.scrollIntoViewInstantly(ltgTile.nxHeader());

    ltgTile.nxHeader().shouldBe(visible).shouldHave(text("License Threat Groups"));
    ltgTile.newButton().shouldBe(hidden);

    ltgTile.getAllApplicableLicenseThreatGroupSection().shouldHaveSize(2);

    // Scroll table into view
    ScrollUtil.scrollIntoViewInstantly(ltgTile.licenseThreatGroupsTable());

    // Test local section content
    ApplicableLicenseThreatGroupSection section = ltgTile.getApplicableLicenseThreatGroupSection(0);
    section.getTitle().shouldBe(visible).shouldHave(text("Local to " + currentOwner.getName()));
    section.getEmptyDescriptor().shouldBe(visible);

    // Test inherited section content
    section = ltgTile.getApplicableLicenseThreatGroupSection(1);
    ScrollUtil.scrollIntoViewInstantly(section.getTitle());
    section.getTitle().shouldBe(visible).shouldHave(text("Inherited from Root Organization"));
    section.getEmptyDescriptor().shouldBe(hidden);
    section.getSectionContentRows().shouldHaveSize(LicenseThreatGroupDataHelper.TEST_LICENSE_THREAT_GROUP_COUNT);
  }

  @Override
  @Test
  public void testApplicationCategoryTile() {
    testApplicationCategoryTile_noneDefined();

    Tag category = tempEntity.newTag(application.getParentOwnerId(), "Test Tag", Color.dark_blue);
    refresh();

    testApplicationCategoryTile_Empty();
    testApplicationCategoryTile_WithAppliedCategory(category);
  }

  private void testApplicationCategoryTile_noneDefined() {
    CategoryTile categoryTile = OwnerSummaryPage.categoryTile();
    categoryTile.nxSubHeader().shouldBe(visible).shouldHave(CategoryTile.subHeaderText(application));
    categoryTile.newButton().shouldBe(visible).shouldHave(CategoryTile.buttonText(application), CLM.DISABLED);

    categoryTile.categoryLists().shouldHaveSize(1);

    NxList appliedCategoryList = categoryTile.categoryList(0);

    appliedCategoryList.emptyDescriptor().shouldBe(visible).shouldHave(CategoryTile.noneDefinedText());
    appliedCategoryList.elements().shouldBe(empty);
  }

  private void testApplicationCategoryTile_Empty() {
    CategoryTile categoryTile = OwnerSummaryPage.categoryTile();
    categoryTile.nxSubHeader().shouldBe(visible).shouldHave(CategoryTile.subHeaderText(application));
    categoryTile.newButton().shouldBe(visible, enabled).shouldHave(CategoryTile.buttonText(application));

    categoryTile.categoryLists().shouldHaveSize(1);

    NxList appliedCategoryList = categoryTile.categoryList(0);

    appliedCategoryList.emptyDescriptor().shouldBe(visible).shouldHave(CategoryTile.noneAssignedText());
    appliedCategoryList.elements().shouldBe(empty);
  }

  private void testApplicationCategoryTile_WithAppliedCategory(Tag category) {
    tempEntity.newApplicationTag(application.getId(), category.getId());

    refresh();

    CategoryTile categoryTile = OwnerSummaryPage.categoryTile();
    categoryTile.nxSubHeader().shouldBe(visible).shouldHave(CategoryTile.subHeaderText(application));
    categoryTile.newButton().shouldBe(visible, enabled).shouldHave(CategoryTile.buttonText(application));

    categoryTile.categoryLists().shouldHaveSize(1);

    NxList appliedCategoryList = categoryTile.categoryList(0);

    String nxColorClass = NxColor.getNxColorFromColor(category.getColor()).toNxClass();

    appliedCategoryList.emptyDescriptor().shouldBe(hidden);
    appliedCategoryList.elements().shouldHaveSize(1);
    appliedCategoryList.element(0).name().shouldBe(visible).shouldHave(text(category.getName()));
    appliedCategoryList.element(0).description().shouldBe(visible).shouldHave(text(category.getDescription()));
    appliedCategoryList.element(0).icon().shouldBe(visible).shouldHave(cssClass(nxColorClass));
    appliedCategoryList.element(0).chevron().shouldBe(hidden);
  }

  @Override
  @Test
  public void testActionDropDown() {
    super.testActionDropDown();

    testEvaluateFile(true, "mockApplicationBinary.war");
  }

  @Test
  public void testEvaluateApplication_Foundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    refresh();

    testEvaluateFile(false, "承銷競價拍賣系統_原碼.jar");
  }

  @Test
  public void testMoveApplicationLink() {
    MoveOwnerDialog moveAppModal = new MoveOwnerDialog();
    moveAppModal.shouldBe(hidden);
    ActionDropDown.actionButton().click();
    ActionDropDown.moveOwner().shouldBe(visible).shouldHave(text("Move " + application.getName())).click();
    moveAppModal.shouldBe(visible);
  }

  @Ignore
  @Test
  public void testChangeApplicationId() {
    ChangeApplicationIdDialog changeApplicationIdDialog = new ChangeApplicationIdDialog();
    changeApplicationIdDialog.shouldBe(hidden);
    ActionDropDown.actionButton().click();
    ActionDropDown.changeApplicationId().shouldBe(visible).shouldNotBe(DISABLED).click();
    changeApplicationIdDialog.shouldBe(visible);
    changeApplicationIdDialog.currentId().shouldHave(text(application.getPublicId()));
    changeApplicationIdDialog.newIdDiv().shouldHave(cssClass("pristine"));
    changeApplicationIdDialog.newId().shouldBe(Condition.empty);

    eyesWatcher.eyesCheck("Change application dialog");

    // current id is not a valid input
    changeApplicationIdDialog.newId().val(application.getPublicId());
    changeApplicationIdDialog.newIdDiv().shouldHave(cssClass("invalid"));
    changeApplicationIdDialog.newIdInvalidMessage().shouldBe(visible);
    // use invalid characters and assert the violation popover message
    String invalidCharsMessage = "Use valid characters: alphanumeric, \"_\", \".\" or \"-\"";
    changeApplicationIdDialog.newId().val("*");
    changeApplicationIdDialog.newIdInvalidMessage().shouldHave(text(invalidCharsMessage)).shouldBe(visible);
    // assert that the popover violation message for spaces is the same as invalid characters.
    changeApplicationIdDialog.newId().val("Spaced ID");
    changeApplicationIdDialog.newIdInvalidMessage().shouldHave(text(invalidCharsMessage)).shouldBe(visible);

    // now change the id to a new, valid one
    changeApplicationIdDialog.newId().val("newAppId");
    changeApplicationIdDialog.newIdDiv().shouldNotHave(cssClass("invalid"));
    changeApplicationIdDialog.newIdInvalidMessage().shouldNotBe(visible);
    changeApplicationIdDialog.changeButton().shouldNotHave(cssClass("disabled")).shouldBe(enabled).click();
    FormMask.seeAndWaitForDismissal();
    changeApplicationIdDialog.shouldBe(hidden);
    waitUntilUrl(OwnerSummaryPage.url(OwnerType.APPLICATION, "newAppId"));
    OwnerSummaryPage.summaryTile().publicId().shouldHave(text("newAppId"));
    // check that sidebar app link is updated
    OrgsAndPoliciesSidebar sideBar = new OrgsAndPoliciesSidebar();
    sideBar.getApplicationLink(0).click();
    waitUntilUrl(OwnerSummaryPage.url(OwnerType.APPLICATION, "newAppId"));

    // log in as a user that doesn't have permission to change the id of this app
    createUser();
    grantPermissions(getUsername(), application.getId(), Permission.READ);

    logout();
    login();

    try {
      refreshOrOpen(OwnerSummaryPage.url(OwnerType.APPLICATION, "newAppId"));
      ActionDropDown.actionButton().click();
      ActionDropDown.changeApplicationId().shouldBe(visible).shouldHave(DISABLED).click();
      changeApplicationIdDialog.shouldBe(hidden);
    }
    finally {
      logout();
      loginAsAdmin();
    }
  }

  private void testEvaluateFile(boolean isNotificationsAllowed, String filename) {
    File tempFile = null;

    try {
      tempFile = tempDir.newFile(filename);
    }
    catch (IOException e) {
      throw new AssertionError("Could not create temporary mock binary to evaluate. ", e);
    }
    finally {
      if (tempFile != null) {
        testCLMServer.getHdsServer().respondWith("{\"scanId\": \"blah\", \"timeToReport\": 0}")
            .atUri("rest/application/analysis");
        testCLMServer.getHdsServer().respondWith(getClass().getResource("/AppEvalReport/report.zip"))
            .atUri("rest/application/analysis/blah");

        ActionDropDown.actionButton().click();
        ActionDropDown.evaluateFile()
            .shouldBe(visible)
            .shouldNotBe(DISABLED)
            .shouldHave(text("Evaluate a File"))
            .click();

        EvaluateApplicationModal modal = new EvaluateApplicationModal();
        modal.shouldBe(visible);
        modal.dismissSelectedFileButton().shouldNot(visible);
        modal.fileInput().shouldBe(visible).sendKeys(tempFile.getAbsolutePath());

        //Check for NxFileUpload validation if no file is selected
        modal.dismissSelectedFileButton().shouldBe(visible).click();
        modal.fileUploadError().shouldBe(visible);
        modal.fileInput().shouldBe(visible).sendKeys(tempFile.getAbsolutePath());

        NxFormSelect stageSelect = modal.stageSelect();
        assertThat(stageSelect.selectedItem().getText()).isEqualTo(EvaluateApplicationModal.SELECT_STAGE_TEXT);
        stageSelect.click();
        stageSelect.listItems().shouldHaveSize(5).shouldHave(texts(EvaluateApplicationModal.SELECT_STAGE_TEXT,
            StageTypes.BUILD.getName(), StageTypes.STAGE_RELEASE.getName(), StageTypes.RELEASE.getName(),
            StageTypes.OPERATE.getName()));

        stageSelect.listItem(3).shouldHave(textCaseSensitive(StageTypes.RELEASE.getName())).click();
        assertThat(stageSelect.selectedItem().getText()).isEqualTo(StageTypes.RELEASE.getName());

        if (!isNotificationsAllowed) {
          modal.notificationsContainer().shouldNot(exist);
        }
        else {
          modal.notificationsContainer().should(exist);
          EvaluateApplicationModal.disabledNotificationsMessage().shouldBe(hidden);
          modal.notifyRadioButtons().yes().shouldBe(visible, selected);
          modal.notifyRadioButtons().no().shouldBe(visible).shouldNotBe(selected);
        }

        modal.cancelButton().shouldBe(visible, enabled);
        modal.uploadButton().shouldBe(visible, enabled).click();

        EvaluationStatusModal evaluationStatusModal = new EvaluationStatusModal();
        evaluationStatusModal.shouldBe(visible);
        evaluationStatusModal.bundleFileName().shouldBe(text(tempFile.getName()));
        evaluationStatusModal.bundleAppName().shouldBe(text(application.getName()));
        evaluationStatusModal.bundleStageName().shouldBe(textCaseSensitive(StageTypes.RELEASE.getName()));

        // Give a maximum of 1 minute for the file to be uploaded
        evaluationStatusModal.evaluateBundleStatus().waitUntil(text("Done"), 60000);

        evaluationStatusModal.closeButton().shouldBe(visible, enabled);

        PolicyEvaluation policyEvaluations = new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(
            application.getId(), StageTypes.RELEASE.getId());

        assertThat(policyEvaluations).isNotNull();

        eyesWatcher.eyesCheck("evaluate file dialog");

        evaluationStatusModal.viewReportButton().shouldBe(visible, enabled).click();

        Selenide.switchTo().window(1);

        waitUntilUrl(ApplicationReportPage.url(application, policyEvaluations.getScanId()));

        WebDriverRunner.getWebDriver().close();
        Selenide.switchTo().window(0);

        List<String> debugMessages = logOutput.getDebugMessages(ScanService.log.getName());
        String savingFileTo = debugMessages.stream().filter(s -> s.contains("Saving file to")).findFirst().get();
        assertThat(savingFileTo).contains(filename);
      }
    }
  }

  @Test
  public void testEvaluateFileBtnDisabledWithoutPermissions() {
    // log in as a user that doesn't have permission to evaluate this app
    createUser();
    grantPermissions(getUsername(), application.getId(), Permission.READ);

    logout();
    login();

    try {
      refreshOrOpen(OwnerSummaryPage.url(application));
      ActionDropDown.actionButton().click();
      ActionDropDown.evaluateFile().shouldBe(visible).shouldHave(DISABLED).hover();
      Tooltip.get().shouldBe(visible).shouldHave(text("Insufficient permissions to evaluate application"));
      ActionDropDown.evaluateFile().click();
      new EvaluateApplicationModal().shouldBe(hidden);
    }
    finally {
      logout();
      loginAsAdmin();
    }
  }

  @Test
  public void testImportPolicy_NotAvailable() {
    ActionDropDown.actionButton().click();
    ActionDropDown.importPoliciesButton().shouldNotBe(visible);
  }

  @Test
  public void testDataRetentionTile() {
    OwnerSummaryPage.summaryTile().dataRetentionButton().shouldNot(exist);
    OwnerSummaryPage.dataRetentionTile().shouldNot(exist);
  }

  @Test
  public void testSourceControlTile() {
    SourceControlTile tile = OwnerSummaryPage.sourceControlTile();

    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(tile.getElement());

    tile.shouldBe(visible);
    tile.nxSubHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s application", application.getName())));
    tile.rows().shouldHaveSize(1);

    tile.itemSubText().shouldNotBe(visible);
    tile.itemText().shouldBe(visible)
        .shouldHave(Condition.text("Source Control not configured"));

    SourceControl rootSourceControl =
        tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    refresh();

    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(tile.getElement());

    tile.shouldBe(visible);
    tile.nxSubHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s application", application.getName())));
    tile.rows().shouldHaveSize(1);

    tile.itemText().shouldBe(visible).shouldHave(Condition.text("Repository URL needed"));
    tile.itemSubText().shouldBe(visible).shouldHave(Condition.text("Inherit access token (GitHub)"));

    rootSourceControl.setToken("TESK_TOKEN");
    sourceControlDAO.update(rootSourceControl);
    refresh();

    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible);

    ScrollUtil.scrollIntoViewInstantly(tile.getElement());

    tile.shouldBe(visible);
    tile.nxSubHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s application", application.getName())));
    tile.rows().shouldHaveSize(1);

    tile.itemText().shouldBe(visible).shouldHave(Condition.text("Repository URL needed"));
    tile.itemSubText().shouldBe(visible).shouldHave(Condition.text(String
        .format("Inherit access token from %s (GitHub)", rootOrganization.getName())));

    tempEntity.newSourceControl(application.getId(), "http://github.com/aaa/bbb", "TEST_TOKEN", null);
    refresh();

    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible);

    ScrollUtil.scrollIntoViewInstantly(tile.getElement());

    tile.shouldBe(visible);
    tile.nxSubHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s application", application.getName())));
    tile.rows().shouldHaveSize(1);

    tile.itemText().shouldBe(visible).shouldHave(Condition.text("http://github.com/aaa/bbb"));
    tile.itemSubText().shouldBe(visible).shouldHave(Condition.text(String
        .format("Provides default access token for %s (GitHub)", application.getName())));
  }

  @Test
  public void testSourceControlTile_LicensingAwareNoLicense() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    SourceControlTile tile = OwnerSummaryPage.sourceControlTile();

    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible);

    ScrollUtil.scrollIntoViewInstantly(tile.getElement());

    tile.shouldBe(visible);
    tile.nxSubHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s application", application.getName())));
    tile.notSupported().shouldBe(visible);
    tile.content().shouldNotBe(visible);
    tile.notSupported().shouldHave(text("Source Control is not supported by your license"));

    tile.itemText().shouldNotBe(visible);
    tile.itemSubText().shouldNotBe(visible);
  }

  @Test
  public void testSourceControlTile_LicensingAwareNotificationOnly() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    SourceControlTile tile = OwnerSummaryPage.sourceControlTile();

    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible);

    ScrollUtil.scrollIntoViewInstantly(tile.getElement());

    tile.shouldBe(visible);
    tile.nxSubHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s application", application.getName())));
    tile.notSupported().shouldNotBe(visible);
    tile.content().shouldBe(visible);

    tile.itemText().shouldBe(visible);
  }

  @Test
  public void testSourceControlRepositoryHeader_github() {
    testSourceControl("http://localhost/my/app", SourceControlProvider.GITHUB, "fa-github");
  }

  @Test
  public void testSourceControlRepositoryHeader_gitlab() {
    testSourceControl("http://localhost/my/app", SourceControlProvider.GITLAB, "fa-gitlab");
  }

  @Test
  public void testSourceControlRepositoryHeader_bitbucket() {
    testSourceControl("http://localhost/scm/my/app", SourceControlProvider.BITBUCKET, "fa-bitbucket");
  }

  @Test
  public void testSourceControlRepositoryHeader_azure() {
    testSourceControl("http://localhost/user/prj/_git/app", SourceControlProvider.AZURE, "fa-git");
  }

  private void testSourceControl(String repoUrl, SourceControlProvider provider, String expectedIcon) {
    tempEntity.newSourceControl(application.getId(), repoUrl, "token", provider);

    refresh();

    OwnerSummaryPage.repositoryUrlAnchor().shouldHave(text(repoUrl));
    OwnerSummaryPage.repositoryUrlIcon().shouldHave(cssClass(expectedIcon));
  }

  @Test
  public void testSourceControlRepositoryHeader_noRepoGetsTerminalIcon() {
    refresh();

    OwnerSummaryPage.repositoryUrlAnchor().shouldNotBe(visible);
    OwnerSummaryPage.repositoryUrlIcon().shouldNotBe(visible);
  }

  @Test
  public void testLabelTile_Inherited_Truncation() {
    for (int i = 0; i < 20; i++) {
      tempEntity.newLabel(organization.getId());
    }

    refreshOrOpen(OwnerSummaryPage.url(newApplication));
    LabelTile labelTile = OwnerSummaryPage.labelTile();
    labelTile.labelLists().shouldHaveSize(1);
    labelTile.inheritedLabelsLists().shouldHaveSize(1);
    OwnerSummaryPage.summaryTile().labelsButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(labelTile.getElement());

    InheritedLabelsList inheritedLabelList = labelTile.inheritedLabelsList(organization.getId());
    inheritedLabelList.should(exist).shouldBe(visible);
    labelTile.labelListSubheader(1).shouldBe(visible).click();
    inheritedLabelList.should(exist).shouldNotBe(visible);
    labelTile.labelListSubheader(1).shouldHave(LabelTile.inheritedText(organization.getName()));
    ScrollUtil.scrollIntoViewInstantly(labelTile.getElement());
    eyesWatcher.eyesCheck("Inherited Component Labels Header Truncation");
  }

  @Test
  public void testAccessTile_Inherited_Truncation() {
    Role readRole = tempEntity.newRole("Read Only", false, Permission.READ);
    RoleDAO roleDAO = new RoleDAO();
    List<Role> roleList = new ArrayList<>(roleDAO.getApplicationRoles());
    tempEntity
        .newMembershipMapping(organization.getId(), readRole.getId(), "Group", MemberType.GROUP);
    roleList.add(readRole);

    refreshOrOpen(OwnerSummaryPage.url(newApplication));
    AccessTile accessTile = OwnerSummaryPage.accessTile();
    accessTile.accessLists().shouldHaveSize(2);
    accessTile.inheritedAccessLists().shouldHaveSize(1);
    OwnerSummaryPage.summaryTile().accessButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(accessTile.getElement());

    InheritedAccessList inheritedAccessList = accessTile.inheritedAccessList(organization.getId());
    inheritedAccessList.should(exist).shouldBe(visible);
    accessTile.accessListSubheader(0).shouldBe(visible).click();
    inheritedAccessList.should(exist).shouldNotBe(visible);
    accessTile.accessListSubheader(0).shouldHave(LabelTile.inheritedText(organization.getName()));
    ScrollUtil.scrollIntoViewInstantly(accessTile.getElement());
    eyesWatcher.eyesCheck("Inherited Access Header Truncation");
  }
}
