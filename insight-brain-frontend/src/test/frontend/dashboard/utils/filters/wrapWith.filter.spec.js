/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import dashboardUtilsModule from '../../../../../main/frontend/dashboard/utils/dashboard.utils.module';

describe('wrapWith.filter.spec', function() {

  beforeEach(angular.mock.module(dashboardUtilsModule.name));

  var wrapWith;

  beforeEach(inject(function($filter) {
    wrapWith = $filter('wrapWith');
  }));

  it('should wrap non-empty string with supplied prefix and suffix', function() {
    expect(wrapWith('boo', 'ba', 'n')).toEqual('baboon');
  });

  it('should return emtpy string if applied on one', function() {
    expect(wrapWith('', 'pre-', '-post')).toEqual('');
  });
});
