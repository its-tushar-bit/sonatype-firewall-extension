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
import com.sonatype.clm.testing.functional.pages.AccessEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage.SummaryTile;
import com.sonatype.insight.brain.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ApplicationAccessEditorTest
    extends AbstractAccessEditorTest
{

  private String serverId;

  @Before
  public void init() {
    serverId = tempEntity.newLdapServer("LDAP").getId();
    tempEntity.newLdapConnection(serverId);

    LdapUserMapping userMapping = tempEntity.newLdapUserMapping(serverId);
    userMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    userMapping.setDynamicGroupSearchEnabled(false);
    new LdapUserMappingDAO().update(userMapping);

    // note the ȧ being used to force a character to be encoded
    super.init(tempEntity.newApplicationWithParent("test_ȧpp_id"));
  }

  @Override
  protected void goFromSummaryToAddRole() {
    refresh(); // pills often fail to load CLM-5827
    SummaryTile.accessButton().click();
    SummaryTile.addRoleButton().click();
  }

  @Override
  protected void goFromSummaryToEditRole(Role role) {
    refresh(); // pills often fail to load CLM-5827
    SummaryTile.localAccessRole(role.getName()).click();
  }

  @Test
  public void testAddGroupWithoutSearching() {
    goFromSummaryToAddRole();

    // select a role
    Dropdown roleDropdown = AccessEditorPage.roleDropdown().shouldBe(visible);
    roleDropdown.selectedItem().click();

    SelenideElement roleEntry = roleDropdown.listItem(1).shouldBe(visible);
    final String roleName = roleEntry.getText();
    roleEntry.click();

    SelenideElement addGroupButton = AccessEditorPage.addGroupButton();
    addGroupButton.shouldBe(visible, disabled);

    AccessEditorPage.addGroupBox().shouldBe(visible).val("FooBar");
    addGroupButton.shouldBe(enabled).click();

    DoubleColumnPicker picker = new DoubleColumnPicker();
    picker.availableItems().shouldHave(texts("FooBar"));
    picker.checkAllLeft().click();
    picker.pickCheckedItemsButton().click();

    AccessEditorPage.saveButton().shouldNotBe(CLM.DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    AccessEditorPage.addGroupBox().shouldBe(visible, value(""));

    List<MembershipMapping> mappings = getMembershipMappings(currentOwner.getId(), roleName);
    assertThat(mappings.size(), is(1));

    MembershipMapping mapping = mappings.get(0);
    assertThat(mapping.getMemberType(), is(MemberType.GROUP));
    assertThat(mapping.getMemberName(), is("FooBar"));
  }
}
