/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Named
@Singleton
public class ApiSourceControlEventService
{
  private final OwnerDAO ownerDAO;

  private final SourceControlEventDAO sourceControlEventDAO;

  @Inject
  public ApiSourceControlEventService(OwnerDAO ownerDAO, SourceControlEventDAO sourceControlEventDAO) {
    this.ownerDAO = ownerDAO;
    this.sourceControlEventDAO = sourceControlEventDAO;
  }

  @Authorize(permission = Permission.READ)
  public List<ApiSourceControlEventDTO> getApiSourceControlEventData(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      final ApiSourceControlEventFilterDTO filter)
  {
    validateFilter(filter);
    filter.setApplicationIds(ownerDAO.getDescendantOrSelfApplicationIds(ownerDAO.getById(ownerId)));
    List<SourceControlEvent> events = sourceControlEventDAO.selectEventsByCriteria(
        filter.getApplicationIds(),
        filter.getCreatedOnOrAfter(),
        filter.isAscending(),
        filter.getLimit(),
        filter.getOffset());
    return events.stream()
        .map(ApiSourceControlEventAdapterDTO::convert)
        .collect(Collectors.toList());
  }

  private void validateFilter(ApiSourceControlEventFilterDTO filter) {
    if (filter.getLimit() < 1) {
      throw new BadRequestException("Filter limit cannot be less than 1");
    }
    if (filter.getOffset() < 0) {
      throw new BadRequestException("Filter offset cannot be less than 0");
    }
  }
}
