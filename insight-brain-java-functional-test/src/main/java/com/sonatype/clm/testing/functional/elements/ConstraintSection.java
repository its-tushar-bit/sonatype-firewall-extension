/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class ConstraintSection
    extends PolicyEditorSection
{
  private static final String rootSelector = "#policy-edit-constraints";

  public ConstraintSection() {
    super($(rootSelector));
  }

  public ElementsCollection constraintSummaries() {
    return root.$$(".constraint-summary");
  }

  public ConstraintSummary constraintSummary(int i) {
    return new ConstraintSummary(rootSelector + "  .constraint:nth-child(" + (i + 1) + ") .constraint-summary");
  }

  public SelenideElement createConstraintButton() {
    return root.$("#add-constraint-button");
  }

  public static class ConstraintSummary
  {
    private String rootSelector;

    public static Condition subheaderText(int numConstraints, String operator) {
      if (numConstraints > 1) {
        return text("is in violation if " + (operator.equals("OR") ? "any" : "all") + " of the following are true:");
      }
      else {
        return text("is in violation if the following is true:");
      }
    }

    public ConstraintSummary(String rootSelector) {
      this.rootSelector = rootSelector;
    }

    public SelenideElement name() {
      return $(rootSelector + " .constraint-summary-name");
    }

    public SelenideElement subheader() {
      return $(rootSelector + " .constraint-summary-subheader");
    }

    public ElementsCollection conditions() {
      return $$(rootSelector + " .constraint-summary-condition");
    }

    public SelenideElement condition(int i) {
      return $(rootSelector + " .constraint-summary-condition:nth-child(" + (i + 4) + ")");
    }
  }
}
