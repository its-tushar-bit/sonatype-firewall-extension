/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ContextConfiguration(classes = SpringInjectedTestBeanStateResetTest.TestConfig.class)
public class SpringInjectedTestBeanStateResetTest
    extends SpringInjectedTest
{
  @Inject
  private MutableSettings mutableSettings;

  @Test
  public void test1ShouldAllowMutatingInjectedBeanStateWithinATest() {
    mutableSettings.setValue("changed");

    assertThat(mutableSettings.getValue()).isEqualTo("changed");
  }

  @Test
  public void test2ShouldRestoreInjectedBeanStateBetweenTests() {
    assertThat(mutableSettings.getValue()).isEqualTo("default");
  }

  @Configuration
  static class TestConfig
  {
    @Bean
    public MutableSettings mutableSettings() {
      return new MutableSettings("default");
    }
  }

  static class MutableSettings
  {
    private String value;

    MutableSettings(final String value) {
      this.value = value;
    }

    String getValue() {
      return value;
    }

    void setValue(final String value) {
      this.value = value;
    }
  }
}
