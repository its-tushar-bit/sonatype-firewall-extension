/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class RequestWaiverPage
        extends BasicElement<RequestWaiverPage>
{
  private static final String ROOT_SELECTOR = "#request-waiver-page";

  public static String url(String violationId) {
    return BaseUrl.resolvePageUrl("/requestWaiver/{id}", violationId);
  }

  public static String urlWithQueryParams(String violationId, String type, String sidebarReference) {
    return BaseUrl.resolvePageUrl(
            "/requestWaiver/{id}?type={type}&sidebarReference={sidebarReference}",
            violationId,
            type,
            sidebarReference
    );
  }

  public SelenideElement root() {
    return $(ROOT_SELECTOR);
  }

  public SelenideElement requestWaiverHeader() {
    return child(".nx-h1");
  }

  public SelenideElement requestWaiverReadOnlyData() {
    return child(".nx-read-only");
  }

  public SelenideElement requestWaiverPolicyViolationId() {
    return child("#request-waivers-policy-violation-id");
  }

  public NxBackButton backButton() {
    return new NxBackButton("#menu-bar__back-button-container");
  }
}
