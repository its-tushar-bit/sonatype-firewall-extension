/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.callflowanalysis.ApiCallFlowAnalysisConfigDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.configuration.CallFlowAnalysisConfigDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.CallFlowAnalysisConfig;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
public class ApiCallFlowAnalysisConfigService
{
  private final CallFlowAnalysisConfigDAO callFlowAnalysisConfigDAO;

  private final IdUtils idUtils;

  @Inject
  public ApiCallFlowAnalysisConfigService(
      final CallFlowAnalysisConfigDAO callFlowAnalysisConfigDAO,
      final IdUtils idUtils)
  {
    this.callFlowAnalysisConfigDAO = callFlowAnalysisConfigDAO;
    this.idUtils = idUtils;
  }

  @Authorize(permission = Permission.WRITE)
  public ApiCallFlowAnalysisConfigDTO upsertCallFlowAnalysisConfig(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      final ApiCallFlowAnalysisConfigDTO apiCallFlowAnalysisConfigDTO)
  {
    validateCallFlowAnalysisConfigDTO(apiCallFlowAnalysisConfigDTO, ownerId);
    CallFlowAnalysisConfig existingConfigByOwner = callFlowAnalysisConfigDAO.getByOwnerId(ownerId);
    CallFlowAnalysisConfig modelToPersist = buildCallFlowConfigModel(apiCallFlowAnalysisConfigDTO);
    if (existingConfigByOwner == null) {
      callFlowAnalysisConfigDAO.insert(modelToPersist);
    }
    else {
      modelToPersist.setId(existingConfigByOwner.getId());
      callFlowAnalysisConfigDAO.update(modelToPersist);
    }
    auditCallFlowAnalysisConfigUpdates(apiCallFlowAnalysisConfigDTO);
    return buildApiCallFlowConfigDTO(modelToPersist);
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ApiCallFlowAnalysisConfigDTO getCallFlowAnalysisConfig(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId) throws NotFoundException
  {
    return getCallFlowAnalysisConfig(ownerId);
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ApiCallFlowAnalysisConfigDTO getCallFlowAnalysisConfigByPublicId(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.ID) final String ownerId) throws NotFoundException
  {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    return getCallFlowAnalysisConfig(internalOwnerId);
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteCallFlowAnalysisConfig(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId)
  {
    CallFlowAnalysisConfig existingConfigByOwner = callFlowAnalysisConfigDAO.getByOwnerId(ownerId);
    if (existingConfigByOwner == null) {
      throw new NotFoundException("Call Flow Analysis Config not found for ownerId " + ownerId);
    }
    callFlowAnalysisConfigDAO.delete(existingConfigByOwner);
  }

  private ApiCallFlowAnalysisConfigDTO getCallFlowAnalysisConfig(String ownerId) throws NotFoundException {
    CallFlowAnalysisConfig config = callFlowAnalysisConfigDAO.getByOwnerIdWithHierarchy(ownerId);
    if (config == null) {
      throw new NotFoundException("Call Flow Analysis Config not found for ownerId " + ownerId);
    }
    return buildApiCallFlowConfigDTO(config);
  }

  private void auditCallFlowAnalysisConfigUpdates(final ApiCallFlowAnalysisConfigDTO callFlowAnalysisConfig) {
    AuditData.get().setData("namespaces", callFlowAnalysisConfig.namespaces);
  }

  private void validateCallFlowAnalysisConfigDTO(ApiCallFlowAnalysisConfigDTO dto, String ownerId) {
    if (dto.ownerId == null) {
      throw new BadRequestException("ownerId cannot be null");
    }
    if (!dto.ownerId.equals(ownerId)) {
      throw new BadRequestException("ownerId does not match");
    }
  }

  private CallFlowAnalysisConfig buildCallFlowConfigModel(ApiCallFlowAnalysisConfigDTO dto) {
    return new CallFlowAnalysisConfig(
        dto.enabled, dto.namespaces, dto.algorithm, dto.threadCount, dto.ownerId);
  }

  private ApiCallFlowAnalysisConfigDTO buildApiCallFlowConfigDTO(CallFlowAnalysisConfig callFlowAnalysisConfig) {
    ApiCallFlowAnalysisConfigDTO apiCallFlowAnalysisConfigDTO = new ApiCallFlowAnalysisConfigDTO();
    apiCallFlowAnalysisConfigDTO.id = callFlowAnalysisConfig.getId();
    apiCallFlowAnalysisConfigDTO.algorithm = callFlowAnalysisConfig.getAlgorithm();
    apiCallFlowAnalysisConfigDTO.enabled = callFlowAnalysisConfig.isEnabled();
    apiCallFlowAnalysisConfigDTO.namespaces = callFlowAnalysisConfig.getNamespaces();
    apiCallFlowAnalysisConfigDTO.threadCount = callFlowAnalysisConfig.getThreadCount();
    apiCallFlowAnalysisConfigDTO.ownerId = callFlowAnalysisConfig.getOwnerId();
    return apiCallFlowAnalysisConfigDTO;
  }
}
