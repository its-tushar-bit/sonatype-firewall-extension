/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../../main/frontend/owner.manager/owner.manager.module';

describe('GrandfatherModalController', function () {
  let scope, vm, $httpBackend, CLMLocations, $q, mockSelectedApplication;

  beforeEach(angular.mock.module(ownerManagerModule.name));

  beforeEach(inject(function ($rootScope, $controller, _$httpBackend_, _CLMLocations_, _$q_) {
    scope = $rootScope.$new();
    scope.$dismiss = jasmine.createSpy('$dismiss').and.returnValue(undefined);

    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    $q = _$q_;

    mockSelectedApplication = {
      publicId: '1234567890',
    };

    vm = $controller('GrandfatherModalController', {
      $scope: scope,
      selectedApplication: mockSelectedApplication,
    });

    vm.grandfatherMask = { wrap: SpecUtil.promiseWrapper($q) };
  }));

  it('initializes variables', function () {
    expect(vm.applicationPublicId).toEqual(mockSelectedApplication.publicId);
    expect(vm.error).toBeFalsy();
  });

  it('calls scope.$dismiss when the server responds with success', function () {
    $httpBackend.expectPUT(CLMLocations.getGrandfatherUrl(mockSelectedApplication.publicId)).respond(200);

    vm.grandfather();

    $httpBackend.flush();
    expect(vm.error).toBeUndefined();
    expect(scope.$dismiss).toHaveBeenCalled();
  });

  it('sets vm.error when the server responds with error', function () {
    $httpBackend
      .expectPUT(CLMLocations.getGrandfatherUrl(mockSelectedApplication.publicId))
      .respond(500, 'Some failure');

    vm.grandfather();

    $httpBackend.flush();
    expect(vm.error).toEqual('Some failure');
    expect(scope.$dismiss).not.toHaveBeenCalled();
  });
});
