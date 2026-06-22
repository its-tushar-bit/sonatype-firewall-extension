/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.component.ComponentDetailsAdapter;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyCompliance;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.PolicyResults;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps {@link ComponentPolicyEvaluator} so Guide self-hosted REST and MCP can enrich
 * component responses with the {@link GuidePolicyCompliance} wire shape. Evaluates against
 * the root organization at the {@code release} stage by default; an override path is
 * available for MCP callers passing an explicit {@code applicationId} / {@code stage}.
 *
 * <p>
 * Soft-fail: any internal exception (HDS unreachable, Drools compile error, missing owner)
 * yields a result map with no entry for the affected PURL — callers see "no policy data"
 * rather than an exception.
 *
 * <p>
 * The returned map is keyed by {@link PackageURL#canonicalize() canonical} PURL form so
 * scoped-npm and similar formats look up consistently regardless of how callers spell the
 * raw input. This is the same canonicalization {@link
 * com.sonatype.insight.brain.guide.api.purl.GuidePurlAssembler} applies on the request
 * side; mismatched canonicalization between insertion and lookup is the regression class
 * fixed on this branch.
 */
@Named
@Singleton
public class GuidePolicyEvaluator
{
  private static final Logger log = LoggerFactory.getLogger(GuidePolicyEvaluator.class);

  private static final Stage DEFAULT_STAGE = new Stage(Stage.ID_RELEASE);

  private final ApiComponentDetailsServiceV2 detailsService;

  private final ComponentDetailsLoaderFactory loaderFactory;

  private final ComponentPolicyEvaluator policyEvaluator;

  private final OwnerDAO ownerDAO;

  private final PolicyDAO policyDAO;

  @Inject
  public GuidePolicyEvaluator(
      ApiComponentDetailsServiceV2 detailsService,
      ComponentDetailsLoaderFactory loaderFactory,
      ComponentPolicyEvaluator policyEvaluator,
      OwnerDAO ownerDAO,
      PolicyDAO policyDAO)
  {
    this.detailsService = detailsService;
    this.loaderFactory = loaderFactory;
    this.policyEvaluator = policyEvaluator;
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
  }

  public Map<String, GuidePolicyCompliance> evaluate(List<String> purls) {
    return evaluate(purls, Organization.ROOT_ORGANIZATION_ID, DEFAULT_STAGE);
  }

  public Map<String, GuidePolicyCompliance> evaluate(List<String> purls, String ownerId, Stage stage) {
    if (purls == null || purls.isEmpty()) {
      return Collections.emptyMap();
    }
    try {
      return evaluateInternal(purls, ownerId, stage);
    }
    catch (RuntimeException e) {
      // Soft-fail: policy enrichment is secondary to the search/detail/vuln/recommendations response,
      // so a failure here drops the policy data rather than failing the whole request. Log the full
      // stack trace at ERROR so a genuine defect surfaces in alerts instead of looking like a routine
      // HDS outage (cf. CLM-38213, where a silent-empty result masked a real failure).
      log.error("Guide policy evaluation failed for {} purls (owner={}, stage={})",
          purls.size(), ownerId, stage.getStageTypeId(), e);
      return Collections.emptyMap();
    }
  }

  private Map<String, GuidePolicyCompliance> evaluateInternal(
      List<String> purls,
      String ownerId,
      Stage stage)
  {
    Owner owner = ownerDAO.getById(ownerId);
    if (owner == null) {
      log.warn("Owner not found for ownerId={} — skipping policy enrichment", ownerId);
      return Collections.emptyMap();
    }

    // 1. Build the batched HDS request.
    //
    // ApiComponentDetailsServiceV2 will call PackageUrlIdentifier.ensureCompleteIdentifier()
    // on each entry during conversion to ComponentEvaluationDataRequest — a check that
    // fires per-format (e.g. npm requires a "type" coordinate, which a bare
    // "pkg:npm/lodash@4.17.21" doesn't carry until the identifier defaults are applied).
    // If a single PURL fails that validation, the whole batch construction throws and we
    // fail every PURL in the batch. Run the same check ourselves up front so a single bad
    // PURL is logged-and-skipped rather than killing the rest.
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    request.components = new ArrayList<>();
    Map<Integer, String> requestIndexToCanonicalPurl = new HashMap<>();
    for (int i = 0; i < purls.size(); i++) {
      String input = purls.get(i);
      String canonical = canonicalize(input);
      if (canonical == null) {
        log.debug("Skipping malformed PURL: {}", input);
        continue;
      }
      try {
        new PackageUrlIdentifier(canonical).ensureCompleteIdentifier();
      }
      catch (RuntimeException e) {
        // InvalidComponentIdentifierException + any other parse-time failure thrown by
        // PackageUrlIdentifier. All are RuntimeExceptions in the current model.
        log.debug("Skipping incomplete PURL {}: {}", canonical, e.getMessage());
        continue;
      }
      ApiComponentDTOV2 dto = new ApiComponentDTOV2();
      dto.packageUrl = canonical;
      request.components.add(dto);
      requestIndexToCanonicalPurl.put(request.components.size() - 1, canonical);
    }
    if (request.components.isEmpty()) {
      return Collections.emptyMap();
    }

    // 2. Single batched HDS call.
    List<ComponentEvaluationData> evaluationDataList = detailsService.getComponentDetailsListFromHds(
        request, ApiComponentDetailsServiceV2.PURPOSE_EVALUATION);
    if (evaluationDataList == null) {
      // Defensive: the HDS client should never return null here, but if it does the soft-fail
      // catch in evaluate(...) would log a generic NPE. A targeted warning makes the cause clear.
      log.warn("HDS returned null component details list for {} purls", request.components.size());
      return Collections.emptyMap();
    }

    // 3. Convert each HDS evaluation result to a Drools-ready Component fact.
    //
    // Canonical reference: ApiComponentEvaluationServiceV2.ComponentEvaluationTask.run()
    // walks the same shape but adds two steps we deliberately skip here:
    // - ComponentDetailsLoader.getComponentDetailsLocally(...) for admin-claimed
    // component overrides — Guide self-hosted only handles public OSS, no claims.
    // - augmentSecurityVulnerabilities(...) for vuln URL enrichment — the current
    // GuidePolicyCompliance wire shape doesn't expose vuln URLs anywhere.
    // If a future story surfaces vuln URLs in the wire shape, mirror that helper here.
    ComponentDetailsLoader loader = loaderFactory.newInstance(owner);
    Map<String, Component> componentByCanonicalPurl = new HashMap<>();
    for (ComponentEvaluationData data : evaluationDataList) {
      NamedComponentDetails details = ComponentDetailsAdapter.convert(data);
      Component component = loader.augmentComponentDetails(details);
      String purl = requestIndexToCanonicalPurl.get(data.requestIndex);
      if (purl != null) {
        componentByCanonicalPurl.put(purl, component);
      }
    }

    if (componentByCanonicalPurl.isEmpty()) {
      return Collections.emptyMap();
    }

    // 4. Single batched Drools eval.
    List<Component> components = new ArrayList<>(componentByCanonicalPurl.values());
    List<Policy> policies = policyDAO.getApplicableByOwnerIdWithHierarchy(ownerId);
    Map<String, Policy> policiesById = policies.stream()
        .collect(Collectors.toMap(Policy::getId, Function.identity()));
    // Pass the policies we already fetched (for policiesById) to the 5-arg overload; the 4-arg
    // overload would re-fetch the same getApplicableByOwnerIdWithHierarchy(ownerId) list internally.
    PolicyResults results = policyEvaluator.evaluate(ownerId, stage, policies, components, false);

    // 5. Pre-resolve OwnerType for every distinct waiver owner so the mapper can produce
    // waiver.scopeOwnerType. Looking the owner up once per id keeps the mapper pure.
    Map<String, OwnerType> ownerTypeByOwnerId = new HashMap<>();
    for (PolicyAlert waived : results.getWaivedAlerts()) {
      ComponentFact cf =
          waived.getTrigger().getComponentFacts().getFirst();
      PolicyWaiver pw = results.getPolicyWaiver(cf);
      if (pw == null || pw.getOwnerId() == null || ownerTypeByOwnerId.containsKey(pw.getOwnerId())) {
        continue;
      }
      Owner waiverOwner = ownerDAO.getById(pw.getOwnerId());
      if (waiverOwner != null) {
        ownerTypeByOwnerId.put(pw.getOwnerId(), waiverOwner.getType());
      }
    }

    // 6. Map per-component, key by canonical PURL.
    Map<String, GuidePolicyCompliance> out = new HashMap<>();
    for (Map.Entry<String, Component> e : componentByCanonicalPurl.entrySet()) {
      out.put(e.getKey(), GuidePolicyComplianceMapper.toCompliance(
          results, e.getValue(), ownerId, stage, policiesById, ownerTypeByOwnerId));
    }
    return out;
  }

  private static String canonicalize(String purl) {
    if (purl == null) {
      return null;
    }
    try {
      return new PackageURL(purl).canonicalize();
    }
    catch (MalformedPackageURLException e) {
      return null;
    }
  }
}
