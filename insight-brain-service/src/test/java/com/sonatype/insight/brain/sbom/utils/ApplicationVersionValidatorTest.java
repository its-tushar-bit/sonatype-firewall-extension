/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.sbom.utils.ApplicationVersionValidator.validate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApplicationVersionValidatorTest
{
  @Test
  public void rejectsNull() {
    assertThatThrownBy(() -> validate(null))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("must not be null");
  }

  @Test
  public void rejectsEmptyAfterTrim() {
    assertThatThrownBy(() -> validate("   "))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("between 1 and 200");
  }

  @Test
  public void rejectsLongerThan200() {
    assertThatThrownBy(() -> validate(StringUtils.repeat('a', 201)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("between 1 and 200");
  }

  @Test
  public void acceptsExactly200() {
    String input = StringUtils.repeat('a', 200);
    assertThat(validate(input)).isEqualTo(input);
  }

  @Test
  public void rejectsControlCharacter() {
    assertThatThrownBy(() -> validate("v\u00001.0"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("control characters");
  }

  @Test
  public void rejectsTab() {
    assertThatThrownBy(() -> validate("v\t1.0"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("control characters");
  }

  @Test
  public void rejectsLeadingControlCharacter() {
    // Reject *before* trim() — String.trim() would silently strip a leading \x01.
    assertThatThrownBy(() -> validate("\u0001abc"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("control characters");
  }

  @Test
  public void rejectsTrailingControlCharacter() {
    // Reject *before* trim() — String.trim() would silently strip a trailing \x01.
    assertThatThrownBy(() -> validate("abc\u0001"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("control characters");
  }

  @Test
  public void rejectsForwardSlash() {
    assertThatThrownBy(() -> validate("v/1"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("path separators");
  }

  @Test
  public void rejectsBackslash() {
    assertThatThrownBy(() -> validate("v\\1"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("path separators");
  }

  @Test
  public void rejectsDoubleDot() {
    assertThatThrownBy(() -> validate("v..1"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("path separators or '..'");
  }

  @Test
  public void rejectsHtmlMetacharLessThan() {
    assertThatThrownBy(() -> validate("v<1"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("HTML metacharacters");
  }

  @Test
  public void rejectsHtmlMetacharAmpersand() {
    assertThatThrownBy(() -> validate("v&1"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("HTML metacharacters");
  }

  @Test
  public void rejectsDisallowedPercent() {
    assertThatThrownBy(() -> validate("v%1"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("disallowed character '%'");
  }

  @Test
  public void acceptsValidSemverWithBuildMetadata() {
    assertThat(validate("1.2.3-rc.1+build.42")).isEqualTo("1.2.3-rc.1+build.42");
  }

  @Test
  public void trimsLeadingAndTrailing() {
    assertThat(validate("  1.0  ")).isEqualTo("1.0");
  }

  @Test
  public void acceptsInternalSpaces() {
    assertThat(validate("version 1 0")).isEqualTo("version 1 0");
  }

  @Test
  public void rejectsConsecutiveSpaces() {
    assertThatThrownBy(() -> validate("a  b"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("consecutive spaces");
  }
}
