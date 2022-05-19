/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import sourceControlModule from 'MainRoot/owner.manager/source.control/module';
import utilityModule from 'MainRoot/utility/utility.module';
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { actions as applicationActions } from 'MainRoot/OrgsAndPolicies/applicationsSlice';
import { actions as organizationsActions } from 'MainRoot/OrgsAndPolicies/organizationsSlice';

describe('source.control.tile', function () {
  const ROOT_ORGANIZATION_ID = 'rootOrganizationId';
  const SUB_ORGANIZATION_ID = 'subOrganizationId';
  const APPLICATION_ID = 'applicationId';

  let $rootScope,
    $scope,
    EventNameConstant,
    $q,
    $componentController,
    mockCLMContextLocations,
    getByIdDeferred,
    vm,
    mockSourceControlService,
    getSourceControlDeferred;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(angular.mock.module(sourceControlModule.name, utilityModule.name));

  beforeEach(inject(function (_$rootScope_, $injector, _$componentController_, _$q_) {
    $rootScope = _$rootScope_;
    $scope = $rootScope.$new();
    EventNameConstant = $injector.get('event.name.constant');

    mockCLMContextLocations = jasmine.createSpyObj('CLMContextLocations', [
      'isOrganization',
      'getEntityId',
      'isApplication',
      'isRootOrg',
    ]);
    mockSourceControlService = jasmine.createSpyObj('mockSourceControlService', [
      'getCompositeSourceControlRecord',
      'getProviderTypesMap',
    ]);
    $componentController = _$componentController_;
    $q = _$q_;
    getByIdDeferred = $q.defer();
    getSourceControlDeferred = $q.defer();
    mockSourceControlService.getProviderTypesMap.and.returnValue({
      github: 'GitHub',
    });
    spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });
    spyOn(organizationsActions, 'loadOrganizations').and.returnValue(getByIdDeferred.promise);
    spyOn(applicationActions, 'loadApplications').and.returnValue(getByIdDeferred.promise);
  }));

  describe('load root organization', function () {
    beforeEach(inject(function () {
      mockCLMContextLocations.isOrganization.and.returnValue(true);
      mockCLMContextLocations.isRootOrg.and.returnValue(true);
      mockCLMContextLocations.isApplication.and.returnValue(false);
      mockCLMContextLocations.getEntityId.and.returnValue(ROOT_ORGANIZATION_ID);
      mockSourceControlService.getCompositeSourceControlRecord.and.callFake(function (ownerType, id) {
        return ownerType === 'organization' && id === ROOT_ORGANIZATION_ID ? getSourceControlDeferred.promise : null;
      });

      vm = $componentController('sourceControlTile', {
        $scope: $scope,
        CLMContextLocations: mockCLMContextLocations,
        SourceControlService: mockSourceControlService,
      });
      vm.isSourceControlSupported = true;
    }));

    it('sets the proper org and app owner flags', function () {
      expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
      expect(mockCLMContextLocations.isRootOrg).toHaveBeenCalled();
      expect(mockCLMContextLocations.isApplication).toHaveBeenCalled();
      expect(vm.isOrg).toBe(true);
      expect(vm.isRootOrg).toBe(true);
      expect(vm.isApp).toBe(false);
    });

    it('loads the root org owner name and reports on success', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID }] });
      $scope.$digest();

      expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
      expect(vm.loadOrganizations).toHaveBeenCalled();
      expect(vm.error).toBeUndefined();
    });

    it('sets the error message on failure for root organization owner id', function () {
      getByIdDeferred.reject({ status: 404, data: 'not found' });

      $scope.$digest();

      expect(vm.error).toEqual('not found');
    });

    it('sets the error message on failure for the root organization source control', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID }] });
      getSourceControlDeferred.reject({ status: 400, data: 'bad request' });

      $scope.$digest();

      expect(vm.error).toEqual('bad request');
    });

    it('sets the source control and does not report an error for the sub organization', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID }] });
      getSourceControlDeferred.resolve({ provider: { value: 'github' }, token: { value: 'TOKEN' } });

      $scope.$digest();

      expect(vm.error).toBeUndefined();
      expect(vm.effectiveProvider).toEqual('github');
    });

    it('reloads on broadcasted owner summary reload event', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID }] });

      $scope.$digest();

      getByIdDeferred = $q.defer();

      $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

      getByIdDeferred.resolve({ payload: [{ name: 'organizationNameUpdated', id: ROOT_ORGANIZATION_ID }] });

      $scope.$digest();

      expect(vm.error).toBeUndefined();
    });

    it('loads the source control and provides the correct text if provider is not defined', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID }] });
      getSourceControlDeferred.resolve({ provider: { value: null, parentValue: null }, token: {} });

      $scope.$digest();

      expect(vm.error).toBeUndefined();
      expect(vm.effectiveProvider).toBeNull();
      expect(vm.itemText).toEqual('');
      expect(vm.itemSubText).toEqual('Source Control not configured');
    });

    it('loads the source control and provides the correct text if provider is defined', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID }] });
      getSourceControlDeferred.resolve({ provider: { value: 'github' }, token: {} });

      $scope.$digest();

      expect(vm.error).toBeUndefined();
      expect(vm.effectiveProvider).toEqual('github');
      expect(vm.itemText).toEqual('GitHub');
      expect(vm.itemSubText).toEqual('Provides the default source control configuration settings');
    });

    describe('vm.loading', function () {
      it('is set to false when all calls success', function () {
        getByIdDeferred.resolve({ payload: [{ name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID }] });
        getSourceControlDeferred.resolve({ provider: { value: 'github' }, token: {} });

        $scope.$digest();
        expect(vm.loading).toBeFalsy();
      });

      it('is set to false when owner identifier cannot be retrieved', function () {
        getByIdDeferred.reject({ status: 400, data: 'bad request' });

        $scope.$digest();
        expect(vm.error).toEqual('bad request');
        expect(vm.loading).toBeFalsy();
      });

      it('is set to false when composite source control cannot be retrieved', function () {
        getByIdDeferred.resolve({ payload: [{ name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID }] });
        getSourceControlDeferred.reject({ status: 400, data: 'bad request' });

        $scope.$digest();
        expect(vm.error).toEqual('bad request');
        expect(vm.loading).toBeFalsy();
      });

      it('is set to true while waiting for owner identifier', function () {
        $scope.$digest();
        expect(vm.error).toEqual(undefined);
        expect(vm.loading).toBeTruthy();
      });

      it('is set to true while waiting for product features', function () {
        getByIdDeferred.resolve({ payload: [{ name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID }] });
        $scope.$digest();
        expect(vm.error).toEqual(undefined);
        expect(vm.loading).toBeTruthy();
      });

      it('is set to true while waiting for composite source control', function () {
        getByIdDeferred.resolve({ payload: [{ name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID }] });
        $scope.$digest();
        expect(vm.error).toEqual(undefined);
        expect(vm.loading).toBeTruthy();
      });
    });
  });

  describe('load sub organization', function () {
    beforeEach(inject(function () {
      mockCLMContextLocations.isOrganization.and.returnValue(true);
      mockCLMContextLocations.isRootOrg.and.returnValue(false);
      mockCLMContextLocations.isApplication.and.returnValue(false);
      mockCLMContextLocations.getEntityId.and.returnValue(SUB_ORGANIZATION_ID);
      mockSourceControlService.getCompositeSourceControlRecord.and.callFake(function (ownerType, id) {
        return ownerType === 'organization' && id === SUB_ORGANIZATION_ID ? getSourceControlDeferred.promise : null;
      });

      vm = $componentController('sourceControlTile', {
        $scope: $scope,
        CLMContextLocations: mockCLMContextLocations,
        SourceControlService: mockSourceControlService,
      });
      vm.isSourceControlSupported = true;
    }));

    it('sets the proper org and app owner flags', function () {
      expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
      expect(mockCLMContextLocations.isRootOrg).toHaveBeenCalled();
      expect(mockCLMContextLocations.isApplication).toHaveBeenCalled();
      expect(vm.isOrg).toBe(true);
      expect(vm.isRootOrg).toBe(false);
      expect(vm.isApp).toBe(false);
    });

    it('loads the owner name of the sub organization and reports on success', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'subOrganizationName', id: SUB_ORGANIZATION_ID }] });

      $scope.$digest();

      expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
      expect(vm.loadOrganizations).toHaveBeenCalled();
      expect(vm.error).toBeUndefined();
    });

    it('loads the source control and provides the correct text if provider is not defined', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'subOrganizationName', id: SUB_ORGANIZATION_ID }] });
      getSourceControlDeferred.resolve({ provider: { value: null, parentValue: null }, token: {} });

      $scope.$digest();

      expect(vm.error).toBeUndefined();
      expect(vm.effectiveProvider).toBeNull();
      expect(vm.itemText).toEqual('');
      expect(vm.itemSubText).toEqual('Source Control not configured');
    });

    it('loads source control and provides correct text if provider is defined and nothing to inherit', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'subOrganizationName', id: SUB_ORGANIZATION_ID }] });
      getSourceControlDeferred.resolve({
        provider: { value: null, parentName: 'root org', parentValue: 'github' },
        token: { value: null, parentName: null, parentValue: null },
      });

      $scope.$digest();

      expect(vm.error).toBeUndefined();
      expect(vm.effectiveProvider).toEqual('github');
      expect(vm.itemText).toEqual('GitHub');
      expect(vm.itemSubText).toEqual('Inherit access token');
    });

    it('loads source control and provides correct text if provider is defined and inherited value', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'subOrganizationName', id: SUB_ORGANIZATION_ID }] });
      getSourceControlDeferred.resolve({
        provider: { value: null, parentName: 'root org', parentValue: 'github' },
        token: { value: null, parentName: 'Root Organization', parentValue: 'token' },
      });

      $scope.$digest();

      expect(vm.error).toBeUndefined();
      expect(vm.effectiveProvider).toEqual('github');
      expect(vm.itemText).toEqual('GitHub');
      expect(vm.itemSubText).toEqual('Inherit access token from Root Organization');
    });

    it('loads source control and provides correct text if provider is defined and token specified', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'subOrganizationName', id: SUB_ORGANIZATION_ID }] });
      getSourceControlDeferred.resolve({
        provider: { value: null, parentName: 'root org', parentValue: 'github' },
        token: { value: 'token', parentName: 'Root Organization', parentValue: 'token' },
      });

      vm.ownerName = 'subOrganizationName';
      $scope.$digest();

      expect(vm.error).toBeUndefined();
      expect(vm.effectiveProvider).toEqual('github');
      expect(vm.itemText).toEqual('GitHub');
      expect(vm.itemSubText).toEqual('Provides default access token for subOrganizationName');
    });
  });

  describe('load application', function () {
    beforeEach(inject(function () {
      mockCLMContextLocations.isOrganization.and.returnValue(false);
      mockCLMContextLocations.isRootOrg.and.returnValue(false);
      mockCLMContextLocations.isApplication.and.returnValue(true);
      mockCLMContextLocations.getEntityId.and.returnValue(APPLICATION_ID);
      mockSourceControlService.getCompositeSourceControlRecord.and.callFake(function (ownerType, id) {
        return ownerType === 'application' && id === APPLICATION_ID ? getSourceControlDeferred.promise : null;
      });

      vm = $componentController('sourceControlTile', {
        $scope: $scope,
        CLMContextLocations: mockCLMContextLocations,
        SourceControlService: mockSourceControlService,
      });
      vm.isSourceControlSupported = true;
    }));

    it('sets the proper org and app owner flags', function () {
      expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
      expect(mockCLMContextLocations.isRootOrg).toHaveBeenCalled();
      expect(mockCLMContextLocations.isApplication).toHaveBeenCalled();
      expect(vm.isOrg).toBe(false);
      expect(vm.isRootOrg).toBe(false);
      expect(vm.isApp).toBe(true);
    });

    it('loads the owner name of the application and reports on success', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'applicationName', publicId: APPLICATION_ID, id: APPLICATION_ID }] });

      $scope.$digest();

      expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
      expect(vm.loadApplications).toHaveBeenCalled();
      expect(vm.error).toBeUndefined();
    });

    it('loads the source control and provides the correct subtext if provider is not defined', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'applicationName', publicId: APPLICATION_ID, id: APPLICATION_ID }] });
      getSourceControlDeferred.resolve({
        provider: { value: null, parentValue: null },
        token: { value: null, parentValue: null },
      });

      $scope.$digest();

      expect(vm.error).toBeUndefined();
      expect(vm.effectiveProvider).toBeNull();
      expect(vm.itemText).toEqual('');
      expect(vm.itemSubText).toEqual('Source Control not configured');
    });

    it('loads source control and provides correct subtext if provider is defined and nothing to inherit', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'applicationName', publicId: APPLICATION_ID, id: APPLICATION_ID }] });
      getSourceControlDeferred.resolve({
        provider: { value: null, parentName: 'root org', parentValue: 'github' },
        token: { value: null, parentName: null, parentValue: null },
      });

      $scope.$digest();

      expect(vm.error).toBeUndefined();
      expect(vm.effectiveProvider).toEqual('github');
      expect(vm.itemText).toEqual('Repository URL needed');
      expect(vm.itemSubText).toEqual('Inherit access token (GitHub)');
    });

    it('loads source control and provides correct subtext if provider is defined and inherited value', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'applicationName', publicId: APPLICATION_ID, id: APPLICATION_ID }] });
      getSourceControlDeferred.resolve({
        provider: { value: null, parentName: 'root org', parentValue: 'github' },
        token: { value: null, parentName: 'Root Organization', parentValue: 'token' },
      });

      $scope.$digest();

      expect(vm.error).toBeUndefined();
      expect(vm.effectiveProvider).toEqual('github');
      expect(vm.itemText).toEqual('Repository URL needed');
      expect(vm.itemSubText).toEqual('Inherit access token from Root Organization (GitHub)');
    });

    it('loads source control and provides correct subtext if provider is defined and token specified', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'applicationName', publicId: APPLICATION_ID, id: APPLICATION_ID }] });
      getSourceControlDeferred.resolve({
        provider: { value: null, parentName: 'root org', parentValue: 'github' },
        token: { value: 'token', parentName: 'Root Organization', parentValue: 'token' },
      });
      vm.ownerName = 'applicationName';

      $scope.$digest();

      expect(vm.error).toBeUndefined();
      expect(vm.effectiveProvider).toEqual('github');
      expect(vm.itemText).toEqual('Repository URL needed');
      expect(vm.itemSubText).toEqual('Provides default access token for applicationName (GitHub)');
    });

    it('loads the source control and provides the correct text if provider is not defined', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'applicationName', publicId: APPLICATION_ID, id: APPLICATION_ID }] });
      getSourceControlDeferred.resolve({ provider: null, token: {} });

      $scope.$digest();

      expect(vm.error).toBeUndefined();
      expect(vm.effectiveProvider).toBeNull();
      expect(vm.itemText).toEqual('');
      expect(vm.itemSubText).toEqual('Source Control not configured');
    });

    it('loads source control and provides correct text if provider is defined and no url', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'applicationName', publicId: APPLICATION_ID, id: APPLICATION_ID }] });
      getSourceControlDeferred.resolve({
        provider: { value: null, parentName: 'root org', parentValue: 'github' },
        token: { value: null, parentName: null, parentValue: null },
      });

      $scope.$digest();

      expect(vm.error).toBeUndefined();
      expect(vm.effectiveProvider).toEqual('github');
      expect(vm.itemText).toEqual('Repository URL needed');
      expect(vm.itemSubText).toEqual('Inherit access token (GitHub)');
    });

    it('loads source control and provides correct text if provider is defined and has url', function () {
      getByIdDeferred.resolve({ payload: [{ name: 'applicationName', publicId: APPLICATION_ID, id: APPLICATION_ID }] });
      getSourceControlDeferred.resolve({
        provider: { value: null, parentName: 'root org', parentValue: 'github' },
        token: { value: null, parentName: null, parentValue: null },
        repositoryUrl: 'url',
      });

      $scope.$digest();

      expect(vm.error).toBeUndefined();
      expect(vm.effectiveProvider).toEqual('github');
      expect(vm.itemText).toEqual('url');
      expect(vm.itemSubText).toEqual('Inherit access token (GitHub)');
    });
  });
});
