/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionList;
import com.sonatype.clm.testing.functional.elements.ActionList.ActionListElement;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.ldap.ReorderLdapModal;
import com.sonatype.clm.testing.functional.pages.LdapServerListPage;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;

import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static org.assertj.core.api.Assertions.assertThat;

public class LdapServerListTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void startup() {
    refreshOrOpen(LdapServerListPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    refreshOrOpen(LdapServerListPage.url());
  }

  @After
  public void end() {
    LdapServerDAO ldapServerDAO = new LdapServerDAO();
    for (LdapServer ldapServer : ldapServerDAO.getAll()) {
      ldapServerDAO.delete(ldapServer);
    }
  }

  @Test
  public void testLdapServerList() {
    LdapServerListPage ldapServerListPage = new LdapServerListPage();
    ldapServerListPage.shouldBe(visible);

    ActionList serverList = ldapServerListPage.ldapServerList();

    serverList.elements().shouldHaveSize(0);
    serverList.emptyDescriptor().shouldBe(visible);

    tempEntity.newLdapServer("IQ Ldap Server");
    tempEntity.newLdapServer("Another Ldap Server");

    refresh();

    serverList.emptyDescriptor().shouldBe(hidden);
    serverList.elements().shouldHaveSize(2);

    ActionListElement row = serverList.element(0);
    row.chevron().shouldBe(visible);
    row.shouldBe(visible).shouldHave(text("IQ Ldap Server"));
    ActionListElement row2 = serverList.element(1);
    row2.chevron().shouldBe(visible);
    row2.shouldBe(visible).shouldHave(text("Another Ldap Server"));
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testReorderLdapServers() {
    tempEntity.newLdapServer("Fourth Server");
    tempEntity.newLdapServer("Third Server");
    tempEntity.newLdapServer("Second Server");
    tempEntity.newLdapServer("First Server");
    refresh();

    ReorderLdapModal modal = new LdapServerListPage().openModalWithAssert();
    modal.assertOrder("Fourth Server", "Third Server", "Second Server", "First Server");
    modal.assertUpDownButtonEnabled(false, false);
    modal.saveButton().shouldBe(DISABLED).hover();
    Tooltip.get().shouldHave(text("There are no changes to update."));

    eyesWatcher.eyesCheck();
    SelenideElement row = modal.row(0);
    row.click();

    row.shouldBe(ReorderLdapModal.SELECTED);
    modal.assertUpDownButtonEnabled(false, true);

    modal.moveToLastButton().click();
    modal.assertOrder("Third Server", "Second Server", "First Server", "Fourth Server");
    modal.assertUpDownButtonEnabled(true, false);

    modal.row(0).click();
    modal.moveDownButton().click();
    modal.assertOrder("Second Server", "Third Server", "First Server", "Fourth Server");
    modal.assertUpDownButtonEnabled(true, true);

    modal.row(2).click();
    modal.moveUpButton().click();
    modal.assertOrder("Second Server", "First Server", "Third Server", "Fourth Server");
    modal.assertUpDownButtonEnabled(true, true);

    modal.moveToFirstButton().click();
    modal.assertOrder("First Server", "Second Server", "Third Server", "Fourth Server");
    modal.assertUpDownButtonEnabled(false, true);

    modal.saveButton().shouldNotBe(DISABLED).click();
    modal.should(disappear);

    new LdapServerListPage().shouldBe(visible).ldapServerList().elements()
        .shouldHave(texts("First Server", "Second Server", "Third Server", "Fourth Server"));

    List<LdapServer> actualLdapServers = new LdapServerDAO().getAll();
    String[] ldapServerNames = new String[]{"First Server", "Second Server", "Third Server", "Fourth Server"};
    for (int i = 0; i < ldapServerNames.length; i++) {
      LdapServer ldapServer = actualLdapServers.get(i);
      assertThat(ldapServer.getName()).isEqualTo(ldapServerNames[i]);
      assertThat(ldapServer.getPriority()).isEqualTo(i + 1);
    }
  }
}
