/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.reports;

import java.util.List;

import com.sonatype.clm.testing.functional.widget.MultiSelect;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.ex.UIAssertionError;
import com.codeborne.selenide.impl.CollectionSource;
import com.codeborne.selenide.impl.Describe;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.WebElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;

public class LicenseCIP
{
  private static final String ROOT_ID = "#license-editor";

  public static ElementsCollection declaredLicenses() {
    return $$("#declaredLicenseBlock > li > div");
  }

  public static ElementsCollection observedLicenses() {
    return $$("#observedLicenseBlock > li > div");
  }

  public static ElementsCollection effectiveLicenses() {
    return $$("#effectiveLicenseBlock > li > div");
  }

  public static ElementsCollection linkToALP() {
    return $$("#link-to-alp");
  }

  public static SelenideElement linkALP() {
    return $("#link-to-alp").find("a");
  }

  public static SelenideElement scope() {
    return $(createSelector(ROOT_ID, "select[name=scope]"));
  }

  public static ElementsCollection scopes() {
    return $$(createSelector(ROOT_ID, "select[name=scope]", "option"));
  }

  public static SelenideElement status() {
    return $(createSelector(ROOT_ID, "select[name=status]"));
  }

  public static ElementsCollection statuses() {
    return $$(createSelector(ROOT_ID, "select[name=status]", "option"));
  }

  public static MultiSelect licenseSelector() {
    return new MultiSelect(createSelector(ROOT_ID, ".multi-dropdown"));
  }

  public static SelenideElement comment() {
    return $(createSelector(ROOT_ID, "textarea[name=comment]"));
  }

  public static SelenideElement updateButton() {
    return $(createSelector(ROOT_ID, "button[type=submit]"));
  }

  public static CollectionCondition licenseThreats(final Integer... expectedThreats) {
    return new CollectionCondition()
    {
      @Override
      public boolean missingElementSatisfiesCondition() {
        return false;
      }

      private Integer missingClassIndex;

      @Override
      public boolean test(List<WebElement> elements) {
        missingClassIndex = null;
        if (elements.size() != expectedThreats.length) {
          return false;
        }

        for (int i = 0; i < expectedThreats.length; i++) {
          WebElement element = elements.get(i);
          if (!Condition.cssClass(convertToCssClass(expectedThreats[i])).apply(WebDriverRunner.driver(), element)) {
            missingClassIndex = i;
            return false;
          }
        }
        return true;
      }

      @Override
      public void fail(
          final CollectionSource collection,
          final CheckResult lastCheckResult,
          @Nullable final Exception cause,
          final long timeoutMs)
      {
        if (missingClassIndex != null) {
          Driver driver = WebDriverRunner.driver();

          String description = new Describe(driver, collection.getElements().get(missingClassIndex))
              .toString();

          throw new UIAssertionError(driver, "Failed to locate CSS class: "
              + convertToCssClass(expectedThreats[missingClassIndex]) + " on "
              + description)
          {
          };
        }
      }

      private String convertToCssClass(Integer threatLevel) {
        if (threatLevel == null) {
          return "unspecified";
        }
        else if (threatLevel > 7) {
          return "critical";
        }
        else if (threatLevel > 3) {
          return "severe";
        }
        else if (threatLevel > 0) {
          return "moderate";
        }
        else {
          return "none";
        }
      }
    };
  }
}
