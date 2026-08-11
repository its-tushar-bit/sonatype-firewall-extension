/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * Contains the blast radius of the JVM's memory-mapped read fault to the Lucene search-read path.
 * <p>
 * A memory-mapped index read over a corrupt or truncated segment can hit a mapped-page SIGBUS that
 * HotSpot surfaces as an {@link InternalError} ("unsafe memory access"). Because
 * {@link InternalError} is a {@link VirtualMachineError}, it would otherwise trip the global
 * {@code JavaLangErrorHandler} and shut the JVM down -- letting a user-supplied advanced-search
 * query terminate the process (DoS -- CLM-44515).
 * <p>
 * This advice wraps such a fault, at the {@link LuceneSearchIndexClient} public read boundary, in a
 * {@link PerRequestMmapFault} (a {@code NonFatalRequestFault}) so the handler treats it as a
 * per-request HTTP 5xx while keeping the {@link InternalError} chained. Any other
 * {@link InternalError} is rethrown unchanged and stays fatal.
 * <p>
 * The pointcut covers every public {@link LuceneSearchIndexClient} entry point <em>except</em> the
 * writer/merge methods -- that is, all read paths, plus a handful of harmless control-flow methods
 * ({@code backendId()}, {@code getIndexSize()}, {@code isFullRebuildInProgress()}, ...) that never
 * perform mmap'd segment reads and so never raise the "unsafe memory access" fault. This blocklist
 * form is deliberate: it fail-safe covers any read path added to the client later, rather than
 * relying on an allowlist that would fail open (letting a newly added read path terminate the JVM)
 * if it drifted out of sync. It uses AspectJ compile-time weaving (the {@code aspectj-maven-plugin}),
 * which -- unlike a Spring AOP proxy -- weaves the advice into the class's own bytecode, so it also
 * covers Hybrid search through the Lucene
 * delegate. The writer/merge path is left fatal: {@link LuceneSearchIndexClient#populateIndex()} and
 * {@link LuceneSearchIndexClient#updateIndex} (which drive {@code LuceneIndexWriterOwner}) are
 * excluded, and {@code LuceneIndexWriterOwner} itself is a separate class that is not matched. A
 * merge-time mmap fault is not user-triggerable and points at real corruption, so shutting down on
 * it remains the correct response -- a deliberate boundary (CLM-44515 review).
 */
@Aspect
public class SearchMmapFaultAspect
{
  @Around("execution(public * com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient.*(..)) "
      + "&& !execution(* com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient.populateIndex(..)) "
      + "&& !execution(* com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient.updateIndex(..))")
  public Object deFatalizeSearchReadMmapFault(final ProceedingJoinPoint joinPoint) throws Throwable {
    try {
      return joinPoint.proceed();
    }
    catch (InternalError e) {
      if (PerRequestMmapFault.isUnsafeMemoryAccessFault(e)) {
        throw new PerRequestMmapFault(e);
      }
      throw e;
    }
  }
}
