/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { buildComponentsListExportPayload } from 'MainRoot/nosc/componentsList/componentsListExport';

describe('componentsListExport (CLM-42214)', () => {
  it('returns a Classic export payload without catalog-only filters', () => {
    expect(
      buildComponentsListExportPayload({
        organizations: new Set(['Java Team']),
        ecosystems: new Set(['npm']),
      }),
    ).toEqual({ orderBy: 'APPLICATION_COUNT' });
  });
});
