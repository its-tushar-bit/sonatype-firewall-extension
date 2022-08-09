/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;

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
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationHelper;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.util.SbomUtils;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.generators.xml.BomXmlGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.cyclonedx.model.vulnerability.Vulnerability.Affect;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Method;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Severity;
import org.cyclonedx.model.vulnerability.Vulnerability.Source;
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

  private final ApiReportDataServiceV2 apiReportDataServiceV2;

  private final ApplicationHelper applicationHelper;

  private final BaseUrl baseUrl;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  public ApiCycloneDxServiceV2(
      ApiReportDataServiceV2 apiReportDataServiceV2,
      ApplicationHelper applicationHelper,
      BaseUrl baseUrl,
      PolicyEvaluationDAO policyEvaluationDAO)
  {
    this.apiReportDataServiceV2 = apiReportDataServiceV2;
    this.applicationHelper = applicationHelper;
    this.baseUrl = baseUrl;
    this.policyEvaluationDAO = policyEvaluationDAO;
  }

  @Authorize(permission = Permission.READ)
  public Response getByScanId(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String scanId,
      String acceptType,
      Version version)
  {
    Application application = applicationHelper.getApplicationByIdNotNull(applicationId);

    return getByScanId(application, scanId, acceptType, version);
  }

  @Authorize(permission = Permission.READ)
  public Response getLatest(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String stageId,
      String acceptType,
      Version version)
  {
    Application application = applicationHelper.getApplicationByIdNotNull(applicationId);
    PolicyEvaluation evaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(), stageId);
    if (evaluation == null) {
      throw new NotFoundException("Unable to locate a scan for " + applicationId + " in stage " + stageId);
    }

    return getByScanId(application, evaluation.getScanId(), acceptType, version);
  }

  private Response getByScanId(Application application, String scanId, String acceptType, Version version) {
    AuditData.get().setReportId(scanId);
    if (MediaType.APPLICATION_JSON.equals(acceptType) && version.getVersion() < 1.2) {
      throw new NotAcceptableException("CycloneDX json schema does not support versions less than 1.2");
    }

    try {
      ApiReportRawDataDTOV2 data = apiReportDataServiceV2.getDataNoAuth(application.getPublicId(), scanId);

      Bom bom = new Bom();
      bom.setSerialNumber(toUuid(scanId));
      if (version.getVersion() >= 1.2) {
        PolicyEvaluation policyEvaluation =
            policyEvaluationDAO.getLastByApplicationIdAndScanId(application.getId(), scanId);
        if (policyEvaluation != null) {
          Metadata metadata = new Metadata();
          metadata.setTimestamp(policyEvaluation.getTime());
          bom.setMetadata(metadata);
        }
      }

      String url;
      try {
        url = baseUrl.get() + UserInterfaceLinksHelper.getReportUrl(application.getPublicId(), scanId);
      }
      catch (Exception e) {
        log.debug("Failed to locate baseUrl", e);
        url = UserInterfaceLinksHelper.getReportUrl(application.getPublicId(), scanId);
      }
      bom.addExternalReference(createExternalReference(url, "IQ Report", ExternalReference.Type.BOM));

      List<String> components = createBomComponents(version, data.components, bom);

      //New vulnerability information is available from SBoM 1.4
      if (CollectionUtils.isNotEmpty(bom.getComponents()) &&  version.getVersion() >= 1.4) {
        bom.setVulnerabilities(getVulnerabilityInformation(data.components, components));
      }

      if (MediaType.APPLICATION_JSON.equals(acceptType)) {
        BomJsonGenerator generator = BomGeneratorFactory.createJson(version, bom);
        return Response.ok(generator.toJsonString(), MediaType.APPLICATION_JSON)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                HttpHeaderUtils.buildContentDispositionHeaderValue(application.getPublicId() + '-' + scanId + ".json"))
            .build();
      }
      BomXmlGenerator generator = BomGeneratorFactory.createXml(version, bom);
      generator.generate();
      return Response.ok(generator.toXmlString(), MediaType.APPLICATION_XML)
          .header(HttpHeaders.CONTENT_DISPOSITION,
              HttpHeaderUtils.buildContentDispositionHeaderValue(application.getPublicId() + '-' + scanId + ".xml"))
          .build();
    }
    catch (IOException | ParserConfigurationException | GeneratorException e) {
      throw new InternalServerException("An error occurred generating report", e);
    }
  }

  //Visible for testing
  List<Vulnerability> getVulnerabilityInformation(
      final List<ApiReportComponentDTOV2> componentInfo,
      final List<String> componentPurls)
  {
    Map<String, Vulnerability> vulnerabilities = new HashMap<>();
    for (ApiReportComponentDTOV2 component : componentInfo) {
      if (component.securityData != null &&
          !MatchState.UNKNOWN.getId().equals(component.matchState) &&
          CollectionUtils.isNotEmpty(component.securityData.securityIssues)) {

        String purl = component.packageUrl;
        if (componentPurls.contains(purl)) {
          Affect affect = new Affect();
          affect.setRef(purl);

          for (ApiSecurityIssueDTO securityIssue : component.securityData.securityIssues) {
            if (!vulnerabilities.containsKey(securityIssue.reference)) {
              createVulnerabilityForSecurityIssue(securityIssue, affect, purl, vulnerabilities);
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
      Map<String, Vulnerability> vulnerabilities)
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

      setMethod(securityIssue, rating);
      setSeverity(securityIssue, rating);

      Source sourceVuln = new Source();
      sourceVuln.setName(source.getName());
      rating.setSource(sourceVuln);
      vulnerability.addRating(rating);

      if (StringUtils.isNotBlank(securityIssue.cwe)) {
        String[] cwes = securityIssue.cwe.split(",");
        for (String cwe : cwes) {
          vulnerability.addCwe(Integer.parseInt(cwe));
        }
      }
      vulnerabilities.put(vulnerability.getId(), vulnerability);
    }
    catch (Exception e) {
      log.error("Error creating SBoM Vulnerability for component {} with refId", purl, securityIssue.reference, e);
    }
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

  private void setMethod(final ApiSecurityIssueDTO securityIssue, final Rating rating) {
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
          default:
            rating.setMethod(Method.OTHER);
        }
      }
    }
  }

  private String toUuid(String scanId) {
    if (scanId != null && scanId.length() == 32) {
      return "urn:uuid:" + scanId.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
    }
    return scanId;
  }

  private static List<String> createBomComponents(
      final Version version,
      final List<ApiReportComponentDTOV2> reportComponents,
      final Bom bom)
  {
    List<String> components = new ArrayList<>();
    for (ApiReportComponentDTOV2 reportComponent : reportComponents) {
      if (!MatchState.UNKNOWN.getId().equals(reportComponent.matchState)) {
        Component component = createComponent(version, reportComponent, components);
        if (component != null) {
          bom.addComponent(component);
        }
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
    return new MultiLicenseDAO().getLicensesByMultiLicenseIdNotNull(apiLicense.licenseId).stream()
        .map(l -> createLicense(l.getId(), l.getShortDisplayName())).collect(Collectors.toSet());
  }

  private static License createLicense(String id, String name) {
    License license = new License();
    LicenseChoice licenseChoice = LicenseResolver.resolve(id);
    if (licenseChoice == null || CollectionUtils.isEmpty(licenseChoice.getLicenses()) ||
        licenseChoice.getLicenses().get(0) == null) {
      // The given id cannot be resolved to an SPDX license, so instead we have to use the name
      license.setName(name);
    }
    else {
      license.setId(id);
    }
    return license;
  }

  private static Component createComponent(
      final Version version,
      final ApiReportComponentDTOV2 reportComponent,
      final List<String> components)
  {
    try {
      Component bomComponent = new Component();
      bomComponent.setType(Type.LIBRARY);

      PackageUrlIdentifier purl;
      if (StringUtils.isNotBlank(reportComponent.packageUrl)) {
        purl = new PackageUrlIdentifier(reportComponent.packageUrl);
      }
      else {
        purl =
            PackageUrlIdentifier.fromComponentIdentifier(reportComponent.componentIdentifier.toComponentIdentifier());
      }

      PackageURL packageUrl = new PackageURL(purl.getPackageUrl());
      bomComponent.setPurl(packageUrl);
      bomComponent.setGroup(packageUrl.getNamespace());
      bomComponent.setName(packageUrl.getName());
      bomComponent.setVersion(packageUrl.getVersion());

      bomComponent.setBomRef(purl.getPackageUrl());
      components.add(purl.getPackageUrl());

      bomComponent.setModified(MatchState.SIMILAR.getId().equals(reportComponent.matchState));
      setProperties(version, reportComponent, bomComponent);
      setLicenseInformation(reportComponent, bomComponent);

      return bomComponent;
    }
    catch (MalformedPackageURLException e) {
      log.debug("Failed to create PackageURL for {}", reportComponent.packageUrl, e);
    }
    catch (Exception e) {
      log.warn("There was an error creating SBoM component", e);
    }
    return null;
  }

  private static void setLicenseInformation(
      final ApiReportComponentDTOV2 reportComponent,
      final Component bomComponent)
  {
    if (reportComponent.licenseData != null) {
      Set<License> licenses = new HashSet<>();
      if (reportComponent.licenseData.overriddenLicenses != null
          && !reportComponent.licenseData.overriddenLicenses.isEmpty()) {
        reportComponent.licenseData.overriddenLicenses.stream().map(ApiCycloneDxServiceV2::convert)
            .forEach(licenses::addAll);
      }
      else if (reportComponent.licenseData.declaredLicenses != null) {
        reportComponent.licenseData.declaredLicenses.stream().map(ApiCycloneDxServiceV2::convert)
            .forEach(licenses::addAll);
        reportComponent.licenseData.observedLicenses.stream().map(ApiCycloneDxServiceV2::convert)
            .forEach(licenses::addAll);
      }
      bomComponent.setLicenseChoice(new LicenseChoice());
      bomComponent.getLicenseChoice().setLicenses(new ArrayList<>(licenses));
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
        addProperty(SbomUtils.SONATYPE_HASH_PROPERTY_NAME, reportComponent.hash, bomComponent);
      }
      addProperty("Match State", reportComponent.matchState, bomComponent);
      addProperty(SbomUtils.IDENTIFICATION_SOURCE_PROPERTY_NAME, reportComponent.identificationSource, bomComponent);
    }
  }

  private static void addProperty(final String name, final String value, final Component component) {
    Property property = new Property();
    property.setName(name);
    property.setValue(value);
    component.addProperty(property);
  }
}
