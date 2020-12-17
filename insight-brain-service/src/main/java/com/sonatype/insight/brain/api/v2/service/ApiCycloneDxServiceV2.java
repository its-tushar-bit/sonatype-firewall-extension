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
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
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

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.generators.xml.BomXmlGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.Hash.Algorithm;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
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
  public Response getByScanId(@AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId, String scanId) {
    Application application = applicationHelper.getApplicationByIdNotNull(applicationId);

    return getByScanId(application, scanId);
  }

  @Authorize(permission = Permission.READ)
  public Response getLatest(@AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId, String stageId) {
    Application application = applicationHelper.getApplicationByIdNotNull(applicationId);
    PolicyEvaluation evaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(), stageId);
    if (evaluation == null) {
      throw new NotFoundException("Unable to locate a scan for " + applicationId + " in stage " + stageId);
    }

    return getByScanId(application, evaluation.getScanId());
  }

  private Response getByScanId(Application application, String scanId) {
    AuditData.get().setReportId(scanId);
    try {
      ApiReportRawDataDTOV2 data = apiReportDataServiceV2.getDataNoAuth(application.getPublicId(), scanId);

      Bom bom = new Bom();
      bom.setSerialNumber(scanId);

      String url;
      try {
        url = baseUrl.get() + UserInterfaceLinksHelper.getReportUrl(application.getPublicId(), scanId);
      }
      catch (Exception e) {
        log.debug("Failed to locate baseUrl", e);
        url = UserInterfaceLinksHelper.getReportUrl(application.getPublicId(), scanId);
      }
      bom.addExternalReference(createExternalReference(url, "IQ Report", ExternalReference.Type.BOM));

      createBomComponents(data.components.stream().filter(c -> !MatchState.UNKNOWN.getId().equals(c.matchState))
          .collect(Collectors.toList())).forEach(bom::addComponent);

      BomXmlGenerator generator = BomGeneratorFactory.createXml(Version.VERSION_11, bom);
      generator.generate();

      return Response.ok(generator.toXmlString(), MediaType.APPLICATION_XML)
          .header(HttpHeaders.CONTENT_DISPOSITION,
              "Content-Disposition: attachment; filename=\"" + application.getPublicId() + '-' + scanId + ".xml\"")
          .build();
    }
    catch (IOException | ParserConfigurationException | TransformerException e) {
      throw new InternalServerException("An error occurred generating report", e);
    }
  }

  private static List<Component> createBomComponents(List<ApiReportComponentDTOV2> reportComponents) {
    Map<String, ApiReportComponentDTOV2> pathToComponent = new HashMap<>();

    reportComponents.stream()
        .forEach(component -> component.pathnames.forEach(path -> pathToComponent.put(path, component)));

    LinkedHashMap<ApiReportComponentDTOV2, Set<ApiReportComponentDTOV2>> childToParents =
        calculateParents(pathToComponent);

    Map<ApiReportComponentDTOV2, Component> converted = new HashMap<>();

    reportComponents.stream().forEach(component -> converted.put(component, createComponent(component)));

    childToParents.forEach((child, parents) -> {
      parents.stream().filter(p -> p != null).forEach(parent -> {
        converted.get(parent).addComponent(converted.get(child));
      });
    });

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
          childToParent.computeIfAbsent(pathToComponent.get(path), c -> new HashSet<>())
              .add(pathToComponent.get(parentCandidate));
          return;
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
    return new MultiLicenseDAO().getLicensesByMultiLicenseIdNotNull(apiLicense.licenseId).stream().map(l -> {
      License license = new License();
      license.setId(l.getId());
      license.setName(l.getShortDisplayName());
      return license;
    }).collect(Collectors.toSet());
  }

  private static Component createComponent(ApiReportComponentDTOV2 reportComponent) {
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
    if (MatchState.SIMILAR.getId().equals(reportComponent.matchState)) {
      bomComponent.setModified(true);
    }
    if (reportComponent.hash != null) {
      bomComponent.addHash(new Hash(Algorithm.SHA1, reportComponent.hash));
    }

    return bomComponent;
  }
}
