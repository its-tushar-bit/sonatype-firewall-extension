/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import configurationModule from '../../../../main/frontend/configuration/module';
import systemNoticeModule from '../../../../main/frontend/systemNotice/systemNoticeModule';
import SystemNoticeMockData from '../../systemNotice/systemNoticeMockData';

describe('systemNoticeConfigurationControllerSpec.js', function () {
  beforeEach(
    angular.mock.module(systemNoticeModule.name, configurationModule.name, function ($provide) {
      SpecUtil.mockPermissionService($provide);
      $provide.value('$cookies', {
        get: angular.noop,
      });
    })
  );

  var $rootScope, $scope, systemNoticeService, getSystemNoticeDeferred, saveSystemNoticeDeferred, vm;

  beforeEach(inject(function (_$rootScope_, _systemNoticeService_, $q, $componentController) {
    $rootScope = _$rootScope_;
    $scope = $rootScope.$new();
    systemNoticeService = _systemNoticeService_;
    getSystemNoticeDeferred = $q.defer();
    saveSystemNoticeDeferred = $q.defer();
    spyOn(systemNoticeService, 'getSystemNotice').and.returnValue(getSystemNoticeDeferred.promise);
    spyOn(systemNoticeService, 'saveSystemNotice').and.returnValue(saveSystemNoticeDeferred.promise);
    vm = $componentController('systemNoticeConfiguration');
  }));

  afterEach(function () {
    $scope.$destroy();
  });

  describe('loading the system notice', function () {
    it('loads it if the request succeeds', function () {
      getSystemNoticeDeferred.resolve(SystemNoticeMockData.getSystemNotice('message', true));
      $scope.$apply();

      expect(systemNoticeService.getSystemNotice).toHaveBeenCalled();
      expect(vm.systemNotice.message).toEqual('message');
      expect(vm.systemNotice.enabled).toBe(true);
    });

    it('sets the error and loads the default values if the request fails', function () {
      getSystemNoticeDeferred.reject({ status: 404, data: 'not found' });
      $scope.$apply();

      expect(systemNoticeService.getSystemNotice).toHaveBeenCalled();
      expect(vm.systemNotice.message).toEqual('Error: could not get the system notice from the server');
      expect(vm.systemNotice.enabled).toBe(true);
      expect(vm.error.status).toEqual(404);
      expect(vm.error.data).toEqual('not found');
    });

    it('deletes any error', function () {
      getSystemNoticeDeferred.resolve(SystemNoticeMockData.getSystemNotice('message', true));
      $scope.$apply();
      vm.error = 'error';
      vm.load();

      expect(systemNoticeService.getSystemNotice.calls.count()).toBe(2);
      expect(vm.error).toBeUndefined();
    });

    it('sets the loaded flag after a successful load', function () {
      vm.loaded = true;
      vm.load();

      expect(vm.loaded).toBe(false);

      getSystemNoticeDeferred.resolve(SystemNoticeMockData.getSystemNotice('message', true));
      $scope.$apply();

      expect(vm.loaded).toBe(true);
    });

    it('sets the loaded flag after an unsuccessful load', function () {
      vm.loaded = true;
      vm.load();

      expect(vm.loaded).toBe(false);

      getSystemNoticeDeferred.reject({ status: 404, data: 'not found' });
      $scope.$apply();

      expect(vm.loaded).toBe(true);
    });
  });

  describe('saving the system notice', function () {
    it('sends it to the server, updates its saved values, and broadcasts that it has been updated', function () {
      getSystemNoticeDeferred.resolve(SystemNoticeMockData.getSystemNotice('message', true));
      $scope.$apply();
      vm.systemNotice.message = 'updated message';
      vm.systemNotice.enabled = true;
      spyOn($rootScope, '$broadcast');
      vm.save();
      saveSystemNoticeDeferred.resolve({
        enabled: true,
        message: 'saved message',
      });
      expect($rootScope.$broadcast).not.toHaveBeenCalledWith('systemNoticeUpdated', vm.systemNotice);
      $scope.$apply();

      expect(systemNoticeService.getSystemNotice).toHaveBeenCalled();
      expect(systemNoticeService.saveSystemNotice).toHaveBeenCalledWith(vm.systemNotice);
      expect(vm.error).toBeUndefined();
      expect(vm.savedSystemNotice.message).toEqual('saved message');
      expect(vm.savedSystemNotice.enabled).toBe(true);
      expect($rootScope.$broadcast).toHaveBeenCalledWith(
        'systemNoticeUpdated',
        SystemNoticeMockData.getSystemNotice('saved message', true)
      );
    });

    it('sets the error if it fails to send it to the server', function () {
      getSystemNoticeDeferred.resolve(SystemNoticeMockData.getSystemNotice('message', true));
      $scope.$apply();
      vm.systemNotice.message = 'updated message';
      vm.systemNotice.enabled = true;
      vm.save();
      saveSystemNoticeDeferred.reject({ status: 401, data: 'unauthorized' });
      $scope.$apply();

      expect(systemNoticeService.getSystemNotice).toHaveBeenCalled();
      expect(systemNoticeService.saveSystemNotice).toHaveBeenCalledWith(vm.systemNotice);
      expect(vm.error.status).toEqual(401);
      expect(vm.error.data).toEqual('unauthorized');
    });

    it('deletes any error', function () {
      getSystemNoticeDeferred.resolve(SystemNoticeMockData.getSystemNotice('message', true));
      $scope.$apply();
      vm.error = 'error';
      vm.save();
      saveSystemNoticeDeferred.resolve({ status: 204, data: 'no content' });
      $scope.$apply();

      expect(systemNoticeService.getSystemNotice).toHaveBeenCalled();
      expect(systemNoticeService.saveSystemNotice).toHaveBeenCalledWith(vm.systemNotice);
      expect(vm.error).toBeUndefined();
    });
  });

  it('reverts the system notice to its original data on cancel', function () {
    getSystemNoticeDeferred.resolve(SystemNoticeMockData.getSystemNotice('message', true));
    $scope.$apply();
    vm.systemNotice.message = 'updated message';
    vm.systemNotice.enabled = false;
    vm.cancel();

    expect(systemNoticeService.getSystemNotice).toHaveBeenCalled();
    expect(vm.systemNotice.message).toEqual('message');
    expect(vm.systemNotice.enabled).toBe(true);
  });

  describe('checking if there are changes by calling isChanged', function () {
    it('returns true if the savedSystemNotice does not equal the systemNotice', function () {
      getSystemNoticeDeferred.resolve(SystemNoticeMockData.getSystemNotice('message', true));
      $scope.$apply();

      expect(systemNoticeService.getSystemNotice).toHaveBeenCalled();

      vm.systemNotice.message = 'updated message';
      vm.systemNotice.enabled = false;
      expect(vm.isChanged()).toBe(true);

      vm.systemNotice.message = 'updated message';
      vm.systemNotice.enabled = true;
      expect(vm.isChanged()).toBe(true);

      vm.systemNotice.message = 'message';
      vm.systemNotice.enabled = false;
      expect(vm.isChanged()).toBe(true);
    });

    it('returns false if the savedSystemNotice equals the systemNotice', function () {
      getSystemNoticeDeferred.resolve(SystemNoticeMockData.getSystemNotice('message', true));
      $scope.$apply();

      expect(systemNoticeService.getSystemNotice).toHaveBeenCalled();
      expect(vm.isChanged()).toBe(false);
    });
  });
});
