/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.ErrorBox;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class OwnerSummaryPage
{
  public static String url(String contextType, String id) {
    return "new/assets/index.html#/management/" + contextType + "/" + id + "/view";
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

    public static SelenideElement addLabelButton() {
      return $("#add-label-button");
    }

    public static SelenideElement localLabel(String labelName) {
      return $$("#owner-pill-comp-labels  ul div.title").findBy(text(labelName));
    }

    public static ErrorBox error() {
      return new ErrorBox(root().find(".clm-alert.alert-error"));
    }
    
    public static SelenideElement appCategoriesButton() {
      return root().find("#owner-app-categories-button");
    }
    
    public static SelenideElement policyButton() {
      return root().find("#owner-policy-button");
    }

    public static SelenideElement labelsButton() {
      return root().find("#owner-comp-labels-button");
    }

    public static SelenideElement ltgsButton() {
      return root().find("#owner-ltgs-button");
    }

    public static SelenideElement accessButton() {
      return root().find("#owner-access-button");
    }
    
  }
}
