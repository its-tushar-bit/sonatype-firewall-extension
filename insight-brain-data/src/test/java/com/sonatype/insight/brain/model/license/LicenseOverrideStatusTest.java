/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @since 1.6
 */
public class LicenseOverrideStatusTest
{
  @Test
  public void testGetByName() {
    for (LicenseOverrideStatus status : LicenseOverrideStatus.values()) {
      assertThat(LicenseOverrideStatus.getByName(status.getName())).isEqualTo(status);
    }
  }

  @Test
  public void testGetByName_Null() {
    assertThat(LicenseOverrideStatus.getByName(null)).isNull();
  }

  @Test
  public void testGetByName_Invalid() {
    assertThatThrownBy(() -> LicenseOverrideStatus.getByName("Yeti")).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unknown license override status with name: Yeti");
  }
}
