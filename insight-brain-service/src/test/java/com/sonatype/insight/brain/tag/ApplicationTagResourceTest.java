/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ApplicationTagResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testCRUD() throws Exception {
    Application app = tempEntity.newApplicationWithParent("testApp");

    String url = getRestUrl(ApplicationTagResource.SERVICE_PATH, app.getPublicId());

    //Get
    Response response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);
    Tag[] tags = JsonHelpers.fromJson(response.getResponseBody(), Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(0));

    //Add
    Tag tag = tempEntity.newTag(app.getOrganizationId(), "tag name");
    response = AuthedRestAccess.post(url, JsonHelpers.asJson(tag));
    assertResponseStatus(204, response);

    //Get
    response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);
    tags = JsonHelpers.fromJson(response.getResponseBody(), Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(1));
    tempEntity.assertTag(tag, tags[0]);

    //Delete
    response = AuthedRestAccess.delete(url + "/" + tag.getId());
    assertResponseStatus(204, response);

    //Get
    response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);
    tags = JsonHelpers.fromJson(response.getResponseBody(), Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(0));
  }
}
