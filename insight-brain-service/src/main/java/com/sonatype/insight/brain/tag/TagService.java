/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.List;

import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
/**
 * @since 1.9
 */
class TagService
{
  @Authorize(permission = Permission.READ)
  public List<Tag> getTags(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId) {
    return new TagDAO().getByOrganizationId(organizationId);
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
    return new TagDAO().getByApplicationId(IdUtils.getInternalOwnerId(IdUtils.TYPE_APPLICATION, applicationPublicId));
  }

  @Authorize(permission = Permission.READ)
  public List<ApplicationTag> getApplicationTagsByOrgId(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId) {
    return new ApplicationTagDAO().getByOrganizationId(organizationId);
  }

  @Authorize(permission = Permission.WRITE)
  public ApplicationTag addApplicationTag(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId, Tag tag)
  {
    ApplicationTag appTag = new ApplicationTag(IdUtils.getInternalOwnerId(IdUtils.TYPE_APPLICATION, applicationPublicId), tag.getId());
    new ApplicationTagDAO().insert(appTag);
    return appTag;
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteApplicationTag(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      String tagId)
  {
    ApplicationTagDAO appTagDAO = new ApplicationTagDAO();
    ApplicationTag appTag = appTagDAO.getByApplicationIdAndTagId(IdUtils.getInternalOwnerId(IdUtils.TYPE_APPLICATION, applicationPublicId), tagId);
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
  public PolicyTag addPolicyTag(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String orgId, String policyId, Tag tag)
  {
    PolicyTag policyTag = new PolicyTag(policyId, tag.getId());
    new PolicyTagDAO().insert(policyTag);
    return policyTag;
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
  public List<PolicyTag> getPoliciesByTag(final String tagId) {
    PolicyTagDAO policyTagDAO = new PolicyTagDAO();
    return policyTagDAO.getByTagId(tagId);
  }
}
