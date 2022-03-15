/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.AccessTile;
import com.sonatype.clm.testing.functional.elements.AccessTileList;
import com.sonatype.clm.testing.functional.elements.AccessTileList.AccessTileListElement;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.GreedyTable.HeaderColumn;
import com.sonatype.clm.testing.functional.elements.InnerSourceRepositoryTile;
import com.sonatype.clm.testing.functional.elements.LabelTile;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupTile;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.OwnerEditorDialog;
import com.sonatype.clm.testing.functional.elements.PolicyTile;
import com.sonatype.clm.testing.functional.elements.PolicyTileList;
import com.sonatype.clm.testing.functional.elements.PolicyTileList.PolicyTileListElement;
import com.sonatype.clm.testing.functional.elements.ThreatGroupTileSimpleList;
import com.sonatype.clm.testing.functional.elements.ThreatGroupTileSimpleList.ThreatGroupTileSimpleListElement;
import com.sonatype.clm.testing.functional.elements.TileSimpleList;
import com.sonatype.clm.testing.functional.elements.TileSimpleList.TileSimpleListElement;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.ElementsCollection;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.IQ_DISABLED;
import static com.sonatype.clm.testing.functional.elements.GreedyTable.HeaderColumn.COLUMN_SELECTED;
import static com.sonatype.clm.testing.functional.elements.GreedyTable.HeaderColumn.DOWN_SELECTED;
import static com.sonatype.clm.testing.functional.elements.GreedyTable.HeaderColumn.UP_SELECTED;
import static com.sonatype.clm.testing.functional.elements.PolicyTileList.threatLevel;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractSummaryViewTest
    extends AbstractFunctionalTest
{
  protected Owner currentOwner;

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  protected void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
  }

  @After
  public void afterEach() {
    // This ensures no open modals that might interfere with further logic are open after each test
    refresh();
  }

  @Test
  public void testSummaryTile() {
    OwnerSummaryPage.summaryTile().name().shouldBe(visible).shouldHave(text(currentOwner.getName()));
    OwnerSummaryPage.summaryTile().headerIcon().shouldBe(visible);

    if (OwnerType.APPLICATION.equals(currentOwner.getType())) {
      OwnerSummaryPage.summaryTile().publicId().shouldBe(visible).shouldHave(text(currentOwner.getPublicId()));
    }
    else {
      OwnerSummaryPage.summaryTile().publicId().shouldBe(hidden);
    }
  }

  @Test
  public void testSummaryTile_missing() {
    refreshOrOpen(OwnerSummaryPage.url(currentOwner.getType(), "fakeid"));

    ErrorBox error = OwnerSummaryPage.summaryTile().error();
    error.shouldBe(visible);
    error.shouldHave(text("Could not find an " + currentOwner.getType().toString()));
    error.retryButton().shouldBe(visible, enabled);
  }

  @Test
  public void testActionDropDown() {
    ActionDropDown.menu().shouldBe(hidden);
    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.menu().shouldBe(visible);
    ActionDropDown.actionButton().click();
    ActionDropDown.menu().shouldBe(hidden);
  }

  @Test
  public void testEditAppOrgNameLink() {
    String shortTypeName = currentOwner.getType().toString().equalsIgnoreCase("application") ? "App" : "Org";
    ActionDropDown.actionButton().click();
    OwnerEditorDialog.root().shouldBe(hidden);
    ActionDropDown.editOwner().shouldHave(text(shortTypeName)).click();
    OwnerEditorDialog.root().shouldBe(visible);
    OwnerEditorDialog.title().shouldHave(text(currentOwner.getType().toString()));
  }

  @Test
  public void testTile_default() {
    testLabelTile_no_labels();
    testAccessTile_no_local_access();
    testPolicyTile_no_policies();
  }

  @Test
  public void testInnerSourceRepositoryTile_NotConfigured() {
    refresh();
    MainHeader.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().innerSourceRepositoryButton().shouldBe(visible).click();
    InnerSourceRepositoryTile innerSourceRepositoryTile = OwnerSummaryPage.innerSourceRepositoryTile();
    innerSourceRepositoryTile.should(exist);
    innerSourceRepositoryTile.listTitle().shouldNot(exist);
    ElementsCollection rows = innerSourceRepositoryTile.rows();
    rows.shouldHaveSize(1);
    rows.get(0).shouldBe(text("No repositories are configured"));
    innerSourceRepositoryTile.editButton().shouldHave(text("Edit"));
  }

  @Test
  public void testInnerSourceRepositoryTile_Configured() {
    try {
      setCurrentOwnerRepositoryConnectionStatus(currentOwner, true);
      RepositoryConnection repositoryConnection1 = tempEntity.newRepositoryConnection(currentOwner.getId(),
          "http://some.base.url.1", RepositoryFormat.MAVEN, null, null);
      RepositoryConnection repositoryConnection2 = tempEntity.newRepositoryConnection(currentOwner.getId(),
          "http://some.base.url.2", RepositoryFormat.NPM, null, null);

      refresh();
      MainHeader.closeNavigationSidebar();
      OwnerSummaryPage.summaryTile().dropdownButton().click();
      OwnerSummaryPage.summaryTile().innerSourceRepositoryButton().shouldBe(visible).click();
      InnerSourceRepositoryTile innerSourceRepositoryTile = OwnerSummaryPage.innerSourceRepositoryTile();
      innerSourceRepositoryTile.should(exist);
      innerSourceRepositoryTile.listTitle().shouldHave(text("Local"));
      ElementsCollection rows = innerSourceRepositoryTile.rows();
      rows.shouldHaveSize(2);
      rows.get(0).shouldBe(text(repositoryConnection1.getBaseUrl() + "\n" + repositoryConnection1.getFormat()));
      rows.get(1).shouldBe(text(repositoryConnection2.getBaseUrl() + "\n" + repositoryConnection2.getFormat()));
      innerSourceRepositoryTile.editButton().shouldHave(text("Edit"));
    }
    finally {
      setCurrentOwnerRepositoryConnectionStatus(currentOwner, null);
    }
  }

  private void setCurrentOwnerRepositoryConnectionStatus(Owner currentOwner, Boolean repoConnectionsEnabled) {
    if (OwnerType.APPLICATION.equals(currentOwner.getType())) {
      Application app = (Application) currentOwner;
      app.setRepositoryConnectionEnabled(repoConnectionsEnabled);
      new ApplicationDAO().update(app);
    }
    else if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      Organization org = (Organization) currentOwner;
      org.setRepositoryConnectionEnabled(repoConnectionsEnabled);
      new OrganizationDAO().update(org);
    }
  }

  @Test
  public void testInnerSourceRepositoryTile_Configured_Inherited() {
    OrganizationDAO organizationDAO = new OrganizationDAO();
    Organization parentOwner = organizationDAO.getById(currentOwner.getParentOwnerId());
    try {
      RepositoryConnection repositoryConnection1 = tempEntity.newRepositoryConnection(currentOwner.getParentOwnerId(),
          "http://some.base.url.1", RepositoryFormat.MAVEN, null, null);
      RepositoryConnection repositoryConnection2 = tempEntity.newRepositoryConnection(currentOwner.getParentOwnerId(),
          "http://some.base.url.2", RepositoryFormat.NPM, null, null);
      parentOwner.setAllowRepositoryConnectionOverride(false);
      parentOwner.setRepositoryConnectionEnabled(true);
      organizationDAO.update(parentOwner);

      refresh();
      MainHeader.closeNavigationSidebar();
      OwnerSummaryPage.summaryTile().dropdownButton().click();
      OwnerSummaryPage.summaryTile().innerSourceRepositoryButton().shouldBe(visible).click();
      InnerSourceRepositoryTile innerSourceRepositoryTile = OwnerSummaryPage.innerSourceRepositoryTile();
      innerSourceRepositoryTile.should(exist);
      innerSourceRepositoryTile.listTitle().shouldHave(text("Inherited from " + parentOwner.getName()));
      ElementsCollection rows = innerSourceRepositoryTile.rows();
      rows.shouldHaveSize(2);
      rows.get(0).shouldBe(text(repositoryConnection1.getBaseUrl() + "\n" + repositoryConnection1.getFormat()));
      rows.get(1).shouldBe(text(repositoryConnection2.getBaseUrl() + "\n" + repositoryConnection2.getFormat()));
      innerSourceRepositoryTile.editButton().shouldHave(text("Edit"));
    }
    finally {
      parentOwner.setAllowRepositoryConnectionOverride(true);
      parentOwner.setRepositoryConnectionEnabled(null);
      organizationDAO.update(parentOwner);
    }
  }

  @Test
  public void testInnerSourceRepositoryTile_FeatureDisabled() {
    testCLMServer.getCLMServer().getConfiguration()
        .setFeatures(ImmutableMap.of(Feature.INNER_SOURCE_REPOSITORY_INTEGRATION.getFlag(), false));
    refresh();
    MainHeader.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().innerSourceRepositoryButton().shouldNot(exist);
    OwnerSummaryPage.innerSourceRepositoryTile().shouldNot(exist);
  }

  public void testLabelTile_no_labels() {

    LabelTile labelTile = OwnerSummaryPage.labelTile();
    labelTile.subHeader().shouldBe(visible).shouldHave(LabelTile.subHeaderText(currentOwner.getName()));
    labelTile.newButton().shouldBe(visible, enabled);

    labelTile.labelLists().shouldHaveSize(1);

    // scroll to the labels tile
    MainHeader.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().labelsButtonInDropdown().shouldBe(visible).click();

    TileSimpleList list = labelTile.labelList(0);
    list.subsectionHeader().shouldBe(visible).shouldHave(text("Local"));
    list.emptyDescriptor().shouldBe(visible);
  }

  public void testAccessTile_no_local_access() {

    int hierarchySize = getHierarchySize(currentOwner);
    AccessTile accessTile = OwnerSummaryPage.accessTile();
    accessTile.subHeader().shouldBe(visible).shouldHave(AccessTile.subHeaderText(currentOwner.getName()));
    accessTile.newButton().shouldBe(visible, enabled);
    accessTile.accessLists().shouldHaveSize(hierarchySize);

    // scroll to the access tile
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().accessButton().shouldBe(visible).click();

    for (int i = 0; i < hierarchySize; i++) {
      AccessTileList list = accessTile.accessList(i);

      if (i == 0) {
        list.ownerName().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().should(exist);

      }
      else {
        list.ownerName().shouldBe(hidden);
        list.emptyDescriptor().shouldBe(hidden);
      }
    }
  }

  private void testPolicyTile_no_policies() {
    int hierarchySize = getHierarchySize(currentOwner);

    PolicyTile policyTile = OwnerSummaryPage.policyTile();
    policyTile.subHeader().shouldBe(visible).shouldHave(PolicyTile.subHeaderText(currentOwner.getName()));
    policyTile.newButton().shouldBe(visible, enabled);

    policyTile.policyLists().shouldHaveSize(hierarchySize);

    for (int i = 0; i < hierarchySize; i++) {
      PolicyTileList list = policyTile.policyList(i);

      if (i == 0) {
        list.ownerName().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().shouldBe(visible);
      }
      else {
        list.ownerName().shouldBe(hidden);
      }

      list.rows().shouldBe(empty);
    }
  }

  @Test
  public void testTiles_Local() {

    List<Label> localLabels = new ArrayList<>();
    localLabels.add(tempEntity.newLabel(currentOwner.getId(), "Temp Local Label 1", Color.dark_purple));
    localLabels.add(tempEntity.newLabel(currentOwner.getId(), "Temp Local Label 2", "With Subtitle", Color.dark_blue));

    List<LicenseThreatGroup> locaLTGs = new ArrayList<>();
    locaLTGs.add(tempEntity.newLicenseThreatGroup(currentOwner.getId(), "Temp Local License 1", 9));
    locaLTGs.add(tempEntity.newLicenseThreatGroup(currentOwner.getId(), "Temp Local License 2", 1));

    User testUser = tempEntity.newUser("testUser", "Test", "User", "testuser@sonatype.com");
    RoleDAO roleDAO = new RoleDAO();
    List<Role> roleList = new ArrayList<>(roleDAO.getApplicationRoles());
    Role writeRole = tempEntity.newRole("Write Only", false, Permission.WRITE);
    tempEntity.newMembershipMapping(currentOwner.getId(), writeRole.getId(), testUser.getUsername());
    Role readRole = tempEntity.newRole("Read Only", false, Permission.READ);
    tempEntity.newMembershipMapping(currentOwner.getId(), readRole.getId(), "Group", MemberType.GROUP);
    roleList.add(readRole);
    roleList.add(writeRole);

    List<Policy> localPolicies = new ArrayList<>();
    localPolicies.add(tempEntity.newPolicy(currentOwner.getId(), "Policy 1", 10, Action.ID_FAIL, Stage.ID_BUILD, null));
    localPolicies.add(tempEntity.newPolicy(currentOwner.getId(), "Policy 2", 5, Action.ID_WARN, Stage.ID_BUILD, null));
    localPolicies.add(tempEntity.newPolicy(currentOwner.getId(), "Policy 3", 4, null, null, new Notifications(
        new UserNotification("test@test.com", Stage.ID_BUILD))));

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    MainHeader.closeNavigationSidebar();
    testLabelTile_Local(localLabels);
    eyesWatcher.eyesCheck();
    testLTGTile_Local(locaLTGs);
    testAccessTile_Local(testUser);
    testPolicyTile_Local(localPolicies);
  }

  private void testLabelTile_Local(List<Label> localLabels) {

    LabelTile labelTile = OwnerSummaryPage.labelTile();
    labelTile.labelLists().shouldHaveSize(1);

    // scroll to the labels tile
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().labelsButtonInDropdown().shouldBe(visible).click();

    TileSimpleList list = labelTile.labelList(0);
    list.subsectionHeader().shouldBe(visible).shouldHave(text("Local"));
    list.emptyDescriptor().shouldNot(exist);
    list.elements().shouldHaveSize(localLabels.size());

    for (int i = 0; i < localLabels.size(); i++) {
      TileSimpleListElement actualLabel = list.element(i);
      Label expectedLabel = localLabels.get(i);

      if (expectedLabel.getDescription() == null) {
        actualLabel.description().shouldNot(exist);
      }
      else {
        actualLabel.description().shouldBe(visible).shouldHave(text(expectedLabel.getDescription()));
      }

      actualLabel.icon().shouldBe(visible).shouldHave(cssClass(expectedLabel.getColor().toValue()));
      actualLabel.name().shouldBe(visible).shouldHave(text(expectedLabel.getLabel()));
      actualLabel.chevron().shouldBe(visible);
    }
  }

  private void testLTGTile_Local(List<LicenseThreatGroup> locaLTGs) {
    int hierarchySize = getHierarchySize(currentOwner);

    LicenseThreatGroupTile ltgTile = OwnerSummaryPage.licenseThreatGroupTile();
    ltgTile.ltgLists().shouldHaveSize(hierarchySize);

    if (OwnerType.APPLICATION.equals(currentOwner.getType())) {
      ltgTile.newButton().shouldBe(hidden);
    }
    else {
      ltgTile.newButton().shouldBe(visible);
    }

    // scroll to the ltgs
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().ltgsButton().shouldBe(visible).click();

    for (int i = 0; i < hierarchySize; i++) {
      ThreatGroupTileSimpleList list = ltgTile.ltgList(i);

      if (i == 0) {
        list.ownerName().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().shouldNot(exist);
        list.elements().shouldHaveSize(locaLTGs.size());

        for (int j = 0; j < locaLTGs.size(); j++) {
          ThreatGroupTileSimpleListElement actualLTG = list.element(j);
          LicenseThreatGroup expectedLTG = locaLTGs.get(j);

          actualLTG.name().shouldBe(visible).shouldHave(text(expectedLTG.getName()));

          actualLTG.threatLevel().shouldBe(visible)
              .shouldHave(ThreatGroupTileSimpleList.threatLevel(expectedLTG.getThreatLevel()));
          actualLTG.chevron().shouldBe(visible);
        }
      }
      else if (i != hierarchySize - 1) {
        list.ownerName().shouldNot(exist);
        list.elements().shouldBe(empty);
      }
      else {
        list.ownerName().shouldBe(visible);
        list.emptyDescriptor().shouldBe(hidden);
        list.elements().shouldHaveSize(LicenseThreatGroupDataHelper.TEST_LICENSE_THREAT_GROUP_COUNT);
      }
    }
  }

  private void testAccessTile_Local(User testUser) {

    int hierarchySize = getHierarchySize(currentOwner);
    AccessTile accessTile = OwnerSummaryPage.accessTile();
    accessTile.accessLists().shouldHaveSize(hierarchySize);

    // scroll to the access tile
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().accessButton().shouldBe(visible).click();

    for (int i = 0; i < hierarchySize; i++) {
      AccessTileList list = accessTile.accessList(i);
      list.emptyDescriptor().shouldBe(hidden);

      if (i == 0) {
        list.elements().shouldHaveSize(2);
        list.ownerName().shouldBe(visible).shouldHave(text("Local"));

        AccessTileListElement readOnly = list.element(0);
        readOnly.chevron().shouldBe(visible);
        readOnly.role().shouldBe(visible).shouldHave(text("Read Only"));
        readOnly.groupIcon().shouldBe(visible);
        readOnly.members().shouldBe(visible).shouldHave(text("Group"));

        AccessTileListElement writeOnly = list.element(1);
        writeOnly.chevron().shouldBe(visible);
        writeOnly.role().shouldBe(visible).shouldHave(text("Write Only"));
        writeOnly.userIcon().shouldBe(visible);
        writeOnly.members().shouldBe(visible).shouldHave(text(testUser.calculateDisplayName()));

      }
      else {
        list.ownerName().shouldBe(hidden);
        list.elements().shouldHaveSize(0);
      }
    }
  }

  private void testPolicyTile_Local(List<Policy> localPolicies) {

    int hierarchySize = getHierarchySize(currentOwner);
    PolicyTile policyTile = OwnerSummaryPage.policyTile();
    policyTile.policyLists().shouldHaveSize(hierarchySize);

    // scroll to the policy tile
    OwnerSummaryPage.summaryTile().policyButton().shouldBe(visible).click();

    for (int i = 0; i < hierarchySize; i++) {
      PolicyTileList list = policyTile.policyList(i);
      list.emptyDescriptor().shouldBe(hidden);

      if (i == 0) {
        list.rows().shouldHaveSize(4); // 3 rows plus header
        list.ownerName().shouldBe(visible).shouldHave(text("Local"));

        assertPolicyHeader(list);

        PolicyTileListElement policyElement1 = list.row(1);
        Policy actualPolicy1 = localPolicies.get(0);
        assertPolicy(policyElement1, actualPolicy1);
        PolicyTileListElement policyElement2 = list.row(2);
        Policy actualPolicy2 = localPolicies.get(1);
        assertPolicy(policyElement2, actualPolicy2);
        PolicyTileListElement policyElement3 = list.row(3);
        Policy actualPolicy3 = localPolicies.get(2);
        assertPolicy(policyElement3, actualPolicy3);

        list.buildHeaderColumn().anchor().click();
        list.buildHeaderColumn().upArrow().shouldHave(UP_SELECTED);
        list.threatLegendHeaderColumn().downArrow().shouldNotHave(DOWN_SELECTED);
        list.threatLegendHeaderColumn().upArrow().shouldNotHave(UP_SELECTED);

        policyElement1 = list.row(1);
        policyElement2 = list.row(2);
        policyElement3 = list.row(3);
        assertPolicy(policyElement1, actualPolicy1);
        assertPolicy(policyElement2, actualPolicy2);
        assertPolicy(policyElement3, actualPolicy3);

        list.buildHeaderColumn().anchor().click();
        list.buildHeaderColumn().upArrow().shouldNotHave(UP_SELECTED);
        list.buildHeaderColumn().downArrow().shouldHave(DOWN_SELECTED);

        policyElement1 = list.row(1);
        policyElement2 = list.row(2);
        policyElement3 = list.row(3);
        assertPolicy(policyElement1, actualPolicy3);
        assertPolicy(policyElement2, actualPolicy2);
        assertPolicy(policyElement3, actualPolicy1);
      }
      else {
        list.ownerName().shouldBe(hidden);
        list.rows().shouldHaveSize(0);
      }
    }
  }

  @Test
  public void testTiles_Inherited() {
    List<List<Label>> inheritedLabels = new ArrayList<>();
    List<List<LicenseThreatGroup>> inheritedLTGs = new ArrayList<>();
    List<List<Policy>> inheritedPolicies = new ArrayList<>();

    User testUser = tempEntity.newUser("testUser", "Inherited Test", "User", "testuser@sonatype.com");
    Role readRole = tempEntity.newRole("Read Only", false, Permission.READ);
    Role writeRole = tempEntity.newRole("Write Only", false, Permission.WRITE);

    List<Owner> parentOwners = new ArrayList<>();

    for (Owner owner : new OwnerDAO().walkHierarchy(currentOwner.getParentOwnerId())) {
      List<LicenseThreatGroup> ltgs = new ArrayList<>();
      List<Label> labels = new ArrayList<>();
      List<Policy> policies = new ArrayList<>();
      parentOwners.add(owner);

      if (owner.getId() != null) {
        labels.add(tempEntity.newLabel(owner.getId(), owner.getId() + " Label 1", Color.dark_purple));
        labels.add(tempEntity.newLabel(owner.getId(), owner.getId() + " Label 2", "With Subtitle", Color.dark_blue));

        inheritedLabels.add(labels);

        ltgs.add(tempEntity.newLicenseThreatGroup(owner.getId(), "Temp License 1 - " + owner.getName(), 9));
        ltgs.add(tempEntity.newLicenseThreatGroup(owner.getId(), "Temp License 2 - " + owner.getName(), 1));

        inheritedLTGs.add(ltgs);

        tempEntity.newMembershipMapping(owner.getId(), writeRole.getId(), testUser.getUsername());
        tempEntity.newMembershipMapping(owner.getId(), readRole.getId(), "Group", MemberType.GROUP);

        policies.add(tempEntity.newPolicy(owner.getId(), "Policy 1 " + owner.getName(), 10, Action.ID_FAIL,
            Stage.ID_BUILD, null));
        policies.add(tempEntity.newPolicy(owner.getId(), "Policy 2 " + owner.getName(), 5, Action.ID_WARN,
            Stage.ID_BUILD, null));

        inheritedPolicies.add(policies);
      }
    }

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    MainHeader.closeNavigationSidebar();
    testLabelTile_Inherited(inheritedLabels, parentOwners);
    testLTGTile_Inherited(inheritedLTGs, parentOwners);
    testAccessTile_Inherited(testUser, parentOwners);
    testPolicyTile_Inherited(inheritedPolicies, parentOwners);
  }

  private void testLabelTile_Inherited(List<List<Label>> inheritedLabels, List<Owner> parentOwners) {
    final int hierarchySize = parentOwners.size() + 1;
    LabelTile labelTile = OwnerSummaryPage.labelTile();
    assertThat(inheritedLabels).hasSameSizeAs(parentOwners);
    labelTile.labelLists().shouldHaveSize(hierarchySize);

    // scroll to the labels tile
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().labelsButtonInDropdown().shouldBe(visible).click();

    for (int i = 0; i < hierarchySize; i++) {
      TileSimpleList list = labelTile.labelList(i);

      if (i == 0) {
        list.subsectionHeader().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().shouldBe(visible);
        list.elements().shouldBe(empty);
      }
      else {
        final int expectedLabelCount = inheritedLabels.get(i - 1).size();
        list.elements().shouldHaveSize(expectedLabelCount);
        list.subsectionHeader().shouldBe(visible)
            .shouldHave(LabelTile.inheritedText(parentOwners.get(i - 1).getName()));

        for (int j = 0; j < expectedLabelCount; j++) {
          TileSimpleListElement actualLabel = list.element(j);
          Label expectedLabel = inheritedLabels.get(i - 1).get(j);

          if (expectedLabel.getDescription() == null) {
            actualLabel.description().shouldNot(exist);
          }
          else {
            actualLabel.description().shouldBe(visible).shouldHave(text(expectedLabel.getDescription()));
          }

          actualLabel.icon().shouldBe(visible).shouldHave(cssClass(expectedLabel.getColor().toValue()));
          actualLabel.name().shouldBe(visible).shouldHave(text(expectedLabel.getLabel()));
          actualLabel.chevron().shouldNot(exist);
        }
      }
    }
  }

  @Test
  public void testDeleteOwner() {
    List<Owner> parentOwners = new ArrayList<>();

    for (Owner owner : new OwnerDAO().walkHierarchy(currentOwner.getParentOwnerId())) {
      parentOwners.add(owner);
    }

    String ownerName = currentOwner.getName();

    // Test cancel button
    ActionDropDown.actionButton().click();
    ActionDropDown.deleteOwnerButton().shouldBe(visible).click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.cancelButton().click();

    DeleteModal.root().shouldBe(hidden);

    currentOwner = new OwnerDAO().getById(currentOwner.getId());

    OwnerSummaryPage.summaryTile().name().shouldBe(visible).shouldHave(text(currentOwner.getName()));
    assertThat(currentOwner).isNotNull();

    ActionDropDown.actionButton().click();
    ActionDropDown.deleteOwnerButton().shouldBe(visible).shouldHave(text(ownerName)).click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(DeleteModal.headerText(currentOwner.getType().toString()));
    DeleteModal.body().shouldHave(DeleteModal.bodyText(ownerName));

    DeleteModal.root().shouldBe(visible);
    DeleteModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    currentOwner = new OwnerDAO().getById(currentOwner.getId());

    assertThat(currentOwner).isNull();

    if (Organization.ROOT_ORGANIZATION_ID.equals(parentOwners.get(parentOwners.size() - 1).getId())) {
      OwnerSummaryPage.summaryTile().name().shouldBe(visible).shouldNotHave(text(ownerName));
    }
    else {
      OwnerSummaryPage.summaryTile().name().shouldBe(visible)
          .shouldNotHave(text(parentOwners.get(parentOwners.size() - 1).getName()));
    }
  }

  private void testLTGTile_Inherited(List<List<LicenseThreatGroup>> inheritedLTGs, List<Owner> parentOwners) {
    LicenseThreatGroupTile ltgTile = OwnerSummaryPage.licenseThreatGroupTile();

    // scroll to the ltgs
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().ltgsButton().shouldBe(visible).click();

    final int hierarchyCount = ltgTile.ltgLists().size();
    for (int i = 0; i < hierarchyCount; i++) {
      ThreatGroupTileSimpleList list = ltgTile.ltgList(i);

      if (i == 0) {
        if (OwnerType.APPLICATION.equals(currentOwner.getType())) {
          list.ownerName().shouldNot(exist);
          list.emptyDescriptor().shouldNot(exist);
        }
        else {
          list.ownerName().shouldBe(visible).shouldHave(text("Local"));
          list.emptyDescriptor().shouldBe(visible);
        }
        list.elements().shouldBe(empty);
      }
      else {
        int expectedTestLTGSize = Organization.ROOT_ORGANIZATION_ID
            .equals(parentOwners.get(i - 1).getId()) ? LicenseThreatGroupDataHelper.TEST_LICENSE_THREAT_GROUP_COUNT : 0;
        int expectedLTGCount = inheritedLTGs.get(i - 1).size() + expectedTestLTGSize;
        list.elements().shouldHaveSize(expectedLTGCount);
        list.ownerName().shouldBe(visible)
            .shouldHave(LicenseThreatGroupTile.inheritedText(parentOwners.get(i - 1).getName()));

        for (int j = 0; j < expectedLTGCount; j++) {
          ThreatGroupTileSimpleListElement actualLTG = list.element(j);

          if (inheritedLTGs.size() < i) {
            LicenseThreatGroup expectedLTG = inheritedLTGs.get(i - 1).get(j);
            actualLTG.name().shouldBe(visible).shouldHave(text(expectedLTG.getName()));
            actualLTG.threatLevel().shouldBe(visible)
                .shouldHave(ThreatGroupTileSimpleList.threatLevel(expectedLTG.getThreatLevel()));
          }

          actualLTG.chevron().shouldBe(hidden);
        }
      }
    }
  }

  private void testAccessTile_Inherited(User testUser, List<Owner> parentOwners) {

    int hierarchySize = getHierarchySize(currentOwner);
    AccessTile accessTile = OwnerSummaryPage.accessTile();
    accessTile.accessLists().shouldHaveSize(hierarchySize);

    // scroll to the access tile
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().accessButton().shouldBe(visible).click();

    for (int i = 0; i < hierarchySize; i++) {
      AccessTileList list = accessTile.accessList(i);

      if (i == 0) {
        list.ownerName().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().should(exist);

      }
      else {
        list.emptyDescriptor().shouldBe(hidden);
        list.ownerName().shouldBe(visible).shouldHave(AccessTile.inheritedText(parentOwners.get(i - 1).getName()));
        list.elements().shouldHaveSize(2);

        AccessTileListElement readOnly = list.element(0);
        readOnly.chevron().shouldBe(hidden);
        readOnly.role().shouldBe(visible).shouldHave(text("Read Only"));
        readOnly.groupIcon().shouldBe(visible);
        readOnly.members().shouldBe(visible).shouldHave(text("Group"));

        AccessTileListElement writeOnly = list.element(1);
        writeOnly.chevron().shouldBe(hidden);
        writeOnly.role().shouldBe(visible).shouldHave(text("Write Only"));
        writeOnly.userIcon().shouldBe(visible);
        writeOnly.members().shouldBe(visible).shouldHave(text(testUser.calculateDisplayName()));
      }
    }
  }

  private void testPolicyTile_Inherited(List<List<Policy>> inheritedPolicies, List<Owner> parentOwners) {
    int hierarchySize = parentOwners.size() + 1;
    PolicyTile policyTile = OwnerSummaryPage.policyTile();
    assertThat(inheritedPolicies).hasSameSizeAs(parentOwners);
    policyTile.policyLists().shouldHaveSize(hierarchySize);

    // scroll to the policy tile
    OwnerSummaryPage.summaryTile().policyButton().shouldBe(visible).click();

    for (int i = 0; i < hierarchySize; i++) {
      PolicyTileList list = policyTile.policyList(i);

      if (i == 0) {
        list.ownerName().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().should(exist);
      }
      else {
        list.emptyDescriptor().shouldBe(hidden);
        list.ownerName().shouldBe(visible).shouldHave(PolicyTile.inheritedText(parentOwners.get(i - 1).getName()));
        list.rows().shouldHaveSize(3); // 2 rows plus header

        assertPolicyHeader(list);

        PolicyTileListElement policyElement1 = list.row(1);
        Policy actualPolicy1 = inheritedPolicies.get(i - 1).get(0);
        assertPolicy(policyElement1, actualPolicy1);
        PolicyTileListElement policyElement2 = list.row(2);
        Policy actualPolicy2 = inheritedPolicies.get(i - 1).get(1);
        assertPolicy(policyElement2, actualPolicy2);

        list.nameHeaderColumn().anchor().click();
        list.nameHeaderColumn().upArrow().shouldHave(UP_SELECTED);
        list.threatLegendHeaderColumn().downArrow().shouldNotHave(DOWN_SELECTED);
        list.threatLegendHeaderColumn().upArrow().shouldNotHave(UP_SELECTED);

        assertPolicy(policyElement1, actualPolicy1);
        assertPolicy(policyElement2, actualPolicy2);

        list.nameHeaderColumn().anchor().click();
        list.nameHeaderColumn().upArrow().shouldNotHave(UP_SELECTED);
        list.nameHeaderColumn().downArrow().shouldHave(DOWN_SELECTED);

        assertPolicy(policyElement1, actualPolicy2);
        assertPolicy(policyElement2, actualPolicy1);
      }
    }
  }

  private void assertPolicyHeader(PolicyTileList list) {
    list.selectedHeaderElements().shouldHaveSize(1);

    HeaderColumn column = list.selectedHeaderColumn();
    column.root.shouldBe(visible).shouldHave(COLUMN_SELECTED);
    column.downArrow().shouldBe(visible).shouldHave(DOWN_SELECTED); // initial state
    column.upArrow().shouldBe(visible).shouldNotHave(UP_SELECTED);
  }

  private void assertPolicy(PolicyTileListElement policy, Policy actualPolicy) {
    String actionTypeId = actualPolicy.getActions().get(Stage.ID_BUILD);
    if (actionTypeId == null) {
      actionTypeId = "no action";
    }
    policy.chevron().shouldBe(visible);
    policy.threatLegend().shouldBe(visible).shouldHave(threatLevel(actualPolicy.getThreatLevel()));
    policy.name().shouldBe(visible).shouldHave(text(actualPolicy.getName()));
    policy.proxy().shouldBe(visible).shouldHave(PolicyTile.noActionText());
    policy.develop().shouldBe(visible).shouldHave(PolicyTile.noActionText());
    policy.build().shouldBe(visible).shouldHave(text(actionTypeId));
    policy.stageRelease().shouldBe(visible).shouldHave(PolicyTile.noActionText());
    policy.release().shouldBe(visible).shouldHave(PolicyTile.noActionText());
    policy.operate().shouldBe(visible).shouldHave(PolicyTile.noActionText());

    if (actionTypeId.equals(Action.ID_WARN)) {
      policy.build().find("i").shouldHave(PolicyTileListElement.WARN_ICON).shouldHave(PolicyTileListElement.WARN)
          .shouldNotHave(PolicyTileListElement.FAIL_ICON).shouldNotHave(PolicyTileListElement.FAIL);
    }
    else if (actionTypeId.equals(Action.ID_FAIL)) {
      policy.build().find("i").shouldHave(PolicyTileListElement.FAIL_ICON).shouldHave(PolicyTileListElement.FAIL)
          .shouldNotHave(PolicyTileListElement.WARN_ICON).shouldNotHave(PolicyTileListElement.WARN);
    }
  }

  @Test
  public void testPolicyTile_Foundation_Firewall() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION, ProductLicenseDetails.PRODUCT_FIREWALL);
    Policy policy = tempEntity.newPolicy(currentOwner.getId(), "Policy 1", 10, null, null, null);

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    OwnerSummaryPage.summaryTile().policyButton().shouldBe(visible).click();

    assertPolicyTile_Foundation(policy, false);
  }

  @Test
  public void testPolicyTile_Foundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    Policy policy = tempEntity.newPolicy(currentOwner.getId(), "Policy 1", 10, null, null, null);

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    OwnerSummaryPage.summaryTile().policyButton().shouldBe(visible).click();
    
    assertPolicyTile_Foundation(policy, true);
    eyesWatcher.eyesCheck();
  }
  
  private void assertPolicyTile_Foundation(Policy policy, boolean proxyActionReadOnly) {
    PolicyTile policyTile = OwnerSummaryPage.policyTile();
    PolicyTileList list = policyTile.policyList(0);

    PolicyTileListElement policyElement = list.row(1);
    policyElement.threatLegend().shouldBe(visible).shouldHave(threatLevel(policy.getThreatLevel()));
    policyElement.name().shouldBe(visible).shouldHave(text(policy.getName()));

    HeaderColumn proxy = list.header(2);
    proxy.anchor().shouldHave(text("PROXY"));
    if (proxyActionReadOnly) {
      proxy.root.shouldHave(IQ_DISABLED);
      policyElement.column(2).shouldBe(visible).shouldHave(PolicyTile.noActionText()).shouldHave(IQ_DISABLED);
    }
    else {
      proxy.root.shouldNotHave(IQ_DISABLED);
      policyElement.column(2).shouldBe(visible).shouldHave(PolicyTile.noActionText()).shouldNotHave(IQ_DISABLED);
    }

    // check a few of the other stages (develop, and operate) and make sure they're disabled.
    HeaderColumn develop = list.header(3);
    develop.anchor().shouldHave(text("DEVELOP"));
    develop.root.shouldHave(IQ_DISABLED);
    policyElement.column(3).shouldBe(visible).shouldHave(PolicyTile.noActionText()).shouldHave(IQ_DISABLED);
    HeaderColumn operate = list.header(8);
    // Uncomment when fixing CLM-18691
    //operate.anchor().shouldHave(text("OPERATE"));
    operate.root.shouldHave(IQ_DISABLED);
    policyElement.column(8).shouldBe(visible).shouldHave(PolicyTile.noActionText()).shouldHave(IQ_DISABLED);

    policyElement.chevron().shouldBe(visible);
  }

  @Test
  public void testPolicyTile_LimitedStageLicensing() {
    List<Policy> localPolicies = new ArrayList<>();
    localPolicies
        .add(tempEntity.newPolicy(currentOwner.getId(), "Release", 10, Action.ID_FAIL, Stage.ID_RELEASE, null));

    setLicensedProducts(ProductLicenseDetails.PRODUCT_RISK);
    
    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    PolicyTile policyTile = OwnerSummaryPage.policyTile();

    OwnerSummaryPage.summaryTile().policyButton().shouldBe(visible).click();

    PolicyTileList list = policyTile.policyList(0);

    PolicyTileListElement policyElement = list.row(1);
    Policy actualPolicy = localPolicies.get(0);
    HeaderColumn proxy = list.header(2);

    list.nameHeaderColumn().anchor().click();
    list.nameHeaderColumn().upArrow().shouldHave(UP_SELECTED);
    proxy.downArrow().shouldNotHave(DOWN_SELECTED);
    proxy.upArrow().shouldNotHave(UP_SELECTED);

    //should only have proxy and release
    proxy.anchor().shouldHave(text("PROXY")).click();
    proxy.upArrow().shouldHave(UP_SELECTED);
    HeaderColumn release = list.header(3);
    release.anchor().shouldHave(text("RELEASE"));
    release.downArrow().shouldNotHave(DOWN_SELECTED);
    release.upArrow().shouldNotHave(UP_SELECTED);
    list.header(4).name().shouldNot(exist);

    policyElement.chevron().shouldBe(visible);
    policyElement.threatLegend().shouldBe(visible).shouldHave(threatLevel(actualPolicy.getThreatLevel()));
    policyElement.name().shouldBe(visible).shouldHave(text(actualPolicy.getName()));
    policyElement.column(2).shouldBe(visible).shouldHave(PolicyTile.noActionText());
    policyElement.column(3).shouldBe(visible).shouldHave(text(actualPolicy.getActions().get(Stage.ID_RELEASE)));
    policyElement.column(4).shouldHave(PolicyTileListElement.CHEVRON);
  }

  protected int getHierarchySize(Owner owner) {
    int hierarchySize = 0;
    Iterator<Owner> iterator = new OwnerDAO().walkHierarchy(owner).iterator();

    for (; iterator.hasNext(); ++hierarchySize) {
      iterator.next();
    }
    return hierarchySize;
  }

  protected abstract void testReportLinks();

  protected abstract void testApplicationCategoryTile();
}
