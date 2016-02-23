/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.AccessTileList;
import com.sonatype.clm.testing.functional.elements.AccessTileList.AccessTileListElement;
import com.sonatype.clm.testing.functional.elements.RepositoriesAccessTile;
import com.sonatype.clm.testing.functional.pages.RepositoriesSummaryPage;
import com.sonatype.clm.testing.functional.pages.RepositoriesSummaryPage.SummaryTile;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;

public class RepositoriesSummaryPageTest
    extends AbstractFunctionalTest
{
  private static final int HIERARCHY_SIZE = 2;

  @BeforeClass
  public static void startup() {
    open(RepositoriesSummaryPage.URL);
    loginAsAdmin();
  }

  @Before
  public void init() {
    open(RepositoriesSummaryPage.URL);
  }

  @Test
  public void testRepositorySummaryView()
  {
    SelenideElement nameElement = SummaryTile.name();
    nameElement.isDisplayed();
    nameElement.shouldHave(text("Repositories"));
    SummaryTile.configButton().isDisplayed();
    SummaryTile.accessButton().isDisplayed();
  }

  @Test
  public void testRepositoryTile_default() {
    RepositoriesAccessTile accessTile = new RepositoriesAccessTile();
    accessTile.subHeader().shouldBe(visible).shouldHave(RepositoriesAccessTile.subHeaderText("All Repositories"));
    accessTile.newButton().shouldBe(visible, enabled);
    accessTile.accessLists().shouldHaveSize(HIERARCHY_SIZE);

    // scroll to the access tile
    RepositoriesSummaryPage.SummaryTile.accessButton().shouldBe(visible).click();

    AccessTileList localList = accessTile.accessList(0);

    localList.ownerName().shouldBe(visible).shouldHave(text("Local"));
    localList.emptyDescriptor().should(exist);

    AccessTileList inheritedList = accessTile.accessList(1);

    inheritedList.ownerName().shouldNotBe(visible);
    inheritedList.emptyDescriptor().shouldNotBe(visible);
  }

  @Test
  public void testRepositoryTile_Local() {
    Role readRole = tempEntity.newRole("Read Only", false, Permission.READ);
    Role writeRole = tempEntity.newRole("Write Only", false, Permission.WRITE);
    User testUser = tempEntity.newUser("testUser", "Test", "User", "testuser@sonatype.com");
    RoleDAO roleDAO = new RoleDAO();
    List<Role> roleList = new ArrayList<>(roleDAO.getApplicationRoles());
    tempEntity
        .newMembershipMapping(RepositoryContainer.REPOSITORY_CONTAINER_ID, writeRole.getId(), testUser.getUsername());
    tempEntity
        .newMembershipMapping(RepositoryContainer.REPOSITORY_CONTAINER_ID, readRole.getId(), "Group", MemberType.GROUP);
    roleList.add(readRole);
    roleList.add(writeRole);

    refresh();

    RepositoriesAccessTile accessTile = new RepositoriesAccessTile();
    accessTile.accessLists().shouldHaveSize(HIERARCHY_SIZE);

    // scroll to the access tile
    RepositoriesSummaryPage.SummaryTile.accessButton().shouldBe(visible).click();

    AccessTileList localList = accessTile.accessList(0);
    localList.emptyDescriptor().shouldNotBe(visible);

    localList.elements().shouldHaveSize(2);
    localList.ownerName().shouldBe(visible).shouldHave(text("Local"));

    AccessTileListElement readOnly = localList.element(0);
    readOnly.chevron().shouldBe(visible);
    readOnly.role().shouldBe(visible).shouldHave(text("Read Only"));
    readOnly.groupIcon().shouldBe(visible);
    readOnly.members().shouldBe(visible).shouldHave(text("Group"));

    AccessTileListElement writeOnly = localList.element(1);
    writeOnly.chevron().shouldBe(visible);
    writeOnly.role().shouldBe(visible).shouldHave(text("Write Only"));
    writeOnly.userIcon().shouldBe(visible);
    writeOnly.members().shouldBe(visible).shouldHave(text(testUser.calculateDisplayName()));

    AccessTileList inheritedList = accessTile.accessList(1);

    inheritedList.ownerName().shouldNotBe(visible);
    inheritedList.elements().shouldHaveSize(0);
  }

  @Test
  public void testTiles_Inherited() {
    User testUser = tempEntity.newUser("testUser", "Inherited Test", "User", "testuser@sonatype.com");
    Role readRole = tempEntity.newRole("Read Only", false, Permission.READ);
    Role writeRole = tempEntity.newRole("Write Only", false, Permission.WRITE);

    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, writeRole.getId(), testUser.getUsername());
    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, readRole.getId(), "Group", MemberType.GROUP);

    refresh();

    RepositoriesAccessTile accessTile = new RepositoriesAccessTile();
    accessTile.accessLists().shouldHaveSize(HIERARCHY_SIZE);

    // scroll to the access tile
    RepositoriesSummaryPage.SummaryTile.accessButton().shouldBe(visible).click();

    AccessTileList localList = accessTile.accessList(0);

    localList.ownerName().shouldBe(visible).shouldHave(text("Local"));
    localList.emptyDescriptor().should(exist);

    AccessTileList inheritedList = accessTile.accessList(1);

    inheritedList.emptyDescriptor().shouldNotBe(visible);
    inheritedList.ownerName().shouldBe(visible).shouldHave(RepositoriesAccessTile.inheritedText("Root Organization"));
    inheritedList.elements().shouldHaveSize(2);

    AccessTileListElement readOnly = inheritedList.element(0);
    readOnly.chevron().shouldNotBe(visible);
    readOnly.role().shouldBe(visible).shouldHave(text("Read Only"));
    readOnly.groupIcon().shouldBe(visible);
    readOnly.members().shouldBe(visible).shouldHave(text("Group"));

    AccessTileListElement writeOnly = inheritedList.element(1);
    writeOnly.chevron().shouldNotBe(visible);
    writeOnly.role().shouldBe(visible).shouldHave(text("Write Only"));
    writeOnly.userIcon().shouldBe(visible);
    writeOnly.members().shouldBe(visible).shouldHave(text(testUser.calculateDisplayName()));
  }
}
