/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MediaTypeUtilsTest
{
  @Test
  public void testByName() {
    assertThat(MediaTypeUtils.byName("readme.txt"), is("text/plain;charset=UTF-8"));
    assertThat(MediaTypeUtils.byName("index.html"), is("text/html;charset=UTF-8"));
    assertThat(MediaTypeUtils.byName("index.HtMl"), is("text/html;charset=UTF-8"));
    assertThat(MediaTypeUtils.byName("index.htm"), is("text/html;charset=UTF-8"));
    assertThat(MediaTypeUtils.byName("style.css"), is("text/css;charset=UTF-8"));

    assertThat(MediaTypeUtils.byName("image.png"), is("image/png"));
    assertThat(MediaTypeUtils.byName("image.gif"), is("image/gif"));
    assertThat(MediaTypeUtils.byName("image.jpg"), is("image/jpeg"));
    assertThat(MediaTypeUtils.byName("image.jpeg"), is("image/jpeg"));

    assertThat(MediaTypeUtils.byName("controller.js"), is("application/x-javascript"));
    assertThat(MediaTypeUtils.byName("data.json"), is("application/json"));
    assertThat(MediaTypeUtils.byName("print.pdf"), is("application/pdf"));

    assertThat(MediaTypeUtils.byName("unknown.extension"), is("application/octet-stream"));
    assertThat(MediaTypeUtils.byName("dir.html/noextension"), is("application/octet-stream"));
  }
}
