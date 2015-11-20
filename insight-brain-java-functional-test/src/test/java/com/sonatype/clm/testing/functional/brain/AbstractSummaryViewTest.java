/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.AccessTile;
import com.sonatype.clm.testing.functional.elements.AccessTileList;
import com.sonatype.clm.testing.functional.elements.AccessTileList.AccessTileListElement;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.elements.LabelTile;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupTile;
import com.sonatype.clm.testing.functional.elements.OwnerEditorDialog;
import com.sonatype.clm.testing.functional.elements.ThreatGroupTileSimpleList;
import com.sonatype.clm.testing.functional.elements.ThreatGroupTileSimpleList.ThreatGroupTileSimpleListElement;
import com.sonatype.clm.testing.functional.elements.OwnerEditorDialog;
import com.sonatype.clm.testing.functional.elements.TileSimpleList;
import com.sonatype.clm.testing.functional.elements.TileSimpleList.TileSimpleListElement;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public abstract class AbstractSummaryViewTest
    extends AbstractFunctionalTest
{
  private Owner currentOwner;

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  @BeforeClass
  public static void boot() {
    open(ReportListPage.URL);
    loginAsAdmin();
  }

  protected void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    open(OwnerSummaryPage.url(currentOwner.getType().toString(), currentOwner.getPublicId()));
    OwnerSummaryPage.SummaryTile.name().shouldHave(text(currentOwner.getName()));
  }

  @Test
  public void testSummaryTile() {
    OwnerSummaryPage.SummaryTile.name().shouldBe(visible).shouldHave(text(currentOwner.getName()));
    OwnerSummaryPage.SummaryTile.icon().shouldBe(visible);
  }

  @Test
  public void testSummaryTile_missing() {
    open(OwnerSummaryPage.url(currentOwner.getType().toString(), "fakeid"));

    ErrorBox error = OwnerSummaryPage.SummaryTile.error();
    error.root().shouldBe(visible);
    error.message().shouldHave(text("Could not find an " + currentOwner.getType().toString()));
    error.retryButton().shouldBe(visible, enabled);
  }

  @Test
  public void testActionDropDown() {
    ActionDropDown.actionButton().shouldBe(visible);
    ActionDropDown.menu().shouldNotBe(visible);
    ActionDropDown.actionButton().click();
    ActionDropDown.menu().shouldBe(visible);
  }

  @Test
  public void testEditAppOrgNameLink() {
    String shortTypeName = currentOwner.getType().toString().equalsIgnoreCase("application") ? "App" : "Org";
    ActionDropDown.actionButton().click();
    ActionDropDown.editOwner().shouldHave(text(shortTypeName));
    OwnerEditorDialog.root().shouldNotBe(visible);
    ActionDropDown.editOwner().click();
    OwnerEditorDialog.root().shouldBe(visible);
    OwnerEditorDialog.title().shouldHave(text(currentOwner.getType().toString()));
  }

  @Test
  public void testTile_default() {
    testLabelTile_no_labels();
    testAccessTile_no_local_access();
  }
  
  public void testLabelTile_no_labels() {
    int hierarchySize = getHierarchySize(currentOwner.getId());

    LabelTile labelTile = new LabelTile();
    labelTile.subHeader().shouldBe(visible).shouldHave(LabelTile.subHeaderText(currentOwner.getName()));
    labelTile.newButton().shouldBe(visible, enabled);

    labelTile.labelLists().shouldHaveSize(hierarchySize);

    for (int i = 0; i < labelTile.labelLists().size(); i++) {
      TileSimpleList list = labelTile.labelList(i);

      if (i == 0) {
        list.subsectionHeader().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().shouldBe(visible);
      }
      else {
        list.subsectionHeader().shouldNotBe(visible);
      }

      list.elements().shouldBe(empty);
    }
  }
  
  public void testAccessTile_no_local_access() {

    int hierarchySize = getHierarchySize(currentOwner.getId());
    AccessTile accessTile = new AccessTile();
    accessTile.subHeader().shouldBe(visible).shouldHave(AccessTile.subHeaderText(currentOwner.getName()));
    accessTile.newButton().shouldBe(visible, enabled);
    accessTile.accessLists().shouldHaveSize(hierarchySize);

    // scroll to the access tile
    OwnerSummaryPage.SummaryTile.accessButton().shouldBe(visible).click();

    for (int i = 0; i < accessTile.accessLists().size(); i++) {
      AccessTileList list = accessTile.accessList(i);

      if (i == 0) {
        list.ownerName().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().should(exist);

      } else {
        list.ownerName().shouldNotBe(visible);
        list.emptyDescriptor().shouldNotBe(visible);
      }
    }
  }
  
  @Test
  public void testTiles_Local() {

    List<Label> localLabels = new ArrayList<>();
    localLabels.add(tempEntity.newLabel(currentOwner.getId(), "Temp Local Label 1", Color.black));
    localLabels.add(tempEntity.newLabel(currentOwner.getId(), "Temp Local Label 2", "With Subtitle", Color.blue));

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

    refreshOrOpen(OwnerSummaryPage.url(currentOwner.getType().toString(), currentOwner.getPublicId()));
    testLabelTile_Local(localLabels);
    testLTGTile_Local(locaLTGs);
    testAccessTile_Local(testUser);
  }

  private void testLabelTile_Local(List<Label> localLabels) {

    int hierarchySize = getHierarchySize(currentOwner.getId());
    LabelTile labelTile = new LabelTile();
    labelTile.labelLists().shouldHaveSize(hierarchySize);

    for (int i = 0; i < labelTile.labelLists().size(); i++) {
      TileSimpleList list = labelTile.labelList(i);

      if (i == 0) {
        list.subsectionHeader().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().shouldNot(exist);
        list.elements().shouldHaveSize(localLabels.size());

        for (int j = 0; j < list.elements().size(); j++) {
          TileSimpleListElement actualLabel = list.element(j);
          Label expectedLabel = localLabels.get(j);

          if (expectedLabel.getDescription() == null) {
            actualLabel.description().shouldNot(exist);
          }
          else {
            actualLabel.description().shouldBe(visible).shouldHave(text(expectedLabel.getDescription()));
          }

          actualLabel.icon().shouldBe(visible).shouldHave(cssClass(expectedLabel.getColor().toString()));
          actualLabel.name().shouldBe(visible).shouldHave(text(expectedLabel.getLabel()));
          actualLabel.chevron().shouldBe(visible);
        }
      }
      else {
        list.subsectionHeader().shouldNot(exist);
        list.elements().shouldBe(empty);
      }
    }
  }

  private void testLTGTile_Local(List<LicenseThreatGroup> locaLTGs) {
    int hierarchySize = getHierarchySize(currentOwner.getId());

    LicenseThreatGroupTile ltgTile = new LicenseThreatGroupTile();
    ltgTile.ltgLists().shouldHaveSize(hierarchySize);

    if (OwnerType.APPLICATION.equals(currentOwner.getType())) {
      ltgTile.newButton().shouldNotBe(visible);
    }
    else {
      ltgTile.newButton().shouldBe(visible);
    }

    // scroll to the ltgs
    OwnerSummaryPage.SummaryTile.ltgsButton().shouldBe(visible).click();

    for (int i = 0; i < ltgTile.ltgLists().size(); i++) {
      ThreatGroupTileSimpleList list = ltgTile.ltgList(i);

      if (i == 0) {
        list.ownerName().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().shouldNot(exist);
        list.elements().shouldHaveSize(locaLTGs.size());

        for (int j = 0; j < list.elements().size(); j++) {
          ThreatGroupTileSimpleListElement actualLTG = list.element(j);
          LicenseThreatGroup expectedLTG = locaLTGs.get(j);

          actualLTG.name().shouldBe(visible).shouldHave(text(expectedLTG.getName()));

          actualLTG.threatLevel().shouldBe(visible).shouldHave(
              ThreatGroupTileSimpleList.threatLevel(expectedLTG.getThreatLevel()));
          actualLTG.chevron().shouldBe(visible);
        }
      }
      else if (i != hierarchySize - 1) {
        list.ownerName().shouldNot(exist);
        list.elements().shouldBe(empty);
      }
      else {
        list.ownerName().shouldBe(visible);
        list.emptyDescriptor().shouldNotBe(visible);
        list.elements().shouldHaveSize(LicenseThreatGroupDAO.DEFAULT_LICENSE_THREAT_GROUP_COUNT);
      }
    }
  }

  private void testAccessTile_Local(User testUser) {

    int hierarchySize = getHierarchySize(currentOwner.getId());
    AccessTile accessTile = new AccessTile();
    accessTile.accessLists().shouldHaveSize(hierarchySize);

    // scroll to the access tile
    OwnerSummaryPage.SummaryTile.accessButton().shouldBe(visible).click();

    for (int i = 0; i < accessTile.accessLists().size(); i++) {
      AccessTileList list = accessTile.accessList(i);
      list.emptyDescriptor().shouldNotBe(visible);

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

      } else {
        list.ownerName().shouldNotBe(visible);
        list.elements().shouldHaveSize(0);
      }
    }
  }

  @Test
  public void testTiles_Inherited() {
    List<List<Label>> inheritedLabels = new ArrayList<>();
    List<List<LicenseThreatGroup>> inheritedLTGs = new ArrayList<>();
    User testUser = tempEntity.newUser("testUser", "Inherited Test", "User", "testuser@sonatype.com");
    Role readRole = tempEntity.newRole("Read Only", false, Permission.READ);
    Role writeRole = tempEntity.newRole("Write Only", false, Permission.WRITE);
    RoleDAO roleDAO = new RoleDAO();
    List<Role> roleList = new ArrayList<>(roleDAO.getApplicationRoles());
    
    List<Owner> parentOwners = new ArrayList<>();
    
    for (Owner owner : new OwnerDAO().walkHierarchy(currentOwner.getParentOwnerId())) {
      List<LicenseThreatGroup> ltgs = new ArrayList<>();
      List<Label> labels = new ArrayList<>();
      parentOwners.add(owner);

      if (owner.getId() != null) {
        labels.add(tempEntity.newLabel(owner.getId(), owner.getId() + " Label 1", Color.black));
        labels.add(tempEntity.newLabel(owner.getId(), owner.getId() + " Label 2", "With Subtitle", Color.blue));

        inheritedLabels.add(labels);

        ltgs.add(tempEntity.newLicenseThreatGroup(owner.getId(), "Temp License 1 - " + owner.getName(), 9));
        ltgs.add(tempEntity.newLicenseThreatGroup(owner.getId(), "Temp License 2 - " + owner.getName(), 1));

        inheritedLTGs.add(ltgs);

        tempEntity.newMembershipMapping(owner.getId(), writeRole.getId(), testUser.getUsername());
        tempEntity.newMembershipMapping(owner.getId(), readRole.getId(), "Group", MemberType.GROUP);
      }
    }

    refreshOrOpen(OwnerSummaryPage.url(currentOwner.getType().toString(), currentOwner.getPublicId()));
    testLabelTile_Inherited(inheritedLabels, parentOwners);
    testLTGTile_Inherited(inheritedLTGs, parentOwners);
    testAccessTile_Inherited(testUser, parentOwners);
  }

  private void testLabelTile_Inherited(List<List<Label>> inheritedLabels, List<Owner> parentOwners) {
    LabelTile labelTile = new LabelTile();
    assertThat(inheritedLabels.size(), equalTo(parentOwners.size()));
    labelTile.labelLists().shouldHaveSize(parentOwners.size() + 1);

    for (int i = 0; i < labelTile.labelLists().size(); i++) {
      TileSimpleList list = labelTile.labelList(i);

      if (i == 0) {
        list.subsectionHeader().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().shouldBe(visible);
        list.elements().shouldBe(empty);
      }
      else {
        list.elements().shouldHaveSize(inheritedLabels.get(i - 1).size());
        list.subsectionHeader().shouldBe(visible)
            .shouldHave(LabelTile.inheritedText(parentOwners.get(i - 1).getName()));

        for (int j = 0; j < list.elements().size(); j++) {
          TileSimpleListElement actualLabel = list.element(j);
          Label expectedLabel = inheritedLabels.get(i - 1).get(j);

          if (expectedLabel.getDescription() == null) {
            actualLabel.description().shouldNot(exist);
          }
          else {
            actualLabel.description().shouldBe(visible).shouldHave(text(expectedLabel.getDescription()));
          }

          actualLabel.icon().shouldBe(visible).shouldHave(cssClass(expectedLabel.getColor().toString()));
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

    DeleteModal.root().shouldNotBe(visible);

    currentOwner = new OwnerDAO().getById(currentOwner.getId());

    OwnerSummaryPage.SummaryTile.name().shouldBe(visible).shouldHave(text(currentOwner.getName()));
    assertThat(currentOwner, is(not(nullValue())));

    ActionDropDown.actionButton().click();
    ActionDropDown.deleteOwnerButton().shouldBe(visible).shouldHave(text(ownerName));
    ActionDropDown.deleteOwnerButton().click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(DeleteModal.headerText(currentOwner.getType().toString()));
    DeleteModal.body().shouldHave(DeleteModal.bodyText(ownerName));


    DeleteModal.root().shouldBe(visible);
    DeleteModal.deleteButton().click();

    // Modal should be hidden 800 ms after delete REST call is successful
    DeleteModal.root().shouldNotBe(visible);

    currentOwner = new OwnerDAO().getById(currentOwner.getId());

    assertThat(currentOwner, is(nullValue()));

    if (Organization.ROOT_ORGANIZATION_ID.equals(parentOwners.get(parentOwners.size() - 1).getId())) {
      OwnerSummaryPage.SummaryTile.name().shouldBe(visible).shouldNotHave(text(ownerName));
    }
    else {
      OwnerSummaryPage.SummaryTile.name().shouldBe(visible)
          .shouldNotHave(text(parentOwners.get(parentOwners.size() - 1).getName()));
    }
  }

  private void testLTGTile_Inherited(List<List<LicenseThreatGroup>> inheritedLTGs, List<Owner> parentOwners) {
    LicenseThreatGroupTile ltgTile = new LicenseThreatGroupTile();

    // scroll to the ltgs
    OwnerSummaryPage.SummaryTile.ltgsButton().shouldBe(visible).click();

    for (int i = 0; i < ltgTile.ltgLists().size(); i++) {
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
        int expectedDefaultLTGSize = Organization.ROOT_ORGANIZATION_ID.equals(parentOwners.get(i - 1).getId())
            ? LicenseThreatGroupDAO.DEFAULT_LICENSE_THREAT_GROUP_COUNT : 0;
        list.elements().shouldHaveSize(inheritedLTGs.get(i - 1).size() + expectedDefaultLTGSize);
        list.ownerName().shouldBe(visible)
            .shouldHave(LicenseThreatGroupTile.inheritedText(parentOwners.get(i - 1).getName()));

        for (int j = 0; j < list.elements().size(); j++) {
          ThreatGroupTileSimpleListElement actualLTG = list.element(j);

          if (inheritedLTGs.size() < i) {
            LicenseThreatGroup expectedLTG = inheritedLTGs.get(i - 1).get(j);
            actualLTG.name().shouldBe(visible).shouldHave(text(expectedLTG.getName()));
            actualLTG.threatLevel().shouldBe(visible).shouldHave(
                ThreatGroupTileSimpleList.threatLevel(expectedLTG.getThreatLevel()));
          }

          actualLTG.chevron().shouldBe(visible);
        }
      }
    }
  }

  private void testAccessTile_Inherited(User testUser, List<Owner> parentOwners) {

    int hierarchySize = getHierarchySize(currentOwner.getId());
    AccessTile accessTile = new AccessTile();
    accessTile.accessLists().shouldHaveSize(hierarchySize);

    // scroll to the access tile
    OwnerSummaryPage.SummaryTile.accessButton().shouldBe(visible).click();

    for (int i = 0; i < accessTile.accessLists().size(); i++) {
      AccessTileList list = accessTile.accessList(i);

      if (i == 0) {
        list.ownerName().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().should(exist);

      } else {
        list.emptyDescriptor().shouldNotBe(visible);
        list.ownerName().shouldBe(visible)
            .shouldHave(AccessTile.inheritedText(parentOwners.get(i - 1).getName()));
        list.elements().shouldHaveSize(2);

        AccessTileListElement readOnly = list.element(0);
        readOnly.chevron().shouldNotBe(visible);
        readOnly.role().shouldBe(visible).shouldHave(text("Read Only"));
        readOnly.groupIcon().shouldBe(visible);
        readOnly.members().shouldBe(visible).shouldHave(text("Group"));

        AccessTileListElement writeOnly = list.element(1);
        writeOnly.chevron().shouldNotBe(visible);
        writeOnly.role().shouldBe(visible).shouldHave(text("Write Only"));
        writeOnly.userIcon().shouldBe(visible);
        writeOnly.members().shouldBe(visible).shouldHave(text(testUser.calculateDisplayName()));
      }
    }
  }

  protected int getHierarchySize(String ownerId) {
    int hierarchySize = 0;
    Iterator<Owner> iterator = new OwnerDAO().walkHierarchy(ownerId).iterator();

    for (; iterator.hasNext(); ++hierarchySize) {
      iterator.next();
    }
    return hierarchySize;
  }

  protected abstract void testReportLinks();

  protected abstract void testApplicationCategoryTile();
}
