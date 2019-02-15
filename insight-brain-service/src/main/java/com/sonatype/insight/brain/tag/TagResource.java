/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(TagResource.RESOURCE_PATH)
/**
 * @since 1.9
 */
public class TagResource
{
  public static final String RESOURCE_PATH = "rest/tag/";

  public static final String USED_BY_APPLICATION_PATH = "application";

  public static final String APPLICATION_PATH = "application/{applicationPublicId}";

  public static final String ORGANIZATION_PATH = "organization/{organizationId}";

  private final TagService service;

  @Inject
  public TagResource(TagService service) {
    this.service = service;
  }

  @GET
  @Path(USED_BY_APPLICATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<Tag> getTagsUsedByApplications() {
    return service.getTagsUsedByApplications();
  }

  @GET
  @Path(APPLICATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicableTags getApplicationApplicableTags(@PathParam("applicationPublicId") String applicationPublicId) {
    return service.getApplicableTags(OwnerType.APPLICATION, applicationPublicId);
  }

  @GET
  @Path(ORGANIZATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicableTags getApplicableTags(@PathParam("organizationId") String organizationId) {
    return service.getApplicableTags(OwnerType.ORGANIZATION, organizationId);
  }

  @GET
  @Path(APPLICATION_PATH + "/applicable")
  @Produces(MediaType.APPLICATION_JSON)
  public List<Tag> getApplicableTagsByApplicationPublicId(
      @PathParam("applicationPublicId") String applicationPublicId)
  {
    return service.getApplicableTagsByApplicationPublicId(applicationPublicId);
  }

  @GET
  @Path(ORGANIZATION_PATH + "/applied")
  @Produces(MediaType.APPLICATION_JSON)
  public AppliedTags getAppliedTags(@PathParam("organizationId") String organizationId) {
    return service.getAppliedTags(organizationId);
  }

  @GET
  @Path(ORGANIZATION_PATH + "/policy")
  @Produces(MediaType.APPLICATION_JSON)
  public List<PolicyTag> getAppliedPolicyTags(@PathParam("organizationId") String organizationId) {
    return service.getAppliedPolicyTags(organizationId);
  }

  @POST
  @Path(ORGANIZATION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_APPLICATION_CATEGORY)
  public Tag addTag(@PathParam("organizationId") String organizationId, Tag tag) {
    return service.addTag(organizationId, tag);
  }

  @PUT
  @Path(ORGANIZATION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_APPLICATION_CATEGORY)
  public Tag updateTag(@PathParam("organizationId") String organizationId, Tag tag) {
    return service.updateTag(organizationId, tag);
  }

  @DELETE
  @Path(ORGANIZATION_PATH + "/{tagId}")
  @Audited(AuditEvent.DELETE_APPLICATION_CATEGORY)
  public void deleteTag(@PathParam("organizationId") String organizationId, @PathParam("tagId") String tagId) {
    service.deleteTag(organizationId, tagId);
  }

  public static class TagsByOwner
  {
    public TagsByOwner() {
    }

    public TagsByOwner(Owner owner, List<Tag> tags) {
      this.ownerId = owner.getPublicId();
      this.ownerName = owner.getName();
      this.ownerType = owner.getType();
      this.tags = tags;
    }

    public String ownerId;

    public String ownerName;

    public OwnerType ownerType;

    public List<Tag> tags;
  }

  public static class ApplicableTags
  {
    public List<TagsByOwner> tagsByOwner;
  }

  public static class ApplicationTagsByOwner
  {
    public ApplicationTagsByOwner() {
    }

    public ApplicationTagsByOwner(Owner owner, List<ApplicationTag> applicationTags) {
      this.ownerId = owner.getPublicId();
      this.ownerName = owner.getName();
      this.ownerType = owner.getType();
      this.applicationTags = applicationTags;
    }

    public String ownerId;

    public String ownerName;

    public OwnerType ownerType;

    public List<ApplicationTag> applicationTags;
  }

  public static class AppliedTags
  {
    public List<ApplicationTagsByOwner> applicationTagsByOwner;
  }
}
