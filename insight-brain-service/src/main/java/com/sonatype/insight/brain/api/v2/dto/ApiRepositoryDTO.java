/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.clm.dto.model.repository.RepositoryDTO;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.model.repository.Repository;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

/**
 * @since 1.76
 */
public class ApiRepositoryDTO
{
  public String repositoryId;

  public String publicId;

  public String format;

  public String type;

  public boolean auditEnabled;

  public boolean quarantineEnabled;

  public boolean policyCompliantComponentSelectionEnabled;

  public boolean namespaceConfusionProtectionEnabled;

  public boolean monitoringEnabled;

  // Owned by the virtual_repository_config satellite table, not by the repository row.
  // Request-only in this PR: POST /api/v2/firewall/{id}/repositories accepts it and persists it
  // to the satellite. No response hydrates it — neither getConfiguredRepositories nor
  // addRepository — so it is excluded from every response and from the generated OpenAPI
  // response schema. The Firewall-Enterprise-scoped read endpoint (FIRE-660 Stories 3.2 / 3.4a,
  // FIRE-661+) will surface it.
  @JsonProperty(access = Access.WRITE_ONLY)
  public String upstreamUrl;

  public String proxyUrl;

  public static RepositoryDTO toRepositoryDTO(ApiRepositoryDTO dto) {
    RepositoryDTO repositoryDTO = new RepositoryDTO();
    repositoryDTO.name = dto.publicId;
    repositoryDTO.format = dto.format;
    repositoryDTO.type = toRepositoryType(dto.type);
    repositoryDTO.auditEnabled = dto.auditEnabled;
    repositoryDTO.quarantineEnabled = dto.quarantineEnabled;
    repositoryDTO.policyCompliantComponentSelectionEnabled = dto.policyCompliantComponentSelectionEnabled;
    repositoryDTO.namespaceConfusionProtectionEnabled = dto.namespaceConfusionProtectionEnabled;
    repositoryDTO.monitoringEnabled = dto.monitoringEnabled;
    return repositoryDTO;
  }

  public static Repository toRepository(ApiRepositoryDTO dto) {
    Repository repository = new Repository();
    repository.setId(dto.repositoryId);
    repository.setPublicId(dto.publicId);
    repository.setFormat(dto.format);
    repository.setRepositoryType(toRepositoryType(dto.type));
    repository.setAuditEnabled(dto.auditEnabled);
    repository.setQuarantineEnabled(dto.quarantineEnabled);
    repository.setPolicyCompliantComponentSelectionEnabled(dto.policyCompliantComponentSelectionEnabled);
    repository.setNamespaceConfusionProtectionEnabled(dto.namespaceConfusionProtectionEnabled);
    repository.setMonitoringEnabled(dto.monitoringEnabled);
    return repository;
  }

  public static ApiRepositoryDTO fromRepository(Repository repository) {
    ApiRepositoryDTO apiRepositoryDTO = new ApiRepositoryDTO();
    apiRepositoryDTO.repositoryId = repository.getId();
    apiRepositoryDTO.publicId = repository.getPublicId();
    apiRepositoryDTO.format = repository.getFormat();
    apiRepositoryDTO.type = repository.getRepositoryType().name();
    apiRepositoryDTO.auditEnabled = repository.isAuditEnabled();
    apiRepositoryDTO.quarantineEnabled = repository.isQuarantineEnabled();
    apiRepositoryDTO.policyCompliantComponentSelectionEnabled = repository.isPolicyCompliantComponentSelectionEnabled();
    apiRepositoryDTO.namespaceConfusionProtectionEnabled = repository.isNamespaceConfusionProtectionEnabled();
    apiRepositoryDTO.monitoringEnabled = repository.isMonitoringEnabled();
    return apiRepositoryDTO;
  }

  public static RepositoryType toRepositoryType(String repositoryType) {
    if (repositoryType == null) {
      return null;
    }
    try {
      return RepositoryType.valueOf(repositoryType);
    }
    catch (IllegalArgumentException e) {
      // no matching type
      return null;
    }
  }
}
