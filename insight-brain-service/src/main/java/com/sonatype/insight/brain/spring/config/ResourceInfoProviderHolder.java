/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import jakarta.inject.Provider;
import jakarta.ws.rs.container.ResourceInfo;

/**
 * Holder for a JAX-RS {@link ResourceInfo} provider that is set lazily via
 * {@code @Context} injection on the containing request filter. This allows
 * Spring-managed audit filters to receive the Jersey-injected ResourceInfo
 * without requiring the filter to be managed by Jersey's DI container.
 *
 * <p>
 * Shared between single-tenant {@link JerseyConfiguration} and
 * multi-tenant {@code MtiqJerseyConfiguration} to avoid duplication.
 */
public final class ResourceInfoProviderHolder
    implements Provider<ResourceInfo>
{
  private volatile Provider<ResourceInfo> delegate;

  @Override
  public ResourceInfo get() {
    return delegate == null ? null : delegate.get();
  }

  public void setDelegate(Provider<ResourceInfo> delegate) {
    this.delegate = delegate;
  }
}
