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

public class UserActivityTable
{
  private final SelenideElement container;

  public UserActivityTable(String tableId) {
    this.container = $("#" + tableId);
  }

  public UserActivityTable(SelenideElement container) {
    this.container = container;
  }

  public SelenideElement table() {
    return container;
  }

  // Header elements
  public SelenideElement usernameHeader() {
    return container.$(byText("Username"));
  }

  public SelenideElement loginCountHeader() {
    return container.$$("th").findBy(com.codeborne.selenide.Condition.text("Login Count"));
  }

  public SelenideElement lastActiveHeader() {
    return container.$(byText("Last Active"));
  }

  public SelenideElement timestampHeader() {
    return container.$(byText("Timestamp"));
  }

  public SelenideElement domainHeader() {
    return container.$(byText("Domain"));
  }

  public SelenideElement typeHeader() {
    return container.$(byText("Type"));
  }

  public SelenideElement errorHeader() {
    return container.$(byText("Error"));
  }

  public SelenideElement requestUriHeader() {
    return container.$(byText("Request URI"));
  }

  public SelenideElement methodHeader() {
    return container.$(byText("Method"));
  }

  public SelenideElement ipAddressHeader() {
    return container.$(byText("IP Address"));
  }

  public SelenideElement userAgentHeader() {
    return container.$(byText("User Agent"));
  }

  // Table body and rows
  public SelenideElement tableBody() {
    return container.$("tbody");
  }

  public ElementsCollection rows() {
    return container.$$("tbody tr");
  }

  public SelenideElement row(int index) {
    return rows().get(index);
  }

  public SelenideElement firstRow() {
    return row(0);
  }

  // Overview table specific methods
  public ElementsCollection userRows() {
    return container.$$("tbody tr.user-activity-row");
  }

  public SelenideElement userRow(int index) {
    return userRows().get(index);
  }

  public SelenideElement usernameCell(int rowIndex) {
    return userRow(rowIndex).$(".username-cell, td:first-child");
  }

  public SelenideElement loginCountCell(int rowIndex) {
    return userRow(rowIndex).$(".login-count-cell, td:nth-child(2)");
  }

  public SelenideElement lastActiveCell(int rowIndex) {
    return userRow(rowIndex).$(".last-active-cell, td:nth-child(3)");
  }

  public SelenideElement chevronCell(int rowIndex) {
    return userRow(rowIndex).$(".nx-cell--meta-info, td:last-child");
  }

  // Details table specific methods
  public ElementsCollection activityRows() {
    return container.$$("tbody tr.user-activity-detail-row");
  }

  public SelenideElement activityRow(int index) {
    return activityRows().get(index);
  }

  public SelenideElement timestampCell(int rowIndex) {
    return activityRow(rowIndex).$(".timestamp-cell, td:first-child");
  }

  public SelenideElement domainCell(int rowIndex) {
    return activityRow(rowIndex).$(".domain-cell, td:nth-child(2)");
  }

  public SelenideElement typeCell(int rowIndex) {
    return activityRow(rowIndex).$(".type-cell, td:nth-child(3)");
  }

  public SelenideElement errorCell(int rowIndex) {
    return activityRow(rowIndex).$(".error-cell, td:nth-child(4)");
  }

  public SelenideElement uriCell(int rowIndex) {
    return activityRow(rowIndex).$(".uri-cell, td:nth-child(5)");
  }

  public SelenideElement methodCell(int rowIndex) {
    return activityRow(rowIndex).$(".method-cell, td:nth-child(6)");
  }

  public SelenideElement ipAddressCell(int rowIndex) {
    return activityRow(rowIndex).$(".ip-address-cell, td:nth-child(7)");
  }

  public SelenideElement userAgentCell(int rowIndex) {
    return activityRow(rowIndex).$(".user-agent-cell, td:nth-child(8)");
  }

  // Common table methods
  public SelenideElement emptyMessage() {
    return container.$(".nx-table__empty-message, tbody tr td");
  }

  public SelenideElement pagination() {
    return container.parent().$(".nx-table-container__footer");
  }

  public SelenideElement previousPageButton() {
    return pagination().$("button[aria-label*='Previous'], button");
  }

  public SelenideElement nextPageButton() {
    return pagination().$("button[aria-label*='Next']");
  }

  // Sorting helpers
  public void clickUsernameHeader() {
    usernameHeader().click();
  }

  public void clickLoginCountHeader() {
    loginCountHeader().click();
  }

  public void clickLastActiveHeader() {
    lastActiveHeader().click();
  }

  public void clickTimestampHeader() {
    timestampHeader().click();
  }

  public void clickDomainHeader() {
    domainHeader().click();
  }

  public void clickTypeHeader() {
    typeHeader().click();
  }

  // Row interaction helpers
  public void clickUserRow(int index) {
    userRow(index).click();
  }

  public void clickFirstUserRow() {
    clickUserRow(0);
  }

  // Utility methods
  public String getUsernameText(int rowIndex) {
    return usernameCell(rowIndex).text();
  }

  public String getLoginCountText(int rowIndex) {
    return loginCountCell(rowIndex).text();
  }

  public String getLastActiveText(int rowIndex) {
    return lastActiveCell(rowIndex).text();
  }

  public int getRowCount() {
    return userRows().size() > 0 ? userRows().size() : activityRows().size();
  }

  public boolean isEmpty() {
    return getRowCount() == 0;
  }

  public boolean hasEmptyMessage() {
    return emptyMessage().exists();
  }

  public void waitForTable() {
    table().shouldBe(com.codeborne.selenide.Condition.visible);
  }
}
