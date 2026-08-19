/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Collection;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.ConditionValueType;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.ConditionValueTypes;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.utils.IdUtils;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Timed
@Path(ConditionValueTypeResource.RESOURCE_PATH)
public class ConditionValueTypeResource
{
  public static final String RESOURCE_PATH =
      "rest/conditionValueType/" + //
          "{ownerType: application|organization|repository_container|repository_manager|repository}/{ownerId}";

  private static final Logger log = LoggerFactory.getLogger(ConditionValueTypeResource.class);

  private final ProductLicense productLicense;

  private final IdUtils idUtils;

  @Inject
  public ConditionValueTypeResource(final ProductLicense productLicense, final IdUtils idUtils) {
    this.productLicense = productLicense;
    this.idUtils = idUtils;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Collection<ConditionValueType> getConditionValueTypes(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId)
  {
    log.debug("Received request to get all {} condition value types for policyOwnerId ID {}", ownerType, ownerId);

    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    return (Collection) ConditionValueTypes.getAll(internalOwnerId, Set.copyOf(productLicense.getFeatures()));
  }
}
