/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
    userFilterDTO.name = ACTIVE_FILTER_NAME;
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
  public UserFilterDTO getActiveUserFilterForCurrentUser(@QueryParam("type") UserFilterType type) throws IOException {
    return userFilterService.getActiveUserFilterForCurrentUser(type);
  }

  @GET
  @Path(NAMED_FILTERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getNamedFiltersForCurrentUserExceptionMeter")
  public List<UserFilterDTO> getNamedFiltersForCurrentUser(@QueryParam("type") UserFilterType type) throws IOException {
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
