/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IntegrationVersionCacheLoaderTest
{
  @Mock
  private HdsClient mockHdsClient;

  private IntegrationVersionCacheLoader loader;

  @BeforeEach
  public void setup() {
    loader = new IntegrationVersionCacheLoader(mockHdsClient);
  }

  @Test
  public void testLoad_ReturnsVersionsFromHds() throws Exception {
    IntegrationVersionCacheKey key = new IntegrationVersionCacheKey("Maven_Plugin", 5);

    IqIntegrationVersion[] hdsResponse = {
      new IqIntegrationVersion("Maven_Plugin", "1.5.0"),
      new IqIntegrationVersion("Maven_Plugin", "1.4.0"),
      new IqIntegrationVersion("Maven_Plugin", "1.3.0"),
      new IqIntegrationVersion("Maven_Plugin", "1.2.0"),
      new IqIntegrationVersion("Maven_Plugin", "1.1.0")
    };

    when(mockHdsClient.get(IqIntegrationVersion[].class, "rest/iqIntegrations/versions",
        Map.of("name", "Maven_Plugin", "limit", "5"))).thenReturn(hdsResponse);

    List<IqIntegrationVersion> result = loader.load(key);

    assertThat(result).hasSize(5);
    assertThat(result.get(0).version()).isEqualTo("1.5.0");
    assertThat(result.get(1).version()).isEqualTo("1.4.0");
    assertThat(result.get(2).version()).isEqualTo("1.3.0");
    assertThat(result.get(3).version()).isEqualTo("1.2.0");
    assertThat(result.get(4).version()).isEqualTo("1.1.0");

    verify(mockHdsClient).get(IqIntegrationVersion[].class, "rest/iqIntegrations/versions",
        Map.of("name", "Maven_Plugin", "limit", "5"));
  }

  @Test
  public void testLoad_ReturnsEmptyListWhenNoVersionsFound() throws Exception {
    IntegrationVersionCacheKey key = new IntegrationVersionCacheKey("Unknown_Plugin", 10);

    IqIntegrationVersion[] emptyResponse = {};

    when(mockHdsClient.get(IqIntegrationVersion[].class, "rest/iqIntegrations/versions",
        Map.of("name", "Unknown_Plugin", "limit", "10"))).thenReturn(emptyResponse);

    List<IqIntegrationVersion> result = loader.load(key);

    assertThat(result).isEmpty();

    verify(mockHdsClient).get(IqIntegrationVersion[].class, "rest/iqIntegrations/versions",
        Map.of("name", "Unknown_Plugin", "limit", "10"));
  }
}
