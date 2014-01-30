/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import java.util.Arrays;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class FeatureResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testFeatures_Licensed() throws Exception {
    // Get all features
    Response response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    String[] features = JsonHelpers.fromJson(response.getResponseBody(), String[].class);
    Assert.assertNotNull(features);
    Assert.assertTrue(Arrays.asList(features).contains("policy"));
    Assert.assertTrue(Arrays.asList(features).contains("labels"));
    Assert.assertTrue(Arrays.asList(features).contains("policy-violations"));
  }

  @Test
  public void testFeatures_Unlicensed() throws Exception {
    // Get all features
    uninstallLicense();
    Response response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    String[] features = JsonHelpers.fromJson(response.getResponseBody(), String[].class);
    Assert.assertThat(features, is(notNullValue()));
    Assert.assertThat(features, is(emptyArray()));
  }

  private String getServiceURL() {
    return getRestBaseUrl() + FeaturesResource.SERVICE_PATH;
  }
}
