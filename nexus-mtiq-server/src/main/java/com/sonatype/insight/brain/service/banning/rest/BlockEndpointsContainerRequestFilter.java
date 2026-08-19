/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning.rest;

import com.google.common.annotations.VisibleForTesting;
import com.sonatype.insight.brain.banning.BlockIfMultiTenant;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.lang.reflect.Method;
import org.springframework.util.ClassUtils;

/**
 * This filter checks for endpoint methods or resource classes annotated with BlockIfMultiTenant and stop the request to
 * send back a NOT_FOUND response
 */
@jakarta.ws.rs.ext.Provider
// high priority (i.e. low number) to get called before others like LicenseAwareContainerDynamicFeature
@Priority(BlockEndpointsContainerRequestFilter.PRIORITY)
public class BlockEndpointsContainerRequestFilter
    implements ContainerRequestFilter
{
  public static final int PRIORITY = Priorities.AUTHENTICATION / 2;

  private final Provider<ResourceInfo> resourceInfoProvider;

  @Inject
  public BlockEndpointsContainerRequestFilter(Provider<ResourceInfo> resourceInfoProvider) {
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
    Class<?> userClass = ClassUtils.getUserClass(clazz);
    if (blocked == null && userClass != clazz) {
      blocked = userClass.getAnnotation(BlockIfMultiTenant.class);
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
    Class<?> userClass = ClassUtils.getUserClass(method.getDeclaringClass());
    if (blocked == null && userClass != method.getDeclaringClass()) {
      try {
        blocked = userClass.getMethod(method.getName(), method.getParameterTypes())
            .getAnnotation(BlockIfMultiTenant.class);
      }
      catch (NoSuchMethodException e) {
        throw new IllegalStateException(e);
      }
    }

    return blocked != null;
  }
}
