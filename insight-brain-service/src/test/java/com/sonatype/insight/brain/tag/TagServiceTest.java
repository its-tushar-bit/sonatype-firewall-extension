/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.error.exception.NotFoundException;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @since 1.9
 */
public class TagServiceTest extends InjectedTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Inject
  private TagService tagService;

  /**
   * Confirm that if we accidentally try to delete a Tag in the context of
   * the wrong Organization the operation will fail.
   */
  @Test
  public void testDeleteTagFromWrongOrg() throws Exception {
    Organization organization1 = tempEntity.newOrganization();
    Organization organization2 = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(organization1.getId(), "Tag");

    try {
      tagService.deleteTag(organization2.getId(), tag.getId());
      fail("Should have thrown NotFoundException");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is("Cannot find a tag with id " + tag.getId() + " for organization id "
          + organization2.getId()));
    }
  }

  @Test
  public void testDeleteApplicationTag_NotFound() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Application application1 = tempEntity.newApplication(organization.getId());
    Application application2 = tempEntity.newApplication(organization.getId());
    Tag tag = tempEntity.newTag(organization.getId(), "Tag");
    tempEntity.newApplicationTag(application1.getId(), tag.getId());

    try {
      tagService.deleteApplicationTag(application2.getPublicId(), tag.getId());
      fail("Should have thrown NotFoundException");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is("Tag with id " + tag.getId() + " is not applied to application with id "
          + application2.getPublicId()));
    }
  }

  @Test
  public void testGetTagsUsedByApplications() {
    Organization organization1 = tempEntity.newOrganization("testGetTagsUsedByApplicationsOrg1");
    Application application1 = tempEntity.newApplication(organization1.getId());
    // Tag used by application1
    Tag tag1 = tempEntity.newTag(organization1.getId(), "name1");
    tempEntity.newApplicationTag(application1.getId(), tag1.getId());

    Organization organization2 = tempEntity.newOrganization("testGetTagsUsedByApplicationsOrg2");
    Application application2 = tempEntity.newApplication(organization2.getId());
    // Tag used by application2
    Tag tag2 = tempEntity.newTag(organization2.getId(), "name2");
    tempEntity.newApplicationTag(application2.getId(), tag2.getId());

    // Tags not used by any application
    tempEntity.newTag(organization1.getId(), "name3");
    tempEntity.newTag(organization2.getId(), "name4");

    List<Tag> allTags = tagService.getTagsUsedByApplications();
    assertThat(allTags, hasSize(2));
    assertTagInList(allTags, tag1);
    assertTagInList(allTags, tag2);
  }

  @Test
  public void testGetApplicableTagsByApplicationPublicId() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag orgTag = tempEntity.newTag(org.getId(), "orgTag");
    Tag parentOrgTag = tempEntity.newTag(org.getParentOrganizationId(), "parentOrgTag");

    List<Tag> tags = tagService.getApplicableTagsByApplicationPublicId(app.getPublicId());
    assertThat(tags, hasSize(2));
    assertTagInList(tags, orgTag);
    assertTagInList(tags, parentOrgTag);
  }

  private void assertTagInList(List<Tag> tags, Tag expectedTag) {
    for (Tag tag : tags) {
      if (tag.getId().equals(expectedTag.getId())) {
        return;
      }
    }
    fail("Expected a tag with name " + expectedTag.getName());
  }
}