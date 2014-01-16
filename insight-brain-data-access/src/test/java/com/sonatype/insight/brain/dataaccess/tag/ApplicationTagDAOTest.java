/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dataaccess.tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @since 1.9
 */
public class ApplicationTagDAOTest
    extends AbstractDbDAOTest
{
  private ApplicationTagDAO dao = new ApplicationTagDAO();

  private Tag tag;

  @Before
  public void before() {
    createDefaultApplication();
    tag = createTag("TestTag name", "TestTag description", organization.getId());
  }

  @Test
  public void testCRUD() throws Exception {
    // Create
    ApplicationTag appTag = new ApplicationTag(applicationId, tag.getId());
    dao.insert(appTag);
    assertThat(appTag.getId(), notNullValue());

    // Get
    appTag = dao.getById(appTag.getId());
    assertThat(appTag, notNullValue());
    assertAppTag(applicationId, tag.getId(), appTag);

    // Delete
    dao.delete(appTag);

    // Get
    appTag = dao.getById(appTag.getId());
    assertThat(appTag, nullValue());
  }

  @Test
  public void testUpdateNotSupported() throws Exception {
    ApplicationTag appTag = new ApplicationTag(applicationId, tag.getId());
    dao.insert(appTag);

    ApplicationTag updatedAppTag = new ApplicationTag("updated_app_id", tag.getId());
    updatedAppTag.setId(appTag.getId());

    try {
      dao.update(updatedAppTag);
      fail("Expected UnsupportedOperationException");
    }
    catch (UnsupportedOperationException expected) {
      //Expected
    }
  }

  @Test
  public void testGetByApplicationId() throws Exception {
    Application app1 = createApplication("one", "one", organization.getId());
    Application app2 = createApplication("two", "two", organization.getId());

    List<Tag> app1Tags = new ArrayList<>();
    List<Tag> app2Tags = new ArrayList<>();

    app1Tags.add(createTag("tag1", "tag1", organization.getId()));
    app1Tags.add(createTag("tag2", "tag2", organization.getId()));
    app2Tags.add(createTag("tag3", "tag3", organization.getId()));
    app2Tags.add(createTag("tag4", "tag4", organization.getId()));

    for (Tag tag : app1Tags) {
      createApplicationTag(app1.getId(), tag.getId());
    }

    for (Tag tag : app2Tags) {
      createApplicationTag(app2.getId(), tag.getId());
    }

    assertAppTags(app1.getId(), app1Tags, dao.getByApplicationId(app1.getId()));
    assertAppTags(app2.getId(), app2Tags, dao.getByApplicationId(app2.getId()));
  }

  @Test
  public void testGetByApplicationIdAndTagId() throws Exception {
    createApplicationTag(applicationId, tag.getId());
    ApplicationTag appTag = dao.getByApplicationIdAndTagId(applicationId, tag.getId());
    assertAppTag(applicationId, tag.getId(), appTag);
  }

  private void assertAppTag(String appId, String tagId, ApplicationTag actual) {
    assertThat(actual.getApplicationId(), is(appId));
    assertThat(actual.getTagId(), is(tagId));
  }

  private void assertAppTags(String appId, List<Tag> expected, List<ApplicationTag> actual) {
    assertThat(actual.size(), is(expected.size()));

    Set<String> tagIds = new HashSet<>();
    for (Tag tag : expected) {
      tagIds.add(tag.getId());
    }

    for (ApplicationTag appTag : actual) {
      assertThat(appTag.getApplicationId(), equalTo(appId));
      assertThat(tagIds.contains(appTag.getTagId()), is(true));
    }
  }
}
