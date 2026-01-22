/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import java.time.Duration;
import java.time.Instant;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.codahale.metrics.health.HealthCheck.Result;
import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

public class ProductLicenseOperationalCheckTest
    extends AbstractComponentTest
{
  @Inject
  private ProductLicenseOperationalCheck check;

  @Mock
  private ProductLicense productLicenseMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(ProductLicense.class).toInstance(productLicenseMock);
    super.configure(binder);
  }

  @Test
  public void testExecute_Healthy() {
    doReturn(true).when(productLicenseMock).isValid();
    doReturn(Instant.now().plus(Duration.ofHours(32)).toEpochMilli()).when(productLicenseMock).getExpirationTimestamp();

    Result result = check.execute();
    assertThat(result.isHealthy()).isTrue();
    assertThat(result.getDetails()).containsEntry("remainingDays", 1);

    doReturn(Instant.now().plus(Duration.ofHours(20)).toEpochMilli()).when(productLicenseMock).getExpirationTimestamp();

    result = check.execute();
    assertThat(result.isHealthy()).isTrue();
    assertThat(result.getDetails()).containsEntry("remainingDays", 0);
  }

  @Test
  public void testExecute_Unhealthy() {
    doReturn(false).when(productLicenseMock).isValid();
    doReturn(Instant.now().minus(Duration.ofHours(30)).toEpochMilli()).when(productLicenseMock)
        .getExpirationTimestamp();

    Result result = check.execute();
    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getDetails()).containsEntry("remainingDays", 0);
  }
}
