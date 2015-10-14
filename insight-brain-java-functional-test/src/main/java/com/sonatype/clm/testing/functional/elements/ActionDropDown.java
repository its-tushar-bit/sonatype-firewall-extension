/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class ActionDropDown
{

  public static SelenideElement root() {
    return $("#action-dropdown");
  }

  public static SelenideElement menu() {
    return root().find(".dropdown-menu");
  }

  public static SelenideElement editOwner() {
    return root().find("#app-org-link");
  }

  public static SelenideElement deleteOwnerButton() {
    return root().find("#delete-owner-link");
  }

  public static SelenideElement actionButton() {
    return root().find("button");
  }

  public static Condition reportLinkText(String stageName) {
    stageName = stageName.equals("Stage Release") ? "stage" : stageName;
    return text("View " + stageName + " report");
  }

  public static Condition disabled() {
    return cssClass("disabled");
  }

  public static String reportLinkUrl(String publicId, String scanId) {
    return Configuration.baseUrl + "new/assets/index.html#/reports/" + publicId + "/" + scanId;
  }

  public static ElementsCollection reportLinks() {
    return root().findAll("#app-report-link");
  }

  public static SelenideElement reportLink(int num) {
    return reportLinks().get(num);
  }

}
