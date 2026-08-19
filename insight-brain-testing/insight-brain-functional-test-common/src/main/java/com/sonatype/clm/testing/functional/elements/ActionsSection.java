/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ActionsSection
{
  public static final String ROOT_SELECTOR = "#policy-edit-actions";

  public static final String ACTIONS_TABLE_ROOT_SELECTOR = "#edit-policy-actions-table";

  public SelenideElement title() {
    return $(createSelector(ROOT_SELECTOR, ".nx-h2"));
  }

  public SelenideElement header() {
    return $(ROOT_SELECTOR + " .nx-h2");
  }

  public SelenideElement paragraph() {
    return $(createSelector(ROOT_SELECTOR, "p"));
  }

  public SelenideElement table() {
    return $(ACTIONS_TABLE_ROOT_SELECTOR);
  }

  public ElementsCollection tableRows() {
    return $$(createSelector(ACTIONS_TABLE_ROOT_SELECTOR, ".nx-table-row"));
  }

  public Stage proxy() {
    return new Stage(ACTIONS_TABLE_ROOT_SELECTOR, "proxy");
  }

  public Stage develop() {
    return new Stage(ACTIONS_TABLE_ROOT_SELECTOR, "develop");
  }

  public Stage source() {
    return new Stage(ACTIONS_TABLE_ROOT_SELECTOR, "source");
  }

  public Stage build() {
    return new Stage(ACTIONS_TABLE_ROOT_SELECTOR, "build");
  }

  public Stage stageRelease() {
    return new Stage(ACTIONS_TABLE_ROOT_SELECTOR, "stage");
  }

  public Stage release() {
    return new Stage(ACTIONS_TABLE_ROOT_SELECTOR, "release");
  }

  public Stage operate() {
    return new Stage(ACTIONS_TABLE_ROOT_SELECTOR, "operate");
  }

  public Stage compliance() {
    return new Stage(ACTIONS_TABLE_ROOT_SELECTOR, "compliance");
  }

  public static WebElementCondition warnClass() {
    return Condition.cssClass("warn");
  }

  public static WebElementCondition activeClass() {
    return Condition.cssClass("active");
  }

  public ElementsCollection headers() {
    return $$(createSelector(ACTIONS_TABLE_ROOT_SELECTOR, "th:nth-child(n+2):nth-child(-n+8)"));
  }

  public SelenideElement quarantineWarningMessage() {
    return $("#quarantine-warning-message");
  }

  public NxRadio inheritParentActions() {
    return new NxRadio($("#edit-policy-actions-override-inherit"));
  }

  public NxRadio overrideParentActions() {
    return new NxRadio($("#edit-policy-actions-override-override"));
  }

  public SelenideElement actionsOverrideSection() {
    return $(createSelector("#edit-policy-actions-override"));
  }

  public static class Stage
  {
    private String rootSelector;

    private String stageName;

    public Stage(String rootSelector, String stageName) {
      this.rootSelector = rootSelector;
      this.stageName = stageName;
    }

    public NxRadio noActionRadio() {
      return new NxRadio($(createSelector(rootSelector, "tr", nthChild(1), "td.", stageName, ".nx-radio")));
    }

    public NxRadio warnRadio() {
      return new NxRadio($(createSelector(rootSelector, "tr", nthChild(2), "td.", stageName, ".nx-radio")));
    }

    public NxRadio failRadio() {
      return new NxRadio($(createSelector(rootSelector, "tr", nthChild(3), "td.", stageName, ".nx-radio")));
    }

    public SelenideElement header() {
      return $(createSelector(ACTIONS_TABLE_ROOT_SELECTOR, "th.", stageName));
    }

    public ElementsCollection cells() {
      return $$(createSelector(rootSelector, "td.", stageName));
    }
  }
}
