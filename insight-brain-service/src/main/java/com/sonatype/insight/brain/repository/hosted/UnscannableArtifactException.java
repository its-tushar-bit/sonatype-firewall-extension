/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import com.sonatype.insight.error.HttpStatusCode;

/**
 * Thrown by the synchronous hosted-enforcement path when the uploaded scan file is well-formed
 * but does not describe an artifact that the policy evaluator can fingerprint — for example a
 * Maven {@code -sources.jar}, {@code -javadoc.jar}, signature/checksum file, or any artifact
 * whose insight-scanner output yields {@code pathname=null, hash=null}.
 * <p>
 * Rendered as HTTP 422 (Unprocessable Entity). The caller (NXRM) treats this as "skip
 * enforcement, allow the upload" — the artifact has nothing for IQ to evaluate against, which
 * is a deterministic input-shape problem and not an IQ outage. This avoids the false-positive
 * "policy evaluation unavailable" banner the user was seeing when uploading sources jars.
 */
@SuppressWarnings("serial")
@HttpStatusCode(422)
public class UnscannableArtifactException
    extends RuntimeException
{
  public UnscannableArtifactException(final String message) {
    super(message);
  }
}
