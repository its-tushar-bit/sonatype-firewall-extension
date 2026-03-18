/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.ReportCip;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class WaiverCip
    extends ReportCip
{
  public static class AddWaiverDialog
  {
    public static SelenideElement root() {
      return $("#add-waiver-modal");
    }

    public static SelenideElement waiveViolationOnly() {
      return $("#waiver-scope-violation-only");
    }

    public static SelenideElement scopedWaiver() {
      return $("#waiver-scope-scoped");
    }

    public static SelenideElement policyName() {
      return $("#waiver-policy-name");
    }

    public static SelenideElement constraintName() {
      return $("#waiver-constraint-name");
    }

    public static SelenideElement waiverConditions() {
      return $("#waiver-conditions");
    }

    public static SelenideElement allComponents() {
      return scopeContainer().findAll("label.radio").find(text("all components")).find("input");
    }

    // input for the application of the waiver (selectedComponent or allComponents)
    public static ElementsCollection apply() {
      return scopeContainer().findAll("input[name='waiver-hash']");
    }

    public static SelenideElement cancelButton() {
      return root().find("button:nth-child(2)");
    }

    public static SelenideElement comment() {
      return root().find("textarea");
    }

    public static SelenideElement saveButton() {
      return root().find("button.btn-primary");
    }

    public static SelenideElement waiverOwner() {
      return $("#waiver-owner");
    }

    public static ElementsCollection waiverOwnerOptions() {
      return waiverOwner().findAll("option");
    }

    public static ElementsCollection scope() {
      return scopeContainer().findAll("input[name='waiverSelectedTarget']");
    }

    public static SelenideElement scope(String name) {
      return scope().find(value(name));
    }

    public static SelenideElement scopeContainer() {
      return $("#add-waiver-scope");
    }

    public static SelenideElement selectedComponent() {
      return scopeContainer().find("label.radio", 0).find("input");
    }

    public static SelenideElement selectedScope() {
      return scope().find(selected);
    }
  }

  public static class ConfirmRemoveWaiverDialog
  {
    public static SelenideElement cancelButton() {
      return $("#cancel-remove-waiver");
    }

    public static SelenideElement removeButton() {
      return $("#confirm-remove-waiver");
    }
  }

  public static class ExistingWaiver
  {
    private SelenideElement element;

    ExistingWaiver(SelenideElement element) {
      this.element = element;
    }

    public SelenideElement comment() {
      return element.find("td:nth-child(4)");
    }

    public SelenideElement created() {
      return element.find("td:nth-child(2)");
    }

    public SelenideElement owner() {
      return element.find("td:nth-child(3)");
    }

    public SelenideElement policy() {
      return element.find("td:nth-child(1)");
    }

    public SelenideElement removeButton() {
      return element.find(".tm-remove-waiver");
    }
  }

  public static class PolicyWaiverRow
      extends BasicElement<PolicyWaiverRow>
  {
    PolicyWaiverRow(String... selectors) {
      super(selectors);
    }

    public SelenideElement policyName() {
      return $(createSelector(selector, "td:nth-child(1)"));
    }

    public ElementsCollection actions() {
      return $$(createSelector(selector, "td:nth-child(1)", "ul li"));
    }

    public ElementsCollection constraints() {
      return $$(createSelector(selector, "td:nth-child(2) b"));
    }

    public ElementsCollection conditions() {
      return $$(createSelector(selector, "td:nth-child(3) > div > div"));
    }

    public SelenideElement waiveButton() {
      return $(createSelector(selector, ".btn-primary"));
    }

    public SelenideElement requestWaiverButton() {
      return $(createSelector(selector, ".btn-secondary"));
    }

    public void shouldBe(
        String cssClass,
        String policyName,
        String[] expectedConstraints,
        String[] expectedConditions)
    {
      policyName().shouldHave(text(policyName));
      constraints().shouldHave(texts(expectedConstraints));
      conditions().shouldHave(texts(expectedConditions));
      policyName().shouldHave(cssClass(cssClass));
    }
  }

  public static class ViewWaiversDialog
  {
    public static SelenideElement closeButton() {
      return $("#close-component-existing-waivers");
    }

    public static SelenideElement emptyText() {
      return $("#no-waivers-assigned");
    }

    public static ExistingWaiver row(int num) {
      return new ExistingWaiver(rows().get(num));
    }

    public static ElementsCollection rows() {
      return $$("#component-existing-waiver-modal tbody tr");
    }
  }

  public static class UnquarantineDialog
  {
    private static final String ROOT = "#release-quarantine-modal";

    public static SelenideElement releaseButton() {
      return $(createSelector(ROOT, ".btn.btn-primary"));
    }
  }

  private static String[] getRowSelector() {
    return new String[]{CONTAINER_ID, ".cip-policy-table tbody tr"};
  }

  public static PolicyWaiverRow row(int num) {
    return new PolicyWaiverRow(createSelector(getRowSelector()), nthChild(num + 1));
  }

  public static ElementsCollection rows() {
    return $$(createSelector(getRowSelector()));
  }

  public static SelenideElement viewWaivers() {
    return $("#view-existing-waivers");
  }

  public static SelenideElement viewTransitiveViolations() {
    return $("#view-transitive-violations");
  }

  public static SelenideElement unquarantineButton() {
    return $(createSelector(CONTAINER_ID, "a.btn"));
  }

  public static class RequestWaiverDialog
  {
    public static SelenideElement explanatoryText() {
      return $("#request-waiver-explanatory-text");
    }

    public static SelenideElement policyName() {
      return $("#request-waiver-policy-name");
    }

    public static SelenideElement constraintName() {
      return $("#request-waiver-constraint-name");
    }

    public static SelenideElement waiverConditions() {
      return $("#request-waiver-conditions");
    }

    public static SelenideElement policyViolationId() {
      return $("#request-waiver-policy-violation-id");
    }

    public static SelenideElement policyViolationPageLink() {
      return $(".request-waiver-policy-violation-page-link > a");
    }

    public static SelenideElement policyCurlExample() {
      return $("#request-waiver-curl-example");
    }

    public static SelenideElement closeButton() {
      return $("#request-waiver-modal-close-btn");
    }
  }
}
