/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.service.AbstractLicenseTest;
import com.sonatype.insight.brain.utils.IdUtils;

import org.junit.Assert;
import org.junit.Test;

public class PolicyClientTest
    extends AbstractLicenseTest
{
  @Test
  public void testLinkToManagement() throws Exception {
    String appId = "app id";
    PolicyClient policyClient = new PolicyClient(brain.getClientConfiguration(), appId);
    UriBuilder uriBuilder = UriBuilder.fromPath(brain.getClientConfiguration().getServerUrl());
    uriBuilder.path(UserInterfaceLinksResource.SERVICE_PATH).path(UserInterfaceLinksResource.MANAGEMENT_PATH);
    Assert.assertEquals(policyClient.linkToManagement(), uriBuilder.build(IdUtils.TYPE_APPLICATION, appId).toString());
  }
}
