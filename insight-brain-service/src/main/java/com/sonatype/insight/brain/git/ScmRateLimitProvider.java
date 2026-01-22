/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.nexus.scm.api.RateLimitCapturer;
import com.sonatype.nexus.scm.api.RateLimitCapturerProvider;
import com.sonatype.nexus.scm.api.RateLimitCapturers;
import com.sonatype.nexus.scm.api.DefaultRateLimitCapturers;

import io.dropwizard.lifecycle.Managed;

@Named
@Singleton
public class ScmRateLimitProvider
    implements RateLimitCapturers, Managed
{
  private final TenantReference<RateLimitCapturers> rateLimitCapturers =
      new TenantReference<>(DefaultRateLimitCapturers::new);

  @Override
  public void start() throws Exception {
    RateLimitCapturerProvider.initialiseRateLimitCapturerProvider(this);
  }

  @Override
  public RateLimitCapturer forGitHub() {
    return rateLimitCapturers.get().forGitHub();
  }

  @Override
  public RateLimitCapturer forGraphQl() {
    return rateLimitCapturers.get().forGraphQl();
  }
}
