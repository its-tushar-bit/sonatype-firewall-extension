/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import java.util.List;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.exactText;

public class DataRetentionTile
    extends OwnerTile
{
  private static final String DATA_RETENTION_OWNER_ELEMENT_ID = "#owner-pill-retention";

  public static final String MAX_AGE_HEADER = "Age";

  public static final String MAX_REPORTS_HEADER = "Reports";

  public static final String DONT_PURGE = "Don't Purge";

  public static final String NOT_AVAILABLE = "N/A";

  public DataRetentionTile() {
    super(DATA_RETENTION_OWNER_ELEMENT_ID);
  }

  public SelenideElement editButton() {
    return child("#edit-retention-button");
  }

  public WebElementCondition subHeaderText(String ownerName) {
    return Condition.text("applying to " + ownerName);
  }

  public ElementsCollection rows() {
    return children(".nx-table-row");
  }

  public ElementsCollection rowHeaders() {
    return children("th");
  }

  private int column(String contextId) {
    List<SelenideElement> rowHeaders = rowHeaders().asFixedIterable().stream().toList();

    for (int i = 0; i < rowHeaders.size(); i++) {
      if (rowHeaders.get(i).has(exactText(contextId))) {
        return i;
      }
    }

    return -1;
  }

  public ElementsCollection maxAges() {
    return rows().get(1).$$("td");
  }

  public SelenideElement maxAge(String contextId) {
    return maxAges().get(column(contextId));
  }

  public ElementsCollection maxReports() {
    return rows().get(2).$$("td");
  }

  public SelenideElement maxReport(String contextId) {
    return maxReports().get(column(contextId));
  }

  public SelenideElement successMetrics() {
    return child(".retention-tile__success-metrics");
  }
}
