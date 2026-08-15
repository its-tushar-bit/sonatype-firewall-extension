/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Collections;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.tag.PolicyTagResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.Assert.assertTag;
import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2PolicyTagResourceTest
{
  private IqTestContext ctx;

  private TagDAO tagDAO;

  @BeforeEach
  void setUp() {
    tagDAO = ctx.lookup(TagDAO.class);
  }

  private HttpRequest restRequest(String policyId, OwnerType ownerType, String ownerId) {
    return ctx.restRequest().path(PolicyTagResource.RESOURCE_PATH).parameter(policyId, ownerType, ownerId);
  }

  @Test
  void testGetPolicyTags_Organization() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Tag tag1 = ctx.tempEntity().newTag(org.getId());
    ctx.tempEntity().newTag(org.getId());
    String policyId = ctx.tempEntity().newPolicy(org).getId();
    ctx.tempEntity().newPolicyTag(policyId, tag1.getId());

    HttpResponse response = restRequest(policyId, OwnerType.ORGANIZATION, org.getId()).get();
    ctx.assertResponseStatus(200, response);
    Tag[] tags = response.getBody(Tag[].class);
    assertThat(tags).hasSize(1);
    assertTag(tag1, tags[0]);
  }

  @Test
  void testGetPolicyTags_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent("getpolicytags");
    Tag tag1 = ctx.tempEntity().newTag(app.getParentOwnerId());
    ctx.tempEntity().newTag(app.getParentOwnerId());
    String policyId = ctx.tempEntity().newPolicy(app.getParentOwnerId()).getId();
    ctx.tempEntity().newPolicyTag(policyId, tag1.getId());

    HttpResponse response = restRequest(policyId, OwnerType.APPLICATION, app.getPublicId()).get();
    ctx.assertResponseStatus(200, response);
    Tag[] tags = response.getBody(Tag[].class);
    assertThat(tags).hasSize(1);
    assertTag(tag1, tags[0]);
  }

  @Test
  void testUpdatePolicyTags() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Tag tag = ctx.tempEntity().newTag(org.getId());
    String policyId = ctx.tempEntity().newPolicy(org).getId();

    HttpResponse response = restRequest(policyId, OwnerType.ORGANIZATION, org.getId()).body(
        Collections.singletonList(tag)).put();
    ctx.assertResponseStatus(200, response);
    Tag[] tags = response.getBody(Tag[].class);
    assertThat(tags).hasSize(1);
    assertTag(tag, tags[0]);
    tags = tagDAO.getByPolicyId(policyId).toArray(new Tag[0]);
    assertThat(tags).hasSize(1);
    assertTag(tag, tags[0]);
  }
}
