/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.hds.DefaultHdsClient;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.17.0
 */
@Named
@Timed
@Path(RepositoryResource.RESOURCE_PATH)
public class RepositoryResource
{
  public static final String RESOURCE_PATH = "rest/integration/repositories";

  private static final String REPOSITORY_PATH = "{repositoryManagerInstanceId}/{repositoryPublicId}/";

  static final String SUMMARY_PATH = REPOSITORY_PATH + "summary";

  public static final String ENABLE_PATH = REPOSITORY_PATH + "enable/{enabled}";

  static final String QUARANTINE_PATH = REPOSITORY_PATH + "quarantine/{enabled}";

  static final String EVALUATE_COMPONENTS_PATH = REPOSITORY_PATH + "evaluate/audit";

  static final String EVALUATE_COMPONENTS_ADHOC_PATH = REPOSITORY_PATH + "evaluate/adhoc";

  static final String COMPONENTS_PATH = REPOSITORY_PATH + "components/{pathname: .+}";

  static final String EVALUATE_COMPONENTS_WITH_QUARANTINE_PATH = REPOSITORY_PATH + "evaluate/quarantine";

  static final String UNQUARANTINED_COMPONENTS_PATH = REPOSITORY_PATH + "components/unquarantined";

  static final String PROPRIETARY_NAMES_PATH = REPOSITORY_PATH + "proprietary/names";

  static final String IGNORE_PATTERNS_PATH = "evaluate/ignorePatterns";

  private final RepositoryService repositoryService;
  
  private final FirewallIgnorePatternService firewallIgnorePatternService;

  @Inject
  public RepositoryResource(
      RepositoryService repositoryService,
      FirewallIgnorePatternService firewallIgnorePatternService)
  {
    this.repositoryService = repositoryService;
    this.firewallIgnorePatternService = firewallIgnorePatternService;
  }

  /**
   * Enable a repository. Both the repository manager and the repository may be known or unknown to the IQ server. If
   * unknown, new entities are created in the IQ server database.
   */
  @POST
  @Path(ENABLE_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  public void setEnabled(@PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
                         @PathParam("repositoryPublicId") String repositoryPublicId,
                         @PathParam("enabled") boolean enabled)
  {
    AuditData.get().setEvent(enabled ? AuditEvent.CONNECT_REPOSITORY : AuditEvent.DISCONNECT_REPOSITORY);
    repositoryService.setEnabled(repositoryManagerInstanceId, repositoryPublicId, enabled);
  }

  @GET
  @Path(SUMMARY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public RepositoryPolicyEvaluationSummary getPolicyEvaluationSummary(
      @PathParam("repositoryManagerInstanceId") final String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") final String repositoryPublicId)
  {
    return repositoryService.getPolicyEvaluationSummary(repositoryManagerInstanceId, repositoryPublicId);
  }

  @POST
  @Path(EVALUATE_COMPONENTS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_REPOSITORY)
  public void evaluateComponents(@PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
                                 @PathParam("repositoryPublicId") String repositoryPublicId,
                                 RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
                                 @Context final HttpServletRequest request)
  {
    repositoryService.evaluateComponents(repositoryManagerInstanceId, repositoryPublicId,
        componentEvaluationDataRequestList, false, DefaultHdsClient.getClientUserAgent(request));
  }

  /**
   * @since 1.89
   */
  @POST
  @Path(EVALUATE_COMPONENTS_ADHOC_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_AD_HOC)
  public RepositoryComponentEvaluationDataList evaluateComponentsAdhoc(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      @Context HttpServletRequest request)
  {
    return repositoryService.evaluateComponentsAdhoc(repositoryManagerInstanceId, repositoryPublicId,
        componentEvaluationDataRequestList, DefaultHdsClient.getClientUserAgent(request));
  }

  @POST
  @Path(EVALUATE_COMPONENTS_WITH_QUARANTINE_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_REPOSITORY)
  public RepositoryComponentEvaluationDataList evaluateComponentsWithQuarantine(
      @PathParam("repositoryManagerInstanceId") final String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") final String repositoryPublicId,
      final RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      @Context final HttpServletRequest request)
  {
    return repositoryService.evaluateComponents(repositoryManagerInstanceId, repositoryPublicId,
        componentEvaluationDataRequestList, true, DefaultHdsClient.getClientUserAgent(request));
  }

  @Path(QUARANTINE_PATH)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_QUARANTINE)
  public void setQuarantine(@PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
                            @PathParam("repositoryPublicId") String repositoryPublicId,
                            @PathParam("enabled") boolean enabled)
  {
    repositoryService.setQuarantine(repositoryManagerInstanceId, repositoryPublicId, enabled);
  }

  @DELETE
  @Path(COMPONENTS_PATH)
  public void removeComponent(@PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
                              @PathParam("repositoryPublicId") String repositoryPublicId,
                              @PathParam("pathname") String pathname)
  {
    repositoryService.removeComponent(repositoryManagerInstanceId, repositoryPublicId, pathname);
  }

  /**
   * @since 1.20
   */
  @GET
  @Path(UNQUARANTINED_COMPONENTS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public UnquarantinedComponentList getUnquarantinedComponents(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      @QueryParam("sinceUtcTimestamp") long sinceUtcTimestamp)
  {
    return repositoryService.getUnquarantinedComponents(repositoryManagerInstanceId, repositoryPublicId,
        sinceUtcTimestamp);
  }

  /**
   * @since 1.35
   */
  @GET
  @Path(IGNORE_PATTERNS_PATH)
  @Produces({ MediaType.APPLICATION_JSON })
  public FirewallIgnorePatterns getIgnorePatterns() {
    return firewallIgnorePatternService.getIgnorePatterns();
  }

  /**
   * @since 1.106
   */
  @POST
  @Path(PROPRIETARY_NAMES_PATH)
  @Consumes({MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.ADD_PROPRIETARY_COMPONENT_NAMES)
  public void addProprietaryComponentNames(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      ProprietaryComponentNames proprietaryComponentNames)
  {
    repositoryService.addProprietaryComponentNames(repositoryManagerInstanceId, repositoryPublicId,
        proprietaryComponentNames);
  }

  /**
   * @since 1.106
   */
  @DELETE
  @Path(PROPRIETARY_NAMES_PATH)
  @Audited(AuditEvent.REMOVE_PROPRIETARY_COMPONENT_NAMES)
  public void removeProprietaryComponentNames(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId)
  {
    repositoryService.removeProprietaryComponentNames(repositoryManagerInstanceId, repositoryPublicId);
  }
}
