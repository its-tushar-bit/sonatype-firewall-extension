/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for the MTIQ temp-dir validation that was restored to match
 * the original InsightBrainService behavior: mkdir, write, and delete.
 */
public class MultiTenantTempDirValidationTest
{
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void shouldPassValidationWithWritableTempDir() {
    String original = System.getProperty("java.io.tmpdir");
    try {
      System.setProperty("java.io.tmpdir", tempFolder.getRoot().getAbsolutePath());
      assertThat(MultiTenantInsightBrainService.validateTempDir()).isTrue();
    }
    finally {
      System.setProperty("java.io.tmpdir", original);
    }
  }

  @Test
  public void shouldCreateMissingTempDir() throws IOException {
    File newDir = new File(tempFolder.getRoot(), "new-temp-dir");
    assertThat(newDir.exists()).isFalse();

    String original = System.getProperty("java.io.tmpdir");
    try {
      System.setProperty("java.io.tmpdir", newDir.getAbsolutePath());
      assertThat(MultiTenantInsightBrainService.validateTempDir()).isTrue();
      assertThat(newDir.exists()).isTrue();
      assertThat(newDir.isDirectory()).isTrue();
    }
    finally {
      System.setProperty("java.io.tmpdir", original);
    }
  }

  @Test
  public void shouldFailWhenTempPathIsNotADirectory() throws IOException {
    File regularFile = tempFolder.newFile("not-a-directory");

    String original = System.getProperty("java.io.tmpdir");
    try {
      System.setProperty("java.io.tmpdir", regularFile.getAbsolutePath());
      assertThat(MultiTenantInsightBrainService.validateTempDir()).isFalse();
    }
    finally {
      System.setProperty("java.io.tmpdir", original);
    }
  }
}
