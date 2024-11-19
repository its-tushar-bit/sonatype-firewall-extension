/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverRevocationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverRevocationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverRevocationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation.ComponentMatcherStrategyForRevocation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.ImmutableList;

public class ApiAutoPolicyWaiverRevocationService
{
  private final AutoPolicyWaiverRevocationDAO autoPolicyWaiverRevocationDAO;

  private final AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final CurrentUser currentUser;

  private static final Pattern CVE_REGEX_PATTERN = Pattern.compile("((CVE|SONATYPE|sonatype)-\\d+-\\d+)");

  private static final List<String> SECURITY_CONDITIONS = ImmutableList
      .of(SecurityVulnerabilitySeverityConditionType.ID, SecurityVulnerabilityStatusConditionType.ID);

  @Inject
  public ApiAutoPolicyWaiverRevocationService(
      AutoPolicyWaiverRevocationDAO autoPolicyWaiverRevocationDAO,
      AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      ApplicationDAO applicationDAO,
      OrganizationDAO organizationDAO,
      PolicyViolationDAO policyViolationDAO,
      CurrentUser currentUser)
  {
    this.autoPolicyWaiverRevocationDAO = autoPolicyWaiverRevocationDAO;
    this.autoPolicyWaiverDAO = autoPolicyWaiverDAO;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.currentUser = currentUser;
  }

  @Authorize(permission = Permission.WAIVE_POLICY_VIOLATIONS)
  public ApiAutoPolicyWaiverRevocationDTO addAutoPolicyWaiverRevocation(
      @AuthzContext(Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String ownerId,
      ApiAutoPolicyWaiverRevocationDTO apiAutoPolicyWaiverRevocationDTO)
  {
    checkOwnerType(ownerType, ownerId);
    validateRequestDto(apiAutoPolicyWaiverRevocationDTO);

    AutoPolicyWaiverRevocation autoPolicyWaiverRevocation = new AutoPolicyWaiverRevocation();
    autoPolicyWaiverRevocation.setOwnerId(ownerId);
    autoPolicyWaiverRevocation.setCreatorId(currentUser.getUserPrincipal().getUsername());
    autoPolicyWaiverRevocation.setCreatorName(currentUser.getUserPrincipal().getDisplayName());
    autoPolicyWaiverRevocation.setCreateTime(new Date());
    autoPolicyWaiverRevocation.setAutoPolicyWaiverId(apiAutoPolicyWaiverRevocationDTO.autoPolicyWaiverId);
    autoPolicyWaiverRevocation.setHash(apiAutoPolicyWaiverRevocationDTO.hash);
    autoPolicyWaiverRevocation.setAssociatedPackageUrl(apiAutoPolicyWaiverRevocationDTO.associatedPackageUrl);
    autoPolicyWaiverRevocation.setScanId(apiAutoPolicyWaiverRevocationDTO.scanId);
    autoPolicyWaiverRevocation.setComponentMatchStrategy(apiAutoPolicyWaiverRevocationDTO.componentMatchStrategy);
    autoPolicyWaiverRevocation.setPolicyViolationId(apiAutoPolicyWaiverRevocationDTO.policyViolationId);
    autoPolicyWaiverRevocation.setThreatLevel(apiAutoPolicyWaiverRevocationDTO.threatLevel);
    autoPolicyWaiverRevocation.setComponentDisplayName(apiAutoPolicyWaiverRevocationDTO.componentDisplayName);
    autoPolicyWaiverRevocation.setPolicyName(apiAutoPolicyWaiverRevocationDTO.policyName);
    autoPolicyWaiverRevocation.setVulnerabilityIdentifiers(apiAutoPolicyWaiverRevocationDTO.vulnerabilityIdentifiers);

    if (apiAutoPolicyWaiverRevocationDTO.policyViolationId != null) {
      autoPolicyWaiverRevocation.setPolicyViolationId(apiAutoPolicyWaiverRevocationDTO.policyViolationId);

      PolicyViolation policyViolation = policyViolationDAO.getById(apiAutoPolicyWaiverRevocationDTO.policyViolationId);
      if (policyViolation != null) {
        if (autoPolicyWaiverRevocation.getThreatLevel() == null) {
          autoPolicyWaiverRevocation.setThreatLevel(policyViolation.getThreatLevel());
        }

        if (autoPolicyWaiverRevocation.getComponentDisplayName() == null) {
          autoPolicyWaiverRevocation.setComponentDisplayName(
              ComponentDisplayNameUtil.fromPolicyViolation(policyViolation).getName() + " " +
                  policyViolation.getComponentIdentifier().getCoordinates().get("version"));
        }

        if (autoPolicyWaiverRevocation.getPolicyName() == null) {
          autoPolicyWaiverRevocation.setPolicyName(policyViolation.getPolicyName());
        }

        if (autoPolicyWaiverRevocation.getVulnerabilityIdentifiers() == null &&
            policyViolation.getThreatCategory().equals(
                PolicyThreatCategory.SECURITY)) {
          autoPolicyWaiverRevocation.setVulnerabilityIdentifiers(getCveIdentifiers(policyViolation));
        }
      }
    }

    autoPolicyWaiverRevocationDAO.insert(autoPolicyWaiverRevocation);
    auditAutoPolicyWaiverRevocation(autoPolicyWaiverRevocation);
    return ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(autoPolicyWaiverRevocation);
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
  public List<ApiAutoPolicyWaiverRevocationDTO> getAutoPolicyWaiverRevocations(
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

    List<ApiAutoPolicyWaiverRevocationDTO> results = new ArrayList<>();
    for (AutoPolicyWaiverRevocation revocation : revocations) {
      results.add(ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(revocation));
    }
    return results;
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

  private void validateRequestDto(ApiAutoPolicyWaiverRevocationDTO apiAutoPolicyWaiverRevocationDTO) {
    if (apiAutoPolicyWaiverRevocationDTO == null) {
      throw new BadRequestException("request body is required");
    }
    if (apiAutoPolicyWaiverRevocationDTO.autoPolicyWaiverId == null) {
      throw new BadRequestException("autoPolicyWaiverId is required");
    }
    if (apiAutoPolicyWaiverRevocationDTO.ownerId == null) {
      throw new BadRequestException("ownerId is required");
    }
    try {
      autoPolicyWaiverDAO.getByIdAndOwnerIdNotNull(
          apiAutoPolicyWaiverRevocationDTO.autoPolicyWaiverId,
          apiAutoPolicyWaiverRevocationDTO.ownerId);
    }
    catch (NotFoundException e) {
      throw new BadRequestException("combination of ownerId and autoPolicyWaiverId is invalid");
    }
    if (apiAutoPolicyWaiverRevocationDTO.scanId == null) {
      throw new BadRequestException("scanId is required");
    }
    ComponentMatcherStrategyForRevocation strategy = apiAutoPolicyWaiverRevocationDTO.componentMatchStrategy;
    if (strategy == ComponentMatcherStrategyForRevocation.ALL_VERSIONS) {
      if (apiAutoPolicyWaiverRevocationDTO.associatedPackageUrl == null) {
        throw new BadRequestException("associatedPackageUrl is required");
      }
    }
    else {
      if (apiAutoPolicyWaiverRevocationDTO.hash == null) {
        throw new BadRequestException("hash is required");
      }
    }
  }

  private void auditAutoPolicyWaiverRevocation(AutoPolicyWaiverRevocation autoPolicyWaiverRevocation) {
    AuditData.get().setData("autoPolicyWaiverRevocationId", autoPolicyWaiverRevocation.getId());
  }

  String getCveIdentifiers(PolicyViolation policyViolation) {
    if (policyViolation == null) {
      throw new IllegalStateException("PolicyViolation cannot be null");
    }
    List<ConstraintFact> constraintFacts = policyViolation.getConstraintFacts();
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
}
