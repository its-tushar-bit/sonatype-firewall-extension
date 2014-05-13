/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ComponentDetailResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetApplicationDetailsByHash() throws Exception {
    Application app = tempEntity.newApplicationWithParent("testGetApplicationDetailsByHash");
    String hash = "ababababab";
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash, "groupId", "artifactId", "version");
    Response response = AuthedRestAccess.get(getServiceURL() + "/applications?hash=" + hash);
    assertResponseStatus(200, response);
    ApplicationComponentDetailsDTO[] applicationComponentDetailsDTOs = JsonHelpers.fromJson(response.getResponseBody(),
        ApplicationComponentDetailsDTO[].class);
    assertThat(applicationComponentDetailsDTOs, notNullValue());
    assertThat(applicationComponentDetailsDTOs, arrayWithSize(1));
  }

  @Test
  public void testGetComponentNameByHash() throws Exception {
    Application app = tempEntity.newApplicationWithParent("testGetComponentNameByHash");
    String hash = "ababababab";
    String url = getServiceURL() + "/name?hash=" + hash;

    Response response = AuthedRestAccess.get(url);
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("Unknown component with hash ababababab"));

    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash, "groupId", "artifactId", "version");
    response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);
    String name = response.getResponseBody();
    assertThat(name, is("groupId:artifactId:version"));
  }

  private String getServiceURL() {
    return getRestBaseUrl() + ComponentDetailResource.SERVICE_PATH;
  }
}
