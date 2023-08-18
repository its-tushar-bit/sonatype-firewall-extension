/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning.rest;

import com.sonatype.insight.brain.api.v2.ApiProxyServerConfigurationResource;
import com.sonatype.insight.brain.configuration.ldap.LdapResource;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsResource;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TemporarilyBannedRestResourcesTest
{
  @Test
  public void testClassesBanned() {
    TemporarilyBannedRestResources underTest = new TemporarilyBannedRestResources();

    assertThat(underTest.isBanned(LdapResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiProxyServerConfigurationResource.class)).isTrue();
    assertThat(underTest.isBanned(SuccessMetricsResource.class)).isTrue();
  }
}
