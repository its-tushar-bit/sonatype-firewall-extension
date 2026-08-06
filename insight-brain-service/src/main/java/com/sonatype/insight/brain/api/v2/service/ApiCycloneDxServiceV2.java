/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.SbomTaxonomy;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeNodeDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.NotAcceptableException;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationHelper;
import com.sonatype.insight.brain.sbom.export.SbomExportUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURL.StandardTypes;
import com.github.packageurl.PackageURLBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.generators.xml.BomXmlGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Evidence;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.Tool;
import org.cyclonedx.model.component.evidence.Occurrence;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.cyclonedx.model.vulnerability.Vulnerability.Affect;
import org.cyclonedx.model.vulnerability.Vulnerability.Analysis;
import org.cyclonedx.model.vulnerability.Vulnerability.Analysis.Justification;
import org.cyclonedx.model.vulnerability.Vulnerability.Analysis.State;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Method;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Severity;
import org.cyclonedx.model.vulnerability.Vulnerability.Source;
import org.cyclonedx.parsers.JsonParser;
import org.cyclonedx.parsers.Parser;
import org.cyclonedx.parsers.XmlParser;
import org.cyclonedx.util.LicenseResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ApiCycloneDxServiceV2
{
  private static final Logger log = LoggerFactory.getLogger(ApiCycloneDxServiceV2.class);

  public static final String NVD = "NVD";

  public static final String CVE = "cve";

  public static final String SONATYPE_NAMESPACE = "sonatype";

  public static final String IQ_APP_PREFIX = "iq_application_";

  public static final Pattern CWE_REGEX = Pattern.compile("(?:cwe-)?(\\d+)", Pattern.CASE_INSENSITIVE);

  // Sentinel hash key used in the components map for synthetic entries created when a dependency
  // tree node has no matching BOM component. The outer map key is the purl (or bom-ref), and the
  // inner map's only entry is this sentinel — the real component hash is unknown.
  static final String SYNTHETIC_COMPONENT_HASH = "synthetic-component-hash";

  private final ApiReportDataServiceV2 apiReportDataServiceV2;

  private final ApplicationHelper applicationHelper;

  private final BaseUrl baseUrl;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final VersionService versionService;

  private static MultiLicenseDAO multiLicenseDAO;

  @Inject
  public ApiCycloneDxServiceV2(
      ApiReportDataServiceV2 apiReportDataServiceV2,
      ApplicationHelper applicationHelper,
      BaseUrl baseUrl,
      PolicyEvaluationDAO policyEvaluationDAO,
      VersionService versionService,
      MultiLicenseDAO multiLicenseDAO)
  {
    this.apiReportDataServiceV2 = apiReportDataServiceV2;
    this.applicationHelper = applicationHelper;
    this.baseUrl = baseUrl;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.versionService = versionService;
    ApiCycloneDxServiceV2.multiLicenseDAO = multiLicenseDAO;
  }

  @Authorize(permission = Permission.READ)
  public Response getByScanId(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String scanId,
      String acceptType,
      Version version)
  {
    Application application = applicationHelper.getApplicationByIdNotNull(applicationId);
    return getByScanIdNoAuthz(application, scanId, acceptType, version, null);
  }

  @Authorize(permission = Permission.READ)
  public Response getLatest(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String stageId,
      String acceptType,
      Version version)
  {
    Application application = applicationHelper.getApplicationByIdNotNull(applicationId);
    return getLatestNoAuthz(application, stageId, acceptType, version);
  }

  @Authorize(permission = Permission.READ)
  public Response getByScanId(
      @AuthzContext(AuthzContext.Key.OWNER) final Owner owner,
      final String scanId,
      final String acceptType,
      final Version version)
  {
    return getByScanIdNoAuthz(owner, scanId, acceptType, version, null);
  }

  @Authorize(permission = Permission.READ)
  public Response getByScanId(
      @AuthzContext(AuthzContext.Key.OWNER) final Owner owner,
      final String scanId,
      final String acceptType,
      final Version version,
      final String linkedSpdxUrl)
  {
    return getByScanIdNoAuthz(owner, scanId, acceptType, version, linkedSpdxUrl);
  }

  @Authorize(permission = Permission.READ)
  public Response getLatest(
      @AuthzContext(AuthzContext.Key.OWNER) final Owner owner,
      final String stageId,
      final String acceptType,
      final Version version)
  {
    return getLatestNoAuthz(owner, stageId, acceptType, version);
  }

  /** Unannotated shared impl so the authz aspect fires only on the public entry point. */
  private Response getByScanIdNoAuthz(
      final Owner owner,
      final String scanId,
      final String acceptType,
      final Version version,
      final String linkedSpdxUrl)
  {
    AuditData.get().setReportId(scanId);
    if (MediaType.APPLICATION_JSON.equals(acceptType) && version.getVersion() < 1.2) {
      throw new NotAcceptableException("CycloneDX json schema does not support versions less than 1.2");
    }
    try {
      Bom bom = buildBom(owner, scanId, version, linkedSpdxUrl);
      return generateResponse(version, owner, acceptType, bom);
    }
    catch (IOException | GeneratorException e) {
      throw new InternalServerException("An error occurred generating report", e);
    }
  }

  private Response getLatestNoAuthz(
      final Owner owner,
      final String stageId,
      final String acceptType,
      final Version version)
  {
    PolicyEvaluation evaluation = policyEvaluationDAO.getLastByOwnerIdAndStageId(owner.getId(), stageId);
    if (evaluation == null) {
      throw new NotFoundException("Unable to locate a scan for " + owner.getId() + " in stage " + stageId);
    }
    return getByScanIdNoAuthz(owner, evaluation.getScanId(), acceptType, version, null);
  }

  /**
   * Builds a CycloneDX {@link Bom} from the given scan's evaluation results.
   * Public to allow cross-package callers (such as
   * {@link com.sonatype.insight.brain.sbom.generation.ScanResultsSbomPersister}) to derive
   * an SBOM for CLI compliance scans without invoking the full HTTP response generator.
   */
  public Bom buildBom(
      Owner owner,
      String scanId,
      Version version,
      String linkedSpdxUrl) throws IOException
  {
    AuditData.get().setReportId(scanId);
    ApiReportRawDataDTOV2 data = apiReportDataServiceV2.getDataNoAuth(owner, scanId);

    Bom bom = new Bom();
    bom.setSerialNumber(toUuid(scanId));

    String publicId = owner.getPublicId();
    String reportPath = publicId != null
        ? UserInterfaceLinksHelper.getReportUrl(publicId, scanId)
        : UserInterfaceLinksHelper.getHostedRepositoryComponentReportUrl(owner.getId(), scanId);
    String url;
    try {
      url = baseUrl.get() + reportPath;
    }
    catch (Exception e) {
      log.debug("Failed to locate baseUrl", e);
      url = reportPath;
    }
    bom.addExternalReference(createExternalReference(url, "IQ Report", ExternalReference.Type.BOM));
    if (linkedSpdxUrl != null) {
      bom.addExternalReference(createExternalReference(linkedSpdxUrl, "SPDX BOM", ExternalReference.Type.BOM));
    }

    Map<String, Map<String, String>> components = createBomComponents(version, data.components, bom);

    // New vulnerability information is available from CycloneDx 1.4
    if (CollectionUtils.isNotEmpty(bom.getComponents()) && version.compareTo(Version.VERSION_14) >= 0) {
      bom.setVulnerabilities(getVulnerabilityInformation(data.components, components, version));
    }

    if (version.compareTo(Version.VERSION_12) >= 0) {
      PolicyEvaluation policyEvaluation =
          policyEvaluationDAO.getLastByOwnerIdAndScanId(owner.getId(), scanId);
      ApiDependencyTreeNodeDTO dependenciesData = apiReportDataServiceV2.getDependencyTreeNoAuth(owner, scanId);
      addMetadata(policyEvaluation, dependenciesData, bom, version, components, data, owner);
      if (hasDependenciesData(components, dependenciesData)) {
        addDependencyTree(dependenciesData, bom, components);
      }
    }

    return bom;
  }

  private boolean hasDependenciesData(
      final Map<String, Map<String, String>> components,
      final ApiDependencyTreeNodeDTO dependenciesData)
  {
    return components.size() > 1 && dependenciesData != null &&
        CollectionUtils.isNotEmpty(dependenciesData.getChildren());
  }

  private Response generateResponse(
      final Version version,
      final Owner owner,
      final String acceptType,
      final Bom bom) throws IOException, GeneratorException
  {
    Parser parser;
    String content;
    MediaType type;
    if (MediaType.APPLICATION_JSON.equals(acceptType)) {
      BomJsonGenerator generator = BomGeneratorFactory.createJson(version, bom);
      content = generator.toJsonString();
      type = MediaType.APPLICATION_JSON_TYPE;
      parser = new JsonParser();
    }
    else {
      BomXmlGenerator generator = BomGeneratorFactory.createXml(version, bom);
      content = generator.toXmlString();
      type = MediaType.APPLICATION_XML_TYPE;
      parser = new XmlParser();
    }

    List<ParseException> exceptions = parser.validate(content.getBytes(), version);

    if (!exceptions.isEmpty()) {
      log.debug("The SBOM generated is not valid, list of errors [{}]",
          StringUtils.join(exceptions.stream().map(Throwable::getMessage).toArray(), ","));
    }
    String filenameBase = owner.getPublicId() != null
        ? owner.getPublicId() + "-bom"
        : "hrc-" + owner.getId() + "-bom";
    return Response.ok(content, type)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            HttpHeaderUtils.buildContentDispositionHeaderValue(
                filenameBase + "." + type.getSubtype()))
        .build();
  }

  private void addDependencyTree(
      ApiDependencyTreeNodeDTO dependenciesData,
      Bom bom,
      Map<String, Map<String, String>> components)
  {
    if (ObjectUtils.allNotNull(dependenciesData, dependenciesData.getPackageUrl())) {
      List<Dependency> dependencies = convert(dependenciesData, bom, components);
      if (!dependencies.isEmpty()) {
        bom.setDependencies(new ArrayList<>(dependencies));
      }
    }
  }

  private void addMetadata(
      final PolicyEvaluation policyEvaluation,
      final ApiDependencyTreeNodeDTO dependenciesData,
      final Bom bom,
      final Version version,
      final Map<String, Map<String, String>> components,
      final ApiReportRawDataDTOV2 data,
      final Owner owner)
  {
    if (policyEvaluation != null) {
      Metadata metadata = new Metadata();

      if (version.compareTo(Version.VERSION_12) > 0) {
        Property scanIdProperty = SbomExportUtils.createCycloneDxProperty(
            "Scan ID", policyEvaluation.getScanId());
        metadata.addProperty(scanIdProperty);

        if (StringUtils.isNotBlank(data.globalInformation.dataVersionDate)) {
          Property dataDate = SbomExportUtils.createCycloneDxProperty(
              "Data Date", data.globalInformation.dataVersionDate);
          metadata.addProperty(dataDate);
        }
      }

      metadata.setTimestamp(policyEvaluation.getTime());
      if (dependenciesData != null) {
        String parentPurl = resolveParentPackageUrl(dependenciesData, policyEvaluation, owner);
        if (parentPurl != null) {
          String parentBomRef = createNewBomRef();
          Component parentComponent = createComponent(parentPurl, Type.APPLICATION, parentBomRef);
          // Including metadata component also in the components list to generate the dependency tree correctly.
          // the fake hash below is not used anywhere, but just to complete the Map.
          components.put(parentPurl, ImmutableMap.of("fake-meta-component-hash", parentBomRef));
          metadata.setComponent(parentComponent);
        }
      }
      addToolVendorInfo(metadata);
      bom.setMetadata(metadata);
    }
  }

  private String resolveParentPackageUrl(
      ApiDependencyTreeNodeDTO dependenciesData,
      PolicyEvaluation policyEvaluation,
      Owner owner)
  {
    if (StringUtils.isNotBlank(dependenciesData.getPackageUrl())) {
      return dependenciesData.getPackageUrl();
    }
    String ownerName = owner.getPublicId() != null
        ? applicationHelper.getApplicationByIdNotNull(policyEvaluation.getOwnerId()).getName()
        : "hrc-" + owner.getId();
    return buildFakeParentPackageUrl(dependenciesData, ownerName, policyEvaluation.getScanId());
  }

  public static String buildFakeParentPackageUrl(
      ApiDependencyTreeNodeDTO dependenciesData,
      String applicationName,
      String scanId)
  {
    // In the case where a dependency tree exists but is missing a parent component we construct a fake parent
    // component so that we can output the dependency tree. The purl will be of the form
    // pkg:generic/sonatype/<appName>@<scanId>
    // Note that we do want not persist this parent purl back in the dependencies.json because
    // it is done here only to get the sbom dependency tree.
    try {
      String purl = PackageURLBuilder.aPackageURL()
          .withType(StandardTypes.GENERIC)
          .withNamespace(SONATYPE_NAMESPACE)
          .withName(IQ_APP_PREFIX + applicationName)
          .withVersion(scanId)
          .build()
          .canonicalize();
      dependenciesData.setPackageUrl(purl);
      return purl;
    }
    catch (MalformedPackageURLException e) {
      log.debug("Unable to construct a fake parent component url from appName:{} and scanId:{}", applicationName,
          scanId);
      return null;
    }
  }

  private void addToolVendorInfo(Metadata metadata) {
    Tool tool = new Tool();
    tool.setVendor("Sonatype Inc.");
    tool.setName("Nexus IQ Server");
    tool.setVersion(versionService.getFullVersion());
    metadata.addTool(tool);
  }

  List<Dependency> convert(ApiDependencyTreeNodeDTO node, Bom bom, Map<String, Map<String, String>> components) {
    Set<Dependency> dependencies = new LinkedHashSet<>();
    convert(dependencies, node, bom, components);
    return new ArrayList<>(dependencies);
  }

  Dependency convert(
      Set<Dependency> dependencies,
      ApiDependencyTreeNodeDTO node,
      Bom bom,
      Map<String, Map<String, String>> components)
  {
    String purl = node.getPackageUrl();
    if (StringUtils.isBlank(purl) && node.getComponentIdentifier() != null) {
      PackageUrlIdentifier purlId =
          PackageUrlIdentifier.fromComponentIdentifier(node.getComponentIdentifier().toComponentIdentifier());
      purl = purlId != null ? purlId.getPackageUrl() : null;
    }
    String componentRef = resolveOrCreateComponentRef(purl, bom, components);
    Dependency dependency = new Dependency(componentRef);
    dependencies.add(dependency);
    if (node.getChildren() != null) {
      for (ApiDependencyTreeNodeDTO childNode : node.getChildren()) {
        Dependency childDependency = convert(dependencies, childNode, bom, components);
        dependency.addDependency(new Dependency(childDependency.getRef()));
      }
    }
    return dependency;
  }

  // CLM-36995: resolves a component's bom-ref from the components map, with fallbacks:
  // 1. Try base purl matching (strip qualifiers) when exact match fails — handles the case
  // where dependencies.json and bom.json use different qualifier formats for the same component
  // 2. If still not found, create a component in the BOM so the dependency tree structure is
  // preserved rather than silently dropping the node and its subtree
  private String resolveOrCreateComponentRef(
      String purl,
      Bom bom,
      Map<String, Map<String, String>> components)
  {
    if (purl != null) {
      // Exact match
      if (components.containsKey(purl)) {
        return resolveComponentRef(purl, components);
      }
      // Fallback: match by base purl (strip qualifiers)
      String basePurl = purl.contains("?") ? purl.substring(0, purl.indexOf('?')) : purl;
      for (String key : components.keySet()) {
        String baseKey = key.contains("?") ? key.substring(0, key.indexOf('?')) : key;
        if (basePurl.equalsIgnoreCase(baseKey)) {
          return resolveComponentRef(key, components);
        }
      }
    }
    // No match found — create a synthetic component so the node is not dropped.
    // When purl is null, bomRef is used as the map key, so each null-purl node gets its own entry
    // (no identity to deduplicate on without a componentIdentifier).
    log.debug("No matching component found for purl '{}', creating component for dependency tree", purl);
    String bomRef = createNewBomRef();
    Component component = purl != null ? createComponent(purl, Component.Type.LIBRARY, bomRef) : null;
    if (component == null) {
      component = new Component();
      component.setType(Component.Type.LIBRARY);
      component.setName("unknown");
      component.setVersion("unknown");
      component.setBomRef(bomRef);
    }
    bom.addComponent(component);
    String key = purl != null ? purl : bomRef;
    Map<String, String> entry = new HashMap<>();
    entry.put(SYNTHETIC_COMPONENT_HASH, component.getBomRef());
    components.put(key, entry);
    return component.getBomRef();
  }

  private String resolveComponentRef(final String packageUrl, final Map<String, Map<String, String>> components) {
    Map<String, String> hashToRefs = components.get(packageUrl);
    // While it may be possible to have multiple components with the same identity in rare circumstances (CLM-24747)
    // with different hashes the dependency tree has to be based on identities. So for the tree we can only pick
    // one of the available values in case there are multiples.
    return hashToRefs.entrySet().iterator().next().getValue();
  }

  // Visible for testing
  List<Vulnerability> getVulnerabilityInformation(
      final List<ApiReportComponentDTOV2> componentInfo,
      final Map<String, Map<String, String>> componentIdentity,
      final Version version)
  {
    Map<String, Vulnerability> vulnerabilities = new HashMap<>();
    for (ApiReportComponentDTOV2 component : componentInfo) {
      if (component.securityData != null &&
          !MatchState.UNKNOWN.getId().equals(component.matchState) &&
          CollectionUtils.isNotEmpty(component.securityData.securityIssues))
      {

        String purl = component.packageUrl;

        Map<String, String> componentIdentityInfo = componentIdentity.get(purl);

        if (MapUtils.isNotEmpty(componentIdentityInfo)) {
          for (ApiSecurityIssueDTO securityIssue : component.securityData.securityIssues) {
            Affect affect = new Affect();
            affect.setRef(componentIdentityInfo.get(component.hash));
            if (!vulnerabilities.containsKey(securityIssue.reference)) {
              createVulnerabilityForSecurityIssue(securityIssue, affect, purl, vulnerabilities, version);
            }
            else {
              vulnerabilities.get(securityIssue.reference).getAffects().add(affect);
            }
          }
        }
        else {
          log.debug("Vulnerability with purl {} does not have a matching component", purl);
        }
      }
    }
    return new ArrayList<>(vulnerabilities.values());
  }

  private void createVulnerabilityForSecurityIssue(
      ApiSecurityIssueDTO securityIssue,
      Affect affect,
      String purl,
      Map<String, Vulnerability> vulnerabilities,
      Version version)
  {
    try {
      Vulnerability vulnerability = new Vulnerability();
      vulnerability.setAffects(Lists.newArrayList(affect));
      vulnerability.setId(securityIssue.reference);

      Rating rating = new Rating();
      rating.setScore(Double.valueOf(securityIssue.severity.toString()));
      rating.setVector(securityIssue.cvssVector);

      Source source = new Source();
      if (CVE.equals(securityIssue.source)) {
        source.setName(NVD);
      }
      else {
        source.setName(securityIssue.source.toUpperCase(Locale.ROOT));
      }
      source.setUrl(securityIssue.url);
      vulnerability.setSource(source);

      setMethod(securityIssue, rating, version);
      setSeverity(securityIssue, rating);

      Source sourceVuln = new Source();
      sourceVuln.setName(source.getName());
      rating.setSource(sourceVuln);
      vulnerability.addRating(rating);

      if (securityIssue.analysis != null) {
        Analysis analysis = new Analysis();
        analysis.setDetail(securityIssue.analysis.detail);
        analysis.setJustification(Justification.fromString(securityIssue.analysis.justification));
        String response = securityIssue.analysis.response;
        if (StringUtils.isNotBlank(response)) {
          analysis.setResponses(
              Arrays.stream(response.split(",")).map(Analysis.Response::fromString).collect(Collectors.toList()));
        }
        analysis.setState(State.fromString(securityIssue.analysis.state));
        vulnerability.setAnalysis(analysis);
      }

      if (StringUtils.isNotBlank(securityIssue.cwe)) {
        String[] cwes = securityIssue.cwe.split(",");
        for (String cwe : cwes) {
          Matcher cweMatcher = CWE_REGEX.matcher(cwe);
          if (cweMatcher.matches()) {
            vulnerability.addCwe(Integer.parseInt(cweMatcher.group(1)));
          }
          else {
            log.debug("Ignoring cwe {} not matching the format {}.", cwe, CWE_REGEX.pattern());
          }
        }
      }
      addReferences(vulnerability, securityIssue);

      vulnerabilities.put(vulnerability.getId(), vulnerability);
    }
    catch (Exception e) {
      log.error("Error creating SBoM Vulnerability for component {} with refId {}", purl, securityIssue.reference, e);
    }
  }

  private static void addReferences(final Vulnerability vulnerability, final ApiSecurityIssueDTO securityIssue) {
    List<Vulnerability.Reference> refs = SbomExportUtils.buildReferencesForVulnerability(
        securityIssue.reference, securityIssue.vulnIds);
    if (refs.isEmpty()) {
      return;
    }
    List<Vulnerability.Reference> existing = vulnerability.getReferences();
    if (CollectionUtils.isNotEmpty(existing)) {
      refs.addAll(0, existing);
    }
    vulnerability.setReferences(refs);
  }

  private void setSeverity(final ApiSecurityIssueDTO securityIssue, final Rating rating) {
    if (StringUtils.isNotBlank(securityIssue.threatCategory)) {
      String severityValue = securityIssue.threatCategory.toLowerCase(Locale.ROOT);
      Severity severity = Severity.fromString(severityValue);
      if (severity != null) {
        rating.setSeverity(severity);
      }
      else {
        switch (severityValue) {
          case "critical":
            rating.setSeverity(Severity.CRITICAL);
            break;
          case "severe":
            rating.setSeverity(Severity.HIGH);
            break;
          case "moderate":
            rating.setSeverity(Severity.MEDIUM);
            break;
          default:
            rating.setSeverity(Severity.UNKNOWN);
        }
      }
    }
    else {
      rating.setSeverity(Severity.UNKNOWN);
    }
  }

  private void setMethod(final ApiSecurityIssueDTO securityIssue, final Rating rating, final Version version) {
    if (StringUtils.isNotBlank(securityIssue.cvssVectorSource)) {
      Method method = Method.fromString(securityIssue.cvssVectorSource);
      if (method != null) {
        rating.setMethod(method);
      }
      else {
        switch (securityIssue.cvssVectorSource.toLowerCase(Locale.ROOT)) {
          case "cve_cvss_2":
            rating.setMethod(Method.CVSSV2);
            break;
          case "cve_cvss_3":
            rating.setMethod(Method.CVSSV3);
            break;
          case "cve_cvss_31":
            rating.setMethod(Method.CVSSV31);
            break;
          case "cve_cvss_4":
            rating.setMethod(Method.CVSSV4);
            break;
          default:
            rating.setMethod(Method.OTHER);
        }
      }

      if (version.compareTo(Version.VERSION_15) < 0 && rating.getMethod().equals(Method.CVSSV4)) {
        rating.setMethod(Method.OTHER);
      }
    }
  }

  private String toUuid(String scanId) {
    if (scanId != null && scanId.length() == 32) {
      return "urn:uuid:" + scanId.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
    }
    return scanId;
  }

  private static Map<String, Map<String, String>> createBomComponents(
      final Version version,
      final List<ApiReportComponentDTOV2> reportComponents,
      final Bom bom)
  {
    // CLM-36995: include all components regardless of match state so the dependency tree
    // structure is preserved in the exported SBOM. Unknown components are valid per the
    // CycloneDX spec — they represent components the tool could not fully identify.
    Map<String, Map<String, String>> components = new HashMap<>();
    for (ApiReportComponentDTOV2 reportComponent : reportComponents) {
      Component component = createLibraryComponent(version, reportComponent, components);
      if (component != null) {
        bom.addComponent(component);
      }
    }
    return components;
  }

  private static ExternalReference createExternalReference(String url, String name, ExternalReference.Type type) {
    ExternalReference reference = new ExternalReference();
    reference.setType(type);
    reference.setUrl(url);
    reference.setComment(name);
    return reference;
  }

  private static Set<License> convert(ApiLicenseDTO apiLicense) {
    return multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(apiLicense.licenseId)
        .stream()
        .map(l -> createLicense(l.getId(), l.getShortDisplayName()))
        .collect(Collectors.toSet());
  }

  private static License createLicense(String id, String name) {
    License license = new License();
    LicenseChoice licenseChoice = LicenseResolver.resolve(id);
    if (licenseChoice == null || CollectionUtils.isEmpty(licenseChoice.getLicenses()) ||
        licenseChoice.getLicenses().get(0) == null)
    {
      // The given id cannot be resolved to an SPDX license, so instead we have to use the name
      license.setName(name);
    }
    else {
      // License Id was resolved successfully meaning that we found a valid SPDX license ID match, therefore
      // we use(trust) the license id value returned by resolver instead of the original value.
      license.setId(licenseChoice.getLicenses().get(0).getId());
    }
    return license;
  }

  private static Component createLibraryComponent(
      final Version version,
      final ApiReportComponentDTOV2 reportComponent,
      final Map<String, Map<String, String>> components)
  {
    try {
      String purl = reportComponent.packageUrl;
      if (StringUtils.isBlank(purl)) {
        if (reportComponent.componentIdentifier == null) {
          log.debug("Skipping BOM component with no packageUrl and no componentIdentifier (hash={}, matchState={})",
              reportComponent.hash, reportComponent.matchState);
          return null;
        }
        PackageUrlIdentifier purlId = PackageUrlIdentifier
            .fromComponentIdentifier(reportComponent.componentIdentifier.toComponentIdentifier());
        if (purlId == null) {
          log.debug("Skipping BOM component; unable to derive packageUrl from componentIdentifier (hash={})",
              reportComponent.hash);
          return null;
        }
        purl = purlId.getPackageUrl();
      }

      String bomRef = createNewBomRef();

      Map<String, String> componentInfo = components.get(purl);

      if (MapUtils.isEmpty(componentInfo)) {
        componentInfo = new HashMap<>();
        componentInfo.put(reportComponent.hash, bomRef);
        components.put(purl, componentInfo);
      }
      else {
        // A component with the same coordinates already exists
        String componentHash = reportComponent.hash;

        // If the component has a different hash, it's a different component
        if (!componentInfo.containsKey(componentHash)) {
          componentInfo.put(componentHash, bomRef);
        }
        else {
          // Component has same hash and same coordinates, it's the same component
          return null;
        }
      }

      Component bomComponent = createComponent(purl, Type.LIBRARY, bomRef);

      if (Objects.nonNull(bomComponent)) {
        bomComponent.setModified(MatchState.SIMILAR.getId().equals(reportComponent.matchState));
        setProperties(version, reportComponent, bomComponent);
        setLicenseInformation(reportComponent, bomComponent);
        setSha256(reportComponent, bomComponent);
        bomComponent.setSwid(reportComponent.swid);
        bomComponent.setCpe(reportComponent.cpe);

        if (version.compareTo(Version.VERSION_15) >= 0) {
          List<Occurrence> occurrences = reportComponent.pathnames
              .stream()
              .map(p -> {
                Occurrence o = new Occurrence();
                o.setLocation(p);
                return o;
              })
              .collect(Collectors.toList());

          Evidence e = new Evidence();
          e.setOccurrences(occurrences);
          bomComponent.setEvidence(e);
        }
      }
      return bomComponent;
    }
    catch (Exception e) {
      log.warn("There was an error creating SBOM component", e);
    }
    return null;
  }

  private static String createNewBomRef() {
    return UUID.randomUUID().toString();
  }

  private static Component createComponent(final String purl, final Type type, String bomRef) {
    try {
      Component bomComponent = new Component();
      bomComponent.setType(type);

      PackageURL packageUrl = new PackageURL(purl);
      bomComponent.setPurl(purl);
      bomComponent.setGroup(packageUrl.getNamespace());
      bomComponent.setName(packageUrl.getName());
      bomComponent.setVersion(packageUrl.getVersion());

      if (StringUtils.isNotBlank(bomRef)) {
        bomComponent.setBomRef(bomRef);
      }
      else {
        bomComponent.setBomRef(purl);
      }
      return bomComponent;
    }
    catch (MalformedPackageURLException e) {
      log.debug("Failed to create PackageURL for {}", purl, e);
    }
    return null;
  }

  private static void setSha256(
      final ApiReportComponentDTOV2 reportComponent,
      final Component bomComponent)
  {
    if (StringUtils.isNotBlank(reportComponent.sha256)) {
      Hash hash = new Hash(Hash.Algorithm.SHA_256, reportComponent.sha256);
      bomComponent.addHash(hash);
    }
  }

  private static void setLicenseInformation(
      final ApiReportComponentDTOV2 reportComponent,
      final Component bomComponent)
  {
    if (reportComponent.licenseData != null) {
      Set<License> licenses = new HashSet<>();
      if (CollectionUtils.isNotEmpty(reportComponent.licenseData.overriddenLicenses)) {
        reportComponent.licenseData.overriddenLicenses.stream()
            .map(ApiCycloneDxServiceV2::convert)
            .forEach(licenses::addAll);
      }
      else if (CollectionUtils.isNotEmpty(reportComponent.licenseData.declaredLicenses)) {
        reportComponent.licenseData.declaredLicenses.stream()
            .map(ApiCycloneDxServiceV2::convert)
            .forEach(licenses::addAll);
        reportComponent.licenseData.observedLicenses.stream()
            .map(ApiCycloneDxServiceV2::convert)
            .forEach(licenses::addAll);
      }
      if (!licenses.isEmpty()) {
        // cyclonedx-core-java 12.2+ normalizes Component.getLicenses() to null when the inner
        // items list is null/empty, so populate the LicenseChoice fully before assigning.
        LicenseChoice licenseChoice = new LicenseChoice();
        licenseChoice.setLicenses(new ArrayList<>(licenses));
        bomComponent.setLicenses(licenseChoice);
      }
    }
  }

  private static void setProperties(
      final Version version,
      final ApiReportComponentDTOV2 reportComponent,
      final Component bomComponent)
  {
    // Properties are only supported for xml/json schema version 1.3+
    if (version.compareTo(Version.VERSION_12) > 0) {
      if (reportComponent.hash != null) {
        bomComponent.setProperties(SbomExportUtils.addOrUpdateBomElementProperty(bomComponent.getProperties(),
            SbomTaxonomy.CDX_SONATYPE_SHA1_PROPERTY_NAME, reportComponent.hash));
      }
      bomComponent.setProperties(SbomExportUtils.addOrUpdateBomElementProperty(bomComponent.getProperties(),
          SbomTaxonomy.CDX_MATCH_STATE_PROPERTY_NAME, reportComponent.matchState));
      bomComponent.setProperties(SbomExportUtils.addOrUpdateBomElementProperty(bomComponent.getProperties(),
          SbomTaxonomy.CDX_IDENTIFICATION_SOURCES_PROPERTY_NAME, reportComponent.identificationSource));
      if (CollectionUtils.isNotEmpty(reportComponent.filenames)) {
        bomComponent.setProperties(SbomExportUtils.addOrUpdateBomElementProperty(bomComponent.getProperties(),
            SbomTaxonomy.CDX_MATCH_FILENAMES_PROPERTY_NAME, StringUtils.join(reportComponent.filenames, ",")));
      }
      // Add the originalPurl property if available (for CycloneDX 1.4+)
      if (version.compareTo(Version.VERSION_13) > 0 && StringUtils.isNotBlank(reportComponent.originalPurl)) {
        bomComponent.setProperties(SbomExportUtils.addOrUpdateBomElementProperty(bomComponent.getProperties(),
            SbomTaxonomy.CDX_ORIGINAL_PURL_PROPERTY_NAME, reportComponent.originalPurl));
      }
    }
  }
}
