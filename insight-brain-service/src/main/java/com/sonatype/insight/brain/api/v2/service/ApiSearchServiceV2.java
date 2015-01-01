/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.conditions.ArtifactCoordinate;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertUtil;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
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

  private final InsightWork work;

  private final BaseUrl baseUrl;

  private final ReportService reportService;


  @Inject
  public ApiSearchServiceV2(final InsightWork work, final BaseUrl baseUrl, final ReportService reportService) {
    this.work = work;
    this.baseUrl = baseUrl;
    this.reportService = reportService;

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
      if (componentIdentifier.isMaven()) {
        coords = new ArtifactCoordinate(componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID),
            componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID),
            componentIdentifier.get(ComponentIdentifier.VERSION));
      }
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
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    for (Application app : getApplicationsWithReadPermission()) {
      PolicyEvaluation eval = policyEvaluationDAO.getLastPrimaryByApplicationIdAndStageId(app.getId(), stageId);
      if (eval == null) {
        continue;
      }

      File reportFile = reportService.getReport(work, app.getId(), eval.getScanId());
      if (reportFile == null) {
        log.error("Cannot search application {} for component, recent report does not exist", app.getName());
        continue;
      }
      List<PolicyAlert> alerts = null;
      JsonNode bomNode = JsonUtils.parse(Report.getEntry(reportFile, "bom.json").buf);
      for (JsonNode componentNode : bomNode.get("aaData")) {
        String h = componentNode.path("hash").asText();
        if (hash != null && !hash.equalsIgnoreCase(h)) {
          continue;
        }

        ComponentIdentifier bomComponentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(componentNode);
        if (coords != null && !coords.matches(bomComponentIdentifier)) {
          continue;
        }

        ApiSearchResultDTOV2 result = new ApiSearchResultDTOV2();
        result.applicationId = app.getPublicId();
        result.applicationName = app.getName();
        result.reportUrl = baseUrl + UserInterfaceLinksResource.getReportUrl(app.getPublicId(), eval.getScanId());
        result.hash = h;
        result.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(bomComponentIdentifier);
        results.results.add(result);

        if (alerts == null) {
          alerts = PolicyAlertUtil.createPolicyAlerts(eval);
        }
        result.threatLevel = null;
        for (PolicyAlert alert : alerts) {
          if (result.threatLevel != null && alert.getTrigger().getThreatLevel() <= result.threatLevel) {
            continue;
          }
          for (ComponentFact fact : alert.getTrigger().getComponentFacts()) {
            if (result.hash.equalsIgnoreCase(fact.getHash())) {
              result.threatLevel = alert.getTrigger().getThreatLevel();
              break;
            }
          }
        }

        if (hash != null) {
          break;
        }
      }
    }

    log.debug("Searched for component with hash={} and componentIdentifier={} in {} ms, got {} hits", hash,
        componentIdentifier, System.currentTimeMillis() - start, results.results.size());

    return results;
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  List<Application> getApplicationsWithReadPermission() {
    return new ApplicationDAO().getAll();
  }
}
