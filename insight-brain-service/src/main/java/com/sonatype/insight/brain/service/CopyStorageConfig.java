/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.error.exception.BadRequestException;

public record CopyStorageConfig(
    int maxTenantThreads,
    int maxCopyThreads)
{
  public void validate() {
    if (maxTenantThreads < 1) {
      throw new BadRequestException("'maxTenantThreads' must be at least 1.");
    }
    if (maxCopyThreads < 1) {
      throw new BadRequestException("'maxCopyThreads' must be at least 1.");
    }

    boolean tooManyThreads;
    try {
      int total = Math.multiplyExact(maxTenantThreads, maxCopyThreads);
      tooManyThreads = total > CopyStorageService.MAX_TENANT_THREAD_POOL_THREADS;
    }
    catch (ArithmeticException e) {
      tooManyThreads = true;
    }

    if (tooManyThreads) {
      String msg = "Configuration could result in too many threads i.e." +
          " maxTenantThreads * maxCopyThreads > %s";
      throw new BadRequestException(msg.formatted(CopyStorageService.MAX_TENANT_THREAD_POOL_THREADS));
    }
  }
}
