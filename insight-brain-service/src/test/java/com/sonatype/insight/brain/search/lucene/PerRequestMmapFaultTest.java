/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.jaxrs.error.NonFatalRequestFault;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PerRequestMmapFaultTest
{
  @Test
  public void isUnsafeMemoryAccessFault_interpretedVariant_isDetected() {
    InternalError e = new InternalError("a fault occurred in an unsafe memory access operation");
    assertThat(PerRequestMmapFault.isUnsafeMemoryAccessFault(e)).isTrue();
  }

  @Test
  public void isUnsafeMemoryAccessFault_compiledVariant_isDetected() {
    InternalError e =
        new InternalError("a fault occurred in a recent unsafe memory access operation in compiled Java code");
    assertThat(PerRequestMmapFault.isUnsafeMemoryAccessFault(e)).isTrue();
  }

  @Test
  public void isUnsafeMemoryAccessFault_otherInternalError_isNotDetected() {
    InternalError e = new InternalError("Enclosing method not found");
    assertThat(PerRequestMmapFault.isUnsafeMemoryAccessFault(e)).isFalse();
  }

  @Test
  public void isUnsafeMemoryAccessFault_nullMessage_isNotDetected() {
    assertThat(PerRequestMmapFault.isUnsafeMemoryAccessFault(new InternalError())).isFalse();
  }

  @Test
  public void isUnsafeMemoryAccessFault_nonInternalErrorWithMatchingMessage_isNotDetected() {
    // Only InternalError qualifies: another VirtualMachineError such as OutOfMemoryError stays fatal
    // even if its message happens to contain the substring.
    assertThat(PerRequestMmapFault.isUnsafeMemoryAccessFault(new OutOfMemoryError("unsafe memory access")))
        .isFalse();
  }

  @Test
  public void wrapsTheInternalErrorAsChainedCauseAndOptsOutOfFatalHandling() {
    InternalError fault = new InternalError("a fault occurred in an unsafe memory access operation");
    PerRequestMmapFault mapped = new PerRequestMmapFault(fault);
    assertThat(mapped)
        .isInstanceOf(SearchIndexException.class)
        .isInstanceOf(NonFatalRequestFault.class)
        .hasCause(fault);
  }
}
