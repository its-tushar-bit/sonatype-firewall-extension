/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import javax.inject.Named;
import javax.ws.rs.Path;

import com.sonatype.insight.brain.saas.AbstractComponentInfoResource;

@Path(IDEComponentInfoResource.SERVICE_PATH)
@Named
public class IDEComponentInfoResource
    extends AbstractComponentInfoResource
{
  public static final String SERVICE_PATH = "rest/ide/component/details";

  @Override
  protected String getToolName() {
    return "ide";
  }
}
