/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ActionsSection
{
  public static final String ROOT_SELECTOR = "#edit-policy-actions-table";

  public Stage proxy() {
    return new Stage(ROOT_SELECTOR, "proxy");
  }

  public Stage develop() {
    return new Stage(ROOT_SELECTOR, "develop");
  }

  public Stage source() {
    return new Stage(ROOT_SELECTOR, "source");
  }

  public Stage build() {
    return new Stage(ROOT_SELECTOR, "build");
  }

  public Stage stageRelease() {
    return new Stage(ROOT_SELECTOR, "stage");
  }

  public Stage release() {
    return new Stage(ROOT_SELECTOR, "release");
  }

  public Stage operate() {
    return new Stage(ROOT_SELECTOR, "operate");
  }

  public static Condition warnClass() {
    return Condition.cssClass("warn");
  }

  public static Condition activeClass() {
    return Condition.cssClass("active");
  }

  public ElementsCollection headers() {
    return $$(createSelector(ROOT_SELECTOR, "th:nth-child(n+2):nth-child(-n+8)"));
  }

  public SelenideElement quarantineWarningMessage() {
    return $("#quarantine-warning-message");
  }

  public static class Stage
  {
    private String rootSelector;

    private String stageName;

    public Stage(String rootSelector, String stageName) {
      this.rootSelector = rootSelector;
      this.stageName = stageName;
    }

    public IqRadio noActionRadio() {
      return new IqRadio($(createSelector(rootSelector, "tr", nthChild(1), "td.", stageName, "iq-radio")));
    }

    public IqRadio warnRadio() {
      return new IqRadio($(createSelector(rootSelector, "tr", nthChild(2), "td.", stageName, "iq-radio")));
    }

    public IqRadio failRadio() {
      return new IqRadio($(createSelector(rootSelector, "tr", nthChild(3), "td.", stageName, "iq-radio")));
    }

    public SelenideElement header() {
      return $(createSelector(ROOT_SELECTOR, "th.", stageName));
    }

    public ElementsCollection cells() {
      return $$(createSelector(rootSelector, "td.", stageName));
    }
  }
}
