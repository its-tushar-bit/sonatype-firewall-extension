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
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.tag.Tag;

import com.codahale.metrics.annotation.Timed;

/**
 * Resource for managing applications applied tags
 *
 * @since 1.9
 */
@Named
@Timed
@Path(ApplicationTagResource.RESOURCE_PATH)
public class ApplicationTagResource
{
  public static final String RESOURCE_PATH = "rest/appliedTag/application/{applicationPublicId}";

  private final TagService tagService;

  @Inject
  public ApplicationTagResource(TagService tagService) {
    this.tagService = tagService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<Tag> getAppliedTags(@PathParam("applicationPublicId") String applicationPublicId) {
    return tagService.getAppliedApplicationTags(applicationPublicId);
  }

  /**
   * Replace all existing tags with the list of tags passed in
   *
   * @since 1.18.0
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  public void updateApplicationTags(@PathParam("applicationPublicId") final String applicationPublicId,
                                    final List<Tag> tags)
  {
    tagService.updateApplicationTags(applicationPublicId, tags);
  }
}
