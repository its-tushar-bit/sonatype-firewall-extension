/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import innerSourceRepositoryModule from 'MainRoot/owner.manager/innersource.repository/module';
import utilityModule from 'MainRoot/utility/utility.module';
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';

describe('innerSourceRepositoryTile', function () {
  let $rootScope,
    $scope,
    EventNameConstant,
    $q,
    $componentController,
    mockCLMContextLocations,
    mockOrganizationStore,
    mockApplicationStore,
    getByIdDeferred1,
    getByIdDeferred2,
    vm,
    mockInnerSourceRepositoryService,
    getRepositoryConnectionsDeferred;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
      SpecUtil.mockNgRedux($provide);
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
    spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });

    mockOrganizationStore = jasmine.createSpyObj('mockOrganizationStore', ['getById']);
    mockApplicationStore = jasmine.createSpyObj('mockApplicationsStore', ['getById']);
    mockInnerSourceRepositoryService = jasmine.createSpyObj('mockInnerSourceRepositoryService', [
      'getRepositoryConnections',
    ]);
    $componentController = _$componentController_;
    $q = _$q_;
    getByIdDeferred1 = $q.defer();
    getByIdDeferred2 = $q.defer();
    getRepositoryConnectionsDeferred = $q.defer();
  }));

  function initializeVm() {
    vm = $componentController('innerSourceRepositoryTile', {
      $scope: $scope,
      CLMContextLocations: mockCLMContextLocations,
      OrganizationStore: mockOrganizationStore,
      ApplicationStore: mockApplicationStore,
      InnerSourceRepositoryService: mockInnerSourceRepositoryService,
    });
    vm.isInnerSourceRepositorySupported = true;
  }

  function mockOrganization() {
    mockCLMContextLocations.isOrganization.and.returnValue(true);
    mockCLMContextLocations.isApplication.and.returnValue(false);
    mockCLMContextLocations.getEntityId.and.returnValue('organizationId');
    mockOrganizationStore.getById.and.callFake(function (id) {
      if (id === 'organizationId') {
        return getByIdDeferred1.promise;
      }
      if (id === 'otherOwnerId') {
        return getByIdDeferred2.promise;
      }
      return null;
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
      return id === 'applicationId' ? getByIdDeferred1.promise : null;
    });
    mockOrganizationStore.getById.and.callFake(function (id) {
      if (id === 'organizationId') {
        return getByIdDeferred1.promise;
      }
      if (id === 'otherOwnerId') {
        return getByIdDeferred2.promise;
      }
      return null;
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

      it('does not load the repository connections if disabled', function () {
        getByIdDeferred1.resolve({
          id: 'organizationId',
        });
        getRepositoryConnectionsDeferred.resolve({
          repositoryConnectionStatus: {
            inheritedFromOrganizationName: null,
            inheritedFromOrgEnabled: null,
            enabled: false,
            allowChange: true,
          },
          repositoryConnections: [
            { ownerId: 'organizationId', baseUrl: 'https://some.base.url.1' },
            { ownerId: 'organizationId', baseUrl: 'https://some.base.url.2' },
          ],
        });

        $scope.$digest();

        expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
        expect(mockOrganizationStore.getById).toHaveBeenCalledWith('organizationId');
        expect(mockInnerSourceRepositoryService.getRepositoryConnections).toHaveBeenCalledWith(
          'organization',
          'organizationId',
          true
        );
        expect(vm.error).toBeUndefined();
        expect(vm.loading).toBeFalsy();
        expect(vm.isInnerSourceRepositorySupported).toBeTruthy();
        expect(vm.innerSourceRepositories).toEqual([
          { ownerId: 'organizationId', baseUrl: 'https://some.base.url.1' },
          { ownerId: 'organizationId', baseUrl: 'https://some.base.url.2' },
        ]);
        expect(vm.innerSourceRepositoriesEnabled).toBeFalsy();
      });

      it('loads the repository connections', function () {
        getByIdDeferred1.resolve({
          id: 'organizationId',
        });
        getRepositoryConnectionsDeferred.resolve({
          repositoryConnectionStatus: {
            inheritedFromOrganizationName: null,
            inheritedFromOrgEnabled: null,
            enabled: true,
            allowChange: true,
          },
          repositoryConnections: [
            { ownerId: 'organizationId', baseUrl: 'https://some.base.url.1' },
            { ownerId: 'organizationId', baseUrl: 'https://some.base.url.2' },
          ],
        });

        $scope.$digest();

        expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
        expect(mockOrganizationStore.getById).toHaveBeenCalledWith('organizationId');
        expect(mockInnerSourceRepositoryService.getRepositoryConnections).toHaveBeenCalledWith(
          'organization',
          'organizationId',
          true
        );
        expect(vm.error).toBeUndefined();
        expect(vm.loading).toBeFalsy();
        expect(vm.isInnerSourceRepositorySupported).toBeTruthy();
        expect(vm.innerSourceRepositories).toEqual([
          { ownerId: 'organizationId', baseUrl: 'https://some.base.url.1' },
          { ownerId: 'organizationId', baseUrl: 'https://some.base.url.2' },
        ]);
        expect(vm.innerSourceRepositoriesEnabled).toBeTruthy();
      });

      it('does not load the repository connections if inherited disabled', function () {
        getByIdDeferred1.resolve({
          id: 'organizationId',
        });
        getRepositoryConnectionsDeferred.resolve({
          repositoryConnectionStatus: {
            inheritedFromOrganizationName: 'otherOwnerName',
            inheritedFromOrgEnabled: false,
            enabled: null,
            allowChange: true,
          },
          repositoryConnections: [
            { ownerId: 'otherOwnerId', baseUrl: 'https://some.base.url.1' },
            { ownerId: 'otherOwnerId', baseUrl: 'https://some.base.url.2' },
          ],
        });

        $scope.$digest();

        expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
        expect(mockOrganizationStore.getById).toHaveBeenCalledWith('organizationId');
        expect(mockInnerSourceRepositoryService.getRepositoryConnections).toHaveBeenCalledWith(
          'organization',
          'organizationId',
          true
        );
        expect(vm.error).toBeUndefined();
        expect(vm.loading).toBeFalsy();
        expect(vm.isInnerSourceRepositorySupported).toBeTruthy();
        expect(vm.innerSourceRepositoriesInheritedFrom).toEqual('otherOwnerName');
        expect(vm.innerSourceRepositories).toEqual([
          { ownerId: 'otherOwnerId', baseUrl: 'https://some.base.url.1' },
          { ownerId: 'otherOwnerId', baseUrl: 'https://some.base.url.2' },
        ]);
        expect(vm.innerSourceRepositoriesEnabled).toBeFalsy();
      });

      it('sets inherited from to the ownerName if the ownerId is different', function () {
        getByIdDeferred1.resolve({
          id: 'organizationId',
        });
        getRepositoryConnectionsDeferred.resolve({
          repositoryConnectionStatus: {
            inheritedFromOrganizationName: 'otherOwnerName',
            inheritedFromOrgEnabled: true,
            enabled: null,
            allowChange: true,
          },
          repositoryConnections: [
            { ownerId: 'otherOwnerId', baseUrl: 'https://some.base.url.1' },
            { ownerId: 'otherOwnerId', baseUrl: 'https://some.base.url.2' },
          ],
        });

        $scope.$digest();

        expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
        expect(mockOrganizationStore.getById).toHaveBeenCalledWith('organizationId');
        expect(mockInnerSourceRepositoryService.getRepositoryConnections).toHaveBeenCalledWith(
          'organization',
          'organizationId',
          true
        );
        expect(vm.error).toBeUndefined();
        expect(vm.loading).toBeFalsy();
        expect(vm.isInnerSourceRepositorySupported).toBeTruthy();
        expect(vm.innerSourceRepositoriesInheritedFrom).toBe('otherOwnerName');
        expect(vm.innerSourceRepositories).toEqual([
          { ownerId: 'otherOwnerId', baseUrl: 'https://some.base.url.1' },
          { ownerId: 'otherOwnerId', baseUrl: 'https://some.base.url.2' },
        ]);
        expect(vm.innerSourceRepositoriesEnabled).toBeTruthy();
      });

      it('sets the error message on getById failure', function () {
        getByIdDeferred1.reject({ status: 404, data: 'not found' });
        getRepositoryConnectionsDeferred.resolve({
          repositoryConnections: [
            { ownerId: 'organizationId', baseUrl: 'https://some.base.url.1' },
            { ownerId: 'organizationId', baseUrl: 'https://some.base.url.2' },
          ],
        });

        $scope.$digest();

        expect(vm.error).toEqual('not found');
        expect(vm.loading).toBeFalsy();
      });

      it('sets the error message on getRepositoryConnectionsDeferred failure', function () {
        getByIdDeferred1.resolve({
          id: 'organizationId',
        });
        getRepositoryConnectionsDeferred.reject({ status: 404, data: 'not found' });

        $scope.$digest();

        expect(vm.error).toEqual('not found');
        expect(vm.loading).toBeFalsy();
      });

      it('skips loading the repository connections if the feature is disabled', function () {
        getByIdDeferred1.resolve({
          id: 'organizationId',
        });
        vm.isInnerSourceRepositorySupported = false;

        $scope.$digest();

        expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
        expect(mockOrganizationStore.getById).toHaveBeenCalledWith('organizationId');
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

      it('does not load the repository connections if disabled', function () {
        getByIdDeferred1.resolve({
          id: 'applicationInternalId',
        });
        getRepositoryConnectionsDeferred.resolve({
          repositoryConnectionStatus: {
            inheritedFromOrganizationName: null,
            inheritedFromOrgEnabled: null,
            enabled: false,
            allowChange: true,
          },
          repositoryConnections: [
            { ownerId: 'applicationInternalId', baseUrl: 'https://some.base.url.1' },
            { ownerId: 'applicationInternalId', baseUrl: 'https://some.base.url.2' },
          ],
        });

        $scope.$digest();

        expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
        expect(mockApplicationStore.getById).toHaveBeenCalledWith('applicationId');
        expect(mockInnerSourceRepositoryService.getRepositoryConnections).toHaveBeenCalledWith(
          'application',
          'applicationInternalId',
          true
        );
        expect(vm.error).toBeUndefined();
        expect(vm.loading).toBeFalsy();
        expect(vm.isInnerSourceRepositorySupported).toBeTruthy();
        expect(vm.innerSourceRepositories).toEqual([
          { ownerId: 'applicationInternalId', baseUrl: 'https://some.base.url.1' },
          { ownerId: 'applicationInternalId', baseUrl: 'https://some.base.url.2' },
        ]);
        expect(vm.innerSourceRepositoriesEnabled).toBeFalsy();
      });

      it('loads the repository connections', function () {
        getByIdDeferred1.resolve({
          id: 'applicationInternalId',
        });
        getRepositoryConnectionsDeferred.resolve({
          repositoryConnectionStatus: {
            inheritedFromOrganizationName: null,
            inheritedFromOrgEnabled: null,
            enabled: true,
            allowChange: true,
          },
          repositoryConnections: [
            { ownerId: 'applicationInternalId', baseUrl: 'https://some.base.url.1' },
            { ownerId: 'applicationInternalId', baseUrl: 'https://some.base.url.2' },
          ],
        });

        $scope.$digest();

        expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
        expect(mockApplicationStore.getById).toHaveBeenCalledWith('applicationId');
        expect(mockInnerSourceRepositoryService.getRepositoryConnections).toHaveBeenCalledWith(
          'application',
          'applicationInternalId',
          true
        );
        expect(vm.error).toBeUndefined();
        expect(vm.loading).toBeFalsy();
        expect(vm.isInnerSourceRepositorySupported).toBeTruthy();
        expect(vm.innerSourceRepositories).toEqual([
          { ownerId: 'applicationInternalId', baseUrl: 'https://some.base.url.1' },
          { ownerId: 'applicationInternalId', baseUrl: 'https://some.base.url.2' },
        ]);
        expect(vm.innerSourceRepositoriesEnabled).toBeTruthy();
      });

      it('does not show the repository connections if inherited disabled', function () {
        getByIdDeferred1.resolve({
          id: 'applicationInternalId',
        });
        getRepositoryConnectionsDeferred.resolve({
          repositoryConnectionStatus: {
            inheritedFromOrganizationName: 'otherOwnerName',
            inheritedFromOrgEnabled: false,
            enabled: null,
            allowChange: true,
          },
          repositoryConnections: [
            { ownerId: 'otherOwnerId', baseUrl: 'https://some.base.url.1' },
            { ownerId: 'otherOwnerId', baseUrl: 'https://some.base.url.2' },
          ],
        });

        $scope.$digest();

        expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
        expect(mockApplicationStore.getById).toHaveBeenCalledWith('applicationId');
        expect(mockInnerSourceRepositoryService.getRepositoryConnections).toHaveBeenCalledWith(
          'application',
          'applicationInternalId',
          true
        );
        expect(vm.error).toBeUndefined();
        expect(vm.loading).toBeFalsy();
        expect(vm.isInnerSourceRepositorySupported).toBeTruthy();
        expect(vm.innerSourceRepositoriesInheritedFrom).toEqual('otherOwnerName');
        expect(vm.innerSourceRepositories).toEqual([
          { ownerId: 'otherOwnerId', baseUrl: 'https://some.base.url.1' },
          { ownerId: 'otherOwnerId', baseUrl: 'https://some.base.url.2' },
        ]);
        expect(vm.innerSourceRepositoriesEnabled).toBeFalsy();
      });

      it('sets inherited from to the ownerName if the ownerId is different', function () {
        getByIdDeferred1.resolve({
          id: 'applicationInternalId',
        });
        getRepositoryConnectionsDeferred.resolve({
          repositoryConnectionStatus: {
            inheritedFromOrganizationName: 'otherOwnerName',
            inheritedFromOrgEnabled: true,
            enabled: null,
            allowChange: true,
          },
          repositoryConnections: [
            { ownerId: 'otherOwnerId', baseUrl: 'https://some.base.url.1' },
            { ownerId: 'otherOwnerId', baseUrl: 'https://some.base.url.2' },
          ],
        });

        $scope.$digest();

        expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
        expect(mockApplicationStore.getById).toHaveBeenCalledWith('applicationId');
        expect(mockInnerSourceRepositoryService.getRepositoryConnections).toHaveBeenCalledWith(
          'application',
          'applicationInternalId',
          true
        );
        expect(vm.error).toBeUndefined();
        expect(vm.loading).toBeFalsy();
        expect(vm.isInnerSourceRepositorySupported).toBeTruthy();
        expect(vm.innerSourceRepositoriesInheritedFrom).toBe('otherOwnerName');
        expect(vm.innerSourceRepositories).toEqual([
          { ownerId: 'otherOwnerId', baseUrl: 'https://some.base.url.1' },
          { ownerId: 'otherOwnerId', baseUrl: 'https://some.base.url.2' },
        ]);
        expect(vm.innerSourceRepositoriesEnabled).toBeTruthy();
      });

      it('sets the error message on getById failure', function () {
        getByIdDeferred1.reject({ status: 404, data: 'not found' });
        getRepositoryConnectionsDeferred.resolve({
          repositoryConnections: [
            { ownerId: 'applicationInternalId', baseUrl: 'https://some.base.url.1' },
            { ownerId: 'applicationInternalId', baseUrl: 'https://some.base.url.2' },
          ],
        });

        $scope.$digest();

        expect(vm.error).toEqual('not found');
        expect(vm.loading).toBeFalsy();
      });

      it('sets the error message on getRepositoryConnectionsDeferred failure', function () {
        getByIdDeferred1.resolve({
          id: 'applicationInternalId',
        });
        getRepositoryConnectionsDeferred.reject({ status: 404, data: 'not found' });

        $scope.$digest();

        expect(vm.error).toEqual('not found');
        expect(vm.loading).toBeFalsy();
      });

      it('skips loading the repository connections if the feature is disabled', function () {
        getByIdDeferred1.resolve({
          id: 'applicationInternalId',
        });

        vm.isInnerSourceRepositorySupported = false;
        $scope.$digest();

        expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
        expect(mockApplicationStore.getById).toHaveBeenCalledWith('applicationId');
        expect(mockInnerSourceRepositoryService.getRepositoryConnections).not.toHaveBeenCalled();
        expect(vm.error).toBeUndefined();
        expect(vm.loading).toBeFalsy();
        expect(vm.isInnerSourceRepositorySupported).toBeFalsy();
        expect(vm.innerSourceRepository).toBeUndefined();
      });
    });
  });
});
