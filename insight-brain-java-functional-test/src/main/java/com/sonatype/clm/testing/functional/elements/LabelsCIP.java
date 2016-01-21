/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.insight.brain.model.Color;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.selector;

public class LabelsCIP
{
  private static final String CLM_LABEL = ".clmLabel";

  private static final String AVAILABLE_LABELS = "#available-labels";

  private static final String APPLIED_LABELS = "#applied-labels";

  private static final String REFRESH_ICON = ".icon-refresh";

  public static Label appliedLabel(int index) {
    return new Label(selector(APPLIED_LABELS, CLM_LABEL, nthChild(index)));
  }

  public static ElementsCollection appliedLabels() {
    return $$(selector(APPLIED_LABELS, CLM_LABEL));
  }

  public static SelenideElement refreshAppliedButton() {
    return $(selector(APPLIED_LABELS, REFRESH_ICON));
  }

  public static Label availableLabel(int index) {
    return new Label(selector(AVAILABLE_LABELS, CLM_LABEL, nthChild(index)));
  }

  public static SelenideElement availableLabelsContainer() {
    return $(AVAILABLE_LABELS);
  }

  public static ElementsCollection availableLabels() {
    return $$(selector(AVAILABLE_LABELS, CLM_LABEL));
  }

  public static SelenideElement refreshAvailableButton() {
    return $(selector(AVAILABLE_LABELS, REFRESH_ICON));
  }

  public static class Label
      extends BasicElement<Label>
  {
    public static Condition color(Color color) {
      return cssClass(color.toValue() + "Label");
    }

    Label(String selector) {
      super(selector);
    }

    public SelenideElement action() {
      return $(selector(this.selector, "i"));
    }
  }

  public static class AddLabelModal
  {
    private static String ROOT = "#labelAssignScopeModal";

    public static SelenideElement root() {
      return $(ROOT);
    }

    public static SelenideElement cancelButton() {
      return $(selector(ROOT, ".btn:not(.btn-primary)"));
    }

    public static SelenideElement saveButton() {
      return $(selector(ROOT, ".btn-primary"));
    }

    public static ElementsCollection scopes() {
      return $$(selector(ROOT, ".radio"));
    }

    public static SelenideElement radio(int i) {
      return $(selector(ROOT, ".radio", nthChild(i), "input"));
    }
  }

  public static class RemoveLabelModal
  {
    private static String ROOT = "#labelRemoveModal";

    public static SelenideElement root() {
      return $(ROOT);
    }

    public static SelenideElement confirmButton() {
      return $(selector(ROOT, ".btn-danger"));
    }
  }
}
