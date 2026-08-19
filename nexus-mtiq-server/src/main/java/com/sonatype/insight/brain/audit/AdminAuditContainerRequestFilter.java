/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import static com.sonatype.insight.brain.api.admin.authorization.AuthContextProperties.SUBJECT_USER;

import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import java.lang.reflect.Method;
import org.springframework.util.ClassUtils;

/**
 * Audits the event kind for an Admin REST resource. Worth to highlight is that this request filter can grab the event
 * even if the REST method is never actually invoked (e.g. because its parameters couldn't be deserialized). Put
 * differently, this request filter is the first opportunity where the request path has been mapped to a REST resource,
 * allowing to reason about the specific operation undertaken by the caller.
 */
@jakarta.ws.rs.ext.Provider
public class AdminAuditContainerRequestFilter
    implements ContainerRequestFilter
{
  private final Provider<ResourceInfo> resourceInfoProvider;

  @Inject
  public AdminAuditContainerRequestFilter(Provider<ResourceInfo> resourceInfoProvider) {
    this.resourceInfoProvider = resourceInfoProvider;
  }

  @VisibleForTesting
  public AdminAuditContainerRequestFilter(ResourceInfo resInfo) {
    this.resourceInfoProvider = () -> resInfo;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    ResourceInfo resInfo = resourceInfoProvider.get();
    Method method = resInfo.getResourceMethod();

    if (method == null) {
      return;
    }

    Audited audited = method.getAnnotation(Audited.class);

    Class<?> userClass = ClassUtils.getUserClass(method.getDeclaringClass());
    if (audited == null && userClass != method.getDeclaringClass()) {
      try {
        audited = userClass.getMethod(method.getName(), method.getParameterTypes()).getAnnotation(Audited.class);
      }
      catch (NoSuchMethodException e) {
        throw new IllegalStateException(e);
      }
    }

    if (audited != null) {
      AuditData.get().setEvent(audited.value());

      String subjectUser = (String) requestContext.getProperty(SUBJECT_USER);
      if (subjectUser != null) {
        AuditData.get().setUsername(subjectUser);
      }
    }
  }
}
