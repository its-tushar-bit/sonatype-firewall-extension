/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.trending;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.trending.ApplicationRiskSummary;
import com.sonatype.insight.brain.model.trending.Applications;
import com.sonatype.insight.brain.model.trending.ComponentsSummary;
import com.sonatype.insight.brain.model.trending.PartialMatch;
import com.sonatype.insight.brain.model.trending.PolicyViolation;
import com.sonatype.insight.brain.model.trending.TrendingReport;
import com.sonatype.insight.brain.model.trending.TrendingReportMetadata;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationLog;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtils;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrendingReportProcessor
{
  private static final Logger log = LoggerFactory.getLogger(TrendingReportProcessor.class);

  public static final long TWENTY_DAYS_MS = 20 * 86400L * 1000L;

  public static final int PERIOD_COUNT = 4;

  public static final int APPLICATION_RISKS_COUNT = 5;

  public static final int PARTIAL_MATCHES_COUNT = 5;

  public static final long PERIOD_LENGTH_MS = TWENTY_DAYS_MS / PERIOD_COUNT;

  private final InsightWork work;

  private final PolicyEvaluationUtils policyEvaluationUtils;

  @Inject
  public TrendingReportProcessor(InsightWork work, PolicyEvaluationUtils policyEvaluationUtils) {
    this.work = work;
    this.policyEvaluationUtils = policyEvaluationUtils;
  }

  public TrendingReport calculate() throws IOException {
    final long now = new Date().getTime();

    TrendingReportMetadata meta = new TrendingReportMetadata("Report Generator", "Report Generator", now, now
        - TWENTY_DAYS_MS, now);

    // total component counts in all applications based on latest application reports
    Map<String, Map<String, Integer>> components = new HashMap<String, Map<String, Integer>>();
    components.put("exact", new HashMap<String, Integer>());
    components.put("similar", new HashMap<String, Integer>());
    components.put("unknown", new HashMap<String, Integer>());

    // per application alerts summary based on latest application reports
    List<ApplicationRiskSummary> applicationRisks = new ArrayList<ApplicationRiskSummary>();

    Map<String, PolicyViolation> policyViolations = new HashMap<String, PolicyViolation>();

    Map<List<String>, Set<String>> partialMatches = new HashMap<List<String>, Set<String>>();

    for (Application app : new ApplicationDAO().getAll()) {
      // alerts counts in this application
      int criticalAlerts = 0, severeAlerts = 0, moderateAlerts = 0, totalAlerts = 0;
      PolicyEvaluationLog evalLog = new PolicyEvaluationLog(work.getAuditDir(app.getId()));
      for (StageType stageType : StageTypes.getAll()) {
        // component counts in the latest report
        PolicyEvaluation lastEval = evalLog.lastByStage(stageType.getId());
        if (lastEval == null) {
          continue;
        }
        File reportFile = ReportResource.getReport(work, app.getId(), lastEval.getScanId());
        if (reportFile == null) {
          log.error("Cannot process application {}, recent report does not exist", app.getName());
          continue;
        }
        JsonNode bomNode = JsonUtils.parse(Report.getEntry(reportFile, "bom.json").buf);
        for (JsonNode componentNode : bomNode.get("aaData")) {
          String matchState = componentNode.path("matchState").asText();
          incrComponent(components, matchState, getComponentKey(componentNode));
          if (MatchState.SIMILAR.getId().equals(matchState)) {
            List<String> key = Arrays.asList(getAttribute(componentNode, "groupId"),
                getAttribute(componentNode, "artifactId"), getAttribute(componentNode, "version"));
            Set<String> hashes = partialMatches.get(key);
            if (hashes == null) {
              hashes = new HashSet<String>();
              partialMatches.put(key, hashes);
            }
            hashes.add(getAttribute(componentNode, "hash"));
          }
        }

        // application policy alert counts in the latest report
        for (PolicyAlert alert : policyEvaluationUtils.findPolicyAlerts(app.getId(), lastEval.getScanId())) {
          PolicyFact policyFact = alert.getTrigger();
          int level = policyFact.getThreatLevel();
          int componentCount = policyFact.getComponentFacts().size();
          totalAlerts += componentCount;
          if (level >= 8) {
            criticalAlerts += componentCount;
          }
          else if (level >= 4) {
            severeAlerts += componentCount;
          }
          else if (level >= 2) {
            moderateAlerts += componentCount;
          }
        }

        // policy alerts counts
        for (PolicyEvaluation eval : evalLog.allByStage(stageType.getId())) {
          int period = (int) ((now - eval.getTime()) / PERIOD_LENGTH_MS);
          if (period >= PERIOD_COUNT) {
            continue; // too old, skip
          }

          for (PolicyAlert alert : policyEvaluationUtils.findPolicyAlerts(app.getId(), eval.getScanId())) {
            PolicyFact policyFact = alert.getTrigger();
            for (ComponentFact componentFact : policyFact.getComponentFacts()) {
              String category = getViolationCategory(componentFact.getConstraintFacts());
              String policyViolationsKey = policyFact.getPolicyId() + ":" + category;
              PolicyViolation violations = policyViolations.get(policyViolationsKey);
              if (violations == null) {
                violations = new PolicyViolation(policyFact.getPolicyName(), category, policyFact.getThreatLevel(),
                    new int[PERIOD_COUNT]);
                policyViolations.put(policyViolationsKey, violations);
              }
              violations.getViolations()[period]++; // ain't perty but works
            }
          }
        }
      }
      applicationRisks.add(new ApplicationRiskSummary(app.getName(), criticalAlerts, severeAlerts, moderateAlerts,
          totalAlerts - criticalAlerts - severeAlerts - moderateAlerts));
    }

    TrendingReport report = new TrendingReport(meta, toComponentsSummary(components), toApplications(applicationRisks),
        new ArrayList<PolicyViolation>(policyViolations.values()), toPartialMatches(partialMatches));

    return report;
  }

  private Applications toApplications(List<ApplicationRiskSummary> applicationRisks) {
    List<ApplicationRiskSummary> risks = top(applicationRisks, APPLICATION_RISKS_COUNT,
        new Comparator<ApplicationRiskSummary>()
        {
          @Override
          public int compare(ApplicationRiskSummary o1, ApplicationRiskSummary o2) {
            return o2.getRisk() - o1.getRisk();
          }
        });
    return new Applications(applicationRisks.size(), risks);
  }

  private List<PartialMatch> toPartialMatches(Map<List<String>, Set<String>> partialMatches) {
    ArrayList<PartialMatch> top = new ArrayList<PartialMatch>();
    for (Map.Entry<List<String>, Set<String>> partialMatch : partialMatches.entrySet()) {
      List<String> key = partialMatch.getKey();
      top.add(new PartialMatch(key.get(0), key.get(1), key.get(2), partialMatch.getValue().size()));
    }
    return top(top, PARTIAL_MATCHES_COUNT, new Comparator<PartialMatch>()
    {
      @Override
      public int compare(PartialMatch o1, PartialMatch o2) {
        return o2.getCount() - o1.getCount();
      }
    });
  }

  private static <T> List<T> top(Collection<T> data, int count, Comparator<T> comparator) {
    ArrayList<T> top = new ArrayList<T>(data);
    Collections.sort(top, comparator);
    if (top.size() > count) {
      top.subList(count, top.size()).clear();
    }
    return top;
  }

  private String getViolationCategory(List<ConstraintFact> constraintFacts) {
    String[] categories = { "security", "license", "quality", "other" };
    int category = 3; // other
    for (ConstraintFact constraintFact : constraintFacts) {
      for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
        String conditionTypeId = conditionFact.getConditionTypeId().toLowerCase(Locale.US);
        if (conditionTypeId.contains("security")) {
          category = 0;
          break;
        }
        else if (conditionTypeId.contains("license")) {
          category = 1;
        }
        else if (category > 2 && (conditionTypeId.contains("age") || conditionTypeId.contains("popularity"))) {
          category = 2;
        }
      }
    }
    return categories[category];
  }

  private static ComponentsSummary toComponentsSummary(Map<String, Map<String, Integer>> components) {
    return new ComponentsSummary(components.get("exact").size(), components.get("similar").size(), components.get(
        "unknown").size());
  }

  private void incrComponent(Map<String, Map<String, Integer>> components, String matchState, String componentKey) {
    Map<String, Integer> matchStateComponents = components.get(matchState);
    Integer count = matchStateComponents.get(componentKey);
    if (count == null) {
      count = new Integer(1);
    }
    else {
      count = count + 1;
    }
    matchStateComponents.put(componentKey, count);
  }

  private static String getComponentKey(JsonNode componentNode) {
    return componentNode.path("hash").asText();
  }

  private static String getAttribute(JsonNode parent, String childname) {
    JsonNode child = parent.path(childname);
    return !child.isNull() ? child.asText() : null;
  }
}
