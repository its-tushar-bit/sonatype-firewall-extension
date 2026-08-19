/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.filter.UserFilterType;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

import static com.sonatype.insight.brain.model.filter.UserFilter.ACTIVE_FILTER_NAME;

@Named
@Timed
@Path(UserFilterResource.RESOURCE_PATH)
public class UserFilterResource
{
  public static final String RESOURCE_PATH = "rest/userFilter";

  public static final String ACTIVE_FILTERS_PATH = "active";

  public static final String NAMED_FILTERS_PATH = "named";

  private final UserFilterService userFilterService;

  @Inject
  public UserFilterResource(UserFilterService userFilterService) {
    this.userFilterService = userFilterService;
  }

  @PUT
  @Path(ACTIVE_FILTERS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.SAVE_USER_FILTER)
  public UserFilterDTO createOrUpdateActiveUserFilterForCurrentUser(UserFilterDTO userFilterDTO) {
    userFilterDTO.setName(ACTIVE_FILTER_NAME);
    return userFilterService.createOrUpdateUserFilterForCurrentUser(userFilterDTO);
  }

  @PUT
  @Path(NAMED_FILTERS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.SAVE_USER_FILTER)
  public UserFilterDTO createOrUpdateNamedUserFilterForCurrentUser(UserFilterDTO userFilterDTO) {
    return userFilterService.createOrUpdateUserFilterForCurrentUser(userFilterDTO);
  }

  @GET
  @Path(ACTIVE_FILTERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getActiveUserFilterForCurrentUserExceptionMeter")
  public UserFilterDTO getActiveUserFilterForCurrentUser(@QueryParam("type") UserFilterType type) {
    return userFilterService.getActiveUserFilterForCurrentUser(type);
  }

  @GET
  @Path(NAMED_FILTERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getNamedFiltersForCurrentUserExceptionMeter")
  public List<UserFilterDTO> getNamedFiltersForCurrentUser(@QueryParam("type") UserFilterType type) {
    return userFilterService.getNamedFiltersForCurrentUser(type);
  }

  @DELETE
  @Audited(AuditEvent.DELETE_USER_FILTER)
  public void deleteFilterForCurrentUserByNameAndType(
      @QueryParam("name") String name,
      @QueryParam("type") UserFilterType type)
  {
    userFilterService.deleteFilterForCurrentUserByNameAndType(name, type);
  }
}
