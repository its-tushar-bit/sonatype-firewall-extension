/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

public class TagResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetTags() throws Exception {
    grantReadPermission(org.getId());

    String url = getRestUrl(TagResource.SERVICE_PATH, org.getId());
    testAuthzGet(url);
  }

  @Test
  public void testAddTag() throws Exception {
    grantWritePermission(org.getId());

    Tag tag = new Tag(org.getId(), "name", "description");
    String url = getRestUrl(TagResource.SERVICE_PATH, org.getId());
    testAuthzPost(url, toJson(tag));
  }

  @Test
  public void testUpdateTag() throws Exception {
    grantWritePermission(org.getId());

    Tag tag = tempEntity.newTag(org.getId(), "name");
    String url = getRestUrl(TagResource.SERVICE_PATH, org.getId());
    testAuthzPut(url, toJson(tag));
  }

  @Test
  public void testDeleteTag() throws Exception {
    grantWritePermission(org.getId());

    Tag tag = tempEntity.newTag(org.getId(), "name");
    String url = getRestUrl(TagResource.SERVICE_PATH + "/{tagId}", org.getId(), tag.getId());
    testAuthzDelete(url);
  }
}
