/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;

import com.google.inject.Binder;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

/**
 * Support class for tests of Sisu components.
 */
public class AbstractComponentTest
    extends InjectedTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public TestName testName = new TestName();

  @Override
  public void configure(Binder binder) {
    InsightConfig config = new InsightConfig();
    try {
      config.setSonatypeWork(tempDir.newFolder("sonatype-work").getAbsolutePath());
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
    config.setSaasAddress("http://unknownhost");
    customizeConfig(config);
    binder.bind(InsightConfig.class).toInstance(config);
  }

  protected void customizeConfig(InsightConfig config) {
    // hook for tests to tweak config before components grab it
  }
}
