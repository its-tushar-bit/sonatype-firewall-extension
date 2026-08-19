/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.ide.IdeMatchedComponent;
import com.sonatype.clm.dto.model.ide.MatchedComponent;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.ide.UserIdePolicyEvaluationDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Timed
@Path(IdeResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.IDE_INTEGRATION)
public class IdeResource
{
  public static final String RESOURCE_PATH = "rest/ide";

  public static final String COORDINATES_SCAN_PATH = "scan/coordinates/{applicationPublicId}";

  private static final Logger log = LoggerFactory.getLogger(IdeResource.class);

  private static final ObjectMapper JSON =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private final HdsClient client;

  private final ApplicationDAO applicationDAO;

  private final HashComponentIdentifierDAO hashComponentIdentifierDAO;

  private final BaseUrl baseUrl;

  private final ComponentPolicyEvaluator componentPolicyEvaluator;

  private final TelemetrySender telemetrySender;

  private final UserIdePolicyEvaluationDAO userIdePolicyEvaluationDao;

  private final CurrentUser currentUser;

  private final Configuration configuration;

  private final ComponentLoaderFactory componentLoaderFactory;

  private final TelemetryUtils telemetryUtils;

  @Inject
  public IdeResource(
      BaseUrl baseUrl,
      HdsClient client,
      ComponentPolicyEvaluator componentPolicyEvaluator,
      TelemetrySender telemetrySender,
      UserIdePolicyEvaluationDAO userIdePolicyEvaluationDao,
      CurrentUser currentUser,
      Configuration configuration,
      ApplicationDAO applicationDAO,
      HashComponentIdentifierDAO hashComponentIdentifierDAO,
      ComponentLoaderFactory componentLoaderFactory,
      TelemetryUtils telemetryUtils)
  {
    this.baseUrl = baseUrl;
    this.client = client;
    this.componentPolicyEvaluator = componentPolicyEvaluator;
    this.telemetrySender = telemetrySender;
    this.userIdePolicyEvaluationDao = userIdePolicyEvaluationDao;
    this.currentUser = currentUser;
    this.configuration = configuration;
    this.componentLoaderFactory = componentLoaderFactory;
    this.applicationDAO = applicationDAO;
    this.hashComponentIdentifierDAO = hashComponentIdentifierDAO;
    this.telemetryUtils = telemetryUtils;
  }

  /**
   * Get the result from a scan request, or a wait delta
   *
   * @param scanType simple or enhanced though we do not enforce that in the Brain
   * @param appPublicId the public application id
   * @return the result of the scan or a wait delta
   * @since 1.2
   */
  @GET
  @Path("scan/{scanType}/{applicationPublicId}/{path:.*}")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  @Audited(AuditEvent.EVALUATE_PROJECT)
  public IdeMatchedComponent doScan(
      @PathParam("scanType") String scanType,
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") String appPublicId,
      @PathParam("path") String path,
      @QueryParam("proprietary") boolean proprietary,
      @Context HttpServletRequest req) throws IOException
  {
    userIdePolicyEvaluationDao.upsert(currentUser.getUsername());

    Application app = applicationDAO.getByPublicIdNotNull(appPublicId);

    MatchedComponent matchedComponent = client.relay(req, MatchedComponent.class, "rest/ide/scan/{scanType}/{path}",
        scanType, path).content;

    return fromMatchedComponent(matchedComponent, app, proprietary, false);
  }

  private IdeMatchedComponent fromMatchedComponent(
      MatchedComponent matchedComponent,
      Application app,
      boolean proprietary,
      boolean forceEvaluation)
  {
    applyManualClaim(matchedComponent, hashComponentIdentifierDAO.getByHash(matchedComponent.getHash()));

    IdeMatchedComponent ideComponent = getComponent(matchedComponent);
    if (needsEvaluation(ideComponent, forceEvaluation)) {
      Component component = componentLoaderFactory.createComponentLoader(app)
          .getComponent(matchedComponent, configuration.isALPObservedLicenseDetectionEnabled());
      component.setProprietary(proprietary);
      List<PolicyAlert> policyAlerts = componentPolicyEvaluator.evaluate(app.getId(), new Stage(DevelopStageType.ID),
          Collections.singletonList(component));
      ideComponent.setAlerts(policyAlerts);
    }
    return ideComponent;
  }

  private void applyManualClaim(MatchedComponent matchedComponent, HashComponentIdentifier hashComponentIdentifier) {
    if (hashComponentIdentifier != null) {
      ComponentIdentifier componentIdentifier = hashComponentIdentifier.getComponentIdentifier();
      matchedComponent.setComponentIdentifier(componentIdentifier);
      matchedComponent.setCatalogDate(hashComponentIdentifier.getCreateTimeLong());
      matchedComponent.setMatchState(MatchState.EXACT.getId());
      matchedComponent.setIdentificationSource(IdentificationSource.MANUAL.getId());
      matchedComponent.setSecurityVulnerabilities(null);
      matchedComponent.setWaitDelta(null);
      matchedComponent.setSimpleMatch(true);
    }
    else {
      matchedComponent.setIdentificationSource(IdentificationSource.SONATYPE.getId());
    }
  }

  private static boolean needsEvaluation(IdeMatchedComponent ideComponent, boolean forceEvaluation) {
    return ideComponent.getWaitDelta() == null
        && (!"unknown".equals(ideComponent.getMatchState()) || !ideComponent.isSimpleMatch() || forceEvaluation);
  }

  /**
   * Get the result from a coordinates-based scan request
   */
  @GET
  @Path(COORDINATES_SCAN_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  @Audited(AuditEvent.EVALUATE_PROJECT)
  public List<IdeMatchedComponent> doCoordinatesScan(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") String appPublicId,
      @QueryParam("componentIdentifier") ComponentIdentifier identifier,
      @QueryParam("proprietary") boolean proprietary,
      @Context HttpServletRequest req) throws IOException
  {
    Application app = applicationDAO.getByPublicIdNotNull(appPublicId);
    Map<String, String> queryParams =
        Collections.singletonMap("componentIdentifier", ComponentIdentifierAdapter.toJson(identifier));

    List<?> list = client.relay(req, List.class, "rest/ide/scan/coordinates", queryParams).content;
    List<MatchedComponent> matchedComponents = JSON.convertValue(list, new TypeReference<List<MatchedComponent>>()
    {
    });

    return fromMatchedComponents(matchedComponents, app, proprietary);
  }

  private List<IdeMatchedComponent> fromMatchedComponents(
      List<MatchedComponent> matchedComponents,
      Application app,
      boolean proprietary)
  {
    final boolean forceEvaluation = true;
    Map<String, HashComponentIdentifier> claimsByHash = getClaimsByHash(matchedComponents);
    ComponentLoader componentLoader = componentLoaderFactory.createComponentLoader(app);
    boolean alpObservedLicenseDetectionEnabled = configuration.isALPObservedLicenseDetectionEnabled();

    List<IdeMatchedComponent> ideComponents = new ArrayList<>(matchedComponents.size());
    List<EvaluationCandidate> candidates = new ArrayList<>();
    for (MatchedComponent matchedComponent : matchedComponents) {
      applyManualClaim(matchedComponent, claimsByHash.get(HashHelper.truncateHash(matchedComponent.getHash())));

      IdeMatchedComponent ideComponent = getComponent(matchedComponent);
      if (needsEvaluation(ideComponent, forceEvaluation)) {
        Component component = componentLoader.getComponent(matchedComponent, alpObservedLicenseDetectionEnabled);
        component.setProprietary(proprietary);
        candidates.add(new EvaluationCandidate(ideComponent, component));
      }
      ideComponents.add(ideComponent);
    }

    if (!candidates.isEmpty()) {
      List<Component> componentsToEvaluate = candidates.stream().map(EvaluationCandidate::component).toList();
      List<PolicyAlert> policyAlerts = componentPolicyEvaluator.evaluate(app.getId(), new Stage(DevelopStageType.ID),
          componentsToEvaluate);
      Map<ComponentKey, List<PolicyAlert>> alertsByComponent = groupAlertsByComponent(policyAlerts);
      for (EvaluationCandidate candidate : candidates) {
        candidate.ideComponent()
            .setAlerts(alertsByComponent.getOrDefault(keyOf(candidate.component()),
                Collections.emptyList()));
      }
    }

    return ideComponents;
  }

  private record EvaluationCandidate(IdeMatchedComponent ideComponent, Component component)
  {
  }

  private Map<String, HashComponentIdentifier> getClaimsByHash(List<MatchedComponent> matchedComponents) {
    List<String> hashes = matchedComponents.stream()
        .map(MatchedComponent::getHash)
        .filter(Objects::nonNull)
        // Truncate before distinct so hashes sharing a stored prefix collapse to one query key. getByHashes
        // truncates again internally, but that is idempotent; the dedup is what this pre-truncation buys.
        .map(HashHelper::truncateHash)
        .distinct()
        .toList();
    if (hashes.isEmpty()) {
      return Collections.emptyMap();
    }
    return hashComponentIdentifierDAO.getByHashes(hashes)
        .stream()
        // Distinct full hashes can truncate to the same stored prefix; keep the first claim, matching the
        // single-component getByHash path which also resolves a truncated hash to one row.
        .collect(Collectors.toMap(HashComponentIdentifier::getHash, Function.identity(),
            (existing, replacement) -> existing));
  }

  // ComponentPolicyEvaluator builds each ComponentFact from the evaluated component's identifier+hash, which is what
  // keyOf reconstructs, so alerts route back to the candidate they were raised for. The evaluator guarantees every
  // alert carries at least one ComponentFact; the guard below only fires if that contract is ever broken, in which
  // case the alert is skipped (and logged) rather than dropped silently or thrown as an NPE.
  static Map<ComponentKey, List<PolicyAlert>> groupAlertsByComponent(List<PolicyAlert> policyAlerts) {
    Map<ComponentKey, List<PolicyAlert>> alertsByComponent = new HashMap<>();
    for (PolicyAlert policyAlert : policyAlerts) {
      PolicyFact trigger = policyAlert.getTrigger();
      List<ComponentFact> componentFacts = trigger == null ? null : trigger.getComponentFacts();
      if (componentFacts == null || componentFacts.isEmpty()) {
        log.warn("Policy alert {} has no component facts; cannot attribute it to a scanned component",
            trigger == null ? null : trigger.getPolicyName());
        continue;
      }
      for (ComponentFact componentFact : componentFacts) {
        ComponentKey key = new ComponentKey(componentFact.getComponentIdentifier(), componentFact.getHash());
        alertsByComponent.computeIfAbsent(key, ignored -> new ArrayList<>()).add(policyAlert);
      }
    }
    return alertsByComponent;
  }

  private static ComponentKey keyOf(Component component) {
    return new ComponentKey(component.getComponentIdentifier(), component.getHash());
  }

  private record ComponentKey(ComponentIdentifier componentIdentifier, String hash)
  {
  }

  /**
   * Submit a scan request, may return the result or a wait delta.
   *
   * @param scanType simple or enhanced though we do not enforce that in the Brain
   * @param applicationPublicId the public applicationId
   * @return the result of the scan or a wait delta
   * @since 1.2
   */
  @POST
  @Path("scan/{scanType}/{applicationPublicId}/{path:.*}")
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_PROJECT)
  public IdeMatchedComponent postScan(
      @PathParam("scanType") String scanType,
      @PathParam("applicationPublicId") String applicationPublicId,
      @PathParam("path") String path,
      @QueryParam("proprietary") boolean proprietary,
      @Context HttpServletRequest req) throws IOException
  {
    return doScan(scanType, applicationPublicId, path, proprietary, req);
  }

  private IdeMatchedComponent getComponent(MatchedComponent mComponent) {
    IdeMatchedComponent ide = new IdeMatchedComponent();
    ide.setComponentIdentifier(mComponent.getComponentIdentifier());
    ide.setHash(mComponent.getHash());
    ide.setMatchState(mComponent.getMatchState());
    ide.setIdentificationSource(mComponent.getIdentificationSource());
    ide.setSimpleMatch(mComponent.isSimpleMatch());
    ide.setWaitDelta(mComponent.getWaitDelta());
    return ide;
  }

  /**
   * Gets the list of available versions for a given GA from the HDS. (e.g. for use by migration wizard)
   *
   * @return the HDS response
   * @since 1.3
   */
  @GET
  @Path("component/versions")
  @Produces(MediaType.APPLICATION_JSON)
  public String[] getVersions(@Context HttpServletRequest req) throws IOException {
    return client.relay(req, String[].class, "rest/ide/artifact/versions").content;
  }

  /**
   * Access a Brain resource
   *
   * @param path the path from the brain root
   * @since 1.3
   */
  @GET
  @Path("brain/{path:.*}")
  public Response brainGet(final @PathParam("path") String path) {
    UriBuilder uriBuilder = baseUrl.redirect().path(path);

    return Response.temporaryRedirect(uriBuilder.build()).build();
  }

  /**
   * Send telemetry data for APPLICATION_EVALUATION_COMPONENT_COUNTS purpose.
   *
   * @param applicationPublicId the public applicationId
   * @param componentCounts the total components by each component type
   * @since 1.136
   */
  @POST
  @Path("telemetry/{applicationPublicId}")
  @Audited(AuditEvent.EVALUATE_PROJECT)
  public void sendTelemetry(
      @PathParam("applicationPublicId") String applicationPublicId,
      Map<String, Long> componentCounts,
      @Context HttpServletRequest req)
  {
    String applicationId = null;
    if (StringUtils.isNotBlank(applicationPublicId)) {
      Application application = applicationDAO.getByPublicId(applicationPublicId);
      applicationId = application != null ? application.getId() : null;
    }
    String userAgent = HdsClient.getClientUserAgent(req);
    String instanceId = HdsClient.getClientInstanceId(req);

    TelemetryData telemetryData = telemetryUtils.buildApplicationEvaluationTelemetryData(
        null,
        applicationId,
        Stage.ID_DEVELOP,
        ScanTriggerType.IDE,
        userAgent,
        instanceId,
        Collections.singletonMap("component_counts", componentCounts));
    telemetrySender.send(telemetryData);
  }

  /**
   * Send telemetry data for APPLICATION_EVALUATION_COMPONENT_COUNTS purpose V2.
   *
   * @param applicationPublicId the public applicationId
   * @param telemetryRequest a map of attributes requested containing the inner map of total components by type
   * @since 1.144
   */
  @POST
  @Path("v2/telemetry/{applicationPublicId}")
  @Audited(AuditEvent.EVALUATE_PROJECT)
  public void sendTelemetryV2(
      @PathParam("applicationPublicId") String applicationPublicId,
      Map<String, Object> telemetryRequest,
      @Context HttpServletRequest req)
  {
    String applicationId = null;
    if (StringUtils.isNotBlank(applicationPublicId)) {
      Application application = applicationDAO.getByPublicId(applicationPublicId);
      applicationId = application != null ? application.getId() : null;
    }
    String userAgent = HdsClient.getClientUserAgent(req);
    String instanceId = HdsClient.getClientInstanceId(req);

    TelemetryData telemetryData = telemetryUtils.buildApplicationEvaluationTelemetryData(
        null,
        applicationId,
        Stage.ID_DEVELOP,
        ScanTriggerType.IDE,
        userAgent,
        instanceId,
        telemetryRequest);
    telemetrySender.send(telemetryData);
  }
}
