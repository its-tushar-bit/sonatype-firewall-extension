/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class TagServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private TagService tagService;

  String policyId = "TagServiceAuthzTest_PolicyId";

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
  public void testApplyTagToApplication_Unauthorized() throws Exception {
    grantReadPermission(app.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tagService.applyTagToApplication(app.getPublicId(), tag);
  }

  @Test
  public void testApplyTagToApplication_Authorized() throws Exception {
    grantWritePermission(app.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tagService.applyTagToApplication(app.getPublicId(), tag);
  }

  @Test(expected = UnauthorizedException.class)
  public void testRemoveApplicationTag_Unauthorized() throws Exception {
    grantReadPermission(app.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tempEntity.newApplicationTag(app.getId(), tag.getId());
    tagService.removeApplicationTag(app.getPublicId(), tag.getId());
  }

  @Test
  public void testRemoveApplicationTag_Authorized() throws Exception {
    grantWritePermission(app.getId());
    Tag tag = tempEntity.newTag(org.getId(), "name");
    tempEntity.newApplicationTag(app.getId(), tag.getId());
    tagService.removeApplicationTag(app.getPublicId(), tag.getId());
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
}
