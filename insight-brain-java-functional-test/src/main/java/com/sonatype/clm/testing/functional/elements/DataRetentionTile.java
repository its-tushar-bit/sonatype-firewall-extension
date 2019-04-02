/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class DataRetentionTile
    extends OwnerTile
{
  private static final String DATA_RETENTION_OWNER_ELEMENT_ID = "#owner-pill-retention";

  public static final String MAX_AGE_HEADER = "Max Age";

  public static final String MAX_REPORTS_HEADER = "Max Reports";

  public static final String DONT_PURGE = "Don't Purge";

  public static final String NOT_AVAILABLE = "N/A";

  public DataRetentionTile() {
    super(DATA_RETENTION_OWNER_ELEMENT_ID);
  }

  public SelenideElement editButton() {
    return child("#edit-retention-button");
  }

  public Condition subHeaderText(String ownerName) {
    return Condition.text("applying to " + ownerName);
  }

  public ElementsCollection rows() {
    return children(".iq-table-row");
  }

  public ElementsCollection rowHeaders() {
    return children("th");
  }

  private int column(String contextId) {
    ElementsCollection rowHeaders = rowHeaders();
    return rowHeaders.indexOf(rowHeaders.find(Condition.exactText(contextId)));
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
}
