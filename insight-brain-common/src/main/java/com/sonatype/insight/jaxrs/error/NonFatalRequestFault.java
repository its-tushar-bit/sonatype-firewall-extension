/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.jaxrs.error;

/**
 * Marker for a {@link Throwable} that the layer raising it has classified as confined to a single
 * request, and therefore must never trip the JVM's automatic-shutdown-on-fatal-error handling.
 * <p>
 * {@link JavaLangErrorHandler#handle} normally walks the causality chain and shuts the JVM down if
 * it finds a {@link VirtualMachineError}. Some request-scoped failures wrap such an error even
 * though the process state is intact -- the canonical case (CLM-44515) is a memory-mapped Lucene
 * search read that hits a mapped-page SIGBUS, which HotSpot surfaces as an {@link InternalError}
 * ("a fault occurred in an unsafe memory access operation") only after it has safely recovered at a
 * defined recovery point. A user-supplied advanced-search query must not be able to terminate the
 * process (DoS).
 * <p>
 * A fault that implements this marker is an <em>explicit</em> per-request opt-out: the handler
 * treats it as non-fatal without walking its cause. It is the responsibility of the layer that sets
 * the marker to only do so for a fault it has proven request-scoped (e.g. by matching the specific
 * mmap read fault at the Lucene search-read boundary), so that nothing genuinely JVM-fatal hides
 * beneath the marker. This scopes the carve-out to the code that opts in, rather than de-fatalizing
 * a class of error process-wide.
 */
public interface NonFatalRequestFault
{
}
