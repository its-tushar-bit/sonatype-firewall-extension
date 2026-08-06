/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.fieldmap;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.PolicyWaiverExpiryStatuses;

import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_CATEGORY_COLOR;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_CATEGORY_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_CATEGORY_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_PUBLIC_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_VERSION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_VIOLATION_POLICY_TYPE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_VIOLATION_STAGE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.APPLICATION_VIOLATION_STATE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_ARCHITECTURE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_ARTIFACT_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_CLASSIFIER;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_EXTENSION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_GROUP_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_PACKAGE_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_PLATFORM;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_QUALIFIER;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_COORDINATE_VERSION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_FORMAT;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_HASH;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LABEL_COLOR;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LABEL_DESCRIPTION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LABEL_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LABEL_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LICENSE_THREAT_GROUP_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_LICENSE_THREAT_LEVEL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_MAX_POLICY_THREAT_LEVEL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_VIOLATION_POLICY_TYPE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.COMPONENT_VIOLATION_STATE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.ITEM_TYPE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.PARENT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.PARENT_ORGANIZATION_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_EVALUATION_STAGE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_THREAT_CATEGORY;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_THREAT_LEVEL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_CONSTRAINT_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_POLICY_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_POLICY_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_AUTO;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_EXPIRY_STATUS;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_IS_AUTO;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_EXPIRES_AT_EPOCH_MS;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_POLICY_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_POLICY_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_POLICY_TYPE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_REQUEST_STATUS;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_SCOPE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_TYPE;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.POLICY_WAIVER_WAIVED_BY;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.REJECTION_REASON;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.REQUESTER_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.REVIEWER_NAME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.REVIEW_TIME;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.NOTE_TO_REVIEWER;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.REPORT_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.SBOM_SPECIFICATION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.VULNERABILITY_DESCRIPTION;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.VULNERABILITY_FIRST_SEEN_EPOCH_MS;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.VULNERABILITY_ID;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.VULNERABILITY_SEVERITY;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.VULNERABILITY_STATUS;
import static com.sonatype.insight.brain.search.index.ItemType.APPLICATION;
import static com.sonatype.insight.brain.search.index.ItemType.LEGAL_VIOLATION;
import static com.sonatype.insight.brain.search.index.ItemType.NON_VULNERABLE_COMPONENT;
import static com.sonatype.insight.brain.search.index.ItemType.POLICY;
import static com.sonatype.insight.brain.search.index.ItemType.POLICY_VIOLATION;
import static com.sonatype.insight.brain.search.index.ItemType.POLICY_WAIVER;
import static com.sonatype.insight.brain.search.index.ItemType.POLICY_WAIVER_REQUEST;
import static com.sonatype.insight.brain.search.index.ItemType.SBOM_METADATA;
import static com.sonatype.insight.brain.search.index.ItemType.SECURITY_VULNERABILITY;

/**
 * Immutable case-sensitive registry of every field-name accepted by the global
 * search query language, keyed on the token the user types (e.g. {@code applicationName}).
 * Each entry declares its owning Lucene label, value shape, and the entity
 * types for which it is meaningful.
 */
public final class FieldMap
{
  private static final Set<ItemType> APP_TYPES = EnumSet.of(APPLICATION);

  private static final Set<ItemType> COMPONENT_BEARING_TYPES = EnumSet.of(
      NON_VULNERABLE_COMPONENT,
      POLICY_VIOLATION,
      LEGAL_VIOLATION);

  private static final Set<ItemType> LICENSE_TYPES = EnumSet.of(NON_VULNERABLE_COMPONENT);

  private static final Set<ItemType> POLICY_TYPES = EnumSet.of(
      POLICY,
      POLICY_VIOLATION,
      LEGAL_VIOLATION);

  private static final Set<ItemType> VIOLATION_TYPES = EnumSet.of(
      POLICY_VIOLATION,
      LEGAL_VIOLATION);

  private static final Set<ItemType> VULNERABILITY_TYPES = EnumSet.of(
      SECURITY_VULNERABILITY,
      POLICY_VIOLATION);

  private static final Set<ItemType> ALL_TYPES = EnumSet.allOf(ItemType.class);

  private static final Set<ItemType> ORG_CARRYING_TYPES = EnumSet.of(
      APPLICATION,
      NON_VULNERABLE_COMPONENT,
      POLICY_VIOLATION,
      LEGAL_VIOLATION,
      SBOM_METADATA,
      POLICY,
      // App- and org-scoped waivers both carry parentOrganizationName/Id (full ancestor chain via
      // DocumentBuilderHelper.applyWaiverOwnerHierarchy), so the organizations filter matches both.
      POLICY_WAIVER,
      // Waiver requests share the same owner denormalization as waivers, so the org filter narrows
      // org-scoped requests only, same asymmetry.
      POLICY_WAIVER_REQUEST,
      // SECURITY_VULNERABILITY docs already carry parentOrganization* (set in DocumentBuilderHelper's
      // vuln build path); widening the org filters to this type is purely additive — it enables local
      // vuln filtering by org without changing any other type's compiled query.
      SECURITY_VULNERABILITY);

  // WAIVER_TYPES covers fields present on BOTH manual/auto waivers and waiver requests (policy name/id,
  // threat level, policy type, scope). WAIVER_ONLY_TYPES covers waiver-only fields (waivedBy, auto,
  // expiry) absent on request docs; WAIVER_REQUEST_TYPES covers the request-only fields.
  private static final Set<ItemType> WAIVER_TYPES = EnumSet.of(POLICY_WAIVER, POLICY_WAIVER_REQUEST);

  private static final Set<ItemType> WAIVER_ONLY_TYPES = EnumSet.of(POLICY_WAIVER);

  private static final Set<ItemType> WAIVER_REQUEST_TYPES = EnumSet.of(POLICY_WAIVER_REQUEST);

  // applicationId is set on APPLICATION docs and on app-scoped waiver docs (setOwner(Application)); org-
  // scoped waivers carry no applicationId so an applications filter narrows to app-scoped waivers only.
  // POLICY_WAIVER only ADDS itself as an allowed type; allowedTypes is consulted per-entity-type in
  // QueryCompiler.compileField, so APPLICATION/VIOLATION/VULN queries are unaffected.
  private static final Set<ItemType> APP_ID_AND_WAIVER_TYPES =
      EnumSet.of(APPLICATION, POLICY_WAIVER, POLICY_WAIVER_REQUEST);

  // applicationCategoryName is query-resolvable on APPLICATION, POLICY_VIOLATION and LEGAL_VIOLATION
  // docs (the denormalized app categories, multi-valued). The single-valued APPLICATION_CATEGORY
  // entity doc carries its own category name but is intentionally NOT in this set, so a
  // category-scoped query does not resolve against it (it compiles to MatchNoDocsQuery there).
  // allowedTypes is consumed via .contains(entityType), so listing only these types is additive.
  private static final Set<ItemType> CATEGORY_NAME_TYPES = EnumSet.of(
      APPLICATION,
      POLICY_VIOLATION,
      LEGAL_VIOLATION);

  // policyEvaluationStage is indexed on APPLICATION, violation, SECURITY_VULNERABILITY, and
  // NON_VULNERABLE_COMPONENT docs (setPolicyEvaluationStage in each build path — the component doc
  // build path sets it per app-per-stage). It is set on the policy-evaluation vuln path
  // (buildApplicationStageSVDocs) but is absent on SBOM-sourced vuln docs, which have no evaluation
  // stage — so a vuln-scoped stage filter matches only the policy-evaluation vuln docs. Waivers carry
  // no stage, so this set excludes POLICY_WAIVER. Widening these filters to the vuln and component
  // types enables local vuln/component filtering by app/stage and is purely additive — allowedTypes
  // is consumed via .contains(entityType), so existing types are unaffected.
  private static final Set<ItemType> APP_VIOLATION_AND_VULN_TYPES = EnumSet.of(
      APPLICATION,
      POLICY_VIOLATION,
      LEGAL_VIOLATION,
      SECURITY_VULNERABILITY,
      NON_VULNERABLE_COMPONENT);

  // applicationName is indexed on APPLICATION, violation, SECURITY_VULNERABILITY and
  // NON_VULNERABLE_COMPONENT docs (setOwner / component build path) and on app-scoped waiver docs
  // (setOwner(Application)); org-scoped waivers carry no applicationName so they simply won't match.
  // Union of the app/violation/vuln/component types with POLICY_WAIVER, purely additive.
  private static final Set<ItemType> APP_VIOLATION_VULN_AND_WAIVER_TYPES = EnumSet.of(
      APPLICATION,
      POLICY_VIOLATION,
      LEGAL_VIOLATION,
      SECURITY_VULNERABILITY,
      NON_VULNERABLE_COMPONENT,
      POLICY_WAIVER,
      POLICY_WAIVER_REQUEST);

  private static final Set<ItemType> REPORT_CARRYING_TYPES = EnumSet.of(
      APPLICATION,
      NON_VULNERABLE_COMPONENT,
      POLICY_VIOLATION,
      LEGAL_VIOLATION);

  private static final Set<String> CATEGORY_COLORS = Set.of(
      "light-red", "light-green", "light-blue", "light-purple",
      "dark-red", "dark-green", "dark-blue", "dark-purple",
      "orange", "yellow");

  private static final Set<String> THREAT_CATEGORIES = Set.of("security", "license", "quality", "other");

  // Denormalized on NON_VULNERABLE_COMPONENT docs (Components leg violation filters/sort). Values are
  // lower-cased at index time to match the keyword lowercase normalizer.
  private static final Set<ItemType> COMPONENT_VIOLATION_TYPES = EnumSet.of(NON_VULNERABLE_COMPONENT);

  // Canonical indexed componentViolationState vocabulary (open/waived/legacy — see
  // DocumentBuilderHelper.COMPONENT_VIOLATION_STATE_*). Legacy is a distinct grandfathered-in state.
  private static final Set<String> COMPONENT_VIOLATION_STATES = Set.of("open", "waived", "legacy");

  // Precomputed violation-state set values written on APPLICATION docs (lowercased), distinct from the
  // violation-doc waiver-status vocabulary: here the app's states are already resolved to open/waived/legacy.
  private static final Set<String> APPLICATION_VIOLATION_STATES = Set.of("open", "waived", "legacy");

  // Indexed policyWaiverRequestStatus vocabulary (PolicyWaiverRequestStatus enum names).
  private static final Set<String> WAIVER_REQUEST_STATUSES = Set.of("REQUESTED", "APPROVED", "REJECTED");

  // Indexed policyWaiverScope vocabulary: owner granularity plus component-targeting.
  private static final Set<String> WAIVER_SCOPES = Set.of("application", "organization", "component");

  // Canonical indexed policyViolationWaiverStatus vocabulary written by DocumentBuilderHelper.
  // Active = open (unwaived); Waived = manual waiver; AutoWaived = auto waiver.
  private static final Set<String> WAIVER_STATUSES = Set.of("Active", "Waived", "AutoWaived");

  private static final Set<String> EVALUATION_STAGES = Set.of(
      "proxy", "develop", "source", "build", "stage-release", "release", "operate", "compliance");

  private static final Set<String> VULNERABILITY_STATUSES = Set.of(
      "Open", "Acknowledged", "Not Applicable", "Confirmed");

  private static final Set<String> SBOM_SPECIFICATIONS = Set.of("CycloneDX", "SPDX");

  private static final Set<String> BOOLEAN_VALUES = Set.of("true", "false");

  private static final Set<String> ITEM_TYPE_VALUES = Set.of(
      "APPLICATION", "COMPONENT", "SECURITY_VULNERABILITY", "POLICY_VIOLATION", "LEGAL_VIOLATION", "POLICY",
      "POLICY_WAIVER", "POLICY_WAIVER_REQUEST");

  /**
   * Grammar key for the violation waiver-status chip. Named because the facet-count bridge must identify
   * this chip to keep the waiver-status buckets counting whole-corpus instead of self-restricting; a
   * rename must break compilation on both sides rather than silently change those semantics.
   */
  public static final String KEY_POLICY_VIOLATION_WAIVER_STATUS = "policyViolationWaiverStatus";

  /**
   * Grammar key for the auto-vs-manual waiver chip. Named for the same reason as
   * {@link #KEY_POLICY_VIOLATION_WAIVER_STATUS}: the facet-count bridge drops an explicit manual-only
   * restriction on this key so both buckets count whole-corpus.
   */
  public static final String KEY_POLICY_WAIVER_AUTO = "policyWaiverAuto";

  private final Map<String, FieldEntry> entries;

  private FieldMap(Map<String, FieldEntry> entries) {
    this.entries = Map.copyOf(entries);
  }

  public Optional<FieldEntry> lookup(String fieldName) {
    return Optional.ofNullable(entries.get(fieldName));
  }

  public boolean isKnown(String fieldName) {
    return entries.containsKey(fieldName);
  }

  public Set<String> knownFieldNames() {
    return entries.keySet();
  }

  public static FieldMap defaultMap() {
    return DEFAULT;
  }

  private static final FieldMap DEFAULT = build();

  private static FieldMap build() {
    Map<String, FieldEntry> m = new LinkedHashMap<>();

    // Discriminator
    m.put("itemType", FieldEntry.keyword(ITEM_TYPE.label, ALL_TYPES, ITEM_TYPE_VALUES));

    // Application. applicationName/applicationId include POLICY_WAIVER so app-scoped waivers can be
    // filtered by their owning application; org-scoped waivers (no applicationName/Id) simply won't match.
    // applicationName also covers NON_VULNERABLE_COMPONENT so the catalog can filter components by app.
    m.put("applicationName", FieldEntry.keyword(APPLICATION_NAME.label, APP_VIOLATION_VULN_AND_WAIVER_TYPES));
    m.put("applicationId", FieldEntry.keyword(APPLICATION_ID.label, APP_ID_AND_WAIVER_TYPES));
    m.put("applicationPublicId", FieldEntry.keyword(APPLICATION_PUBLIC_ID.label, APP_TYPES));
    m.put("applicationVersion", FieldEntry.keyword(APPLICATION_VERSION.label, APP_TYPES));
    m.put("applicationCategoryId", FieldEntry.keyword(APPLICATION_CATEGORY_ID.label, APP_TYPES));
    m.put("applicationCategoryName", FieldEntry.keyword(APPLICATION_CATEGORY_NAME.label, CATEGORY_NAME_TYPES));
    // Application evaluation denormalization (indexed on APPLICATION docs).
    m.put("applicationLastEvaluationTimeEpochMs",
        FieldEntry.numericLong(APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label, APP_TYPES));
    // Retrievable/queryable only, not a facet: faceting is opt-in via the FACET_FIELDS maps in
    // IndexQueryService/CatalogService and this count-bearing token ("stage:severity:count") is not
    // listed there, so it is read off the row and never aggregated into buckets.
    m.put("applicationStageSeverityCount",
        FieldEntry.keyword(APPLICATION_STAGE_SEVERITY_COUNT.label, APP_TYPES));
    // Denormalized violation aggregates on APPLICATION docs backing the Applications filter/sort rail.
    // stages/policyTypes/violationStates are precomputed multi-valued keyword sets (TERMS filters);
    // applicationMaxPolicyThreatLevel is the max-threat int (RANGE filter + desc sort). All APP-only.
    m.put("applicationViolationStage",
        FieldEntry.keyword(APPLICATION_VIOLATION_STAGE.label, APP_TYPES, EVALUATION_STAGES));
    m.put("applicationViolationPolicyType",
        FieldEntry.keyword(APPLICATION_VIOLATION_POLICY_TYPE.label, APP_TYPES, THREAT_CATEGORIES));
    m.put("applicationViolationState",
        FieldEntry.keyword(APPLICATION_VIOLATION_STATE.label, APP_TYPES, APPLICATION_VIOLATION_STATES));
    m.put("applicationMaxPolicyThreatLevel",
        FieldEntry.numericInt(APPLICATION_MAX_POLICY_THREAT_LEVEL.label, APP_TYPES));
    m.put("applicationCategoryColor",
        FieldEntry.keyword(APPLICATION_CATEGORY_COLOR.label, APP_TYPES, CATEGORY_COLORS));
    m.put("applicationCategoryDescription",
        FieldEntry.text(APPLICATION_CATEGORY_DESCRIPTION.label, APP_TYPES));
    m.put("sbomSpecification",
        FieldEntry.keyword(SBOM_SPECIFICATION.label, APP_TYPES, SBOM_SPECIFICATIONS));

    // Component
    m.put("componentHash", FieldEntry.keyword(COMPONENT_HASH.label, COMPONENT_BEARING_TYPES));
    m.put("componentName", FieldEntry.keyword(COMPONENT_NAME.label, COMPONENT_BEARING_TYPES));
    m.put("componentFormat", FieldEntry.keyword(COMPONENT_FORMAT.label, COMPONENT_BEARING_TYPES));

    // Component coordinates.
    m.put("componentCoordinateName",
        FieldEntry.keyword(COMPONENT_COORDINATE_NAME.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinateGroupId",
        FieldEntry.keyword(COMPONENT_COORDINATE_GROUP_ID.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinateArtifactId",
        FieldEntry.keyword(COMPONENT_COORDINATE_ARTIFACT_ID.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinateVersion",
        FieldEntry.keyword(COMPONENT_COORDINATE_VERSION.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinateClassifier",
        FieldEntry.keyword(COMPONENT_COORDINATE_CLASSIFIER.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinateExtension",
        FieldEntry.keyword(COMPONENT_COORDINATE_EXTENSION.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinateQualifier",
        FieldEntry.keyword(COMPONENT_COORDINATE_QUALIFIER.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinateArchitecture",
        FieldEntry.keyword(COMPONENT_COORDINATE_ARCHITECTURE.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinatePlatform",
        FieldEntry.keyword(COMPONENT_COORDINATE_PLATFORM.label, COMPONENT_BEARING_TYPES));
    m.put("componentCoordinatePackageId",
        FieldEntry.keyword(COMPONENT_COORDINATE_PACKAGE_ID.label, COMPONENT_BEARING_TYPES));

    // Component labels
    m.put("componentLabelId", FieldEntry.keyword(COMPONENT_LABEL_ID.label, COMPONENT_BEARING_TYPES));
    m.put("componentLabelName",
        FieldEntry.keyword(COMPONENT_LABEL_NAME.label, COMPONENT_BEARING_TYPES));
    m.put("componentLabelColor",
        FieldEntry.keyword(COMPONENT_LABEL_COLOR.label, COMPONENT_BEARING_TYPES, CATEGORY_COLORS));
    m.put("componentLabelDescription",
        FieldEntry.text(COMPONENT_LABEL_DESCRIPTION.label, COMPONENT_BEARING_TYPES));

    // Component violation denormalization (NON_VULNERABLE_COMPONENT docs only), backing the
    // Components leg policyTypes / violationStates / policyThreatLevel filters + policyThreatLevel
    // sort. Values are the same lower-cased vocabularies as their POLICY_VIOLATION counterparts.
    m.put("componentViolationPolicyType",
        FieldEntry.keyword(COMPONENT_VIOLATION_POLICY_TYPE.label, COMPONENT_VIOLATION_TYPES, THREAT_CATEGORIES));
    m.put("componentViolationState",
        FieldEntry.keyword(COMPONENT_VIOLATION_STATE.label, COMPONENT_VIOLATION_TYPES, COMPONENT_VIOLATION_STATES));
    m.put("componentMaxPolicyThreatLevel",
        FieldEntry.numericInt(COMPONENT_MAX_POLICY_THREAT_LEVEL.label, COMPONENT_VIOLATION_TYPES));

    // License
    m.put("componentEffectiveLicenseId",
        FieldEntry.keyword(COMPONENT_EFFECTIVE_LICENSE_ID.label, LICENSE_TYPES));
    m.put("componentEffectiveLicenseName",
        FieldEntry.keyword(COMPONENT_EFFECTIVE_LICENSE_NAME.label, LICENSE_TYPES));
    m.put("componentLicenseThreatGroupName",
        FieldEntry.keyword(COMPONENT_LICENSE_THREAT_GROUP_NAME.label, LICENSE_TYPES));
    m.put("componentLicenseThreatLevel",
        FieldEntry.numericInt(COMPONENT_LICENSE_THREAT_LEVEL.label, LICENSE_TYPES));

    // Organization. Map the user-facing organization filters onto the ancestor-carrying index
    // fields so a query on an org matches the org and every descendant, matching v1 classic search
    // (which rewrites organizationName/Id -> parentOrganizationName/Id). Both are KEYWORD-kind:
    // parentOrganizationName is analyzed by LowerCaseKeywordAnalyzer (KeywordTokenizer, no word
    // splitting), so the whole org name is a single lowercased token that a KEYWORD TermQuery
    // matches exactly — a TEXT phrase query would split "Acme Corp" and miss it. parentOrganizationId
    // is a single-token hex UUID, likewise exact-matched as KEYWORD.
    m.put("organizationId", FieldEntry.keyword(PARENT_ORGANIZATION_ID.label, ORG_CARRYING_TYPES));
    m.put("organizationName", FieldEntry.keyword(PARENT_ORGANIZATION_NAME.label, ORG_CARRYING_TYPES));

    // Policy
    m.put("policyId", FieldEntry.keyword(POLICY_ID.label, POLICY_TYPES));
    m.put("policyName", FieldEntry.keyword(POLICY_NAME.label, POLICY_TYPES));
    m.put("policyThreatCategory",
        FieldEntry.keyword(POLICY_THREAT_CATEGORY.label, POLICY_TYPES, THREAT_CATEGORIES));
    m.put("policyThreatLevel", FieldEntry.numericInt(POLICY_THREAT_LEVEL.label, POLICY_TYPES));

    // Violation
    m.put("policyViolationPolicyName",
        FieldEntry.keyword(POLICY_VIOLATION_POLICY_NAME.label, VIOLATION_TYPES));
    m.put("policyViolationConstraintName",
        FieldEntry.keyword(POLICY_VIOLATION_CONSTRAINT_NAME.label, VIOLATION_TYPES));
    m.put("policyViolationPolicyId",
        FieldEntry.keyword(POLICY_VIOLATION_POLICY_ID.label, VIOLATION_TYPES));
    m.put("policyViolationThreatCategory",
        FieldEntry.keyword(POLICY_VIOLATION_THREAT_CATEGORY.label, VIOLATION_TYPES,
            THREAT_CATEGORIES));
    m.put("policyViolationThreatLevel",
        FieldEntry.numericInt(POLICY_VIOLATION_THREAT_LEVEL.label, VIOLATION_TYPES));
    m.put(KEY_POLICY_VIOLATION_WAIVER_STATUS,
        FieldEntry.keyword(POLICY_VIOLATION_WAIVER_STATUS.label, VIOLATION_TYPES, WAIVER_STATUSES));

    // Policy waiver. policyWaiverThreatLevel is a distinct key from the POLICY policyThreatLevel:
    // waiver docs index the threat level under POLICY_WAIVER_THREAT_LEVEL, not POLICY_THREAT_LEVEL.
    m.put("policyWaiverThreatLevel",
        FieldEntry.numericInt(POLICY_WAIVER_THREAT_LEVEL.label, WAIVER_TYPES));
    m.put("policyWaiverPolicyName", FieldEntry.keyword(POLICY_WAIVER_POLICY_NAME.label, WAIVER_TYPES));
    m.put("policyWaiverPolicyId", FieldEntry.keyword(POLICY_WAIVER_POLICY_ID.label, WAIVER_TYPES));
    // waivedBy/auto are set only on waiver docs, never on request docs, so narrow to WAIVER_ONLY_TYPES.
    m.put("policyWaiverWaivedBy", FieldEntry.keyword(POLICY_WAIVER_WAIVED_BY.label, WAIVER_ONLY_TYPES));
    // Auto-vs-manual discriminator, indexed as the keyword "true"/"false".
    m.put(KEY_POLICY_WAIVER_AUTO, FieldEntry.keyword(POLICY_WAIVER_AUTO.label, WAIVER_ONLY_TYPES, BOOLEAN_VALUES));
    m.put("policyWaiverIsAuto", FieldEntry.keyword(POLICY_WAIVER_IS_AUTO.label, WAIVER_ONLY_TYPES, BOOLEAN_VALUES));
    m.put("policyWaiverExpiryStatus",
        FieldEntry.keyword(POLICY_WAIVER_EXPIRY_STATUS.label, WAIVER_TYPES, PolicyWaiverExpiryStatuses.ALL));
    // Range-queryable epoch-millis expiry (LongPoint) backing the active-vs-expired filter. A doc with
    // no expiry has no point value, so it never matches an expiry range and is treated as active.
    // Both waivers and requests carry an expiry epoch, so WAIVER_TYPES.
    m.put("policyWaiverExpiresAtEpochMs",
        FieldEntry.numericLong(POLICY_WAIVER_EXPIRES_AT_EPOCH_MS.label, WAIVER_TYPES));
    // Denormalized policy threat category on both waiver and request docs; backs the policyType facet/filter.
    m.put("policyWaiverPolicyType",
        FieldEntry.keyword(POLICY_WAIVER_POLICY_TYPE.label, WAIVER_TYPES, THREAT_CATEGORIES));
    // Owner type (APPLICATION/ORGANIZATION) on both waiver and request docs; RBAC/href/display only.
    m.put("policyWaiverScopeOwnerType", FieldEntry.keyword(POLICY_WAIVER_SCOPE_OWNER_TYPE.label, WAIVER_TYPES));
    // Scope granularity (application/organization/component); backs the scope facet + filter.
    m.put("policyWaiverScope", FieldEntry.keyword(POLICY_WAIVER_SCOPE.label, WAIVER_TYPES, WAIVER_SCOPES));
    // Request-only fields (ItemType.POLICY_WAIVER_REQUEST). status backs the waiverStates filter.
    m.put("policyWaiverRequestStatus",
        FieldEntry.keyword(POLICY_WAIVER_REQUEST_STATUS.label, WAIVER_REQUEST_TYPES, WAIVER_REQUEST_STATUSES));
    m.put("requesterName", FieldEntry.keyword(REQUESTER_NAME.label, WAIVER_REQUEST_TYPES));
    m.put("reviewerName", FieldEntry.keyword(REVIEWER_NAME.label, WAIVER_REQUEST_TYPES));
    m.put("reviewTime", FieldEntry.keyword(REVIEW_TIME.label, WAIVER_REQUEST_TYPES));
    m.put("rejectionReason", FieldEntry.text(REJECTION_REASON.label, WAIVER_REQUEST_TYPES));
    m.put("noteToReviewer", FieldEntry.text(NOTE_TO_REVIEWER.label, WAIVER_REQUEST_TYPES));

    // Vulnerability
    m.put("vulnerabilityId", FieldEntry.keyword(VULNERABILITY_ID.label, VULNERABILITY_TYPES));
    m.put("vulnerabilityDescription",
        FieldEntry.text(VULNERABILITY_DESCRIPTION.label, VULNERABILITY_TYPES));
    m.put("vulnerabilityStatus",
        FieldEntry.keyword(VULNERABILITY_STATUS.label, VULNERABILITY_TYPES, VULNERABILITY_STATUSES));
    m.put("vulnerabilitySeverity",
        FieldEntry.numericFloat(VULNERABILITY_SEVERITY.label, VULNERABILITY_TYPES));
    m.put("vulnerabilityFirstSeenEpochMs",
        FieldEntry.numericLong(VULNERABILITY_FIRST_SEEN_EPOCH_MS.label, VULNERABILITY_TYPES));
    m.put("policyEvaluationStage",
        FieldEntry.keyword(POLICY_EVALUATION_STAGE.label, APP_VIOLATION_AND_VULN_TYPES, EVALUATION_STAGES));
    m.put("reportId", FieldEntry.keyword(REPORT_ID.label, REPORT_CARRYING_TYPES));

    return new FieldMap(m);
  }
}
