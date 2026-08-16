/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.hds.AbstractComponentInfoResourceAuditBaseTest;
import com.sonatype.insight.brain.variant.LegacyServerTest;

@LegacyServerTest
public class IDEComponentInfoResourceAuditTest
    extends AbstractComponentInfoResourceAuditBaseTest
{
  @Override
  protected HttpRequest resourceRequest() {
    return restRequest()
        .path(IDEComponentInfoResource.RESOURCE_PATH, IDEComponentInfoResource.APPLICATION_COMPONENT_DETAILS_PATH);
  }
}
