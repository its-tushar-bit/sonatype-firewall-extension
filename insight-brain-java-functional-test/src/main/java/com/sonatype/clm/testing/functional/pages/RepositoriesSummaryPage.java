/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class RepositoriesSummaryPage
{

  static final String REPOSITORIES_SUMMARY_SELECTOR = "#repositories-summary";

  public static String URL = BaseUrl.uriBuilder().fragment("/management/view/repositories").build().toString();

  public static class SummaryTile
  {
    public static SelenideElement name() {
      return $(SelectorUtils.createSelector(REPOSITORIES_SUMMARY_SELECTOR, "h1"));
    }

    public static SelenideElement configButton() {
      return $("#repositories-configuration-button");
    }

    public static SelenideElement accessButton() {
      return $("#repositories-access-button");
    }

    public static SelenideElement addRoleButton() {
      return $("#add-role-button");
    }

    public static SelenideElement localAccessRole(String roleName) {
      return $$("#repositories-pill-access table td.role").findBy(text(roleName));
    }
  }
}
