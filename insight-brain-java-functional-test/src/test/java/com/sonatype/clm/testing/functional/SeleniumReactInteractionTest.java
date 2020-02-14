/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import com.sonatype.clm.testing.functional.elements.ReactTextInput;
import com.sonatype.clm.testing.functional.pages.EmailConfigurationPage;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.junit.Test;

import static com.codeborne.selenide.Selectors.byId;
import static com.codeborne.selenide.Selenide.$;

public class SeleniumReactInteractionTest
    extends AbstractFunctionalTest
{
  /**
   * Is this test failing with hostname actually having value "a" only instead of "aa"?
   * This is good news. This means we (probably) do not need {@link ReactTextInput} anymore.
   * This test is expected to fail with a Selenium version bump where Selenium starts interacting with React components
   * properly without the workarounds we have.
   * <ul>
   * <li>Remove {@link ReactTextInput}</li>
   * <li>Fix all compilation errors by falling back to good old {@link SelenideElement} for {@link ReactTextInput}s
   * <li>This test should not be needed anymore, simply remove this class</li>
   * </ul>
   */
  @Test
  public void testSeleniumReactInteraction() {
    refreshOrOpen(EmailConfigurationPage.emailConfigurationUrl());
    loginAsAdmin();

    SelenideElement hostname = $(byId("email-config-hostname"));
    hostname.setValue("a");
    hostname.clear();
    hostname.setValue("a");
    hostname.shouldBe(Condition.value("aa"));
  }
}
