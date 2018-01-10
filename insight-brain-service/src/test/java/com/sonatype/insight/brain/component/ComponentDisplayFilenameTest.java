/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.Arrays;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ComponentDisplayFilenameTest
{
  private static String getFilename(String... pathnames) {
    return new ComponentDisplayFilename().addPathnames(Arrays.asList(pathnames)).getFilename().orElse(null);
  }

  @Test
  public void testGetFilename_NoOccurrences() {
    assertThat(getFilename(), is(nullValue()));
  }

  @Test
  public void testGetFilename_FromWindowsPath() {
    assertThat(getFilename("sub\\dir\\some.jar"), is("some.jar"));
  }

  @Test
  public void testGetFilename_FromUnixPath() {
    assertThat(getFilename("sub/dir/some.jar"), is("some.jar"));
  }

  @Test
  public void testGetFilename_FromDependency() {
    assertThat(getFilename("dependency:/project.gid:project.aid:jar:1.2/dep.gid:dep.aid:jar:1.2.3"),
        is("dep.gid:dep.aid:jar:1.2.3"));
  }

  @Test
  public void testGetFilename_MostFrequentWins() {
    assertThat(getFilename("some.jar", "some-1.0.jar", "some-1.0.jar"), is("some-1.0.jar"));
  }

  @Test
  public void testGetFilename_MostFrequentWins_AlphaSortBreaksTie() {
    assertThat(getFilename("x.jar", "c.jar", "a.jar", "b.jar", "y.jar"), is("a.jar"));
  }
}
