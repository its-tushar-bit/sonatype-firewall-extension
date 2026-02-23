/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.AccessTile;
import com.sonatype.clm.testing.functional.elements.RepositoriesSummaryTile;
import com.sonatype.clm.testing.functional.elements.RepositoryConfigurationTile;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;

import static com.codeborne.selenide.Selenide.$$;

public class RepositoryResultsSummaryPage
{
  private RepositoryResultsSummaryPage() {
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/firewall/management/view/repository_container/REPOSITORY_CONTAINER_ID");
  }

  public static RepositoriesSummaryTile summaryTile() {
    return new RepositoriesSummaryTile();
  }

  public static RepositoryConfigurationTile configTile() {
    return new RepositoryConfigurationTile();
  }

  public static AccessTile accessTile() {
    return new AccessTile("#access-tile-pill-access");
  }

  public static AccessTile configurationTile() {
    return new AccessTile("#repositories-pill-configuration");
  }

  public static AccessTile repositoriesTableRepositoryNameHeaderSortBtn() {
    return new AccessTile("#repository-column-header .nx-cell__sort-btn");
  }

  public static AccessTile repositoriesTableRepositoryFormatHeaderSortBtn() {
    return new AccessTile("#repository-format-column-header .nx-cell__sort-btn");
  }

  public static AccessTile repositoriesTableRepositoryTypeHeaderSortBtn() {
    return new AccessTile("#repository-type-column-header .nx-cell__sort-btn");
  }

  public static ElementsCollection getAllLoadingSpinners() {
    return $$(".nx-loading-spinner");
  }
}
