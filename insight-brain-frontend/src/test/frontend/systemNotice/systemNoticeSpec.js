/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import systemNoticeModule from '../../../main/frontend/systemNotice/systemNoticeModule';
import clmLocation from '../../../main/frontend/util/CLMLocation';
import SystemNoticeMockData from './systemNoticeMockData';

describe('systemNoticeSpec', function () {
  beforeEach(angular.mock.module(clmLocation.name, systemNoticeModule.name));

  var $rootScope, $scope, systemNoticeService, getSystemNoticeDeferred, vm;

  beforeEach(inject(function (_$rootScope_, _systemNoticeService_, $q, $componentController) {
    $rootScope = _$rootScope_;
    $scope = $rootScope.$new();
    systemNoticeService = _systemNoticeService_;
    getSystemNoticeDeferred = $q.defer();
    spyOn(systemNoticeService, 'getSystemNotice').and.returnValue(getSystemNoticeDeferred.promise);
    vm = $componentController('systemNotice', {
      systemNoticeService: systemNoticeService,
      $scope: $scope,
    });
  }));

  afterEach(function () {
    $scope.$destroy();
  });

  describe('setting the system notice when calling $onInit', function () {
    it('sets the system notice to the response on a successful request', function () {
      vm.$onInit();
      getSystemNoticeDeferred.resolve(SystemNoticeMockData.getSystemNotice('message', true));
      $scope.$apply();

      expect(systemNoticeService.getSystemNotice).toHaveBeenCalled();
      expect(vm.systemNotice).toEqual(SystemNoticeMockData.getSystemNotice('message', true));
    });

    it('sets the system notice to the default system notice on a failed request', function () {
      vm.$onInit();
      getSystemNoticeDeferred.reject();
      $scope.$apply();

      expect(systemNoticeService.getSystemNotice).toHaveBeenCalled();
      expect(vm.systemNotice).toEqual(
        SystemNoticeMockData.getSystemNotice('Error: could not get the system notice from the server', true)
      );
    });
  });

  it('sets the system notice to the given value on receiving a systemNoticeUpdated event', function () {
    $rootScope.$broadcast('systemNoticeUpdated', SystemNoticeMockData.getSystemNotice('updated message', true));

    expect(systemNoticeService.getSystemNotice).not.toHaveBeenCalled();
    expect(vm.systemNotice).toEqual(SystemNoticeMockData.getSystemNotice('updated message', true));
  });
});
