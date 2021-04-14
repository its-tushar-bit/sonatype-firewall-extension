/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.List;

import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.DoubleColumnPicker;
import com.sonatype.clm.testing.functional.elements.Dropdown;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.pages.AccessEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;

import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationAccessEditorTest
    extends AbstractAccessEditorTest
{
  private String serverId;

  @Before
  public void init() {
    // note the ȧ being used to force a character to be encoded
    super.init(tempEntity.newApplicationWithParent("test_ȧpp_id", "ApplicationAccessEditorTest app"));
  }

  @Override
  protected void goFromSummaryToAddRole() {
    MainHeader.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().accessButton().click();
    OwnerSummaryPage.accessTile().addRoleButton().click();
    waitUntilUrl(AccessEditorPage.urlToCreate(currentOwner));
  }

  @Override
  protected void goFromSummaryToEditRole(Role role) {
    MainHeader.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().accessButton().click();
    OwnerSummaryPage.accessTile().localAccessRole(role.getName()).click();
    waitUntilUrl(AccessEditorPage.urlToEdit(currentOwner, role.getId()));
  }

  @Test
  public void testAddGroupWithoutSearching() {
    serverId = tempEntity.newLdapServer("LDAP").getId();
    tempEntity.newLdapConnection(serverId);

    LdapUserMapping ldapUserMapping = tempEntity.newLdapUserMapping(serverId);
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping.setDynamicGroupSearchEnabled(false);
    new LdapUserMappingDAO().update(ldapUserMapping);

    refresh(); // reload because UI data is cached
    goFromSummaryToAddRole();

    // select a role
    Dropdown roleDropdown = AccessEditorPage.roleDropdown().shouldBe(visible);
    roleDropdown.selectedItem().click();

    SelenideElement roleEntry = roleDropdown.listItem(2).shouldBe(visible);
    final String roleName = roleEntry.getText();
    roleEntry.click();

    SelenideElement addGroupButton = AccessEditorPage.addGroupButton();
    addGroupButton.shouldBe(visible, disabled);

    AccessEditorPage.addGroupBox().shouldBe(visible).val("FooBar");
    addGroupButton.shouldBe(enabled).click();

    DoubleColumnPicker picker = AccessEditorPage.picker();
    picker.availableItems().shouldHave(texts("FooBar"));
    picker.checkAllLeft().click();
    picker.pickCheckedItemsButton().click();

    AccessEditorPage.saveButton().scrollIntoView(true).shouldNotBe(CLM.DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    AccessEditorPage.addGroupBox().shouldBe(visible, value(""));

    List<MembershipMapping> mappings = getMembershipMappings(currentOwner.getId(), roleName);
    assertThat(mappings).hasSize(1);

    MembershipMapping mapping = mappings.get(0);
    assertThat(mapping.getMemberType()).isEqualTo(MemberType.GROUP);
    assertThat(mapping.getMemberName()).isEqualTo("FooBar");
  }
}
