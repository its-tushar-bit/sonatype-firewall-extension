/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.version.graph;

import com.sonatype.clm.testing.functional.elements.VersionsCIP;

import org.junit.Before;

import static com.codeborne.selenide.Condition.visible;

public class VersionGraphEclipseTest
    extends AbstractVersionGraphMavenTest
{
  @Override
  @Before
  public void start() {
    super.start();
    refreshOrOpen("assets/version-graph/ide/eclipse/index.html");
  }

  @Override
  protected void verifyMigrateButton() {
    VersionsCIP.migrateButton().shouldBe(visible);
  }
}
