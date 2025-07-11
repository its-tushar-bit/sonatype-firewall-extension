/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.opensearch;

import java.net.URI;

import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;
import com.sonatype.insight.brain.search.SearchIndexFixture;
import com.sonatype.insight.brain.search.SearchIndexRuleAnnotations.OpenSearchHttpTest;

import org.opensearch.testcontainers.OpensearchContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.DockerImageName;

/**
 * {@link SearchIndexFixture} for OpenSearch using Test Containers
 */
public class OpenSearchHttpSearchIndexFixture
    implements SearchIndexFixture
{
  private static final Logger log = LoggerFactory.getLogger(OpenSearchHttpSearchIndexFixture.class);

  // Note: This is the OpenSearch *SERVER* version.
  // In the root pom.xml is the client `opensearch-client.version` which doesn't exactly match
  private static final DockerImageName OPENSEARCH_IMAGE = DockerImageName.parse("opensearchproject/opensearch:2.19.2");

  protected final OpensearchContainer container;

  @SuppressWarnings("unused")
  public OpenSearchHttpSearchIndexFixture(final String testName, final OpenSearchHttpTest openSearchHttpTest) {
    log.info("Creating new OpenSearch test fixture for test '{}'", testName);

    container = new OpensearchContainer<>(OPENSEARCH_IMAGE);
    container.start();
  }

  @Override
  public SearchConfig getSearchConfig() {
    HttpOpenSearchConfig searchConfig = new HttpOpenSearchConfig();
    String httpAddress = container.getHttpHostAddress();
    searchConfig.setUri(URI.create(httpAddress));
    searchConfig.setUsername(container.getUsername());
    searchConfig.setPassword(container.getPassword());
    return searchConfig;
  }

  @Override
  public boolean isFixtureReusable() {
    return false;
  }

  @Override
  public void close() throws Exception {
    container.close();
  }
}
