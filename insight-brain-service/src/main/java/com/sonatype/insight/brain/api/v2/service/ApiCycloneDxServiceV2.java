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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.util.SbomUtils;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import org.apache.shiro.util.CollectionUtils;
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
import org.cyclonedx.util.LicenseResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ApiCycloneDxServiceV2
{
  private static final Logger log = LoggerFactory.getLogger(ApiCycloneDxServiceV2.class);

  private ApiReportDataServiceV2 apiReportDataServiceV2;

  private ApplicationHelper applicationHelper;

  private BaseUrl baseUrl;

  private PolicyEvaluationDAO policyEvaluationDAO;

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

      createBomComponents(version,
          data.components.stream().filter(c -> !MatchState.UNKNOWN.getId().equals(c.matchState))
              .collect(Collectors.toList())).forEach(bom::addComponent);

      if (MediaType.APPLICATION_JSON.equals(acceptType)) {
        BomJsonGenerator generator = BomGeneratorFactory.createJson(version, bom);
        return Response.ok(generator.toJsonString(), MediaType.APPLICATION_JSON)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "Content-Disposition: attachment; filename=\"" + application.getPublicId() + '-' + scanId + ".json\"")
            .build();
      }
      BomXmlGenerator generator = BomGeneratorFactory.createXml(version, bom);
      generator.generate();
      return Response.ok(generator.toXmlString(), MediaType.APPLICATION_XML)
          .header(HttpHeaders.CONTENT_DISPOSITION,
              "Content-Disposition: attachment; filename=\"" + application.getPublicId() + '-' + scanId + ".xml\"")
          .build();
    }
    catch (IOException | ParserConfigurationException | GeneratorException e) {
      throw new InternalServerException("An error occurred generating report", e);
    }
  }

  private String toUuid(String scanId) {
    if (scanId != null && scanId.length() == 32) {
      return "urn:uuid:" + scanId.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
    }
    return scanId;
  }

  private static List<Component> createBomComponents(Version version, List<ApiReportComponentDTOV2> reportComponents) {
    Map<String, ApiReportComponentDTOV2> pathToComponent = new HashMap<>();

    reportComponents
        .forEach(component -> component.pathnames.forEach(path -> pathToComponent.put(path, component)));

    LinkedHashMap<ApiReportComponentDTOV2, Set<ApiReportComponentDTOV2>> childToParents =
        calculateParents(pathToComponent);

    Map<ApiReportComponentDTOV2, Component> converted = new HashMap<>();

    reportComponents.forEach(component -> converted.put(component, createComponent(version, component)));

    childToParents.forEach((child, parents) ->
        parents.stream().filter(Objects::nonNull)
            .forEach(parent -> converted.get(parent).addComponent(converted.get(child))
            ));

    return childToParents.entrySet().stream().filter(e -> e.getValue().contains(null))
        .map(e -> converted.get(e.getKey())).collect(Collectors.toList());
  }

  private static LinkedHashMap<ApiReportComponentDTOV2, Set<ApiReportComponentDTOV2>> calculateParents(
      Map<String, ApiReportComponentDTOV2> pathToComponent)
  {
    LinkedHashMap<ApiReportComponentDTOV2, Set<ApiReportComponentDTOV2>> childToParent = new LinkedHashMap<>();
    // we sort the list so parents are processed first
    pathToComponent.keySet().stream().sorted().forEach(path -> {
      if (path == null) {
        return;
      }

      LinkedList<String> pathSegments = new LinkedList<>(Arrays.asList(path.split("/")));

      while (!pathSegments.isEmpty()) {
        pathSegments.removeLast();

        String parentCandidate = pathSegments.stream().collect(Collectors.joining("/"));
        if (pathToComponent.containsKey(parentCandidate)) {
          ApiReportComponentDTOV2 parent = pathToComponent.get(parentCandidate);
          ApiReportComponentDTOV2 child = pathToComponent.get(path);

          if (parent != child) {
            childToParent.computeIfAbsent(child, c -> new HashSet<>()).add(parent);
            return;
          }
        }
      }
      // no parent
      childToParent.computeIfAbsent(pathToComponent.get(path), c -> new HashSet<>()).add(null);
    });
    return childToParent;
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

  private static Component createComponent(Version version, ApiReportComponentDTOV2 reportComponent) {
    Component bomComponent = new Component();

    bomComponent.setType(Type.LIBRARY);

    PackageUrlIdentifier purl = PackageUrlIdentifier.fromComponentIdentifier(
        reportComponent.componentIdentifier.toComponentIdentifier());
    try {
      PackageURL packageUrl = new PackageURL(purl.getPackageUrl());
      bomComponent.setPurl(packageUrl);
      bomComponent.setGroup(packageUrl.getNamespace());
      bomComponent.setName(packageUrl.getName());
      bomComponent.setVersion(packageUrl.getVersion());
    }
    catch (MalformedPackageURLException e) {
      log.debug("Failed to create PackageURL for {}", reportComponent.packageUrl, e);
    }

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
    bomComponent.setModified(MatchState.SIMILAR.getId().equals(reportComponent.matchState));
    if (version.compareTo(Version.VERSION_12) > 0 && reportComponent.hash != null) {
      // Properties are only supported for xml/json schema version 1.3+
      Property property = new Property();
      property.setName(SbomUtils.SONATYPE_HASH_PROPERTY_NAME);
      property.setValue(reportComponent.hash);
      bomComponent.addProperty(property);
    }

    return bomComponent;
  }
}
