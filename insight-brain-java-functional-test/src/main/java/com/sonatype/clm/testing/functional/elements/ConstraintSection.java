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

  public ElementsCollection constraintEditors() {
    return $$(rootSelector + " .constraint-editor");
  }

  public ConstraintEditSection constraintEditor(int i) {
    return new ConstraintEditSection(rootSelector + "  .constraint:nth-child(" + (i + 1) + ") .constraint-editor", i);
  }

  public SelenideElement addConstraintButton() {
    return $("#add-constraint-button");
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

    public SelenideElement editConstraintButton() {
      return $(rootSelector + " .edit-constraint-button");
    }

    public SelenideElement deleteConstraintButton() {
      return $(rootSelector + " .delete-constraint-button");
    }

    public ElementsCollection conditions() {
      return $$(rootSelector + " .constraint-summary-condition");
    }

    public SelenideElement condition(int i) {
      return $(rootSelector + " .constraint-summary-condition:nth-child(" + (i + 4) + ")");
    }
  }

  public static class ConstraintEditSection
  {
    private String rootSelector;

    private int index;

    public ConstraintEditSection(String rootSelector, int index) {
      this.rootSelector = rootSelector;
      this.index = index;
    }

    public SelenideElement name() {
      return $("#editor-constraint-name-" + index);
    }

    public DropdownSelector operator() {
      return new DropdownSelector($("#editor-constraint-operator-" + index));
    }

    public ElementsCollection conditions() {
      return $$(rootSelector + " table tr.editor-condition");
    }

    public SelenideElement addConditiontButton() {
      return $(rootSelector + " .add-condition-button");
    }

    public ConditionEditSection condition(int i) {
      return new ConditionEditSection(rootSelector + " table tr.editor-condition:nth-child(" + (i + 1) + ")");
    }

    public AgeConditionEditSection ageCondition(int i) {
      return new AgeConditionEditSection(rootSelector + " table tr.editor-condition:nth-child(" + (i + 1) + ")");
    }

    public DropdownConditionEditSection dropdownCondition(int i) {
      return new DropdownConditionEditSection(rootSelector + " table tr.editor-condition:nth-child(" + (i + 1) + ")");
    }

    public InputConditionEditSection inputCondition(int i) {
      return new InputConditionEditSection(rootSelector + " table tr.editor-condition:nth-child(" + (i + 1) + ")");
    }

    public static class ConditionEditSection
    {
      protected String rootSelector;

      public ConditionEditSection(String rootSelector) {
        this.rootSelector = rootSelector;
      }

      public DropdownSelector type() {
        return new DropdownSelector($(rootSelector + " .editor-condition-type"));
      }

      public DropdownSelector operator() {
        return new DropdownSelector($(rootSelector + " .editor-condition-operator"));
      }

      public SelenideElement deleteConditionButton() {
        return $(rootSelector + " .delete-condition-button");
      }
    }

    public static class DropdownConditionEditSection
        extends ConditionEditSection
    {

      public DropdownConditionEditSection(final String rootSelector) {
        super(rootSelector);
      }

      public DropdownSelector value() {
        return new DropdownSelector($(rootSelector + " .editor-condition-value"));
      }
    }

    public static class AgeConditionEditSection
        extends ConditionEditSection
    {

      public AgeConditionEditSection(final String rootSelector) {
        super(rootSelector);
      }

      public AgeInput value() {
        return new AgeInput(rootSelector + " .editor-condition-value");
      }
    }

    public static class InputConditionEditSection
        extends ConditionEditSection
    {

      public InputConditionEditSection(final String rootSelector) {
        super(rootSelector);
      }

      public SelenideElement value() {
        return $(rootSelector + " .editor-condition-value input");
      }
    }
  }
}
