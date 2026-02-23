/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class FirewallContainerRepositoryResultsPage
    extends BasicElement<FirewallContainerRepositoryResultsPage>
{
  public static final String ROOT = "#container-repository-results-page";

  public FirewallContainerRepositoryResultsPage() {
    super(ROOT);
  }

  public static String url(String repositoryId) {
    return BaseUrl.resolvePageUrl("/firewall/container/repository/{repositoryId}/results", repositoryId);
  }

  public SelenideElement title() {
    return child("#container-repository-results-page__title");
  }

  public static class ContainerRepositoryResultsTable extends BasicElement<ContainerRepositoryResultsTable>
  {
    public static final String ROOT = "#container-repository-results-table";

    public ContainerRepositoryResultsTable() {
      super(ROOT);
    }

    public SelenideElement openFilterDrawerButton() {
      return child("#container-repository-results-table__open-filter-button");
    }

    public SelenideElement table() {
      return child("table");
    }

    public ElementsCollection rows() {
      return children("tbody tr");
    }

    public SelenideElement header() {
      return child("thead");
    }

    public SelenideElement cell(int rowIndex, int colIndex) {
      return child(String.format("tbody tr:nth-child(%d) td:nth-child(%d)", rowIndex + 1, colIndex + 1));
    }

    public SelenideElement pagination() {
      return child(".nx-pagination");
    }
  }

  public NxBackButton backButton() {
    return new NxBackButton();
  }
}
