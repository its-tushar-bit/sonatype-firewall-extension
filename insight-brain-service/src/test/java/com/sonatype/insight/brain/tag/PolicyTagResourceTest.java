/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

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

    HttpRequest request = restRequest().path(PolicyTagResource.RESOURCE_PATH).query("orgId", org.getId())
        .parameter(policyId);

    // Get
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    Tag[] tags = response.getBody(Tag[].class);
    assertThat(tags, is(notNullValue()));
    assertThat(tags.length, is(0));

    // Add
    Tag tag = tempEntity.newTag(org.getId(), "tag name");
    response = request.body(tag).post();
    assertResponseStatus(200, response);
    Tag policyTag = response.getBody(Tag.class);
    assertThat(policyTag, is(notNullValue()));
    assertThat(policyTag.getId(), is(notNullValue()));
    assertThat(policyTag.getId(), is(tag.getId()));
    assertThat(policyTag.getName(), is("tag name"));
    assertThat(policyTag.getOrganizationId(), is(org.getId()));

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
