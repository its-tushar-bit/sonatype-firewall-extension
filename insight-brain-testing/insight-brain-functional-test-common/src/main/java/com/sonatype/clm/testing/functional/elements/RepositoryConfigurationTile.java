/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class RepositoryConfigurationTile
    extends BasicElement<RepositoryConfigurationTile>
{
  private static final String CONFIGURATION_TILE_SELECTOR = "#repositories-pill-configuration";

  public static final WebElementCondition EMPTY_LIST_TEXT =
      text("There are no repositories registered with the server.");

  public RepositoryConfigurationTile() {
    super(CONFIGURATION_TILE_SELECTOR);
  }

  public ConfigurationTable configurationTable() {
    return new ConfigurationTable(createSelector(selector, "#iq-repositories-configuration-table"));
  }

  public SelenideElement emptyDescriptor() {
    return child(".nx-cell--meta-info");
  }

  public ElementsCollection componentsTableConfigurationCountCols() {
    return children(".iq-repositories-configuration-table-repository");
  }

  public static class ConfigurationTable
      extends GreedyTable<ConfigurationTable.ConfigurationTableRow>
  {
    public ConfigurationTable(String... selectors) {
      super(selectors);
    }

    @Override
    public ConfigurationTableRow row(final int num) {
      return new ConfigurationTableRow(selector, "tbody + tbody tr", nthChild(num));
    }

    public ConfigurationTableRow row(final int bodyIndex, final int rowIndex) {
      return new ConfigurationTableRow(selector, "tbody" + ":nth-of-type(" + bodyIndex + ")",
          "tr" + ":nth-of-type(" + rowIndex + ")");
    }

    public ConfigurationTableRow repoManagerConfigTableRow(final int rowIndex) {
      return new ConfigurationTableRow(selector, "tbody tr", nthChild(rowIndex));
    }

    public SelenideElement repositoryPublicIdFilter() {
      return child(".nx-filter-input", "input");
    }

    public SelenideElement repositoryFormatFilter() {
      return child(".nx-filter-dropdown");
    }

    public static class ConfigurationTableRow
        extends BasicElement<ConfigurationTableRow>
    {
      public static final WebElementCondition ENABLED_ICON = cssClass("fa-circle-check");

      public static final WebElementCondition DISABLED_ICON = cssClass("fa-circle-xmark");

      public ConfigurationTableRow(String... selectors) {
        super(selectors);
      }

      public SelenideElement managerId() {
        return child(".iq-collapsible-row__header-title");
      }

      public SelenideElement publicId() {
        return child("td a", nthChild(1));
      }

      public SelenideElement repoManagerConfigTablePublicId() {
        return child("td", nthChild(1));
      }

      public SelenideElement repoManagerConfigTableLink() {
        return $("[data-testid='repositories_configuration-link']");
      }

      public SelenideElement format() {
        return child("td", nthChild(2));
      }

      public SelenideElement repositoryType() {
        return child("td", nthChild(3));
      }

      public SelenideElement enablement() {
        return child("td", nthChild(4));
      }

      public SelenideElement editRepositoryManagerNameButton() {
        return child("td", nthChild(2));
      }

      public SelenideElement editRepositoryButton() {
        return $("[data-testid='repository-edit-button']");
      }

      public SelenideElement deleteButton() {
        return $("[data-testid='repository-delete-button']");
      }

      public static WebElementCondition deleteRepositoryText(String publicId) {
        return text("Are you sure you want to remove the Repository with ID \"" + publicId
            + "\"? This action is not reversible.");
      }
    }
  }
}
