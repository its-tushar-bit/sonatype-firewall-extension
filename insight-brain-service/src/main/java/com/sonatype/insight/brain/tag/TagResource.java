/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
@Path(TagResource.SERVICE_PATH)
/**
 * @since 1.9
 */
public class TagResource
{
  public static final String SERVICE_PATH = "rest/tag/{organizationId}";

  @GET
  @Produces({ MediaType.APPLICATION_JSON })
  @Authorize(permission = Permission.READ)
  public List<Tag> getTags(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) @PathParam("organizationId") String organizationId)
  {
    return new TagDAO().getByOrganizationId(organizationId);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  public Tag addTag(@AuthzContext(AuthzContext.Key.ORGANIZATION_ID) @PathParam("organizationId") String organizationId,
      Tag tag)
  {
    tag.setId(null);
    tag.setOrganizationId(organizationId);
    new TagDAO().insert(tag);

    return tag;
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  public Tag updateTag(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) @PathParam("organizationId") String organizationId, Tag tag)
  {
    tag.setOrganizationId(organizationId);
    new TagDAO().update(tag);

    return tag;
  }

  @DELETE
  @Path("{tagId}")
  @Authorize(permission = Permission.WRITE)
  public void deleteTag(
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) @PathParam("organizationId") String organizationId,
      @PathParam("tagId") String tagId)
  {
    TagDAO tagDAO = new TagDAO();
    Tag tag = tagDAO.getByIdNotNull(tagId);
    if (!organizationId.equals(tag.getOrganizationId())) {
      throw new NotFoundException("Cannot find a tag with id " + tagId + " for organization id " + organizationId);
    }

    tagDAO.delete(tag);
  }
}
