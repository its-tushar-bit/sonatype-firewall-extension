/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.ConstraintSection;
import com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection;
import com.sonatype.clm.testing.functional.elements.Radio;
import com.sonatype.clm.testing.functional.elements.SummarySection;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class PolicyEditorPage
{
  public static final int DEFAULT_THREAT_LEVEL = 5;

  public static String urlToEdit(OwnerType ownerType, String ownerId, String policyId) {
    return urlToEdit(ownerType.toString(), ownerId, policyId);
  }

  public static String urlToEdit(String ownerType, String ownerId, String policyId) {
    return urlToCreate(ownerType, ownerId) + "/" + policyId;
  }

  public static String urlToCreate(OwnerType ownerType, String ownerId) {
    return urlToCreate(ownerType.toString(), ownerId);
  }

  public static String urlToCreate(String ownerType, String ownerId) {
    return BaseUrl.uriBuilder().fragment("/management/edit/{ownerType}/{ownerId}/policy").build(ownerType, ownerId)
        .toString();
  }

  public static SelenideElement title() {
    return $("#policy-editor-summary h2");
  }

  public static SelenideElement saveButton() {
    return $("#save-policy-button");
  }

  public static SelenideElement deleteButton() {
    return $("#delete-policy-button");
  }

  public static SelenideElement constraintsPill() {
    return $("#policy-constraints-button");
  }

  public static SelenideElement actionsPill() {
    return $("#policy-actions-button");
  }

  public static SelenideElement inhertancePill() {
    return $("#policy-inheritance-button");
  }

  public static SelenideElement endOfPagePill() {
    return $("#policy-endofpage-button");
  }

  public static SummarySection summarySection() {
    return new SummarySection();
  }

  public static ConstraintSection constraintSection() {
    return new ConstraintSection();
  }

  public static PolicyInheritsToSection inheritanceSection() {
    return new PolicyInheritsToSection();
  }

  public static ActionsTable actionsTable() {
    return new ActionsTable();
  }

  public static class ActionsTable
  {
    public static final String ROOT_SELECTOR = "#edit-policy-actions-table";

    public Stage proxy() {
      return new Stage(ROOT_SELECTOR, 2);
    }

    public Stage develop() {
      return new Stage(ROOT_SELECTOR, 3);
    }

    public Stage build() {
      return new Stage(ROOT_SELECTOR, 4);
    }

    public Stage stageRelease() {
      return new Stage(ROOT_SELECTOR, 5);
    }

    public Stage release() {
      return new Stage(ROOT_SELECTOR, 6);
    }

    public Stage operate() {
      return new Stage(ROOT_SELECTOR, 7);
    }

    public static Condition warnClass() {
      return Condition.cssClass("warn");
    }

    public static Condition activeClass() {
      return Condition.cssClass("active");
    }

    public static class Stage
    {
      private String rootSelector;
      private int columnNumber;

      public Stage(String rootSelector, int columnNumber) {
        this.rootSelector = rootSelector;
        this.columnNumber = columnNumber;
      }

      public Radio noActionRadio() {
        return new Radio($(createSelector(rootSelector, "tr", nthChild(1), "td", nthChild(columnNumber), ".radio")));
      }

      public Radio warnRadio() {
        return new Radio($(createSelector(rootSelector, "tr", nthChild(2), "td", nthChild(columnNumber), ".radio")));
      }

      public Radio failRadio() {
        return new Radio($(createSelector(rootSelector, "tr", nthChild(3), "td", nthChild(columnNumber), ".radio")));
      }

      public SelenideElement header() {
        return $(createSelector(ROOT_SELECTOR, "th", nthChild(columnNumber)));
      }

      public ElementsCollection cells() {
        return $$(By.xpath("//table[@id='edit-policy-actions-table']//td[" + columnNumber + "]"));
      }
    }
  }
}
