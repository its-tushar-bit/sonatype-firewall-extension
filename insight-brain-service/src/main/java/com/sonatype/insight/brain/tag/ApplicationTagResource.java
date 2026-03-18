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
  @Audited(AuditEvent.CONFIGURE_APPLICATION_CATEGORY)
  public void updateApplicationTags(
      @PathParam("applicationPublicId") final String applicationPublicId,
      final List<Tag> tags)
  {
    tagService.updateApplicationTags(applicationPublicId, tags);
  }
}
