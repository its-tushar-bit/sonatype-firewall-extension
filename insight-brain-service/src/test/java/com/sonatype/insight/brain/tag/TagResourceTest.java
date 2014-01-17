/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class TagResourceTest
    extends AbstractResourceTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Test
  public void testCRUD() throws Exception {
    Organization org = createOrganization("TagResourceTest");

    String url = getRestUrl(TagResource.SERVICE_PATH, org.getId());
    // Get
    Response response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);
    Tag[] tags = JsonHelpers.fromJson(response.getResponseBody(), Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(0));

    // Add
    Tag tag = new Tag(org.getId(), "Tag Name", "Tag description");
    response = AuthedRestAccess.post(url, JsonHelpers.asJson(tag));
    assertResponseStatus(200, response);
    tempEntity.assertTag(tag, JsonHelpers.fromJson(response.getResponseBody(), Tag.class));

    // Get
    response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);
    tags = JsonHelpers.fromJson(response.getResponseBody(), Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(1));
    tempEntity.assertTag(tag, tags[0]);

    // Update
    tag = tags[0];
    tag.setName("Tag Updated Name");
    response = AuthedRestAccess.put(url, JsonHelpers.asJson(tag));
    assertResponseStatus(200, response);
    tempEntity.assertTag(tag, JsonHelpers.fromJson(response.getResponseBody(), Tag.class));

    // Get
    response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);
    tags = JsonHelpers.fromJson(response.getResponseBody(), Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(1));
    tempEntity.assertTag(tag, tags[0]);

    // Delete
    response = AuthedRestAccess.delete(url + "/" + tag.getId());
    assertResponseStatus(204, response);

    // Get
    response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);
    tags = JsonHelpers.fromJson(response.getResponseBody(), Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(0));
  }
}
