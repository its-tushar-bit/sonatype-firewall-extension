/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluator;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.utils.LicenseUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.jaxrs.JsonEncodedComponentIdentifier;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractComponentInfoResource
{
  private static final Logger log = LoggerFactory.getLogger(AbstractComponentInfoResource.class);

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private LicenseDAO licenseDAO = new LicenseDAO();

  private PolicyEvaluator evaluator = new PolicyEvaluator();

  private final SaasClient client;

  private final ComponentDetailsLoader componentDetailsLoader;

  @Context
  private HttpServletRequest request;

  protected AbstractComponentInfoResource(SaasClient client, ComponentDetailsLoader componentDetailsLoader) {
    this.client = client;
    this.componentDetailsLoader = componentDetailsLoader;
  }

  @GET
  @Path("{applicationPublicId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public NamedComponentDetails getComponentDetails(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") String applicationPublicId,
      @QueryParam("componentIdentifier") JsonEncodedComponentIdentifier identifier,
      @QueryParam("matchState") String matchState, @QueryParam("hash") String hash,
      @QueryParam("proprietary") boolean proprietary) throws IOException
  {
    long start = System.currentTimeMillis();

    NamedComponentDetails details = getEvaluatedComponentDetails(applicationPublicId, matchState, hash, proprietary,
        identifier);

    log.debug("Loaded component details for {}, hash {}, in {} ms.", identifier, hash, System.currentTimeMillis()
        - start);

    return details;
  }

  private NamedComponentDetails getEvaluatedComponentDetails(String applicationPublicId, String matchState, String hash,
      boolean proprietary, final ComponentIdentifier identifier) throws IOException
  {
    NamedComponentDetails componentDetails;

    if (identifier != null) {
      componentDetails = getComponentDetails(matchState, hash, identifier);
    }
    else {
      // See CLM-4195
      componentDetails = createEmptyComponentDetails(hash, identifier);
    }

    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    String applicationId = app.getId();

    Component component = loadComponent(app, componentDetails);
    component.setProprietary(proprietary);

    // Evaluate the policies
    List<PolicyAlert> policyAlerts = evaluator.evaluate(applicationId, new Stage(DevelopStageType.ID), new PolicyDAO(),
        Collections.singletonList(component));
    componentDetails.setPolicyAlerts(policyAlerts);
    return componentDetails;
  }

  private NamedComponentDetails getComponentDetails(String matchState, final String hash,
      final ComponentIdentifier identifier)
      throws IOException
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
              componentDetails = client.get(request, NamedComponentDetails.class, "rest/" + getToolName()
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
  private NamedComponentDetails createEmptyComponentDetails(String hash, ComponentIdentifier identifier)
  {
    NamedComponentDetails details = new NamedComponentDetails();
    details.setComponentIdentifier(identifier);
    details.setHash(hash);
    details.setMatchState(MatchState.UNKNOWN.getId());
    return details;
  }

  @GET
  @Path("{applicationPublicId}/list")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public ComponentDetailsList getComponentDetailsList(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") String applicationPublicId,
      @QueryParam("componentIdentifier") JsonEncodedComponentIdentifier identifier,
      @QueryParam("matchState") String matchState) throws IOException
  {
    long start = System.currentTimeMillis();
    if (identifier == null) {
      throw new BadRequestException("componentIdentifier is required");
    }

    String url = "rest/" + getToolName() + "/componentDetails/list";
    ComponentDetailsList componentDetailsList = client.get(request, ComponentDetailsList.class, url);

    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);

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
   * Returns the declared and observed licenses with their threat levels for a GAV
   * 
   * @since 1.6
   */
  @GET
  @Produces({ MediaType.APPLICATION_JSON })
  @Path("licenses/{applicationPublicId}")
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public ComponentLicenses getLicenses(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") String applicationPublicId,
      @QueryParam("componentIdentifier") JsonEncodedComponentIdentifier componentIdentifier) throws IOException
  {
    if (componentIdentifier == null) {
      throw new BadRequestException("componentIdentifier is required");
    }
    
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    ComponentLicenses result = new ComponentLicenses();

    ComponentDetails componentDetails = getComponentDetails(null, null, componentIdentifier);

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

  protected abstract String getToolName();
}
