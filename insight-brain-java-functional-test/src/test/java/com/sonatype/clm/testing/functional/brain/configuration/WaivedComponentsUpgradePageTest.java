/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.WaivedComponentsUpgradePage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class WaivedComponentsUpgradePageTest
    extends AbstractFunctionalTest
{
  @Before
  public void before() {
    refreshOrOpen(WaivedComponentsUpgradePage.url());
    loginAsAdmin();
  }

  @After
  public void after() {
    logout();
    clearAlerts();
  }

  @Test
  public void testDefaultState() {
    WaivedComponentsUpgradePage waivedComponentsUpgradePage = new WaivedComponentsUpgradePage();
    eyesWatcher.eyesCheck("Waived Components Upgrade Page");
    waivedComponentsUpgradePage.update().shouldBe(enabled);
    waivedComponentsUpgradePage.cancel().shouldBe(disabled);
    waivedComponentsUpgradePage.toggleInput().shouldNotBe(checked);
  }

  @Test
  public void testNoChangesError() {
    WaivedComponentsUpgradePage waivedComponentsUpgradePage = new WaivedComponentsUpgradePage();
    waivedComponentsUpgradePage.update().click();
    waivedComponentsUpgradePage.validationErrors()
        .shouldBe(visible)
        .shouldHave(text("There were validation errors. There are no changes to update"));
    waivedComponentsUpgradePage.update().shouldNotBe(visible);
  }

  @Test
  public void testSuccessfulUpdate() {
    WaivedComponentsUpgradePage waivedComponentsUpgradePage = new WaivedComponentsUpgradePage();
    waivedComponentsUpgradePage.toggle().click();
    waivedComponentsUpgradePage.toggleInput().shouldBe(checked);
    waivedComponentsUpgradePage.update().click();
    waivedComponentsUpgradePage.update().shouldBe(enabled);
    waivedComponentsUpgradePage.cancel().shouldBe(disabled);
    waivedComponentsUpgradePage.toggleInput().shouldBe(checked);
  }
}
