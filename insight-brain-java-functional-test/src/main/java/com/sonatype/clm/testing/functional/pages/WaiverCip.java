/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.ReportCip;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.selector;

public class WaiverCip
    extends ReportCip
{

  public static class AddWaiverDialog
  {

    public static SelenideElement root() {
      return $("#add-waiver-modal");
    }

    public static SelenideElement allComponents() {
      return applyContainer().findAll("label.radio").find(text("all components")).find("input");
    }

    // input for the application of the waiver (selectedComponent or allComponents)
    public static ElementsCollection apply() {
      return applyContainer().findAll("input[name='waiver-hash']");
    }

    private static SelenideElement applyContainer() {
      return $("#add-waiver-apply");
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
      return applyContainer().findAll("label.radio").find(text("selected component")).find("input");
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
      return element.find("#remove-waiver");
    }
  }

  public static class PolicyWaiverRow
  {
    private SelenideElement element;

    PolicyWaiverRow(SelenideElement element) {
      this.element = element;
    }

    public SelenideElement policyName() {
      return element.find("td:nth-child(1)");
    }

    public ElementsCollection constraints() {
      return element.findAll("td:nth-child(2) b");
    }

    public ElementsCollection conditions() {
      return element.findAll("td:nth-child(3) > div > div");
    }

    public SelenideElement waiveButton() {
      return element.find(".btn-primary");
    }

    public void shouldBe(int threatLevel, String policyName, String[] expectedConstraints, String[] expectedConditions)
    {
      policyName().shouldHave(text(policyName));
      constraints().shouldHave(CollectionCondition.texts(expectedConstraints));
      conditions().shouldHave(CollectionCondition.texts(expectedConditions));
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
      return $$("#componentExistingWaiverModal tbody tr");
    }
  }

  public static class UnquarantineDialog
  {
    private static final String ROOT = "#release-quarantine-modal";

    public static SelenideElement releaseButton() {
      return $(selector(ROOT, ".btn.btn-primary"));
    }
  }

  public static PolicyWaiverRow row(int num) {
    return new PolicyWaiverRow(rows().get(num));
  }

  public static ElementsCollection rows() {
    return $$(CONTAINER_ID + " .cip-policy-table tbody tr");
  }

  public static SelenideElement viewWaivers() {
    return $("#view-existing-waivers");
  }

  public static SelenideElement unquarantineButton() {
    return $(selector(CONTAINER_ID, "a.btn"));
  }
}
