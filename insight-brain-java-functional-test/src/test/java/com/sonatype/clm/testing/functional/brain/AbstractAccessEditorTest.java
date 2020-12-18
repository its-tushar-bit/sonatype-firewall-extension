/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.DoubleColumnPicker;
import com.sonatype.clm.testing.functional.elements.DoubleColumnPicker.Item;
import com.sonatype.clm.testing.functional.elements.Dropdown;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.OwnerDetailTreeView;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.AccessEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.utils.DoubleColumnPickerTestHelper;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.security.Group;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.elements.CLM.PRISTINE;
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
  public void testAddRole() {
    goFromSummaryToAddRole();
    assertAddRoleInitialStateIsCorrect(APPLICATION_ROLES.size() - 2);
    OwnerDetailTreeView.accessGroup().items().shouldHaveSize(3);

    Dropdown roleDropdown = AccessEditorPage.roleDropdown();
    roleDropdown.selectedItem().shouldHave(AccessEditorPage.DROPDOWN_DEFAULT_TEXT).click();
    eyesWatcher.eyesCheck();
    String roleName = roleDropdown.listItem(2).text();
    assertThat(getMembershipMappings(currentOwner.getId(), roleName)).isEmpty();
    roleDropdown.listItem(2).click();
    AccessEditorPage.saveButton().shouldHave(DISABLED);
    AccessEditorPage.searchButton().shouldHave(DISABLED);
    AccessEditorPage.searchBox().val("*");
    AccessEditorPage.searchButton().shouldBe(enabled).click();

    DoubleColumnPicker picker = AccessEditorPage.picker();
    picker.availableItems().shouldHaveSize(4);

    Item availableItem = picker.availableItem(0);
    availableItem.label().shouldHave(text("Admin Builtin")).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("IQ Server admin@localhost"));
    AccessEditorPage.title().hover(); // hide the tooltip
    Tooltip.get().shouldNot(exist);
    picker.availableItem(1).label().shouldHave(text(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME));

    picker.checkAllLeft().click();
    AccessEditorPage.saveButton().shouldHave(DISABLED);
    picker.pickCheckedItemsButton().click();
    AccessEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    OwnerDetailTreeView.accessGroup().items().shouldHaveSize(4);
    OwnerDetailTreeView.accessGroup().item(3).shouldHave(text(roleName));
    assertAddRoleInitialStateIsCorrect(APPLICATION_ROLES.size() - 3);
    assertThatRoleNotAvailableInDropdown(roleName);
    List<MembershipMapping> membershipMappings = getMembershipMappings(currentOwner.getId(), roleName);
    tempEntity.register(membershipMappings.toArray(new MembershipMapping[membershipMappings.size()]));
    assertThat(membershipMappings).hasSize(4);
  }

  @Test
  public void testEdit() {
    Role role = APPLICATION_ROLES.get(0);
    goFromSummaryToEditRole(role);

    OwnerDetailTreeView.accessGroup().item(1).shouldBe(CLM.SELECTED);
    AccessEditorPage.title().shouldHave(text(role.getName()));

    DoubleColumnPicker picker = AccessEditorPage.picker();
    assertCommonInitialStateIsCorrect(picker);
    DoubleColumnPickerTestHelper.assertDoubleColumnPickerDefaultState(picker, 0, 2, false);
    AccessEditorPage.removeRoleButton().shouldBe(visible);

    AccessEditorPage.searchBox().val("*");
    AccessEditorPage.searchButton().shouldBe(enabled).click();

    picker.availableItems().shouldHaveSize(2);
    Item availableItem = picker.availableItem(0);
    availableItem.label().shouldHave(text("Admin Builtin")).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("IQ Server admin@localhost"));
    AccessEditorPage.title().hover(); // hide the tooltip
    Tooltip.get().shouldNot(exist);
    picker.availableItem(1).label().shouldHave(text(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME));
    picker.checkAllLeft().click();
    picker.pickCheckedItemsButton().shouldBe(enabled).click();
    picker.pickedItems().shouldHaveSize(4);
    // shouldn't submit on enter. Assert by checking that 'save' button is still enabled
    AccessEditorPage.searchBox().pressEnter();
    FormMask.seeAndWaitForDismissal();
    AccessEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    assertCommonInitialStateIsCorrect(picker);
    List<MembershipMapping> membershipMappings = getMembershipMappings(currentOwner.getId(), role.getName());
    tempEntity.register(membershipMappings.toArray(new MembershipMapping[membershipMappings.size()]));
    picker.pickedItems().shouldHaveSize(4);
    assertThat(getMembershipMappings(currentOwner.getId(), role.getName())).hasSize(4);
  }

  @Test
  public void testRemoveBySavingWithNoPickedUsers() {
    Role role = APPLICATION_ROLES.get(2);
    refreshOrOpen(AccessEditorPage.urlToEdit(currentOwner, role.getId()));

    DoubleColumnPicker picker = AccessEditorPage.picker();
    picker.pickedItems().shouldHaveSize(1);

    AccessEditorPage.title().hover(); // hide the tooltip
    OwnerDetailTreeView.accessGroup().entryItems().shouldHave(sizeGreaterThan(0));
    int initialNumAddedRoles = OwnerDetailTreeView.accessGroup().entryItems().size();

    picker.checkAllRight().click();
    picker.unpickCheckedItemsButton().click();
    AccessEditorPage.saveButton().shouldNotHave(DISABLED).click();
    DeleteModal.body().shouldBe(visible).shouldHave(
        AccessEditorPage.confirmRemovalThroughUpdateText(role.getName(), currentOwner.getType()));
    DeleteModal.header().shouldHave(AccessEditorPage.CONFIRM_REMOVAL_HEADER_TEXT);
    DeleteModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    DeleteModal.body().shouldBe(hidden);
    OwnerDetailTreeView.accessGroup().entryItems().shouldHaveSize(initialNumAddedRoles - 1);
    assertAddRoleInitialStateIsCorrect(APPLICATION_ROLES.size() - initialNumAddedRoles + 1);
    assertThat(getMembershipMappings(currentOwner.getId(), role.getName())).isEmpty();
  }

  @Test
  public void testRemove() {
    Role role = APPLICATION_ROLES.get(2);
    refreshOrOpen(AccessEditorPage.urlToEdit(currentOwner, role.getId()));
    AccessEditorPage.title().shouldBe(visible).hover(); // hide the tooltip
    OwnerDetailTreeView.accessGroup().entryItems().shouldHave(sizeGreaterThan(0));
    int initialNumAddedRoles = OwnerDetailTreeView.accessGroup().entryItems().size();
    AccessEditorPage.removeRoleButton().click();
    DeleteModal.body().shouldBe(visible)
        .shouldHave(AccessEditorPage.confirmRemovalText(role.getName(), currentOwner.getType()));
    DeleteModal.header().shouldHave(AccessEditorPage.CONFIRM_REMOVAL_HEADER_TEXT);
    DeleteModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    DeleteModal.body().shouldBe(hidden);
    OwnerDetailTreeView.accessGroup().entryItems().shouldHaveSize(initialNumAddedRoles - 1);
    assertAddRoleInitialStateIsCorrect(APPLICATION_ROLES.size() - initialNumAddedRoles + 1);
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

    AccessEditorPage.searchBox().shouldBe(visible);
    AccessEditorPage.disabledGroupSearchWarning().shouldBe(visible).shouldHave(text(DISABLED_GROUP_SEARCH_WARNING));
    eyesWatcher.eyesCheck();

    // enable group search for one
    ldapUserMapping1.setDynamicGroupSearchEnabled(true);
    new LdapUserMappingDAO().update(ldapUserMapping1);
    refresh();

    AccessEditorPage.disabledGroupSearchWarning().shouldBe(visible).shouldHave(text(DISABLED_GROUP_SEARCH_WARNING));

    // ... and then the other one, too
    ldapUserMapping2.setDynamicGroupSearchEnabled(true);
    new LdapUserMappingDAO().update(ldapUserMapping2);
    refresh();

    AccessEditorPage.disabledGroupSearchWarning().shouldNot(exist);
  }

  private void assertThatRoleNotAvailableInDropdown(final String roleName) {
    AccessEditorPage.roleDropdown().selectedItem().click();
    List<String> roleNames = AccessEditorPage.roleDropdown().listItems().texts();
    assertThat(roleNames).doesNotContain(roleName);
  }

  private void assertAddRoleInitialStateIsCorrect(int numAvailableRoles) {
    OwnerDetailTreeView.accessGroup().item(0).shouldBe(CLM.SELECTED);
    AccessEditorPage.title().shouldHave(AccessEditorPage.NEW_TITLE_TEXT);
    AccessEditorPage.roleDropdown().listItems().shouldHaveSize(numAvailableRoles);
    DoubleColumnPickerTestHelper.assertDoubleColumnPickerDefaultState(AccessEditorPage.picker(), 0, false);
    AccessEditorPage.removeRoleButton().shouldBe(hidden);
    assertCommonInitialStateIsCorrect(AccessEditorPage.picker());
  }

  private void assertCommonInitialStateIsCorrect(DoubleColumnPicker picker) {
    AccessEditorPage.searchBox().shouldHave(value("")).shouldHave(PRISTINE);
    AccessEditorPage.searchButton().shouldHave(DISABLED);
    picker.availableItemList().shouldBe(empty);
    AccessEditorPage.saveButton().shouldHave(DISABLED);
  }

  protected List<MembershipMapping> getMembershipMappings(final String ownerId, final String roleName) {
    return membershipMappingDAO.getByContextIdAndRoleId(ownerId, roleDAO.getByName(roleName).getId());
  }

  abstract void goFromSummaryToAddRole();

  abstract void goFromSummaryToEditRole(final Role role); // TODO remove after repository config tile lands
}
