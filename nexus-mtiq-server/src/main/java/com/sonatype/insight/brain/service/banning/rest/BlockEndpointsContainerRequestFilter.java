/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning.rest;

import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.sonatype.insight.brain.banning.BlockIfMultiTenant;

import com.google.common.annotations.VisibleForTesting;
import ru.vyarus.dropwizard.guice.module.installer.order.Order;

/**
 * This filter checks for endpoint methods or resource classes annotated with BlockIfMultiTenant and stop the request to
 * send back a NOT_FOUND response
 */
@Provider
// high priority (i.e. low number) to get called before others like LicenseAwareContainerDynamicFeature
@Priority(BlockEndpointsContainerRequestFilter.PRIORITY)
@Order(Integer.MAX_VALUE - BlockEndpointsContainerRequestFilter.PRIORITY)
public class BlockEndpointsContainerRequestFilter
    implements ContainerRequestFilter
{
  public static final int PRIORITY = Priorities.AUTHENTICATION / 2;

  private final javax.inject.Provider<ResourceInfo> resourceInfoProvider;

  @Inject
  public BlockEndpointsContainerRequestFilter(javax.inject.Provider<ResourceInfo> resourceInfoProvider) {
    this.resourceInfoProvider = resourceInfoProvider;
  }

  @VisibleForTesting
  public BlockEndpointsContainerRequestFilter(ResourceInfo resInfo) {
    this.resourceInfoProvider = () -> resInfo;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (isClassBlocked() || isMethodBlocked()) {
      requestContext.abortWith(Response.status(Status.NOT_FOUND).build());
    }
  }

  private boolean isClassBlocked() {
    ResourceInfo resInfo = resourceInfoProvider.get();
    Class<?> clazz = resInfo.getResourceClass();

    if (clazz == null) {
      return false;
    }

    BlockIfMultiTenant blocked = clazz.getAnnotation(BlockIfMultiTenant.class);

    if (blocked == null && clazz.getName().contains("Guice$$")) {
      // workaround for https://github.com/google/guice/issues/201
      // resource classes using AOP (e.g. for @Authorize) get subclassed but the generated subclasses miss the
      // annotations, so we have to manually inspect the original class
      blocked = clazz.getSuperclass().getAnnotation(BlockIfMultiTenant.class);
    }

    return blocked != null;
  }

  private boolean isMethodBlocked() {
    ResourceInfo resInfo = resourceInfoProvider.get();
    Method method = resInfo.getResourceMethod();

    if (method == null) {
      return false;
    }

    BlockIfMultiTenant blocked = method.getAnnotation(BlockIfMultiTenant.class);

    if (blocked == null && method.getDeclaringClass().getName().contains("Guice$$")) {
      // workaround for https://github.com/google/guice/issues/201
      // resource classes using AOP (e.g. for @Authorize) get subclassed but the generated subclasses miss the
      // annotations, so we have to manually inspect the original class
      try {
        blocked = method.getDeclaringClass().getSuperclass().getMethod(method.getName(), method.getParameterTypes())
            .getAnnotation(BlockIfMultiTenant.class);
      }
      catch (NoSuchMethodException e) {
        throw new IllegalStateException(e);
      }
    }

    return blocked != null;
  }
}
