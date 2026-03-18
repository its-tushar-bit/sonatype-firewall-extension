/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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
    @Type(value = OwnerHierarchyOrganizationDTO.class, name = "organization"),
    @Type(value = OwnerHierarchyRepositoryContainerDTO.class, name = "repository_container"),
    @Type(value = OwnerHierarchyRepositoryManagerDTO.class, name = "repository_manager"),
    @Type(value = OwnerHierarchyRepositoryDTO.class, name = "repository")
  })

  public abstract static class OwnerHierarchyEntityDTO
  {
    public String id;

    public String name;

    public abstract String getParentId();

    /**
     * The getParentId method is implemented for each owner type, usually returning the value from another field that
     * denotes the parent ID. For ex, the parent org ID for apps or the parent repository manager ID for repositories.
     *
     * @deprecated This method is declared here only to allow json deserialization of the parentId field.
     */
    @SuppressWarnings("unused")
    @Deprecated
    private void setParentId(@SuppressWarnings("unused") String unused) {
      // No-op
    }

    abstract List<String> getChildIds(OwnerType... types);

    public abstract void addChild(OwnerHierarchyEntityDTO child);

    abstract boolean hasChild(String childId);
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

    @JsonInclude(Include.NON_NULL)
    public String repositoryContainerId;

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

    public static Function<RepositoryManager, OwnerHierarchyRepositoryManagerDTO> transformToSyntheticRepositoryManagerDTO =
        repositoryManager -> {
          OwnerHierarchyRepositoryManagerDTO ownerHierarchyRepositoryManagerDTO =
              new OwnerHierarchyRepositoryManagerDTO();
          ownerHierarchyRepositoryManagerDTO.id = repositoryManager.getId();
          ownerHierarchyRepositoryManagerDTO.name = repositoryManager.getName();
          ownerHierarchyRepositoryManagerDTO.synthetic = true;
          return ownerHierarchyRepositoryManagerDTO;
        };

    @Override
    public String getParentId() {
      return parentOrganizationId;
    }

    @Override
    List<String> getChildIds(OwnerType... types) {
      if (applicationIds != null) {
        return Stream.concat(applicationIds.stream(), organizationIds.stream()).collect(Collectors.toList());
      }
      return organizationIds;
    }

    @Override
    public void addChild(final OwnerHierarchyEntityDTO child) {
      if (child instanceof OwnerHierarchyApplicationDTO) {
        applicationIds.add(((OwnerHierarchyApplicationDTO) child).publicId);
      }
      else if (child instanceof OwnerHierarchyOrganizationDTO) {
        organizationIds.add(child.id);
      }
      else {
        throw new IllegalArgumentException("Cannot add child of this type to organization");
      }
    }

    @Override
    boolean hasChild(final String childId) {
      return organizationIds.contains(childId) || (applicationIds != null && applicationIds.contains(childId));
    }

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
    public String getParentId() {
      return organizationId;
    }

    @Override
    List<String> getChildIds(OwnerType... types) {
      return Collections.emptyList();
    }

    @Override
    public void addChild(final OwnerHierarchyEntityDTO child) {
      throw new UnsupportedOperationException("Cannot add child to application");
    }

    @Override
    boolean hasChild(final String childId) {
      return false;
    }

    @Override
    public String toString() {
      return "<Application [name=" + name + ", id=" + id + ", parentId=" + organizationId + "]>";
    }
  }

  public static class OwnerHierarchyRepositoryContainerDTO
      extends OwnerHierarchyEntityDTO
  {
    public List<String> repositoryManagerIds = new ArrayList<>();

    OwnerHierarchyRepositoryContainerDTO() {
      id = RepositoryContainer.REPOSITORY_CONTAINER_ID;
      name = RepositoryContainer.SINGLETON.getName();
    }

    @Override
    public String getParentId() {
      return Organization.ROOT_ORGANIZATION_ID;
    }

    @Override
    List<String> getChildIds(OwnerType... ignored) {
      return repositoryManagerIds;
    }

    @Override
    public void addChild(final OwnerHierarchyEntityDTO child) {
      repositoryManagerIds.add(child.id);
    }

    @Override
    boolean hasChild(final String childId) {
      return this.getChildIds().contains(childId);
    }
  }

  public static class OwnerHierarchyRepositoryManagerDTO
      extends OwnerHierarchyEntityDTO
  {
    public boolean synthetic;

    public String instanceId;

    public List<String> repositoryIds = new ArrayList<>();

    @Override
    public String getParentId() {
      return RepositoryContainer.REPOSITORY_CONTAINER_ID;
    }

    @Override
    List<String> getChildIds(OwnerType... ignored) {
      return repositoryIds;
    }

    @Override
    public void addChild(final OwnerHierarchyEntityDTO child) {
      repositoryIds.add(child.id);
    }

    @Override
    boolean hasChild(final String childId) {
      return this.getChildIds().contains(childId);
    }

    public static Function<RepositoryManager, OwnerHierarchyRepositoryManagerDTO> transformToRepositoryManagerDTO =
        repositoryManager -> {
          OwnerHierarchyRepositoryManagerDTO ownerHierarchyRepositoryManagerDTO =
              new OwnerHierarchyRepositoryManagerDTO();
          ownerHierarchyRepositoryManagerDTO.id = repositoryManager.getId();
          ownerHierarchyRepositoryManagerDTO.name = repositoryManager.getName();
          ownerHierarchyRepositoryManagerDTO.instanceId = repositoryManager.getInstanceId();
          return ownerHierarchyRepositoryManagerDTO;
        };
  }

  public static class OwnerHierarchyRepositoryDTO
      extends OwnerHierarchyEntityDTO
  {
    public String repositoryManagerId;

    public String repositoryType;

    public String format;

    public static Function<Repository, OwnerHierarchyRepositoryDTO> transformToRepositoryDTO = repository -> {
      OwnerHierarchyRepositoryDTO ownerHierarchyRepositoryDTO = new OwnerHierarchyRepositoryDTO();
      ownerHierarchyRepositoryDTO.id = repository.getId();
      ownerHierarchyRepositoryDTO.name = repository.getName();
      ownerHierarchyRepositoryDTO.repositoryManagerId = repository.getRepositoryManagerId();
      ownerHierarchyRepositoryDTO.repositoryType = repository.getRepositoryType().name();
      ownerHierarchyRepositoryDTO.format = repository.getFormat();
      return ownerHierarchyRepositoryDTO;
    };

    @Override
    public String getParentId() {
      return repositoryManagerId;
    }

    @Override
    List<String> getChildIds(OwnerType... ignored) {
      return Collections.emptyList();
    }

    @Override
    public void addChild(final OwnerHierarchyEntityDTO child) {
      throw new UnsupportedOperationException("Cannot add child to repository");
    }

    @Override
    boolean hasChild(final String childId) {
      return false;
    }

    @Override
    public String toString() {
      return "<Repository [id=" + id + ", repositoryManagerId=" + repositoryManagerId + "]>";
    }
  }
}
