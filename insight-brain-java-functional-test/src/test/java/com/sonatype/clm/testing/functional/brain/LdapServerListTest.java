/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.SystemConfigMenu;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.LdapServerListPage;
import com.sonatype.clm.testing.functional.pages.LdapServerListPage.ListRow;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.utils.FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

public class LdapServerListTest
    extends AbstractFunctionalTest
{
  private LdapServerDAO ldapServerDAO;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    ldapServerDAO = lookup(LdapServerDAO.class);
    refreshOrOpen(IndexPage.url());
    waitUntilUrl(DashboardPage.url());
  }

  @After
  public void end() {
    for (LdapServer ldapServer : ldapServerDAO.getAll()) {
      ldapServerDAO.delete(ldapServer);
    }
  }

  protected LdapServerListPage getLdapServerListPage() {
    return new LdapServerListPage();
  }

  @Test
  public void testLdapServerList() {
    navigateToLdapServerList();

    LdapServerListPage ldapServerListPage = new LdapServerListPage();
    ldapServerListPage.shouldBe(visible);

    ldapServerListPage.listElements().shouldHave(size(0));
    ldapServerListPage.emptyDescriptor().shouldBe(visible);

    tempEntity.newLdapServer("IQ Ldap Server");
    tempEntity.newLdapServer("Another Ldap Server");

    refresh();

    ldapServerListPage.emptyDescriptor().shouldBe(hidden);
    ldapServerListPage.listElements().shouldHave(size(2));

    ListRow row = ldapServerListPage.listRow(1);
    row.chevron().shouldBe(visible);
    row.shouldBe(visible).shouldHave(text("IQ Ldap Server"));
    ListRow row2 = ldapServerListPage.listRow(2);
    row2.chevron().shouldBe(visible);
    row2.shouldBe(visible).shouldHave(text("Another Ldap Server"));
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testReorderLdapServers() {
    navigateToLdapServerList();

    tempEntity.newLdapServer("Fourth Server");
    tempEntity.newLdapServer("Third Server");
    tempEntity.newLdapServer("Second Server");
    tempEntity.newLdapServer("First Server");
    refresh();

    LdapServerListPage ldapServerListPage = new LdapServerListPage();

    ldapServerListPage.reorderButton().shouldBe(visible).click();
    ldapServerListPage.addButton().shouldBe(disabled);
    ldapServerListPage.reorderButton().shouldBe(disabled);
    ldapServerListPage.saveButton().shouldBe(visible).click();
    FormUtils.getAlertElement()
        .shouldHave(text(DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));

    ldapServerListPage.listElements()
        .shouldHave(texts("Fourth Server",
            "Third Server", "Second Server", "First Server"));

    eyesWatcher.eyesCheck();
    ListRow reorderRow = ldapServerListPage.listRow(1);
    reorderRow.reorderUp().shouldBe(disabled);
    reorderRow.reorderDown().shouldNotBe(disabled);

    reorderRow.reorderDown().click();
    ldapServerListPage.listElements()
        .shouldHave(texts("Third Server",
            "Fourth Server", "Second Server", "First Server"));

    ListRow reorderRowLast = ldapServerListPage.listRow(4);
    reorderRowLast.reorderUp().shouldNotBe(disabled);
    reorderRowLast.reorderDown().shouldBe(disabled);

    reorderRowLast.reorderUp().click();
    ldapServerListPage.listElements()
        .shouldHave(texts("Third Server",
            "Fourth Server", "First Server", "Second Server"));

    ldapServerListPage.saveButton().shouldBe(visible).click();
    ldapServerListPage.listElements()
        .shouldHave(texts("Third Server",
            "Fourth Server", "First Server", "Second Server"));

    NxSubmitMask.seeAndWaitForDismissal();

    List<LdapServer> actualLdapServers = ldapServerDAO.getAll();
    String[] ldapServerNames = new String[]{"Third Server", "Fourth Server",
      "First Server", "Second Server"};
    for (int i = 0; i < ldapServerNames.length; i++) {
      LdapServer ldapServer = actualLdapServers.get(i);
      assertThat(ldapServer.getName()).isEqualTo(ldapServerNames[i]);
      assertThat(ldapServer.getPriority()).isEqualTo(i + 1);
    }
  }

  protected void navigateToLdapServerList() {
    var systemConfigMenu = new SystemConfigMenu();
    systemConfigMenu.dropdownToggle().click();
    systemConfigMenu.ldap().shouldBe(visible).click();
  }
}
