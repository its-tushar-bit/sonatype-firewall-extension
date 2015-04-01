/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Path;

@Path(CIComponentInfoResource.SERVICE_PATH)
@Named
public class CIComponentInfoResource
    extends AbstractComponentInfoResource
{
  public static final String SERVICE_PATH = "rest/ci/componentDetails";

  @Inject
  public CIComponentInfoResource(SaasClient client, ComponentDetailsLoader componentDetailsLoader) {
    super(client, componentDetailsLoader);
  }

  @Override
  protected String getToolName() {
    return "ci";
  }
}
