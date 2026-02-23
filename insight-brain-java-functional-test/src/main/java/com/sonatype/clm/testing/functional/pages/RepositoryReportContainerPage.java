/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class RepositoryReportContainerPage
{
  public static String url(String repositoryId) {
    return BaseUrl.resolvePageUrl("/firewall/repository/{repositoryId}/result", repositoryId);
  }

  public static SelenideElement title() {
    return $(".nx-page-title");
  }

  public static SelenideElement refreshButton() {
    return $("#report-title-right button");
  }

  public static SelenideElement oldestEvalTime() {
    return $("#report-title .last-eval");
  }

  public static NxBackButton backButton() {
    return new NxBackButton();
  }

  public static class ReEvaluateModal
  {
    public static SelenideElement root() {
      return $("#repository-reevaluate-modal");
    }

    public static SelenideElement submitButton() {
      return $("#perform-re-evaluate");
    }

    public static SelenideElement cancelButton() {
      return $("#close-re-evaluate");
    }
  }
}
