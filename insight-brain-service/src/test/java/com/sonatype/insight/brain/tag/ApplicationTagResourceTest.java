/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import org.junit.Test;

import static com.sonatype.insight.brain.Assert.assertTag;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ApplicationTagResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testCRUD() throws Exception {
    Application app = tempEntity.newApplicationWithParent("testApp");

    HttpRequest request = restRequest().path(ApplicationTagResource.SERVICE_PATH).parameter(app.getPublicId());

    //Get
    Response response = request.get();
    assertResponseStatus(200, response);
    Tag[] tags = fromJson(response, Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(0));

    //Add
    Tag tag = tempEntity.newTag(app.getOrganizationId(), "tag name");
    response = request.body(tag).post();
    assertResponseStatus(204, response);

    //Get
    response = request.get();
    assertResponseStatus(200, response);
    tags = fromJson(response, Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(1));
    assertTag(tag, tags[0]);

    //Delete
    response = request.subpath("{tagId}").parameter(tag.getId()).delete();
    assertResponseStatus(204, response);

    //Get
    response = request.get();
    assertResponseStatus(200, response);
    tags = fromJson(response, Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(0));
  }
}
