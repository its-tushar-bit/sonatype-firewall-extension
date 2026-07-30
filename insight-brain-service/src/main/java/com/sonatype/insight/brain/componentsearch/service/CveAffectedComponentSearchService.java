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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import org.apache.commons.lang3.tuple.Pair;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.componentsearch.dto.ApplicationComponentMatchDTO;
import com.sonatype.insight.brain.componentsearch.dto.ComponentSearchAggregatesDTO;
import com.sonatype.insight.brain.componentsearch.dto.ComponentSearchPageResultDTO;
import com.sonatype.insight.brain.componentsearch.model.ComponentMatchSortField;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.hds.AffectedCoordinates;
import com.sonatype.insight.brain.hds.CveAffectedComponentsService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.StageTypeService;

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

  private final OwnerComponentDAO applicationComponentDAO;

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
      final OwnerComponentDAO applicationComponentDAO,
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
   * @param cveIds the CVE identifiers to search for
   * @param pageNumber one-based page number (first page is 1)
   * @param pageSize number of results per page
   * @param sortBy field to sort by, or null for default multi-field sort (applicationName → componentName → cveId)
   * @param sortOrder sort direction ("asc" or "desc")
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
    Map<String, Set<AffectedCoordinates>> cveToCoordinatesMap =
        cveAffectedComponentsService.fetchAffectedComponentsForMultipleCves(cveIds);
    if (cveToCoordinatesMap.isEmpty()) {
      return Stream.empty();
    }

    Set<AffectedCoordinates> allAffectedCoordinates = cveToCoordinatesMap.values()
        .stream()
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
              allAffectedCoordinates,
              cveToCoordinatesMap);

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
        waivedComponents.size());
  }

  private List<ApplicationComponentMatchDTO> findMatches(final Set<String> cveIds) {
    Map<String, Set<AffectedCoordinates>> cveToCoordinatesMap =
        cveAffectedComponentsService.fetchAffectedComponentsForMultipleCves(cveIds);
    if (cveToCoordinatesMap.isEmpty()) {
      return new ArrayList<>();
    }

    Set<AffectedCoordinates> allAffectedCoordinates = cveToCoordinatesMap.values()
        .stream()
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
            allAffectedCoordinates,
            cveToCoordinatesMap))
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  private Map<String, List<PolicyEvaluation>> getLatestEvaluations(final List<Application> applications) {
    Set<String> ownerIds = applications.stream()
        .map(Application::getId)
        .collect(Collectors.toSet());

    Set<String> relevantStageTypes = stageTypeService
        .getLicensedStageTypes(StageTypeService.DASHBOARD_CONTEXT)
        .stream()
        .map(StageType::getId)
        .collect(Collectors.toSet());

    List<PolicyEvaluation> evaluations =
        policyEvaluationDAO.getLastByOwnerIdsAndStageIds(ownerIds, relevantStageTypes);
    if (evaluations == null) {
      return Map.of();
    }
    return evaluations.stream()
        .collect(Collectors.groupingBy(PolicyEvaluation::getOwnerId));
  }

  private List<ApplicationComponentMatchDTO> processApplication(
      final Application application,
      final List<PolicyEvaluation> evaluations,
      final Set<AffectedCoordinates> affectedCoordinates,
      final Map<String, Set<AffectedCoordinates>> cveToCoordinatesMap)
  {
    if (evaluations == null || evaluations.isEmpty()) {
      return List.of();
    }

    Set<String> appStageIds = evaluations.stream()
        .map(PolicyEvaluation::getStageTypeId)
        .collect(Collectors.toSet());

    Set<String> matchingComponentHashes = new HashSet<>();
    Map<String, List<OwnerComponent>> componentsByStage = new HashMap<>();

    for (PolicyEvaluation evaluation : evaluations) {
      List<OwnerComponent> appComponents = applicationComponentDAO.getByOwnerIdAndStageTypeId(
          application.getId(),
          evaluation.getStageTypeId());

      List<OwnerComponent> matchingComponents = filterMatchingComponents(appComponents, affectedCoordinates);

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
        null);

    List<PolicyViolation> filteredViolations = allAppViolations.stream()
        .filter(v -> matchingComponentHashes.contains(v.getHash()))
        .toList();

    policyViolationDAO.loadConstraintFacts(filteredViolations);

    List<PolicyViolation> cveFilteredViolations = filteredViolations.stream()
        .filter(v -> violationContainsAnyCve(v, cveToCoordinatesMap.keySet()))
        .toList();

    Table<String, String, List<PolicyViolation>> violationsByHashAndCve = buildViolationTable(cveFilteredViolations);

    List<ApplicationComponentMatchDTO> allMatches = new ArrayList<>();

    for (PolicyEvaluation evaluation : evaluations) {
      List<OwnerComponent> matchingComponents = componentsByStage.get(evaluation.getStageTypeId());

      if (matchingComponents == null) {
        continue;
      }

      for (OwnerComponent component : matchingComponents) {
        ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
        if (componentIdentifier == null) {
          continue;
        }

        AffectedCoordinates componentCoords = AffectedCoordinates.fromComponentIdentifier(componentIdentifier);

        for (Map.Entry<String, Set<AffectedCoordinates>> entry : cveToCoordinatesMap.entrySet()) {
          String cveId = entry.getKey();
          Set<AffectedCoordinates> affectedByThisCve = entry.getValue();

          if (affectedByThisCve.contains(componentCoords)) {
            List<PolicyViolation> cveSpecificViolations = violationsByHashAndCve.get(component.getHash(), cveId);
            if (cveSpecificViolations == null) {
              cveSpecificViolations = List.of();
            }

            ApplicationComponentMatchDTO match = dtoBuilder.buildMatch(
                application,
                evaluation,
                component,
                cveId,
                cveSpecificViolations);

            if (match != null) {
              allMatches.add(match);
            }
          }
        }
      }
    }

    return allMatches;
  }

  private List<OwnerComponent> filterMatchingComponents(
      final List<OwnerComponent> components,
      final Set<AffectedCoordinates> affectedCoordinates)
  {
    return components.stream()
        .filter(component -> {
          ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
          if (componentIdentifier == null) {
            return false;
          }
          AffectedCoordinates componentCoords = AffectedCoordinates.fromComponentIdentifier(componentIdentifier);
          return affectedCoordinates.contains(componentCoords);
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
              cveIds.contains(conditionFact.getReference().getValue()))
          {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * @return Table indexed by (componentHash, cveId) containing lists of PolicyViolations
   */
  private Table<String, String, List<PolicyViolation>> buildViolationTable(final List<PolicyViolation> violations) {
    return violations.stream()
        .flatMap(violation -> getCveIds(violation).stream()
            .map(cveId -> Pair.of(violation, cveId)))
        .collect(Tables.toTable(
            pair -> pair.getLeft().getHash(),
            Pair::getRight,
            pair -> List.of(pair.getLeft()),
            (list1, list2) -> Stream.of(list1, list2).flatMap(List::stream).toList(),
            HashBasedTable::create));
  }

  private Set<String> getCveIds(final PolicyViolation violation) {
    List<ConstraintFact> constraintFacts = violation.getConstraintFacts();
    if (constraintFacts == null) {
      return Set.of();
    }

    return constraintFacts.stream()
        .filter(cf -> cf.getConditionFacts() != null)
        .flatMap(cf -> cf.getConditionFacts().stream())
        .filter(cf -> cf.getReference() != null)
        .filter(cf -> TriggerReference.Type.SECURITY_VULNERABILITY_REFID == cf.getReference().getType())
        .map(cf -> cf.getReference().getValue())
        .collect(Collectors.toSet());
  }
}
