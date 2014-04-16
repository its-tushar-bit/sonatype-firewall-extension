/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class TagServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private TagService tagService;

  String policyId = "TagServiceAuthzTest_PolicyId";

  @Before
  public void init() {
    Organization org = tempEntity.newOrganization("TagServiceAuthzTest");
    policyId = tempEntity.newPolicy(org.getId(), "TagServiceAuthzTest").getId();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetTags_Unauthorized() throws Exception {
    login();
    tagService.getTags(org.getId());
  }

  @Test
  public void testGetTags_Authorized() throws Exception {
    grantReadPermission(org.getId());
    tagService.getTags(org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddTag_Unauthorized() throws Exception {
    grantReadPermission(org.getId());
    Tag tag = new Tag(org.getId(), "name", "description", Color.yellow);
    tagService.addTag(org.getId(), tag);
  }

  @Test
  public void testAddTag_Authorized() throws Exception {
    grantWritePermission(org.getId());
    Tag tag = new Tag(org.getId(), "name", "description", Color.yellow);
    tagService.addTag(org.getId(), tag);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateTag_Unauthorized() throws Exception {
    grantReadPermission(org.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tagService.updateTag(org.getId(), tag);
  }

  @Test
  public void testUpdateTag_Authorized() throws Exception {
    grantWritePermission(org.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tagService.updateTag(org.getId(), tag);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteTag_Unauthorized() throws Exception {
    grantReadPermission(org.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tagService.deleteTag(org.getId(), tag.getId());
  }

  @Test
  public void testDeleteTag_Authorized() throws Exception {
    grantWritePermission(org.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tagService.deleteTag(org.getId(), tag.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAppliedApplicationTags_Unauthorized() throws Exception {
    login();
    tagService.getAppliedApplicationTags(app.getPublicId());
  }

  @Test
  public void testGetAppliedApplicationTags_Authorized() throws Exception {
    grantReadPermission(app.getId());
    tagService.getAppliedApplicationTags(app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddApplicationTag_Unauthorized() throws Exception {
    grantReadPermission(app.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tagService.addApplicationTag(app.getPublicId(), tag);
  }

  @Test
  public void testAddApplicationTag_Authorized() throws Exception {
    grantWritePermission(app.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tagService.addApplicationTag(app.getPublicId(), tag);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteApplicationTag_Unauthorized() throws Exception {
    grantReadPermission(app.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tempEntity.newApplicationTag(app.getId(), tag.getId());
    tagService.deleteApplicationTag(app.getPublicId(), tag.getId());
  }

  @Test
  public void testDeleteApplicationTag_Authorized() throws Exception {
    grantWritePermission(app.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tempEntity.newApplicationTag(app.getId(), tag.getId());
    tagService.deleteApplicationTag(app.getPublicId(), tag.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyTags_Unauthorized() throws Exception {
    login();
    tagService.getPolicyTags(org.getId(), policyId);
  }

  @Test
  public void testGetPolicyTags_Authorized() throws Exception {
    grantReadPermission(org.getId());
    tagService.getPolicyTags(org.getId(), policyId);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddPolicyTag_Unauthorized() throws Exception {
    grantReadPermission(org.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tagService.addPolicyTag(org.getId(), policyId, tag);
  }

  @Test
  public void testAddPolicyTag_Authorized() throws Exception {
    grantWritePermission(org.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tagService.addPolicyTag(org.getId(), policyId, tag);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeletePolicyTag_Unauthorized() throws Exception {
    grantReadPermission(org.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tempEntity.newPolicyTag(policyId, tag.getId());
    tagService.deletePolicyTag(org.getId(), policyId, tag.getId());
  }

  @Test
  public void testDeletePolicyTag_Authorized() throws Exception {
    grantWritePermission(org.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tempEntity.newPolicyTag(policyId, tag.getId());
    tagService.deletePolicyTag(org.getId(), policyId, tag.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicationTagsByOrgId_Unauthorized() throws Exception {
    login();
    tagService.getApplicationTagsByOrgId(org.getId());
  }

  @Test
  public void testGetApplicationTagsByOrgId_Authorized() throws Exception {
    grantReadPermission(org.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tempEntity.newApplicationTag(app.getId(), tag.getId());
    tagService.getApplicationTagsByOrgId(org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyTagsByOrgId_Unauthorized() throws Exception {
    login();
    tagService.getPolicyTagsByOrgId(org.getId());
  }

  @Test
  public void testGetPolicyTagsByOrgId_Authorize() throws Exception {
    grantReadPermission(org.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tempEntity.newPolicyTag(policyId, tag.getId());
    tagService.getPolicyTagsByOrgId(org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetTagsByApplicationPublicId_Unauthorized() throws Exception {
    login();
    tagService.getTagsByApplicationPublicId(app.getPublicId());
  }

  @Test
  public void testGetTagsByApplicationPublicId_Authorized() throws Exception {
    grantReadPermission(app.getId());
    tagService.getTagsByApplicationPublicId(app.getPublicId());
  }

  @Test
  public void testGetAllTagsWithReadPermission() {
    Organization organization1 = tempEntity.newOrganization("testGetAllTagsOrg1");
    Application application1 = tempEntity.newApplication(organization1.getId());
    Tag tag1 = tempEntity.newTag(organization1.getId());
    tempEntity.newApplicationTag(application1.getId(), tag1.getId());
    grantReadPermission(application1.getId());

    Organization organization2 = tempEntity.newOrganization("testGetAllTagsOrg2");
    Application application2 = tempEntity.newApplication(organization2.getId());
    Tag tag2 = tempEntity.newTag(organization2.getId());
    tempEntity.newApplicationTag(application2.getId(), tag2.getId());

    List<Tag> allTags = tagService.getTagsUsedByApplications();
    assertThat(allTags, hasSize(1));
    assertThat(allTags.get(0).getId(), is(tag1.getId()));
  }
}
