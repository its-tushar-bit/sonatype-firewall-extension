import configurationModule from '../../../../main/frontend/configuration/module';

describe('automaticSourceControlConfigurationControllerSpec.js', function() {
  beforeEach(angular.mock.module(configurationModule.name, function($provide) {
    SpecUtil.mockPermissionService($provide);
  }));

  var $scope,
      getConfigurationDeferred,
      saveConfigurationDeferred,
      mockAutomaticSourceControlConfigurationService,
      vm;

  beforeEach(inject(function(_$rootScope_, $q, $componentController) {
    $scope = _$rootScope_.$new();
    getConfigurationDeferred = $q.defer();
    saveConfigurationDeferred = $q.defer();
    mockAutomaticSourceControlConfigurationService = {
      getConfiguration: jasmine.createSpy().and.returnValue(getConfigurationDeferred.promise),
      saveConfiguration: jasmine.createSpy().and.returnValue(saveConfigurationDeferred.promise)
    };
    vm = $componentController('automaticSourceControlConfiguration', {
      automaticSourceControlConfigurationService: mockAutomaticSourceControlConfigurationService
    });
    vm.automaticSourceControlConfigurationForm = {
      $valid: true
    };
    vm.automaticSourceControlConfigurationFormMask = {
      wrap: SpecUtil.promiseWrapper($q)
    };
  }));

  describe('loading options and settings from the server', function() {
    it('loads it if the request succeeds', function() {
      vm.$onInit();
      getConfigurationDeferred.resolve({
        enabled: true
      });
      $scope.$digest();

      expect(mockAutomaticSourceControlConfigurationService.getConfiguration).toHaveBeenCalled();
      expect(vm.automaticSourceControlEnabled).toEqual(true);
      expect(vm.savedAutomaticSourceControlEnabled).toEqual(true);
      expect(vm.loaded).toEqual(true);
    });

    it('sets the error if the request for current configuration settings fails', function() {
      vm.$onInit();
      getConfigurationDeferred.reject({status: 404, data: 'not found'});
      $scope.$digest();

      expect(mockAutomaticSourceControlConfigurationService.getConfiguration).toHaveBeenCalled();
      expect(vm.automaticSourceControlEnabled).toBe(undefined);
      expect(vm.savedAutomaticSourceControlEnabled).toBe(undefined);
      expect(vm.error.status).toEqual(404);
      expect(vm.error.data).toEqual('not found');
      expect(vm.loaded).toEqual(false);
    });

    it('deletes any error', function() {
      vm.error = {status: 404, data: 'not found'};

      vm.load();
      getConfigurationDeferred.resolve({});
      $scope.$digest();

      expect(vm.error).toBeUndefined();
    });
  });

  describe('saving settings to the server', function() {
    it('saves and updates the settings if the request succeeds', function() {
      var configuration = {
        enabled: true
      };
      expect(vm.savedAutomaticSourceControlEnabled).toBe(undefined);

      vm.automaticSourceControlEnabled = true;
      vm.save();
      saveConfigurationDeferred.resolve(configuration);
      $scope.$digest();

      expect(mockAutomaticSourceControlConfigurationService.saveConfiguration).toHaveBeenCalledWith(configuration);
      expect(vm.savedAutomaticSourceControlEnabled).toEqual(true);
    });

    it('sets the error if the save request fails', function() {
      var configuration = {
        enabled: true
      };

      vm.automaticSourceControlEnabled = true;
      vm.save();
      saveConfigurationDeferred.reject({status: 400, data: 'bad request'});
      $scope.$digest();

      expect(mockAutomaticSourceControlConfigurationService.saveConfiguration).toHaveBeenCalledWith(configuration);
      expect(vm.savedAutomaticSourceControlEnabled).toEqual(undefined);
      expect(vm.error.status).toEqual(400);
      expect(vm.error.data).toEqual('bad request');
    });

    it('deletes any error', function() {
      vm.error = {status: 404, data: 'not found'};
      vm.automaticSourceControlEnabled = false;
      vm.savedAutomaticSourceControlEnabled = true;

      vm.save();
      saveConfigurationDeferred.resolve({});
      $scope.$digest();

      expect(vm.error).toBeUndefined();
    });

    it('does not save if the form is invalid', function() {
      vm.automaticSourceControlEnabled = false;
      vm.savedAutomaticSourceControlEnabled = true;
      vm.automaticSourceControlConfigurationForm.$valid = false;
      vm.save();

      expect(mockAutomaticSourceControlConfigurationService.saveConfiguration).not.toHaveBeenCalled();
    });

    it('does not save if the form is unchanged', function() {
      vm.$onInit();
      getConfigurationDeferred.resolve({});
      $scope.$digest();

      vm.automaticSourceControlEnabled = false;
      vm.savedAutomaticSourceControlEnabled = false;
      vm.save();

      expect(mockAutomaticSourceControlConfigurationService.saveConfiguration).not.toHaveBeenCalled();
    });
  });

  describe('discarding the changes on cancel', function() {
    it('reverts any changes if the cancel button is pressed', function() {
      vm.$onInit();
      getConfigurationDeferred.resolve({
        enabled: true
      });
      $scope.$digest();

      vm.automaticSourceControlEnabled = false;
      vm.cancel();

      expect(vm.automaticSourceControlEnabled).toEqual(true);
    });
  });

  describe('checking if there are changes by calling isChanged', function() {
    it('returns true if the saved settings do not equal the current settings', function() {
      vm.automaticSourceControlEnabled = true;

      vm.savedAutomaticSourceControlEnabled = false;
      expect(vm.isChanged()).toBe(true);
    });

    it('returns false if the saved settings equal the current settings', function() {
      vm.automaticSourceControlEnabled = true;
      vm.savedAutomaticSourceControlEnabled = true;
      expect(vm.isChanged()).toBe(false);
    });
  });
});
