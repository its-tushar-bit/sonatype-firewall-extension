/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional;

import com.sonatype.clm.testing.functional.pages.IntegrationsPage;

import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Test;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class IntegrationsPageTest
    extends AbstractFunctionalTest
{
  @After
  public void after() {
    logout();
  }

  @Test
  public void testNavigation() {
    refreshOrOpen(IntegrationsPage.urlOverview());
    loginAsAdmin();

    sideNavigation().shouldBe(visible);

    sideCiCdLink().shouldBe(visible).click();
    waitUntilUrl(IntegrationsPage.urlCiCd());
    ciCdSection().shouldBe(visible);

    sideScmLink().shouldBe(visible).click();
    waitUntilUrl(IntegrationsPage.urlScm());
    scmSection().shouldBe(visible);

    sideIssueTrackingLink().shouldBe(visible).click();
    waitUntilUrl(IntegrationsPage.urlIssueTracking());
    issueTrackingSection().shouldBe(visible);

    sideIdeLink().shouldBe(visible).click();
    waitUntilUrl(IntegrationsPage.urlIde());
    ideSection().shouldBe(visible);

    sideOthersLink().shouldBe(visible).click();
    waitUntilUrl(IntegrationsPage.urlOthers());
    othersSection().shouldBe(visible);
  }

  private SelenideElement sideNavigation() {
    return $("#integrations-sidebar");
  }

  private SelenideElement sideCiCdLink() {
    return $("#integrations-sidebar__cicd-link");
  }

  private SelenideElement sideScmLink() {
    return $("#integrations-sidebar__scm-link");
  }

  private SelenideElement sideIssueTrackingLink() {
    return $("#integrations-sidebar__issue-tracking-link");
  }

  private SelenideElement sideIdeLink() {
    return $("#integrations-sidebar__ide-link");
  }

  private SelenideElement sideOthersLink() {
    return $("#integrations-sidebar__others-link");
  }

  private SelenideElement ciCdSection() {
    return $("#iq-integrations-cicd-section");
  }

  private SelenideElement scmSection() {
    return $("#iq-integrations-scm-section");
  }

  private SelenideElement issueTrackingSection() {
    return $("#iq-integrations-issue-tracking-section");
  }

  private SelenideElement ideSection() {
    return $("#iq-integrations-ide-section");
  }

  private SelenideElement othersSection() {
    return $("#iq-integrations-others-section");
  }
}
