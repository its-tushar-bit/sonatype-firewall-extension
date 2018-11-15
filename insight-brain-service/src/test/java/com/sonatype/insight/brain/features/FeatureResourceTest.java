/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import java.util.Arrays;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FeatureResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(FeaturesResource.RESOURCE_PATH);
  }

  @Test
  public void testFeatures_Licensed() throws Exception {
    // Get all features
    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);
    assertNotNull(features);
    assertTrue(Arrays.asList(features).contains("policy"));
    assertTrue(Arrays.asList(features).contains("labels"));
    assertTrue(Arrays.asList(features).contains("policy-violations"));
  }

  @Test
  public void testFeatures_Unlicensed() throws Exception {
    // Get all features
    uninstallLicense();
    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);
    assertThat(features, is(notNullValue()));
    assertThat(features, is(emptyArray()));
  }
}
