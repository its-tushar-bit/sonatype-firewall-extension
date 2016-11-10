/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TwistlockScanTest
{
  @Test
  public void testGetAnalysisJson() throws Exception {
    TwistlockScan twistlockScan = new TwistlockScan(new File("target/test-classes/TwistlockScanTest/scan.zip"));
    assertThat(twistlockScan.getAnalysisJson(),
        is(FileUtils.fileRead("target/test-classes/TwistlockScanTest/expected-analysis.json")));
  }

  @Test
  public void testGetFilesJson() throws Exception {
    TwistlockScan twistlockScan = new TwistlockScan(new File("target/test-classes/TwistlockScanTest/scan.zip"));
    assertThat(twistlockScan.getFilesJson(),
        is(FileUtils.fileRead("target/test-classes/TwistlockScanTest/expected-files.json")));
  }

  @Test
  public void testGetScanXml() throws Exception {
    TwistlockScan twistlockScan = new TwistlockScan(new File("target/test-classes/TwistlockScanTest/scan.zip"));
    assertThat(twistlockScan.getScanXml(),
        is(FileUtils.fileRead("target/test-classes/TwistlockScanTest/expected-scan.xml")));
  }
}
