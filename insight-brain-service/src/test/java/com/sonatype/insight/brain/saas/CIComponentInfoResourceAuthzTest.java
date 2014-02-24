/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

public class CIComponentInfoResourceAuthzTest
    extends AbstractComponentInfoResourceAuthzTest
{
  @Override
  protected String getResourcePath() {
    return CIComponentInfoResource.SERVICE_PATH;
  }
}
