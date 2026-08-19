/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.google.common.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads integration version data from HDS for caching.
 */
@Named
public class IntegrationVersionCacheLoader
    extends CacheLoader<IntegrationVersionCacheKey, List<IqIntegrationVersion>>
{
  private static final Logger log = LoggerFactory.getLogger(IntegrationVersionCacheLoader.class);

  private static final String HDS_INTEGRATION_VERSIONS_PATH = "rest/iqIntegrations/versions";

  private final HdsClient hdsClient;

  @Inject
  public IntegrationVersionCacheLoader(final HdsClient hdsClient) {
    this.hdsClient = hdsClient;
  }

  @Override
  public List<IqIntegrationVersion> load(final IntegrationVersionCacheKey key) throws Exception {
    log.debug("Loading integration versions from HDS for {} with limit {}", key.name(), key.supportedVersionCount());

    IqIntegrationVersion[] versions = hdsClient.get(IqIntegrationVersion[].class,
        HDS_INTEGRATION_VERSIONS_PATH,
        Map.of("name", key.name(), "limit", String.valueOf(key.supportedVersionCount())));

    List<IqIntegrationVersion> result = Arrays.asList(versions);
    log.debug("Loaded {} versions for integration {}", result.size(), key.name());
    return result;
  }
}
