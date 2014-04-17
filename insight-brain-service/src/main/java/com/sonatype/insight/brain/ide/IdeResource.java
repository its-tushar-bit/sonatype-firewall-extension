/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ide.IdeMatchedComponent;
import com.sonatype.clm.dto.model.ide.MatchedComponent;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.component.HashGAVDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.HashGAV;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluator;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.saas.AugmentUtil;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.license.model.CLMEnforcementPoint;

import com.fasterxml.jackson.databind.node.ArrayNode;

@Named
@Path(IdeResource.SERVICE_PATH)
@ProductLicenseEnforcementPoint(CLMEnforcementPoint.Develop)
public class IdeResource
{
  public static final String SERVICE_PATH = "rest/ide";

  private final SaasClient client;

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private PolicyEvaluator evaluator = new PolicyEvaluator();

  private final InsightWork work;

  private final BaseUrl baseUrl;

  @Inject
  public IdeResource(InsightWork work, BaseUrl baseUrl, SaasClient client) {
    this.work = work;
    this.baseUrl = baseUrl;
    this.client = client;
  }

  /**
   * Requests an asset from the SaaS
   * 
   * @return the response from the SaaS
   * @since 1.2
   * @deprecated supporting ide plugins up to version 2.5.0, newer plugins will directly access the
   *             brains assets
   */
  @GET
  @Path("asset/{path:.*}")
  @Deprecated
  public Response getAsset(@PathParam("path") String path, @Context HttpServletRequest request,
      @HeaderParam(HttpHeaders.USER_AGENT) String userAgent) throws IOException
  {
    return client.doProxy(request, "ide/{path}", path);
  }

  /**
   * Get the result from a scan request, or a wait delta
   * 
   * @param scanType simple or enhanced though we do not enforce that in the Brain
   * @param applicationPublicId the public application id
   * @return the result of the scan or a wait delta
   * @since 1.2
   */
  @GET
  @Path("scan/{scanType}/{applicationPublicId}/{path:.*}")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public IdeMatchedComponent doScan(
      @PathParam("scanType") String scanType,
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) @PathParam("applicationPublicId") String applicationPublicId,
      @PathParam("path") String path, @QueryParam("proprietary") boolean proprietary, @Context HttpServletRequest req)
      throws IOException
  {
    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    String applicationId = app.getId();

    MatchedComponent matchedComponent = client.get(req, MatchedComponent.class, "rest/ide/scan/{scanType}/{path}",
        scanType, path);
    // Is this a manually claimed component?
    HashGAV hashGAV = new HashGAVDAO().getByHash(matchedComponent.getHash());
    if (hashGAV != null) {
      matchedComponent.setGroupId(hashGAV.getGroupId());
      matchedComponent.setArtifactId(hashGAV.getArtifactId());
      matchedComponent.setVersion(hashGAV.getVersion());
      matchedComponent.setCatalogDate(hashGAV.getCreateTimeLong());
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
      ArrayNode svData = AugmentUtil.getSVData(work, applicationId, matchedComponent.getGroupId(),
          matchedComponent.getArtifactId(), matchedComponent.getVersion(),
          matchedComponent.getSecurityVulnerabilities());

      ComponentDAO componentDAO = new ComponentDAO();
      Component component = componentDAO.getComponent(app, matchedComponent, svData);
      component.setProprietary(proprietary);
      List<PolicyAlert> policyAlerts = evaluator.evaluate(applicationId, new Stage(DevelopStageType.ID),
          new PolicyDAO(), Collections.singletonList(component));
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
  public IdeMatchedComponent postScan(@PathParam("scanType") String scanType,
      @PathParam("applicationPublicId") String applicationPublicId, @PathParam("path") String path,
      @QueryParam("proprietary") boolean proprietary, @Context HttpServletRequest req) throws IOException
  {
    return doScan(scanType, applicationPublicId, path, proprietary, req);
  }

  private IdeMatchedComponent getComponent(MatchedComponent mComponent) {
    IdeMatchedComponent ide = new IdeMatchedComponent();
    ide.setArtifactId(mComponent.getArtifactId());
    ide.setGroupId(mComponent.getGroupId());
    ide.setVersion(mComponent.getVersion());
    ide.setHash(mComponent.getHash());
    ide.setMatchState(mComponent.getMatchState());
    ide.setIdentificationSource(mComponent.getIdentificationSource());
    ide.setSimpleMatch(mComponent.isSimpleMatch());
    ide.setWaitDelta(mComponent.getWaitDelta());
    return ide;
  }

  /**
   * Gets the list of available versions for a given GA from the SaaS. (e.g. for use by migration wizard)
   * 
   * @return the SaaS response
   * @since 1.3
   */
  @GET
  @Path("component/versions")
  public Response getVersions(@Context HttpServletRequest req) throws IOException {
    return client.doProxy(req, "rest/ide/artifact/versions");
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
