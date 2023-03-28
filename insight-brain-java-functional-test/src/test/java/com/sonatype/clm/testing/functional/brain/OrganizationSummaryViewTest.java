/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.elements.*;
import com.sonatype.clm.testing.functional.elements.CategoryTile.InheritedCategoriesList;
import com.sonatype.clm.testing.functional.elements.CategoryTile.InheritedCategory;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupSummaryTile.ApplicableLicenseThreatGroupSection;
import com.sonatype.clm.testing.functional.elements.PolicyTileList.PolicyTileListElement;
import com.sonatype.clm.testing.functional.pages.DataRetentionEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.WaivedComponentUpgradeConfigurationPage;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.clm.testing.functional.utils.NxColor;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.back;
import static com.sonatype.clm.testing.functional.utils.FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationSummaryViewTest
    extends AbstractSummaryViewTest
{
  private Organization organization;

  private Organization rootOrganization;

  private final OrganizationDAO organizationDAO = new OrganizationDAO();

  private final SourceControlDAO sourceControlDAO = new SourceControlDAO();

  private String waivedComponentUpgradeStageTypeId;

  @Before
  public void init() {
    organization = tempEntity.newOrganization(YE_OLE_ORGANIZATION);
    rootOrganization = organizationDAO.getByIdNotNull(ROOT_ORGANIZATION_ID);
    waivedComponentUpgradeStageTypeId = rootOrganization.getWaivedComponentUpgradeStageTypeId();
    super.init(organization);
    SidebarNavigation.closeNavigationSidebar();
  }

  @After
  public void restoreRootOrganizationState() {
    Organization rootOrganization = organizationDAO.getByIdNotNull(ROOT_ORGANIZATION_ID);
    rootOrganization.setWaivedComponentUpgradeStageTypeId(waivedComponentUpgradeStageTypeId);
    organizationDAO.update(rootOrganization);
  }

  @Test
  public void testActionsDropdownOptions() {
    ActionDropDown.actionButton().click();
    ActionDropDown.copyOrgIdButton().shouldBe(visible);
    ActionDropDown.editOwner().shouldBe(visible);
    ActionDropDown.importPoliciesButton().shouldBe(visible);
    ActionDropDown.deleteOwnerButton().shouldBe(visible);
    ActionDropDown.actions().shouldHaveSize(4);

    eyesWatcher.eyesCheck("organization actions dropdown");
  }

  @Override
  @Test
  public void testReportLinks() {
    ActionDropDown.actionButton().click();
    ActionDropDown.reportLinks().shouldBe(empty);
  }

  @Test
  public void testLTGTile_NoLocal() {
    int hierarchySize = getHierarchySize(organization);

    LicenseThreatGroupSummaryTile ltgTile = OwnerSummaryPage.licenseThreatGroupSummaryTile();

    ScrollUtil.scrollIntoViewInstantly(ltgTile.nxHeader());

    ltgTile.nxHeader().shouldBe(visible).shouldHave(text("License Threat Groups"));
    ltgTile.nxSubHeader().shouldBe(visible).shouldHave(LabelTile.subHeaderText(organization.getName()));
    ltgTile.addLTGButton().shouldBe(visible, enabled);

    ltgTile.getAllApplicableLicenseThreatGroupSection().shouldHaveSize(hierarchySize);

    for (int i = 0; i < hierarchySize; i++) {
      ApplicableLicenseThreatGroupSection section = ltgTile.getApplicableLicenseThreatGroupSection(i);
      ScrollUtil.scrollIntoViewInstantly(section.getTitle());

      if (i == 0) {
        section.getTitle().shouldBe(visible).shouldHave(text("Local"));
        section.getEmptyDescriptor().shouldBe(visible).shouldHave(text("No local threat groups defined"));
      }
      else {
        section.getTitle().shouldBe(visible);
        section.getEmptyDescriptor().shouldBe(hidden);
        section.getTableContent().shouldHaveSize(LicenseThreatGroupDataHelper.TEST_LICENSE_THREAT_GROUP_COUNT);
      }
    }
  }

  @Override
  @Test
  public void testApplicationCategoryTile() {
    testApplicationCategoryTile_Empty();
    testApplicationCategoryTile_WithApplicableCategories();
  }

  @Test
  public void testMoveApplicationLink() {
    ActionDropDown.actionButton().click();
    ActionDropDown.moveApplication().shouldBe(hidden);
  }

  @Test
  public void testCopyOrganizationIdToClipboard() {
    // open the action dropdown
    ActionDropDown.actionButton().click();
    ActionDropDown.copyOrgIdButton().shouldBe(visible).click();
    NxToast toast = new NxToast("success");
    toast.shouldBe(visible);
    toast.shouldHave(text("Copied!"));
    toast.closeButton().shouldBe(visible).click();
    toast.shouldNotBe(visible);
  }

  @Test
  public void testRenameOrg() {
    ActionDropDown.actionButton().click();
    ActionDropDown.editOwner().shouldBe(visible).click();
    OwnerEditorDialog.root().shouldBe(visible);
    OwnerEditorDialog.name().val("New Org name");
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().shouldBe(hidden);
    OwnerSummaryPage.summaryTile().name().shouldHave(text("New Org name"));
  }

  @Test
  public void testImportPolicy() {
    String filePath = new File(getClass().getResource("/policyExport/sampleNonJsonFile.file")
            .getFile()).getAbsolutePath();

    ActionDropDown.actionButton().click();
    ActionDropDown.importPoliciesButton().shouldBe(visible).click();

    ImportPolicyModal.root().shouldBe(visible);
    ImportPolicyModal.importButton().shouldBe(visible).click();
    FormUtils.getAlertElement()
        .shouldHave(text(DEFAULT_VALIDATION_ERRORS_PREFIX + " Unable to save: fields with invalid or missing data"));

    // ANY file enables the "Import" button
    ImportPolicyModal.fileInput().shouldBe(visible).sendKeys(filePath);
    ImportPolicyModal.importButton().shouldBe(visible).shouldNotHave(cssClass("disabled"));

    // Ensure non-JSON file is not accepted by backend validation
    ImportPolicyModal.importButton().waitUntil(enabled, 2000).click();
    ImportPolicyModal.errorMessage()
        .shouldBe(visible)
        .shouldHave(
          text(
            "An error occurred saving data. The file you selected failed to upload correctly, are you certain it " +
            "is a properly formatted policy import json file?"
          )
      );

    // Clear file selection
    ImportPolicyModal.fileInputClearButton().click();
    ImportPolicyModal.fileInputRequiredFieldError().shouldBe(visible);

    // Select valid JSON file
    filePath = new File(getClass().getResource("/policyExport/samplePolicy.json").getFile()).getAbsolutePath();

    ImportPolicyModal.fileInput().shouldBe(visible).sendKeys(filePath);
    ImportPolicyModal.fileInputRequiredFieldError().shouldNotBe(visible);

    // Give a maximum of 2 seconds for the file to be loaded
    ImportPolicyModal.errorRetryButton().waitUntil(enabled, 2000).click();

    // verify mask and wait for it to go away
    FormMask.seeAndWaitForDismissal();

    // scroll to the labels tile
    OwnerSummaryPage.summaryTile().labelsButton().shouldBe(visible);

    LabelTile labelTile = OwnerSummaryPage.labelTile();
    ScrollUtil.scrollIntoViewInstantly(labelTile.getElement());

    labelTile.labelList(0);
    NxList list = labelTile.labelList(0);
    labelTile.labelListSubheader(0).shouldBe(visible).shouldHave(text("Local to " + organization.getName()));
    list.elements().shouldHaveSize(1);
    NxList.NxListItem actualLabel = list.element(0);
    actualLabel.name().shouldBe(visible).shouldHave(text("Test Label"));

    // scroll to the ltgs
    OwnerSummaryPage.summaryTile().ltgsButton().shouldBe(visible);
    LicenseThreatGroupSummaryTile ltgTile = OwnerSummaryPage.licenseThreatGroupSummaryTile();
    ScrollUtil.scrollIntoViewInstantly(ltgTile.getElement());

    ApplicableLicenseThreatGroupSection section = ltgTile.getApplicableLicenseThreatGroupSection(0);
    section.getEmptyDescriptor().shouldNot(exist);
    section.getLTG(0).shouldHave(text("Test LTG"));
    section.getTableContent().shouldHaveSize(1);

    eyesWatcher.eyesCheck("license threat group tile after policy import");

    // scroll to the policy tile
    OwnerSummaryPage.summaryTile().policyButton().shouldBe(visible);
    PolicyTile policyTile = OwnerSummaryPage.policyTile();
    ScrollUtil.scrollIntoViewInstantly(policyTile.getElement());

    PolicyTileList policyList = policyTile.policyList(0);
    policyList.emptyDescriptor().shouldBe(hidden);
    policyList.rows().shouldHaveSize(2); // 1 row plus header
    policyList.ownerName().shouldBe(visible).shouldHave(text("Local"));
    PolicyTileListElement policyElement = policyList.row(1);
    policyElement.name().shouldBe(visible).shouldHave(text("Test"));
    policyElement.proxy().shouldBe(visible).shouldHave(text("warn"));

    eyesWatcher.eyesCheck("policy tile after policy import");

    // scroll to the application categories tile
    OwnerSummaryPage.summaryTile().appCategoriesButton().shouldBe(visible);
    CategoryTile categoryTile = OwnerSummaryPage.categoryTile();
    ScrollUtil.scrollIntoViewInstantly(categoryTile.getElement());

    NxList categoryList = categoryTile.categoryList(0);
    categoryList.elements().shouldBe(empty);
    categoryList.emptyDescriptor().shouldBe(visible).shouldHave(CategoryTile.noneDefinedText());
  }

  private void testApplicationCategoryTile_Empty() {
    CategoryTile categoryTile = OwnerSummaryPage.categoryTile();
    categoryTile.nxSubHeader().shouldBe(visible).shouldHave(CategoryTile.subHeaderText(organization));
    categoryTile.newButton().shouldBe(visible, enabled).shouldHave(CategoryTile.buttonText(organization));

    categoryTile.categoryLists().shouldHaveSize(1);

    NxList list = categoryTile.categoryList(0);
    SelenideElement subsectionHeader = categoryTile.categoryListSubheader(0);
    list.elements().shouldBe(empty);

    subsectionHeader.shouldBe(visible).shouldHave(text("Local to " + organization.getName()));
    list.emptyDescriptor().shouldBe(visible).shouldHave(CategoryTile.noneDefinedText());
  }

  private void testApplicationCategoryTile_WithApplicableCategories() {
    List<List<Tag>> ownerTags = new ArrayList<>();
    List<Owner> owners = new ArrayList<>();

    for (Owner owner : new OwnerDAO().walkHierarchy(organization)) {
      List<Tag> tags = new ArrayList<>();
      owners.add(owner);

      if (owner.getId() != null) {
        tags.add(tempEntity.newTag(owner.getId(), owner.getName() + " Test Tag 1", Color.dark_purple));
        tags.add(tempEntity.newTag(owner.getId(), owner.getName() + " Test Tag 2", Color.dark_blue));

        ownerTags.add(tags);
      }
    }

    refresh();

    final int hierarchySize = owners.size();
    CategoryTile categoryTile = OwnerSummaryPage.categoryTile();
    assertThat(ownerTags).hasSameSizeAs(owners);

    eyesWatcher.eyesCheck("Organization's Category Tile with applied category");

    for (int i = 0; i < hierarchySize; i++) {
      if (i == 0) {
        assertCategoryTile(i, ownerTags, categoryTile, organization);
      }
      else {
        assertInheritedCategory(i, ownerTags, categoryTile, owners);
      }
    }
  }

  private void assertCategoryTile(
      int i,
      List<List<Tag>> ownerTags,
      CategoryTile categoryTile,
      Organization organization)
  {
    NxList list = categoryTile.categoryList(i);
    list.elements().shouldHaveSize(ownerTags.get(i).size());
    categoryTile.categoryListSubheader(i).shouldBe(visible).shouldHave(text("Local to " + organization.getName()));
    for (int j = 0; j < ownerTags.get(i).size(); j++) {
      NxList.NxListItem actualCategory = list.element(j);
      Tag expectedCategory = ownerTags.get(i).get(j);
      actualCategory.chevron().shouldBe(visible);
      if (expectedCategory.getDescription() == null) {
        actualCategory.description().shouldNot(exist);
      }
      else {
        actualCategory.description().shouldBe(visible).shouldHave(text(expectedCategory.getDescription()));
      }
      String nxColorClass = NxColor.getNxColorFromColor(expectedCategory.getColor()).toNxClass();
      actualCategory.icon().shouldBe(visible).shouldHave(cssClass(nxColorClass));
      actualCategory.name().shouldBe(visible).shouldHave(text(expectedCategory.getName()));
    }
  }

  private void assertInheritedCategory(
      int i,
      List<List<Tag>> ownerTags,
      CategoryTile categoryTile,
      List<Owner> owners)
  {
    InheritedCategoriesList categoriesList = categoryTile.inheritedCategoriesList(owners.get(i).getId());
    categoriesList.should(exist).shouldBe(visible);

    int expectedCategoriesCount = ownerTags.get(i).size();
    categoriesList.elements().shouldHaveSize(expectedCategoriesCount);
    categoryTile.categoryLists().shouldHaveSize(i);
    categoryTile.inheritedCategoriesLists().shouldHaveSize(i);
    for (int j = 0; j < ownerTags.get(i).size(); j++) {
      InheritedCategory actualCategory = categoriesList.element(j);
      Tag expectedCategory = ownerTags.get(i).get(j);
      actualCategory.label().shouldBe(visible).shouldHave(text(expectedCategory.getName()));
      actualCategory.description().shouldBe(visible).shouldHave(text(expectedCategory.getDescription()));
      String nxColorClass = NxColor.getNxColorFromColor(expectedCategory.getColor()).toNxClass();
      actualCategory.icon().shouldBe(visible).shouldHave(cssClass(nxColorClass));
    }
    categoryTile.categoryListSubheader(i).shouldBe(visible)
        .shouldHave(CategoryTile.inheritedText(owners.get(i).getName())).click();
    categoriesList.should(exist).shouldNotBe(visible);
  }

  @Test
  public void testDataRetentionTile() {
    DataRetentionTile tile = OwnerSummaryPage.dataRetentionTile();

    OwnerSummaryPage.summaryTile().dataRetentionButton().shouldBe(visible);

    ScrollUtil.scrollIntoViewInstantly(tile.getElement());

    tile.shouldBe(visible);
    tile.nxSubHeader().shouldBe(visible).shouldHave(tile.subHeaderText(currentOwner.getName()));

    List<String> contextIds = Arrays.asList(
        Stage.ID_DEVELOP,
        Stage.ID_SOURCE,
        Stage.ID_BUILD,
        Stage.ID_STAGE_RELEASE,
        Stage.ID_RELEASE,
        Stage.ID_OPERATE,
        DataRetentionPolicy.CONTEXT_ID_CONTINUOUS_MONITORING);

    tile.rows().shouldHaveSize(3);
    ElementsCollection rowHeaders = tile.rowHeaders();
    rowHeaders.shouldHaveSize(contextIds.size() + 1);
    rowHeaders.last(rowHeaders.size() - 1).shouldHave(exactTexts(contextIds));

    ElementsCollection maxAges = tile.maxAges();
    maxAges.shouldHaveSize(contextIds.size() + 1);
    maxAges.get(0).shouldHave(exactTextCaseSensitive(DataRetentionTile.MAX_AGE_HEADER));
    ElementsCollection maxReports = tile.maxReports();
    maxReports.shouldHaveSize(contextIds.size() + 1);
    maxReports.get(0).shouldHave(exactTextCaseSensitive(DataRetentionTile.MAX_REPORTS_HEADER));

    tile.successMetrics().shouldBe(visible).shouldHave(exactTextCaseSensitive("Max Age: 1 year"));

    DataRetentionPolicyDAO dao = new DataRetentionPolicyDAO();
    DataRetentionPolicy customMaxCount = new DataRetentionPolicy();
    customMaxCount.setOwnerId(organization.getId());
    customMaxCount.setContextId(Stage.ID_DEVELOP);
    customMaxCount.setPurgingEnabled(true);
    customMaxCount.setMaxCount(6);
    dao.insert(customMaxCount);

    DataRetentionPolicy sourceMaxAgeWeeks = new DataRetentionPolicy();
    sourceMaxAgeWeeks.setOwnerId(organization.getId());
    sourceMaxAgeWeeks.setContextId(Stage.ID_SOURCE);
    sourceMaxAgeWeeks.setPurgingEnabled(true);
    sourceMaxAgeWeeks.setMaxAgeInDays(21);
    dao.insert(sourceMaxAgeWeeks);

    DataRetentionPolicy customMaxAgeAndMaxCount = new DataRetentionPolicy();
    customMaxAgeAndMaxCount.setOwnerId(organization.getId());
    customMaxAgeAndMaxCount.setContextId(Stage.ID_BUILD);
    customMaxAgeAndMaxCount.setPurgingEnabled(true);
    customMaxAgeAndMaxCount.setMaxAgeInDays(14);
    customMaxAgeAndMaxCount.setMaxCount(8);
    dao.insert(customMaxAgeAndMaxCount);

    DataRetentionPolicy disabled = new DataRetentionPolicy();
    disabled.setOwnerId(organization.getId());
    disabled.setContextId(Stage.ID_RELEASE);
    disabled.setPurgingEnabled(false);
    dao.insert(disabled);

    DataRetentionPolicy customMaxAge = new DataRetentionPolicy();
    customMaxAge.setOwnerId(organization.getId());
    customMaxAge.setContextId(Stage.ID_OPERATE);
    customMaxAge.setPurgingEnabled(true);
    customMaxAge.setMaxAgeInDays(7);
    dao.insert(customMaxAge);

    DataRetentionPolicy dontPurgeSuccessMetrics = new DataRetentionPolicy();
    dontPurgeSuccessMetrics.setOwnerId(organization.getId());
    dontPurgeSuccessMetrics.setContextId(DataRetentionPolicy.CONTEXT_ID_SUCCESS_METRICS);
    dontPurgeSuccessMetrics.setPurgingEnabled(false);
    dao.insert(dontPurgeSuccessMetrics);

    refresh();
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().dataRetentionButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(tile.getElement());

    tile.maxAge(Stage.ID_DEVELOP).shouldBe(visible).shouldHave(exactTextCaseSensitive(DataRetentionTile.NOT_AVAILABLE));
    tile.maxReport(Stage.ID_DEVELOP).shouldBe(visible).shouldHave(exactTextCaseSensitive("6"));
    tile.maxAge(Stage.ID_SOURCE).shouldBe(visible).shouldHave(exactTextCaseSensitive("3 w"));
    tile.maxReport(Stage.ID_SOURCE).shouldBe(visible)
        .shouldHave(exactTextCaseSensitive(DataRetentionTile.NOT_AVAILABLE));
    tile.maxAge(Stage.ID_BUILD).shouldBe(visible).shouldHave(exactTextCaseSensitive("2 w"));
    tile.maxReport(Stage.ID_BUILD).shouldBe(visible).shouldHave(exactTextCaseSensitive("8"));
    tile.maxAge(Stage.ID_RELEASE).shouldBe(visible).shouldHave(exactTextCaseSensitive(DataRetentionTile.DONT_PURGE));
    tile.maxReport(Stage.ID_RELEASE).shouldBe(visible).shouldHave(exactTextCaseSensitive(DataRetentionTile.DONT_PURGE));
    tile.maxAge(Stage.ID_OPERATE).shouldBe(visible).shouldHave(exactTextCaseSensitive("1 w"));
    tile.maxReport(Stage.ID_OPERATE).shouldBe(visible)
        .shouldHave(exactTextCaseSensitive(DataRetentionTile.NOT_AVAILABLE));
    tile.successMetrics().shouldBe(visible).shouldHave(exactTextCaseSensitive("Don't Purge"));
  }

  @Test
  public void testDataRetentionTile_Routing() {
    DataRetentionTile tile = new DataRetentionTile();
    DataRetentionEditorPage page = new DataRetentionEditorPage();

    OwnerSummaryPage.summaryTile().dataRetentionButton().shouldBe(visible);

    ScrollUtil.scrollIntoViewInstantly(tile.getElement());
    tile.shouldBe(visible);
    page.shouldNot(exist);

    tile.editButton().click();

    tile.shouldNot(exist);
    page.shouldBe(visible);

    back();
    OwnerSummaryPage.summaryTile().dataRetentionButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(tile.getElement());
    tile.shouldBe(visible);
    page.shouldNot(exist);
  }

  @Test
  public void testDataRetentionTile_LicensingAware() {
    testProductLicense.setStageTypes(StageTypes.RELEASE);
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_MONITORING);
    refresh();

    SidebarNavigation.closeNavigationSidebar();
    DataRetentionTile tile = OwnerSummaryPage.dataRetentionTile();

    OwnerSummaryPage.summaryTile().dataRetentionButton().shouldBe(visible);

    ScrollUtil.scrollIntoViewInstantly(tile.getElement());
    tile.shouldBe(visible);
    tile.nxSubHeader().shouldBe(visible).shouldHave(tile.subHeaderText(currentOwner.getName()));
    tile.rows().shouldHaveSize(3);
    ElementsCollection rowHeaders = tile.rowHeaders();
    rowHeaders.shouldHaveSize(2);
    rowHeaders.last(rowHeaders.size() - 1).shouldHave(exactTexts(Stage.ID_RELEASE));
  }

  @Test
  public void testSourceControlTile() {
    SourceControlTile tile = OwnerSummaryPage.sourceControlTile();

    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(tile.getElement());

    tile.shouldBe(visible);
    tile.nxSubHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s organization", organization.getName())));
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
        .format("Configures the integration with an external SCM for the %s organization", organization.getName())));
    tile.rows().shouldHaveSize(1);

    tile.itemText().shouldBe(visible).shouldHave(Condition.text("GitHub"));
    tile.itemSubText().shouldBe(visible).shouldHave(Condition.text("Inherit access token"));

    rootSourceControl.setToken("TESK_TOKEN");
    sourceControlDAO.update(rootSourceControl);
    refresh();

    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible);

    ScrollUtil.scrollIntoViewInstantly(tile.getElement());

    tile.shouldBe(visible);
    tile.nxSubHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s organization", organization.getName())));
    tile.rows().shouldHaveSize(1);

    tile.itemText().shouldBe(visible).shouldHave(Condition.text("GitHub"));
    tile.itemSubText().shouldBe(visible).shouldHave(Condition.text(String
        .format("Inherit access token from %s", rootOrganization.getName())));

    tempEntity.newSourceControl(organization.getId(), null, "TEST_TOKEN", null);
    refresh();

    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible);

    ScrollUtil.scrollIntoViewInstantly(tile.getElement());

    tile.shouldBe(visible);
    tile.nxSubHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s organization", organization.getName())));
    tile.rows().shouldHaveSize(1);

    tile.itemText().shouldBe(visible).shouldHave(Condition.text("GitHub"));
    tile.itemSubText().shouldBe(visible).shouldHave(Condition.text(String
        .format("Provides default access token for %s", organization.getName())));
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
        .format("Configures the integration with an external SCM for the %s organization", organization.getName())));
    tile.notSupported().shouldBe(visible);
    tile.content().shouldNotBe(visible);
    tile.notSupported().shouldHave(text("Source Control is not supported by your license"));

    tile.itemText().shouldNotBe(visible);
    tile.itemSubText().shouldNotBe(visible);

    eyesWatcher.eyesCheck("Source Control No License");
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
        .format("Configures the integration with an external SCM for the %s organization", organization.getName())));
    tile.notSupported().shouldNotBe(visible);
    tile.content().shouldBe(visible);

    tile.itemText().shouldBe(visible);
  }

  @Test
  public void testWaivedComponentUpgradesTile_Routing() {
    // Config org stages
    rootOrganization.setWaivedComponentUpgradeStageTypeId(OperateStageType.ID);
    organizationDAO.update(rootOrganization);
    String operateStageConfiguredText = WaivedComponentUpgradeConfigurationPage.rootOrgMonitoredStageText("Operate");
    String buildStageConfiguredText = WaivedComponentUpgradeConfigurationPage.rootOrgMonitoredStageText("Build");

    // Configure elements
    WaivedComponentUpgradeConfigurationPage configPage = new WaivedComponentUpgradeConfigurationPage();
    SelenideElement tileListLink = OwnerSummaryPage.waivedComponentUpgradesConfiguredStage();
    SelenideElement configForm = configPage.configurationForm();

    // Nav to root org summary view
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    OwnerSummaryPage.summaryTile().waivedComponentUpgradesButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(tileListLink);
    tileListLink.shouldBe(visible);
    tileListLink.shouldHave(text(operateStageConfiguredText));
    configForm.shouldNot(exist);

    // Nav to config page and set monitored stage to Build
    tileListLink.click();

    tileListLink.shouldNot(exist);
    configForm.shouldBe(visible);

    NxRadio stageBuildRadio = configPage.stagesByLabel().get("Build");
    configPage.stagesByLabel().get("Operate").shouldBe(selected);
    stageBuildRadio.click();
    configPage.updateButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    // Nav to summary view and assert stage change is reflected in tile
    back();

    OwnerSummaryPage.summaryTile().waivedComponentUpgradesButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(tileListLink);
    tileListLink.shouldBe(visible);
    tileListLink.shouldHave(text(buildStageConfiguredText));
    configForm.shouldNot(exist);
  }
}
