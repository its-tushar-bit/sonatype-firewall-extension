/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Path;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightWork;

@Path(CIComponentInfoResource.SERVICE_PATH)
@Named
public class CIComponentInfoResource
    extends AbstractComponentInfoResource
{
  public static final String SERVICE_PATH = "rest/ci/component/details";

  @Inject
  public CIComponentInfoResource(SaasClient client, InsightWork work) {
    super(client, work);
  }

  @Override
  protected String getToolName() {
    return "ci";
  }

  @Override
  @Authorize(permission = Permission.READ)
  protected void verifyReadAccess(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId) {
    // the method interceptor for the authz annos is all that matters here
  }
}
