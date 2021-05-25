/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class AuditLogContent
    extends BasicElement<AuditLogContent>
{
  public AuditLogContent(String selector) {
    super(selector);
  }

  public SelenideElement table() {
    return child("#audit-log-table");
  }

  public ElementsCollection rowWithoutDate(int index) {
    return children(getRowSelector(index) + ":not(:first-child)");
  }

  public SelenideElement dateFromRow(int rowIndex) {
    return child(getRowSelector(rowIndex) + ":first-child");
  }

  public SelenideElement emptyMessage() {
    return child("tbody > tr .nx-cell--meta-info span");
  }

  private String getRowSelector(int index) {
    return "tbody > tr:nth-child(" + (index + 1) + ") td";
  }
}
