/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selectors.byText;

public class UserActivityFilter
{
  private final SelenideElement container;

  public UserActivityFilter() {
    // Look for the filter drawer/modal by common class patterns
    this.container = $(".nx-drawer, .user-activity-filter, .filter-drawer");
  }

  public UserActivityFilter(SelenideElement container) {
    this.container = container;
  }

  public SelenideElement filterDrawer() {
    return container;
  }

  // Common filter elements
  public SelenideElement closeButton() {
    return container.$("button[aria-label*='Close'], .nx-drawer__close, .close-button, button");
  }

  public SelenideElement applyButton() {
    return container.$(byText("Apply"));
  }

  public SelenideElement resetButton() {
    return container.$(byText("Reset"));
  }

  public SelenideElement cancelButton() {
    return container.$(byText("Cancel"));
  }

  // Overview page specific filters
  public SelenideElement timeRangeSection() {
    return container.$(".time-range-section, .filter-section");
  }

  public SelenideElement timeRangeLabel() {
    return timeRangeSection().$("label, .nx-label");
  }

  public ElementsCollection timeRangeOptions() {
    return timeRangeSection().$$("input[type='radio'], .nx-radio");
  }

  public SelenideElement timeRangeOption24Hours() {
    return container.$(byText("Past 24 hours"));
  }

  public SelenideElement timeRangeOption7Days() {
    return container.$(byText("Past 7 days"));
  }

  public SelenideElement timeRangeOption30Days() {
    return container.$(byText("Past 30 days"));
  }

  // Details page specific filters
  public SelenideElement activityTypeSection() {
    return container.$(byText("Activity Type")).parent();
  }

  public SelenideElement activityTypeDropdown() {
    return activityTypeSection().$("select, .nx-dropdown");
  }

  public SelenideElement domainSection() {
    return container.$(byText("Domain")).parent();
  }

  public SelenideElement domainDropdown() {
    return domainSection().$("select, .nx-dropdown");
  }

  public SelenideElement errorStatusSection() {
    return container.$(byText("Error Status")).parent();
  }

  public SelenideElement errorStatusDropdown() {
    return errorStatusSection().$("select, .nx-dropdown");
  }

  // Filter interaction methods
  public void selectTimeRange24Hours() {
    timeRangeOption24Hours().click();
  }

  public void selectTimeRange7Days() {
    timeRangeOption7Days().click();
  }

  public void selectTimeRange30Days() {
    timeRangeOption30Days().click();
  }

  public void selectActivityType(String activityType) {
    activityTypeDropdown().selectOption(activityType);
  }

  public void selectDomain(String domain) {
    domainDropdown().selectOption(domain);
  }

  public void selectErrorStatus(String errorStatus) {
    errorStatusDropdown().selectOption(errorStatus);
  }

  public void applyFilters() {
    applyButton().click();
  }

  public void resetFilters() {
    resetButton().click();
  }

  public void cancelFilters() {
    cancelButton().click();
  }

  public void closeFilter() {
    closeButton().click();
  }

  // Validation methods
  public boolean isOpen() {
    return container.isDisplayed();
  }

  public boolean isClosed() {
    return !container.isDisplayed();
  }

  public String getSelectedTimeRange() {
    try {
      return timeRangeOptions().stream()
          .filter(SelenideElement::isSelected)
          .findFirst()
          .map(SelenideElement::getValue)
          .orElse(null);
    }
    catch (Exception e) {
      return null;
    }
  }

  public String getSelectedActivityType() {
    return activityTypeDropdown().getSelectedOption().text();
  }

  public String getSelectedDomain() {
    return domainDropdown().getSelectedOption().text();
  }

  public String getSelectedErrorStatus() {
    return errorStatusDropdown().getSelectedOption().text();
  }

  // Helper to wait for filter to be open/closed
  public void waitForOpen() {
    container.shouldBe(com.codeborne.selenide.Condition.visible);
  }

  public void waitForClosed() {
    container.shouldBe(com.codeborne.selenide.Condition.hidden);
  }
}
