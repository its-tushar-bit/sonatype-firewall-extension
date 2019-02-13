/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.insight.brain.HttpRequest;

public class CLIResourceAuthzTest
    extends AbstractScanResourceAuthzTest
{
  @Override
  protected HttpRequest scanRequest() {
    return restRequest().path(CLIResource.RESOURCE_PATH, CLIResource.SCAN_PATH).parameter(app.getPublicId());
  }
}
