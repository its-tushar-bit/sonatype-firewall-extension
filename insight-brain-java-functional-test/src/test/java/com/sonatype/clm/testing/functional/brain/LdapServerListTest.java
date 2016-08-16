/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.TileSimpleList;
import com.sonatype.clm.testing.functional.elements.TileSimpleList.TileSimpleListElement;
import com.sonatype.clm.testing.functional.pages.LdapServerListPage;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;

public class LdapServerListTest
    extends AbstractFunctionalTest
{

  @BeforeClass
  public static void startup() {
    open(LdapServerListPage.URL);
    loginAsAdmin();
  }

  @After
  public void end() {
    LdapServerDAO ldapServerDAO = new LdapServerDAO();
    for (LdapServer server : ldapServerDAO.getAll()) {
      ldapServerDAO.delete(server);
    }
  }

  @Test
  public void testLdapServerList() {
    LdapServerListPage ldapServerListPage = new LdapServerListPage();
    ldapServerListPage.shouldBe(visible);

    ldapServerListPage.ldapServerList().elements().shouldHaveSize(0);
    ldapServerListPage.ldapServerList().emptyDescriptor().shouldBe(visible);
    
    tempEntity.newLdapServer("IQ Ldap Server");
    tempEntity.newLdapServer("Another Ldap Server");

    refresh();

    ldapServerListPage.ldapServerList().emptyDescriptor().shouldNotBe(visible);

    TileSimpleList list = ldapServerListPage.ldapServerList();
    list.elements().shouldHaveSize(2);

    TileSimpleListElement row = list.element(0);
    row.chevron().shouldBe(visible);
    row.name().shouldBe(visible).shouldHave(text("Another Ldap Server"));
    TileSimpleListElement row2 = list.element(1);
    row2.chevron().shouldBe(visible);
    row2.name().shouldBe(visible).shouldHave(text("IQ Ldap Server"));
  }
}
