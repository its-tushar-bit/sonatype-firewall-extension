/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.autowaivers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverExclusionAdapter;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverExclusionResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverExclusionRequestDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverExclusionDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.telemetry.autowaivers.AutoPolicyWaiverExclusionTelemetryCollector;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ApiAutoPolicyWaiverExclusionService
{
  private static final Logger log = LoggerFactory.getLogger(ApiAutoPolicyWaiverExclusionService.class);

  private static final Pattern CVE_REGEX_PATTERN = Pattern.compile("((CVE|SONATYPE|sonatype)-\\d+-\\d+)");

  private static final List<String> SECURITY_CONDITIONS = List.of(SecurityVulnerabilitySeverityConditionType.ID,
      SecurityVulnerabilityStatusConditionType.ID);

  private final ReportService reportService;

  private final AutoPolicyWaiverExclusionDAO autoPolicyWaiverExclusionDAO;

  private final AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final CurrentUser currentUser;

  private final OwnerDAO ownerDAO;

  private final TelemetrySender telemetrySender;

  private final TelemetryUtils telemetryUtils;

  @Inject
  public ApiAutoPolicyWaiverExclusionService(
      ReportService reportService,
      AutoPolicyWaiverExclusionDAO autoPolicyWaiverExclusionDAO,
      AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      ApplicationDAO applicationDAO,
      OrganizationDAO organizationDAO,
      CurrentUser currentUser,
      OwnerDAO ownerDAO,
      TelemetrySender telemetrySender,
      TelemetryUtils telemetryUtils)
  {
    this.reportService = reportService;
    this.autoPolicyWaiverExclusionDAO = autoPolicyWaiverExclusionDAO;
    this.autoPolicyWaiverDAO = autoPolicyWaiverDAO;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.currentUser = currentUser;
    this.ownerDAO = ownerDAO;
    this.telemetrySender = telemetrySender;
    this.telemetryUtils = telemetryUtils;
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  public ApiAutoPolicyWaiverExclusionResponseDTO addAutoPolicyWaiverExclusion(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      ApiAutoPolicyWaiverExclusionRequestDTO requestDTO)
  {
    AutoPolicyWaiverUtil.validateAutoWaiversFeatureEnabled();
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

    // Initialize exclusion and add basic details
    AutoPolicyWaiverExclusion newExclusion = new AutoPolicyWaiverExclusion();
    newExclusion.setOwnerId(ownerId);
    newExclusion.setCreatorId(currentUser.getUserPrincipal().getUsername());
    newExclusion.setCreatorName(currentUser.getUserPrincipal().getDisplayName());
    newExclusion.setCreateTime(new Date());
    newExclusion.setAutoPolicyWaiverId(requestDTO.autoPolicyWaiverId);
    newExclusion.setPolicyViolationId(requestDTO.policyViolationId);
    newExclusion.setScanId(requestDTO.scanId);
    newExclusion.setComponentMatchStrategy(requestDTO.matchStrategy);

    // Attributes shared for all strategies; only needed for exclusion log in the UI
    newExclusion.setPolicyName(policyViolation.policyName);
    newExclusion.setThreatLevel(policyViolation.policyThreatLevel);
    newExclusion.setComponentDisplayName(
        ComponentDisplayNameUtil.fromIdentifier(component.componentIdentifier).toString());
    try {

      List<ConstraintFact> constraintFacts =
          Arrays.asList(JsonUtils.parse(policyViolation.constraintFactsJson, ConstraintFact[].class));
      newExclusion.setVulnerabilityIdentifiers(getCveIdentifiers(constraintFacts));
      component.componentIdentifier.ensureComplete();
      newExclusion.setComponentIdentifier(component.componentIdentifier);

      // Set attributes based on the match strategy - ALL_VERSIONS does not require any extra data
      if (requestDTO.matchStrategy == ComponentMatcherStrategyForExclusion.POLICY_VIOLATION) {
        newExclusion.setPolicyId(policyViolation.policyId);
        newExclusion.setHash(component.hash);
        newExclusion.setConstraintFacts(constraintFacts);
        log.debug("Excluding policy violation for component {} with hash {}",
            component.componentIdentifier,
            component.hash);
      }
      else if (requestDTO.matchStrategy == ComponentMatcherStrategyForExclusion.EXACT_COMPONENT) {
        newExclusion.setHash(component.hash);
        log.debug("Excluding policy violation for exact component {} with hash {}",
            component.componentIdentifier,
            component.hash);
      }
      else {
        log.debug("Excluding policy violation for all versions of component {}",
            component.componentIdentifier);
      }

      checkForExistingRecord(newExclusion);

      autoPolicyWaiverExclusionDAO.insert(newExclusion);
      auditAutoPolicyWaiverRevocation(newExclusion);
      log.debug("Added auto policy waiver exclusion {}", newExclusion.getId());

      Owner owner = ownerDAO.getById(ownerId);
      AutoPolicyWaiverExclusionTelemetryCollector telemetryCollector =
          new AutoPolicyWaiverExclusionTelemetryCollector(telemetryUtils);
      telemetryCollector.addTelemetryForCreateAutoWaiverExclusion(newExclusion, owner);
      sendTelemetry(telemetryCollector);

      return ApiAutoPolicyWaiverExclusionAdapter.convertToDTO(newExclusion);
    }
    catch (IOException e) {
      throw new BadRequestException("Couldn't add auto-waiver exclusion. Failed to parse " +
          "constraint facts JSON", e);
    }
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  public void deleteAutoPolicyWaiverExclusion(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      String autoPolicyWaiverExclusionId)
  {
    AutoPolicyWaiverUtil.validateAutoWaiversFeatureEnabled();
    checkOwnerType(ownerType, ownerId);
    AutoPolicyWaiverExclusion autoPolicyWaiverExclusion =
        autoPolicyWaiverExclusionDAO.getByIdNotNull(autoPolicyWaiverExclusionId);
    if (!ownerId.equals(autoPolicyWaiverExclusion.getOwnerId())) {
      throw new NotFoundException("Cannot find an auto policy waiver exclusion with ID "
          + autoPolicyWaiverExclusionId + " for " + ownerType
          + " with ID " + ownerId);
    }
    auditAutoPolicyWaiverRevocation(autoPolicyWaiverExclusion);
    autoPolicyWaiverExclusionDAO.delete(autoPolicyWaiverExclusion);

    Owner owner = ownerDAO.getById(ownerId);
    AutoPolicyWaiverExclusionTelemetryCollector telemetryCollector =
        new AutoPolicyWaiverExclusionTelemetryCollector(telemetryUtils);
    telemetryCollector.addTelemetryForDeleteAutoWaiverExclusion(autoPolicyWaiverExclusion, owner);
    sendTelemetry(telemetryCollector);

    log.debug("Deleted auto policy waiver exclusion {}", autoPolicyWaiverExclusionId);
  }

  @Authorize(permission = Permission.READ)
  public List<ApiAutoPolicyWaiverExclusionResponseDTO> getAutoPolicyWaiverExclusions(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.INTERNAL_ID) String ownerId,
      String autoPolicyWaiverId,
      int page,
      int pageSize)
  {
    AutoPolicyWaiverUtil.validateAutoWaiversFeatureEnabled();
    checkOwnerType(ownerType, ownerId);

    List<AutoPolicyWaiverExclusion> exclusions =
        autoPolicyWaiverExclusionDAO.getByOwnerIdAndAutoPolicyWaiverIdPaginated(
            ownerId,
            autoPolicyWaiverId,
            page,
            pageSize);

    Owner owner = getOwner(ownerType, ownerId);

    List<ApiAutoPolicyWaiverExclusionResponseDTO> results = new ArrayList<>();
    for (AutoPolicyWaiverExclusion exclusion : exclusions) {
      results.add(ApiAutoPolicyWaiverExclusionAdapter.convertToDTO(owner, exclusion));
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

  private Owner getOwner(final OwnerType ownerType, final String ownerId) throws BadRequestException {
    switch (ownerType) {
      case APPLICATION:
        return applicationDAO.getById(ownerId);
      case ORGANIZATION:
        return organizationDAO.getById(ownerId);
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

  private void validateRequestDto(ApiAutoPolicyWaiverExclusionRequestDTO requestDTO) {
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
    ComponentMatcherStrategyForExclusion strategy = requestDTO.matchStrategy;
    if (strategy == null) {
      throw new BadRequestException("matchStrategy is required");
    }
  }

  private void auditAutoPolicyWaiverRevocation(AutoPolicyWaiverExclusion autoPolicyWaiverExclusion) {
    AuditData.get().setData("autoPolicyWaiverRevocationId", autoPolicyWaiverExclusion.getId());
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

  private void checkForExistingRecord(AutoPolicyWaiverExclusion newExclusion) {
    AutoPolicyWaiverExclusion existingExclusion =
        autoPolicyWaiverExclusionDAO.getByOwnerIdPolicyViolation(
            newExclusion.getOwnerId(),
            newExclusion.getAutoPolicyWaiverId(),
            newExclusion.getPolicyViolationId());
    if (existingExclusion != null) {
      throw new BadRequestException("Exclusion already exists for this policy violation");
    }
  }

  private void sendTelemetry(AutoPolicyWaiverExclusionTelemetryCollector telemetryCollector) {
    telemetrySender.send(telemetryCollector.getTelemetryData());
  }
}
