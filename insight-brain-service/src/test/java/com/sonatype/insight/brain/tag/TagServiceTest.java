/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.tag.TagResource.ApplicableTags;
import com.sonatype.insight.brain.tag.TagResource.AppliedTags;
import com.sonatype.insight.brain.webhook.ManagementEvent.TagEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.model.HasStringId;

import org.junit.Test;

import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @since 1.9
 */
public class TagServiceTest
    extends AbstractComponentTest
{
  @Inject
  private TagService tagService;

  @Inject
  private AsyncEventBus eventBus;

  private Comparator<HasStringId> byId = Comparator.comparing(HasStringId::getId);

  @Test
  public void testAddUpdateAndDeleteTagPostEvents() throws Exception {
    TestEventHandler<TagEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    eventBus.register(handler);

    Organization organization = tempEntity.newOrganization();
    Tag tag = new Tag(organization.getId(), "TAG", "test tag", Color.yellow);

    Tag created = tagService.addTag(organization.getId(), tag);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(CREATED);
    assertThat(handler.getEvent().ownerId).isEqualTo(organization.getId());
    assertThat(handler.getEvent().tag.getId()).isEqualTo(tag.getId());

    handler.setLatch(new CountDownLatch(1));

    created.setDescription("some new description");
    tagService.updateTag(organization.getId(), created);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(UPDATED);
    assertThat(handler.getEvent().ownerId).isEqualTo(organization.getId());
    assertThat(handler.getEvent().tag.getId()).isEqualTo(tag.getId());

    handler.setLatch(new CountDownLatch(1));

    tagService.deleteTag(organization.getId(), created.getId());

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(DELETED);
    assertThat(handler.getEvent().ownerId).isEqualTo(organization.getId());
    assertThat(handler.getEvent().tag.getId()).isEqualTo(tag.getId());

    eventBus.unregister(handler);
  }

  @Test
  public void testUpdateApplicationTags() {
    Application app = tempEntity.newApplicationWithParent("appPublicId");
    Tag tag = tempEntity.newTag(app.getOrganizationId(), "Tag");

    List<ApplicationTag> applicationTags = tagService.updateApplicationTags(app.getPublicId(),
        Collections.singletonList(tag));
    assertThat(applicationTags).hasSize(1);
    assertThat(applicationTags.get(0).getApplicationId()).isEqualTo(app.getId());
    assertThat(applicationTags.get(0).getTagId()).isEqualTo(tag.getId());

    List<Tag> tags = tagService.getAppliedApplicationTags(app.getPublicId());
    assertThat(tags).hasSize(1);
    assertThat(tags.get(0).getName()).isEqualTo(tag.getName());
    assertThat(tags.get(0).getId()).isEqualTo(tag.getId());
    assertThat(tags.get(0).getDescription()).isEqualTo(tag.getDescription());
    assertThat(tags.get(0).getColor()).isEqualTo(tag.getColor());
    assertThat(tags.get(0).getOrganizationId()).isEqualTo(tag.getOrganizationId());
  }

  @Test
  public void testUpdatePolicyTags() {
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization);
    Tag tagOne = tempEntity.newTag(organization.getId());
    Tag tagTwo = tempEntity.newTag(organization.getId());
    Tag tagThree = tempEntity.newTag(organization.getId());
    tempEntity.newPolicyTag(policy.getId(), tagOne.getId());
    tempEntity.newPolicyTag(policy.getId(), tagTwo.getId());

    List<Tag> updatedPolicyTags = new ArrayList<>();
    updatedPolicyTags.add(tagTwo);
    updatedPolicyTags.add(tagThree);

    updatedPolicyTags = tagService
        .updatePolicyTags(OwnerType.ORGANIZATION, organization.getId(), policy.getId(), updatedPolicyTags);
    assertThat(updatedPolicyTags).usingElementComparator(byId).containsExactlyInAnyOrder(tagTwo, tagThree);
  }

  @Test
  public void testUpdatePolicyTags_PolicyNotBelongingToOrg() throws Exception {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(org2);

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      tagService.updatePolicyTags(OwnerType.ORGANIZATION, org1.getId(), policy.getId(), new ArrayList<>());
    }).withMessage("Cannot find a policy with id " + policy.getId() + " for owner id " + org1.getId());
  }

  @Test
  public void testUpdatePolicyTags_AppLevelPolicy() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      tagService.updatePolicyTags(OwnerType.APPLICATION, app.getPublicId(), policy.getId(), new ArrayList<>());
    }).withMessageContaining("policy owned by application");
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

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      tagService.deleteTag(organization2.getId(), tag.getId());
    }).withMessage(
        "Cannot find an application category with id " + tag.getId() + " for organization id " + organization2.getId());
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
    assertThat(allTags).usingElementComparator(byId).containsExactlyInAnyOrder(tag1, tag2);
  }

  @Test
  public void testGetApplicableTagsByApplicationPublicId() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Tag orgTag = tempEntity.newTag(org.getId(), "orgTag");
    Tag parentOrgTag = tempEntity.newTag(org.getParentOrganizationId(), "parentOrgTag");

    List<Tag> tags = tagService.getApplicableTagsByApplicationPublicId(app.getPublicId());
    assertThat(tags).usingElementComparator(byId).containsExactlyInAnyOrder(orgTag, parentOrgTag);
  }

  @Test
  public void testGetApplicableTags() {
    Organization org = tempEntity.newOrganization();
    Organization parentOrg = new OrganizationDAO().getById(org.getParentOrganizationId());
    Tag orgTag = tempEntity.newTag(org.getId(), "Org Tag");
    Tag parentTag = tempEntity.newTag(org.getParentOrganizationId(), "Root Tag");

    ApplicableTags tags = tagService.getApplicableTags(OwnerType.ORGANIZATION, org.getId());
    assertThat(tags.tagsByOwner).hasSize(2);

    assertThat(tags.tagsByOwner.get(0).tags).usingElementComparator(byId).containsExactlyInAnyOrder(orgTag);
    assertThat(tags.tagsByOwner.get(0).ownerName).isEqualTo(org.getName());
    assertThat(tags.tagsByOwner.get(0).ownerId).isEqualTo(org.getId());

    assertThat(tags.tagsByOwner.get(1).tags).usingElementComparator(byId).containsExactlyInAnyOrder(parentTag);
    assertThat(tags.tagsByOwner.get(1).ownerName).isEqualTo(parentOrg.getName());
    assertThat(tags.tagsByOwner.get(1).ownerId).isEqualTo(parentOrg.getId());
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
    assertThat(tags.applicationTagsByOwner).hasSize(2);

    assertThat(tags.applicationTagsByOwner.get(0).applicationTags).usingElementComparator(byId)
        .containsExactlyInAnyOrder(orgTag);
    assertThat(tags.applicationTagsByOwner.get(0).ownerName).isEqualTo(org.getName());
    assertThat(tags.applicationTagsByOwner.get(0).ownerId).isEqualTo(org.getId());

    assertThat(tags.applicationTagsByOwner.get(1).applicationTags).usingElementComparator(byId)
        .containsExactlyInAnyOrder(parentTag);
    assertThat(tags.applicationTagsByOwner.get(1).ownerName).isEqualTo(parentOrg.getName());
    assertThat(tags.applicationTagsByOwner.get(1).ownerId).isEqualTo(parentOrg.getId());
  }

  @Test
  public void testGetAppliedPolicyTags() {
    Organization org = tempEntity.newOrganization();
    Organization parentOrg = new OrganizationDAO().getById(org.getParentOrganizationId());
    Policy policy = tempEntity.newPolicy(org);

    PolicyTag orgTag = tempEntity.newPolicyTag(policy.getId(), tempEntity.newTag(org.getId(), "Org Tag").getId());
    PolicyTag parentTag = tempEntity.newPolicyTag(policy.getId(), tempEntity.newTag(parentOrg.getId(), "Root Tag")
        .getId());

    List<PolicyTag> tags = tagService.getAppliedPolicyTags(org.getId());
    assertThat(tags).usingElementComparator(byId).containsExactlyInAnyOrder(orgTag, parentTag);
  }

  @Test
  public void testUpdateTag_DifferentOwnerId() throws Exception {
    Organization ownerOrg = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(ownerOrg.getId());

    Organization otherOrg = tempEntity.newOrganization();
    tag.setOrganizationId(otherOrg.getId());

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      tagService.updateTag(otherOrg.getId(), tag);
    }).withMessage(
        "Cannot find an application category with id " + tag.getId() + " for organization id " + otherOrg.getId());
  }
}
