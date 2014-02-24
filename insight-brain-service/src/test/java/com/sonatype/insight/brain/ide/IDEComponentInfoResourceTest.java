/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import com.sonatype.insight.brain.saas.AbstractComponentInfoResourceTest;

public class IDEComponentInfoResourceTest
    extends AbstractComponentInfoResourceTest
{
  @Override
  protected String getResourcePath() {
    return IDEComponentInfoResource.SERVICE_PATH;
  }
}
