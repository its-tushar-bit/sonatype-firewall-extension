/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.DoubleColumnPicker;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.OwnerDetailTreeView;
import com.sonatype.clm.testing.functional.pages.AccessEditorPage;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage.SummaryTile;
import com.sonatype.clm.testing.functional.utils.DoubleColumnPickerTestHelper;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;
import static com.sonatype.clm.testing.functional.elements.CLM.INITIAL_VALUE_CLASS;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED_CLASS;
import static com.sonatype.clm.testing.functional.elements.OwnerDetailTreeView.OwnerDetailTreeViewGroup.OwnerDetailTreeViewItem.SELECTED_CLASS;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class AccessEditorTest
    extends AbstractFunctionalTest
{
  private static final List<Role> APPLICATION_ROLES = new RoleDAO().getApplicationRoles();

  private final MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();

  private final RoleDAO roleDAO = new RoleDAO();

  private Organization organization;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OrganizationManagementPage.URL);
    loginAsAdmin();
  }

  @Before
  public void init() {
    organization = tempEntity.newOrganization();
    refreshOrOpen(OwnerSummaryPage.url("organization", organization.getId()));
    refresh(); // TODO remove after CLM-5827
  }

  @Test
  public void testAddRole() {
    SummaryTile.accessButton().click();
    SummaryTile.addRoleButton().click();
    assertAddRoleInitialStateIsCorrect(APPLICATION_ROLES.size(), AccessEditorPage.urlToCreate("organization", organization.getId()));
    OwnerDetailTreeView.accessGroup().items().shouldHaveSize(2);

    AccessEditorPage.roleDropdown().selectedItem().shouldHave(AccessEditorPage.DROPDOWN_DEFAULT_TEXT).click();
    String roleName = AccessEditorPage.roleDropdown().listItem(1).text();
    assertThat(getMembershipMappings(organization.getId(), roleName), is(empty()));
    AccessEditorPage.roleDropdown().listItem(1).click();
    AccessEditorPage.saveButton().shouldHave(DISABLED_CLASS);
    AccessEditorPage.searchButton().shouldHave(DISABLED_CLASS);
    AccessEditorPage.searchBox().val("*");
    AccessEditorPage.searchButton().shouldBe(enabled).click();
    DoubleColumnPicker.availableItems().shouldHaveSize(1);
    DoubleColumnPicker.checkAllLeft().click();
    AccessEditorPage.saveButton().shouldHave(DISABLED_CLASS);
    DoubleColumnPicker.pickCheckedItemsButton().click();
    AccessEditorPage.saveButton().shouldNotHave(DISABLED_CLASS).click();
    FormMask.root().shouldBe(visible).shouldNotBe(visible);
    OwnerDetailTreeView.accessGroup().items().shouldHaveSize(3);
    OwnerDetailTreeView.accessGroup().item(2).root().shouldHave(text(roleName));
    assertAddRoleInitialStateIsCorrect(APPLICATION_ROLES.size() - 1, AccessEditorPage.urlToCreate("organization", organization.getId()));
    assertThatRoleNotAvailableInDropdown(roleName);
    assertThat(getMembershipMappings(organization.getId(), roleName), hasSize(1));
  }

  @Test
  public void testEdit() {
    User u1 = tempEntity.newUser();
    User u2 = tempEntity.newUser();
    Role role = APPLICATION_ROLES.get(0);
    tempEntity.newMembershipMapping(organization.getId(), role.getId(), u1.getUsername());
    tempEntity.newMembershipMapping(organization.getId(), role.getId(), u2.getUsername());
    assertThat(getMembershipMappings(organization.getId(), role.getName()), hasSize(2));
    refresh();
    OwnerSummaryPage.SummaryTile.localAccessRole(role.getName()).click();

    waitUntilUrl(AccessEditorPage.urlToEdit("organization", organization.getId(), role.getId()));
    OwnerDetailTreeView.accessGroup().item(2).root().shouldHave(SELECTED_CLASS);
    AccessEditorPage.title().shouldHave(text(role.getName()));
    assertCommonInitialStateIsCorrect();
    DoubleColumnPickerTestHelper.assertDoubleColumnPickerDefaultState(0, 2, false);
    AccessEditorPage.removeRoleButton().shouldBe(visible);

    AccessEditorPage.searchBox().val("*");
    AccessEditorPage.searchButton().shouldBe(enabled).click();
    DoubleColumnPicker.availableItems().shouldHaveSize(1);
    DoubleColumnPicker.checkAllLeft().click();
    DoubleColumnPicker.pickCheckedItemsButton().shouldBe(enabled).click();
    DoubleColumnPicker.pickedItems().shouldHaveSize(3);
    // shouldn't submit on enter. Assert by checking that 'save' button is still enabled
    AccessEditorPage.searchBox().pressEnter();
    AccessEditorPage.saveButton().shouldNotHave(DISABLED_CLASS).click();
    FormMask.root().shouldBe(visible).shouldNotBe(visible);
    assertCommonInitialStateIsCorrect();
    DoubleColumnPicker.pickedItems().shouldHaveSize(3);
    assertThat(getMembershipMappings(organization.getId(), role.getName()), hasSize(3));
  }

  @Test
  public void testRemoveBySavingWithNoPickedUsers() {
    Application app = tempEntity.newApplication(organization.getId());
    User u1 = tempEntity.newUser();
    Role role = APPLICATION_ROLES.get(0);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), u1.getUsername());
    assertThat(getMembershipMappings(app.getId(), role.getName()), hasSize(1));
    open(AccessEditorPage.urlToEdit("application", app.getPublicId(), role.getId()));
    refresh(); // TODO remove after CLM-5827
    OwnerDetailTreeView.accessGroup().items().shouldHaveSize(3); // access, add role, current
    DoubleColumnPicker.checkAllRight().click();
    DoubleColumnPicker.unpickCheckedItemsButton().click();
    AccessEditorPage.saveButton().shouldNotHave(DISABLED_CLASS).click();
    DeleteModal.body().shouldBe(visible);
    DeleteModal.body().shouldHave(AccessEditorPage.confirmRemovalThroughUpdateText(role.getName(), "application"));
    DeleteModal.header().shouldHave(AccessEditorPage.CONFIRM_REMOVAL_HEADER_TEXT);
    DeleteModal.continueButton().click();
    DeleteModal.body().shouldNotBe(visible);
    OwnerDetailTreeView.accessGroup().items().shouldHaveSize(2);
    assertAddRoleInitialStateIsCorrect(APPLICATION_ROLES.size(), AccessEditorPage.urlToCreate("application", app.getPublicId()));
    assertThat(getMembershipMappings(organization.getId(), role.getName()), is(empty()));
  }

  @Test
  public void testRemove() {
    Application app = tempEntity.newApplication(organization.getId());
    User u1 = tempEntity.newUser();
    Role role = APPLICATION_ROLES.get(0);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), u1.getUsername());
    assertThat(getMembershipMappings(app.getId(), role.getName()), hasSize(1));
    open(AccessEditorPage.urlToEdit("application", app.getPublicId(), role.getId()));
    refresh(); // TODO remove after CLM-5827
    OwnerDetailTreeView.accessGroup().items().shouldHaveSize(3); // access, add role, current
    AccessEditorPage.removeRoleButton().click();
    DeleteModal.body().shouldBe(visible);
    DeleteModal.body().shouldHave(AccessEditorPage.confirmRemovalText(role.getName(), "application"));
    DeleteModal.header().shouldHave(AccessEditorPage.CONFIRM_REMOVAL_HEADER_TEXT);
    DeleteModal.continueButton().click();
    DeleteModal.body().shouldNotBe(visible);
    OwnerDetailTreeView.accessGroup().items().shouldHaveSize(2);
    assertAddRoleInitialStateIsCorrect(APPLICATION_ROLES.size(), AccessEditorPage.urlToCreate("application", app.getPublicId()));
    assertThat(getMembershipMappings(organization.getId(), role.getName()), is(empty()));
  }

  private void assertThatRoleNotAvailableInDropdown(final String roleName) {
    AccessEditorPage.roleDropdown().selectedItem().click();
    String[] roleNames = AccessEditorPage.roleDropdown().listItems().getTexts();
    assertThat(asList(roleNames), not(contains(roleName)));
  }

  private void assertAddRoleInitialStateIsCorrect(int numAvailableRoles, String url) {
    waitUntilUrl(url);
    OwnerDetailTreeView.accessGroup().item(1).root().shouldHave(SELECTED_CLASS);
    AccessEditorPage.title().shouldHave(AccessEditorPage.NEW_TITLE_TEXT);
    AccessEditorPage.roleDropdown().listItems().shouldHaveSize(numAvailableRoles);
    DoubleColumnPickerTestHelper.assertDoubleColumnPickerDefaultState(0, false);
    AccessEditorPage.removeRoleButton().shouldNotBe(visible);
    assertCommonInitialStateIsCorrect();
  }

  private void assertCommonInitialStateIsCorrect() {
    AccessEditorPage.searchBox().shouldHave(value("")).shouldHave(INITIAL_VALUE_CLASS);
    AccessEditorPage.searchButton().shouldHave(DISABLED_CLASS);
    DoubleColumnPicker.availableItemList().shouldBe(empty);
    AccessEditorPage.saveButton().shouldHave(DISABLED_CLASS);
  }

  private List<MembershipMapping> getMembershipMappings(final String ownersId, final String roleName) {
    return membershipMappingDAO.getByContextIdAndRoleId(ownersId, roleDAO.getByName(roleName).getId());
  }
}
