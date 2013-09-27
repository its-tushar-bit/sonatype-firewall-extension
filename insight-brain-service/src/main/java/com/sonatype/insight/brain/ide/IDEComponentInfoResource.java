/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Path;

import com.sonatype.insight.brain.saas.AbstractComponentInfoResource;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.brain.service.InsightWork;

@Path(IDEComponentInfoResource.SERVICE_PATH)
@Named
public class IDEComponentInfoResource
    extends AbstractComponentInfoResource
{
  public static final String SERVICE_PATH = "rest/ide/component/details";

  @Inject
  public IDEComponentInfoResource(SaasClient client, InsightWork work) {
    super(client, work);
  }

  @Override
  protected String getToolName() {
    return "ide";
  }
}
