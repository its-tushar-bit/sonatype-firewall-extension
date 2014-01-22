/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.io;

import java.io.File;
import java.io.IOException;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileCleanerTest
{
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void canDeleteFile() throws IOException {
    File file = folder.newFile();
    assertThat(file.exists(), is(true));

    new FileCleaner().delete(file);
    assertThat(file.exists(), is(false));
  }

  @Test(expected = FileDeletionException.class)
  public void undeletableFileThrowsSpecializedException() throws IOException {
    File file = mock(File.class, RETURNS_DEEP_STUBS);

    // simulate system that couldn't delete the file
    when(file.delete()).thenReturn(false);

    // satisfy encapsulated library calls
    when(file.getCanonicalFile().exists()).thenReturn(true);

    new FileCleaner().delete(file);
  }

  @Test(expected = FileDeletionException.class)
  public void errorThrowsSpecializedException() throws IOException {
    File file = mock(File.class);

    // specific call from encapsulated library that can cause exceptions
    when(file.getCanonicalFile()).thenThrow(new IOException("BOOM"));

    new FileCleaner().delete(file);
  }

  @Test
  public void ignoresNullFileObjects() throws FileDeletionException {
    new FileCleaner().delete(null);
  }
}
