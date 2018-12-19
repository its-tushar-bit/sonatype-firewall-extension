/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.insight.brain.HttpRequest;

public class RepoManComponentInfoResourceAuditTest
    extends AbstractComponentInfoResourceAuditBaseTest
{
  @Override
  protected HttpRequest resourceRequest() {
    return restRequest().path(RepoManComponentInfoResource.RESOURCE_PATH,
        RepoManComponentInfoResource.APPLICATION_COMPONENT_DETAILS_PATH);
  }
}
