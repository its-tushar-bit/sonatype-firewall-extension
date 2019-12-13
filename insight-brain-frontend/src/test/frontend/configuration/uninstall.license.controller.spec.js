/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import productLicenseModule from '../../../main/frontend/configuration/license/ProductLicenseModule';

describe('uninstall.license.controller.spec.js', function () {
  var vm,
      scope,
      reloadSpy;

  beforeEach(angular.mock.module(productLicenseModule.name));

  beforeEach(inject(function($rootScope, $controller, $q) {
    scope = $rootScope.$new();
    reloadSpy = jasmine.createSpy('reloadSpy');

    vm = $controller('uninstall.license.controller', {
      $scope: scope,
      $window: { location: { reload: reloadSpy }}
    });

    vm.formMask = {wrap: SpecUtil.promiseWrapper($q)};
  }));

  afterEach(function() {
    scope.$destroy();
  });

  it('sets the error variable correctly upon failure', inject(function($httpBackend, CLMLocations) {
    $httpBackend.expectDELETE(CLMLocations.getLicenseUploadUrl()).respond(500, 'failed');

    vm.uninstall();
    $httpBackend.flush();

    expect(vm.submitError).toEqual('failed');
  }));

  it('reloads page upon success', inject(function($httpBackend, CLMLocations) {
    $httpBackend.expectDELETE(CLMLocations.getLicenseUploadUrl()).respond(204);

    vm.uninstall();
    $httpBackend.flush();

    expect(reloadSpy).toHaveBeenCalled();
  }));
});
