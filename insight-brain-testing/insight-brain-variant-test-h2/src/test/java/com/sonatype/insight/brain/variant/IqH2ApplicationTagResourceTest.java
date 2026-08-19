/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.tag.ApplicationTagResource;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.Assert.assertTag;
import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApplicationTagResourceTest
{
  private IqTestContext ctx;

  @Test
  void testCRUD() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent("testApp");

    HttpRequest request =
        ctx.restRequest().path(ApplicationTagResource.RESOURCE_PATH).parameter(app.getPublicId());

    // Get
    HttpResponse response = request.get();
    ctx.assertResponseStatus(200, response);
    Tag[] retrievedTags = response.getBody(Tag[].class);
    assertThat(retrievedTags).isEmpty();

    // Update
    List<Tag> tags = new ArrayList<>();
    tags.add(ctx.tempEntity().newTag(app.getOrganizationId(), "tag name 1"));
    tags.add(ctx.tempEntity().newTag(app.getOrganizationId(), "tag name 2"));
    response = request.body(tags).put();
    ctx.assertResponseStatus(204, response);

    response = request.get();
    ctx.assertResponseStatus(200, response);
    retrievedTags = response.getBody(Tag[].class);
    assertThat(retrievedTags).hasSize(2);
    Arrays.sort(retrievedTags, Comparator.comparing(Tag::getName));
    assertTag(tags.get(0), retrievedTags[0]);
    assertTag(tags.get(1), retrievedTags[1]);

    // Delete
    response = request.body(Collections.emptyList()).put();
    ctx.assertResponseStatus(204, response);

    // Get
    response = request.get();
    ctx.assertResponseStatus(200, response);
    retrievedTags = response.getBody(Tag[].class);
    assertThat(retrievedTags).isEmpty();
  }
}
