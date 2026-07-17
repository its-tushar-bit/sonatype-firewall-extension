/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single entry in an owner's ancestor path. Used by the owner-picker endpoints to render
 * breadcrumbs.
 *
 * <p>
 * <b>Disclosure note:</b> when this entry appears in an {@code ancestorPath} on an owner
 * that the caller is permitted to see (via {@code EVALUATE_APPLICATION} or
 * {@code EVALUATE_COMPONENT}), the ancestor's id, name, and type are surfaced regardless
 * of whether the caller holds {@code READ} on the ancestor itself. This is intentional:
 * the picker's permission model is {@code EVALUATE_*} (not {@code READ}), and filtering
 * ancestors by {@code READ} would produce holed breadcrumbs. Do not use this DTO outside
 * of picker/breadcrumb contexts without re-evaluating that trade-off.
 *
 * @param id internal ID of the ancestor owner
 * @param name display name of the ancestor owner
 * @param type either {@code "organization"} or {@code "application"}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OwnerPathEntry(
    String id,
    String name,
    String type)
{
}
