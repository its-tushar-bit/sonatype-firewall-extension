/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlConfigTest
{
  @Test
  public void testGetCloneDirectory() {
    File sonatypeWork = new File("sonatype-work");

    SourceControlConfig sourceControlConfig = new SourceControlConfig();
    sourceControlConfig.setSonatypeWorkDir(sonatypeWork);

    assertThat(sourceControlConfig.getCloneDirectory())
        .isEqualTo(new File(sonatypeWork, SourceControlConfig.DEFAULT_SOURCE_CONTROL_CLONE_DIR));

    sourceControlConfig.setCloneDirectory("");
    assertThat(sourceControlConfig.getCloneDirectory())
        .isEqualTo(new File(sonatypeWork, SourceControlConfig.DEFAULT_SOURCE_CONTROL_CLONE_DIR));

    String relativePath = "abc";
    assertThat(new File(relativePath)).isRelative();
    sourceControlConfig.setCloneDirectory(relativePath);
    assertThat(sourceControlConfig.getCloneDirectory()).isEqualTo(new File(sonatypeWork, relativePath));

    String absolutePath = new File("abc").getAbsolutePath();
    sourceControlConfig.setCloneDirectory(absolutePath);
    assertThat(sourceControlConfig.getCloneDirectory()).isEqualTo(new File(absolutePath));
  }
}
