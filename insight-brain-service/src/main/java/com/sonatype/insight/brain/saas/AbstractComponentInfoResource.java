/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.ide.ComponentDetails;
import com.sonatype.clm.dto.model.ide.ComponentDetailsList;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.component.HashGAVDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.HashGAV;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluator;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractComponentInfoResource
{
  private static final Logger log = LoggerFactory.getLogger(AbstractComponentInfoResource.class);

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private LicenseDAO licenseDAO = new LicenseDAO();

  private PolicyEvaluator evaluator = new PolicyEvaluator();

  private final SaasClient client;

  private final InsightWork work;

  @Context
  private HttpServletRequest request;

  protected AbstractComponentInfoResource(SaasClient client, InsightWork work) {
    this.client = client;
    this.work = work;
  }

  @GET
  @Path("list/{applicationPublicId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public ComponentDetailsList getComponentDetailsList(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") String applicationPublicId,
      @QueryParam("groupId") String groupId, @QueryParam("artifactId") String artifactId,
      @QueryParam("version") String version) throws IOException
  {
    long start = System.currentTimeMillis();

    log.debug("Getting {} component details list for application id {}, GAV {}:{}:{}.", getToolName(),
        applicationPublicId, groupId, artifactId, version);
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    ComponentDetailsList componentDetailsList = client.get(request, ComponentDetailsList.class,
        "rest/ide/component/details/list");

    for (ComponentDetails componentDetails : componentDetailsList.getList()) {
      loadComponent(app, componentDetails);
    }

    log.debug("Loaded component details list for {}:{}:{} in {} ms.", groupId, artifactId, version,
        System.currentTimeMillis() - start);

    return componentDetailsList;
  }

  @GET
  @Path("{applicationPublicId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public ComponentDetails getComponentDetails(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") String applicationPublicId,
      @QueryParam("instanceId") String instanceId, @QueryParam("groupId") String groupId,
      @QueryParam("artifactId") String artifactId, @QueryParam("version") String version,
      @QueryParam("hash") String hash, @QueryParam("matchState") String matchState,
      @QueryParam("proprietary") boolean proprietary) throws IOException
  {
    long start = System.currentTimeMillis();

    log.debug("Getting {} component details for application id {}, GAV {}:{}:{}, hash {}.", getToolName(),
        applicationPublicId, groupId, artifactId, version, hash);
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    String applicationId = app.getId();

    ComponentDetails componentDetails = getComponentDetails(groupId, artifactId, version, hash, matchState);

    Component component = loadComponent(app, componentDetails);
    component.setProprietary(proprietary);

    // Evaluate the policies
    List<PolicyAlert> policyAlerts = evaluator.evaluate(applicationId, new Stage(DevelopStageType.ID), policyDAO(),
        Collections.singletonList(component));
    componentDetails.setPolicyAlerts(policyAlerts);

    log.debug("Loaded component details for {}:{}:{}, hash {}, in {} ms.", groupId, artifactId, version, hash,
        System.currentTimeMillis() - start);

    return componentDetails;
  }

  private ComponentDetails getComponentDetails(String groupId, String artifactId, String version, String hash,
      String matchState) throws IOException
  {
    ComponentDetails componentDetails = null;

    // Look among claimed components first
    final HashGAV hashGAV;
    HashGAVDAO hashGAVDAO = new HashGAVDAO();
    if (hash != null && !hash.trim().isEmpty()) {
      hashGAV = hashGAVDAO.getByHash(hash);
    }
    else {
      hashGAV = hashGAVDAO.getByGAV(groupId, artifactId, version);
    }
    if (hashGAV != null) {
      componentDetails = new ComponentDetails(hashGAV.getGroupId(), hashGAV.getArtifactId(), hashGAV.getVersion());
      componentDetails.setHash(hashGAV.getHash());
      componentDetails.setMatchState(MatchState.EXACT.getId());
      componentDetails.setCatalogDate(hashGAV.getCreateTimeLong());
      componentDetails.setIdentificationSource(IdentificationSource.MANUAL.getId());
      componentDetails.setIdentificationSourceComment(hashGAV.getComment());
    }

    // Get component details from the SaaS server, if not found locally
    if (componentDetails == null) {
      try {
        componentDetails = client.get(request, ComponentDetails.class, "rest/ide/component/details");
        componentDetails.setMatchState(MatchState.EXACT.getId());
      }
      catch (NotFoundException e) {
        // GAV is unknown to SaaS, still want to provide minimal data for details view
        componentDetails = new ComponentDetails(groupId, artifactId, version);
        componentDetails.setMatchState(MatchState.UNKNOWN.getId());
      }

      componentDetails.setHash(hash); // SaaS does not set hash
      if (matchState != null && !matchState.trim().isEmpty()) {
        componentDetails.setMatchState(matchState);
      }
      componentDetails.setIdentificationSource(IdentificationSource.SONATYPE.getId());
    }

    return componentDetails;
  }

  private Component loadComponent(Application application, ComponentDetails componentDetails) throws IOException {
    // Load the augmented data for licenses and security vulnerabilities
    ArrayNode svData = AugmentUtil.getSVData(work, application.getId(), componentDetails.getGroupId(),
        componentDetails.getArtifactId(), componentDetails.getVersion(), componentDetails.getSecurityVulnerabilities());
    ComponentDAO componentDAO = new ComponentDAO();
    Component component = componentDAO.getComponent(application, componentDetails, svData);

    // Use CLM data to populate the component details
    if (component.getLicenseOverrideId() != null) {
      com.sonatype.insight.brain.model.license.License overriddenLicense = licenseDAO.getByIdNotNull(component
          .getLicenseOverrideId());
      componentDetails.getOverriddenLicenses().add(
          new License(overriddenLicense.getId(), overriddenLicense.getShortDisplayName()));
    }
    if (!component.getLicenseThreatGroups().isEmpty()) {
      int licenseThreatLevel = 0;
      for (LicenseThreatGroup licenseThreatGroup : component.getLicenseThreatGroups()) {
        licenseThreatLevel = Math.max(licenseThreatLevel, licenseThreatGroup.getThreatLevel());
      }
      componentDetails.setLicenseThreatLevel(licenseThreatLevel);
    }
    if (componentDetails.getSecurityVulnerabilities() != null) {
      for (SecurityVulnerability issue : componentDetails.getSecurityVulnerabilities()) {
        issue.setStatus(SecurityVulnerabilityStatus.OPEN.getName());
        for (com.sonatype.insight.brain.model.component.SecurityVulnerability sv : component
            .getSecurityVulnerabilities()) {
          if (issue.getRefId().equals(sv.getRefId()) && issue.getSource().equals(sv.getSource())) {
            issue.setStatus(sv.getStatus().getName());
            break;
          }
        }
      }
    }
    return component;
  }

  private PolicyDAO policyDAO() {
    return new PolicyDAO(work.getWorkDir());
  }

  @GET
  @Path("selectableLicenses/{applicationPublicId}")
  @Produces({ MediaType.APPLICATION_JSON })
  @Authorize(permission = Permission.READ)
  public Set<License> getSelectableLicenses(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") String applicationPublicId,
      @QueryParam("instanceId") String instanceId, @QueryParam("groupId") String groupId,
      @QueryParam("artifactId") String artifactId, @QueryParam("version") String version) throws IOException
  {
    applicationDAO.getByPublicIdNotNull(applicationPublicId);

    // Get component details from the SAAS server
    ComponentDetails componentDetails = getComponentDetails(groupId, artifactId, version, null, null);

    MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
    Set<License> result = new LinkedHashSet<License>();
    Set<License> licenses = new LinkedHashSet<License>();
    licenses.addAll(componentDetails.getDeclaredLicenses());
    licenses.addAll(componentDetails.getObservedLicenses());
    Iterator<License> licenseIter = licenses.iterator();
    while (licenseIter.hasNext()) {
      License license = licenseIter.next();
      MultiLicense multiLicense = multiLicenseDAO.getById(license.getLicenseId());
      if (multiLicense.isUnspecified()) {
        continue;
      }
      Set<com.sonatype.insight.brain.model.license.License> _licenses = multiLicenseDAO
          .getLicensesByMultiLicenseIdNotNull(multiLicense.getId());
      for (com.sonatype.insight.brain.model.license.License _license : _licenses) {
        if (_license.getId().endsWith("-UNSPECIFIED")) {
          String licenseIdPrefix = _license.getId().substring(0, _license.getId().length() - "UNSPECIFIED".length());
          for (com.sonatype.insight.brain.model.license.License otherLicense : licenseDAO.getAll()) {
            if (otherLicense.getId().startsWith(licenseIdPrefix) && !_license.getId().equals(otherLicense.getId())) {
              result.add(new License(otherLicense.getId(), otherLicense.getShortDisplayName()));
            }
          }
        }
        result.add(new License(_license.getId(), _license.getShortDisplayName()));
      }
    }
    return result;
  }

  /**
   * Returns the declared and observed licenses with their threat levels for a GAV
   * 
   * @since 1.6
   */
  @GET
  @Produces({ MediaType.APPLICATION_JSON })
  @Path("licenses/{applicationPublicId}")
  @Authorize(permission = Permission.READ)
  public ComponentLicenses getLicenses(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") String applicationPublicId,
      @QueryParam("groupId") String groupId, @QueryParam("artifactId") String artifactId,
      @QueryParam("version") String version) throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    ComponentLicenses result = new ComponentLicenses();

    ComponentDetails componentDetails = getComponentDetails(groupId, artifactId, version, null, null);
    result.declaredlicenses = getLicensesWithThreatLevels(application, componentDetails.getDeclaredLicenses());
    result.observedlicenses = getLicensesWithThreatLevels(application, componentDetails.getObservedLicenses());

    return result;
  }

  /**
   * @since 1.6
   */
  private List<LicenseWithThreatLevel> getLicensesWithThreatLevels(Application application, Set<License> multiLicenses)
  {
    List<LicenseWithThreatLevel> result = new ArrayList<LicenseWithThreatLevel>();

    if (multiLicenses != null) {
      MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
      LicenseDAO licenseDAO = new LicenseDAO();
      for (License multiLicense : multiLicenses) {
        Set<com.sonatype.insight.brain.model.license.License> licenses = multiLicenseDAO
            .getLicensesByMultiLicenseIdNotNull(multiLicense.getLicenseId());
        for (com.sonatype.insight.brain.model.license.License license : licenses) {
          LicenseWithThreatLevel licenseWithThreatLevel = new LicenseWithThreatLevel();
          licenseWithThreatLevel.license = new License(license.getId(), license.getShortDisplayName());
          licenseWithThreatLevel.threatLevel = licenseDAO.getLicenseThreatLevelByApplicationAndLicenseId(application,
              license.getId());

          result.add(licenseWithThreatLevel);
        }
      }
    }

    return result;
  }

  /**
   * @since 1.6
   */
  public static class ComponentLicenses
  {
    public List<LicenseWithThreatLevel> declaredlicenses;
    public List<LicenseWithThreatLevel> observedlicenses;
  }

  /**
   * @since 1.6
   */
  public static class LicenseWithThreatLevel
  {
    public License license;
    public Integer threatLevel;
  }

  protected abstract String getToolName();
}
