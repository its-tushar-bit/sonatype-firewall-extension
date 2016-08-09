/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LdapConnectionForm;
import com.sonatype.clm.testing.functional.elements.LdapNameEditor;
import com.sonatype.clm.testing.functional.elements.LdapNameEditor.NameEditor;
import com.sonatype.clm.testing.functional.pages.LdapConfigurationPage;
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
import static com.codeborne.selenide.Condition.empty;
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
    LdapConfigurationPage.root().shouldBe(visible);
    LdapNameEditor ldapNameEditor = LdapConfigurationPage.ldapNameEditor();
    NameEditor nameEditor = ldapNameEditor.nameEditor();

    ldapNameEditor.saveButton().shouldBe(visible, disabled);
    ldapNameEditor.cancelButton().shouldBe(visible, enabled);

    nameEditor.shouldBe(visible).setValue("CLM Ldap Server");
    ldapNameEditor.saveButton().shouldBe(visible, enabled).click();

    // On connection configuration page
    LdapConnectionForm ldapConnectionForm = LdapConfigurationPage.ldapConnectionForm();
    ldapConnectionForm.hostname().shouldBe(visible);
    ldapConnectionForm.port().shouldHave(value("389"));
    ldapConnectionForm.saveButton().shouldBe(disabled);
  }

  @Test
  public void testResetForm() {
    LdapConnectionForm ldapConnectionForm = LdapConfigurationPage.ldapConnectionForm();

    ldapConnectionForm.hostname().shouldBe(visible, empty).setValue("ldap.clm");
    ldapConnectionForm.searchBase().shouldBe(visible, empty).setValue("dc=win,dc=blackforest,dc=local");

    for (SelenideElement field : ldapConnectionForm.getRequiredFields()) {
      field.shouldNotHave(cssClass("ng-invalid-required"));
    }

    ldapConnectionForm.saveButton().shouldBe(visible, enabled);
    ldapConnectionForm.cancelButton().click();

    // Continue and discard changes (reset)
    LdapConfigurationPage.discardChangesModalButton().shouldBe(visible, enabled).click();
    LdapConfigurationPage.discardChangesModalButton().shouldNotBe(visible);

    ldapConnectionForm.saveButton().shouldBe(disabled);
    ldapConnectionForm.cancelButton().shouldBe(disabled);
  }

  @Test
  public void testErrorPopovers() {
    LdapConnectionForm ldapConnectionForm = LdapConfigurationPage.ldapConnectionForm();
    ldapConnectionForm.hostname().shouldBe(visible);

    for (SelenideElement element : ldapConnectionForm.getRequiredFields()) {
      element.sendKeys("a");
      element.sendKeys(Keys.BACK_SPACE);
      popoverViolations(element).shouldHave(text("Please enter a value"));
    }

    ldapConnectionForm.cancelButton().click();
    LdapConfigurationPage.discardChangesModalButton().shouldBe(visible, enabled).click();
  }

  @Test
  public void testDeleteServer() {
    LdapConfigurationPage.deleteButton().shouldBe(visible);
    LdapConfigurationPage.deleteButton().click();
    LdapConfigurationPage.deleteConfirmationButton().shouldBe(visible).click();
    LdapConfigurationPage.root().should(disappear);
    waitUntilNotUrl(LdapConfigurationPage.URL);
  }
}
