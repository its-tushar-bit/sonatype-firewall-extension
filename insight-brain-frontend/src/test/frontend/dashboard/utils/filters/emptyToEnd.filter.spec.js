/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import dashboardUtilsModule from '../../../../../main/frontend/dashboard/utils/dashboard.utils.module';

describe('emptyToEnd.filter.spec', function () {
  beforeEach(angular.mock.module(dashboardUtilsModule.name));

  var emptyToEnd,
    data = [{ key: null }, { key: 'value' }, { key: null }],
    expectedResult = [{ key: 'value' }, { key: null }, { key: null }];
  beforeEach(inject(function ($filter) {
    emptyToEnd = $filter('emptyToEnd');
  }));

  it('should filter all null values to the end of an array of objects', function () {
    expect(emptyToEnd(data, 'key')).toEqual(expectedResult);
  });

  it('should filter all null values to the end when given a compound key', function () {
    expect(emptyToEnd(data, ['key', 'key2'])).toEqual(expectedResult);
  });
});
