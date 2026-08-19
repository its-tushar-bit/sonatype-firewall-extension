/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  ApplicationRiskScore,
  ApplicationsFilterFacetCounts,
} from 'MainRoot/nosc/applications/applicationListTypes';

type FacetEntry = ApplicationsFilterFacetCounts['stages'][number];

/** Derives org/app/stage facet rows from the current page when the API omits facet maps. */
export function deriveFacetsFromPageRows(
  applications: ReadonlyArray<ApplicationRiskScore>,
): Pick<ApplicationsFilterFacetCounts, 'stages' | 'organizations' | 'applications'> {
  const stageCounts = new Map<string, FacetEntry>();
  const orgCounts = new Map<string, FacetEntry>();
  const appCounts = new Map<string, FacetEntry>();

  applications.forEach((app) => {
    if (app.organizationId) {
      orgCounts.set(app.organizationId, {
        id: app.organizationId,
        label: app.organizationName,
        count: (orgCounts.get(app.organizationId)?.count ?? 0) + 1,
      });
    }
    appCounts.set(app.applicationId, {
      id: app.applicationId,
      label: app.applicationName,
      count: 1,
    });
    app.stageRisks.forEach((stage) => {
      stageCounts.set(stage.stageTypeId, {
        id: stage.stageTypeId,
        label: stage.stageTypeName,
        count: (stageCounts.get(stage.stageTypeId)?.count ?? 0) + 1,
      });
    });
  });

  const sortByLabel = (left: FacetEntry, right: FacetEntry) => left.label.localeCompare(right.label);

  return {
    stages: Array.from(stageCounts.values()).sort(sortByLabel),
    organizations: Array.from(orgCounts.values()).sort(sortByLabel),
    applications: Array.from(appCounts.values()).sort(sortByLabel),
  };
}
