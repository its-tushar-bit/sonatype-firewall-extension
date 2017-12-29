/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ConstraintSection
{
  private static final String rootSelector = "#policy-edit-constraints";

  public ElementsCollection constraintSummaries() {
    return $$(rootSelector + " .constraint-summary");
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

    public Dropdown operator() {
      return new Dropdown("#editor-constraint-operator-" + index);
    }

    public ElementsCollection conditions() {
      return $$(rootSelector + " .policy-conditions .policy-condition");
    }

    public SelenideElement addConditionButton() {
      return $(rootSelector + " .add-condition-button");
    }

    public ConditionEditSection<?> condition(int i) {
      return new ConditionEditSection<>(rootSelector, ".policy-conditions .policy-condition", nthChild(i + 1));
    }

    public AgeConditionEditSection ageCondition(int i) {
      return new AgeConditionEditSection(rootSelector, ".policy-conditions .policy-condition", nthChild(i + 1));
    }

    public DropdownConditionEditSection dropdownCondition(int i) {
      return new DropdownConditionEditSection(rootSelector, ".policy-conditions .policy-condition", nthChild(i + 1));
    }

    public InputConditionEditSection inputCondition(int i) {
      return new InputConditionEditSection(rootSelector, ".policy-conditions .policy-condition", nthChild(i + 1));
    }

    public CoordinatesCondition coordinatesCondition(int i) {
      return new CoordinatesCondition(rootSelector, ".policy-conditions .policy-condition", nthChild(i + 1));
    }

    public static class ConditionEditSection<T>
        extends BasicElement<ConditionEditSection<T>>
    {

      public ConditionEditSection(String... rootSelectors) {
        super(rootSelectors);
      }

      public Dropdown type() {
        return new Dropdown(childSelector(".condition-type"));
      }

      public Dropdown operator() {
        return new Dropdown(childSelector(".condition-operator"));
      }

      public SelenideElement deleteConditionButton() {
        return child(".delete-condition-button");
      }
    }

    public static class DropdownConditionEditSection
        extends ConditionEditSection<DropdownConditionEditSection>
    {

      public DropdownConditionEditSection(final String... rootSelector) {
        super(rootSelector);
      }

      public Dropdown value() {
        return new Dropdown(childSelector(".condition-value"));
      }
    }

    public static class AgeConditionEditSection
        extends ConditionEditSection<AgeConditionEditSection>
    {

      public AgeConditionEditSection(final String... rootSelector) {
        super(rootSelector);
      }

      public AgeInput value() {
        return new AgeInput(childSelector(".condition-value"));
      }
    }

    public static class InputConditionEditSection
        extends ConditionEditSection<InputConditionEditSection>
    {

      public InputConditionEditSection(final String... rootSelector) {
        super(rootSelector);
      }

      public SelenideElement value() {
        return child(".condition-value input");
      }
    }

    public static class CoordinatesCondition
        extends ConditionEditSection<CoordinatesCondition>
    {
      public CoordinatesCondition(String... rootSelector) {
        super(rootSelector);
      }

      public void setOperator(String op) {
        // this isn't great perf
        Dropdown typeDropdown = operator();
        typeDropdown.selectedItem().click();
        typeDropdown.listItems().findBy(text(op)).click();
      }

      public Dropdown format() {
        return new Dropdown(childSelector(".condition-value", "dropdown-selector"));
      }

      public SelenideElement groupId() {
        return child(".condition-value", "input[name*=\"groupid\"]");
      }

      public SelenideElement artifactId() {
        return child(".condition-value", "input[name*=\"artifactid\"]");
      }

      public SelenideElement name() {
        return child(".condition-value", "input[name*=\"name\"]");
      }

      public SelenideElement qualifier() {
        return child(".condition-value", "input[name*=\"qualifier\"]");
      }

      public SelenideElement version() {
        return child(".condition-value", "input[name*=\"version\"]");
      }

      public SelenideElement extension() {
        return child(".condition-value", "input[name*=\"extension\"]");
      }

      public SelenideElement classifier() {
        return child(".condition-value", "input[name*=\"classifier\"]");
      }

      public SelenideElement value() {
        return child(".condition-value", ".condition-value input");
      }
    }
  }
}
