/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage.AdministratorsRoleMappingList;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage.AdministratorsRoleMappingList.RoleMappingElement;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage.AdministratorsRoleMappingList.RoleMappingElement.Content;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;

import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class AdministratorsTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void initialLogin() {
    refreshOrOpen(AdministratorsPage.url());
    loginAsAdmin();
  }

  @Before
  public void startup() {
    //create two users
    tempEntity.newUser("test-a", "secret", "John", "Doe", "john@doe.net");
    tempEntity.newUser("test-b", "secret", "Jane", "Doe", "jane@doe.net");

    refreshOrOpen(AdministratorsPage.url());
  }

  @After
  public void prepareToLeavePage() {
    SelenideElement cancelBtn =
        AdministratorsPage.administratorsRoleMappingList().element(0).content().cancelButton();

    if (cancelBtn.isDisplayed()) {
      cancelBtn.click();

      //dismiss unsaved changes
      SelenideElement modalBtn = $(".modal-dialog .btn-primary");
      if (modalBtn.exists()) {
        modalBtn.click();
      }
    }
  }

  @Test
  public void testDefaultRolesAndBuiltinUsers() {
    AdministratorsRoleMappingList mapping = AdministratorsPage.administratorsRoleMappingList();
    mapping.elements().shouldHaveSize(2);

    RoleMappingElement roleRow = mapping.element(0);
    Content content = roleRow.content();
    roleRow.shouldBe(visible);
    content.members().shouldHave(texts("Admin BuiltIn"));
    content.editor().shouldBe(hidden);

    RoleMappingElement policyAdminRoleRow = mapping.element(1);
    policyAdminRoleRow.shouldBe(visible);
    policyAdminRoleRow.content().editor().shouldBe(hidden);
  }

  @Test
  public void testClickEdit() {
    RoleMappingElement roleRow = AdministratorsPage.administratorsRoleMappingList().element(0);
    Content content = roleRow.content();
    roleRow.hover();

    roleRow.editButton().shouldBe(visible);

    roleRow.editButton().click();
    content.editor().shouldBe(visible);
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testSearch() {
    RoleMappingElement roleRow = AdministratorsPage.administratorsRoleMappingList().element(0);
    Content content = roleRow.content();
    roleRow.hover();
    roleRow.editButton().click();
    content.queryInput().setValue("Jan*");
    content.search();

    content.availableMembers().shouldHave(texts("Jane Doe"));
    content.appliedMembers().shouldHave(texts("Admin BuiltIn"));

    content.queryInput().setValue("*Do*");
    content.search();

    content.availableMembers().shouldHave(texts("Jane Doe", "John Doe"));
    content.appliedMembers().shouldHave(texts("Admin BuiltIn"));
  }

  @Test
  public void testAddUser() {
    RoleMappingElement roleRow = AdministratorsPage.administratorsRoleMappingList().element(0);
    Content content = roleRow.content();
    roleRow.hover();
    roleRow.editButton().click();
    content.queryInput().setValue("*Do*");
    content.search();

    content.availableMember("John Doe").click();
    content.pick();

    content.availableMembers().shouldHaveSize(1);
    content.availableMembers().shouldHave(texts("Jane Doe"));
    content.appliedMembers().shouldHaveSize(2);
    content.appliedMembers().shouldHave(texts("Admin BuiltIn", "John Doe"));

    content.availableMember("Jane Doe").click();
    content.pick();

    content.availableMembers().shouldHaveSize(0);
    content.appliedMembers().shouldHaveSize(3);
    content.appliedMembers().shouldHave(texts("Admin BuiltIn", "Jane Doe", "John Doe"));
  }

  @Test
  public void testRemoveUser() {
    RoleMappingElement roleRow = AdministratorsPage.administratorsRoleMappingList().element(0);
    Content content = roleRow.content();
    roleRow.hover();
    roleRow.editButton().click();
    content.queryInput().setValue("*Do*");
    content.search();
    content.availableMember("John Doe").click();
    content.pick();
    content.availableMember("Jane Doe").click();
    content.pick();

    //note that this click unchecks John, leaving Jane checked for removal
    content.appliedMember("John Doe").click();
    content.unpick();

    content.availableMembers().shouldHaveSize(1);
    content.availableMembers().shouldHave(texts("Jane Doe"));
    content.appliedMembers().shouldHaveSize(2);
    content.appliedMembers().shouldHave(texts("Admin BuiltIn", "John Doe"));
  }

  @Test
  public void testSaveChanges() {
    RoleMappingElement roleRow = AdministratorsPage.administratorsRoleMappingList().element(0);
    Content content = roleRow.content();
    roleRow.hover();
    roleRow.editButton().click();
    content.queryInput().setValue("*Do*");
    content.search();
    content.availableMember("John Doe").click();
    content.pick();

    content.confirmButton().click();
    //make sure we grab latest dom, as the save will rebuild it
    roleRow = AdministratorsPage.administratorsRoleMappingList().element(0);

    content.editor().shouldBe(hidden);
    content.members().shouldHave(texts("Admin BuiltIn, John Doe"));
  }

  @Test
  public void testGroupSearchWarning() {
    // no ldap servers configured
    RoleMappingElement policyAdministrator = AdministratorsPage.administratorsRoleMappingList()
        .element(0);
    policyAdministrator.shouldBe(visible).shouldHave(text("Policy Administrator")).editButton().click();
    policyAdministrator.content().shouldBe(visible).groupSearchWarning().shouldBe(hidden);

    // all servers have group search disabled
    LdapUserMappingDAO ldapUserMappingDAO = new LdapUserMappingDAO();
    String ldap1 = tempEntity.newLdapServer("LDAP1").getId();
    tempEntity.newLdapConnection(ldap1);

    LdapUserMapping userMapping1 = tempEntity.newLdapUserMapping(ldap1);
    userMapping1.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    userMapping1.setDynamicGroupSearchEnabled(false);
    ldapUserMappingDAO.update(userMapping1);

    String ldap2 = tempEntity.newLdapServer("LDAP2").getId();
    tempEntity.newLdapConnection(ldap2);

    LdapUserMapping userMapping2 = tempEntity.newLdapUserMapping(ldap2);
    userMapping2.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    userMapping2.setDynamicGroupSearchEnabled(false);
    ldapUserMappingDAO.update(userMapping2);

    refreshOrOpen(AdministratorsPage.url());

    policyAdministrator = AdministratorsPage.administratorsRoleMappingList().element(0);
    policyAdministrator.shouldBe(visible).editButton().click();
    policyAdministrator.content().shouldBe(visible).groupSearchWarning().shouldBe(visible).shouldHave(text(
        Content.DISABLED_GROUP_SEARCH_WARNING));

    // mix servers have group search disabled and disabled
    userMapping2.setDynamicGroupSearchEnabled(true);
    ldapUserMappingDAO.update(userMapping2);

    prepareToLeavePage();
    refreshOrOpen(AdministratorsPage.url());

    policyAdministrator = AdministratorsPage.administratorsRoleMappingList().element(0);
    policyAdministrator.shouldBe(visible).editButton().click();
    policyAdministrator.content().shouldBe(visible).groupSearchWarning().shouldBe(visible).shouldHave(text(
        Content.DISABLED_GROUP_SEARCH_WARNING));

    // all servers have group search enabled
    userMapping1.setDynamicGroupSearchEnabled(true);
    ldapUserMappingDAO.update(userMapping1);

    prepareToLeavePage();
    refreshOrOpen(AdministratorsPage.url());

    policyAdministrator = AdministratorsPage.administratorsRoleMappingList().element(1);
    policyAdministrator.shouldBe(visible).editButton().click();
    policyAdministrator.content().shouldBe(visible).groupSearchWarning().shouldBe(hidden);
  }
}
