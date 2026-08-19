/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import com.sonatype.insight.brain.product.license.ProductLicense;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

@ExtendWith(MockitoExtension.class)
public class ProductLicenseOperationalCheckTest
{
  @Mock
  private ProductLicense productLicense;

  private ProductLicenseOperationalCheck check;

  @BeforeEach
  public void setUp() {
    check = new ProductLicenseOperationalCheck(productLicense);
  }

  @Test
  public void testExecute_Healthy() throws Exception {
    doReturn(true).when(productLicense).isValid();
    doReturn(Instant.now().plus(Duration.ofHours(32)).toEpochMilli()).when(productLicense).getExpirationTimestamp();

    Health result = check.execute();
    assertThat(result.getStatus()).isEqualTo(Status.UP);
    assertThat(result.getDetails()).containsEntry("remainingDays", 1);

    doReturn(Instant.now().plus(Duration.ofHours(20)).toEpochMilli()).when(productLicense).getExpirationTimestamp();

    result = check.execute();
    assertThat(result.getStatus()).isEqualTo(Status.UP);
    assertThat(result.getDetails()).containsEntry("remainingDays", 0);
  }

  @Test
  public void testExecute_Unhealthy() throws Exception {
    doReturn(false).when(productLicense).isValid();
    doReturn(Instant.now().minus(Duration.ofHours(30)).toEpochMilli()).when(productLicense)
        .getExpirationTimestamp();

    Health result = check.execute();
    assertThat(result.getStatus()).isEqualTo(Status.DOWN);
    assertThat(result.getDetails()).containsEntry("remainingDays", 0);
  }
}
