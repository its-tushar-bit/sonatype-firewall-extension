/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MostRecentMemoizingFunctionTest
{
  @Test
  public void testApply() {
    Function<String, String> mockFunction = mock(Function.class);
    when(mockFunction.apply("arg1")).thenReturn("value1");
    when(mockFunction.apply("arg2")).thenReturn("value2");
    MostRecentMemoizingFunction<String, String> mostRecentMemoizingFunction =
        new MostRecentMemoizingFunction<>(mockFunction);

    // 1st call, calls delegate with the argument and memoizes the argument and result
    assertThat(mostRecentMemoizingFunction.apply("arg1")).isEqualTo("value1");
    verify(mockFunction).apply("arg1");

    Mockito.clearInvocations(mockFunction);

    // 2nd call, with the same argument returns the memoized result
    assertThat(mostRecentMemoizingFunction.apply("arg1")).isEqualTo("value1");
    verify(mockFunction, never()).apply(any());

    // 3rd call, with a different argument triggers a delegate call and the new argument and result being memoized
    assertThat(mostRecentMemoizingFunction.apply("arg2")).isEqualTo("value2");
    verify(mockFunction).apply("arg2");
  }
}
