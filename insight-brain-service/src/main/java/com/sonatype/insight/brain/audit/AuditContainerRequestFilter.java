/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.lang.reflect.Method;

import javax.annotation.Priority;
import javax.inject.Named;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import com.sonatype.insight.brain.service.RestComponent;

/**
 * Audits the event kind for a REST resource. Worth to highlight is that this request filter can grab the event even if
 * the REST method is never actually invoked (e.g. because its parameters couldn't be deserialized). Put differently,
 * this request filter is the first opportunity where the request path has been mapped to a REST resource, allowing to
 * reason about the specific operation undertaken by the caller.
 */
@Named
@Provider
// high priority (i.e. low number) to get called before others like LicenseAwareContainerDynamicFeature
@Priority(value = Priorities.AUTHENTICATION / 2)
class AuditContainerRequestFilter
    implements ContainerRequestFilter, RestComponent
{
  @Context
  private ResourceInfo resInfo;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    Method method = resInfo.getResourceMethod();
    if (method != null) {
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
      }
    }
  }
}
