/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.NexusOnePage;

import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

/**
 * Verifies that the Nexus One SPA loads and renders its hello-world routes.
 */
public class NexusOnePageLoadTest
    extends AbstractFunctionalTest
{
  @Test
  public void testNexusOneSpaLoads() {
    refreshOrOpen(NexusOnePage.url());
    NexusOnePage page = new NexusOnePage();
    page.shouldBe(visible);
    page.heading().shouldHave(text("Hello World 1"));
  }

  @Test
  public void testNexusOneRoutesWork() {
    refreshOrOpen(NexusOnePage.url("/hello2"));
    NexusOnePage page = new NexusOnePage();
    page.shouldBe(visible);
    page.heading().shouldHave(text("Hello World 2"));
  }
}
