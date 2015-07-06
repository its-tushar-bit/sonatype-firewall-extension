/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.ErrorBox;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class OwnerSummaryPage
{
  public static String url(String contextType, String id) {
    return "new/assets/index.html#/management/" + contextType + "/" + id;
  }

  public static class SummaryTile
  {
    private static SelenideElement root() {
      return $("#owner-summary");
    }
    public static SelenideElement name() {
      return root().find("h1");
    }

    public static SelenideElement icon() {
      return $("img");
    }

    public static ErrorBox error() {
      return new ErrorBox(root().find(".clm-error"));
    }
  }
}
