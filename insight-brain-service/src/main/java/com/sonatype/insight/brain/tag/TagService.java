/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationCategoryDTO;
import com.sonatype.insight.brain.audit.ApplicationCategoryAuditDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.webhook.ManagementEventService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;

@Named
/**
 * @since 1.9
 */
public class TagService
{
  private final ApplicationService applicationService;

  private final ApplicationTagDAO applicationTagDAO;

  private final TagDAO tagDAO;

  private final OwnerDAO ownerDAO;

  private final PolicyDAO policyDAO;

  private final PolicyTagDAO policyTagDAO;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final ManagementEventService managementEventService;

  private final IdUtils idUtils;

  @Inject
  public TagService(ApplicationService applicationService,
                    ApplicationTagDAO applicationTagDAO,
                    TagDAO tagDAO,
                    OwnerDAO ownerDAO,
                    PolicyTagDAO policyTagDAO,
                    ApplicationDAO applicationDAO,
                    OrganizationDAO organizationDAO,
                    PolicyDAO policyDAO,
                    ManagementEventService managementEventService,
                    final IdUtils idUtils)
  {
    this.applicationService = applicationService;
    this.applicationTagDAO = applicationTagDAO;
    this.tagDAO = tagDAO;
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
    this.policyTagDAO = policyTagDAO;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.managementEventService = managementEventService;
    this.idUtils = idUtils;
  }

  public List<ApiApplicationCategoryDTO> getTagsUsedByApplications() {
    List<String> applicationIds = applicationService.getApplications().stream().map(Application::getId).collect(
        Collectors.toList());
    List<Tag> tags = tagDAO.getByApplicationIds(applicationIds);
    return tags.stream().map(TagService::toDTO).collect(Collectors.toList());
  }

  @Authorize(permission = Permission.READ)
  public ApplicableTagsDTO getApplicableTags(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId)
  {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    ApplicableTagsDTO tags = new ApplicableTagsDTO();
    tags.applicationCategoriesByOwner = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(internalOwnerId)) {
      TagsByOwnerDTO tagsByOwner = new TagsByOwnerDTO(owner,
          tagDAO.getByOrganizationId(owner.getId()).stream().map(TagService::toDTO).collect(Collectors.toList()));
      tags.applicationCategoriesByOwner.add(tagsByOwner);
    }

    return tags;
  }

  @Authorize(permission = Permission.READ)
  public List<ApiApplicationCategoryDTO> getTags(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String id) {
    String internalOwnerId = idUtils.getInternalOwnerId(OwnerType.ORGANIZATION, id);
    List<ApiApplicationCategoryDTO> applicationCategoryList =
        tagDAO.getByOrganizationId(internalOwnerId).stream().map(TagService::toDTO).collect(Collectors.toList());
    return applicationCategoryList;
  }

  @Authorize(permission = Permission.WRITE)
  public ApiApplicationCategoryDTO addTag(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId,
      ApiApplicationCategoryDTO dto)
  {
    if (dto.id != null) {
      throw new BadRequestException("ID must be null when creating an Application Category.");
    }

    validateOwnerId(organizationId, dto);

    Tag tag = fromDTO(dto, organizationId);
    tagDAO.insert(tag);

    managementEventService.postEvent(CREATED, tag);
    auditApplicationCategory(tag);

    dto = toDTO(tag);
    return dto;
  }

  @Authorize(permission = Permission.WRITE)
  public ApiApplicationCategoryDTO updateTag(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId,
      ApiApplicationCategoryDTO dto)
  {
    validateOwnerId(organizationId, dto);

    Tag tag = fromDTO(dto, organizationId);
    if (!organizationId.equals(tagDAO.getByIdNotNull(tag.getId()).getOrganizationId())) {
      throw new NotFoundException(
          "Cannot find an application category with id " + tag.getId() + " for organization id " + organizationId);
    }

    tagDAO.update(tag);

    managementEventService.postEvent(UPDATED, tag);
    auditApplicationCategory(tag);

    return toDTO(tag);
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteTag(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId, String tagId) {
    Tag tag = tagDAO.getByIdNotNull(tagId);
    if (!organizationId.equals(tag.getOrganizationId())) {
      throw new NotFoundException(
          "Cannot find an application category with id " + tagId + " for organization id " + organizationId);
    }
    tagDAO.delete(tag);

    managementEventService.postEvent(DELETED, tag);
    auditApplicationCategory(tag);
  }

  @Authorize(permission = Permission.READ)
  public List<Tag> getAppliedApplicationTags(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId)
  {
    return tagDAO.getByApplicationId(idUtils.getInternalOwnerId(OwnerType.APPLICATION, applicationPublicId));
  }

  @Authorize(permission = Permission.READ)
  public AppliedTagsDTO getAppliedTags(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId) {
    AppliedTagsDTO entities = new AppliedTagsDTO();
    entities.applicationTagsByOwner = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(organizationId)) {
      ApplicationTagsByOwnerDTO appTags =
          new ApplicationTagsByOwnerDTO(owner, applicationTagDAO.getByOrganizationId(owner.getId()));
      entities.applicationTagsByOwner.add(appTags);
    }

    return entities;
  }

  @Authorize(permission = Permission.WRITE)
  public List<ApplicationTag> updateApplicationTags(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) final String applicationPublicId,
      final List<Tag> tags)
  {
    String applicationId = idUtils.getInternalOwnerId(OwnerType.APPLICATION, applicationPublicId);

    List<ApplicationTag> applicationTags = new ArrayList<>();
    try (TransactionContext tx = applicationTagDAO.createTransactionContext()) {
      tx.begin();
      for (ApplicationTag applicationTag : applicationTagDAO.getByApplicationId(applicationId)) {
        applicationTagDAO.delete(tx, applicationTag);
      }

      for (Tag tag : tags) {
        ApplicationTag applicationTag = new ApplicationTag(applicationId, tag.getId());
        applicationTagDAO.insert(tx, applicationTag);
        applicationTags.add(applicationTag);
      }
      tx.commit();
      auditUpdateApplicationTags(tags);
    }
    return applicationTags;
  }

  @Authorize(permission = Permission.READ)
  public List<Tag> getPolicyTags(@AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
                                 @AuthzContext(AuthzContext.Key.ID) String ownerId,
                                 String policyId)
  {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    assertInHierarchy(internalOwnerId, policyDAO.getById(policyId));

    return tagDAO.getByPolicyId(policyId);
  }

  @Authorize(permission = Permission.WRITE)
  List<Tag> updatePolicyTags(@AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
                             @AuthzContext(AuthzContext.Key.ID) String ownerId,
                             String policyId,
                             final List<Tag> newTags)
  {
    String internalId = idUtils.getInternalOwnerId(ownerType, ownerId);
    Policy policy = policyDAO.getByIdNotNull(policyId);
    if (!internalId.equals(policy.getOwnerId())) {
      throw new NotFoundException("Cannot find a policy with id " + policyId + " for owner id " + ownerId);
    }
    if (!OwnerType.ORGANIZATION.equals(ownerType)) {
      throw new BadRequestException("Cannot configure application categories for policy owned by " + ownerType);
    }

    try (TransactionContext tx = policyTagDAO.createTransactionContext()) {
      tx.begin();
      auditUpdatePolicyTags(policy, newTags);

      final List<PolicyTag> existingPolicyTags = policyTagDAO.getByPolicyId(tx, policyId);
      for (PolicyTag existingPolicyTag : existingPolicyTags) {
        boolean tagFound = false;
        for (Tag newTag : newTags) {
          if (existingPolicyTag.getTagId().equals(newTag.getId())) {
            tagFound = true;
            newTags.remove(newTag);
            break;
          }
        }
        if (!tagFound) {
          policyTagDAO.delete(tx, existingPolicyTag);
        }
      }

      for (Tag newTag : newTags) {
        PolicyTag policyTag = new PolicyTag(policyId, newTag.getId());
        policyTagDAO.insert(tx, policyTag);
      }

      tx.commit();
    }

    return tagDAO.getByPolicyId(policyId);
  }

  private void auditUpdatePolicyTags(final Policy policy, final List<Tag> newTags) {
    AuditData.get().setPolicy(policy).setInheritanceScope(ApplicationCategoryAuditDTO.transcribe(newTags));
  }

  private void auditUpdateApplicationTags(final List<Tag> applicationTags) {
    AuditData.get().setApplicationCategories(ApplicationCategoryAuditDTO.transcribe(applicationTags));
  }

  @Authorize(permission = Permission.READ)
  public List<PolicyTag> getAppliedPolicyTags(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId) {
    List<PolicyTag> policyTags = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(organizationId)) {
      policyTags.addAll(policyTagDAO.getByOrganizationId(owner.getId()));
    }

    return policyTags;
  }

  @Authorize(permission = Permission.READ)
  public List<ApiApplicationCategoryDTO> getApplicableTagsByApplicationPublicId(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) final String applicationPublicId)
  {
    List<ApiApplicationCategoryDTO> result = new ArrayList<>();
    String organizationId = applicationDAO.getByPublicIdNotNull(applicationPublicId).getOrganizationId();
    while (organizationId != null) {
      List<Tag> tags = tagDAO.getByOrganizationId(organizationId);
      result.addAll(tags.stream().map(TagService::toDTO).collect(Collectors.toList()));
      organizationId = organizationDAO.getById(organizationId).getParentOrganizationId();
    }
    return result;
  }

  private void assertInHierarchy(String ownerId, Policy policy) {
    for (Owner candidate : ownerDAO.walkHierarchy(ownerId)) {
      if (policy.getOwnerId().equals(candidate.getId())) {
        return;
      }
    }
    throw new NotFoundException("Cannot find a policy with id " + policy.getId() + " for owner id " + ownerId);
  }

  private void auditApplicationCategory(Tag tag) {
    AuditData.get().setData("applicationCategoryId", tag.getId()).setData("applicationCategoryName", tag.getName())
        .setData("applicationCategoryDescription", tag.getDescription())
        .setEnum("applicationCategoryColor", tag.getColor());
  }

  private void validateOwnerId(String organizationId, ApiApplicationCategoryDTO dto) {
    if (dto.organizationId != null && !dto.organizationId.equals(organizationId)) {
      throw new BadRequestException("Organization ID mismatch.");
    }
  }

  public static ApiApplicationCategoryDTO toDTO(Tag tag) {
    ApiApplicationCategoryDTO dto = new ApiApplicationCategoryDTO();

    dto.id = tag.getId();
    dto.name = tag.getName();
    dto.description = tag.getDescription();
    dto.organizationId = tag.getOrganizationId();
    dto.color = tag.getColor().toValue();

    return dto;
  }

  public static Tag fromDTO(ApiApplicationCategoryDTO dto, String organizationId) {
    Tag tag = new Tag();

    tag.setId(dto.id);
    tag.setName(dto.name);
    tag.setDescription(dto.description);
    tag.setColor(Color.convertColorStringToEnum(dto.color));
    tag.setOrganizationId(organizationId);

    return tag;
  }
}
