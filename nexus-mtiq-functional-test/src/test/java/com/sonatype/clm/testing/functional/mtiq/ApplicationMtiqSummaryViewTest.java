/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.scan.ScanService;
import com.sonatype.insight.test.LogOutput;

import org.junit.Before;
import org.junit.Rule;

public class ApplicationMtiqSummaryViewTest
    extends AbstractMtiqSummaryViewTest
{
  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  @Rule
  public LogOutput logOutput = new LogOutput(ScanService.log.getName());

  @Before
  public void init() {
    // note the ȧ being used to force a character to be encoded
    Application application = tempEntity.newApplicationWithParent(getClass().getSimpleName() + "ȧpp",
        YE_OLE_APPLICATION, YE_OLE_ORGANIZATION);

    super.init(application);
  }
}
