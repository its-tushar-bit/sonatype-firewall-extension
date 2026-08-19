/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import com.sonatype.insight.jaxrs.error.NonFatalRequestFault;

import org.aspectj.lang.Aspects;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit-tests the advice logic of {@link SearchMmapFaultAspect}. End-to-end AspectJ compile-time
 * weaving of the advice onto {@link LuceneSearchIndexClient} is verified by the live search-flood
 * reproduction against a running IQ (see the CLM-44515 PR description), which is the authoritative
 * proof that a real memory-mapped read fault is contained rather than crashing the JVM.
 */
public class SearchMmapFaultAspectTest
{
  private SearchMmapFaultAspect aspect;

  @BeforeEach
  public void setUp() {
    // aspectOf returns the CTW-managed singleton; obtaining it also proves the aspect was woven.
    aspect = Aspects.aspectOf(SearchMmapFaultAspect.class);
  }

  @Test
  public void aspectIsAWovenSingleton() {
    assertThat(aspect).isNotNull();
  }

  @Test
  public void normalReturnValuePassesThrough() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    when(joinPoint.proceed()).thenReturn("results");
    assertThat(aspect.deFatalizeSearchReadMmapFault(joinPoint)).isEqualTo("results");
  }

  @Test
  public void mmapReadFaultBecomesNonFatalPerRequestFaultWithChainedCause() throws Throwable {
    InternalError fault = new InternalError("a fault occurred in an unsafe memory access operation");
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    when(joinPoint.proceed()).thenThrow(fault);

    assertThatThrownBy(() -> aspect.deFatalizeSearchReadMmapFault(joinPoint))
        .isInstanceOf(PerRequestMmapFault.class)
        .isInstanceOf(NonFatalRequestFault.class)
        .hasCause(fault);
  }

  @Test
  public void genuineInternalErrorIsRethrownUnchangedAndStaysFatal() throws Throwable {
    InternalError fatal = new InternalError("Enclosing method not found");
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    when(joinPoint.proceed()).thenThrow(fatal);

    assertThatThrownBy(() -> aspect.deFatalizeSearchReadMmapFault(joinPoint)).isSameAs(fatal);
  }

  @Test
  public void ordinaryExceptionIsRethrownUnchanged() throws Throwable {
    RuntimeException boom = new RuntimeException("boom");
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    when(joinPoint.proceed()).thenThrow(boom);

    assertThatThrownBy(() -> aspect.deFatalizeSearchReadMmapFault(joinPoint)).isSameAs(boom);
  }
}
