/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class ReportCip
{
  protected static String CONTAINER_ID = "#informationPanel";

  public static void close() {
    $(CONTAINER_ID + " .close").click();
  }

  private static ElementsCollection tabs() {
    return $$(CONTAINER_ID + " li a");
  }

  public static SelenideElement componentInfoTab() {
    return tabs().findBy(text("Component Info"));
  }

  public static SelenideElement policyTab() {
    return tabs().findBy(text("Policy"));
  }

  public static SelenideElement labelsTab() {
    return tabs().findBy(text("Labels"));
  }
}
