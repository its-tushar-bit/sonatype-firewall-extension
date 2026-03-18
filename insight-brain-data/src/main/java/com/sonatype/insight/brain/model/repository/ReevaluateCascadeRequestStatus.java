/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import java.util.Arrays;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * Status enumeration for cascade re-evaluation requests.
 *
 * @since 1.196
 */
public enum ReevaluateCascadeRequestStatus
{
  /** Request created, awaiting background processing */
  PENDING,

  /** Background task is actively processing repositories */
  IN_PROGRESS,

  /** All repositories processed successfully */
  COMPLETED,

  /** No repositories found containing the specified component */
  NO_COMPONENTS_FOUND,

  /** Processing failed due to error */
  FAILED;

  public static ReevaluateCascadeRequestStatus fromString(String status) {
    return Arrays.stream(ReevaluateCascadeRequestStatus.values())
        .filter(requestStatus -> requestStatus.name().equalsIgnoreCase(status))
        .findFirst()
        .orElseThrow(() -> new NotFoundException(String.format("Provided status %s is not found", status)));
  }
}
