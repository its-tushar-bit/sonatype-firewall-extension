/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;

public class RepositoryConfigurationTile
    extends BasicElement<RepositoryConfigurationTile>
{
  private static final String CONFIGURATION_TILE_SELECTOR = "#repositories-pill-configuration";

  public static final Condition EMPTY_LIST_TEXT = text("There are no repositories registered with the server.");

  public RepositoryConfigurationTile() {
    super(CONFIGURATION_TILE_SELECTOR);
  }

  public ConfigurationTable configurationTable() {
    return new ConfigurationTable(createSelector(selector, "#iq-repositories-configuration-table"));
  }

  public SelenideElement emptyDescriptor() {
    return child(".nx-cell--meta-info");
  }

  public static class ConfigurationTable
      extends GreedyTable<ConfigurationTable.ConfigurationTableRow>
  {
    public ConfigurationTable(String... selectors) {
      super(selectors);
    }

    @Override
    public ConfigurationTableRow row(final int num) {
      return new ConfigurationTableRow(selector, "tbody tr", nthChild(num));
    }

    public static class ConfigurationTableRow
        extends BasicElement<ConfigurationTableRow>
    {
      public static final Condition ENABLED_ICON = cssClass("fa-check-circle");

      public static final Condition DISABLED_ICON = cssClass("fa-times-circle");

      public ConfigurationTableRow(String... selectors) {
        super(selectors);
      }

      public SelenideElement publicId() {
        return child("td a", nthChild(1));
      }

      public SelenideElement managerId() {
        return child("td", nthChild(2));
      }

      public SelenideElement status() {
        return child("td", nthChild(3));
      }

      public SelenideElement statusIcon() {
        return child("td", nthChild(3), "> .fa");
      }

      public SelenideElement deleteButton() {
        return child("td", nthChild(4), "> div > button");
      }

      public static Condition deleteRepositoryText(String publicId) {
        return text("Are you sure you want to remove the Repository with ID \"" + publicId
            + "\"? This action is not reversible.");
      }
    }
  }
}
