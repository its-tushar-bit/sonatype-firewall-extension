/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PerUserRateLimiterTest
{
  @Test
  public void singlePermit_returnsAndReleasesCleanly() {
    PerUserRateLimiter limiter = new PerUserRateLimiter(1);
    try (PerUserRateLimiter.Permit permit = limiter.acquire("alice")) {
      assertThat(permit).isNotNull();
    }
    // After close, a subsequent acquire for the same user must succeed.
    try (PerUserRateLimiter.Permit permit = limiter.acquire("alice")) {
      assertThat(permit).isNotNull();
    }
  }

  @Test
  public void concurrentRequestsForSameUserOverCap_areRejected() throws Exception {
    PerUserRateLimiter limiter = new PerUserRateLimiter(1);
    PerUserRateLimiter.Permit held = limiter.acquire("alice");
    try {
      assertThatThrownBy(() -> limiter.acquire("alice"))
          .isInstanceOf(RateLimitedException.class);
    }
    finally {
      held.close();
    }
  }

  @Test
  public void differentUsers_haveIndependentBuckets() {
    PerUserRateLimiter limiter = new PerUserRateLimiter(1);
    PerUserRateLimiter.Permit alice = limiter.acquire("alice");
    try {
      // Bob still has his own permit available.
      try (PerUserRateLimiter.Permit bob = limiter.acquire("bob")) {
        assertThat(bob).isNotNull();
      }
    }
    finally {
      alice.close();
    }
  }

  @Test
  public void anonymousUsersShareASingleBucket() {
    PerUserRateLimiter limiter = new PerUserRateLimiter(1);
    PerUserRateLimiter.Permit p1 = limiter.acquire(null);
    try {
      assertThatThrownBy(() -> limiter.acquire(""))
          .isInstanceOf(RateLimitedException.class);
      assertThatThrownBy(() -> limiter.acquire("   "))
          .isInstanceOf(RateLimitedException.class);
    }
    finally {
      p1.close();
    }
  }

  @Test
  public void permitClose_isIdempotent() {
    PerUserRateLimiter limiter = new PerUserRateLimiter(1);
    PerUserRateLimiter.Permit permit = limiter.acquire("alice");
    permit.close();
    permit.close();
    // A third acquire must succeed \u2014 the double-close did not leak a permit.
    try (PerUserRateLimiter.Permit again = limiter.acquire("alice")) {
      assertThat(again).isNotNull();
    }
  }

  @Test
  public void mapperReturns429WithRetryAfterHeader() {
    RateLimitedExceptionMapper mapper = new RateLimitedExceptionMapper();
    jakarta.ws.rs.core.Response response =
        mapper.toResponse(new RateLimitedException("Too many concurrent Global Search requests"));
    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getHeaderString("Retry-After")).isNotNull();
    assertThat(response.getHeaderString("Cache-Control")).isEqualTo("no-store");
  }
}
