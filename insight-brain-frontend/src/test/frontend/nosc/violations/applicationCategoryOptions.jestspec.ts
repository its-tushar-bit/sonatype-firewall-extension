/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import { getApplicationTagsUrl } from 'MainRoot/util/CLMLocation';
import { fetchApplicationCategoryOptions } from 'MainRoot/nosc/violations/applicationCategoryOptions';

describe('fetchApplicationCategoryOptions (CLM-44129)', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    _setBaseUrlForTesting('http://localhost');
  });

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    axiosMock.restore();
  });

  it('loads and sorts tags from the Classic applicationCategories endpoint', async () => {
    axiosMock.onGet(getApplicationTagsUrl()).reply(200, [
      { id: 'cat-z', name: 'Zeta', organizationId: 'org-1', color: 'blue' },
      { id: 'cat-a', name: 'Alpha', organizationId: 'org-1', color: 'green' },
      { id: '', name: 'blank-id' },
      { id: 'cat-empty-name', name: '  ' },
    ]);

    const options = await fetchApplicationCategoryOptions();
    expect(options).toEqual([
      { id: 'cat-a', name: 'Alpha' },
      { id: 'cat-z', name: 'Zeta' },
    ]);
  });
});
