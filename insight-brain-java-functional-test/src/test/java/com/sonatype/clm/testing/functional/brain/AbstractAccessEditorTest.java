/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.List;
import java.util.stream.Stream;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.elements.OwnerDetailSidebar;
import com.sonatype.clm.testing.functional.elements.Tooltip;
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.pages.AccessEditorPage.DISABLED_GROUP_SEARCH_WARNING;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractAccessEditorTest
    extends AbstractFunctionalTest
{
  private List<Role> applicationRoles;

  protected MembershipMappingDAO membershipMappingDAO;

  protected RoleDAO roleDAO;

  protected LdapUserMappingDAO ldapUserMappingDAO;

  protected Owner currentOwner;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    membershipMappingDAO = lookup(MembershipMappingDAO.class);
    roleDAO = lookup(RoleDAO.class);
    ldapUserMappingDAO = lookup(LdapUserMappingDAO.class);
    applicationRoles = Stream.of(Role.APPLICATION_EVALUATOR_ROLE_ID,
        Role.COMPONENT_EVALUATOR_ROLE_ID,
        Role.DEVELOPER_ROLE_ID,
        Role.LEGAL_REVIEWER_ROLE_ID,
        Role.OWNER_ROLE_ID).map(roleDAO::getById).toList();
  }

  protected void init(Owner owner) {
    this.currentOwner = owner;

    User u1 = tempEntity.newUser();
    User u2 = tempEntity.newUser();
    Role role = applicationRoles.get(0);
    tempEntity.newMembershipMapping(currentOwner.getId(), role.getId(), u1.getUsername());
    tempEntity.newMembershipMapping(currentOwner.getId(), role.getId(), u2.getUsername());

    role = applicationRoles.get(2);
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
    OwnerDetailSidebar.accessGroup().items().shouldHave(size(3));

    AccessEditorPage accessEditorPage = new AccessEditorPage();
    AccessEditorPage.AddMembersForm addMembersForm = accessEditorPage.addMembersForm();

    assertAddRoleInitialStateIsCorrect(applicationRoles.size() - 1, accessEditorPage);

    NxFormSelect roleDropdown = addMembersForm.roleSelect();
    roleDropdown.shouldHave(AccessEditorPage.DROPDOWN_DEFAULT_TEXT).click();
    String roleName = roleDropdown.listItem(2).text();
    assertThat(getMembershipMappings(currentOwner.getId(), roleName)).isEmpty();
    roleDropdown.listItem(2).click();
    addMembersForm.searchBox().setValue("*").click();

    addMembersForm.searchResults().shouldHave(size(4));
    addMembersForm.searchResults()
        .shouldHave(texts("Admin BuiltIn", "John Doe", "John Doe", "Authenticated Users (Group)"));

    addMembersForm.searchResults().get(0).shouldHave(text("Admin BuiltIn"));
    addMembersForm.searchResults().get(0).click();
    addMembersForm.addedItems().shouldHave(size(1));

    addMembersForm.searchBox().setValue("*").click();
    addMembersForm.searchResults().get(0).click();
    addMembersForm.addedItems().shouldHave(size(2));

    addMembersForm.searchBox().setValue("*").click();
    addMembersForm.searchResults().get(0).click();
    addMembersForm.addedItems().shouldHave(size(3));

    addMembersForm.searchBox().setValue("*").click();
    addMembersForm.searchResults().get(0).shouldHave(text("Authenticated Users (Group)"));
    addMembersForm.searchResults().get(0).click();

    addMembersForm.addedItems().shouldHave(size(4));
    addMembersForm.addedItems()
        .shouldHave(texts("Admin BuiltIn", "Authenticated Users (Group)", "John Doe", "John Doe"));

    addMembersForm.saveButton().shouldNotHave(cssClass("disabled")).click();
    FormMask.seeAndWaitForDismissal();

    OwnerDetailSidebar.accessGroup().items().shouldHave(size(4));
    OwnerDetailSidebar.accessGroup().item(3).shouldHave(text(roleName));
    assertAddRoleInitialStateIsCorrect(applicationRoles.size() - 2, accessEditorPage);
    assertThatRoleNotAvailableInDropdown(roleName, addMembersForm);
    List<MembershipMapping> membershipMappings = getMembershipMappings(currentOwner.getId(), roleName);
    assertThat(membershipMappings).hasSize(4);
  }

  @Test
  public void testEdit() {
    Role role = applicationRoles.get(0);
    goFromSummaryToEditRole(role);

    OwnerDetailSidebar.accessGroup().item(1).shouldBe(CLM.SELECTED);

    AccessEditorPage accessEditorPage = new AccessEditorPage();
    AccessEditorPage.AddMembersForm addMembersForm = accessEditorPage.addMembersForm();
    accessEditorPage.title().shouldHave(text(role.getName()));
    assertCommonInitialStateIsCorrect(accessEditorPage);

    addMembersForm.searchBox().setValue("*").click();
    addMembersForm.searchResults().shouldHave(size(2));

    addMembersForm.searchResults().get(0).shouldHave(text("Admin BuiltIn"));
    addMembersForm.searchResults().get(1).shouldHave(text("Authenticated Users (Group)"));

    addMembersForm.searchResults().get(0).click();
    addMembersForm.addedItems().shouldHave(size(3));

    addMembersForm.searchBox().setValue("*").click();
    addMembersForm.searchResults().get(0).click();

    addMembersForm.addedItems().shouldHave(size(4));
    addMembersForm.addedItems()
        .shouldHave(texts("Admin BuiltIn", "Authenticated Users (Group)", "John Doe", "John Doe"));

    addMembersForm.saveButton().shouldNotHave(cssClass("disabled")).click();
    FormMask.seeAndWaitForDismissal();
    assertCommonInitialStateIsCorrect(accessEditorPage);
    assertThat(getMembershipMappings(currentOwner.getId(), role.getName())).hasSize(4);
  }

  @Test
  public void testRemoveBySavingWithNoPickedUsers() {
    Role role = applicationRoles.get(2);
    refreshOrOpen(AccessEditorPage.urlToEdit(currentOwner, role.getId()));

    AccessEditorPage accessEditorPage = new AccessEditorPage();
    AccessEditorPage.AddMembersForm addMembersForm = accessEditorPage.addMembersForm();
    addMembersForm.addedItems().shouldHave(size(1));

    OwnerDetailSidebar.accessGroup().entryItems().shouldHave(sizeGreaterThan(0));
    int initialNumAddedRoles = OwnerDetailSidebar.accessGroup().entryItems().size();

    addMembersForm.addedItems().get(0).click();
    addMembersForm.saveButton().shouldNotHave(cssClass("disabled")).click();
    NxDeleteModal deleteModal = addMembersForm.getDeleteModal();
    deleteModal.alertContent()
        .shouldBe(visible)
        .shouldHave(
            text(addMembersForm.confirmRemovalThroughUpdateText(role.getName(), currentOwner.getType())));
    deleteModal.header().shouldHave(AccessEditorPage.CONFIRM_REMOVAL_HEADER_TEXT);
    deleteModal.submitButton().click();
    FormMask.seeAndWaitForDismissal();
    deleteModal.shouldBe(hidden);
    OwnerDetailSidebar.accessGroup().entryItems().shouldHave(size(initialNumAddedRoles - 1));
    assertAddRoleInitialStateIsCorrect(applicationRoles.size() - initialNumAddedRoles + 2, accessEditorPage);
    assertThat(getMembershipMappings(currentOwner.getId(), role.getName())).isEmpty();
  }

  @Test
  public void testRemove() {
    Role role = applicationRoles.get(2);
    refreshOrOpen(AccessEditorPage.urlToEdit(currentOwner, role.getId()));

    AccessEditorPage accessEditorPage = new AccessEditorPage();
    accessEditorPage.title().shouldHave(text(role.getName()));
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
    OwnerDetailSidebar.accessGroup().entryItems().shouldHave(size(initialNumAddedRoles - 1));
    assertAddRoleInitialStateIsCorrect(applicationRoles.size() - initialNumAddedRoles + 2, accessEditorPage);
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
    ldapUserMappingDAO.update(ldapUserMapping1);

    String ldapServerId2 = tempEntity.newLdapServer("LDAP_2").getId();
    tempEntity.newLdapConnection(ldapServerId2);

    LdapUserMapping ldapUserMapping2 = tempEntity.newLdapUserMapping(ldapServerId2);
    ldapUserMapping2.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping2.setDynamicGroupSearchEnabled(false);
    ldapUserMappingDAO.update(ldapUserMapping2);

    refresh(); // reload because UI data is cached
    goFromSummaryToAddRole();

    AccessEditorPage accessEditorPage = new AccessEditorPage();
    AccessEditorPage.AddMembersForm addMembersForm = accessEditorPage.addMembersForm();
    addMembersForm.searchBox().shouldBe(visible);

    addMembersForm.disabledGroupSearchWarning()
        .shouldBe(visible)
        .shouldHave(text(DISABLED_GROUP_SEARCH_WARNING));

    // enable group search for one
    ldapUserMapping1.setDynamicGroupSearchEnabled(true);
    ldapUserMappingDAO.update(ldapUserMapping1);
    refresh();

    addMembersForm.disabledGroupSearchWarning()
        .shouldBe(visible)
        .shouldHave(text(DISABLED_GROUP_SEARCH_WARNING));

    // ... and then the other one, too
    ldapUserMapping2.setDynamicGroupSearchEnabled(true);
    ldapUserMappingDAO.update(ldapUserMapping2);
    refresh();

    addMembersForm.disabledGroupSearchWarning().shouldNot(exist);
  }

  private void assertThatRoleNotAvailableInDropdown(
      final String roleName,
      AccessEditorPage.AddMembersForm addMembersForm)
  {
    addMembersForm.roleSelect().click();
    List<String> roleNames = addMembersForm.roleSelect().listItems().texts();
    assertThat(roleNames).doesNotContain(roleName);
  }

  private void assertAddRoleInitialStateIsCorrect(
      int numAvailableRoles,
      AccessEditorPage accessEditorPage)
  {
    OwnerDetailSidebar.accessGroup().item(0).shouldBe(CLM.SELECTED);
    accessEditorPage.title().shouldHave(AccessEditorPage.NEW_TITLE_TEXT);
    accessEditorPage.addMembersForm().roleSelect().listItems().shouldHave(size(numAvailableRoles));
    accessEditorPage.addMembersForm().deleteRoleButton().shouldBe(hidden);
    assertCommonInitialStateIsCorrect(accessEditorPage);
  }

  private void assertCommonInitialStateIsCorrect(AccessEditorPage accessEditorPage) {
    accessEditorPage.addMembersForm().searchBox().shouldHave(value(""));
    accessEditorPage.addMembersForm().searchResults().shouldHave(size(0));
  }

  protected List<MembershipMapping> getMembershipMappings(final String ownerId, final String roleName) {
    return membershipMappingDAO.getByContextIdAndRoleId(ownerId, roleDAO.getByName(roleName).getId());
  }

  abstract void goFromSummaryToAddRole();

  abstract void goFromSummaryToEditRole(final Role role); // TODO remove after repository config tile lands
}
