/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  LEGAL_APPLICATION_DETAILS_SET_COMPONENT_NAME_FILTER,
  LEGAL_APPLICATION_DETAILS_SET_LICENSE_NAME_FILTER,
  updateComponentNameFilter,
  updateLicenseNameFilter,
} from '../../../../../main/frontend/legal/application/filter/legalApplicationDetailsFilterActions';

import 'TestRoot/SpecUtil';

describe('LegalApplicationDetailsFilterActions', function () {
  let store = {};
  const initState = {
    application: {
      name: null,
      error: null,
      loading: false,
    },
    stageType: {
      name: null,
      error: null,
      loading: false,
    },
    components: {
      results: Object.freeze([]),
      filteredResults: Object.freeze([]),
      error: null,
      loading: false,
    },
    componentFilter: '',
    licenseFilter: '',
    reviewStatusFilter: Object.freeze([]),
    licenseThreatGroupFilter: Object.freeze([]),
    sort: Object.freeze({
      column: 'component',
      sortOrder: 'asc',
    }),
    page: 1,
    selected: Object.freeze({
      progressOptions: new Set(),
      licenseThreatGroups: new Set(),
    }),
  };

  it('updateComponentNameFilter dispatches LEGAL_APPLICATION_DETAILS_SET_COMPONENT_NAME_FILTER action', function () {
    store = SpecUtil.mockReduxStore(initState);
    store.dispatch(updateComponentNameFilter({ filter: 'test' }));

    const actions = store.getActions();
    expect(actions.length).toBe(1);
    expect(actions[0].type).toBe(LEGAL_APPLICATION_DETAILS_SET_COMPONENT_NAME_FILTER);
    expect(actions[0].payload).toEqual({ filter: 'test' });
  });

  it('updateLicenseNameFilter dispatches LEGAL_APPLICATION_DETAILS_SET_LICENSE_NAME_FILTER action', function () {
    store = SpecUtil.mockReduxStore(initState);
    store.dispatch(updateLicenseNameFilter({ filter: 'test' }));

    const actions = store.getActions();
    expect(actions.length).toBe(1);
    expect(actions[0].type).toBe(LEGAL_APPLICATION_DETAILS_SET_LICENSE_NAME_FILTER);
    expect(actions[0].payload).toEqual({ filter: 'test' });
  });
});
