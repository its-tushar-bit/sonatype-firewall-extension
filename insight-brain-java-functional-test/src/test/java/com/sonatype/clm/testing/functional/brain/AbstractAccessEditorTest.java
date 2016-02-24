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
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.OwnerDetailTreeView;
import com.sonatype.clm.testing.functional.pages.AccessEditorPage;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.utils.DoubleColumnPickerTestHelper;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.elements.CLM.INITIAL_VALUE;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public abstract class AbstractAccessEditorTest
    extends AbstractFunctionalTest
{
  private static final List<Role> APPLICATION_ROLES = new RoleDAO().getApplicationRoles();

  private final MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();

  private final RoleDAO roleDAO = new RoleDAO();

  private Owner currentOwner;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OrganizationManagementPage.URL);
    loginAsAdmin();
  }

  protected void init(Owner owner) {
    this.currentOwner = owner;
    open(OwnerSummaryPage.url(owner.getType().toString(), owner.getPublicId()));
  }

  @Test
  public void testAddRole() {
    goFromSummaryToAddRole();
    assertAddRoleInitialStateIsCorrect(APPLICATION_ROLES.size());
    OwnerDetailTreeView.accessGroup().items().shouldHaveSize(2);

    AccessEditorPage.roleDropdown().selectedItem().shouldHave(AccessEditorPage.DROPDOWN_DEFAULT_TEXT).click();
    String roleName = AccessEditorPage.roleDropdown().listItem(1).text();
    assertThat(getMembershipMappings(currentOwner.getId(), roleName), is(empty()));
    AccessEditorPage.roleDropdown().listItem(1).click();
    AccessEditorPage.saveButton().shouldHave(DISABLED);
    AccessEditorPage.searchButton().shouldHave(DISABLED);
    AccessEditorPage.searchBox().val("*");
    AccessEditorPage.searchButton().shouldBe(enabled).click();
    DoubleColumnPicker.availableItems().shouldHaveSize(1);
    DoubleColumnPicker.availableItem(0).root().hover();
    DoubleColumnPicker.availableItem(0).tooltip().shouldBe(visible).shouldHave(text("CLM admin@localhost"));
    AccessEditorPage.title().hover(); // hide the tooltip
    DoubleColumnPicker.availableItem(0).tooltip().shouldNot(exist);

    DoubleColumnPicker.checkAllLeft().click();
    AccessEditorPage.saveButton().shouldHave(DISABLED);
    DoubleColumnPicker.pickCheckedItemsButton().click();
    AccessEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    OwnerDetailTreeView.accessGroup().items().shouldHaveSize(3);
    OwnerDetailTreeView.accessGroup().item(2).root().shouldHave(text(roleName));
    assertAddRoleInitialStateIsCorrect(APPLICATION_ROLES.size() - 1);
    assertThatRoleNotAvailableInDropdown(roleName);
    List<MembershipMapping> membershipMappings = getMembershipMappings(currentOwner.getId(), roleName);
    tempEntity.register(membershipMappings.toArray(new MembershipMapping[membershipMappings.size()]));
    assertThat(membershipMappings, hasSize(1));
  }

  @Test
  public void testEdit() {
    User u1 = tempEntity.newUser();
    User u2 = tempEntity.newUser();
    Role role = APPLICATION_ROLES.get(0);
    tempEntity.newMembershipMapping(currentOwner.getId(), role.getId(), u1.getUsername());
    tempEntity.newMembershipMapping(currentOwner.getId(), role.getId(), u2.getUsername());
    assertThat(getMembershipMappings(currentOwner.getId(), role.getName()), hasSize(2));
    goFromSummaryToEditRole(role);

    waitUntilUrl(AccessEditorPage.urlToEdit(currentOwner.getType().toString(), currentOwner.getPublicId(), role.getId()));
    OwnerDetailTreeView.accessGroup().item(2).root().shouldBe(CLM.SELECTED);
    AccessEditorPage.title().shouldHave(text(role.getName()));
    assertCommonInitialStateIsCorrect();
    DoubleColumnPickerTestHelper.assertDoubleColumnPickerDefaultState(0, 2, false);
    AccessEditorPage.removeRoleButton().shouldBe(visible);

    AccessEditorPage.searchBox().val("*");
    AccessEditorPage.searchButton().shouldBe(enabled).click();
    DoubleColumnPicker.availableItems().shouldHaveSize(1);
    DoubleColumnPicker.availableItem(0).root().hover();
    DoubleColumnPicker.availableItem(0).tooltip().shouldBe(visible).shouldHave(text("CLM admin@localhost"));
    AccessEditorPage.title().hover(); // hide the tooltip
    DoubleColumnPicker.availableItem(0).tooltip().shouldNot(exist);
    DoubleColumnPicker.checkAllLeft().click();
    DoubleColumnPicker.pickCheckedItemsButton().shouldBe(enabled).click();
    DoubleColumnPicker.pickedItems().shouldHaveSize(3);
    // shouldn't submit on enter. Assert by checking that 'save' button is still enabled
    AccessEditorPage.searchBox().pressEnter();
    AccessEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    assertCommonInitialStateIsCorrect();
    List<MembershipMapping> membershipMappings = getMembershipMappings(currentOwner.getId(), role.getName());
    tempEntity.register(membershipMappings.toArray(new MembershipMapping[membershipMappings.size()]));
    DoubleColumnPicker.pickedItems().shouldHaveSize(3);
    assertThat(getMembershipMappings(currentOwner.getId(), role.getName()), hasSize(3));
  }

  @Test
  public void testRemoveBySavingWithNoPickedUsers() {
    User u1 = tempEntity.newUser();
    Role role = APPLICATION_ROLES.get(2);
    tempEntity.newMembershipMapping(currentOwner.getId(), role.getId(), u1.getUsername());
    assertThat(getMembershipMappings(currentOwner.getId(), role.getName()), hasSize(1));
    open(AccessEditorPage.urlToEdit(currentOwner.getType().toString(), currentOwner.getPublicId(), role.getId()));
    AccessEditorPage.title().hover(); // hide the tooltip
    int initialNumAddedRoles = OwnerDetailTreeView.accessGroup().entryItems().size();
    DoubleColumnPicker.checkAllRight().click();
    DoubleColumnPicker.unpickCheckedItemsButton().click();
    AccessEditorPage.saveButton().shouldNotHave(DISABLED).click();
    DeleteModal.body().shouldBe(visible);
    DeleteModal.body().shouldHave(AccessEditorPage.confirmRemovalThroughUpdateText(role.getName(), currentOwner.getType().toString()));
    DeleteModal.header().shouldHave(AccessEditorPage.CONFIRM_REMOVAL_HEADER_TEXT);
    DeleteModal.continueButton().click();
    DeleteModal.body().shouldNotBe(visible);
    OwnerDetailTreeView.accessGroup().entryItems().shouldHaveSize(initialNumAddedRoles - 1);
    assertAddRoleInitialStateIsCorrect(APPLICATION_ROLES.size() - initialNumAddedRoles + 1);
    assertThat(getMembershipMappings(currentOwner.getId(), role.getName()), is(empty()));
  }

  @Test
  public void testRemove() {
    User u1 = tempEntity.newUser();
    Role role = APPLICATION_ROLES.get(2);
    tempEntity.newMembershipMapping(currentOwner.getId(), role.getId(), u1.getUsername());
    assertThat(getMembershipMappings(currentOwner.getId(), role.getName()), hasSize(1));
    open(AccessEditorPage.urlToEdit(currentOwner.getType().toString(), currentOwner.getPublicId(), role.getId()));
    AccessEditorPage.title().hover(); // hide the tooltip
    int initialNumAddedRoles = OwnerDetailTreeView.accessGroup().entryItems().size();
    AccessEditorPage.removeRoleButton().click();
    DeleteModal.body().shouldBe(visible);
    DeleteModal.body().shouldHave(AccessEditorPage.confirmRemovalText(role.getName(), currentOwner.getType().toString()));
    DeleteModal.header().shouldHave(AccessEditorPage.CONFIRM_REMOVAL_HEADER_TEXT);
    DeleteModal.continueButton().click();
    DeleteModal.body().shouldNotBe(visible);
    OwnerDetailTreeView.accessGroup().entryItems().shouldHaveSize(initialNumAddedRoles - 1);
    assertAddRoleInitialStateIsCorrect(APPLICATION_ROLES.size() - initialNumAddedRoles + 1);
    assertThat(getMembershipMappings(currentOwner.getId(), role.getName()), is(empty()));
  }

  private void assertThatRoleNotAvailableInDropdown(final String roleName) {
    AccessEditorPage.roleDropdown().selectedItem().click();
    String[] roleNames = AccessEditorPage.roleDropdown().listItems().getTexts();
    assertThat(asList(roleNames), not(contains(roleName)));
  }

  private void assertAddRoleInitialStateIsCorrect(int numAvailableRoles) {
    OwnerDetailTreeView.accessGroup().item(1).root().shouldBe(CLM.SELECTED);
    AccessEditorPage.title().shouldHave(AccessEditorPage.NEW_TITLE_TEXT);
    AccessEditorPage.roleDropdown().listItems().shouldHaveSize(numAvailableRoles);
    DoubleColumnPickerTestHelper.assertDoubleColumnPickerDefaultState(0, false);
    AccessEditorPage.removeRoleButton().shouldNotBe(visible);
    assertCommonInitialStateIsCorrect();
  }

  private void assertCommonInitialStateIsCorrect() {
    AccessEditorPage.searchBox().shouldHave(value("")).shouldHave(INITIAL_VALUE);
    AccessEditorPage.searchButton().shouldHave(DISABLED);
    DoubleColumnPicker.availableItemList().shouldBe(empty);
    AccessEditorPage.saveButton().shouldHave(DISABLED);
  }

  private List<MembershipMapping> getMembershipMappings(final String ownersId, final String roleName) {
    return membershipMappingDAO.getByContextIdAndRoleId(ownersId, roleDAO.getByName(roleName).getId());
  }

  abstract void goFromSummaryToAddRole();

  abstract void goFromSummaryToEditRole(final Role role); // TODO remove after repository config tile lands
}
