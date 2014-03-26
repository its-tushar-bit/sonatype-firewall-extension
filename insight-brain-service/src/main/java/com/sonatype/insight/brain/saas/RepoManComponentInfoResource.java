/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Path;

/**
 * Provides data supporting the component information panel (CIP) used by repository managers.
 * 
 * @since 1.10
 */
@Path(RepoManComponentInfoResource.SERVICE_PATH)
@Named
public class RepoManComponentInfoResource
    extends AbstractComponentInfoResource
{
  public static final String SERVICE_PATH = "rest/rm/component/details";

  @Inject
  public RepoManComponentInfoResource(SaasClient client, ComponentDetailsLoader componentDetailsLoader)
  {
    super(client, componentDetailsLoader);
  }

  @Override
  protected String getToolName() {
    return "rm";
  }
}
