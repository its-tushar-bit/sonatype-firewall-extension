/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.ide.IdeMatchedComponent;
import com.sonatype.clm.dto.model.ide.MatchedComponent;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.features.LicensedFeature;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.BaseUrl;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(IdeResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.IDE_INTEGRATION)
public class IdeResource
{
  public static final String RESOURCE_PATH = "rest/ide";

  private final HdsClient client;

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private final BaseUrl baseUrl;

  private final ComponentPolicyEvaluator componentPolicyEvaluator;

  @Inject
  public IdeResource(BaseUrl baseUrl,
                     HdsClient client,
                     ComponentPolicyEvaluator componentPolicyEvaluator)
  {
    this.baseUrl = baseUrl;
    this.client = client;
    this.componentPolicyEvaluator = componentPolicyEvaluator;
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
    Application app = applicationDAO.getByPublicIdNotNull(appPublicId);
    String applicationId = app.getId();

    MatchedComponent matchedComponent = client.relay(req, MatchedComponent.class, "rest/ide/scan/{scanType}/{path}",
        scanType, path);
    // Is this a manually claimed component?
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifierDAO().getByHash(matchedComponent
        .getHash());
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

    IdeMatchedComponent ideComponent = getComponent(matchedComponent);
    if (ideComponent.getWaitDelta() == null
        && (!"unknown".equals(ideComponent.getMatchState()) || !ideComponent.isSimpleMatch())) {
      ComponentDAO componentDAO = new ComponentDAO();
      Component component = componentDAO.getComponent(app, matchedComponent);
      component.setProprietary(proprietary);
      List<PolicyAlert> policyAlerts = componentPolicyEvaluator.evaluate(applicationId, new Stage(DevelopStageType.ID),
          Collections.singletonList(component));
      ideComponent.setAlerts(policyAlerts);
    }
    return ideComponent;
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
  public IdeMatchedComponent postScan(@PathParam("scanType") String scanType,
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
    return client.relay(req, String[].class, "rest/ide/artifact/versions");
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
}
