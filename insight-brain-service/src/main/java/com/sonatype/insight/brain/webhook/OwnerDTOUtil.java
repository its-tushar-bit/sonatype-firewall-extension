/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.webhook.ManagementEvent.OwnerEvent;
import com.sonatype.insight.brain.webhook.dto.PolicyManagementPayload.OwnerDTO;
import com.sonatype.insight.brain.webhook.dto.PolicyManagementPayload.OwnerDTO.LabelDTO;
import com.sonatype.insight.brain.webhook.dto.PolicyManagementPayload.OwnerDTO.LicenseThreatGroupDTO;
import com.sonatype.insight.brain.webhook.dto.PolicyManagementPayload.OwnerDTO.PolicyDTO;
import com.sonatype.insight.brain.webhook.dto.PolicyManagementPayload.OwnerDTO.RoleDTO;
import com.sonatype.insight.brain.webhook.dto.PolicyManagementPayload.OwnerDTO.RoleDTO.MemberDTO;
import com.sonatype.insight.brain.webhook.dto.PolicyManagementPayload.OwnerDTO.ApplicationCategoryDTO;

/**
 * @since 1.25.0
 */
@Named
@Singleton
public class OwnerDTOUtil
{
  private final OwnerDAO ownerDAO;

  private final TagDAO tagDAO;

  private final LabelDAO labelDAO;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final PolicyDAO policyDAO;

  private final RoleDAO roleDAO;

  private final MembershipMappingDAO membershipMappingDAO;

  @Inject
  public OwnerDTOUtil(
      final OwnerDAO ownerDAO,
      final TagDAO tagDAO,
      final LabelDAO labelDAO,
      final LicenseThreatGroupDAO licenseThreatGroupDAO,
      final PolicyDAO policyDAO,
      final RoleDAO roleDAO,
      final MembershipMappingDAO membershipMappingDAO)
  {
    this.ownerDAO = ownerDAO;
    this.tagDAO = tagDAO;
    this.labelDAO = labelDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.policyDAO = policyDAO;
    this.roleDAO = roleDAO;
    this.membershipMappingDAO = membershipMappingDAO;
  }

  public OwnerDTO buildOwnerDTO(ManagementEvent managementEvent) {
    // Populate Owner
    Owner owner;
    if (managementEvent instanceof OwnerEvent) {
      // not just more efficient but in case of deletion, the owner cannot be loaded from DB
      owner = ((OwnerEvent) managementEvent).owner;
    }
    else {
      owner = ownerDAO.getById(managementEvent.ownerId);
    }
    OwnerDTO ownerDTO = new OwnerDTO();
    ownerDTO.id = owner.getId();
    ownerDTO.publicId = owner.getPublicId();
    ownerDTO.name = owner.getName();
    ownerDTO.type = owner.getType().name();
    ownerDTO.parentOwnerId = owner.getParentOwnerId();

    // Populate Tags
    // Only populate Organization tags as the tags are created and edited at the Organization level
    if (OwnerType.ORGANIZATION.equals(owner.getType())) {
      List<Tag> tags = tagDAO.getByOrganizationId(managementEvent.ownerId);
      ownerDTO.applicationCategories = new ArrayList<>();
      for (Tag tag : tags) {
        ApplicationCategoryDTO applicationCategoryDTO = new ApplicationCategoryDTO();
        applicationCategoryDTO.id = tag.getId();
        applicationCategoryDTO.name = tag.getName();
        applicationCategoryDTO.description = tag.getDescription();
        applicationCategoryDTO.color = tag.getColor().toValue();

        ownerDTO.applicationCategories.add(applicationCategoryDTO);
      }
    }

    // Populate Labels
    List<Label> labels = labelDAO.getByOwnerId(managementEvent.ownerId);
    ownerDTO.labels = new ArrayList<>();
    for (Label label : labels) {
      LabelDTO labelDTO = new LabelDTO();
      labelDTO.id = label.getId();
      labelDTO.name = label.getLabel();
      labelDTO.description = label.getDescription();
      labelDTO.color = label.getColor().toValue();

      ownerDTO.labels.add(labelDTO);
    }

    // Populate License Threat Groups
    List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO
        .getByOwnerId(managementEvent.ownerId);
    ownerDTO.licenseThreatGroups = new ArrayList<>();
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      LicenseThreatGroupDTO licenseThreatGroupDTO = new LicenseThreatGroupDTO();
      licenseThreatGroupDTO.id = licenseThreatGroup.getId();
      licenseThreatGroupDTO.name = licenseThreatGroup.getName();
      licenseThreatGroupDTO.threatLevel = licenseThreatGroup.getThreatLevel();

      ownerDTO.licenseThreatGroups.add(licenseThreatGroupDTO);
    }

    // Populate Policies
    List<Policy> policies = policyDAO.getByOwnerId(managementEvent.ownerId);
    ownerDTO.policies = new ArrayList<>();
    for (Policy policy : policies) {
      PolicyDTO policyDTO = new PolicyDTO();
      policyDTO.id = policy.getId();
      policyDTO.name = policy.getName();
      policyDTO.threatLevel = policy.getThreatLevel();

      ownerDTO.policies.add(policyDTO);
    }

    // Populate Roles
    List<MembershipMapping> membershipMappings = membershipMappingDAO
        .getByContextId(managementEvent.ownerId);
    Map<String, List<MemberDTO>> memberPayloadByRole = new LinkedHashMap<>();
    for (MembershipMapping membershipMapping : membershipMappings) {
      List<MemberDTO> memberDTOs = memberPayloadByRole.get(membershipMapping.getRoleId());
      if (memberDTOs == null) {
        memberDTOs = new ArrayList<>();
        memberPayloadByRole.put(membershipMapping.getRoleId(), memberDTOs);
      }
      MemberDTO memberDTO = new MemberDTO();
      memberDTO.name = membershipMapping.getMemberName();
      memberDTO.type = membershipMapping.getMemberType().name();

      memberDTOs.add(memberDTO);
    }

    ownerDTO.roles = new ArrayList<>();
    for (String roleId : memberPayloadByRole.keySet()) {
      Role role = roleDAO.getByIdNotNull(roleId);
      RoleDTO roleDTO = new RoleDTO();
      roleDTO.id = role.getId();
      roleDTO.name = role.getName();
      roleDTO.members = memberPayloadByRole.get(roleId);

      ownerDTO.roles.add(roleDTO);
    }

    return ownerDTO;
  }
}
