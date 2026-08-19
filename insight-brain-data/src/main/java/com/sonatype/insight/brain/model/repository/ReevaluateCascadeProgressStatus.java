/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import java.util.Arrays;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * Status enumeration for cascade re-evaluation progress.
 *
 * @since 1.196
 */
public enum ReevaluateCascadeProgressStatus
{
  PENDING,
  COMPLETED,
  FAILED;

  public static ReevaluateCascadeProgressStatus fromString(String status) {
    return Arrays.stream(ReevaluateCascadeProgressStatus.values())
        .filter(progressStatus -> progressStatus.name().equalsIgnoreCase(status))
        .findFirst()
        .orElseThrow(() -> new NotFoundException(String.format("Provided status %s is not found", status)));
  }
}
