/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TwistlockScanTest
{
  private String readFile(String filename) throws Exception {
    return FileUtils.fileRead("target/test-classes/TwistlockScanTest/" + filename, "UTF-8");
  }

  @Test
  public void testGetFilesJson() throws Exception {
    TwistlockScan twistlockScan = new TwistlockScan(new File("target/test-classes/TwistlockScanTest/scan.zip"));
    assertThat(twistlockScan.getFilesJson()).isEqualTo(readFile("expected-files.json"));
  }

  @Test
  public void testGetScanXml() throws Exception {
    TwistlockScan twistlockScan = new TwistlockScan(new File("target/test-classes/TwistlockScanTest/scan.zip"));
    assertThat(twistlockScan.getScanXml()).isEqualTo(readFile("expected-scan.xml").replace("\r\n", "\n"));
  }
}
