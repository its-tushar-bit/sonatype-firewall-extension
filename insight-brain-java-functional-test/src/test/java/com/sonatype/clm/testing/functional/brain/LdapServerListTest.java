/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LdapNameEditor;
import com.sonatype.clm.testing.functional.elements.LdapNameEditor.NameEditor;
import com.sonatype.clm.testing.functional.elements.TileSimpleList;
import com.sonatype.clm.testing.functional.elements.TileSimpleList.TileSimpleListElement;
import com.sonatype.clm.testing.functional.pages.LdapConfigurationPage;
import com.sonatype.clm.testing.functional.pages.LdapServerListPage;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
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
    Mockito.when(featureUtils.hasMultipleLdapServersEnabled()).thenReturn(false);

    LdapServerDAO ldapServerDAO = new LdapServerDAO();
    for (LdapServer server : ldapServerDAO.getAll()) {
      ldapServerDAO.delete(server);
    }
  }

  @Test
  public void testLdapServerList() {
    refreshOrOpen(LdapServerListPage.URL);

    LdapServerListPage ldapServerListPage = new LdapServerListPage();
    ldapServerListPage.shouldBe(visible);

    ldapServerListPage.header().shouldBe(visible).shouldBe(text("LDAP Server"));
    ldapServerListPage.subHeader().shouldNot(exist);

    ldapServerListPage.ldapServerList().elements().shouldHaveSize(0);
    ldapServerListPage.ldapServerList().emptyDescriptor().shouldBe(visible).shouldHave(
        text("No LDAP server is defined"));

    ldapServerListPage.newServerButton().shouldBe(visible, enabled).click();

    waitUntilUrl(LdapConfigurationPage.createLdapUrl());
    LdapNameEditor ldapNameEditor = LdapConfigurationPage.ldapNameEditor();
    NameEditor nameEditor = ldapNameEditor.nameEditor();
    
    nameEditor.shouldBe(visible).setValue("IQ Ldap Server");
    ldapNameEditor.saveButton().shouldBe(visible, enabled).click();

    LdapConfigurationPage.ldapServersLink().click();

    ldapServerListPage.newServerButton().shouldBe(visible, disabled);
    ldapServerListPage.ldapServerList().emptyDescriptor().shouldNotBe(visible);

    TileSimpleList list = ldapServerListPage.ldapServerList();
    list.elements().shouldHaveSize(1);

    TileSimpleListElement row = list.element(0);
    row.chevron().shouldBe(visible);
    row.name().shouldBe(visible).shouldHave(text("IQ Ldap Server"));
  }

  @Test
  public void testMultipleLdapServerList() {
    // enable multiple ldap servers
    Mockito.when(featureUtils.hasMultipleLdapServersEnabled()).thenReturn(true);

    refreshOrOpen(LdapServerListPage.URL);

    LdapServerListPage ldapServerListPage = new LdapServerListPage();
    ldapServerListPage.shouldBe(visible);

    ldapServerListPage.header().shouldBe(visible).shouldBe(text("LDAP Servers"));
    ldapServerListPage.subHeader().shouldBe(visible).shouldHave(text("will be queried in the order listed"));
    
    ldapServerListPage.ldapServerList().elements().shouldHaveSize(0);
    ldapServerListPage.ldapServerList().emptyDescriptor().shouldBe(visible).shouldHave(
        text("No LDAP servers are defined"));

    ldapServerListPage.newServerButton().shouldBe(visible, enabled).click();

    waitUntilUrl(LdapConfigurationPage.createLdapUrl());
    LdapNameEditor ldapNameEditor = LdapConfigurationPage.ldapNameEditor();
    NameEditor nameEditor = ldapNameEditor.nameEditor();

    nameEditor.shouldBe(visible).setValue("IQ Ldap Server");
    ldapNameEditor.saveButton().shouldBe(visible, enabled).click();

    LdapConfigurationPage.ldapServersLink().click();

    ldapServerListPage.newServerButton().shouldBe(visible, enabled).click();
    
    waitUntilUrl(LdapConfigurationPage.createLdapUrl());

    nameEditor.shouldBe(visible).setValue("Another Ldap Server");
    ldapNameEditor.saveButton().shouldBe(visible, enabled).click();

    LdapConfigurationPage.ldapServersLink().click();

    ldapServerListPage.newServerButton().shouldBe(visible, enabled);
    ldapServerListPage.ldapServerList().emptyDescriptor().shouldNotBe(visible);

    TileSimpleList list = ldapServerListPage.ldapServerList();
    list.elements().shouldHaveSize(2);

    TileSimpleListElement row = list.element(0);
    row.chevron().shouldBe(visible);
    row.name().shouldBe(visible).shouldHave(text("IQ Ldap Server"));
    TileSimpleListElement row2 = list.element(1);
    row2.chevron().shouldBe(visible);
    row2.name().shouldBe(visible).shouldHave(text("Another Ldap Server"));
  }
}
