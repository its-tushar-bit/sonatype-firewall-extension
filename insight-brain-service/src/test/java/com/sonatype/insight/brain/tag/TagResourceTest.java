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
import com.sonatype.insight.brain.tag.TagResource.ApplicableTags;

import org.junit.Test;

import static com.sonatype.insight.brain.Assert.assertTag;
import static org.assertj.core.api.Assertions.assertThat;

public class TagResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testCRUD() throws Exception {
    Organization org = tempEntity.newOrganization("TagResourceTest");
    tempEntity.newTag(org.getParentOrganizationId(), "Root Tag");

    HttpRequest request = restRequest().path(TagResource.RESOURCE_PATH, TagResource.ORGANIZATION_PATH).parameter(
        org.getId());
    // Get
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    ApplicableTags tags = response.getBody(ApplicableTags.class);
    assertThat(tags).isNotNull();
    assertThat(tags.tagsByOwner).hasSize(2);
    assertThat(tags.tagsByOwner.get(0).tags).hasSize(0);
    assertThat(tags.tagsByOwner.get(1).tags).hasSize(1);

    // Add
    Tag tag = new Tag(org.getId(), "Tag Name", "Tag description", Color.yellow);
    response = request.body(tag).post();
    assertResponseStatus(200, response);
    assertTag(tag, response.getBody(Tag.class));

    // Get
    response = request.get();
    assertResponseStatus(200, response);
    tags = response.getBody(ApplicableTags.class);
    assertThat(tags.tagsByOwner).hasSize(2);
    assertThat(tags.tagsByOwner.get(0).tags).hasSize(1);
    assertTag(tag, tags.tagsByOwner.get(0).tags.get(0));

    // Update
    tag = tags.tagsByOwner.get(0).tags.get(0);
    tag.setName("Tag Updated Name");
    response = request.body(tag).put();
    assertResponseStatus(200, response);
    assertTag(tag, response.getBody(Tag.class));

    // Get
    response = request.get();
    assertResponseStatus(200, response);
    tags = response.getBody(ApplicableTags.class);
    assertThat(tags).isNotNull();
    assertThat(tags.tagsByOwner).hasSize(2);
    assertThat(tags.tagsByOwner.get(0).tags).hasSize(1);
    assertTag(tag, tags.tagsByOwner.get(0).tags.get(0));

    // Delete
    response = request.subpath("{tagId}").parameter(tag.getId()).delete();
    assertResponseStatus(204, response);

    // Get
    response = request.get();
    assertResponseStatus(200, response);
    tags = response.getBody(ApplicableTags.class);
    assertThat(tags).isNotNull();
    assertThat(tags.tagsByOwner).hasSize(2);
    assertThat(tags.tagsByOwner.get(0).tags).hasSize(0);
  }
}
