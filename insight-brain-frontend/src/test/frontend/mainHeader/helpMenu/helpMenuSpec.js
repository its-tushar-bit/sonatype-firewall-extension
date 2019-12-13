/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import mainHeaderModule from '../../../../main/frontend/mainHeader/module';

describe('helpMenu', function() {
  var vm;

  beforeEach(angular.mock.module(mainHeaderModule.name));

  beforeEach(inject(function($componentController) {
    window.clmServerVersion = '1.2.3-4';
    vm = $componentController('helpMenu');
  }));

  it('Major Minor Version', function () {
    expect(vm.majorMinorVersion).toEqual('1.2');
  });
});
