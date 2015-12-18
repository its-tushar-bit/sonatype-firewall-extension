/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.InlineEditor;
import com.sonatype.clm.testing.functional.pages.LdapConfigurationPage;
import com.sonatype.clm.testing.functional.pages.LdapConnectionConfigurationPage;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;

import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;

public class LdapConfigurationTest
    extends AbstractFunctionalTest
{
  private LdapServer server;
  @BeforeClass
  public static void startup() {
    open(LdapConfigurationPage.URL);
    loginAsAdmin();
  }

  @Before
  public void before() {
    server = tempEntity.newLdapServer("CLM Ldap Server");
    refreshOrOpen(LdapConfigurationPage.URL);
    LdapConfigurationPage.root().should(appear);
  }

  @After
  public void end() {
    LdapServerDAO ldapServerDAO = new LdapServerDAO();
    for (LdapServer server : ldapServerDAO.getAll()) {
      ldapServerDAO.delete(server);
    }
  }

  @Test
  public void testCreateLdapServer() {
    new LdapServerDAO().delete(server);
    refresh();
    LdapConfigurationPage.root().should(appear);
    InlineEditor ldapName = LdapConfigurationPage.name();
    ldapName.getElement().should(appear);
    ldapName.getElement().shouldBe(visible);

    LdapConfigurationPage.nameSaveButton().shouldBe(visible);
    LdapConfigurationPage.nameCancelButton().shouldBe(visible);

    ldapName.setValue("CLM Ldap Server");
    LdapConfigurationPage.nameSaveButton().click();

    // On connection configuration page
    LdapConnectionConfigurationPage.hostname().shouldBe(visible);
    LdapConnectionConfigurationPage.port().shouldHave(value("389"));
    LdapConnectionConfigurationPage.saveButton().shouldBe(disabled);
  }

  @Test
  public void testResetForm() {
    LdapConnectionConfigurationPage.hostname().should(appear);
    LdapConnectionConfigurationPage.hostname().setValue("ldap.clm");
    LdapConnectionConfigurationPage.searchBase().setValue("dc=win,dc=blackforest,dc=local");

    for (SelenideElement field : LdapConnectionConfigurationPage.getRequiredFields()) {
      field.shouldNotHave(cssClass("ng-invalid-required"));
    }

    LdapConnectionConfigurationPage.saveButton().shouldBe(enabled);

    LdapConnectionConfigurationPage.cancelButton().click();

    LdapConnectionConfigurationPage.discardChangesButton().shouldBe(visible, enabled);
    LdapConnectionConfigurationPage.discardChangesButton().click();

    LdapConnectionConfigurationPage.discardChangesButton().shouldNotBe(visible);
    LdapConnectionConfigurationPage.saveButton().shouldBe(disabled);
    LdapConnectionConfigurationPage.cancelButton().click();
  }

  @Test
  public void testErrorPopovers() {
    LdapConnectionConfigurationPage.hostname().should(appear);

    for (SelenideElement element : LdapConnectionConfigurationPage.getRequiredFields()) {
      element.sendKeys("a");
      element.sendKeys(Keys.BACK_SPACE);
      popoverViolations(element).shouldHave(text("Please enter a value"));
    }

    LdapConnectionConfigurationPage.cancelButton().click();
    LdapConnectionConfigurationPage.discardChangesButton().shouldBe(visible, enabled);
    LdapConnectionConfigurationPage.discardChangesButton().click();
  }

  @Test
  public void testDeleteServer() {
    LdapConfigurationPage.deleteButton().should(appear).shouldBe(visible);
    LdapConfigurationPage.deleteButton().click();
    LdapConfigurationPage.deleteConfirmationButton().shouldBe(visible).click();
    LdapConfigurationPage.root().should(disappear);
    waitUntilNotUrl(LdapConfigurationPage.URL);
  }
}
