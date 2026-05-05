/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

/**
 * Response DTO returned by the hosted component scan upload endpoint.
 * <p>
 * Communicates the component identifier that was accepted for asynchronous evaluation.
 */
public record HostedComponentScanResponse(String componentId, String jobId)
{
}
