/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

/**
 * Resource for managing tags associated with policies
 *
 * @since 1.9
 */
@Named
@Timed
@Path(PolicyTagResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.POLICY_MANAGEMENT)
public class PolicyTagResource
{
  public static final String RESOURCE_PATH = "rest/appliedTag/policy/{policyId}/{ownerType}/{ownerId}";

  private final TagService tagService;

  @Inject
  public PolicyTagResource(TagService tagService) {
    this.tagService = tagService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ProductLicenseEnforcementPoint(LicensedFeature.POLICY_READ_ONLY)
  public List<Tag> getPolicyTags(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @PathParam("policyId") String policyId)
  {
    return tagService.getPolicyTags(ownerType, ownerId, policyId);
  }

  /**
   * Replace all existing {@link PolicyTag} with the list of {@link Tag} passed in
   *
   * @since 1.18.0
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_POLICY_INHERITANCE)
  public List<Tag> updatePolicyTags(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @PathParam("policyId") String policyId,
      List<Tag> tags)
  {
    return tagService.updatePolicyTags(ownerType, ownerId, policyId, tags);
  }
}
