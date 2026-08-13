/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.jaxrs.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaLangErrorHandlerTest
{
  private JavaLangErrorHandler handler;

  @BeforeEach
  public void setUp() {
    handler = new JavaLangErrorHandler();
    // Never exit the test JVM: handle() calls Runtime.exit when a fatal error is found and
    // exitOnFatalError is true. We assert the fatal-vs-not classification via getLastFatalError(),
    // which is only set when handle() classified the throwable as fatal.
    handler.setExitOnFatalErrorSupplier(() -> false);
  }

  // A request-scoped fault that opts out of fatal handling. In production this is
  // com.sonatype.insight.brain.search.lucene.PerRequestMmapFault, which the search layer wraps a
  // memory-mapped read InternalError in; here a lightweight stand-in exercises the handler contract.
  private static final class TestRequestFault
      extends RuntimeException
      implements NonFatalRequestFault
  {
    TestRequestFault(final Throwable cause) {
      super(cause);
    }
  }

  // A fault that is ITSELF a VirtualMachineError (an InternalError) yet wears the request-scoped
  // marker. Used to prove findFatalError checks the marker BEFORE the VirtualMachineError branch:
  // were that branch evaluated first, this would be classified fatal.
  private static final class FatalTypedRequestFault
      extends InternalError
      implements NonFatalRequestFault
  {
    FatalTypedRequestFault() {
      super("a fault occurred in an unsafe memory access operation");
    }
  }

  private static InternalError mmapReadFault() {
    return new InternalError("a fault occurred in an unsafe memory access operation");
  }

  // CLM-44515: a memory-mapped Lucene index read fault surfaces as an "unsafe memory access"
  // InternalError. When the search layer has wrapped it in a NonFatalRequestFault, it is a
  // per-request failure and must NOT terminate the JVM (DoS via advanced search).
  @Test
  public void nonFatalRequestFaultWrappingMmapInternalErrorIsNotFatal() {
    handler.handle(new TestRequestFault(mmapReadFault()));
    assertThat(handler.getLastFatalError()).isNull();
  }

  // The marker check runs BEFORE the VirtualMachineError branch. This fault is itself a
  // VirtualMachineError (an InternalError) that wears the marker, so were the VirtualMachineError
  // branch evaluated first it would be classified fatal; a null result proves the ordering. This is
  // a distinct path from the wrapping case above, where the VirtualMachineError is only the cause.
  @Test
  public void markerOnFaultThatIsItselfVirtualMachineErrorShortCircuitsBeforeFatalBranch() {
    FatalTypedRequestFault fault = new FatalTypedRequestFault();
    assertThat(fault).isInstanceOf(VirtualMachineError.class);
    handler.handle(fault);
    assertThat(handler.getLastFatalError()).isNull();
  }

  // The marker can sit anywhere in the chain: findFatalError checks it at every recursion level, so
  // it is still honored when an outer non-marker wrapper (e.g. a SearchIndexException produced by an
  // existing catch(Exception) block) sits above the marked fault.
  @Test
  public void markerNestedBeneathPlainWrapperIsNotFatal() {
    Throwable chain = new RuntimeException("advanced search failed", new TestRequestFault(mmapReadFault()));
    handler.handle(chain);
    assertThat(handler.getLastFatalError()).isNull();
  }

  // CLM-44515 scoping (review): the carve-out is now scoped to the search-read path via the marker,
  // NOT a process-wide message match. A raw "unsafe memory access" InternalError that was NOT wrapped
  // by the search layer -- e.g. from a jemalloc profiling native downcall or any other mmap/Unsafe
  // consumer, or from the Lucene writer/merge path -- has no marker and stays fatal.
  @Test
  public void unmarkedUnsafeMemoryAccessInternalErrorRemainsFatal() {
    InternalError error = mmapReadFault();
    handler.handle(error);
    assertThat(handler.getLastFatalError()).isSameAs(error);
  }

  @Test
  public void outOfMemoryErrorRemainsFatal() {
    OutOfMemoryError error = new OutOfMemoryError("Java heap space");
    handler.handle(error);
    assertThat(handler.getLastFatalError()).isSameAs(error);
  }

  @Test
  public void stackOverflowErrorRemainsFatal() {
    StackOverflowError error = new StackOverflowError();
    handler.handle(error);
    assertThat(handler.getLastFatalError()).isSameAs(error);
  }

  @Test
  public void unrelatedInternalErrorRemainsFatal() {
    InternalError error = new InternalError("something else went badly wrong");
    handler.handle(error);
    assertThat(handler.getLastFatalError()).isSameAs(error);
  }

  @Test
  public void internalErrorWithNullMessageRemainsFatal() {
    InternalError error = new InternalError();
    handler.handle(error);
    assertThat(handler.getLastFatalError()).isSameAs(error);
  }

  @Test
  public void ordinaryExceptionIsNotFatal() {
    handler.handle(new RuntimeException("boom"));
    assertThat(handler.getLastFatalError()).isNull();
  }

  // A genuine VirtualMachineError nested beneath an ordinary (non-marker) wrapper must still be
  // classified as fatal: findFatalError walks the cause chain when there is no marker to opt out.
  @Test
  public void genuineFatalErrorNestedBeneathPlainWrapperStillFatal() {
    RuntimeException chain = new RuntimeException("wrapping", new OutOfMemoryError("Java heap space"));
    handler.handle(chain);
    assertThat(handler.getLastFatalError()).isInstanceOf(OutOfMemoryError.class);
  }
}
