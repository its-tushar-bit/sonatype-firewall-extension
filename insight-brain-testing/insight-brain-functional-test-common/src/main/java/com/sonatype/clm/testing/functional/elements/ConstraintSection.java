/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ConstraintSection
{
  private static final String rootSelector = "#policy-edit-constraints";

  public ElementsCollection constraintSummaries() {
    return $$(rootSelector + " .constraint-editor__item");
  }

  public ConstraintSummary constraintSummary(int i) {
    return new ConstraintSummary(
        rootSelector + " .constraint-editor__item:nth-child(" + (i + 1) + ")");
  }

  public ElementsCollection constraintEditors() {
    return $$(rootSelector + " .constraint-editor__item--editable");
  }

  public ConstraintEditSection constraintEditor(int i) {
    return new ConstraintEditSection(
        rootSelector + " .constraint-editor__item--editable:nth-child(" + (i + 1) + ")", i);
  }

  public SelenideElement addConstraintButton() {
    return $(".constraint-editor__add-constraint-btn");
  }

  public SelenideElement header() {
    return $(rootSelector + " .nx-h2");
  }

  public SelenideElement getInputValidationElement(SelenideElement element) {
    return element.closest(".nx-text-input").find(".nx-field-validation-message");
  }

  public static class ConstraintSummary
  {
    @SuppressWarnings("hiding")
    private String rootSelector;

    public static WebElementCondition subheaderText(int numConstraints, String operator) {
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
      return $(rootSelector + " .nx-list__text");
    }

    public SelenideElement subheader() {
      return $(rootSelector + " .nx-list__subtext");
    }

    public SelenideElement editConstraintButton() {
      return $(rootSelector + " .constraint-editor__edit-btn");
    }

    public SelenideElement deleteConstraintButton() {
      return $(rootSelector + " .constraint-editor__delete-btn");
    }

    public ElementsCollection conditions() {
      return $$(rootSelector + " .nx-list__item");
    }

    public SelenideElement condition(int i) {
      return $(rootSelector + " .nx-list__item:nth-of-type(" + (i + 1) + ")");
    }

    public ElementsCollection conditionUnsupportedMessages() {
      return $$(rootSelector + " .nx-alert--error");
    }

    public SelenideElement conditionUnsupportedMessage() {
      return $(rootSelector + " .nx-alert--error .nx-alert__content");
    }
  }

  public static class ConstraintEditSection
  {
    @SuppressWarnings("hiding")
    private String rootSelector;

    private int index;

    public ConstraintEditSection(String rootSelector, int index) {
      this.rootSelector = rootSelector;
      this.index = index;
    }

    public SelenideElement name() {
      return $("#editor-constraint-name-" + index);
    }

    public NxFormSelect operator() {
      return new NxFormSelect("#editor-constraint-operator-" + index);
    }

    public ElementsCollection conditions() {
      return $$(rootSelector + " .constraint-editor__conditions .nx-list__item");
    }

    public SelenideElement addConditionButton() {
      return $(rootSelector + " .constraint-editor__add-condition-btn");
    }

    public ConditionEditSection<?> condition(int i) {
      return new ConditionEditSection<>(
          rootSelector,
          ".constraint-editor__conditions .nx-list__item",
          nthChild(i + 1));
    }

    public AgeConditionEditSection ageCondition(int i) {
      return new AgeConditionEditSection(
          rootSelector + " .constraint-editor__conditions .nx-list__item"
              + nthChild(i + 1));
    }

    public DropdownConditionEditSection dropdownCondition(int i) {
      return new DropdownConditionEditSection(rootSelector,
          ".constraint-editor__conditions .nx-list__item", nthChild(i + 1));
    }

    public InputConditionEditSection inputCondition(int i) {
      return new InputConditionEditSection(
          rootSelector,
          ".constraint-editor__conditions .nx-list__item",
          nthChild(i + 1));
    }

    public CoordinatesCondition coordinatesCondition(int i) {
      return new CoordinatesCondition(
          rootSelector,
          ".constraint-editor__conditions .nx-list__item",
          nthChild(i + 1));
    }

    public ElementsCollection conditionUnsupportedMessages() {
      return $$(rootSelector + " .nx-alert--error");
    }

    public SelenideElement conditionUnsupportedMessage() {
      return $(rootSelector + " .nx-alert--error");
    }

    public static class ConditionEditSection<T>
        extends BasicElement<ConditionEditSection<T>>
    {
      public ConditionEditSection(String... rootSelectors) {
        super(rootSelectors);
      }

      public NxFormSelect type() {
        return new NxFormSelect(childSelector(".constraint-editor__condition-type .nx-form-select__select"));
      }

      public NxFormSelect operator() {
        return new NxFormSelect(childSelector(".constraint-editor__condition-operator .nx-form-select__select"));
      }

      public SelenideElement deleteConditionButton() {
        return child(".constraint-editor__delete-condition-btn");
      }
    }

    public static class DropdownConditionEditSection
        extends ConditionEditSection<DropdownConditionEditSection>
    {
      public DropdownConditionEditSection(final String... rootSelector) {
        super(rootSelector);
      }

      public NxFormSelect value() {
        return new NxFormSelect(childSelector(".constraint-editor__values .nx-form-select__select"));
      }
    }

    public static class AgeConditionEditSection
        extends ConditionEditSection<AgeConditionEditSection>
    {
      private String rootSelector;

      public AgeConditionEditSection(final String rootSelector) {
        super(rootSelector);
        this.rootSelector = rootSelector;
      }

      public AgeInput value() {
        return new AgeInput(this.rootSelector);
      }
    }

    public static class InputConditionEditSection
        extends ConditionEditSection<InputConditionEditSection>
    {
      public InputConditionEditSection(final String... rootSelector) {
        super(rootSelector);
      }

      public SelenideElement value() {
        return child(".constraint-editor__values--input input");
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
        NxFormSelect typeDropdown = operator();
        typeDropdown.click();
        typeDropdown.listItems().findBy(text(op)).click();
      }

      public NxFormSelect format() {
        return new NxFormSelect(childSelector(".constraint-editor__coordinate-format"));
      }

      public SelenideElement groupId() {
        return child(".constraint-editor__values--input", "input[name*=\"groupId\"]");
      }

      public SelenideElement artifactId() {
        return child(".constraint-editor__values--input", "input[name*=\"artifactId\"]");
      }

      public SelenideElement name() {
        return child(".constraint-editor__values--input", "input[name*=\"name\"]");
      }

      public SelenideElement qualifier() {
        return child(".constraint-editor__values--input", "input[name*=\"qualifier\"]");
      }

      public SelenideElement version() {
        return child(".constraint-editor__values--input", "input[name*=\"version\"]");
      }

      public SelenideElement extension() {
        return child(".constraint-editor__values--input", "input[name*=\"extension\"]");
      }

      public SelenideElement classifier() {
        return child(".constraint-editor__values--input", "input[name*=\"classifier\"]");
      }

      public SelenideElement value() {
        return child(".constraint-editor__values--input", ".constraint-editor__values--input input");
      }
    }
  }
}
