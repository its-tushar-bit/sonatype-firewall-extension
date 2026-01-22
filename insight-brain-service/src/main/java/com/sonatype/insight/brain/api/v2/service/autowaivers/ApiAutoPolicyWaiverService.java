/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.autowaivers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverAdapter;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverStatusDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverExclusionDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.AutoPolicyWaiverExclusionMatcherWrapper;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.autowaivers.AutoPolicyWaiverTelemetryCollector;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.v2.service.autowaivers.AutoPolicyWaiverUtil.anyEqualByOwnerAndScope;

public class ApiAutoPolicyWaiverService
{
  private static final Logger log = LoggerFactory.getLogger(ApiAutoPolicyWaiverService.class);

  private final AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final OwnerDAO ownerDAO;

  private final CurrentUser currentUser;

  private final AutoPolicyWaiverTelemetryCollector autoPolicyWaiverTelemetryCollector;

  private final AutoPolicyWaiverExclusionDAO autoPolicyWaiverExclusionDAO;

  private TelemetrySender telemetrySender;

  @Inject
  public ApiAutoPolicyWaiverService(
      AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      ApplicationDAO applicationDAO,
      OrganizationDAO organizationDAO,
      PolicyViolationDAO policyViolationDAO,
      OwnerDAO ownerDAO,
      CurrentUser currentUser,
      AutoPolicyWaiverTelemetryCollector autoPolicyWaiverTelemetryCollector,
      AutoPolicyWaiverExclusionDAO autoPolicyWaiverExclusionDAO,
      TelemetrySender telemetrySender)
  {
    this.autoPolicyWaiverDAO = autoPolicyWaiverDAO;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.ownerDAO = ownerDAO;
    this.currentUser = currentUser;
    this.autoPolicyWaiverTelemetryCollector = autoPolicyWaiverTelemetryCollector;
    this.autoPolicyWaiverExclusionDAO = autoPolicyWaiverExclusionDAO;
    this.telemetrySender = telemetrySender;
  }

  @Authorize(permission = Permission.READ)
  public ApiAutoPolicyWaiverDTO getAutoPolicyWaiver(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      String autoPolicyWaiverId)
  {
    AutoPolicyWaiverUtil.validateAutoWaiversFeatureEnabled();
    checkOwnerType(ownerType, ownerId);
    AutoPolicyWaiver autoPolicyWaiver =
        autoPolicyWaiverDAO.getByIdAndOwnerIdNotNull(autoPolicyWaiverId, ownerId);
    if (!ownerId.equals(autoPolicyWaiver.getOwnerId())) {
      throw new NotFoundException(
          "Cannot find an auto policy waiver with ID " + autoPolicyWaiverId + " for " + ownerType
              + " with ID " + ownerId);
    }
    Owner owner = ownerDAO.getById(ownerId);

    return ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver, owner);
  }

  @Authorize(permission = Permission.READ)
  public List<ApiAutoPolicyWaiverDTO> getAutoPolicyWaivers(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId)
  {
    AutoPolicyWaiverUtil.validateAutoWaiversFeatureEnabled();
    checkOwnerType(ownerType, ownerId);
    Owner owner = ownerDAO.getById(ownerId);
    List<ApiAutoPolicyWaiverDTO> apiAutoPolicyWaiverDTOs = new ArrayList<>();

    List<AutoPolicyWaiver> autoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(ownerId);
    autoPolicyWaivers.forEach(
        autoPolicyWaiver -> apiAutoPolicyWaiverDTOs.add(
            ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver, owner)));

    return apiAutoPolicyWaiverDTOs;
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  public List<ApiAutoPolicyWaiverDTO> addAutoPolicyWaivers(
      final @AuthzContext(Key.TYPE) OwnerType ownerType,
      final @AuthzContext(Key.INTERNAL_ID) String ownerId,
      final List<ApiAutoPolicyWaiverDTO> apiAutoPolicyWaivers)
  {
    AutoPolicyWaiverUtil.validateAutoWaiversFeatureEnabled();
    checkOwnerType(ownerType, ownerId);

    validateApiAutoPolicyWaivers(apiAutoPolicyWaivers);

    // validate that we weren't given duplicates by owner and scope
    if (AutoPolicyWaiverUtil.anyEqualByScope(apiAutoPolicyWaivers)) {
      throw new BadRequestException("Only one auto policy waiver is allowed for a given owner and scope "
          + "(not reachable/no path forward combination)");
    }

    // validate that we weren't given duplicates by owner and scope for existing waivers
    if (anyEqualByOwnerAndScope(ownerId, apiAutoPolicyWaivers, autoPolicyWaiverDAO.getByOwnerId(ownerId))) {
      throw new BadRequestException("Only one auto policy waiver is allowed for a given owner and scope "
          + "(not reachable/no path forward combination)");
    }

    Owner owner = ownerDAO.getById(ownerId);
    List<ApiAutoPolicyWaiverDTO> storedApiAutoPolicyWaivers = new ArrayList<>();

    for (ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO : apiAutoPolicyWaivers) {
      AutoPolicyWaiver autoPolicyWaiver = getAutoPolicyWaiver(ownerId, apiAutoPolicyWaiverDTO);
      autoPolicyWaiverDAO.insert(autoPolicyWaiver);
      auditAutoPolicyWaiver(autoPolicyWaiver);

      log.debug("Auto policy waiver created for {} with ID {}", ownerType, autoPolicyWaiver.getId());

      autoPolicyWaiverTelemetryCollector.addTelemetryForCreateAutoWaiver(autoPolicyWaiver, owner);

      storedApiAutoPolicyWaivers.add(ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver));
    }

    sendTelemetry();

    return storedApiAutoPolicyWaivers;
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  public ApiAutoPolicyWaiverDTO addAutoPolicyWaiver(
      final @AuthzContext(Key.TYPE) OwnerType ownerType,
      final @AuthzContext(Key.INTERNAL_ID) String ownerId,
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO)
  {
    List<ApiAutoPolicyWaiverDTO> storedAutoPolicyWaivers =
        addAutoPolicyWaivers(ownerType, ownerId, Collections.singletonList(apiAutoPolicyWaiverDTO));

    // when we get here no exceptions were thrown, so we can safely return the first element of the list
    return storedAutoPolicyWaivers.get(0);
  }

  @NotNull
  private AutoPolicyWaiver getAutoPolicyWaiver(String ownerId, ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO) {
    AutoPolicyWaiver autoPolicyWaiver = new AutoPolicyWaiver();
    autoPolicyWaiver.setOwnerId(ownerId);
    autoPolicyWaiver.setThreatLevel(apiAutoPolicyWaiverDTO.threatLevel);
    if (apiAutoPolicyWaiverDTO.reachability != null) {
      autoPolicyWaiver.setReachability(apiAutoPolicyWaiverDTO.reachability);
    }
    if (apiAutoPolicyWaiverDTO.pathForward != null) {
      autoPolicyWaiver.setPathForward(apiAutoPolicyWaiverDTO.pathForward);
    }
    autoPolicyWaiver.setScopesOperatorAny(apiAutoPolicyWaiverDTO.scopesOperatorAny);
    autoPolicyWaiver.setCreatorId(currentUser.getUserPrincipal().getUsername());
    autoPolicyWaiver.setCreatorName(currentUser.getUserPrincipal().getDisplayName());
    autoPolicyWaiver.setCreateTime(new Date());
    return autoPolicyWaiver;
  }

  @Authorize(permission = Permission.WRITE)
  public ApiAutoPolicyWaiverDTO updateAutoPolicyWaiver(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      String autoPolicyWaiverId,
      ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO)
  {
    AutoPolicyWaiverUtil.validateAutoWaiversFeatureEnabled();
    if (apiAutoPolicyWaiverDTO.autoPolicyWaiverId == null ||
        !autoPolicyWaiverId.equals(apiAutoPolicyWaiverDTO.autoPolicyWaiverId)) {
      throw new BadRequestException("Auto policy waiver ID in request path does not match request" +
          " body");
    }
    checkOwnerType(ownerType, ownerId);
    validateRequestDto(apiAutoPolicyWaiverDTO);

    AutoPolicyWaiver autoPolicyWaiver =
        autoPolicyWaiverDAO.getByIdAndOwnerIdNotNull(apiAutoPolicyWaiverDTO.autoPolicyWaiverId, ownerId);

    validateAutoWaiverUpdateDto(apiAutoPolicyWaiverDTO, autoPolicyWaiver);

    autoPolicyWaiver.setThreatLevel(apiAutoPolicyWaiverDTO.threatLevel);

    autoPolicyWaiver.setReachability(apiAutoPolicyWaiverDTO.reachability);
    autoPolicyWaiver.setPathForward(apiAutoPolicyWaiverDTO.pathForward);
    autoPolicyWaiver.setScopesOperatorAny(apiAutoPolicyWaiverDTO.scopesOperatorAny);
    autoPolicyWaiverDAO.update(autoPolicyWaiver);
    auditAutoPolicyWaiver(autoPolicyWaiver);

    log.debug("Auto policy waiver updated for {} with ID {}", ownerType, autoPolicyWaiver.getId());

    Owner owner = ownerDAO.getById(ownerId);
    autoPolicyWaiverTelemetryCollector.addTelemetryForUpdateAutoWaiver(autoPolicyWaiver, owner);
    sendTelemetry();

    return ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  public void deleteAutoPolicyWaiver(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      String autoPolicyWaiverId)
  {
    AutoPolicyWaiverUtil.validateAutoWaiversFeatureEnabled();
    checkOwnerType(ownerType, ownerId);
    AutoPolicyWaiver autoPolicyWaiver = autoPolicyWaiverDAO.getByIdNotNull(autoPolicyWaiverId);
    if (!ownerId.equals(autoPolicyWaiver.getOwnerId())) {
      throw new NotFoundException(
          "Cannot find an auto policy waiver with ID " + autoPolicyWaiverId + " for " + ownerType
              + " with ID " + ownerId);
    }
    auditAutoPolicyWaiver(autoPolicyWaiver);
    autoPolicyWaiverDAO.delete(autoPolicyWaiver);
    log.debug("Auto policy waiver deleted for {} with ID {}", ownerType, autoPolicyWaiver.getId());

    Owner owner = ownerDAO.getById(ownerId);
    autoPolicyWaiverTelemetryCollector.addTelemetryForDeleteAutoWaiver(autoPolicyWaiver, owner);
    sendTelemetry();
  }

  @Authorize(permission = Permission.READ)
  public ApiAutoPolicyWaiverStatusDTO getAutoPolicyWaiverStatus(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId)
  {
    AutoPolicyWaiverUtil.validateAutoWaiversFeatureEnabled();
    checkOwnerType(ownerType, ownerId);
    List<String> ownerIds = ownerDAO.getOwnerIds(ownerId);
    List<AutoPolicyWaiver> autoPolicyWaivers = new ArrayList<>();
    for (String id : ownerIds) {
      autoPolicyWaivers.addAll(autoPolicyWaiverDAO.getByOwnerId(id));
      if (!autoPolicyWaivers.isEmpty()) {
        break;
      }
    }

    ApiAutoPolicyWaiverStatusDTO dto = new ApiAutoPolicyWaiverStatusDTO();
    if (autoPolicyWaivers.isEmpty()) {
      dto.isAutoWaiverEnabled = false;
      return dto;
    }

    AutoPolicyWaiver applicableWaiver = autoPolicyWaivers.get(0);
    dto.isAutoWaiverEnabled = true;
    dto.autoPolicyWaiverId = applicableWaiver.getId();
    dto.autoPolicyWaiverOwnerId = applicableWaiver.getOwnerId();
    dto.isInherited = !applicableWaiver.getOwnerId().equals(ownerId);
    if (dto.isInherited || ownerType == OwnerType.ORGANIZATION) {
      Organization owner = organizationDAO.getById(applicableWaiver.getOwnerId());
      dto.autoPolicyWaiverOwnerName = owner.getName();
      dto.autoPolicyWaiverOwnerType = OwnerType.ORGANIZATION.toString();
    }
    else {
      Application owner = applicationDAO.getById(applicableWaiver.getOwnerId());
      dto.autoPolicyWaiverOwnerName = owner.getName();
      dto.autoPolicyWaiverOwnerType = OwnerType.APPLICATION.toString();
    }
    return dto;
  }

  public ApiAutoPolicyWaiverDTO getApplicableAutoPolicyWaiver(final String violationId) {
    AutoPolicyWaiverUtil.validateAutoWaiversFeatureEnabled();
    PolicyViolation policyViolation = policyViolationDAO.getById(violationId);
    if (policyViolation == null) {
      throw new NotFoundException("Could not find policy violation with ID " + violationId + ".");
    }

    checkReadPermission(policyViolation.getApplicationId());

    if (policyViolation.getAutoPolicyWaiverId() == null) {
      return null;
    }

    AutoPolicyWaiver autoPolicyWaiver = getAutoPolicyWaiverByAppOwnerHierarchy(
        policyViolation.getAutoPolicyWaiverId(), policyViolation.getApplicationId());

    if (autoPolicyWaiver != null) {
      String autoPolicyWaiverOwnerId = autoPolicyWaiver.getOwnerId();
      policyViolationDAO.loadConstraintFacts(Collections.singletonList(policyViolation));
      List<AutoPolicyWaiverExclusion> autoPolicyWaiverExclusions =
          autoPolicyWaiverExclusionDAO.getByOwnerIdAndAutoPolicyWaiverId(
              autoPolicyWaiverOwnerId,
              autoPolicyWaiver.getId()
          );

      boolean allExclusionsInvalid = true;
      for (AutoPolicyWaiverExclusion exclusion : autoPolicyWaiverExclusions) {
        boolean matches = new AutoPolicyWaiverExclusionMatcherWrapper(exclusion)
            .matchesViolation(policyViolation);

        if (matches || (exclusion.getPolicyViolationId() != null &&
            exclusion.getPolicyViolationId().equals(violationId))) {
          allExclusionsInvalid = false;
          break;
        }
      }

      if (allExclusionsInvalid) {
        Owner owner = ownerDAO.getById(autoPolicyWaiverOwnerId);
        return ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver, owner);
      }
    }
    return null;
  }

  // visible for testing
  @Authorize(permission = Permission.READ)
  public AutoPolicyWaiver getApplicableAutoPolicyWaiverWithPermissionCheck(
      String autoPolicyWaiverId,
      @AuthzContext(Key.OWNER) Owner owner)
  {
    return autoPolicyWaiverDAO.getByIdAndOwnerIdNullable(autoPolicyWaiverId, owner.getId());
  }

  @Authorize(permission = Permission.READ)
  public List<ApiAutoPolicyWaiverStatusDTO> getApplicableAutoWaivers(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId)
  {
    AutoPolicyWaiverUtil.validateAutoWaiversFeatureEnabled();
    checkOwnerType(ownerType, ownerId);
    final List<ApiAutoPolicyWaiverStatusDTO> autoPolicyWaiverStatuses = new ArrayList<>();
    final List<String> ownerIds = ownerDAO.getOwnerIds(ownerId);
    final List<AutoPolicyWaiver> autoPolicyWaivers = new ArrayList<>();
    // Adds auto waivers in order of lowest to highest owner (application level -> organization level)
    ownerIds.forEach(id -> autoPolicyWaivers.addAll(autoPolicyWaiverDAO.getByOwnerId(id)));

    final List<AutoPolicyWaiver> applicableAutoWaivers =
        AutoPolicyWaiverUtil.getApplicableAutoPolicyWaivers(autoPolicyWaivers);

    applicableAutoWaivers.forEach(autoPolicyWaiver ->
        autoPolicyWaiverStatuses.add(buildApiAutoPolicyWaiverStatusDTO(autoPolicyWaiver, ownerType, ownerId)));
    return autoPolicyWaiverStatuses;
  }

  private ApiAutoPolicyWaiverStatusDTO buildApiAutoPolicyWaiverStatusDTO(
      final AutoPolicyWaiver autoPolicyWaiver,
      final OwnerType ownerType,
      final String ownerId)
  {
    ApiAutoPolicyWaiverStatusDTO dto = new ApiAutoPolicyWaiverStatusDTO();

    dto.isAutoWaiverEnabled = true;
    dto.autoPolicyWaiverId = autoPolicyWaiver.getId();
    dto.autoPolicyWaiverOwnerId = autoPolicyWaiver.getOwnerId();
    dto.createTime = autoPolicyWaiver.getCreateTime();
    dto.threatLevel = autoPolicyWaiver.getThreatLevel();
    dto.hasNotReachable = autoPolicyWaiver.hasReachability();
    dto.hasNoPathForward = autoPolicyWaiver.hasPathForward();
    dto.scopesOperatorAny = autoPolicyWaiver.getScopesOperatorAny();
    dto.isInherited = !autoPolicyWaiver.getOwnerId().equals(ownerId);
    if (Boolean.TRUE.equals(dto.isInherited) || ownerType == OwnerType.ORGANIZATION) {
      Organization owner = organizationDAO.getById(autoPolicyWaiver.getOwnerId());
      dto.autoPolicyWaiverOwnerName = owner.getName();
      dto.autoPolicyWaiverOwnerType = OwnerType.ORGANIZATION.toString();
    }
    else {
      Application owner = applicationDAO.getById(autoPolicyWaiver.getOwnerId());
      dto.autoPolicyWaiverOwnerName = owner.getName();
      dto.autoPolicyWaiverOwnerType = OwnerType.APPLICATION.toString();
    }
    return dto;
  }

  private AutoPolicyWaiver getAutoPolicyWaiverByAppOwnerHierarchy(String autoPolicyWaiverId, String applicationId) {
    List<String> ownerIds = ownerDAO.getOwnerIds(applicationId);
    for (String ownerId : ownerIds) {
      Owner owner = ownerDAO.getById(ownerId);
      AutoPolicyWaiver applicableAutoPolicyWaiver =
          getApplicableAutoPolicyWaiverWithPermissionCheck(autoPolicyWaiverId, owner);
      if (applicableAutoPolicyWaiver != null) {
        return applicableAutoPolicyWaiver;
      }
    }
    return null;
  }

  private void checkOwnerType(OwnerType ownerType, String ownerId) throws IllegalStateException {
    switch (ownerType) {
      case APPLICATION:
        AuditData.get().setData("applicationId", ownerId).setApplication(applicationDAO.getById(ownerId));
        break;
      case ORGANIZATION:
        AuditData.get().setData("organizationId", ownerId).setOrganization(organizationDAO.getById(ownerId));
        break;
      default:
        throw new IllegalStateException("Unknown owner type: " + ownerType);
    }
  }

  private void validateApiAutoPolicyWaivers(final List<ApiAutoPolicyWaiverDTO> apiAutoPolicyWaivers)
      throws BadRequestException
  {
    if (apiAutoPolicyWaivers == null || apiAutoPolicyWaivers.isEmpty()) {
      throw new BadRequestException("No auto policy waiver configurations provided");
    }

    apiAutoPolicyWaivers.forEach(this::validateRequestDto);
  }

  private void validateRequestDto(ApiAutoPolicyWaiverDTO dto) throws BadRequestException {
    if (dto.threatLevel < 1 || dto.threatLevel > 10) {
      throw new BadRequestException(
          "Invalid threat level: " + dto.threatLevel + ". Value must be between 1 and 10.");
    }
    if (AutoPolicyWaiverUtil.checkSettingDisabled(dto.pathForward)
        && AutoPolicyWaiverUtil.checkSettingDisabled(dto.reachability)) {
      throw new BadRequestException("Path forward and reachability cannot both be false");
    }
  }

  private void validateAutoWaiverUpdateDto(
      ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO,
      AutoPolicyWaiver autoPolicyWaiver) throws BadRequestException
  {
    if (apiAutoPolicyWaiverDTO.threatLevel == autoPolicyWaiver.getThreatLevel() &&
        apiAutoPolicyWaiverDTO.reachability == autoPolicyWaiver.hasReachability() &&
        apiAutoPolicyWaiverDTO.pathForward == autoPolicyWaiver.hasPathForward() &&
        apiAutoPolicyWaiverDTO.scopesOperatorAny == autoPolicyWaiver.getScopesOperatorAny()) {
      throw new BadRequestException("No changes made to auto policy waiver configuration");
    }
  }

  private void auditAutoPolicyWaiver(AutoPolicyWaiver autoPolicyWaiver) {
    AuditData.get().setData("autoPolicyWaiverId", autoPolicyWaiver.getId());
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(@SuppressWarnings("unused") @AuthzContext(Key.APPLICATION_ID) String applicationId) {
    // no-op
  }

  private void sendTelemetry() {
    telemetrySender.send(autoPolicyWaiverTelemetryCollector.getTelemetryData());
  }
}
