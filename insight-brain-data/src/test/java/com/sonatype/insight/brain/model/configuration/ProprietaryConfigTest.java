/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProprietaryConfigTest
{
  @Test
  public void testSetPackages() {
    ProprietaryConfig config = new ProprietaryConfig("ownerId", null /* packages */, null /* regexes */);
    assertThat(config.getPackages()).hasSize(0);
    assertThat(config.getPackagesJson()).isNull();
    assertThat(config.getRegexes()).hasSize(0);
    assertThat(config.getRegexesJson()).isNull();

    config = new ProprietaryConfig("ownerId", Collections.emptyList() /* packages */, null /* regexes */);
    assertThat(config.getPackages()).hasSize(0);
    assertThat(config.getPackagesJson()).isNull();
    assertThat(config.getRegexes()).hasSize(0);
    assertThat(config.getRegexesJson()).isNull();

    config = new ProprietaryConfig("ownerId", Collections.singletonList("foo") /* packages */, null /* regexes */);
    assertThat(config.getPackages()).hasSize(1);
    assertThat(config.getPackages().get(0)).isEqualTo("foo");
    assertThat(config.getPackagesJson()).isEqualTo("[\"foo\"]");
    assertThat(config.getRegexes()).hasSize(0);
    assertThat(config.getRegexesJson()).isNull();

    config.setPackages(null);
    assertThat(config.getPackages()).hasSize(0);
    assertThat(config.getPackagesJson()).isNull();
    assertThat(config.getRegexes()).hasSize(0);
    assertThat(config.getRegexesJson()).isNull();

    config.setPackages(Collections.emptyList());
    assertThat(config.getPackages()).hasSize(0);
    assertThat(config.getPackagesJson()).isNull();
    assertThat(config.getRegexes()).hasSize(0);
    assertThat(config.getRegexesJson()).isNull();

    config.setPackages(Collections.singletonList("bar"));
    assertThat(config.getPackages()).hasSize(1);
    assertThat(config.getPackages().get(0)).isEqualTo("bar");
    assertThat(config.getPackagesJson()).isEqualTo("[\"bar\"]");
    assertThat(config.getRegexes()).hasSize(0);
    assertThat(config.getRegexesJson()).isNull();
  }

  @Test
  public void testSetRegexes() {
    ProprietaryConfig config = new ProprietaryConfig("ownerId", null /* packages */, null /* regexes */);
    assertThat(config.getPackages()).hasSize(0);
    assertThat(config.getPackagesJson()).isNull();
    assertThat(config.getRegexes()).hasSize(0);
    assertThat(config.getRegexesJson()).isNull();

    config = new ProprietaryConfig("ownerId", null /* packages */, Collections.emptyList() /* regexes */);
    assertThat(config.getPackages()).hasSize(0);
    assertThat(config.getPackagesJson()).isNull();
    assertThat(config.getRegexes()).hasSize(0);
    assertThat(config.getRegexesJson()).isNull();

    config = new ProprietaryConfig("ownerId", null /* packages */, Collections.singletonList("foo") /* regexes */);
    assertThat(config.getPackages()).hasSize(0);
    assertThat(config.getPackagesJson()).isNull();
    assertThat(config.getRegexes()).hasSize(1);
    assertThat(config.getRegexes().get(0)).isEqualTo("foo");
    assertThat(config.getRegexesJson()).isEqualTo("[\"foo\"]");

    config.setRegexes(null);
    assertThat(config.getPackages()).hasSize(0);
    assertThat(config.getPackagesJson()).isNull();
    assertThat(config.getRegexes()).hasSize(0);
    assertThat(config.getRegexesJson()).isNull();

    config.setRegexes(Collections.emptyList());
    assertThat(config.getPackages()).hasSize(0);
    assertThat(config.getPackagesJson()).isNull();
    assertThat(config.getRegexes()).hasSize(0);
    assertThat(config.getRegexesJson()).isNull();

    config.setRegexes(Collections.singletonList("bar"));
    assertThat(config.getPackages()).hasSize(0);
    assertThat(config.getPackagesJson()).isNull();
    assertThat(config.getRegexes()).hasSize(1);
    assertThat(config.getRegexes().get(0)).isEqualTo("bar");
    assertThat(config.getRegexesJson()).isEqualTo("[\"bar\"]");
  }
}
