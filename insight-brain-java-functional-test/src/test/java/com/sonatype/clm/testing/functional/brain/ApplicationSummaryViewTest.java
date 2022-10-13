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

import com.sonatype.clm.testing.functional.elements.*;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupSummaryTile.ApplicableLicenseThreatGroupSection;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView.OrganizationNode;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.utils.NxColor;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.*;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationSummaryViewTest
    extends AbstractSummaryViewTest
{
  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  private Application application;

  private Organization rootOrganization;

  private SourceControlDAO sourceControlDAO = new SourceControlDAO();

  private OrganizationDAO organizationDAO = new OrganizationDAO();

  @Before
  public void init() {
    //note the ȧ being used to force a character to be encoded
    application = tempEntity.newApplicationWithParent(getClass().getSimpleName() + "ȧpp", YE_OLE_APPLICATION,
        YE_OLE_ORGANIZATION);
    rootOrganization = organizationDAO.getByIdNotNull(ROOT_ORGANIZATION_ID);

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
    SelectContactModal.searchButton().shouldBe(disabled);
    SelectContactModal.cancelButton().shouldBe(visible).click();
    SelectContactModal.body().shouldBe(hidden);
    OwnerSummaryPage.summaryTile().contact().shouldNotHave(text(tempUser.calculateDisplayName()));
    // wildcard search returns all users
    ActionDropDown.actionButton().click();
    ActionDropDown.selectContact().click();
    SelectContactModal.searchBox().shouldBe(visible).val("*");
    SelectContactModal.searchButton().shouldBe(enabled).click();
    SelectContactModal.users().shouldHaveSize(2).shouldHave(texts("Admin Builtin", tempUser.calculateDisplayName()));

    // wildcard suffix search narrows search results
    SelectContactModal.searchBox().val(tempUser.getFirstName() + "*");
    SelectContactModal.searchButton().click();
    SelectContactModal.users().shouldHaveSize(1).shouldHave(texts(tempUser.calculateDisplayName()));
    // update contact
    SelectContactModal.updateButton().shouldHave(DISABLED);
    SelectContactModal.userRadio(tempUser.calculateDisplayName()).click();
    SelectContactModal.updateButton().shouldNotHave(DISABLED).click();
    SelectContactModal.body().shouldBe(hidden);
    OwnerSummaryPage.summaryTile().contact().shouldHave(text(tempUser.calculateDisplayName()));
    // updated contact is retained upon page refresh
    refresh();
    OwnerSummaryPage.summaryTile().contact().shouldHave(text(tempUser.calculateDisplayName()));
    eyesWatcher.eyesCheck("Contact selected");
    // attempt removal but cancel out of confirmation dialog
    ActionDropDown.actionButton().click();
    ActionDropDown.selectContact().shouldBe(visible).click();
    SelectContactModal.currentUserLabel().shouldHave(text(tempUser.calculateDisplayName()));
    SelectContactModal.searchBox().val("preserves modal state");
    SelectContactModal.removeButton().shouldBe(visible, enabled).click();
    SelectContactModal.body().shouldBe(hidden);
    RemoveModal.body().shouldBe(visible).shouldHave(RemoveModal.bodyText(tempUser.calculateDisplayName()));
    RemoveModal.header().shouldHave(RemoveModal.headerText("Contact"));
    RemoveModal.cancelButton().click();
    RemoveModal.body().shouldBe(hidden);
    SelectContactModal.body().shouldBe(visible);
    SelectContactModal.searchBox().shouldHave(value("preserves modal state"));
    // remove contact
    SelectContactModal.removeButton().click();
    RemoveModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    RemoveModal.body().shouldBe(hidden);
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
    SelectContactModal.searchButton().click();
    SelectContactModal.users().shouldHaveSize(1).shouldHave(texts(tempUser.calculateDisplayName()));
    // update contact
    SelectContactModal.userRadio(tempUser.calculateDisplayName()).click();
    SelectContactModal.updateButton().shouldNotHave(DISABLED).click();
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
    ActionDropDown.moveApplication().shouldBe(visible);
    ActionDropDown.deleteOwnerButton().shouldBe(visible);
    ActionDropDown.grandfather().shouldBe(visible);
    ActionDropDown.revokeGrandfathered().shouldBe(visible);
    ActionDropDown.evaluateFile().shouldBe(visible);

    ActionDropDown.actions().shouldHaveSize(9);

    eyesWatcher.eyesCheck("application actions dropdown");
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
    ScrollUtil.scrollIntoView(ltgTile.nxHeader());
    ltgTile.nxHeader().shouldBe(visible).shouldHave(text("License Threat Groups"));
    ltgTile.nxSubHeader().shouldBe(visible).shouldHave(LabelTile.subHeaderText(application.getName()));
    ltgTile.newButton().shouldBe(hidden);

    ltgTile.getAllApplicableLicenseThreatGroupSection().shouldHaveSize(1);
    ScrollUtil.scrollIntoView(ltgTile.nxHeader());
    eyesWatcher.eyesCheck("Application License Threat Group Tile with no local threats");

    ApplicableLicenseThreatGroupSection section = ltgTile.getApplicableLicenseThreatGroupSection(0);
    ScrollUtil.scrollIntoView(section.getTitle());
    section.getTitle().shouldBe(visible).shouldHave(text("INHERITED FROM ROOT ORGANIZATION"));
    section.getEmptyDescriptor().shouldBe(hidden);
    section.getTableContent().shouldHaveSize(LicenseThreatGroupDataHelper.TEST_LICENSE_THREAT_GROUP_COUNT);
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

    eyesWatcher.eyesCheck("Application Category Tile when there is no defined categories");
  }

  private void testApplicationCategoryTile_Empty() {
    CategoryTile categoryTile = OwnerSummaryPage.categoryTile();
    categoryTile.nxSubHeader().shouldBe(visible).shouldHave(CategoryTile.subHeaderText(application));
    categoryTile.newButton().shouldBe(visible, enabled).shouldHave(CategoryTile.buttonText(application));

    categoryTile.categoryLists().shouldHaveSize(1);

    NxList appliedCategoryList = categoryTile.categoryList(0);

    appliedCategoryList.emptyDescriptor().shouldBe(visible).shouldHave(CategoryTile.noneAssignedText());
    appliedCategoryList.elements().shouldBe(empty);

    eyesWatcher.eyesCheck("Application Category Tile with no category assigned");
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

    eyesWatcher.eyesCheck("Application Category Tile with applied category");
  }

  @Override
  @Test
  public void testActionDropDown() {
    super.testActionDropDown();

    testEvaluateFile(true);
  }

  @Test
  public void testEvaluateApplication_Foundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    refresh();

    testEvaluateFile(false);
  }

  @Test
  public void testMoveApplicationLink() {
    MoveApplicationDialog moveAppModal = new MoveApplicationDialog();
    moveAppModal.shouldBe(hidden);
    ActionDropDown.actionButton().click();
    ActionDropDown.moveApplication().shouldBe(visible).shouldHave(text("Move " + application.getName())).click();
    moveAppModal.shouldBe(visible);

    eyesWatcher.eyesCheck("Move application modal");
  }

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
    changeApplicationIdDialog.changeButton().shouldHave(cssClass("disabled"));

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
    OrganizationNode organizationNode = OwnerTreeView.organization(0);
    organizationNode.treeViewElement().click();
    waitUntilNotUrl(OwnerSummaryPage.url(OwnerType.APPLICATION, "newAppId"));
    organizationNode.application(0).click();
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

  private void testEvaluateFile(boolean isNotificationsAllowed) {
    File tempFile = null;

    try {
      tempFile = tempDir.newFile("mockApplicationBinary.war");
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
        modal.fileInput().shouldBe(visible).sendKeys(tempFile.getAbsolutePath());

        Dropdown stageDropdown = modal.stageDropdown();
        stageDropdown.selectedItem().shouldHave(text(EvaluateApplicationModal.SELECT_STAGE_TEXT)).click();
        stageDropdown.listItems().shouldHaveSize(4).shouldHave(texts(StageTypes.BUILD.getName(),
            StageTypes.STAGE_RELEASE.getName(), StageTypes.RELEASE.getName(), StageTypes.OPERATE.getName()));

        stageDropdown.listItem(2).shouldHave(textCaseSensitive(StageTypes.RELEASE.getName())).click();
        stageDropdown.selectedItem().shouldBe(textCaseSensitive(StageTypes.RELEASE.getName()));

        if (!isNotificationsAllowed) {
          EvaluateApplicationModal.disabledNotificationsMessage()
              .shouldBe(text("Notifications are not supported by your license."));
        }
        else {
          EvaluateApplicationModal.disabledNotificationsMessage().shouldBe(hidden);
        }
        Condition disabledOrEnabled = !isNotificationsAllowed ? disabled : enabled;
        modal.notifyRadioButtons().yes().shouldBe(visible, selected, disabledOrEnabled);
        modal.notifyRadioButtons().no().shouldBe(visible, disabledOrEnabled).shouldNotBe(selected);

        modal.cancelButton().shouldBe(visible, enabled);
        modal.uploadButton().shouldBe(visible, enabled).click();

        modal.bundleFileName().shouldBe(text(tempFile.getName()));
        modal.bundleAppName().shouldBe(text(application.getName()));
        modal.bundleStageName().shouldBe(textCaseSensitive(StageTypes.RELEASE.getName()));

        // Give a maximum of 1 minute for the file to be uploaded
        modal.evaluateBundleStatus().waitUntil(text("Done"), 60000);

        modal.closeButton().shouldBe(visible, enabled);

        PolicyEvaluation policyEvaluations = new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(
            application.getId(), StageTypes.RELEASE.getId());

        assertThat(policyEvaluations).isNotNull();

        eyesWatcher.eyesCheck("evaluate file dialog");

        modal.viewReportButton().shouldBe(visible, enabled).click();

        Selenide.switchTo().window(1);

        waitUntilUrl(ApplicationReportPage.url(application, policyEvaluations.getScanId()));

        WebDriverRunner.getWebDriver().close();
        Selenide.switchTo().window(0);
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
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible).click();

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
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    tile.nxSubHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s application", application.getName())));
    tile.rows().shouldHaveSize(1);

    tile.itemText().shouldBe(visible).shouldHave(Condition.text("Repository URL needed"));
    tile.itemSubText().shouldBe(visible).shouldHave(Condition.text("Inherit access token (GitHub)"));

    eyesWatcher.eyesCheck("Application Source Control configured without URL");

    rootSourceControl.setToken("TESK_TOKEN");
    sourceControlDAO.update(rootSourceControl);
    refresh();

    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    tile.nxSubHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s application", application.getName())));
    tile.rows().shouldHaveSize(1);

    tile.itemText().shouldBe(visible).shouldHave(Condition.text("Repository URL needed"));
    tile.itemSubText().shouldBe(visible).shouldHave(Condition.text(String
        .format("Inherit access token from %s (GitHub)", rootOrganization.getName())));

    eyesWatcher.eyesCheck("Source Control configured without URL. Inherit token from Organization");

    tempEntity.newSourceControl(application.getId(), "http://github.com/aaa/bbb", "TEST_TOKEN", null);
    refresh();

    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    tile.nxSubHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s application", application.getName())));
    tile.rows().shouldHaveSize(1);

    tile.itemText().shouldBe(visible).shouldHave(Condition.text("http://github.com/aaa/bbb"));
    tile.itemSubText().shouldBe(visible).shouldHave(Condition.text(String
        .format("Provides default access token for %s (GitHub)", application.getName())));

    eyesWatcher.eyesCheck("Source Control configured with URL and token on application");
  }

  @Test
  public void testSourceControlTile_LicensingAwareNoLicense() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    SourceControlTile tile = OwnerSummaryPage.sourceControlTile();

    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    tile.nxSubHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s application", application.getName())));
    tile.notSupported().shouldBe(visible);
    tile.content().shouldNotBe(visible);
    tile.notSupported().shouldHave(text("Source Control is not supported by your license"));

    tile.itemText().shouldNotBe(visible);
    tile.itemSubText().shouldNotBe(visible);

    eyesWatcher.eyesCheck("Source control no license");
  }

  @Test
  public void testSourceControlTile_LicensingAwareNotificationOnly() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    SourceControlTile tile = OwnerSummaryPage.sourceControlTile();

    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible).click();

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
}
