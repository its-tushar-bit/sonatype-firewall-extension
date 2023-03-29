/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.lang.reflect.Method;
import javax.inject.Named;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;

import com.google.common.annotations.VisibleForTesting;

/**
 * Audits the event kind for an Admin REST resource. Worth to highlight is that this request filter can grab the event
 * even if the REST method is never actually invoked (e.g. because its parameters couldn't be deserialized). Put
 * differently, this request filter is the first opportunity where the request path has been mapped to a REST resource,
 * allowing to reason about the specific operation undertaken by the caller.
 */
@Named
public class AdminAuditContainerRequestFilter
    implements ContainerRequestFilter
{
  static final String ADMIN_SERVICE = "*ADMIN SERVICE";

  @Context
  private ResourceInfo resInfo;

  @VisibleForTesting
  public AdminAuditContainerRequestFilter() {}

  @VisibleForTesting
  public AdminAuditContainerRequestFilter(ResourceInfo resInfo) {
    this.resInfo = resInfo;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    Method method = resInfo.getResourceMethod();

    if (method == null) {
      return;
    }

    Audited audited = method.getAnnotation(Audited.class);

    if (audited == null && method.getDeclaringClass().getName().contains("Guice$$")) {
      // workaround for https://github.com/google/guice/issues/201
      // resource classes using AOP (e.g. for @Authorize) get subclassed but the generated subclasses miss the
      // annotations so we have to manually inspect the original class
      try {
        audited = method.getDeclaringClass().getSuperclass().getMethod(method.getName(), method.getParameterTypes())
            .getAnnotation(Audited.class);
      }
      catch (NoSuchMethodException e) {
        throw new IllegalStateException(e);
      }
    }

    if (audited != null) {
      AuditData.get().setEvent(audited.value());
      // TODO: Once we have the access token from Auth0 we should be able to set a proper Id for the Username
      AuditData.get().setUsername(ADMIN_SERVICE);
    }
  }
}
