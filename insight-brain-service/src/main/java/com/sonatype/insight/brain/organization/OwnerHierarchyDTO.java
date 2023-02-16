/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @since 1.20.0
 */
public class OwnerHierarchyDTO
{
  public Map<String, OwnerHierarchyEntityDTO> ownersMap;

  public String topParentOrganizationId;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  @JsonSubTypes({
      @Type(value = OwnerHierarchyApplicationDTO.class, name = "application"),
      @Type(value = OwnerHierarchyOrganizationDTO.class, name = "organization")
  })
  public static class OwnerHierarchyEntityDTO
  {
    public String id;

    public String name;
  }

  public static class OwnerHierarchyOrganizationDTO
      extends OwnerHierarchyEntityDTO
  {
    public boolean synthetic;

    public String parentOrganizationId;

    public List<String> applicationIds;

    public int subOrgs;

    public int totalApps;

    public List<String> organizationIds;

    public static Function<Organization, OwnerHierarchyOrganizationDTO> transformToOrganizationDTO = organization -> {
      String organizationId = organization.getId();
      OwnerHierarchyOrganizationDTO ownerHierarchyOrganizationDTO = new OwnerHierarchyOrganizationDTO();
      ownerHierarchyOrganizationDTO.id = organizationId;
      ownerHierarchyOrganizationDTO.name = organization.getName();
      ownerHierarchyOrganizationDTO.synthetic = false;
      if (!organizationId.equals(Organization.ROOT_ORGANIZATION_ID)) {
        ownerHierarchyOrganizationDTO.applicationIds = new ArrayList<>();
      }
      ownerHierarchyOrganizationDTO.organizationIds = new ArrayList<>();
      ownerHierarchyOrganizationDTO.parentOrganizationId = organization.getParentOrganizationId();
      return ownerHierarchyOrganizationDTO;
    };

    public static Function<Organization, OwnerHierarchyOrganizationDTO> transformToSyntheticOrganizationDTO =
        organization -> {
          OwnerHierarchyOrganizationDTO ownerHierarchyOrganizationDTO = transformToOrganizationDTO.apply(organization);
          ownerHierarchyOrganizationDTO.synthetic = true;
          return ownerHierarchyOrganizationDTO;
        };

    @Override
    public String toString() {
      return "<Organization [name=" + name + ", id=" + id + ", parentId=" + parentOrganizationId + "]>";
    }
  }

  public static class OwnerHierarchyApplicationDTO
      extends OwnerHierarchyEntityDTO
  {
    public String publicId;

    public String organizationId;

    public String provider;

    public String repositoryUrl;

    public static Function<Application, OwnerHierarchyApplicationDTO> transformToApplicationDTO = application -> {
      OwnerHierarchyApplicationDTO ownerHierarchyApplicationDTO = new OwnerHierarchyApplicationDTO();
      ownerHierarchyApplicationDTO.id = application.getId();
      ownerHierarchyApplicationDTO.publicId = application.getPublicId();
      ownerHierarchyApplicationDTO.name = application.getName();
      ownerHierarchyApplicationDTO.organizationId = application.getOrganizationId();
      return ownerHierarchyApplicationDTO;
    };

    @Override
    public String toString() {
      return "<Application [name=" + name + ", id=" + id + ", parentId=" + organizationId + "]>";
    }
  }
}
