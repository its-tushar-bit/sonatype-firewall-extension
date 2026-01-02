/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.Collections;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.Assert.assertTag;
import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class PolicyTagResourceTest
    extends AbstractResourceTest
{
  private TagDAO tagDAO;

  @Before
  public void setUp() {
    tagDAO = lookup(TagDAO.class);
  }

  private HttpRequest restRequest(String policyId, OwnerType ownerType, String ownerId) {
    return super.restRequest().path(PolicyTagResource.RESOURCE_PATH).parameter(policyId, ownerType, ownerId);
  }

  @Test
  public void testGetPolicyTags_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    Tag tag1 = tempEntity.newTag(org.getId());
    tempEntity.newTag(org.getId());
    String policyId = tempEntity.newPolicy(org).getId();
    tempEntity.newPolicyTag(policyId, tag1.getId());

    HttpResponse response = restRequest(policyId, OwnerType.ORGANIZATION, org.getId()).get();
    assertResponseStatus(200, response);
    Tag[] tags = response.getBody(Tag[].class);
    assertThat(tags).hasSize(1);
    assertTag(tag1, tags[0]);
  }

  @Test
  public void testGetPolicyTags_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent("getpolicytags");
    Tag tag1 = tempEntity.newTag(app.getParentOwnerId());
    tempEntity.newTag(app.getParentOwnerId());
    String policyId = tempEntity.newPolicy(app.getParentOwnerId()).getId();
    tempEntity.newPolicyTag(policyId, tag1.getId());

    HttpResponse response = restRequest(policyId, OwnerType.APPLICATION, app.getPublicId()).get();
    assertResponseStatus(200, response);
    Tag[] tags = response.getBody(Tag[].class);
    assertThat(tags).hasSize(1);
    assertTag(tag1, tags[0]);
  }

  @Test
  public void testUpdatePolicyTags() throws Exception {
    Organization org = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(org.getId());
    String policyId = tempEntity.newPolicy(org).getId();

    HttpResponse response = restRequest(policyId, OwnerType.ORGANIZATION, org.getId()).body(
        Collections.singletonList(tag)).put();
    assertResponseStatus(200, response);
    Tag[] tags = response.getBody(Tag[].class);
    assertThat(tags).hasSize(1);
    assertTag(tag, tags[0]);
    tags = tagDAO.getByPolicyId(policyId).toArray(new Tag[0]);
    assertThat(tags).hasSize(1);
    assertTag(tag, tags[0]);
  }
}
