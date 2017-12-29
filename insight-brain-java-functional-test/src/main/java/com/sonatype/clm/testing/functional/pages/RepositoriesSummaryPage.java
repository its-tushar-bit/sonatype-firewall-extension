/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.RepositoriesAccessTile;
import com.sonatype.clm.testing.functional.elements.RepositoriesSummaryTile;
import com.sonatype.clm.testing.functional.elements.RepositoryConfigurationTile;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

public class RepositoriesSummaryPage
{
  public static String URL = BaseUrl.uriBuilder().fragment("/management/view/repositories").build().toString();

  public static RepositoriesSummaryTile summaryTile() {
    return new RepositoriesSummaryTile();
  }

  public static RepositoryConfigurationTile configTile() {
    return new RepositoryConfigurationTile();
  }

  public static RepositoriesAccessTile accessTile() {
    return new RepositoriesAccessTile();
  }
}
