/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Summary of an application owner returned by the owner-picker endpoints.
 *
 * @param id internal ID of the application
 * @param publicId public application ID (the customer-facing identifier)
 * @param name display name
 * @param type always {@code "application"}
 * @param ancestorPath chain of parent orgs from root down to (but not including) this app
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AppSummary(
    String id,
    String publicId,
    String name,
    String type,
    List<OwnerPathEntry> ancestorPath)
    implements OwnerSummary
{
}
