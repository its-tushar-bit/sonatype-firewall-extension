/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import dashboardUtilsModule from '../../../../../main/frontend/dashboard/utils/dashboard.utils.module';

describe('stageFilter.filter.spec', function () {
  beforeEach(angular.mock.module(dashboardUtilsModule.name));

  it('empty filter', inject(function ($filter) {
    var stageList = [{ id: 'operate' }, { id: 'build' }, { id: 'release' }, { id: 'stage-release' }],
      result;

    // null filter
    result = $filter('stageFilter')(stageList);
    expect(result).toEqual(stageList);

    // empty filter
    result = $filter('stageFilter')(stageList, { stageTypeFilters: [] });
    expect(result).toEqual(stageList);
  }));

  it('filter', inject(function ($filter) {
    var stageList = [{ id: 'build' }, { id: 'stage-release' }, { id: 'release' }, { id: 'operate' }],
      result;

    result = $filter('stageFilter')(stageList, {
      stageTypeFilters: ['release', 'build'],
    });
    expect(result).toEqual([{ id: 'build' }, { id: 'release' }]);
  }));
});
