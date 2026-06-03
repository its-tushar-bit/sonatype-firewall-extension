/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.reachability;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence;

/**
 * API response for reachability evidence. Wraps the stored evidence types
 * with request-specific metadata (vulnerability ID, displayed count).
 */
@JsonInclude(Include.NON_NULL)
public record ApiReachabilityEvidenceResponse(
    String vulnerabilityId,
    List<StoredReachabilityEvidence.EvidencePath> paths,
    boolean truncated)
{
}
