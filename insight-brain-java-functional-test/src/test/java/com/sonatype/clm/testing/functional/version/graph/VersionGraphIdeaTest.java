/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.version.graph;

import org.junit.Before;

public class VersionGraphIdeaTest
    extends AbstractVersionGraphMavenTest
{
  @Override
  @Before
  public void start() {
    super.start();
    refreshOrOpen("assets/version-graph/ide/idea/index.html");
  }

  @Override
  protected boolean shouldShowMigrateButton() {
    return false;
  }
}
