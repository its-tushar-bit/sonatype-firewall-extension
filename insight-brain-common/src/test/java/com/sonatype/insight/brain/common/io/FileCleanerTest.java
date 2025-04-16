/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.io;

import java.io.File;
import java.io.IOException;

import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileCleanerTest
{
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void canDeleteFile() throws IOException {
    File file = tempFolder.newFile();
    assertThat(file).exists();

    new FileCleaner().delete(file);
    assertThat(file).doesNotExist();
  }

  @Test
  public void canDeleteFolder() throws IOException {
    File folder = tempFolder.newFolder();
    assertThat(folder).exists();
    File file = new File(folder, "test.txt");
    file.createNewFile();
    assertThat(file).exists();

    new FileCleaner().delete(folder);
    assertThat(file).doesNotExist();
    assertThat(folder).doesNotExist();
  }

  @Test(expected = FileDeletionException.class)
  public void errorThrowsSpecializedException() throws IOException {
    File file = mock(File.class);

    // specific call from encapsulated library that can cause exceptions
    when(file.toPath()).thenThrow(new RuntimeException("BOOM"));
    when(file.exists()).thenReturn(true);

    new FileCleaner().delete(file);
  }

  @Test
  public void ignoresNullFileObjects() throws FileDeletionException {
    new FileCleaner().delete(null);
  }

  @Test
  public void ignoresNonExistentFileObjects() throws Exception {
    File file = new File(tempFolder.getRoot(), "lochness.monster");
    assertThat(file).doesNotExist();
    new FileCleaner().delete(file);
  }
}
