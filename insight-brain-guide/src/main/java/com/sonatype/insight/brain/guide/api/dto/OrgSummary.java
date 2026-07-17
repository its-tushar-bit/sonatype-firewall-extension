/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Summary of an organization owner returned by the owner-picker endpoints.
 *
 * @param id internal ID; for organizations the public ID is the same value
 * @param publicId public ID (identical to {@code id} for organizations)
 * @param name display name
 * @param type always {@code "organization"}
 * @param ancestorPath chain of parent orgs from root down to but not including this org
 * @param appCount number of applications DIRECTLY under this org that the caller has
 *          {@code EVALUATE_COMPONENT} on. A picker with {@code appCount == 0}
 *          renders the org as a directly-selectable leaf; {@code > 0} renders it
 *          as a drill target
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrgSummary(
    String id,
    String publicId,
    String name,
    String type,
    List<OwnerPathEntry> ancestorPath,
    long appCount)
    implements OwnerSummary
{
}
