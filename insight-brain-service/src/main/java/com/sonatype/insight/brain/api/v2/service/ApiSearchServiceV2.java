/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;
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
  public ApiSearchServiceV2(final BaseUrl baseUrl, final ApplicationDAO applicationDAO,
      final PolicyEvaluationDAO policyEvaluationDAO, final ApplicationComponentDAO applicationComponentDAO,
      final PolicyViolationDAO policyViolationDAO) {
    this.baseUrl = baseUrl;
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.applicationComponentDAO = applicationComponentDAO;
    this.policyViolationDAO = policyViolationDAO;
  }

  public ApiSearchResultsDTOV2 searchComponent(String stageId, String hash, ComponentIdentifier componentIdentifier)
      throws IOException
  {
    if (StringUtils.isEmpty(stageId)) {
      throw new BadRequestException("Stage has not been specified.");
    }
    if (StageTypes.getById(stageId) == null) {
      throw new BadRequestException("Invalid stage: " + stageId + ".");
    }

    ArtifactCoordinate coords = null;
    if (componentIdentifier != null) {
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

    for (Application app : getApplicationsWithReadPermission()) {
      PolicyEvaluation eval = policyEvaluationDAO.getLastPrimaryByApplicationIdAndStageId(app.getId(), stageId);
      if (eval == null) {
        continue;
      }

      List<ApplicationComponent> applicationComponentList =
          applicationComponentDAO.getByApplicationIdAndStageTypeId(app.getId(), stageId);
      for (ApplicationComponent applicationComponent : applicationComponentList) {
        String h = applicationComponent.getHash();
        if (hash != null && !hash.equalsIgnoreCase(h)) {
          continue;
        }

        ComponentIdentifier otherComponentIdentifier = applicationComponent.getComponentIdentifier();
        if (coords != null && !coords.matches(otherComponentIdentifier)) {
          continue;
        }

        ApiSearchResultDTOV2 result = new ApiSearchResultDTOV2();
        result.applicationId = app.getPublicId();
        result.applicationName = app.getName();
        result.reportUrl = baseUrl + UserInterfaceLinksResource.getReportUrl(app.getPublicId(), eval.getScanId());
        result.hash = h;
        result.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(otherComponentIdentifier);
        results.results.add(result);
        result.threatLevel = getMaxThreatLevel(policyViolationDAO.getActiveByEvaluationIdAndHash(eval.getId(), h));

        if (hash != null) {
          break;
        }
      }
    }

    log.debug("Searched for component with hash={} and componentIdentifier={} in {} ms, got {} hits", hash,
        componentIdentifier, System.currentTimeMillis() - start, results.results.size());

    return results;
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
