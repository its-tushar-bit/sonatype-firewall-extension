/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.conditions.ArtifactCoordinate;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertUtil;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportResource;
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
 * Enables end users to search for components within their applications. This REST API is exposed directly to users.
 * 
 * @since 1.7
 */
@Path(SearchResource.SERVICE_PATH)
@Named
public class SearchResource
{
  public static final String SERVICE_PATH = "api/v1/search/component";

  private static final Logger log = LoggerFactory.getLogger(SearchResource.class);

  private final InsightWork work;

  private final BaseUrl baseUrl;

  @Inject
  public SearchResource(InsightWork work, BaseUrl baseUrl) {
    this.work = work;
    this.baseUrl = baseUrl;
  }

  /**
   * Searches all currently registered applications for a component matching the given search criteria. A component can
   * be searched for by its hash or its coordinates, the latter supporting wildcards like the equivalent policy
   * condition. The mandatory stage parameter restricts which scans/reports of the applications are inspected for the
   * component.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public SearchResults searchComponent(@QueryParam("stageId") String stageId, @QueryParam("hash") String hash,
      @QueryParam("groupId") String groupId, @QueryParam("artifactId") String artifactId,
      @QueryParam("version") String version) throws IOException
  {
    if (StringUtils.isEmpty(stageId)) {
      throw new BadRequestException("Stage has not been specified");
    }
    if (StageTypes.getById(stageId) == null) {
      throw new BadRequestException("Invalid stage: " + stageId);
    }

    ArtifactCoordinate coords = null;
    if (!StringUtils.isEmpty(groupId) || !StringUtils.isEmpty(artifactId) || !StringUtils.isEmpty(version)) {
      coords = new ArtifactCoordinate(groupId, artifactId, version);
    }
    else if (StringUtils.isEmpty(hash)) {
      throw new BadRequestException("Neither hash nor coordinates of component to search for have been specified");
    }
    if (!StringUtils.isEmpty(hash)) {
      if (!hash.matches("[0-9a-fA-F]{20,40}")) {
        throw new BadRequestException("Invalid hash: " + hash);
      }
      hash = hash.substring(0, 20);
    }
    else {
      hash = null;
    }

    log.debug("Searching for component with hash={} and gav={}:{}:{}", hash, groupId, artifactId, version);

    long start = System.currentTimeMillis();
    SearchResults results = new SearchResults();
    results.criteria.stageId = stageId;
    results.criteria.hash = hash;
    results.criteria.groupId = groupId;
    results.criteria.artifactId = artifactId;
    results.criteria.version = version;
    String baseUrl = this.baseUrl.get();
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    for (Application app : getApplicationsWithReadPermission()) {
      PolicyEvaluation eval = policyEvaluationDAO.getLastPrimaryByApplicationIdAndStageId(app.getId(), stageId);
      if (eval == null) {
        continue;
      }

      File reportFile = ReportResource.getReport(work, app.getId(), eval.getScanId());
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

        String g = componentNode.path("groupId").asText();
        String a = componentNode.path("artifactId").asText();
        String v = componentNode.path("version").asText();
        if (coords != null && !coords.matches(g, a, v)) {
          continue;
        }

        SearchResult result = new SearchResult();
        result.applicationId = app.getPublicId();
        result.applicationName = app.getName();
        result.reportUrl = baseUrl + UserInterfaceLinksResource.getReportUrl(app.getPublicId(), eval.getScanId());
        result.hash = h;
        result.groupId = g;
        result.artifactId = a;
        result.version = v;
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

    log.debug("Searched for component with hash={} and gav={}:{}:{} in {} ms, got {} hits", hash, groupId, artifactId,
        version, System.currentTimeMillis() - start, results.results.size());

    return results;
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  List<Application> getApplicationsWithReadPermission() {
    return new ApplicationDAO().getAll();
  }

  /**
   * Aggregates the results of a search.
   */
  public static class SearchResults
  {
    public SearchCriteria criteria = new SearchCriteria();

    public List<SearchResult> results = new ArrayList<SearchResult>();
  }

  /**
   * Describes a single component within an application that matches the search criteria.
   */
  public static class SearchResult
  {
    public String applicationId;

    public String applicationName;

    public String reportUrl;

    public String hash;

    public String groupId;

    public String artifactId;

    public String version;

    public Integer threatLevel;
  }

  /**
   * Describes the search criteria that was processed.
   */
  public static class SearchCriteria
  {
    public String stageId;

    public String hash;

    public String groupId;

    public String artifactId;

    public String version;
  }
}
