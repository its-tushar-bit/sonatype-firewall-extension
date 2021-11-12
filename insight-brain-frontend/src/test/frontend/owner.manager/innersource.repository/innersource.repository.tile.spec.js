/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import innerSourceRepositoryModule from '../../../../main/frontend/owner.manager/innersource.repository/module';
import utilityModule from '../../../../main/frontend/utility/utility.module';
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('innerSourceRepositoryTile', function () {
  let $rootScope,
    $scope,
    EventNameConstant,
    $q,
    $componentController,
    mockCLMContextLocations,
    mockOrganizationStore,
    mockApplicationStore,
    getByIdDeferred,
    vm,
    mockInnerSourceRepositoryService,
    getRepositoryConnectionsDeferred,
    mockProductFeatures,
    loadProductFeaturesDeferred;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
    })
  );

  beforeEach(angular.mock.module(innerSourceRepositoryModule.name, utilityModule.name));

  beforeEach(inject(function (_$rootScope_, $injector, _$componentController_, _$q_) {
    $rootScope = _$rootScope_;
    $scope = $rootScope.$new();
    EventNameConstant = $injector.get('event.name.constant');
    mockCLMContextLocations = jasmine.createSpyObj('CLMContextLocations', [
      'isOrganization',
      'isApplication',
      'getEntityId',
    ]);
    mockOrganizationStore = jasmine.createSpyObj('mockOrganizationStore', ['getById']);
    mockApplicationStore = jasmine.createSpyObj('mockApplicationsStore', ['getById']);
    mockInnerSourceRepositoryService = jasmine.createSpyObj('mockInnerSourceRepositoryService', [
      'getRepositoryConnections',
    ]);
    $componentController = _$componentController_;
    $q = _$q_;
    getByIdDeferred = $q.defer();
    getRepositoryConnectionsDeferred = $q.defer();
    mockProductFeatures = jasmine.createSpyObj('mockProductFeatures', ['isAvailable', 'load']);
    loadProductFeaturesDeferred = $q.defer();
    mockProductFeatures.load.and.returnValue(loadProductFeaturesDeferred.promise);
    mockProductFeatures.isAvailable.and.callFake(function (feature) {
      return feature === 'inner-source-repository-integration';
    });
  }));

  function initializeVm() {
    vm = $componentController('innerSourceRepositoryTile', {
      $scope: $scope,
      CLMContextLocations: mockCLMContextLocations,
      OrganizationStore: mockOrganizationStore,
      ApplicationStore: mockApplicationStore,
      InnerSourceRepositoryService: mockInnerSourceRepositoryService,
      ProductFeatures: mockProductFeatures,
    });
  }

  function mockOrganization() {
    mockCLMContextLocations.isOrganization.and.returnValue(true);
    mockCLMContextLocations.isApplication.and.returnValue(false);
    mockCLMContextLocations.getEntityId.and.returnValue('organizationId');
    mockOrganizationStore.getById.and.callFake(function (id) {
      return id === 'organizationId' ? getByIdDeferred.promise : null;
    });
    mockInnerSourceRepositoryService.getRepositoryConnections.and.callFake(function (ownerType, ownerId) {
      return ownerType === 'organization' && ownerId === 'organizationId'
        ? getRepositoryConnectionsDeferred.promise
        : null;
    });
    initializeVm();
  }

  function mockApplication() {
    mockCLMContextLocations.isOrganization.and.returnValue(false);
    mockCLMContextLocations.isApplication.and.returnValue(true);
    mockCLMContextLocations.getEntityId.and.returnValue('applicationId');
    mockApplicationStore.getById.and.callFake(function (id) {
      return id === 'applicationId' ? getByIdDeferred.promise : null;
    });
    mockInnerSourceRepositoryService.getRepositoryConnections.and.callFake(function (ownerType, ownerId) {
      return ownerType === 'application' && ownerId === 'applicationInternalId'
        ? getRepositoryConnectionsDeferred.promise
        : null;
    });
    initializeVm();
  }

  it('sets the expected org and app owner flags for an organization', function () {
    mockOrganization();
    expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
    expect(mockCLMContextLocations.isApplication).toHaveBeenCalled();

    expect(vm.isOrg).toBe(true);
    expect(vm.isApp).toBe(false);
  });

  it('sets the expected org and app owner flags for an application', function () {
    mockApplication();
    expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
    expect(mockCLMContextLocations.isApplication).toHaveBeenCalled();

    expect(vm.isOrg).toBe(false);
    expect(vm.isApp).toBe(true);
  });

  it('reloads on broadcasted owner summary reload event', function () {
    mockOrganization();

    expect(mockCLMContextLocations.getEntityId).toHaveBeenCalledTimes(1);

    $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

    expect(mockCLMContextLocations.getEntityId).toHaveBeenCalledTimes(2);
  });

  it('loads nothing if it is not an organization or application', function () {
    mockCLMContextLocations.isOrganization.and.returnValue(false);
    mockCLMContextLocations.isApplication.and.returnValue(false);

    initializeVm();

    expect(mockCLMContextLocations.getEntityId).not.toHaveBeenCalled();
    expect(vm.loading).toBeFalsy();
  });

  describe('load', function () {
    describe('organization', function () {
      beforeEach(inject(function () {
        mockOrganization();
      }));

      it('loads the first repository connection', function () {
        getByIdDeferred.resolve({
          id: 'organizationId',
        });
        loadProductFeaturesDeferred.resolve(['inner-source-repository-integration']);
        getRepositoryConnectionsDeferred.resolve([
          { baseUrl: 'https://some.base.url.1' },
          { baseUrl: 'https://some.base.url.2' },
        ]);

        $scope.$digest();

        expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
        expect(mockOrganizationStore.getById).toHaveBeenCalledWith('organizationId');
        expect(mockProductFeatures.load).toHaveBeenCalled();
        expect(mockProductFeatures.isAvailable).toHaveBeenCalledWith('inner-source-repository-integration');
        expect(mockInnerSourceRepositoryService.getRepositoryConnections).toHaveBeenCalledWith(
          'organization',
          'organizationId'
        );
        expect(vm.error).toBeUndefined();
        expect(vm.loading).toBeFalsy();
        expect(vm.isInnerSourceRepositorySupported).toBeTruthy();
        expect(vm.innerSourceRepository).toEqual({ baseUrl: 'https://some.base.url.1' });
      });

      it('sets the error message on getById failure', function () {
        getByIdDeferred.reject({ status: 404, data: 'not found' });
        loadProductFeaturesDeferred.resolve(['inner-source-repository-integration']);
        getRepositoryConnectionsDeferred.resolve([
          { baseUrl: 'https://some.base.url.1' },
          { baseUrl: 'https://some.base.url.2' },
        ]);

        $scope.$digest();

        expect(vm.error).toEqual('not found');
        expect(vm.loading).toBeFalsy();
      });

      it('sets the error message on loadProductFeaturesDeferred failure', function () {
        getByIdDeferred.resolve({
          id: 'organizationId',
        });
        loadProductFeaturesDeferred.reject({ status: 404, data: 'not found' });
        getRepositoryConnectionsDeferred.resolve([
          { baseUrl: 'https://some.base.url.1' },
          { baseUrl: 'https://some.base.url.2' },
        ]);

        $scope.$digest();

        expect(vm.error).toEqual('not found');
        expect(vm.loading).toBeFalsy();
      });

      it('sets the error message on getRepositoryConnectionsDeferred failure', function () {
        getByIdDeferred.resolve({
          id: 'organizationId',
        });
        loadProductFeaturesDeferred.resolve(['inner-source-repository-integration']);
        getRepositoryConnectionsDeferred.reject({ status: 404, data: 'not found' });

        $scope.$digest();

        expect(vm.error).toEqual('not found');
        expect(vm.loading).toBeFalsy();
      });

      it('skips loading the repository connections if the feature is disabled', function () {
        getByIdDeferred.resolve({
          id: 'organizationId',
        });
        loadProductFeaturesDeferred.resolve([]);
        mockProductFeatures.isAvailable.and.returnValue(false);

        $scope.$digest();

        expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
        expect(mockOrganizationStore.getById).toHaveBeenCalledWith('organizationId');
        expect(mockProductFeatures.load).toHaveBeenCalled();
        expect(mockProductFeatures.isAvailable).toHaveBeenCalledWith('inner-source-repository-integration');
        expect(mockInnerSourceRepositoryService.getRepositoryConnections).not.toHaveBeenCalled();
        expect(vm.error).toBeUndefined();
        expect(vm.loading).toBeFalsy();
        expect(vm.isInnerSourceRepositorySupported).toBeFalsy();
        expect(vm.innerSourceRepository).toBeUndefined();
      });
    });

    describe('application', function () {
      beforeEach(inject(function () {
        mockApplication();
      }));

      it('loads the first repository connection', function () {
        getByIdDeferred.resolve({
          id: 'applicationInternalId',
        });
        loadProductFeaturesDeferred.resolve(['inner-source-repository-integration']);
        getRepositoryConnectionsDeferred.resolve([
          { baseUrl: 'https://some.base.url.1' },
          { baseUrl: 'https://some.base.url.2' },
        ]);

        $scope.$digest();

        expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
        expect(mockApplicationStore.getById).toHaveBeenCalledWith('applicationId');
        expect(mockProductFeatures.load).toHaveBeenCalled();
        expect(mockProductFeatures.isAvailable).toHaveBeenCalledWith('inner-source-repository-integration');
        expect(mockInnerSourceRepositoryService.getRepositoryConnections).toHaveBeenCalledWith(
          'application',
          'applicationInternalId'
        );
        expect(vm.error).toBeUndefined();
        expect(vm.loading).toBeFalsy();
        expect(vm.isInnerSourceRepositorySupported).toBeTruthy();
        expect(vm.innerSourceRepository).toEqual({ baseUrl: 'https://some.base.url.1' });
      });

      it('sets the error message on getById failure', function () {
        getByIdDeferred.reject({ status: 404, data: 'not found' });
        loadProductFeaturesDeferred.resolve(['inner-source-repository-integration']);
        getRepositoryConnectionsDeferred.resolve([
          { baseUrl: 'https://some.base.url.1' },
          { baseUrl: 'https://some.base.url.2' },
        ]);

        $scope.$digest();

        expect(vm.error).toEqual('not found');
        expect(vm.loading).toBeFalsy();
      });

      it('sets the error message on loadProductFeaturesDeferred failure', function () {
        getByIdDeferred.resolve({
          id: 'applicationInternalId',
        });
        loadProductFeaturesDeferred.reject({ status: 404, data: 'not found' });
        getRepositoryConnectionsDeferred.resolve([
          { baseUrl: 'https://some.base.url.1' },
          { baseUrl: 'https://some.base.url.2' },
        ]);

        $scope.$digest();

        expect(vm.error).toEqual('not found');
        expect(vm.loading).toBeFalsy();
      });

      it('sets the error message on getRepositoryConnectionsDeferred failure', function () {
        getByIdDeferred.resolve({
          id: 'applicationInternalId',
        });
        loadProductFeaturesDeferred.resolve(['inner-source-repository-integration']);
        getRepositoryConnectionsDeferred.reject({ status: 404, data: 'not found' });

        $scope.$digest();

        expect(vm.error).toEqual('not found');
        expect(vm.loading).toBeFalsy();
      });

      it('skips loading the repository connections if the feature is disabled', function () {
        getByIdDeferred.resolve({
          id: 'applicationInternalId',
        });
        loadProductFeaturesDeferred.resolve([]);
        mockProductFeatures.isAvailable.and.returnValue(false);

        $scope.$digest();

        expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
        expect(mockApplicationStore.getById).toHaveBeenCalledWith('applicationId');
        expect(mockProductFeatures.load).toHaveBeenCalled();
        expect(mockProductFeatures.isAvailable).toHaveBeenCalledWith('inner-source-repository-integration');
        expect(mockInnerSourceRepositoryService.getRepositoryConnections).not.toHaveBeenCalled();
        expect(vm.error).toBeUndefined();
        expect(vm.loading).toBeFalsy();
        expect(vm.isInnerSourceRepositorySupported).toBeFalsy();
        expect(vm.innerSourceRepository).toBeUndefined();
      });
    });
  });
});
