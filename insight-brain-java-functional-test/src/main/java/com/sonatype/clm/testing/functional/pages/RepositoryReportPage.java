/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class RepositoryReportPage
{
  private static final String BASE_URL = "audit-report/index.html";

  public static String url(String repositoryManagerInstanceId, String repositoryPublicId)
      throws UnsupportedEncodingException
  {
    repositoryManagerInstanceId = URLEncoder.encode(repositoryManagerInstanceId, "UTF-8");
    repositoryPublicId = URLEncoder.encode(repositoryPublicId, "UTF-8");

    return BASE_URL + "?repositoryManagerInstanceId=" + repositoryManagerInstanceId + "&repositoryPublicId="
        + repositoryPublicId;
  }

  public static class Summary
  {
    public static SelenideElement root() {
      return $("#summary");
    }

    public static SelenideElement criticalCount() {
      return root().find(".pv-red");
    }

    public static SelenideElement severeCount() {
      return root().find(".pv-orange");
    }

    public static SelenideElement moderateCount() {
      return root().find(".pv-yellow");
    }

    public static SelenideElement violatingComponentsCount() {
      return root().find(".span21 .value_sml");
    }

    public static SelenideElement noPolicyViolations() {
      return $(".pval");
    }

    public static SelenideElement identifiedCount() {
      return root().find(".header-container .value_lrg");
    }

    public static SelenideElement identifiedPercent() {
      return root().find(".header-container .value_sml");
    }
  }
}
