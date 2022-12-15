/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.*;
import com.sonatype.clm.testing.functional.pages.AccessEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.*;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.pages.AccessEditorPage.DISABLED_GROUP_SEARCH_WARNING;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractAccessEditorTest
    extends AbstractFunctionalTest
{
  private static final List<Role> APPLICATION_ROLES = new RoleDAO().getApplicationRoles();

  private final MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();

  private final RoleDAO roleDAO = new RoleDAO();

  protected Owner currentOwner;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  protected void init(Owner owner) {
    this.currentOwner = owner;

    User u1 = tempEntity.newUser();
    User u2 = tempEntity.newUser();
    Role role = APPLICATION_ROLES.get(0);
    tempEntity.newMembershipMapping(currentOwner.getId(), role.getId(), u1.getUsername());
    tempEntity.newMembershipMapping(currentOwner.getId(), role.getId(), u2.getUsername());

    role = APPLICATION_ROLES.get(2);
    tempEntity.newMembershipMapping(currentOwner.getId(), role.getId(), u1.getUsername());

    refreshOrOpen(OwnerSummaryPage.url(owner));
    shouldBeOnInitialPage();
  }

  protected void shouldBeOnInitialPage() {
    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
  }

  @Test
  public void testUserToolTip() {
    tempEntity.newUser("Michael-Hammerrin", "Michael", "Hammerrin", "Mike@sonatype.com");
    goFromSummaryToAddRole();
    AccessEditorPage accessEditorPage = new AccessEditorPage();
    AccessEditorPage.AddMembersForm addMembersForm = accessEditorPage.addMembersForm();
    addMembersForm.searchBox().setValue("Michael*").click();
    addMembersForm.searchResults().get(0).click();
    addMembersForm.addedItems().get(0).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("IQ Server Mike@sonatype.com"));
  }

  @Test
  public void testUserTooltip_UserGroup() {
    Role role = tempEntity.newRole("Write Only", false);
    tempEntity.newMembershipMapping(currentOwner.getId(), role.getId(), "Loooooong Name Group", MemberType.GROUP);
    refresh();
    goFromSummaryToEditRole(role);
    AccessEditorPage accessEditorPage = new AccessEditorPage();
    AccessEditorPage.AddMembersForm addMembersForm = accessEditorPage.addMembersForm();
    addMembersForm.addedItems().get(0).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Loooooong Name Group"));
  }

  @Test
  public void testAddRole() {
    goFromSummaryToAddRole();
    OwnerDetailSidebar.accessGroup().items().shouldHaveSize(3);

    AccessEditorPage accessEditorPage = new AccessEditorPage();
    AccessEditorPage.AddMembersForm addMembersForm = accessEditorPage.addMembersForm();

    assertAddRoleInitialStateIsCorrect(APPLICATION_ROLES.size() - 1, accessEditorPage);

    NxFormSelect roleDropdown = addMembersForm.roleSelect();
    roleDropdown.shouldHave(AccessEditorPage.DROPDOWN_DEFAULT_TEXT).click();
    eyesWatcher.eyesCheck();
    String roleName = roleDropdown.listItem(2).text();
    assertThat(getMembershipMappings(currentOwner.getId(), roleName)).isEmpty();
    roleDropdown.listItem(2).click();
    addMembersForm.saveButton().shouldHave(cssClass("disabled"));
    addMembersForm.searchBox().setValue("*").click();

    addMembersForm.searchResults().shouldHaveSize(4);
    addMembersForm.searchResults()
            .shouldHave(texts("Admin BuiltIn", "John Doe", "John Doe", "Authenticated Users (Group)"));

    addMembersForm.searchResults().get(0).shouldHave(text("Admin BuiltIn"));
    addMembersForm.searchResults().get(0).click();
    addMembersForm.addedItems().shouldHaveSize(1);

    addMembersForm.searchBox().setValue("*").click();
    addMembersForm.searchResults().get(0).click();
    addMembersForm.addedItems().shouldHaveSize(2);

    addMembersForm.searchBox().setValue("*").click();
    addMembersForm.searchResults().get(0).click();
    addMembersForm.addedItems().shouldHaveSize(3);

    addMembersForm.searchBox().setValue("*").click();
    addMembersForm.searchResults().get(0).shouldHave(text("Authenticated Users (Group)"));
    addMembersForm.searchResults().get(0).click();

    addMembersForm.addedItems().shouldHaveSize(4);
    addMembersForm.addedItems()
            .shouldHave(texts("Admin BuiltIn", "Authenticated Users (Group)", "John Doe", "John Doe"));

    addMembersForm.saveButton().shouldNotHave(cssClass("disabled")).click();
    FormMask.seeAndWaitForDismissal();

    OwnerDetailSidebar.accessGroup().items().shouldHaveSize(4);
    OwnerDetailSidebar.accessGroup().item(3).shouldHave(text(roleName));
    assertAddRoleInitialStateIsCorrect(APPLICATION_ROLES.size() - 2, accessEditorPage);
    assertThatRoleNotAvailableInDropdown(roleName, addMembersForm);
    List<MembershipMapping> membershipMappings = getMembershipMappings(currentOwner.getId(), roleName);
    tempEntity.register(membershipMappings.toArray(new MembershipMapping[membershipMappings.size()]));
    assertThat(membershipMappings).hasSize(4);
  }

  @Test
  public void testEdit() {
    Role role = APPLICATION_ROLES.get(0);
    goFromSummaryToEditRole(role);

    OwnerDetailSidebar.accessGroup().item(1).shouldBe(CLM.SELECTED);

    AccessEditorPage accessEditorPage = new AccessEditorPage();
    AccessEditorPage.AddMembersForm addMembersForm = accessEditorPage.addMembersForm();
    accessEditorPage.title().shouldHave(text(role.getName()));
    assertCommonInitialStateIsCorrect(accessEditorPage);

    addMembersForm.searchBox().setValue("*").click();
    addMembersForm.searchResults().shouldHaveSize(2);

    addMembersForm.searchResults().get(0).shouldHave(text("Admin BuiltIn"));
    addMembersForm.searchResults().get(1).shouldHave(text("Authenticated Users (Group)"));

    addMembersForm.searchResults().get(0).click();
    addMembersForm.addedItems().shouldHaveSize(3);

    addMembersForm.searchBox().setValue("*").click();
    addMembersForm.searchResults().get(0).click();

    addMembersForm.addedItems().shouldHaveSize(4);
    addMembersForm.addedItems()
            .shouldHave(texts("Admin BuiltIn", "Authenticated Users (Group)", "John Doe", "John Doe"));

    addMembersForm.saveButton().shouldNotHave(cssClass("disabled")).click();
    FormMask.seeAndWaitForDismissal();
    assertCommonInitialStateIsCorrect(accessEditorPage);
    List<MembershipMapping> membershipMappings = getMembershipMappings(currentOwner.getId(), role.getName());
    tempEntity.register(membershipMappings.toArray(new MembershipMapping[membershipMappings.size()]));
    assertThat(getMembershipMappings(currentOwner.getId(), role.getName())).hasSize(4);
  }

  @Test
  public void testRemoveBySavingWithNoPickedUsers() {
    Role role = APPLICATION_ROLES.get(2);
    refreshOrOpen(AccessEditorPage.urlToEdit(currentOwner, role.getId()));

    AccessEditorPage accessEditorPage = new AccessEditorPage();
    AccessEditorPage.AddMembersForm addMembersForm = accessEditorPage.addMembersForm();
    addMembersForm.addedItems().shouldHaveSize(1);

    OwnerDetailSidebar.accessGroup().entryItems().shouldHave(sizeGreaterThan(0));
    int initialNumAddedRoles = OwnerDetailSidebar.accessGroup().entryItems().size();

    addMembersForm.addedItems().get(0).click();
    addMembersForm.saveButton().shouldNotHave(cssClass("disabled")).click();
    NxDeleteModal deleteModal = addMembersForm.getDeleteModal();
    deleteModal.alertContent().shouldBe(visible).shouldHave(
            text(addMembersForm.confirmRemovalThroughUpdateText(role.getName(), currentOwner.getType())));
    deleteModal.header().shouldHave(AccessEditorPage.CONFIRM_REMOVAL_HEADER_TEXT);
    deleteModal.submitButton().click();
    FormMask.seeAndWaitForDismissal();
    deleteModal.shouldBe(hidden);
    OwnerDetailSidebar.accessGroup().entryItems().shouldHaveSize(initialNumAddedRoles - 1);
    assertAddRoleInitialStateIsCorrect(APPLICATION_ROLES.size() - initialNumAddedRoles + 2, accessEditorPage);
    assertThat(getMembershipMappings(currentOwner.getId(), role.getName())).isEmpty();
  }

  @Test
  public void testRemove() {
    Role role = APPLICATION_ROLES.get(2);
    refreshOrOpen(AccessEditorPage.urlToEdit(currentOwner, role.getId()));

    AccessEditorPage accessEditorPage = new AccessEditorPage();
    AccessEditorPage.AddMembersForm addMembersForm = accessEditorPage.addMembersForm();

    accessEditorPage.title().shouldBe(visible);
    OwnerDetailSidebar.accessGroup().entryItems().shouldHave(sizeGreaterThan(0));
    int initialNumAddedRoles = OwnerDetailSidebar.accessGroup().entryItems().size();

    addMembersForm.deleteRoleButton().click();
    NxDeleteModal deleteModal = addMembersForm.getDeleteModal();
    deleteModal.shouldBe(visible);
    deleteModal.alertContent()
            .shouldHave(text(addMembersForm.confirmRemovalText(role.getName(), currentOwner.getType())));
    deleteModal.header().shouldHave(AccessEditorPage.CONFIRM_REMOVAL_HEADER_TEXT);
    deleteModal.submitButton().click();
    FormMask.seeAndWaitForDismissal();
    deleteModal.shouldBe(hidden);
    OwnerDetailSidebar.accessGroup().entryItems().shouldHaveSize(initialNumAddedRoles - 1);
    assertAddRoleInitialStateIsCorrect(APPLICATION_ROLES.size() - initialNumAddedRoles + 2, accessEditorPage);
    assertThat(getMembershipMappings(currentOwner.getId(), role.getName())).isEmpty();
  }

  @Test
  public void testDisabledGroupSearchWarning() {
    // start with two LDAP servers, both with dynamic group search disabled
    String ldapServerId1 = tempEntity.newLdapServer("LDAP_1").getId();
    tempEntity.newLdapConnection(ldapServerId1);

    LdapUserMapping ldapUserMapping1 = tempEntity.newLdapUserMapping(ldapServerId1);
    ldapUserMapping1.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping1.setDynamicGroupSearchEnabled(false);
    new LdapUserMappingDAO().update(ldapUserMapping1);

    String ldapServerId2 = tempEntity.newLdapServer("LDAP_2").getId();
    tempEntity.newLdapConnection(ldapServerId2);

    LdapUserMapping ldapUserMapping2 = tempEntity.newLdapUserMapping(ldapServerId2);
    ldapUserMapping2.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping2.setDynamicGroupSearchEnabled(false);
    new LdapUserMappingDAO().update(ldapUserMapping2);

    refresh(); // reload because UI data is cached
    goFromSummaryToAddRole();

    AccessEditorPage accessEditorPage = new AccessEditorPage();
    AccessEditorPage.AddMembersForm addMembersForm = accessEditorPage.addMembersForm();
    addMembersForm.searchBox().shouldBe(visible);

    addMembersForm.disabledGroupSearchWarning().shouldBe(visible)
      .shouldHave(text(DISABLED_GROUP_SEARCH_WARNING));
    eyesWatcher.eyesCheck();

    // enable group search for one
    ldapUserMapping1.setDynamicGroupSearchEnabled(true);
    new LdapUserMappingDAO().update(ldapUserMapping1);
    refresh();

    addMembersForm.disabledGroupSearchWarning().shouldBe(visible)
      .shouldHave(text(DISABLED_GROUP_SEARCH_WARNING));

    // ... and then the other one, too
    ldapUserMapping2.setDynamicGroupSearchEnabled(true);
    new LdapUserMappingDAO().update(ldapUserMapping2);
    refresh();

    addMembersForm.disabledGroupSearchWarning().shouldNot(exist);
  }

  private void assertThatRoleNotAvailableInDropdown(
          final String roleName, AccessEditorPage.AddMembersForm addMembersForm)
  {
    addMembersForm.roleSelect().click();
    List<String> roleNames = addMembersForm.roleSelect().listItems().texts();
    assertThat(roleNames).doesNotContain(roleName);
  }

  private void assertAddRoleInitialStateIsCorrect(
          int numAvailableRoles, AccessEditorPage accessEditorPage)
  {
    OwnerDetailSidebar.accessGroup().item(0).shouldBe(CLM.SELECTED);
    accessEditorPage.title().shouldHave(AccessEditorPage.NEW_TITLE_TEXT);
    accessEditorPage.addMembersForm().roleSelect().listItems().shouldHaveSize(numAvailableRoles);
    accessEditorPage.addMembersForm().deleteRoleButton().shouldBe(hidden);
    assertCommonInitialStateIsCorrect(accessEditorPage);
  }

  private void assertCommonInitialStateIsCorrect(AccessEditorPage accessEditorPage) {
    accessEditorPage.addMembersForm().searchBox().shouldHave(value(""));
    accessEditorPage.addMembersForm().searchResults().shouldHaveSize(0);
    accessEditorPage.addMembersForm().saveButton().shouldHave(DISABLED);
  }

  protected List<MembershipMapping> getMembershipMappings(final String ownerId, final String roleName) {
    return membershipMappingDAO.getByContextIdAndRoleId(ownerId, roleDAO.getByName(roleName).getId());
  }

  abstract void goFromSummaryToAddRole();

  abstract void goFromSummaryToEditRole(final Role role); // TODO remove after repository config tile lands
}
