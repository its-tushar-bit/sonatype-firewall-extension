/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { buildComponentsListExportPayload } from 'MainRoot/nosc/componentsList/componentsListExport';
import { EMPTY_COMPONENTS_LIST_FILTERS } from 'MainRoot/nosc/componentsList/componentsListFilters';

describe('componentsListExport (CLM-42214)', () => {
  it('omits name-keyed filters Classic cannot resolve', () => {
    expect(
      buildComponentsListExportPayload({
        ...EMPTY_COMPONENTS_LIST_FILTERS,
        organizations: new Set(['Java Team']),
        ecosystems: new Set(['npm']),
      }),
    ).toEqual({ orderBy: 'APPLICATION_COUNT' });
  });

  it('carries application and stage ids through to Classic (CLM-43211)', () => {
    expect(
      buildComponentsListExportPayload({
        ...EMPTY_COMPONENTS_LIST_FILTERS,
        applications: new Set(['app-2', 'app-1']),
        stages: new Set(['build']),
      }),
    ).toEqual({
      orderBy: 'APPLICATION_COUNT',
      applicationIds: ['app-1', 'app-2'],
      stageIds: ['build'],
    });
  });
});
