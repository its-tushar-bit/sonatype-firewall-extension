/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.componentsearch.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.componentsearch.dto.ApplicationComponentMatchDTO;
import com.sonatype.insight.brain.componentsearch.dto.ComponentSearchAggregatesDTO;
import com.sonatype.insight.brain.componentsearch.dto.ComponentSearchPageResultDTO;
import com.sonatype.insight.brain.componentsearch.model.ComponentMatchSortField;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.hds.AffectedComponentDTO;
import com.sonatype.insight.brain.hds.CveAffectedComponentsService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.StageTypeService;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class CveAffectedComponentSearchService
{
  private static final Logger log = LoggerFactory.getLogger(CveAffectedComponentSearchService.class);

  private final CveAffectedComponentsService cveAffectedComponentsService;

  private final ApplicationService applicationService;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final ComponentMatchDtoBuilder dtoBuilder;

  private final ComponentMatchEnrichmentService enrichmentService;

  private final StageTypeService stageTypeService;

  /**
   * Constructs the CVE affected component search service with required dependencies.
   */
  @Inject
  public CveAffectedComponentSearchService(
      final CveAffectedComponentsService cveAffectedComponentsService,
      final ApplicationService applicationService,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final ApplicationComponentDAO applicationComponentDAO,
      final PolicyViolationDAO policyViolationDAO,
      final ComponentMatchDtoBuilder dtoBuilder,
      final ComponentMatchEnrichmentService enrichmentService,
      final StageTypeService stageTypeService)
  {
    this.cveAffectedComponentsService = cveAffectedComponentsService;
    this.applicationService = applicationService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.applicationComponentDAO = applicationComponentDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.dtoBuilder = dtoBuilder;
    this.enrichmentService = enrichmentService;
    this.stageTypeService = stageTypeService;
  }

  /**
   * Searches for applications containing CVE-affected components with pagination and sorting support.
   * Applies multi-level sorting (primary field, then applicationName, then componentName) and
   * enriches results with remediation guidance.
   *
   * @param cveIds     the CVE identifiers to search for
   * @param pageNumber one-based page number (first page is 1)
   * @param pageSize   number of results per page
   * @param sortBy     field to sort by, or null for default multi-field sort (applicationName → componentName → cveId)
   * @param sortOrder  sort direction ("asc" or "desc")
   * @return paginated result containing total count and sorted matches
   */
  public ComponentSearchPageResultDTO searchCveAffectedComponentsPaginated(
      final Set<String> cveIds,
      final int pageNumber,
      final int pageSize,
      final ComponentMatchSortField sortBy,
      final String sortOrder)
  {
    List<ApplicationComponentMatchDTO> allMatches = findMatches(cveIds);

    ComponentSearchAggregatesDTO aggregates = calculateAggregates(allMatches);

    List<ApplicationComponentMatchDTO> sortedMatches = sortMatches(allMatches, sortBy, sortOrder);

    long total = sortedMatches.size();

    int zeroBasedPageNumber = pageNumber - 1;
    List<ApplicationComponentMatchDTO> pageMatches = paginateMatches(sortedMatches, zeroBasedPageNumber, pageSize);

    List<ApplicationComponentMatchDTO> enrichedMatches =
        enrichmentService.enrichWithRemediationBulk(pageMatches, cveIds);

    return new ComponentSearchPageResultDTO(pageNumber, pageSize, total, aggregates, enrichedMatches);
  }

  public Stream<ApplicationComponentMatchDTO> searchCveAffectedComponentsStreaming(final Set<String> cveIds) {
    Map<String, Set<AffectedComponentDTO>> cveToComponentsMap = getAffectedComponentsMap(cveIds);
    if (cveToComponentsMap.isEmpty()) {
      return Stream.empty();
    }

    Set<AffectedComponentDTO> allAffectedComponents = cveToComponentsMap.values().stream()
        .flatMap(Set::stream)
        .collect(Collectors.toSet());

    List<Application> applications = applicationService.getApplications();
    if (applications.isEmpty()) {
      return Stream.empty();
    }

    Map<String, List<PolicyEvaluation>> latestEvaluationsByAppId = getLatestEvaluations(applications);

    Table<String, String, String> remediationCache = HashBasedTable.create();

    return applications.stream()
        .flatMap(application -> {
          List<PolicyEvaluation> evaluations = latestEvaluationsByAppId.get(application.getId());
          if (evaluations == null || evaluations.isEmpty()) {
            return Stream.empty();
          }

          List<ApplicationComponentMatchDTO> appMatches = processApplication(
              application,
              evaluations,
              allAffectedComponents,
              cveToComponentsMap
          );

          return appMatches.stream();
        })
        .map(match -> enrichmentService.enrichMatchWithCache(match, remediationCache, cveIds));
  }

  private ComponentSearchAggregatesDTO calculateAggregates(List<ApplicationComponentMatchDTO> allMatches) {
    Set<String> uniqueAppIds = new HashSet<>();
    Set<String> uniqueComponents = new HashSet<>();
    Set<String> violatingComponents = new HashSet<>();
    Set<String> waivedComponents = new HashSet<>();

    for (ApplicationComponentMatchDTO match : allMatches) {
      uniqueAppIds.add(match.getApplicationInternalId());

      String componentKey = match.getPackageUrl();
      uniqueComponents.add(componentKey);

      if (match.getViolating()) {
        violatingComponents.add(componentKey);
      }
      if (match.getActiveWaiver()) {
        waivedComponents.add(componentKey);
      }
    }

    return new ComponentSearchAggregatesDTO(
        uniqueAppIds.size(),
        uniqueComponents.size(),
        violatingComponents.size(),
        waivedComponents.size()
    );
  }

  private List<ApplicationComponentMatchDTO> findMatches(final Set<String> cveIds) {
    Map<String, Set<AffectedComponentDTO>> cveToComponentsMap = getAffectedComponentsMap(cveIds);
    if (cveToComponentsMap.isEmpty()) {
      return new ArrayList<>();
    }

    Set<AffectedComponentDTO> allAffectedComponents = cveToComponentsMap.values().stream()
        .flatMap(Set::stream)
        .collect(Collectors.toSet());

    List<Application> applications = applicationService.getApplications();
    if (applications.isEmpty()) {
      return new ArrayList<>();
    }

    Map<String, List<PolicyEvaluation>> latestEvaluationsByAppId = getLatestEvaluations(applications);

    return applications.stream()
        .map(application -> processApplication(
            application,
            latestEvaluationsByAppId.get(application.getId()),
            allAffectedComponents,
            cveToComponentsMap))
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  private Map<String, Set<AffectedComponentDTO>> getAffectedComponentsMap(final Set<String> cveIds) {
    Map<String, Set<AffectedComponentDTO>> cveToComponentsMap = new HashMap<>();
    for (String cveId : cveIds) {
      List<AffectedComponentDTO> affectedComponents = cveAffectedComponentsService.getAffectedComponents(cveId);
      if (CollectionUtils.isNotEmpty(affectedComponents)) {
        cveToComponentsMap.put(cveId, Set.copyOf(affectedComponents));
        log.debug("CVE {}: Found {} affected components from HDS", cveId, affectedComponents.size());
        if (log.isTraceEnabled()) {
          affectedComponents.forEach(dto ->
              log.trace("  Affected: format={}, namespace={}, name={}, version={}",
                  dto.format(), dto.namespace(), dto.name(), dto.version()));
        }
      }
    }
    return cveToComponentsMap;
  }

  private Map<String, List<PolicyEvaluation>> getLatestEvaluations(final List<Application> applications) {
    Set<String> applicationIds = applications.stream()
        .map(Application::getId)
        .collect(Collectors.toSet());

    Set<String> relevantStageTypes = stageTypeService
        .getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)
        .stream()
        .map(StageType::getId)
        .collect(Collectors.toSet());

    List<PolicyEvaluation> evaluations =
        policyEvaluationDAO.getLastByApplicationIdsAndStageIds(applicationIds, relevantStageTypes);
    if (evaluations == null) {
      return Map.of();
    }
    return evaluations.stream()
        .collect(Collectors.groupingBy(PolicyEvaluation::getApplicationId));
  }

  private List<ApplicationComponentMatchDTO> processApplication(
      final Application application,
      final List<PolicyEvaluation> evaluations,
      final Set<AffectedComponentDTO> affectedComponents,
      final Map<String, Set<AffectedComponentDTO>> cveToComponentsMap)
  {
    if (evaluations == null || evaluations.isEmpty()) {
      return List.of();
    }

    Set<String> appStageIds = evaluations.stream()
        .map(PolicyEvaluation::getStageTypeId)
        .collect(Collectors.toSet());

    Set<String> matchingComponentHashes = new HashSet<>();
    Map<String, List<ApplicationComponent>> componentsByStage = new HashMap<>();

    for (PolicyEvaluation evaluation : evaluations) {
      List<ApplicationComponent> appComponents = applicationComponentDAO.getByApplicationIdAndStageTypeId(
          application.getId(),
          evaluation.getStageTypeId()
      );

      List<ApplicationComponent> matchingComponents = filterMatchingComponents(appComponents, affectedComponents);

      if (!matchingComponents.isEmpty()) {
        componentsByStage.put(evaluation.getStageTypeId(), matchingComponents);
        matchingComponents.forEach(c -> matchingComponentHashes.add(c.getHash()));
      }
    }

    if (matchingComponentHashes.isEmpty()) {
      return List.of();
    }

    List<PolicyViolation> allAppViolations = policyViolationDAO.getUnfixedBy(
        Set.of(application.getId()),
        appStageIds,
        null,
        null,
        null,
        null,
        null,
        null
    );

    List<PolicyViolation> filteredViolations = allAppViolations.stream()
        .filter(v -> matchingComponentHashes.contains(v.getHash()))
        .toList();

    policyViolationDAO.loadConstraintFacts(filteredViolations);

    List<PolicyViolation> cveFilteredViolations = filteredViolations.stream()
        .filter(v -> violationContainsAnyCve(v, cveToComponentsMap.keySet()))
        .toList();

    Map<String, List<PolicyViolation>> violationsByHash = cveFilteredViolations.stream()
        .collect(Collectors.groupingBy(PolicyViolation::getHash));

    List<ApplicationComponentMatchDTO> allMatches = new ArrayList<>();

    for (PolicyEvaluation evaluation : evaluations) {
      List<ApplicationComponent> matchingComponents = componentsByStage.get(evaluation.getStageTypeId());

      if (matchingComponents == null) {
        continue;
      }

      for (ApplicationComponent component : matchingComponents) {
        ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
        if (componentIdentifier == null) {
          continue;
        }

        AffectedComponentDTO affectedComponent = AffectedComponentDTO.fromComponentIdentifier(componentIdentifier);
        List<PolicyViolation> componentViolations = violationsByHash.getOrDefault(component.getHash(), List.of());

        for (Map.Entry<String, Set<AffectedComponentDTO>> entry : cveToComponentsMap.entrySet()) {
          String cveId = entry.getKey();
          Set<AffectedComponentDTO> affectedByThisCve = entry.getValue();

          if (affectedByThisCve.contains(affectedComponent)) {
            ApplicationComponentMatchDTO match = dtoBuilder.buildMatch(
                application,
                evaluation,
                component,
                cveId,
                componentViolations
            );

            if (match != null) {
              allMatches.add(match);
            }
          }
        }
      }
    }

    return allMatches;
  }

  private List<ApplicationComponent> filterMatchingComponents(
      final List<ApplicationComponent> components,
      final Set<AffectedComponentDTO> affectedComponents)
  {
    return components.stream()
        .filter(component -> {
          ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
          if (componentIdentifier == null) {
            return false;
          }
          AffectedComponentDTO affectedComponent =
              AffectedComponentDTO.fromComponentIdentifier(componentIdentifier);
          return affectedComponents.contains(affectedComponent);
        })
        .toList();
  }

  private List<ApplicationComponentMatchDTO> sortMatches(
      final List<ApplicationComponentMatchDTO> matches,
      final ComponentMatchSortField sortBy,
      final String sortOrder)
  {
    Comparator<ApplicationComponentMatchDTO> comparator;

    if (sortBy == null) {
      comparator = ComponentMatchSortField.APPLICATION_NAME.getComparator()
          .thenComparing(ComponentMatchSortField.COMPONENT_NAME.getComparator())
          .thenComparing(ComponentMatchSortField.CVE_ID.getComparator());
    }
    else {
      comparator = sortBy.getComparator();

      if ("desc".equalsIgnoreCase(sortOrder)) {
        comparator = comparator.reversed();
      }
    }

    matches.sort(comparator);
    return matches;
  }

  private List<ApplicationComponentMatchDTO> paginateMatches(
      final List<ApplicationComponentMatchDTO> matches,
      final int pageNumber,
      final int pageSize)
  {
    int startIndex = pageNumber * pageSize;
    if (startIndex >= matches.size()) {
      return List.of();
    }

    int endIndex = Math.min(startIndex + pageSize, matches.size());
    return matches.subList(startIndex, endIndex);
  }

  private boolean violationContainsAnyCve(final PolicyViolation violation, final Set<String> cveIds) {
    List<ConstraintFact> constraintFacts = violation.getConstraintFacts();
    if (constraintFacts == null) {
      return false;
    }

    for (ConstraintFact constraintFact : constraintFacts) {
      if (constraintFact.getConditionFacts() != null) {
        for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
          if (conditionFact.getReference() != null &&
              TriggerReference.Type.SECURITY_VULNERABILITY_REFID == conditionFact.getReference().getType() &&
              cveIds.contains(conditionFact.getReference().getValue())) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
