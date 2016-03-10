/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.tag.TagResource.ApplicableTags;
import com.sonatype.insight.brain.tag.TagResource.AppliedTags;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @since 1.9
 */
public class TagServiceTest
    extends AbstractComponentTest
{
  @Inject
  private TagService tagService;

  @Test
  public void testUpdateApplicationTags() {
    Application app = tempEntity.newApplicationWithParent("appPublicId");
    Tag tag = tempEntity.newTag(app.getOrganizationId(), "Tag");

    List<ApplicationTag> applicationTags = tagService.updateApplicationTags(app.getPublicId(),
        Collections.singletonList(tag));
    assertThat(applicationTags, hasSize(1));
    assertThat(applicationTags.get(0).getApplicationId(), is(app.getId()));
    assertThat(applicationTags.get(0).getTagId(), is(tag.getId()));

    List<Tag> tags = tagService.getAppliedApplicationTags(app.getPublicId());
    assertThat(tags, hasSize(1));
    assertThat(tags.get(0).getName(), is(tag.getName()));
    assertThat(tags.get(0).getId(), is(tag.getId()));
    assertThat(tags.get(0).getDescription(), is(tag.getDescription()));
    assertThat(tags.get(0).getColor(), is(tag.getColor()));
    assertThat(tags.get(0).getOrganizationId(), is(tag.getOrganizationId()));
  }

  @Test
  public void testUpdatePolicyTags() {
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization.getId(), "testUpdatePolicyTags_Policy");
    Tag tagOne = tempEntity.newTag(organization.getId());
    Tag tagTwo = tempEntity.newTag(organization.getId());
    Tag tagThree = tempEntity.newTag(organization.getId());
    tempEntity.newPolicyTag(policy.getId(), tagOne.getId());
    tempEntity.newPolicyTag(policy.getId(), tagTwo.getId());

    List<Tag> updatedPolicyTags = new ArrayList<>();
    updatedPolicyTags.add(tagTwo);
    updatedPolicyTags.add(tagThree);

    updatedPolicyTags = tagService.updatePolicyTags(organization.getId(), policy.getId(), updatedPolicyTags);
    assertThat(updatedPolicyTags, hasSize(2));
    assertTagInList(updatedPolicyTags, tagTwo);
    assertTagInList(updatedPolicyTags, tagThree);
  }

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
      assertThat(e.getMessage(),
          is("Cannot find an application category with id " + tag.getId() + " for organization id "
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
      assertThat(e.getMessage(),
          is("An application category with id " + tag.getId() + " is not applied to application with id "
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

  @Test
  public void testGetApplicableTags() {
    Organization org = tempEntity.newOrganization();
    Organization parentOrg = new OrganizationDAO().getById(org.getParentOrganizationId());
    Tag orgTag = tempEntity.newTag(org.getId(), "Org Tag");
    Tag parentTag = tempEntity.newTag(org.getParentOrganizationId(), "Root Tag");

    ApplicableTags tags = tagService.getApplicableTags(org.getId());
    assertThat(tags.tagsByOwner, hasSize(2));

    assertThat(tags.tagsByOwner.get(0).tags, hasSize(1));
    assertThat(tags.tagsByOwner.get(0).ownerName, is(org.getName()));
    assertThat(tags.tagsByOwner.get(0).ownerId, is(org.getId()));
    assertTagInList(tags.tagsByOwner.get(0).tags, orgTag);

    assertThat(tags.tagsByOwner.get(1).tags, hasSize(1));
    assertThat(tags.tagsByOwner.get(1).ownerName, is(parentOrg.getName()));
    assertThat(tags.tagsByOwner.get(1).ownerId, is(parentOrg.getId()));
    assertTagInList(tags.tagsByOwner.get(1).tags, parentTag);
  }

  @Test
  public void testGetAppliedTags() {
    Organization org = tempEntity.newOrganization();
    Organization parentOrg = new OrganizationDAO().getById(org.getParentOrganizationId());
    Application application = tempEntity.newApplication(org.getId());
    ApplicationTag orgTag = tempEntity.newApplicationTag(application.getId(), tempEntity.newTag(org.getId(), "Org Tag")
        .getId());
    ApplicationTag parentTag = tempEntity.newApplicationTag(application.getId(),
        tempEntity.newTag(org.getParentOrganizationId(), "Root Tag").getId());

    AppliedTags tags = tagService.getAppliedTags(org.getId());
    assertThat(tags.applicationTagsByOwner, hasSize(2));

    assertThat(tags.applicationTagsByOwner.get(0).applicationTags, hasSize(1));
    assertThat(tags.applicationTagsByOwner.get(0).ownerName, is(org.getName()));
    assertThat(tags.applicationTagsByOwner.get(0).ownerId, is(org.getId()));
    assertTagInList(tags.applicationTagsByOwner.get(0).applicationTags, orgTag);

    assertThat(tags.applicationTagsByOwner.get(1).applicationTags, hasSize(1));

    assertThat(tags.applicationTagsByOwner.get(1).ownerName, is(parentOrg.getName()));
    assertThat(tags.applicationTagsByOwner.get(1).ownerId, is(parentOrg.getId()));
    assertTagInList(tags.applicationTagsByOwner.get(1).applicationTags, parentTag);
  }

  @Test
  public void testGetAppliedPolicyTags() {
    Organization org = tempEntity.newOrganization();
    Organization parentOrg = new OrganizationDAO().getById(org.getParentOrganizationId());
    Policy policy = tempEntity.newPolicy(org.getId(), "policy");

    PolicyTag orgTag = tempEntity.newPolicyTag(policy.getId(), tempEntity.newTag(org.getId(), "Org Tag").getId());
    PolicyTag parentTag = tempEntity.newPolicyTag(policy.getId(), tempEntity.newTag(parentOrg.getId(), "Root Tag")
        .getId());

    List<PolicyTag> tags = tagService.getAppliedPolicyTags(org.getId());
    assertTagInList(tags, orgTag);
    assertTagInList(tags, parentTag);
  }

  private void assertTagInList(List<Tag> tags, Tag expectedTag) {
    for (Tag tag : tags) {
      if (tag.getId().equals(expectedTag.getId())) {
        return;
      }
    }
    fail("Expected a tag with name " + expectedTag.getName());
  }

  private static void assertTagInList(List<ApplicationTag> tags, ApplicationTag expectedTag) {
    for (ApplicationTag tag : tags) {
      if (tag.getId().equals(expectedTag.getId())) {
        return;
      }
    }
    fail("Unable to find matching application tag");
  }

  private static void assertTagInList(List<PolicyTag> tags, PolicyTag expectedTag) {
    for (PolicyTag tag : tags) {
      if (tag.getId().equals(expectedTag.getId())) {
        return;
      }
    }
    fail("Unable to find matching policy tag");
  }
}
