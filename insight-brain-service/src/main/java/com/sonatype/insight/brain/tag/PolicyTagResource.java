/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.List;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.tag.Tag;

import com.google.inject.Inject;

/**
 * Resource for managing tags associated with policies
 * 
 * @since 1.9
 */
@Named
@Path(PolicyTagResource.SERVICE_PATH)
public class PolicyTagResource
{
  public static final String SERVICE_PATH = "rest/appliedTag/policy/{policyId}";

  private final TagService tagService;

  @Inject
  public PolicyTagResource(TagService tagService) {
    this.tagService = tagService;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  public List<Tag> getPolicyTags(@QueryParam("orgId") String orgId, @PathParam("policyId") String policyId) {
    return tagService.getPolicyTags(orgId, policyId);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({ MediaType.APPLICATION_JSON })
  public Tag addPolicyTag(@QueryParam("orgId") String orgId, @PathParam("policyId") String policyId, Tag tag) {
    return tagService.addPolicyTag(orgId, policyId, tag);
  }

  @DELETE
  @Path("{tagId}")
  public void deletePolicyTag(@QueryParam("orgId") String orgId, @PathParam("policyId") String policyId,
      @PathParam("tagId") String tagId)
  {
    tagService.deletePolicyTag(orgId, policyId, tagId);
  }
}
