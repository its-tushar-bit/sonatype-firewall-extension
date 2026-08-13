/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HeaderUtilsTest
{
  @Test
  public void testEscapeQuotationsInFilename() {
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename(null)).isNull();
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename("")).isEqualTo("");
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename("a")).isEqualTo("a");
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename("ab")).isEqualTo("ab");
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename("a b")).isEqualTo("a b");
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename("a\tb")).isEqualTo("a\tb");
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename("\"")).isEqualTo("\\\"");
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename("\"\"")).isEqualTo("\\\"\\\"");
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename("\"a\"b")).isEqualTo("\\\"a\\\"b");
    // We have to write a backslash in a String as \\ and a double quote as \"
    // A single backslash i.e. \\ will escape the quote \" and so is fine
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename("\\\"a\\\"")).isEqualTo("\\\"a\\\"");
    // A double backslash i.e. \\\\ will not escape the quote \" and so an extra backslash needs to be added
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename("\\\\\"a\\\\\"")).isEqualTo("\\\\\\\"a\\\\\\\"");
    // A triple backslash i.e. \\\\\\ will escape the quote \" and so is fine
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename("\\\\\\\"a\\\\\\\"")).isEqualTo("\\\\\\\"a\\\\\\\"");
    // A quadruple backslash i.e. \\\\\\\\ will not escape the quote \" and so an extra backslash needs to be added
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename("\\\\\\\\\"a\\\\\\\\\"")).isEqualTo(
        "\\\\\\\\\\\"a\\\\\\\\\\\"");
    // We must not escape the final double quote in the result so remove any final odd backslashes
    // i.e. 1 -> 0, 2 -> 2, 3 -> 2, 4 -> 4, 5 -> 4, etc
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename("\\")).isEqualTo("");
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename("\\\\")).isEqualTo("\\\\");
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename("\\\\\\")).isEqualTo("\\\\");
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename("\\\\\\\\")).isEqualTo("\\\\\\\\");
    assertThat(HttpHeaderUtils.escapeQuotationsInFilename("\\\\\\\\\\")).isEqualTo("\\\\\\\\");
  }

  @Test
  public void testEncodeFilename() throws Exception {
    assertThat(HttpHeaderUtils.encodeFilenameUtf8(null)).isEqualTo(null);
    assertThat(HttpHeaderUtils.encodeFilenameUtf8("")).isEqualTo("");
    assertThat(HttpHeaderUtils.encodeFilenameUtf8("a")).isEqualTo("a");
    assertThat(HttpHeaderUtils.encodeFilenameUtf8("a b")).isEqualTo("a%20b");
    String name = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ!#$&+-.^_`|~ 中";
    String encodedName = HttpHeaderUtils.encodeFilenameUtf8(name);
    assertThat(encodedName).isEqualTo(
        "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ!#$&+-.^_`|~%20%E4%B8%AD");
    assertThat(URLDecoder.decode(encodedName, StandardCharsets.UTF_8.name()).replaceFirst(" ", "+")).isEqualTo(name);
  }

  @Test
  public void testBuildContentDispositionHeaderValue() {
    assertThat(HttpHeaderUtils.buildContentDispositionHeaderValue("my app.ext")).isEqualTo(
        "attachment; filename=\"my app.ext\"; filename*=UTF-8''my%20app.ext");
    assertThat(HttpHeaderUtils.buildContentDispositionHeaderValue("中文.txt")).isEqualTo(
        "attachment; filename=\"中文.txt\"; filename*=UTF-8''%E4%B8%AD%E6%96%87.txt");
  }
}
