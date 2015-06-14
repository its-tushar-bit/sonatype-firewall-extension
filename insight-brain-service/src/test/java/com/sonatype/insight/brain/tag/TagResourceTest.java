/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static com.sonatype.insight.brain.Assert.assertTag;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class TagResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testCRUD() throws Exception {
    Organization org = tempEntity.newOrganization("TagResourceTest");

    HttpRequest request = restRequest().path(TagResource.SERVICE_PATH, TagResource.ORGANIZATION_PATH).parameter(org.getId());
    // Get
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    Tag[] tags = response.getBody(Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(0));

    // Add
    Tag tag = new Tag(org.getId(), "Tag Name", "Tag description", Color.yellow);
    response = request.body(tag).post();
    assertResponseStatus(200, response);
    assertTag(tag, response.getBody(Tag.class));

    // Get
    response = request.get();
    assertResponseStatus(200, response);
    tags = response.getBody(Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(1));
    assertTag(tag, tags[0]);

    // Update
    tag = tags[0];
    tag.setName("Tag Updated Name");
    response = request.body(tag).put();
    assertResponseStatus(200, response);
    assertTag(tag, response.getBody(Tag.class));

    // Get
    response = request.get();
    assertResponseStatus(200, response);
    tags = response.getBody(Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(1));
    assertTag(tag, tags[0]);

    // Delete
    response = request.subpath("{tagId}").parameter(tag.getId()).delete();
    assertResponseStatus(204, response);

    // Get
    response = request.get();
    assertResponseStatus(200, response);
    tags = response.getBody(Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(0));
  }
}
