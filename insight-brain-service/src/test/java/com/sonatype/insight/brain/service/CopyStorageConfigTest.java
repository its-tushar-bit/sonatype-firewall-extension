/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class CopyStorageConfigTest
{
  @Test
  public void testValidate_Valid() {
    CopyStorageConfig config = new CopyStorageConfig(1, 1);

    assertThatCode(config::validate).doesNotThrowAnyException();
  }

  @Test
  public void testValidate_Max() {
    CopyStorageConfig config = new CopyStorageConfig(1, 200);

    assertThatCode(config::validate).doesNotThrowAnyException();
  }

  @Test
  public void testValidate_InvalidMaxTenantThreads() {
    CopyStorageConfig config = new CopyStorageConfig(0, 1);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(config::validate)
        .withMessageContaining("'maxTenantThreads' must be at least 1.");
  }

  @Test
  public void testValidate_InvalidMaxCopyThreads() {
    CopyStorageConfig config = new CopyStorageConfig(1, 0);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(config::validate)
        .withMessageContaining("'maxCopyThreads' must be at least 1.");
  }

  @Test
  public void testValidate_TooManyThreads() {
    CopyStorageConfig config = new CopyStorageConfig(1, 201);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(config::validate)
        .withMessageContaining(
            "Configuration could result in too many threads i.e. maxTenantThreads * maxCopyThreads > 200");
  }

  @Test
  public void testValidate_IntegerOverflowInSum() {
    // Test that causes overflow in the sum calculation
    // 2 * Integer.MAX_VALUE will overflow
    CopyStorageConfig config = new CopyStorageConfig(2, Integer.MAX_VALUE);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(config::validate)
        .withMessageContaining(
            "Configuration could result in too many threads i.e. maxTenantThreads * maxCopyThreads > 200");
  }

  @Test
  public void testRecordEquality() {
    CopyStorageConfig config1 = new CopyStorageConfig(1, 2);
    CopyStorageConfig config2 = new CopyStorageConfig(1, 2);
    CopyStorageConfig config3 = new CopyStorageConfig(2, 1);

    assertThat(config1).isEqualTo(config2);
    assertThat(config1).isNotEqualTo(config3);
    assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
  }

  @Test
  public void testRecordAccessors() {
    CopyStorageConfig config = new CopyStorageConfig(1, 2);

    assertThat(config.maxTenantThreads()).isEqualTo(1);
    assertThat(config.maxCopyThreads()).isEqualTo(2);
  }

  @Test
  public void testRecordToString() {
    CopyStorageConfig config = new CopyStorageConfig(1, 2);

    assertThat(config.toString()).isEqualTo("CopyStorageConfig[maxTenantThreads=1, maxCopyThreads=2]");
  }
}
