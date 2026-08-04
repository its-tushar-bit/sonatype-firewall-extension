/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getApplicationTagsUrl } from 'MainRoot/util/CLMLocation';
import { ApplicationCategoryOption } from 'MainRoot/nosc/violations/violationListTypes';

type ApplicationCategoryWire = {
  readonly id?: string;
  readonly name?: string;
};

/**
 * Load Application Category (tag) options for the Violations filter rail.
 *
 * Uses the same estate-wide endpoint Classic dashboard filters use
 * ({@link getApplicationTagsUrl} → {@code GET /api/v2/applicationCategories/application}).
 * List facets for categories are deferred on the violations list API (name-keyed index field),
 * so the rail options come from this tags API rather than facet maps.
 */
export async function fetchApplicationCategoryOptions(): Promise<
  ReadonlyArray<ApplicationCategoryOption>
> {
  const response = await axios.get<ReadonlyArray<ApplicationCategoryWire>>(getApplicationTagsUrl());
  const rows = Array.isArray(response.data) ? response.data : [];
  return rows
    .filter((row): row is ApplicationCategoryWire & { id: string; name: string } =>
      Boolean(row?.id?.trim() && row?.name?.trim()),
    )
    .map((row) => ({ id: row.id, name: row.name }))
    .sort((a, b) => a.name.localeCompare(b.name));
}
