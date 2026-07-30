/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

/**
 * Shared identity surface for scan-based components persisted to repositories. Implemented by
 * {@link ProxyRepositoryComponent} (Firewall proxy-repo persistence) and, once CLM-42787 lands,
 * by {@code HostedRepositoryComponent} (hosted-repo artifacts).
 */
public interface RepositoryComponent
{
  String getRepositoryId();

  String getPathname();

  String getHash();

  String getComponentId();
}
