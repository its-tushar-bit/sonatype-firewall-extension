/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.version.graph;

public class VersionGraphIdeaTest
    extends AbstractVersionGraphMavenTest
{
  @Override
  protected String getStartPageUrl() {
    return "assets/version-graph/ide/idea/index.html";
  }

  @Override
  protected boolean shouldShowMigrateButton() {
    return false;
  }
}
