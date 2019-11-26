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
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.CategoryTile;
import com.sonatype.clm.testing.functional.elements.DataRetentionTile;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.ImportPolicyModal;
import com.sonatype.clm.testing.functional.elements.LabelTile;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupTile;
import com.sonatype.clm.testing.functional.elements.PolicyTile;
import com.sonatype.clm.testing.functional.elements.PolicyTileList;
import com.sonatype.clm.testing.functional.elements.PolicyTileList.PolicyTileListElement;
import com.sonatype.clm.testing.functional.elements.SourceControlTile;
import com.sonatype.clm.testing.functional.elements.ThreatGroupTileSimpleList;
import com.sonatype.clm.testing.functional.elements.TileSimpleList;
import com.sonatype.clm.testing.functional.elements.TileSimpleList.TileSimpleListElement;
import com.sonatype.clm.testing.functional.pages.DataRetentionEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.DataRetentionPolicyDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.back;
import static com.sonatype.clm.testing.functional.elements.TileSimpleList.CLICKABLE;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationSummaryViewTest
    extends AbstractSummaryViewTest
{
  private Organization organization;

  private Organization rootOrganization;

  private OrganizationDAO organizationDAO = new OrganizationDAO();

  private SourceControlDAO sourceControlDAO = new SourceControlDAO();

  @Before
  public void init() {
    organization = tempEntity.newOrganization(YE_OLE_ORGANIZATION);
    rootOrganization = organizationDAO.getByIdNotNull(ROOT_ORGANIZATION_ID);
    super.init(organization);
  }

  @Override
  @Test
  public void testReportLinks() {
    ActionDropDown.actionButton().click();
    ActionDropDown.reportLinks().shouldBe(empty);
  }

  @Test
  public void testLTGTile_NoLocal() {
    int hierarchySize = getHierarchySize(organization.getId());

    LicenseThreatGroupTile ltgTile = OwnerSummaryPage.licenseThreatGroupTile();
    ltgTile.subHeader().shouldBe(visible).shouldHave(LabelTile.subHeaderText(organization.getName()));
    ltgTile.newButton().shouldBe(visible, enabled);

    ltgTile.ltgLists().shouldHaveSize(hierarchySize);

    for (int i = 0; i < hierarchySize; i++) {
      ThreatGroupTileSimpleList list = ltgTile.ltgList(i);

      if (i == 0) {
        list.ownerName().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().shouldBe(visible).shouldHave(text("No local threat groups defined"));
        list.elements().shouldBe(empty);
      }
      else {
        list.ownerName().scrollTo().shouldBe(visible);
        list.emptyDescriptor().shouldBe(hidden);
        list.elements().shouldHaveSize(LicenseThreatGroupDataHelper.TEST_LICENSE_THREAT_GROUP_COUNT);
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
  public void testImportPolicy() {
    String filePath = new File(getClass().getResource("/policyExport/samplePolicy.json").getFile()).getAbsolutePath();

    ActionDropDown.actionButton().click();
    ActionDropDown.importPoliciesButton().shouldBe(visible).click();

    ImportPolicyModal.root().shouldBe(visible);
    ImportPolicyModal.importButton().shouldBe(visible, disabled);

    ImportPolicyModal.fileInput().shouldBe(visible).sendKeys(filePath);

    // Give a maximum of 2 seconds for the file to be loaded
    ImportPolicyModal.importButton().waitUntil(enabled, 2000).click();
    // verify mask and wait for it to go away
    FormMask.seeAndWaitForDismissal();

    // scroll to the labels tile
    OwnerSummaryPage.summaryTile().labelsButton().shouldBe(visible).click();
    LabelTile labelTile = OwnerSummaryPage.labelTile();
    labelTile.labelList(0);
    TileSimpleList list = labelTile.labelList(0);
    list.subsectionHeader().shouldBe(visible).shouldHave(text("Local"));
    list.elements().shouldHaveSize(1);
    TileSimpleListElement actualLabel = list.element(0);
    actualLabel.name().shouldBe(visible).shouldHave(text("Test Label"));

    // scroll to the ltgs
    OwnerSummaryPage.summaryTile().ltgsButton().shouldBe(visible).click();
    LicenseThreatGroupTile ltgTile = OwnerSummaryPage.licenseThreatGroupTile();
    ThreatGroupTileSimpleList threatGroupTileSimpleList = ltgTile.ltgList(0);
    threatGroupTileSimpleList.emptyDescriptor().shouldNot(exist);
    threatGroupTileSimpleList.elements().shouldHaveSize(1);
    threatGroupTileSimpleList.element(0).name().shouldBe(visible).shouldHave(text("Test LTG"));

    // scroll to the policy tile
    OwnerSummaryPage.summaryTile().policyButton().shouldBe(visible).click();
    PolicyTile policyTile = OwnerSummaryPage.policyTile();
    PolicyTileList policyList = policyTile.policyList(0);
    policyList.emptyDescriptor().shouldBe(hidden);
    policyList.rows().shouldHaveSize(2); // 1 row plus header
    policyList.ownerName().shouldBe(visible).shouldHave(text("Local"));
    PolicyTileListElement policyElement = policyList.row(1);
    policyElement.name().shouldBe(visible).shouldHave(text("Test"));
    policyElement.proxy().shouldBe(visible).shouldHave(text("warn"));

    // scroll to the application categories tile
    OwnerSummaryPage.summaryTile().appCategoriesButton().shouldBe(visible).click();
    CategoryTile categoryTile = OwnerSummaryPage.categoryTile();
    TileSimpleList categoryList = categoryTile.categoryList(0);
    categoryList.elements().shouldBe(empty);
    categoryList.emptyDescriptor().shouldBe(visible).shouldHave(CategoryTile.noneDefinedText());
  }

  private void testApplicationCategoryTile_Empty() {
    CategoryTile categoryTile = OwnerSummaryPage.categoryTile();
    categoryTile.subHeader().shouldBe(visible).shouldHave(CategoryTile.subHeaderText(organization));
    categoryTile.newButton().shouldBe(visible, enabled).shouldHave(CategoryTile.buttonText(organization));

    categoryTile.categoryLists().shouldHaveSize(1);

    TileSimpleList list = categoryTile.categoryList(0);
    list.elements().shouldBe(empty);

    list.subsectionHeader().shouldBe(visible).shouldHave(text("Local"));
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
    categoryTile.categoryLists().shouldHaveSize(hierarchySize);

    for (int i = 0; i < hierarchySize; i++) {
      TileSimpleList list = categoryTile.categoryList(i);

      if (i == 0) {
        list.subsectionHeader().shouldBe(visible).shouldHave(text("Local"));
        list.root.shouldHave(CLICKABLE);
      }
      else {
        list.subsectionHeader().shouldBe(visible).shouldHave(CategoryTile.inheritedText(owners.get(i).getName()));
        list.root.shouldNotHave(CLICKABLE);
      }

      list.elements().shouldHaveSize(ownerTags.get(i).size());

      for (int j = 0; j < ownerTags.get(i).size(); j++) {
        TileSimpleListElement actualCategory = list.element(j);
        Tag expectedCategory = ownerTags.get(i).get(j);

        if (i == 0) {
          actualCategory.chevron().shouldBe(visible);
        }
        else {
          actualCategory.chevron().shouldNot(exist);
        }

        if (expectedCategory.getDescription() == null) {
          actualCategory.description().shouldNot(exist);
        }
        else {
          actualCategory.description().shouldBe(visible).shouldHave(text(expectedCategory.getDescription()));
        }

        actualCategory.icon().shouldBe(visible).shouldHave(cssClass(expectedCategory.getColor().toValue()));
        actualCategory.name().shouldBe(visible).shouldHave(text(expectedCategory.getName()));
      }
    }
  }

  @Test
  public void testDataRetentionTile() {
    DataRetentionTile tile = OwnerSummaryPage.dataRetentionTile();

    OwnerSummaryPage.summaryTile().dataRetentionButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    tile.subHeader().shouldBe(visible).shouldHave(tile.subHeaderText(currentOwner.getName()));

    List<String> contextIds = Arrays.asList(
        Stage.ID_DEVELOP,
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

    tile.successMetrics().shouldBe(visible).shouldHave(exactTextCaseSensitive("Max Age 1 year"));

    DataRetentionPolicyDAO dao = new DataRetentionPolicyDAO();
    DataRetentionPolicy customMaxCount = new DataRetentionPolicy();
    customMaxCount.setOwnerId(organization.getId());
    customMaxCount.setContextId(Stage.ID_DEVELOP);
    customMaxCount.setPurgingEnabled(true);
    customMaxCount.setMaxCount(6);
    dao.insert(customMaxCount);
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
    OwnerSummaryPage.summaryTile().dataRetentionButton().shouldBe(visible).click();

    tile.maxAge(Stage.ID_DEVELOP).shouldBe(visible).shouldHave(exactTextCaseSensitive(DataRetentionTile.NOT_AVAILABLE));
    tile.maxReport(Stage.ID_DEVELOP).shouldBe(visible).shouldHave(exactTextCaseSensitive("6"));
    tile.maxAge(Stage.ID_BUILD).shouldBe(visible).shouldHave(exactTextCaseSensitive("2 weeks"));
    tile.maxReport(Stage.ID_BUILD).shouldBe(visible).shouldHave(exactTextCaseSensitive("8"));
    tile.maxAge(Stage.ID_RELEASE).shouldBe(visible).shouldHave(exactTextCaseSensitive(DataRetentionTile.DONT_PURGE));
    tile.maxReport(Stage.ID_RELEASE).shouldBe(visible).shouldHave(exactTextCaseSensitive(DataRetentionTile.DONT_PURGE));
    tile.maxAge(Stage.ID_OPERATE).shouldBe(visible).shouldHave(exactTextCaseSensitive("1 week"));
    tile.maxReport(Stage.ID_OPERATE).shouldBe(visible)
        .shouldHave(exactTextCaseSensitive(DataRetentionTile.NOT_AVAILABLE));
    tile.successMetrics().shouldBe(visible).shouldHave(exactTextCaseSensitive("Don\'t Purge"));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testDataRetentionTile_Routing() {
    DataRetentionTile tile = new DataRetentionTile();
    DataRetentionEditorPage page = new DataRetentionEditorPage();

    OwnerSummaryPage.summaryTile().dataRetentionButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    page.shouldNot(exist);

    tile.editButton().click();

    tile.shouldNot(exist);
    page.shouldBe(visible);

    back();
    OwnerSummaryPage.summaryTile().dataRetentionButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    page.shouldNot(exist);
  }

  @Test
  public void testDataRetentionTile_LicensingAware() {
    testProductLicense.setStageTypes(StageTypes.RELEASE);
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_MONITORING);
    refresh();

    DataRetentionTile tile = OwnerSummaryPage.dataRetentionTile();

    OwnerSummaryPage.summaryTile().dataRetentionButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    tile.subHeader().shouldBe(visible).shouldHave(tile.subHeaderText(currentOwner.getName()));
    tile.rows().shouldHaveSize(3);
    ElementsCollection rowHeaders = tile.rowHeaders();
    rowHeaders.shouldHaveSize(2);
    rowHeaders.last(rowHeaders.size() - 1).shouldHave(exactTexts(Stage.ID_RELEASE));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testSourceControlTile() {
    SourceControlTile tile = OwnerSummaryPage.sourceControlTile();

    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    tile.subHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s organization", organization.getName())));
    tile.rows().shouldHaveSize(1);

    tile.itemText().shouldNotBe(visible);
    tile.itemSubText().shouldBe(visible)
        .shouldHave(Condition.text("Source Control not configured"));

    eyesWatcher.eyesCheck("Source control not configured");

    SourceControl rootSourceControl =
        tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    refresh();

    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    tile.subHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s organization", organization.getName())));
    tile.rows().shouldHaveSize(1);

    tile.itemText().shouldBe(visible).shouldHave(Condition.text("GitHub"));
    tile.itemSubText().shouldBe(visible).shouldHave(Condition.text("Inherit access token"));

    eyesWatcher.eyesCheck("Valid source control configured, token on root");

    rootSourceControl.setToken("TESK_TOKEN");
    sourceControlDAO.update(rootSourceControl);
    refresh();

    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    tile.subHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s organization", organization.getName())));
    tile.rows().shouldHaveSize(1);

    tile.itemText().shouldBe(visible).shouldHave(Condition.text("GitHub"));
    tile.itemSubText().shouldBe(visible).shouldHave(Condition.text(String
        .format("Inherit access token from %s", rootOrganization.getName())));

    eyesWatcher.eyesCheck("Valid source control configured, token on root");

    tempEntity.newSourceControl(organization.getId(), null, "TEST_TOKEN", null);
    refresh();

    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    tile.subHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s organization", organization.getName())));
    tile.rows().shouldHaveSize(1);

    tile.itemText().shouldBe(visible).shouldHave(Condition.text("GitHub"));
    tile.itemSubText().shouldBe(visible).shouldHave(Condition.text(String
        .format("Provides default access token for %s", organization.getName())));

    eyesWatcher.eyesCheck("Valid source control configured, token on organization");
  }

  @Test
  public void testSourceControlTile_LicensingAwareNoLicense() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    refresh();
    SourceControlTile tile = OwnerSummaryPage.sourceControlTile();

    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    tile.subHeader().shouldBe(visible).shouldHave(Condition.text(String
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
    SourceControlTile tile = OwnerSummaryPage.sourceControlTile();

    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    tile.subHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s organization", organization.getName())));
    tile.notSupported().shouldNotBe(visible);
    tile.content().shouldBe(visible);

    tile.itemSubText().shouldBe(visible);

    eyesWatcher.eyesCheck("Source Control Notifications Only License");
  }
}
