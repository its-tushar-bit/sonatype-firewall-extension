/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.Arrays;
import java.util.HashSet;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class SuccessMetricsResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(SuccessMetricsResource.RESOURCE_PATH);
  }

  @Test
  public void testSuccessMetricCRUD() throws Exception {
    String metricsName = "Metrics";
    User tempUser = tempEntity.newUser();
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    SuccessMetricsScopeDTO successMetricsScopeDTO = new SuccessMetricsScopeDTO(
        new HashSet<>(Arrays.asList(app.getId())), null);
    SuccessMetricsDTO successMetricsDTO = new SuccessMetricsDTO(metricsName, successMetricsScopeDTO);
    
    // Create
    HttpRequest request = restRequest().auth(tempUser.getUsername(), tempUser.getPassword());
    HttpResponse response = request.body(successMetricsDTO).post();
    assertResponseStatus(200, response);
    SuccessMetricsDTO result = response.getBody(SuccessMetricsDTO.class);
    assertThat(result, notNullValue());
    assertThat(result.id, notNullValue());
    assertThat(result.name, is(successMetricsDTO.name));
    
    // Get the SuccessMetrics
    response = request.get();
    assertResponseStatus(200, response);
    SuccessMetricsDTO[] results = response.getBody(SuccessMetricsDTO[].class);
    assertThat(results.length, is(1));
    assertThat(results[0].name, is(metricsName));

    // Try to update (unsupported)
    response = restRequest().auth(tempUser.getUsername(), tempUser.getPassword()).body(results[0])
        .subpath("{successMetricsId}").parameter(results[0].id).put();
    assertResponseStatus(405, response);
    assertThat(response.getStatusText(), is("Method Not Allowed"));

    // Delete
    response = restRequest().auth(tempUser.getUsername(), tempUser.getPassword()).subpath("{successMetricsId}")
        .parameter(results[0].id).delete();
    assertResponseStatus(204, response);

    // Get the SuccessMetrics
    response = request.get();
    assertResponseStatus(200, response);
    results = response.getBody(SuccessMetricsDTO[].class);
    assertThat(results.length, is(0));
  }
}
