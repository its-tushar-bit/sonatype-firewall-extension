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
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.tag.Tag;

import com.google.inject.Inject;

/**
 * Resource for managing applications applied tags
 *
 * @since 1.9
 */
@Named
@Path(ApplicationTagResource.SERVICE_PATH)
public class ApplicationTagResource
{
  public static final String SERVICE_PATH = "rest/applicationTag/{applicationPublicId}";

  private final TagService tagService;

  @Inject
  public ApplicationTagResource(TagService tagService) {
    this.tagService = tagService;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  public List<Tag> getAppliedTags(@PathParam("applicationPublicId") String applicationPublicId) {
    return tagService.getAppliedApplicationTags(applicationPublicId);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public void applyTag(@PathParam("applicationPublicId") String applicationPublicId, Tag tag) {
    tagService.applyTagToApplication(applicationPublicId, tag);
  }

  @DELETE
  @Path("{tagId}")
  public void removeApplicationTag(@PathParam("applicationPublicId") String applicationPublicId, @PathParam("tagId") String tagId) {
    tagService.removeApplicationTag(applicationPublicId, tagId);
  }
}
