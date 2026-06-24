/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.google.common.collect.Lists;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.ArtifactCoordinate;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.BOM_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.DEPENDENCIES_JSON;

/**
 * @since 1.13.0
 */
@Named
public class ApiSearchServiceV2
{
  private static final Logger log = LoggerFactory.getLogger(ApiSearchServiceV2.class);

  // Upper bound on the number of applications whose components are fetched in a single batch during a
  // coordinate/wildcard search. A search across a large organization therefore holds at most one batch of components
  // in memory rather than every inspected application's components at once (CLM-40023). Hash searches filter the
  // components in SQL and do not use this batching.
  private static final int COMPONENT_SCAN_BATCH_SIZE = 1000;

  private final BaseUrl baseUrl;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final ReportService reportService;

  private final ComponentLoaderFactory componentLoaderFactory;

  @Inject
  public ApiSearchServiceV2(
      final BaseUrl baseUrl,
      final ApplicationDAO applicationDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final ApplicationComponentDAO applicationComponentDAO,
      final PolicyViolationDAO policyViolationDAO,
      final ReportService reportService,
      final ComponentLoaderFactory componentLoaderFactory)
  {
    this.baseUrl = baseUrl;
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.applicationComponentDAO = applicationComponentDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.reportService = reportService;
    this.componentLoaderFactory = componentLoaderFactory;
  }

  public ApiSearchResultsDTOV2 searchComponent(
      String stageId,
      String hash,
      ComponentIdentifier componentIdentifier,
      String packageUrl)
  {
    AuditData.get().setComponentHash(hash).setComponentIdentifier(componentIdentifier);

    if (StringUtils.isBlank(stageId)) {
      throw new BadRequestException("Stage has not been specified.");
    }
    if (StageTypes.getById(stageId) == null) {
      throw new BadRequestException("Invalid stage: " + stageId + ".");
    }

    ArtifactCoordinate coords = null;
    if (packageUrl != null) {
      ComponentIdentifier purlComponentIdentifier = new PackageUrlIdentifier(packageUrl).toComponentIdentifier();
      AuditData.get().setComponentIdentifier(purlComponentIdentifier);

      purlComponentIdentifier = constructWildcardedComponentIdentifier(purlComponentIdentifier);
      coords = new ArtifactCoordinate(purlComponentIdentifier);
    }
    else if (componentIdentifier != null) {
      componentIdentifier = constructWildcardedComponentIdentifier(componentIdentifier);
      coords = new ArtifactCoordinate(componentIdentifier);
    }
    else if (StringUtils.isBlank(hash)) {
      throw new BadRequestException("Neither hash nor coordinates of component to search for have been specified.");
    }
    if (!StringUtils.isBlank(hash)) {
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
    results.criteria.packageUrl = packageUrl;
    results.criteria.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    String baseUrl = this.baseUrl.get();

    List<Application> apps = getApplicationsWithReadPermission();

    AuditData.get().setData("inspectedApplicationCount", apps.size());

    // Batch-fetch the last primary evaluation per application for the stage, avoiding a per-application query.
    Set<String> appIds = apps.stream().map(Application::getId).collect(Collectors.toSet());
    Map<String, PolicyEvaluation> evalByAppId =
        policyEvaluationDAO.getLastPrimaryByApplicationIdsAndStageId(appIds, stageId);

    // Collect the components matching the search criteria, preserving the original application iteration order and
    // remembering which hashes matched. The component fetch is bounded so a search across a large organization does
    // not materialise every inspected application's components at once (CLM-40023).
    List<MatchedComponent> matches = new ArrayList<>();
    Set<String> matchedHashes = new HashSet<>();
    if (hash != null) {
      collectMatchesByHash(apps, evalByAppId, stageId, hash, coords, matches, matchedHashes);
    }
    else {
      collectMatchesByCoordinates(apps, evalByAppId, stageId, coords, matches, matchedHashes);
    }

    // Batch-fetch the active violations for all matched (app, hash) pairs, grouped for per-component max-threat lookup.
    // This is the cross-product of all matched app ids x all matched hashes, so a wide wildcard search matching many
    // distinct hashes across many apps can fetch (appX, hashY) rows that never matched together. Correctness is
    // unaffected -- the ViolationKey lookup below is exact and unmatched rows are never read -- and the common
    // single-hash search has no blow-up; the over-fetch is an accepted trade-off for eliminating per-component queries.
    // The matchedHashes.isEmpty() guard only triggers when every match has a null hash, which is unreachable in
    // practice (application_component.hash is NOT NULL, so candidateHash is never null); it is kept as a defensive
    // short-circuit so the path is explicit rather than relying on the DAO to no-op on an empty hash set.
    Map<ViolationKey, List<PolicyViolation>> violationsByAppAndHash = (matches.isEmpty() || matchedHashes.isEmpty())
        ? Map.of()
        : policyViolationDAO
            .getActiveByApplicationIdsAndStageIdAndHashes(
                matches.stream().map(m -> m.app().getId()).collect(Collectors.toSet()), stageId, matchedHashes)
            .stream()
            .collect(Collectors.groupingBy(v -> new ViolationKey(v.getApplicationId(), v.getHash())));

    // Per-request cache so each matched application's report is loaded and parsed at most once.
    Map<String, Map<String, Component>> componentsByHashByAppId = new HashMap<>();

    for (MatchedComponent match : matches) {
      Application app = match.app();
      String candidateHash = match.candidateHash();
      String reportHtmlUrl = UserInterfaceLinksHelper.getReportUrl(app.getPublicId(), match.eval().getScanId());

      ApiSearchResultDTOV2 result = new ApiSearchResultDTOV2();
      result.applicationId = app.getPublicId();
      result.applicationName = app.getName();
      result.reportHtmlUrl = reportHtmlUrl;
      result.reportUrl = baseUrl + reportHtmlUrl;
      result.hash = candidateHash;
      result.componentIdentifier =
          ApiComponentIdentifierDTOV2.fromComponentIdentifier(match.componentIdentifier());
      result.packageUrl = PackageUrlIdentifier.toPackageUrl(match.componentIdentifier());
      results.results.add(result);
      result.threatLevel = getMaxThreatLevel(
          violationsByAppAndHash.getOrDefault(new ViolationKey(app.getId(), candidateHash), List.of()));
      result.dependencyData = getApiDependencyDataDTO(app, match.eval(), candidateHash, componentsByHashByAppId);
    }

    AuditData.get().setData("resultRecordCount", results.results.size());

    log.debug("Searched for component with hash={} and componentIdentifier={} in {} ms, got {} hits", hash,
        componentIdentifier, System.currentTimeMillis() - start, results.results.size());

    return results;
  }

  /**
   * Hash search: the {@code (application_id, stage_type_id, hash)} unique constraint guarantees at most one matching
   * component per application, so the hash is filtered in SQL and only the (few) matching rows are loaded rather than
   * every inspected application's full component set.
   */
  private void collectMatchesByHash(
      final List<Application> apps,
      final Map<String, PolicyEvaluation> evalByAppId,
      final String stageId,
      final String hash,
      final ArtifactCoordinate coords,
      final List<MatchedComponent> matches,
      final Set<String> matchedHashes)
  {
    // Query both case variants so matching stays case-insensitive (mirroring the previous in-memory equalsIgnoreCase);
    // a stored hash is single-case hex, so at most one variant exists and the unique constraint still holds.
    Set<String> hashVariants = new HashSet<>();
    hashVariants.add(hash.toLowerCase(Locale.ROOT));
    hashVariants.add(hash.toUpperCase(Locale.ROOT));

    // (application_id, stage_type_id, hash) is unique, so the only way an application yields more than one row here is
    // the (vanishingly unlikely) case of the same hex hash stored under different letter-casing; keep the first to
    // mirror the previous first-match-wins behavior rather than failing the merge.
    Map<String, ApplicationComponent> matchedComponentByAppId = applicationComponentDAO
        .getMapByApplicationIdsAndStageTypeIdsAndHashes(evalByAppId.keySet(), Set.of(stageId), hashVariants)
        .values()
        .stream()
        .collect(Collectors.toMap(ApplicationComponent::getApplicationId, component -> component,
            (existing, replacement) -> existing));

    for (Application app : apps) {
      PolicyEvaluation eval = evalByAppId.get(app.getId());
      if (eval == null) {
        continue;
      }
      ApplicationComponent applicationComponent = matchedComponentByAppId.get(app.getId());
      if (applicationComponent != null) {
        addMatchIfCoordinatesMatch(coords, app, eval, applicationComponent, matches, matchedHashes);
      }
    }
  }

  /**
   * Coordinate/wildcard search: the hash is unknown, so the database cannot pre-filter to matching rows. Components are
   * fetched in application-id batches and filtered in memory, so peak memory is bounded to a single batch rather than
   * every inspected application's components at once.
   */
  private void collectMatchesByCoordinates(
      final List<Application> apps,
      final Map<String, PolicyEvaluation> evalByAppId,
      final String stageId,
      final ArtifactCoordinate coords,
      final List<MatchedComponent> matches,
      final Set<String> matchedHashes)
  {
    List<Application> evaluatedApps = apps.stream().filter(app -> evalByAppId.containsKey(app.getId())).toList();
    for (List<Application> appBatch : Lists.partition(evaluatedApps, COMPONENT_SCAN_BATCH_SIZE)) {
      Set<String> batchAppIds = appBatch.stream().map(Application::getId).collect(Collectors.toSet());
      Map<String, List<ApplicationComponent>> componentsByAppId = applicationComponentDAO
          .getByApplicationIdsAndStageTypeId(batchAppIds, stageId)
          .stream()
          .collect(Collectors.groupingBy(ApplicationComponent::getApplicationId));
      for (Application app : appBatch) {
        PolicyEvaluation eval = evalByAppId.get(app.getId());
        for (ApplicationComponent applicationComponent : componentsByAppId.getOrDefault(app.getId(), List.of())) {
          addMatchIfCoordinatesMatch(coords, app, eval, applicationComponent, matches, matchedHashes);
        }
      }
    }
  }

  private void addMatchIfCoordinatesMatch(
      final ArtifactCoordinate coords,
      final Application app,
      final PolicyEvaluation eval,
      final ApplicationComponent applicationComponent,
      final List<MatchedComponent> matches,
      final Set<String> matchedHashes)
  {
    ComponentIdentifier candidateComponentIdentifier = applicationComponent.getComponentIdentifier();
    if (coords != null && coordsDoesNotMatch(coords, candidateComponentIdentifier)) {
      return;
    }
    String candidateHash = applicationComponent.getHash();
    matches.add(new MatchedComponent(app, eval, candidateHash, candidateComponentIdentifier));
    if (candidateHash != null) {
      matchedHashes.add(candidateHash);
    }
  }

  private ApiDependencyDataDTO getApiDependencyDataDTO(
      final Application app,
      final PolicyEvaluation eval,
      final String candidateHash,
      final Map<String, Map<String, Component>> componentsByHashByAppId)
  {
    if (!SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.isEnabled()) {
      return null;
    }

    ApiDependencyDataDTO dependencyData = new ApiDependencyDataDTO();
    Map<String, Component> componentsByHash =
        componentsByHashByAppId.computeIfAbsent(app.getId(), id -> loadComponentsByHash(app, eval));

    Component component = candidateHash == null ? null : componentsByHash.get(candidateHash);
    if (component != null) {
      dependencyData.innerSourceData = component.getInnerSourceData();
      dependencyData.parentComponentPurls = component.getParentComponentPurls();
      dependencyData.directDependency = component.getDirectDependency();
      dependencyData.innerSource = component.getInnerSource();
    }
    return dependencyData;
  }

  /**
   * Loads and parses the application's report once, returning its components keyed by hash. On any failure (e.g. a
   * missing report) an empty map is returned so the result is cached and the report is not re-fetched for sibling
   * components of the same application.
   */
  private Map<String, Component> loadComponentsByHash(final Application app, final PolicyEvaluation eval) {
    try {
      ApplicationReport applicationReport = reportService.getReport(app.getId(), eval.getScanId());
      final ReportEntry bomReportEntry = applicationReport.getEntry(BOM_JSON.getName());
      final ReportEntry dependenciesReportEntry = applicationReport.getEntry(DEPENDENCIES_JSON.getName());

      if (bomReportEntry != null && dependenciesReportEntry != null) {
        List<Component> components =
            componentLoaderFactory.createComponentLoader(app)
                .getAll(null, null, bomReportEntry.buf, dependenciesReportEntry.buf);

        Map<String, Component> componentsByHash = new HashMap<>(components.size());
        for (Component component : components) {
          // Preserve the original first-match-wins behavior when multiple components share a hash.
          componentsByHash.putIfAbsent(component.getHash(), component);
        }
        return componentsByHash;
      }
    }
    catch (Exception e) {
      log.warn("Dependency data is incomplete for report id {}.", eval.getScanId(), e);
    }
    return Map.of();
  }

  /**
   * A component (from an application's evaluation) that matched the search criteria, paired with the application and
   * its last primary evaluation. Captured up-front so violation and dependency-data lookups can be batched.
   */
  private record MatchedComponent(
      Application app,
      PolicyEvaluation eval,
      String candidateHash,
      ComponentIdentifier componentIdentifier)
  {
  }

  /**
   * Key for grouping active violations by application and component hash.
   */
  private record ViolationKey(String applicationId, String hash)
  {
  }

  private boolean coordsDoesNotMatch(
      final ArtifactCoordinate coords,
      final ComponentIdentifier candidateComponentIdentifier)
  {
    if (candidateComponentIdentifier == null) {
      return true;
    }

    return !coords.matches(candidateComponentIdentifier);
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
            StringUtils.isBlank(coordinateValue) ? ArtifactCoordinate.PLACEHOLDER : coordinateValue);
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
