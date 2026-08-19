/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.exception;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionHelperTest
{
  @Test
  public void testHasCauseOrSuppressedOfType_IsOfDesiredType() {
    Throwable throwable = new RuntimeException();
    assertThat(ExceptionHelper.hasCauseOrSuppressedOfType(throwable, RuntimeException.class)).isTrue();
    assertThat(ExceptionHelper.hasCauseOrSuppressedOfType(throwable, Exception.class)).isTrue();
  }

  @Test
  public void testHasCauseOrSuppressedOfType_HasCauseOfDesiredType() {
    // Direct cause is of desired type
    Throwable throwable = new IOException(new RuntimeException());
    assertThat(ExceptionHelper.hasCauseOrSuppressedOfType(throwable, RuntimeException.class)).isTrue();

    throwable = new IOException(new NullPointerException());
    assertThat(ExceptionHelper.hasCauseOrSuppressedOfType(throwable, RuntimeException.class)).isTrue();

    // Second level cause is of desired type
    throwable = new IOException(new IOException(new NullPointerException()));
    assertThat(ExceptionHelper.hasCauseOrSuppressedOfType(throwable, RuntimeException.class))
        .isTrue();
  }

  @Test
  public void testHasCauseOrSuppressedOfType_HasSuppressedOfDesiredType() {
    // Direct suppressed is of desired type
    Throwable throwable = new IOException();
    throwable.addSuppressed(new RuntimeException());
    assertThat(ExceptionHelper.hasCauseOrSuppressedOfType(throwable, RuntimeException.class)).isTrue();

    throwable = new IOException();
    throwable.addSuppressed(new NullPointerException());
    assertThat(ExceptionHelper.hasCauseOrSuppressedOfType(throwable, RuntimeException.class)).isTrue();

    // Second level cause has suppressed of desired type
    throwable = new IOException(new IOException(throwable));
    assertThat(ExceptionHelper.hasCauseOrSuppressedOfType(throwable, RuntimeException.class)).isTrue();
  }

  @Test
  public void testHasCauseOrSuppressedOfType_CircularCause() {
    Throwable cause = new IOException();
    Throwable throwable = new IOException(cause);
    cause.initCause(throwable);
    assertThat(ExceptionHelper.hasCauseOrSuppressedOfType(throwable, RuntimeException.class)).isFalse();
  }

  @Test
  public void testHasCauseOrSuppressedOfType_CircularSuppressed_Cause() {
    // Suppressed throwable has original throwable as cause
    Throwable suppressed = new IOException();
    Throwable throwable = new IOException();
    throwable.addSuppressed(suppressed);
    suppressed.initCause(throwable);
    assertThat(ExceptionHelper.hasCauseOrSuppressedOfType(throwable, RuntimeException.class)).isFalse();
  }

  @Test
  public void testHasCauseOrSuppressedOfType_CircularSuppressed_Suppressed() {
    // Suppressed throwable has original throwable as suppressed
    Throwable suppressed = new IOException();
    Throwable throwable = new IOException();
    throwable.addSuppressed(suppressed);
    suppressed.addSuppressed(throwable);
    assertThat(ExceptionHelper.hasCauseOrSuppressedOfType(throwable, RuntimeException.class)).isFalse();
  }
}
