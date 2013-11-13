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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.trending.ApplicationRiskSummary;
import com.sonatype.insight.brain.model.trending.Applications;
import com.sonatype.insight.brain.model.trending.ComponentRiskSummary;
import com.sonatype.insight.brain.model.trending.ComponentsSummary;
import com.sonatype.insight.brain.model.trending.DiffData;
import com.sonatype.insight.brain.model.trending.PartialMatch;
import com.sonatype.insight.brain.model.trending.PoliciesSummary;
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
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @since 1.7
 */
@Named
public class TrendingReportProcessor
{
  public static final String CATEGORY_OTHER = "other";

  public static final String CATEGORY_QUALITY = "quality";

  public static final String CATEGORY_LICENSE = "license";

  public static final String CATEGORY_SECURITY = "security";

  public static final String[] CATEGORIES = { CATEGORY_SECURITY, CATEGORY_LICENSE, CATEGORY_QUALITY, CATEGORY_OTHER };

  public static final String[] THREAT_LEVELS = { "critical", "severe", "moderate", "none" };

  /**
   * Length of reporting period, in milliseconds.
   */
  public static final long TWENTY_DAYS_MS = TimeUnit.DAYS.toMillis(20);

  /**
   * Number of reporting sub-periods.
   */
  public static final int PERIOD_COUNT = 4;

  /**
   * Maximum number of individual application summaries included in the generated report.
   */
  public static final int APPLICATION_RISKS_COUNT = 5;

  /**
   * Maximum number of individual partial match summaries included in the generated report.
   */
  public static final int PARTIAL_MATCHES_COUNT = 5;

  /**
   * Maximum number of individual component risk summaries included in the generated report.
   */
  public static final int COMPONENT_RISKS_COUNT = 5;

  public static final long PERIOD_LENGTH_MS = TWENTY_DAYS_MS / PERIOD_COUNT;

  /**
   * Stage id of application policy evaluations considered during report generation.
   */
  public static final String STAGE_ID = BuildStageType.ID;

  public static interface ProgressMonitor
  {
    public void tick(int total, int current);
  }

  private final InsightWork work;

  private final PolicyEvaluationUtils policyEvaluationUtils;

  private final Predicate<String> isSecurity = new StartsWithPredicate("security");

  private final Predicate<String> isLicense = new StartsWithPredicate("license");

  private final Predicate<String> isAge = new StartsWithPredicate("age");

  private final Predicate<String> isPopularity = new StartsWithPredicate("popularity");

  @Inject
  public TrendingReportProcessor(InsightWork work, PolicyEvaluationUtils policyEvaluationUtils) {
    this.work = work;
    this.policyEvaluationUtils = policyEvaluationUtils;
  }

  /**
   * Generates and returns trending report.
   *
   * @since 1.7
   */
  public TrendingReport calculate(ProgressMonitor monitor) throws IOException {
    final long now = new Date().getTime();

    TrendingReportMetadata meta = new TrendingReportMetadata(now, now - TWENTY_DAYS_MS, now);

    // total component counts in all applications based on latest application reports
    Map<String, Map<String, Integer>> components = new HashMap<String, Map<String, Integer>>();
    components.put("exact", new HashMap<String, Integer>());
    components.put("similar", new HashMap<String, Integer>());
    components.put("unknown", new HashMap<String, Integer>());

    // per application alerts summary based on latest application reports
    List<ApplicationRiskSummary> applicationRisks = new ArrayList<ApplicationRiskSummary>();

    Map<String, PolicyViolation> policyViolations = new HashMap<String, PolicyViolation>();

    Map<List<String>, Set<String>> partialMatches = new HashMap<List<String>, Set<String>>();

    Map<String, int[]> categories = new HashMap<String, int[]>();
    Map<String, int[]> previousCategories = new HashMap<String, int[]>();
    for (String category : CATEGORIES) {
      categories.put(category, new int[THREAT_LEVELS.length]);
      previousCategories.put(category, new int[THREAT_LEVELS.length]);
    }

    Map<String, Map<List<String>, int[]>> componentRisks = new LinkedHashMap<String, Map<List<String>, int[]>>();
    for (String category : CATEGORIES) {
      componentRisks.put(category, new HashMap<List<String>, int[]>());
    }
    componentRisks.put("all", new HashMap<List<String>, int[]>());

    final List<Application> applications = new ArrayList<Application>(new ApplicationDAO().getAll());
    for (int applicationNo = 0; applicationNo < applications.size(); applicationNo++) {
      monitor.tick(applications.size(), applicationNo);

      final Application application = applications.get(applicationNo);
      final Map<String, String> applicationPolicyCategories = determinePolicyCategories(application);

      // alerts counts in this application
      int criticalAlerts = 0, severeAlerts = 0, moderateAlerts = 0, totalAlerts = 0;
      PolicyEvaluationLog evalLog = new PolicyEvaluationLog(work.getAuditDir(application.getId()));

      // most recent evaluation in each reporting period
      // index==0 is most recent evaluation *before* first reporting period
      PolicyEvaluation[] periods = new PolicyEvaluation[PERIOD_COUNT + 1];
      List<PolicyEvaluation> policyEvaluations = evalLog.allByStage(STAGE_ID);
      for (PolicyEvaluation eval : policyEvaluations) {
        // If the report is not available for whatever reason, ignore this policy evaluation as we're strictly looking
        // at these data points for possible inspection later.
        if (!work.getReportFile(application.getId(), eval.getScanId()).exists()) {
          continue;
        }

        int period = PERIOD_COUNT - ((int) ((now - eval.getTime()) / PERIOD_LENGTH_MS));

        if (period < 0) {
          period = 0;
        }

        if (periods[period] == null) {
          periods[period] = eval;
          // fill evaluation gaps
          for (int i = period + 1; i < periods.length && periods[i] == null; i++) {
            periods[i] = eval;
          }
        }

        if (period <= 0) {
          // log entries are ordered newest first
          // current entry is before reporting period
          // no point in looking at earlier records
          break;
        }
      }

      PolicyEvaluation lastEval = periods[PERIOD_COUNT];
      if (lastEval == null) {
        continue;
      }

      // policy alerts counts
      // skip index==0, which is most recent evaluation *before* reporting period
      for (int period = 1; period < periods.length; period++) {
        PolicyEvaluation eval = periods[period];
        if (eval != null) {
          for (PolicyAlert alert : policyEvaluationUtils.findPolicyAlerts(application.getId(), eval.getScanId())) {
            PolicyFact policyFact = alert.getTrigger();
            for (ComponentFact componentFact : policyFact.getComponentFacts()) {
              String category = determineCategory(policyFact.getPolicyId(), applicationPolicyCategories, componentFact);
              String policyViolationsKey = policyFact.getPolicyId() + ":" + category;
              PolicyViolation violations = policyViolations.get(policyViolationsKey);
              if (violations == null) {
                violations = new PolicyViolation(policyFact.getPolicyName(), category, policyFact.getThreatLevel(),
                    new int[PERIOD_COUNT]);
                policyViolations.put(policyViolationsKey, violations);
              }
              violations.getViolations()[period - 1]++; // ain't perty but works
            }
          }
        }
      }

      // component counts in the latest report
      File evalFile = ReportResource.getReport(work, application.getId(), lastEval.getScanId());
      JsonNode bomNode = JsonUtils.parse(Report.getEntry(evalFile, "bom.json").buf);
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
      for (PolicyAlert alert : policyEvaluationUtils.findPolicyAlerts(application.getId(), lastEval.getScanId())) {
        PolicyFact policyFact = alert.getTrigger();
        int level = policyFact.getThreatLevel();
        List<ComponentFact> componentFacts = policyFact.getComponentFacts();
        int componentCount = componentFacts.size();
        totalAlerts += componentCount;
        int levelIdx = 3; // other
        if (level >= 8) {
          criticalAlerts += componentCount;
          levelIdx = 0;
        }
        else if (level >= 4) {
          severeAlerts += componentCount;
          levelIdx = 1;
        }
        else if (level >= 2) {
          moderateAlerts += componentCount;
          levelIdx = 2;
        }
        for (ComponentFact componentFact : componentFacts) {
          String category = determineCategory(policyFact.getPolicyId(), applicationPolicyCategories, componentFact);
          categories.get(category)[levelIdx]++;
          String g = componentFact.getGroupId(), a = componentFact.getArtifactId(), v = componentFact.getVersion();
          if (g != null && a != null && v != null) {
            List<String> componentKey = Arrays.asList(g, a, v);
            incrementComponentRisk(componentRisks, componentKey, "all", levelIdx);
            incrementComponentRisk(componentRisks, componentKey, category, levelIdx);
          }
        }
      }

      // previous categories
      PolicyEvaluation firstEval = periods[0];
      if (firstEval != null && lastEval.getTime() > firstEval.getTime()) {
        for (PolicyAlert alert : policyEvaluationUtils.findPolicyAlerts(application.getId(), firstEval.getScanId())) {
          PolicyFact policyFact = alert.getTrigger();
          int level = policyFact.getThreatLevel();
          List<ComponentFact> componentFacts = policyFact.getComponentFacts();
          int levelIdx = getThreatLevelIdx(level);
          for (ComponentFact componentFact : componentFacts) {
            previousCategories.get(determineCategory(policyFact.getPolicyId(), applicationPolicyCategories, componentFact))[levelIdx]++;
          }
        }
      }
      applicationRisks.add(new ApplicationRiskSummary(application.getName(), criticalAlerts, severeAlerts,
          moderateAlerts, totalAlerts - criticalAlerts - severeAlerts - moderateAlerts));
    }

    monitor.tick(applications.size(), applications.size());

    return new TrendingReport(meta, toComponentsSummary(components), toApplications(applicationRisks),
        toPolicyViolations(policyViolations), toPartialMatches(partialMatches), toDiffData(categories,
        previousCategories), toTopCategoryComponentRisks(componentRisks),
        toPoliciesSummary(policyViolations.values()));
  }

  private PoliciesSummary toPoliciesSummary(Collection<PolicyViolation> violations) {
    int[] totals = new int[THREAT_LEVELS.length];
    for (PolicyViolation violation : violations) {
      totals[getThreatLevelIdx(violation.getThreat())] += violation.getViolations()[PERIOD_COUNT - 1];
    }
    return new PoliciesSummary(totals[0], totals[1], totals[2], totals[3]);
  }

  private int getThreatLevelIdx(int level) {
    int levelIdx = 3; // other
    if (level >= 8) {
      levelIdx = 0;
    }
    else if (level >= 4) {
      levelIdx = 1;
    }
    else if (level >= 2) {
      levelIdx = 2;
    }
    return levelIdx;
  }

  private void incrementComponentRisk(Map<String, Map<List<String>, int[]>> componentRisks, List<String> componentKey,
      String category, int threatLevelIdx)
  {
    Map<List<String>, int[]> categoryComponentRisks = componentRisks.get(category);
    int[] componentRisk = categoryComponentRisks.get(componentKey);
    if (componentRisk == null) {
      componentRisk = new int[THREAT_LEVELS.length];
      categoryComponentRisks.put(componentKey, componentRisk);
    }
    componentRisk[threatLevelIdx]++;
  }

  private Map<String, List<ComponentRiskSummary>> toTopCategoryComponentRisks(
      Map<String, Map<List<String>, int[]>> componentRisks)
  {
    Map<String, List<ComponentRiskSummary>> result = new LinkedHashMap<String, List<ComponentRiskSummary>>();

    for (String category : componentRisks.keySet()) {
      result.put(category, toTopComponentRisks(componentRisks.get(category)));
    }

    return result;
  }

  private List<ComponentRiskSummary> toTopComponentRisks(Map<List<String>, int[]> categoryComponentRisks) {
    ArrayList<ComponentRiskSummary> result = new ArrayList<ComponentRiskSummary>();
    for (Map.Entry<List<String>, int[]> componentRisk : categoryComponentRisks.entrySet()) {
      List<String> gav = componentRisk.getKey();
      int[] risks = componentRisk.getValue();
      result.add(new ComponentRiskSummary(gav.get(0), gav.get(1), gav.get(2), risks[0], risks[1], risks[2], risks[3]));
    }
    return top(result, COMPONENT_RISKS_COUNT, new Comparator<ComponentRiskSummary>()
    {
      @Override
      public int compare(ComponentRiskSummary o1, ComponentRiskSummary o2) {
        return o2.getRisk() - o1.getRisk();
      }
    });
  }

  private Map<String, List<DiffData>> toDiffData(Map<String, int[]> categories, Map<String, int[]> previousCategories) {
    Map<String, List<DiffData>> diffData = new LinkedHashMap<String, List<DiffData>>();
    for (String category : CATEGORIES) {
      List<DiffData> categoryDiffData = new ArrayList<DiffData>(THREAT_LEVELS.length);
      for (int level = 0; level < THREAT_LEVELS.length; level++) {
        categoryDiffData.add(new DiffData(THREAT_LEVELS[level], categories.get(category)[level], previousCategories
            .get(category)[level]));
      }
      diffData.put(category, categoryDiffData);
    }
    return diffData;
  }

  private ArrayList<PolicyViolation> toPolicyViolations(Map<String, PolicyViolation> policyViolations) {
    return new ArrayList<PolicyViolation>(policyViolations.values());
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

  /**
   * Find and categorize all policies for the given application.
   */
  private Map<String, String> determinePolicyCategories(final Application application) {
    Builder<String,String> builder = new Builder<String, String>();
    PolicyDAO policyDAO = new PolicyDAO(work.getWorkDir());
    List<Policy> policies = policyDAO.getByOwnerId(application.getId());
    policies.addAll(policyDAO.getByOwnerId(application.getOrganizationId()));

    for (Policy policy : policies) {
      Set<String> conditionTypeIds = Sets.newHashSet();
      for (Constraint constraint : policy.getConstraints()) {
        conditionTypeIds.addAll(Lists.transform(constraint.getConditions(), new Function<Condition, String>()
        {
          @Nullable
          @Override
          public String apply(@Nullable final Condition input) {
            return input.getConditionTypeId().toLowerCase(Locale.ENGLISH);
          }
        }));
      }
      builder.put(policy.getId(), determineCategory(conditionTypeIds));
    }
    return builder.build();
  }

  /**
   * Attempt to determine the category based upon the present policy, and if that policy has been
   * deleted already, fall back to examining the violation data.
   */
  private String determineCategory(final String policyId, final Map<String, String> applicationPolicyCategories,
                                   final ComponentFact componentFact)
  {
    String policyCategory = applicationPolicyCategories.get(policyId);
    return policyCategory != null ? policyCategory : getViolationCategoryFromConstraintFacts(
        componentFact.getConstraintFacts());
  }

  private String getViolationCategoryFromConstraintFacts(List<ConstraintFact> constraintFacts) {
    Set<String> conditionTypeIds = Sets.newHashSet();
    for (ConstraintFact constraintFact : constraintFacts) {
        conditionTypeIds.addAll(Lists.transform(constraintFact.getConditionFacts(), new Function<ConditionFact, String>()
        {
          @Nullable
          @Override
          public String apply(@Nullable final ConditionFact input) {
            return input.getConditionTypeId().toLowerCase(Locale.ENGLISH);
          }
        }));
      }
    return determineCategory(conditionTypeIds);
  }

  private String determineCategory(final Set<String> conditionTypeIds) {

    if (Sets.filter(conditionTypeIds, isSecurity).size() > 0) {
      return CATEGORY_SECURITY;
    }
    else if (Sets.filter(conditionTypeIds, isLicense).size() > 0) {
      return CATEGORY_LICENSE;
    }
    else if (Sets.filter(conditionTypeIds, isAge).size() > 0 ||
        Sets.filter(conditionTypeIds, isPopularity).size() > 0) {
      return CATEGORY_QUALITY;
    }
    return CATEGORY_OTHER;
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

  /**
   * Predicate wrapper for finding Strings starting with a given value in a collection.
   */
  private static class StartsWithPredicate implements Predicate<String>{
    private final String startsWith;
    StartsWithPredicate(String startsWith){
      this.startsWith = startsWith;
    }
    @Override
    public boolean apply(@Nullable final String input) {
      return input.startsWith(startsWith);
    }
  }
}
