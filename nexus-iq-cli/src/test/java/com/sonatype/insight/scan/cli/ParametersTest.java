/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ParametersTest
{
  @Test
  public void testInvalidStage() throws Exception {
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar",
        "-t", "invalid-stage-id");
    assertThat(params.getError().getMessage()).isEqualTo("An invalid stage was specified: -t invalid-stage-id");
  }

  @Test
  public void testBaseDir() throws Exception {
    assertThat(new Parameters().getBaseDir()).isNull();
    assertThat(new Parameters("-b").getBaseDir()).isNull();
    assertThat(new Parameters("-b", "/dir").getBaseDir()).isEqualTo(new File("/dir"));
    assertThat(new Parameters("--base-dir", "/dir").getBaseDir()).isEqualTo(new File("/dir"));
  }
}
