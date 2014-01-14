/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
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

import com.sonatype.insight.brain.model.tag.Tag;

@Named
@Path(TagResource.SERVICE_PATH)
/**
 * @since 1.9
 */
public class TagResource
{
  public static final String SERVICE_PATH = "rest/tag/{organizationId}";

  private final TagService service;

  @Inject
  public TagResource(TagService service) {
    this.service = service;
  }

  @GET
  @Produces({ MediaType.APPLICATION_JSON })
  public List<Tag> getTags(@PathParam("organizationId") String organizationId) {
    return service.getTags(organizationId);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Tag addTag(@PathParam("organizationId") String organizationId, Tag tag) {
    return service.addTag(organizationId, tag);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Tag updateTag(@PathParam("organizationId") String organizationId, Tag tag) {
    return service.updateTag(organizationId, tag);
  }

  @DELETE
  @Path("{tagId}")
  public void deleteTag(@PathParam("organizationId") String organizationId, @PathParam("tagId") String tagId) {
    service.deleteTag(organizationId, tagId);
  }
}
