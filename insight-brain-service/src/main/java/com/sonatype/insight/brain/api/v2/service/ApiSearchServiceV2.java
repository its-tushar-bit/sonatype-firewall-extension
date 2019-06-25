/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.ArtifactCoordinate;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.purl.PurlIdentifier;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.error.exception.BadRequestException;

import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.13.0
 */
@Named
public class ApiSearchServiceV2
{
  private static final Logger log = LoggerFactory.getLogger(ApiSearchServiceV2.class);

  private final BaseUrl baseUrl;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final PolicyViolationDAO policyViolationDAO;

  @Inject
  public ApiSearchServiceV2(final BaseUrl baseUrl,
                            final ApplicationDAO applicationDAO,
                            final PolicyEvaluationDAO policyEvaluationDAO,
                            final ApplicationComponentDAO applicationComponentDAO,
                            final PolicyViolationDAO policyViolationDAO)
  {
    this.baseUrl = baseUrl;
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.applicationComponentDAO = applicationComponentDAO;
    this.policyViolationDAO = policyViolationDAO;
  }

  public ApiSearchResultsDTOV2 searchComponent(String stageId,
                                               String hash,
                                               ComponentIdentifier componentIdentifier,
                                               String packageUrl)
  {
    AuditData.get().setComponentHash(hash).setComponentIdentifier(componentIdentifier);

    if (StringUtils.isEmpty(stageId)) {
      throw new BadRequestException("Stage has not been specified.");
    }
    if (StageTypes.getById(stageId) == null) {
      throw new BadRequestException("Invalid stage: " + stageId + ".");
    }

    ArtifactCoordinate coords = null;
    if (packageUrl != null) {
      componentIdentifier = new PurlIdentifier(packageUrl).toComponentIdentifier();
      AuditData.get().setComponentIdentifier(componentIdentifier);

      componentIdentifier =
          constructWildcardedComponentIdentifier(componentIdentifier);
      coords = new ArtifactCoordinate(componentIdentifier);
    }
    else if (componentIdentifier != null) {
      componentIdentifier = constructWildcardedComponentIdentifier(componentIdentifier);
      coords = new ArtifactCoordinate(componentIdentifier);
    }
    else if (StringUtils.isEmpty(hash)) {
      throw new BadRequestException("Neither hash nor coordinates of component to search for have been specified.");
    }
    if (!StringUtils.isEmpty(hash)) {
      if (!hash.matches("[0-9a-fA-F]{20,40}")) {
        throw new BadRequestException("Invalid hash: " + hash + ".");
      }
      hash = hash.substring(0, 20);
    }
    else {
      hash = null;
    }

    log.debug("Searching for component with hash={} and componentIdentifier={}", hash, componentIdentifier);

    long start = System.currentTimeMillis();
    ApiSearchResultsDTOV2 results = new ApiSearchResultsDTOV2();
    results.criteria.stageId = stageId;
    results.criteria.hash = hash;
    results.criteria.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    String baseUrl = this.baseUrl.get();

    List<Application> apps = getApplicationsWithReadPermission();

    AuditData.get().setData("inspectedApplicationCount", apps.size());

    for (Application app : apps) {
      PolicyEvaluation eval = policyEvaluationDAO.getLastPrimaryByApplicationIdAndStageId(app.getId(), stageId);
      if (eval == null) {
        continue;
      }

      List<ApplicationComponent> applicationComponentList = applicationComponentDAO.getByApplicationIdAndStageTypeId(
          app.getId(), stageId);
      for (ApplicationComponent applicationComponent : applicationComponentList) {
        String candidateHash = applicationComponent.getHash();
        if (hash != null && !hash.equalsIgnoreCase(candidateHash)) {
          continue;
        }

        ComponentIdentifier candidateComponentIdentifier = applicationComponent.getComponentIdentifier();
        if (coords != null && coordsDoesNotMatch(coords, candidateComponentIdentifier)) {
          continue;
        }

        ApiSearchResultDTOV2 result = new ApiSearchResultDTOV2();
        result.applicationId = app.getPublicId();
        result.applicationName = app.getName();
        result.reportUrl = baseUrl + UserInterfaceLinksResource.getReportUrl(app.getPublicId(), eval.getScanId());
        result.hash = candidateHash;
        result.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(candidateComponentIdentifier);
        results.results.add(result);
        result.threatLevel = getMaxThreatLevel(
            policyViolationDAO.getActiveByApplicationIdAndStageIdAndHash(app.getId(), stageId, candidateHash));

        if (hash != null) {
          break;
        }
      }
    }

    AuditData.get().setData("resultRecordCount", results.results.size());

    log.debug("Searched for component with hash={} and componentIdentifier={} in {} ms, got {} hits", hash,
        componentIdentifier, System.currentTimeMillis() - start, results.results.size());

    return results;
  }

  private boolean coordsDoesNotMatch(
      final ArtifactCoordinate coords,
      final ComponentIdentifier candidateComponentIdentifier)
  {
    if (candidateComponentIdentifier == null) {
      return true;
    }

    switch (candidateComponentIdentifier.getFormat()) {
      case ComponentIdentifier.FORMAT_PYPI:
        return !coords.matchesIgnoreCase(candidateComponentIdentifier);
      default:
        return !coords.matches(candidateComponentIdentifier);
    }
  }

  private ComponentIdentifier constructWildcardedComponentIdentifier(final ComponentIdentifier componentIdentifier) {
    return new ComponentIdentifier(componentIdentifier.getFormat(),
        convertToWildcardWhereNeeded(componentIdentifier.getFormat(), componentIdentifier.getCoordinates()));
  }

  /**
   * Converts coordinates so that all values are explicitly set
   */
  private Map<String, String> convertToWildcardWhereNeeded(final String format, final Map<String, String> coordinates) {
    final Map<String, String> convertedCoordinates = new LinkedHashMap<>(coordinates);
    final Set<String> allCoordinateNames = ComponentIdentifier.getAllCoordinateNames(format);
    final Set<String> requiredCoordinateNames = ComponentIdentifier.getAllRequiredCoordinateNames(format);
    for (String coordinateName : allCoordinateNames) {
      final String coordinateValue = coordinates.get(coordinateName);
      if (requiredCoordinateNames.contains(coordinateName)) {
        // a required coordinate must have a value, so null/empty implies it is a wildcard
        convertedCoordinates.put(coordinateName,
            StringUtils.isEmpty(coordinateValue) ? ArtifactCoordinate.PLACEHOLDER : coordinateValue);
      }
      else {
        // an optional coordinate can have a value or be empty, so only null implies it is a wildcard
        convertedCoordinates
            .put(coordinateName, coordinateValue == null ? ArtifactCoordinate.PLACEHOLDER : coordinateValue);
      }
    }
    return convertedCoordinates;
  }

  private Integer getMaxThreatLevel(final List<PolicyViolation> policyViolations) {
    Integer result = null;
    for (PolicyViolation policyViolation : policyViolations) {
      int threatLevel = policyViolation.getThreatLevel();
      if (result == null || threatLevel > result) {
        result = threatLevel;
      }
    }
    return result;
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  List<Application> getApplicationsWithReadPermission() {
    return applicationDAO.getAll();
  }
}
