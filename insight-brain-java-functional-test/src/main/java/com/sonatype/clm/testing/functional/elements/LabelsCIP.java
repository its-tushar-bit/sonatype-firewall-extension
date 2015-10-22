/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.insight.brain.model.Color;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selenide.$;

public class LabelsCIP
{
  public static SelenideElement appliedLabel(int index) {
    return appliedLabels().get(index);
  }

  public static ElementsCollection appliedLabels() {
    return $("#applied-labels").$$(".clmLabel");
  }

  public static SelenideElement refreshAppliedButton() {
    return $("#applied-labels .icon-refresh");
  }

  public static SelenideElement availableLabel(int index) {
    return availableLabels().get(index);
  }

  public static SelenideElement availableLabelsContainer() {
    return $("#available-labels");
  }

  public static ElementsCollection availableLabels() {
    return availableLabelsContainer().$$(".clmLabel");
  }

  public static SelenideElement refreshAvailableButton() {
    return $("#available-labels .icon-refresh");
  }

  public static class Label
  {
    public static Condition color(Color color) {
      return cssClass(color.toValue() + "Label");
    }
  }
}
