/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class TagServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private TagService tagService;

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
    Tag tag = new Tag(org.getId(), "name", "description");
    tagService.addTag(org.getId(), tag);
  }

  @Test
  public void testAddTag_Authorized() throws Exception {
    grantWritePermission(org.getId());
    Tag tag = new Tag(org.getId(), "name", "description");
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
}
