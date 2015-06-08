/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.LicenseUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ComponentInfoService
{
  private static final Logger log = LoggerFactory.getLogger(ComponentInfoService.class);

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private LicenseDAO licenseDAO = new LicenseDAO();

  private final SaasClient hdsClient;

  private final ComponentDetailsLoader componentDetailsLoader;

  private final InsightWork insightWork;

  private final ReportService reportService;

  private String toolName;

  @Inject
  public ComponentInfoService(SaasClient hdsClient, ComponentDetailsLoader componentDetailsLoader,
      InsightWork insightWork, ReportService reportService)
  {
    this.hdsClient = hdsClient;
    this.componentDetailsLoader = componentDetailsLoader;
    this.insightWork = insightWork;
    this.reportService = reportService;
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public NamedComponentDetails getComponentDetails_EvaluateComponentPermission(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId, ComponentIdentifier identifier,
      String matchState, String hash, boolean proprietary, HttpServletRequest httpRequest) throws IOException
  {
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    NamedComponentDetails details = getComponentDetails(app, identifier, matchState, hash, proprietary, httpRequest);

    return details;
  }

  @Authorize(permission = Permission.READ)
  public NamedComponentDetails getComponentDetails_ReadPermission(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId, String reportId,
      ComponentIdentifier componentIdentifier, String matchState, String hash, boolean proprietary,
      HttpServletRequest httpRequest) throws IOException
  {
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    checkComponentIsInReport(app, reportId, componentIdentifier);
    NamedComponentDetails details = getComponentDetails(app, componentIdentifier, matchState, hash, proprietary,
        httpRequest);

    return details;
  }

  private void checkComponentIsInReport(Application app, String reportId, ComponentIdentifier checkedComponentIdentifier)
      throws IOException
  {
    File reportFile = getReportFile(app, reportId);
    checkedComponentIdentifier = checkedComponentIdentifier.createAlternativeVersion("");
    ReportEntry bomReportEntry = Report.getEntry(reportFile, "bom.json");
    List<Component> components = new ComponentDAO().getAll(bomReportEntry.buf);
    for (Component component : components) {
      ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
      if (componentIdentifier != null) {
        componentIdentifier = componentIdentifier.createAlternativeVersion("");
        if (componentIdentifier.equals(checkedComponentIdentifier)) {
          return;
        }
      }
    }

    // Don't give too many details because we don't want an "attacker" to use this to check if a component is in a
    // report or not.
    log.error("Detected possible attack: Received request for component details for a component that is not in the specified report.");
    throw new InternalServerException("Cannot get component details.");
  }

  private File getReportFile(Application app, String reportId) throws IOException {
    if (StringUtils.isBlank(reportId)) {
      throw new InternalServerException("The report ID must be specified.");
    }

    File reportFile = reportService.getReport(insightWork, app.getId(), reportId);
    if (reportFile == null || !reportFile.isFile()) {
      throw new NotFoundException("Cannot find a report with ID '" + reportId + "'.");
    }

    return reportFile;
  }

  NamedComponentDetails getComponentDetails(Application app, final ComponentIdentifier identifier, String matchState,
      String hash, boolean proprietary, HttpServletRequest httpRequest) throws IOException
  {
    long start = System.currentTimeMillis();

    NamedComponentDetails componentDetails;

    if (identifier != null) {
      componentDetails = getComponentDetailsFromHDS(matchState, hash, identifier, httpRequest);
    }
    else {
      // See CLM-4195
      componentDetails = createEmptyComponentDetails(hash, identifier);
    }

    Component component = loadComponent(app, componentDetails);
    component.setProprietary(proprietary);

    // Evaluate the policies
    List<PolicyAlert> policyAlerts = new ComponentPolicyEvaluator().evaluate(app.getId(),
        new Stage(DevelopStageType.ID), Collections.singletonList(component));
    componentDetails.setPolicyAlerts(policyAlerts);

    log.debug("Loaded component details for {}, hash {}, in {} ms.", identifier, hash, System.currentTimeMillis()
        - start);
    return componentDetails;
  }

  private NamedComponentDetails getComponentDetailsFromHDS(String matchState, final String hash,
      final ComponentIdentifier identifier, final HttpServletRequest httpRequest) throws IOException
  {
    return componentDetailsLoader.getComponentDetails(identifier, hash, matchState,
        new ComponentDetailsLoader.HostedDataServicesSource()
        {
          @Override
          public NamedComponentDetails getDetails() throws IOException {
            NamedComponentDetails componentDetails;

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("componentIdentifier", ComponentIdentifierAdapter.toJson(identifier));
            if (hash != null) {
              queryParams.put("hash", hash);
            }

            try {
              componentDetails = hdsClient.get(httpRequest, NamedComponentDetails.class, "rest/" + toolName
                  + "/componentDetails", queryParams);
              componentDetails.setMatchState(MatchState.EXACT.getId());
            }
            catch (NotFoundException e) {
              // Identifier is unknown to HDS, still want to provide minimal data for details view
              componentDetails = createEmptyComponentDetails(hash, identifier);
            }
            return componentDetails;
          }
        });
  }

  // Intended for unknown cases
  private NamedComponentDetails createEmptyComponentDetails(String hash, ComponentIdentifier identifier) {
    NamedComponentDetails details = new NamedComponentDetails();
    details.setComponentIdentifier(identifier);
    details.setHash(hash);
    details.setMatchState(MatchState.UNKNOWN.getId());
    return details;
  }

  /**
   * Returns a list of component details for the given application and component identifier. It does not evaluate
   * policies and it does not return policy violations.
   * 
   * This method is called by the eclipse plugin, so it needs to check the EVALUATE_COMPONENT permission.
   */
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public ComponentDetailsList getComponentDetailsList_EvaluateComponentPermission(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId, ComponentIdentifier identifier,
      String matchState, HttpServletRequest httpRequest) throws IOException
  {
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    return getComponentDetailsList(app, identifier, matchState, httpRequest);
  }

  /**
   * Returns a list of component details for the given application and component identifier. It does not evaluate
   * policies and it does not return policy violations.
   * 
   * This method is called by the CIP, so it needs to check the READ permission.
   */
  @Authorize(permission = Permission.READ)
  public ComponentDetailsList getComponentDetailsList_ReadPermission(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId, String reportId,
      ComponentIdentifier componentIdentifier, String matchState, HttpServletRequest httpRequest) throws IOException
  {
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    checkComponentIsInReport(app, reportId, componentIdentifier);
    return getComponentDetailsList(app, componentIdentifier, matchState, httpRequest);
  }

  ComponentDetailsList getComponentDetailsList(Application app, ComponentIdentifier identifier, String matchState,
      HttpServletRequest httpRequest) throws IOException
  {
    long start = System.currentTimeMillis();

    if (identifier == null) {
      throw new BadRequestException("componentIdentifier is required");
    }

    String url = "rest/" + toolName + "/componentDetails/list";
    ComponentDetailsList componentDetailsList = hdsClient.get(httpRequest, ComponentDetailsList.class, url);

    for (ComponentDetails componentDetails : componentDetailsList.getList()) {
      componentDetails.setMatchState(StringUtils.isEmpty(matchState) ? MatchState.EXACT.getId() : matchState);
      loadComponent(app, componentDetails);
    }

    log.debug("Loaded component details list in {} ms.", System.currentTimeMillis() - start);

    return componentDetailsList;
  }

  private Component loadComponent(Application application, ComponentDetails componentDetails) throws IOException {
    return componentDetailsLoader.augmentComponentDetails(application, componentDetails);
  }

  /**
   * Returns the declared and observed licenses with their threat levels for a component.
   * 
   * @since 1.6
   */
  @Authorize(permission = Permission.READ)
  public ComponentLicenses getLicenses(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      ComponentIdentifier componentIdentifier, HttpServletRequest httpRequest) throws IOException
  {
    if (componentIdentifier == null) {
      throw new BadRequestException("componentIdentifier is required");
    }

    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    ComponentLicenses result = new ComponentLicenses();

    ComponentDetails componentDetails = getComponentDetailsFromHDS(null, null, componentIdentifier, httpRequest);

    loadComponent(application, componentDetails);
    result.declaredlicenses = getLicensesWithThreatLevels(application, componentDetails.getDeclaredLicenses());
    result.observedlicenses = getLicensesWithThreatLevels(application, componentDetails.getObservedLicenses());
    result.effectiveLicenses = getLicensesWithThreatLevels(application, componentDetails.getEffectiveLicenses());
    result.selectableLicenses = new ArrayList<>(getSelectableLicenses(componentDetails.getDeclaredLicenses(),
        componentDetails.getObservedLicenses()));

    return result;
  }

  private Set<License> getSelectableLicenses(Collection<License> declared, Collection<License> observed) {
    MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
    Set<License> result = new LinkedHashSet<>();
    Set<License> licenses = new LinkedHashSet<>();
    licenses.addAll(declared);
    licenses.addAll(observed);
    Iterator<License> licenseIter = licenses.iterator();
    while (licenseIter.hasNext()) {
      License license = licenseIter.next();

      if (com.sonatype.insight.brain.model.license.License.isEffectivelyUnspecified(license.getLicenseId())) {
        continue;
      }

      MultiLicense multiLicense = multiLicenseDAO.getById(license.getLicenseId());
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
   * @since 1.6
   */
  private List<LicenseWithThreatLevel> getLicensesWithThreatLevels(Application application, Set<License> multiLicenses)
  {
    List<LicenseWithThreatLevel> result = new ArrayList<>();

    if (multiLicenses != null) {
      MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
      for (License multiLicense : multiLicenses) {
        Set<com.sonatype.insight.brain.model.license.License> licenses = multiLicenseDAO
            .getLicensesByMultiLicenseIdNotNull(multiLicense.getLicenseId());
        for (com.sonatype.insight.brain.model.license.License license : licenses) {
          LicenseWithThreatLevel licenseWithThreatLevel = LicenseUtils.getLicenseWithThreatLevel(application, license);
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

    /**
     * @since 1.12
     */
    public List<LicenseWithThreatLevel> effectiveLicenses;

    /**
     * @since 1.13
     */
    public List<License> selectableLicenses;

  }

  /**
   * @since 1.6
   */
  public static class LicenseWithThreatLevel
  {
    public License license;
    public Integer threatLevel;
  }

  public void setToolName(String toolName) {
    this.toolName = toolName;
  }
}
