/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage.AdministratorsRoleMappingList.RoleMappingElement;
import com.sonatype.clm.testing.functional.pages.AdministratorsPage.AdministratorsRoleMappingList.RoleMappingElement.Content;
import com.sonatype.insight.brain.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;

public class AdministratorsTest
    extends AbstractFunctionalTest
{

  @BeforeClass
  public static void startup() {
    open(AdministratorsPage.URL);
    loginAsAdmin();
  }

  @Test
  public void testGroupSearchWarning() {
    // no ldap servers configured
    RoleMappingElement policyAdministrator = AdministratorsPage.administratorsRoleMappingList()
        .element(1);
    policyAdministrator.shouldBe(visible).shouldHave(text("Policy Administrator")).editButton().click();
    policyAdministrator.content().shouldBe(visible).groupSearchWarning().shouldNotBe(visible);

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

    refreshOrOpen(AdministratorsPage.URL);

    policyAdministrator = AdministratorsPage.administratorsRoleMappingList().element(1);
    policyAdministrator.shouldBe(visible).editButton().click();
    policyAdministrator.content().shouldBe(visible).groupSearchWarning().shouldNotBe(visible);

    // mix servers have group search disabled and disabled
    userMapping2.setDynamicGroupSearchEnabled(true);
    ldapUserMappingDAO.update(userMapping2);

    refreshOrOpen(AdministratorsPage.URL);

    policyAdministrator = AdministratorsPage.administratorsRoleMappingList().element(1);
    policyAdministrator.shouldBe(visible).editButton().click();
    policyAdministrator.content().shouldBe(visible).groupSearchWarning().shouldBe(visible).shouldHave(text(
        Content.MIXED_GROUP_SEARCH_WARNING));

    // all servers have group search enabled
    userMapping1.setDynamicGroupSearchEnabled(true);
    ldapUserMappingDAO.update(userMapping1);

    refreshOrOpen(AdministratorsPage.URL);

    policyAdministrator = AdministratorsPage.administratorsRoleMappingList().element(1);
    policyAdministrator.shouldBe(visible).editButton().click();
    policyAdministrator.content().shouldBe(visible).groupSearchWarning().shouldNotBe(visible);
  }
}
