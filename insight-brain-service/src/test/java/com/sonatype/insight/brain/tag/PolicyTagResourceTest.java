/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static com.sonatype.insight.brain.Assert.assertTag;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class PolicyTagResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testCRUD() throws Exception {
    Organization org = tempEntity.newOrganization("PolicyTagResourceTest");

    String policyId = tempEntity.newPolicy(org.getId(), "PolicyTagResourceTest").getId();

    Tag tagOne = tempEntity.newTag(org.getId(), "tag one");
    Tag tagTwo = tempEntity.newTag(org.getId(), "tag two");

    HttpRequest request = restRequest().path(PolicyTagResource.RESOURCE_PATH).query("orgId", org.getId())
        .parameter(policyId);

    // Get
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    Tag[] tags = response.getBody(Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(0));

    // Add
    response = request.body(tagOne).post();
    assertResponseStatus(200, response);
    Tag policyTag = response.getBody(Tag.class);
    assertThat(policyTag, is(notNullValue()));
    assertThat(policyTag.getId(), is(notNullValue()));
    assertThat(policyTag.getId(), is(tagOne.getId()));
    assertThat(policyTag.getName(), is("tag one"));
    assertThat(policyTag.getOrganizationId(), is(org.getId()));

    // Get
    response = request.get();
    assertResponseStatus(200, response);
    tags = response.getBody(Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(1));
    assertTag(tagOne, tags[0]);

    // Update
    List<Tag> updatedTags = Collections.singletonList(tagTwo);
    response = request.body(updatedTags).put();
    assertResponseStatus(200, response);
    tags = response.getBody(Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(1));
    assertTag(tagTwo, tags[0]);

    // Delete
    response = request.subpath("{tagId}").parameter(tagTwo.getId()).delete();
    assertResponseStatus(204, response);

    // Get
    response = request.get();
    assertResponseStatus(200, response);
    tags = response.getBody(Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(0));
  }
}
