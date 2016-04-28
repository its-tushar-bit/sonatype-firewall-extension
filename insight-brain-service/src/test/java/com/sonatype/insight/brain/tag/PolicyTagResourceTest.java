/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.Arrays;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static com.sonatype.insight.brain.Assert.assertTag;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PolicyTagResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PolicyTagResource.RESOURCE_PATH);
  }

  @Test
  public void testGetPolicyTags_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    Tag tag1 = tempEntity.newTag(org.getId());
    tempEntity.newTag(org.getId());
    String policyId = tempEntity.newPolicy(org.getId(), "Test").getId();
    tempEntity.newPolicyTag(policyId, tag1.getId());

    HttpResponse response = restRequest().parameter(policyId).query("ownerId", org.getId())
        .query("ownerType", "organization").get();
    assertResponseStatus(200, response);
    Tag[] tags = response.getBody(Tag[].class);
    assertThat(tags, is(arrayWithSize(1)));
    assertTag(tag1, tags[0]);
  }

  @Test
  public void testGetPolicyTags_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent("getpolicytags");
    Tag tag1 = tempEntity.newTag(app.getParentOwnerId());
    tempEntity.newTag(app.getParentOwnerId());
    String policyId = tempEntity.newPolicy(app.getParentOwnerId(), "Test").getId();
    tempEntity.newPolicyTag(policyId, tag1.getId());

    HttpResponse response = restRequest().parameter(policyId).query("ownerId", app.getPublicId())
        .query("ownerType", "application").get();
    assertResponseStatus(200, response);
    Tag[] tags = response.getBody(Tag[].class);
    assertThat(tags, is(arrayWithSize(1)));
    assertTag(tag1, tags[0]);
  }

  @Test
  public void testUpdatePolicyTags() throws Exception {
    Organization org = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(org.getId());
    String policyId = tempEntity.newPolicy(org.getId(), "Test").getId();

    HttpResponse response = restRequest().parameter(policyId).body(Arrays.asList(tag)).put();
    assertResponseStatus(200, response);
    Tag[] tags = response.getBody(Tag[].class);
    assertThat(tags, is(arrayWithSize(1)));
    assertTag(tag, tags[0]);
    tags = new TagDAO().getByPolicyId(policyId).toArray(new Tag[0]);
    assertThat(tags, is(arrayWithSize(1)));
    assertTag(tag, tags[0]);
  }
}
