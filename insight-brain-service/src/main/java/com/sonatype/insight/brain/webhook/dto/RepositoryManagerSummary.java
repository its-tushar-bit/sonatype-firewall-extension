/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook.dto;

import com.sonatype.insight.brain.model.repository.RepositoryManager;

public class RepositoryManagerSummary
{
  public final String id;

  public final String instanceId;

  public final String name;

  public final String productName;

  public final String productVersion;

  public final String baseUrl;

  public final boolean configured;

  public RepositoryManagerSummary(final RepositoryManager repositoryManager) {
    if (repositoryManager == null) {
      throw new IllegalArgumentException("RepositoryManager cannot be null");
    }
    this.id = repositoryManager.getId();
    this.instanceId = repositoryManager.getInstanceId();
    this.name = repositoryManager.getName();
    this.productName = repositoryManager.getProductName();
    this.productVersion = repositoryManager.getProductVersion();
    this.baseUrl = repositoryManager.getBaseUrl();
    this.configured = repositoryManager.isConfigured();
  }
}
