/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverRevocationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverRevocationResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverRevocationRequestDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverRevocationDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation.ComponentMatcherStrategyForRevocation;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.collect.ImmutableList;

public class ApiAutoPolicyWaiverRevocationService
{
  private final ReportService reportService;

  private final AutoPolicyWaiverRevocationDAO autoPolicyWaiverRevocationDAO;

  private final AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final CurrentUser currentUser;

  private static final Pattern CVE_REGEX_PATTERN = Pattern.compile("((CVE|SONATYPE|sonatype)-\\d+-\\d+)");

  private static final List<String> SECURITY_CONDITIONS = ImmutableList
      .of(SecurityVulnerabilitySeverityConditionType.ID, SecurityVulnerabilityStatusConditionType.ID);

  @Inject
  public ApiAutoPolicyWaiverRevocationService(
      ReportService reportService,
      AutoPolicyWaiverRevocationDAO autoPolicyWaiverRevocationDAO,
      AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      ApplicationDAO applicationDAO,
      OrganizationDAO organizationDAO,
      CurrentUser currentUser)
  {
    this.reportService = reportService;
    this.autoPolicyWaiverRevocationDAO = autoPolicyWaiverRevocationDAO;
    this.autoPolicyWaiverDAO = autoPolicyWaiverDAO;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.currentUser = currentUser;
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  public ApiAutoPolicyWaiverRevocationResponseDTO addAutoPolicyWaiverRevocation(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      ApiAutoPolicyWaiverRevocationRequestDTO requestDTO)
  {
    checkOwnerType(ownerType, ownerId);
    validateAutoPolicyWaiver(ownerType, ownerId, requestDTO.autoPolicyWaiverId);
    validateRequestDto(requestDTO);

    // Load component & policy violation data from report files
    PolicyThreats policyThreats = reportService.getPolicyThreats(requestDTO.applicationPublicId, requestDTO.scanId);
    if (policyThreats == null) {
      throw new BadRequestException("Unable to load report file for provided application public ID & scan ID");
    }

    PolicyThreats.Component component = null;
    PolicyThreats.PolicyViolation policyViolation = null;
    for (PolicyThreats.Component c : policyThreats.aaData) {
      for (PolicyThreats.PolicyViolation violation : c.allViolations) {
        if (requestDTO.policyViolationId.equals(violation.policyViolationId)) {
          policyViolation = violation;
          component = c;
        }
      }
    }
    if (component == null || policyViolation == null) {
      throw new BadRequestException("Component not found in scan");
    }

    // Initialize revocation and add basic details
    AutoPolicyWaiverRevocation newRevocation = new AutoPolicyWaiverRevocation();
    newRevocation.setOwnerId(ownerId);
    newRevocation.setCreatorId(currentUser.getUserPrincipal().getUsername());
    newRevocation.setCreatorName(currentUser.getUserPrincipal().getDisplayName());
    newRevocation.setCreateTime(new Date());
    newRevocation.setAutoPolicyWaiverId(requestDTO.autoPolicyWaiverId);
    newRevocation.setPolicyViolationId(requestDTO.policyViolationId);
    newRevocation.setScanId(requestDTO.scanId);
    newRevocation.setComponentMatchStrategy(requestDTO.matchStrategy);

    // Attributes shared for all strategies; only needed for exclusion log in the UI
    newRevocation.setPolicyName(policyViolation.policyName);
    newRevocation.setThreatLevel(policyViolation.policyThreatLevel);
    newRevocation.setComponentDisplayName(
        ComponentDisplayNameUtil.fromIdentifier(component.componentIdentifier).toString());
    try {

      List<ConstraintFact> constraintFacts =
          Arrays.asList(JsonUtils.parse(policyViolation.constraintFactsJson, ConstraintFact[].class));
      newRevocation.setVulnerabilityIdentifiers(getCveIdentifiers(constraintFacts));
      component.componentIdentifier.ensureComplete();
      newRevocation.setComponentIdentifier(component.componentIdentifier);

      // Set attributes based on the match strategy - ALL_VERSIONS does not require any extra data
      if (requestDTO.matchStrategy == ComponentMatcherStrategyForRevocation.POLICY_VIOLATION) {
        newRevocation.setPolicyId(policyViolation.policyId);
        newRevocation.setHash(component.hash);
        newRevocation.setConstraintFacts(constraintFacts);
      }
      else if (requestDTO.matchStrategy == ComponentMatcherStrategyForRevocation.EXACT_COMPONENT) {
        newRevocation.setHash(component.hash);
      }

      checkForExistingRecord(newRevocation);

      autoPolicyWaiverRevocationDAO.insert(newRevocation);
      auditAutoPolicyWaiverRevocation(newRevocation);
      return ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(newRevocation);
    }
    catch (IOException e) {
      throw new BadRequestException("Failed to parse constraint facts JSON", e);
    }
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  public void deleteAutoPolicyWaiverRevocation(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      String autoPolicyWaiverRevocationId)
  {
    checkOwnerType(ownerType, ownerId);
    AutoPolicyWaiverRevocation autoPolicyWaiverRevocation =
        autoPolicyWaiverRevocationDAO.getByIdNotNull(autoPolicyWaiverRevocationId);
    if (!ownerId.equals(autoPolicyWaiverRevocation.getOwnerId())) {
      throw new NotFoundException("Cannot find an auto policy waiver revocation with ID "
          + autoPolicyWaiverRevocationId + " for " + ownerType
          + " with ID " + ownerId);
    }
    auditAutoPolicyWaiverRevocation(autoPolicyWaiverRevocation);
    autoPolicyWaiverRevocationDAO.delete(autoPolicyWaiverRevocation);
  }

  @Authorize(permission = Permission.READ)
  public List<ApiAutoPolicyWaiverRevocationResponseDTO> getAutoPolicyWaiverRevocations(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.INTERNAL_ID) String ownerId,
      String autoPolicyWaiverId,
      int page,
      int pageSize
  )
  {
    checkOwnerType(ownerType, ownerId);
    List<AutoPolicyWaiverRevocation> revocations =
        autoPolicyWaiverRevocationDAO.getByOwnerIdAndAutoPolicyWaiverIdPaginated(
            ownerId,
            autoPolicyWaiverId,
            page,
            pageSize
        );

    List<ApiAutoPolicyWaiverRevocationResponseDTO> results = new ArrayList<>();
    for (AutoPolicyWaiverRevocation revocation : revocations) {
      results.add(ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation));
    }
    return results;
  }

  private void checkOwnerType(OwnerType ownerType, String ownerId) throws BadRequestException {
    switch (ownerType) {
      case APPLICATION:
        AuditData.get().setData("applicationId", ownerId).setApplication(applicationDAO.getById(ownerId));
        break;
      case ORGANIZATION:
        AuditData.get().setData("organizationId", ownerId).setOrganization(organizationDAO.getById(ownerId));
        break;
      default:
        throw new BadRequestException("Unknown owner type: " + ownerType);
    }
  }

  private void validateAutoPolicyWaiver(OwnerType ownerType, String ownerId, String autoPolicyWaiverId) {
    if (autoPolicyWaiverId == null) {
      throw new BadRequestException("autoPolicyWaiverId is required");
    }
    if (autoPolicyWaiverId.length() > 50) {
      throw new BadRequestException("autoPolicyWaiverId exceeds maximum length of 50 characters");
    }
    try {
      autoPolicyWaiverDAO.getByIdAndOwnerIdNotNull(autoPolicyWaiverId, ownerId);
    }
    catch (NotFoundException e) {
      throw new BadRequestException("Auto policy waiver with ID " + autoPolicyWaiverId + " not found for " + ownerType
          + " with ID " + ownerId);
    }
  }

  private void validateRequestDto(ApiAutoPolicyWaiverRevocationRequestDTO requestDTO) {
    if (requestDTO == null) {
      throw new BadRequestException("request body is required");
    }
    if (requestDTO.applicationPublicId == null) {
      throw new BadRequestException("applicationPublicId is required");
    }
    if (requestDTO.applicationPublicId.length() > 200) {
      throw new BadRequestException("applicationPublicId exceeds maximum length of 200 characters");
    }
    if (requestDTO.scanId == null) {
      throw new BadRequestException("scanId is required");
    }
    if (requestDTO.scanId.length() > 50) {
      throw new BadRequestException("scanId exceeds maximum length of 50 characters");
    }
    if (requestDTO.policyViolationId == null) {
      throw new BadRequestException("policyViolationId is required");
    }
    if (requestDTO.policyViolationId.length() > 50) {
      throw new BadRequestException("policyViolationId exceeds maximum length of 50 characters");
    }
    if (requestDTO.ownerId == null) {
      throw new BadRequestException("ownerId is required");
    }
    if (requestDTO.ownerId.length() > 50) {
      throw new BadRequestException("ownerId exceeds maximum length of 50 characters");
    }
    ComponentMatcherStrategyForRevocation strategy = requestDTO.matchStrategy;
    if (strategy == null) {
      throw new BadRequestException("matchStrategy is required");
    }
  }

  private void auditAutoPolicyWaiverRevocation(AutoPolicyWaiverRevocation autoPolicyWaiverRevocation) {
    AuditData.get().setData("autoPolicyWaiverRevocationId", autoPolicyWaiverRevocation.getId());
  }

  String getCveIdentifiers(List<ConstraintFact> constraintFacts) {
    if (constraintFacts == null) {
      return null;
    }
    List<ConditionFact> securityConditions = getSecurityConditions(constraintFacts);
    return String.join(",", securityConditions.stream().map(this::matchCVEs).flatMap(List::stream).distinct().toList());
  }

  List<ConditionFact> getSecurityConditions(List<ConstraintFact> constraintFacts) {
    if (constraintFacts == null) {
      return List.of();
    }
    return constraintFacts
        .stream()
        .map(ConstraintFact::getConditionFacts)
        .flatMap(Collection::stream)
        .filter(conditionFact -> SECURITY_CONDITIONS.contains(conditionFact.getConditionTypeId()))
        .toList();
  }

  List<String> matchCVEs(ConditionFact conditionFact) {
    List<String> cves = new ArrayList<>();
    if (conditionFact == null || conditionFact.getReference() == null) {
      return cves;
    }
    final Matcher matcher = CVE_REGEX_PATTERN.matcher(conditionFact.getReference().getValue());
    while (matcher.find()) {
      cves.add(matcher.group(1));
    }
    return cves;
  }

  String buildReportUrl(String applicationId, String scanId) {
    if (applicationId == null || scanId == null) {
      return null;
    }
    return "/applicationReport/" + applicationId + "/" + scanId + "/policy";
  }

  private void checkForExistingRecord(AutoPolicyWaiverRevocation newRevocation) {
    AutoPolicyWaiverRevocation existingRevocation = autoPolicyWaiverRevocationDAO.getByOwnerIdPolicyViolation(
        newRevocation.getOwnerId(),
        newRevocation.getAutoPolicyWaiverId(),
        newRevocation.getPolicyViolationId()
    );
    if (existingRevocation != null) {
      throw new BadRequestException("Revocation already exists for this policy violation");
    }
  }
}
