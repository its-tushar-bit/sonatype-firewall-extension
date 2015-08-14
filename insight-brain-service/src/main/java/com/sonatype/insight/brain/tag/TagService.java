/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.tag.TagResource.ApplicableTags;
import com.sonatype.insight.brain.tag.TagResource.ApplicationTagsByOwner;
import com.sonatype.insight.brain.tag.TagResource.AppliedTags;
import com.sonatype.insight.brain.tag.TagResource.TagsByOwner;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.Iterables;

@Named
/**
 * @since 1.9
 */
class TagService
{
  private final ApplicationService applicationService;

  @Inject
  public TagService(ApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  public List<Tag> getTagsUsedByApplications() {
    List<Application> applications = applicationService.getApplications();
    List<Tag> allTags = new ArrayList<>();

    for (Application application : applications) {
      final List<Tag> applicationTags = new TagDAO().getByApplicationId(application.getId());

      for (final Tag tag : applicationTags) {
        if (!Iterables.any(allTags, IdUtils.getIsEqualPredicate(tag))) {
          allTags.add(tag);
        }
      }
    }

    return allTags;
  }

  @Authorize(permission = Permission.READ)
  public ApplicableTags getApplicableTags(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId) {
    TagDAO tagDAO = new TagDAO();
    OwnerDAO ownerDAO = new OwnerDAO();

    ApplicableTags tags = new ApplicableTags();
    tags.tagsByOwner = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(organizationId)) {
      tags.tagsByOwner.add(new TagsByOwner(owner, tagDAO.getByOrganizationId(owner.getId())));
    }

    return tags;
  }

  @Authorize(permission = Permission.WRITE)
  public Tag addTag(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId, Tag tag) {
    tag.setId(null);
    tag.setOrganizationId(organizationId);
    new TagDAO().insert(tag);

    return tag;
  }

  @Authorize(permission = Permission.WRITE)
  public Tag updateTag(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId, Tag tag) {
    tag.setOrganizationId(organizationId);
    new TagDAO().update(tag);

    return tag;
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteTag(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId, String tagId) {
    TagDAO tagDAO = new TagDAO();
    Tag tag = tagDAO.getByIdNotNull(tagId);
    if (!organizationId.equals(tag.getOrganizationId())) {
      throw new NotFoundException("Cannot find a tag with id " + tagId + " for organization id " + organizationId);
    }
    tagDAO.delete(tag);
  }

  @Authorize(permission = Permission.READ)
  public List<Tag> getAppliedApplicationTags(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId) {
    return new TagDAO().getByApplicationId(IdUtils.getInternalOwnerId(OwnerType.APPLICATION, applicationPublicId));
  }

  @Authorize(permission = Permission.READ)
  public AppliedTags getAppliedTags(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId)
  {
    ApplicationTagDAO applicationTagDAO = new ApplicationTagDAO();
    OwnerDAO ownerDAO = new OwnerDAO();

    AppliedTags entities = new AppliedTags();
    entities.applicationTagsByOwner = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(organizationId)) {
      ApplicationTagsByOwner appTags = new ApplicationTagsByOwner(owner,
          applicationTagDAO.getByOrganizationId(owner.getId()));
      entities.applicationTagsByOwner.add(appTags);
    }

    return entities;
  }

  @Authorize(permission = Permission.WRITE)
  public ApplicationTag addApplicationTag(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId, Tag tag)
  {
    ApplicationTag appTag = new ApplicationTag(IdUtils.getInternalOwnerId(OwnerType.APPLICATION, applicationPublicId),
        tag.getId());
    new ApplicationTagDAO().insert(appTag);
    return appTag;
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteApplicationTag(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      String tagId)
  {
    ApplicationTagDAO appTagDAO = new ApplicationTagDAO();
    ApplicationTag appTag = appTagDAO.getByApplicationIdAndTagId(
        IdUtils.getInternalOwnerId(OwnerType.APPLICATION, applicationPublicId), tagId);
    if(appTag == null) {
      throw new NotFoundException("Tag with id " + tagId + " is not applied to application with id " + applicationPublicId);
    }

    appTagDAO.delete(appTag);
  }

  @Authorize(permission = Permission.READ)
  public List<Tag> getPolicyTags(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String orgId, String policyId) {
    return new TagDAO().getByPolicyId(policyId);
  }

  @Authorize(permission = Permission.WRITE)
  public Tag addPolicyTag(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String orgId, String policyId, Tag tag)
  {
    final String tagId = tag.getId();
    PolicyTag policyTag = new PolicyTag(policyId, tag.getId());
    new PolicyTagDAO().insert(policyTag);
    return new TagDAO().getById(tagId);
  }

  @Authorize(permission = Permission.WRITE)
  public void deletePolicyTag(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String orgId, String policyId,
      String tagId)
  {
    PolicyTagDAO policyTagDAO = new PolicyTagDAO();
    PolicyTag policyTag = policyTagDAO.getByPolicyIdAndTagId(policyId, tagId);
    if (policyTag == null) {
      throw new NotFoundException("Tag with id " + tagId + " is not associated with policy with id " + policyId);
    }

    policyTagDAO.delete(policyTag);
  }

  @Authorize(permission = Permission.READ)
  public List<PolicyTag> getAppliedPolicyTags(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId)
  {
    PolicyTagDAO policyTagDAO = new PolicyTagDAO();
    OwnerDAO ownerDAO = new OwnerDAO();

    List<PolicyTag> policyTags = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(organizationId)) {
      policyTags.addAll(policyTagDAO.getByOrganizationId(owner.getId()));
    }

    return policyTags;
  }

  @Authorize(permission = Permission.READ)
  public List<Tag> getApplicableTagsByApplicationPublicId(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) final String applicationPublicId)
  {
    List<Tag> result = new ArrayList<>();

    ApplicationDAO applicationDAO = new ApplicationDAO();
    OrganizationDAO organizationDAO = new OrganizationDAO();
    TagDAO tagDAO = new TagDAO();

    String organizationId = applicationDAO.getByPublicIdNotNull(applicationPublicId).getOrganizationId();
    while (organizationId != null) {
      result.addAll(tagDAO.getByOrganizationId(organizationId));
      organizationId = organizationDAO.getById(organizationId).getParentOrganizationId();
    }
    return result;
  }
}
