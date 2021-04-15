/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import sourceControlModule from '../../../../main/frontend/owner.manager/source.control/module';
import utilityModule from '../../../../main/frontend/utility/utility.module';
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('source.control.tile.spec', function () {
  const ROOT_ORGANIZATION_ID = 'rootOrganizationId';
  const SUB_ORGANIZATION_ID = 'subOrganizationId';
  const APPLICATION_ID = 'applicationId';

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
    mockSourceControlService,
    getSourceControlDeferred,
    mockProductFeatures,
    loadProductFeaturesDefer;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
    })
  );

  beforeEach(angular.mock.module(sourceControlModule.name, utilityModule.name));

  beforeEach(inject(function (
    _$rootScope_,
    $injector,
    _$componentController_,
    _$q_
  ) {
    $rootScope = _$rootScope_;
    $scope = $rootScope.$new();
    EventNameConstant = $injector.get('event.name.constant');

    mockCLMContextLocations = jasmine.createSpyObj('CLMContextLocations', [
      'isOrganization',
      'getEntityId',
      'isApplication',
      'isRootOrg',
    ]);
    mockOrganizationStore = jasmine.createSpyObj('mockOrganizationStore', [
      'getById',
    ]);
    mockApplicationStore = jasmine.createSpyObj('mockApplicationsStore', [
      'getById',
    ]);
    mockSourceControlService = jasmine.createSpyObj(
      'mockSourceControlService',
      ['getCompositeSourceControlRecord', 'getProviderTypesMap']
    );
    $componentController = _$componentController_;
    $q = _$q_;
    getByIdDeferred = $q.defer();
    getSourceControlDeferred = $q.defer();
    mockSourceControlService.getProviderTypesMap.and.returnValue({
      github: 'GitHub',
    });
    mockProductFeatures = jasmine.createSpyObj('mockProductFeatures', [
      'isAvailable',
      'load',
    ]);
    loadProductFeaturesDefer = $q.defer();
    mockProductFeatures.load.and.returnValue(loadProductFeaturesDefer.promise);
    mockProductFeatures.isAvailable.and.callFake(function (feature) {
      return feature === 'notifications' || feature === 'automation';
    });
  }));

  describe('load root organization', function () {
    beforeEach(inject(function () {
      mockCLMContextLocations.isOrganization.and.returnValue(true);
      mockCLMContextLocations.isRootOrg.and.returnValue(true);
      mockCLMContextLocations.isApplication.and.returnValue(false);
      mockCLMContextLocations.getEntityId.and.returnValue(ROOT_ORGANIZATION_ID);
      mockOrganizationStore.getById.and.callFake(function (id) {
        return id === ROOT_ORGANIZATION_ID ? getByIdDeferred.promise : null;
      });
      mockSourceControlService.getCompositeSourceControlRecord.and.callFake(
        function (ownerType, id) {
          return ownerType === 'organization' && id === ROOT_ORGANIZATION_ID
            ? getSourceControlDeferred.promise
            : null;
        }
      );

      vm = $componentController('sourceControlTile', {
        $scope: $scope,
        CLMContextLocations: mockCLMContextLocations,
        OrganizationStore: mockOrganizationStore,
        ApplicationStore: mockApplicationStore,
        SourceControlService: mockSourceControlService,
        ProductFeatures: mockProductFeatures,
      });
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
      getByIdDeferred.resolve({
        name: 'rootOrganizationName',
        id: ROOT_ORGANIZATION_ID,
      });
      loadProductFeaturesDefer.resolve({});

      $scope.$digest();

      expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
      expect(mockOrganizationStore.getById).toHaveBeenCalledWith(
        ROOT_ORGANIZATION_ID
      );
      expect(vm.ownerName).toBe('rootOrganizationName');
      expect(vm.error).toBeUndefined();
    });

    it('sets the error message on failure for root organization owner id', function () {
      getByIdDeferred.reject({ status: 404, data: 'not found' });

      $scope.$digest();

      expect(vm.ownerName).toBeUndefined();
      expect(vm.error).toEqual('not found');
    });

    it('sets the error message on failure for the root organization source control', function () {
      getByIdDeferred.resolve({
        name: 'rootOrganizationName',
        id: ROOT_ORGANIZATION_ID,
      });
      loadProductFeaturesDefer.resolve({});
      getSourceControlDeferred.reject({ status: 400, data: 'bad request' });

      $scope.$digest();

      expect(vm.ownerName).toEqual('rootOrganizationName');
      expect(vm.error).toEqual('bad request');
    });

    it('sets the source control and does not report an error for the sub organization', function () {
      getByIdDeferred.resolve({
        name: 'rootOrganizationName',
        id: ROOT_ORGANIZATION_ID,
      });
      loadProductFeaturesDefer.resolve({});
      getSourceControlDeferred.resolve({
        provider: 'github',
        token: { value: 'TOKEN' },
      });

      $scope.$digest();

      expect(vm.ownerName).toEqual('rootOrganizationName');
      expect(vm.error).toBeUndefined();
      expect(vm.sourceControl.provider).toEqual('github');
    });

    it('reloads on broadcasted owner summary reload event', function () {
      getByIdDeferred.resolve({
        name: 'rootOrganizationName',
        id: ROOT_ORGANIZATION_ID,
      });
      loadProductFeaturesDefer.resolve({});

      $scope.$digest();

      getByIdDeferred = $q.defer();

      $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

      getByIdDeferred.resolve({
        name: 'organizationNameUpdated',
        id: ROOT_ORGANIZATION_ID,
      });

      $scope.$digest();

      expect(vm.ownerName).toBe('organizationNameUpdated');
      expect(vm.error).toBeUndefined();
    });

    it('loads the source control and provides the correct text if provider is not defined', function () {
      getByIdDeferred.resolve({
        name: 'rootOrganizationName',
        id: ROOT_ORGANIZATION_ID,
      });
      loadProductFeaturesDefer.resolve({});
      getSourceControlDeferred.resolve({ provider: null, token: {} });

      $scope.$digest();

      expect(vm.ownerName).toEqual('rootOrganizationName');
      expect(vm.error).toBeUndefined();
      expect(vm.sourceControl.provider).toBeNull();
      expect(vm.itemText).toEqual('');
      expect(vm.itemSubText).toEqual('Source Control not configured');
    });

    it('loads the source control and provides the correct text if provider is defined', function () {
      getByIdDeferred.resolve({
        name: 'rootOrganizationName',
        id: ROOT_ORGANIZATION_ID,
      });
      loadProductFeaturesDefer.resolve({});
      getSourceControlDeferred.resolve({ provider: 'github', token: {} });

      $scope.$digest();

      expect(vm.ownerName).toEqual('rootOrganizationName');
      expect(vm.error).toBeUndefined();
      expect(vm.sourceControl.provider).toEqual('github');
      expect(vm.itemText).toEqual('GitHub');
      expect(vm.itemSubText).toEqual(
        'Provides the default source control configuration settings'
      );
    });

    describe('isSourceControlSupported', function () {
      it('returns true if notifications are supported', function () {
        getByIdDeferred.resolve({
          name: 'rootOrganizationName',
          id: ROOT_ORGANIZATION_ID,
        });
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve({ provider: 'github' });
        mockProductFeatures.isAvailable.and.callFake(function (feature) {
          return feature === 'notifications';
        });
        $scope.$digest();
        expect(vm.isSourceControlSupported).toBeTruthy();
      });

      it('returns true if automation is supported', function () {
        getByIdDeferred.resolve({
          name: 'rootOrganizationName',
          id: ROOT_ORGANIZATION_ID,
        });
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve({ provider: 'github' });
        mockProductFeatures.isAvailable.and.callFake(function (feature) {
          return feature === 'automation';
        });
        $scope.$digest();
        expect(vm.isSourceControlSupported).toBeTruthy();
      });

      it('returns false if notifications and automation is not supported', function () {
        getByIdDeferred.resolve({
          name: 'rootOrganizationName',
          id: ROOT_ORGANIZATION_ID,
        });
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve({ provider: 'github' });
        mockProductFeatures.isAvailable.and.callFake(function () {
          return false;
        });
        $scope.$digest();
        expect(vm.isSourceControlSupported).toBeFalsy();
      });
    });
    describe('isAutomationSupported', function () {
      it('returns true if automation is supported', function () {
        getByIdDeferred.resolve({
          name: 'rootOrganizationName',
          id: ROOT_ORGANIZATION_ID,
        });
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve({ provider: 'github' });
        $scope.$digest();
        expect(vm.isAutomationSupported).toBeTruthy();
      });

      it('returns false if automation is not supported', function () {
        getByIdDeferred.resolve({
          name: 'rootOrganizationName',
          id: ROOT_ORGANIZATION_ID,
        });
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve({ provider: 'github' });
        mockProductFeatures.isAvailable.and.callFake(function (feature) {
          return feature === 'notifications';
        });
        $scope.$digest();
        expect(vm.isAutomationSupported).toBeFalsy();
      });
    });
    describe('vm.loading', function () {
      it('is set to false when all calls success', function () {
        getByIdDeferred.resolve({
          name: 'rootOrganizationName',
          id: ROOT_ORGANIZATION_ID,
        });
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve({ provider: 'github', token: {} });

        $scope.$digest();
        expect(vm.loading).toBeFalsy();
      });

      it('is set to false when owner identifier cannot be retrieved', function () {
        getByIdDeferred.reject({ status: 400, data: 'bad request' });

        $scope.$digest();
        expect(vm.error).toEqual('bad request');
        expect(vm.loading).toBeFalsy();
      });

      it('is set to false when product features cannot be retrieved', function () {
        getByIdDeferred.resolve({
          name: 'rootOrganizationName',
          id: ROOT_ORGANIZATION_ID,
        });
        loadProductFeaturesDefer.reject({ status: 400, data: 'bad request' });
        getSourceControlDeferred.resolve({ provider: 'github', token: {} });

        $scope.$digest();
        expect(vm.error).toEqual('bad request');
        expect(vm.loading).toBeFalsy();
      });

      it('is set to false when composite source control cannot be retrieved', function () {
        getByIdDeferred.resolve({
          name: 'rootOrganizationName',
          id: ROOT_ORGANIZATION_ID,
        });
        loadProductFeaturesDefer.resolve({});
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
        getByIdDeferred.resolve({
          name: 'rootOrganizationName',
          id: ROOT_ORGANIZATION_ID,
        });
        $scope.$digest();
        expect(vm.error).toEqual(undefined);
        expect(vm.loading).toBeTruthy();
      });

      it('is set to true while waiting for composite source control', function () {
        getByIdDeferred.resolve({
          name: 'rootOrganizationName',
          id: ROOT_ORGANIZATION_ID,
        });
        loadProductFeaturesDefer.resolve({});
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
      mockOrganizationStore.getById.and.callFake(function (id) {
        return id === SUB_ORGANIZATION_ID ? getByIdDeferred.promise : null;
      });
      mockSourceControlService.getCompositeSourceControlRecord.and.callFake(
        function (ownerType, id) {
          return ownerType === 'organization' && id === SUB_ORGANIZATION_ID
            ? getSourceControlDeferred.promise
            : null;
        }
      );

      vm = $componentController('sourceControlTile', {
        $scope: $scope,
        CLMContextLocations: mockCLMContextLocations,
        OrganizationStore: mockOrganizationStore,
        ApplicationStore: mockApplicationStore,
        SourceControlService: mockSourceControlService,
        ProductFeatures: mockProductFeatures,
      });
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
      getByIdDeferred.resolve({
        name: 'subOrganizationName',
        id: SUB_ORGANIZATION_ID,
      });
      loadProductFeaturesDefer.resolve({});

      $scope.$digest();

      expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
      expect(mockOrganizationStore.getById).toHaveBeenCalledWith(
        SUB_ORGANIZATION_ID
      );
      expect(vm.ownerName).toBe('subOrganizationName');
      expect(vm.error).toBeUndefined();
    });

    it('loads the source control and provides the correct text if provider is not defined', function () {
      getByIdDeferred.resolve({
        name: 'subOrganizationName',
        id: SUB_ORGANIZATION_ID,
      });
      loadProductFeaturesDefer.resolve({});
      getSourceControlDeferred.resolve({ provider: null, token: {} });

      $scope.$digest();

      expect(vm.ownerName).toEqual('subOrganizationName');
      expect(vm.error).toBeUndefined();
      expect(vm.sourceControl.provider).toBeNull();
      expect(vm.itemText).toEqual('');
      expect(vm.itemSubText).toEqual('Source Control not configured');
    });

    it('loads source control and provides correct text if provider is defined and nothing to inherit', function () {
      getByIdDeferred.resolve({
        name: 'subOrganizationName',
        id: SUB_ORGANIZATION_ID,
      });
      loadProductFeaturesDefer.resolve({});
      getSourceControlDeferred.resolve({
        provider: 'github',
        token: { value: null, parentName: null, parentValue: null },
      });

      $scope.$digest();

      expect(vm.ownerName).toEqual('subOrganizationName');
      expect(vm.error).toBeUndefined();
      expect(vm.sourceControl.provider).toEqual('github');
      expect(vm.itemText).toEqual('GitHub');
      expect(vm.itemSubText).toEqual('Inherit access token');
    });

    it('loads source control and provides correct text if provider is defined and inherited value', function () {
      getByIdDeferred.resolve({
        name: 'subOrganizationName',
        id: SUB_ORGANIZATION_ID,
      });
      loadProductFeaturesDefer.resolve({});
      getSourceControlDeferred.resolve({
        provider: 'github',
        token: {
          value: null,
          parentName: 'Root Organization',
          parentValue: 'token',
        },
      });

      $scope.$digest();

      expect(vm.ownerName).toEqual('subOrganizationName');
      expect(vm.error).toBeUndefined();
      expect(vm.sourceControl.provider).toEqual('github');
      expect(vm.itemText).toEqual('GitHub');
      expect(vm.itemSubText).toEqual(
        'Inherit access token from Root Organization'
      );
    });

    it('loads source control and provides correct text if provider is defined and token specified', function () {
      getByIdDeferred.resolve({
        name: 'subOrganizationName',
        id: SUB_ORGANIZATION_ID,
      });
      loadProductFeaturesDefer.resolve({});
      getSourceControlDeferred.resolve({
        provider: 'github',
        token: {
          value: 'token',
          parentName: 'Root Organization',
          parentValue: 'token',
        },
      });

      $scope.$digest();

      expect(vm.ownerName).toEqual('subOrganizationName');
      expect(vm.error).toBeUndefined();
      expect(vm.sourceControl.provider).toEqual('github');
      expect(vm.itemText).toEqual('GitHub');
      expect(vm.itemSubText).toEqual(
        'Provides default access token for subOrganizationName'
      );
    });
  });

  describe('load application', function () {
    beforeEach(inject(function () {
      mockCLMContextLocations.isOrganization.and.returnValue(false);
      mockCLMContextLocations.isRootOrg.and.returnValue(false);
      mockCLMContextLocations.isApplication.and.returnValue(true);
      mockCLMContextLocations.getEntityId.and.returnValue(APPLICATION_ID);
      mockApplicationStore.getById.and.callFake(function (id) {
        return id === APPLICATION_ID ? getByIdDeferred.promise : null;
      });
      mockSourceControlService.getCompositeSourceControlRecord.and.callFake(
        function (ownerType, id) {
          return ownerType === 'application' && id === APPLICATION_ID
            ? getSourceControlDeferred.promise
            : null;
        }
      );

      vm = $componentController('sourceControlTile', {
        $scope: $scope,
        CLMContextLocations: mockCLMContextLocations,
        OrganizationStore: mockOrganizationStore,
        ApplicationStore: mockApplicationStore,
        SourceControlService: mockSourceControlService,
        ProductFeatures: mockProductFeatures,
      });
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
      getByIdDeferred.resolve({ name: 'applicationName', id: APPLICATION_ID });
      loadProductFeaturesDefer.resolve({});

      $scope.$digest();

      expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
      expect(mockApplicationStore.getById).toHaveBeenCalledWith(APPLICATION_ID);
      expect(vm.ownerName).toBe('applicationName');
      expect(vm.error).toBeUndefined();
    });

    it('loads the source control and provides the correct subtext if provider is not defined', function () {
      getByIdDeferred.resolve({ name: 'applicationName', id: APPLICATION_ID });
      loadProductFeaturesDefer.resolve({});
      getSourceControlDeferred.resolve({ provider: null, token: {} });

      $scope.$digest();

      expect(vm.ownerName).toEqual('applicationName');
      expect(vm.error).toBeUndefined();
      expect(vm.sourceControl.provider).toBeNull();
      expect(vm.itemText).toEqual('');
      expect(vm.itemSubText).toEqual('Source Control not configured');
    });

    it('loads source control and provides correct subtext if provider is defined and nothing to inherit', function () {
      getByIdDeferred.resolve({ name: 'applicationName', id: APPLICATION_ID });
      loadProductFeaturesDefer.resolve({});
      getSourceControlDeferred.resolve({
        provider: 'github',
        token: { value: null, parentName: null, parentValue: null },
      });

      $scope.$digest();

      expect(vm.ownerName).toEqual('applicationName');
      expect(vm.error).toBeUndefined();
      expect(vm.sourceControl.provider).toEqual('github');
      expect(vm.itemText).toEqual('Repository URL needed');
      expect(vm.itemSubText).toEqual('Inherit access token (GitHub)');
    });

    it('loads source control and provides correct subtext if provider is defined and inherited value', function () {
      getByIdDeferred.resolve({ name: 'applicationName', id: APPLICATION_ID });
      loadProductFeaturesDefer.resolve({});
      getSourceControlDeferred.resolve({
        provider: 'github',
        token: {
          value: null,
          parentName: 'Root Organization',
          parentValue: 'token',
        },
      });

      $scope.$digest();

      expect(vm.ownerName).toEqual('applicationName');
      expect(vm.error).toBeUndefined();
      expect(vm.sourceControl.provider).toEqual('github');
      expect(vm.itemText).toEqual('Repository URL needed');
      expect(vm.itemSubText).toEqual(
        'Inherit access token from Root Organization (GitHub)'
      );
    });

    it('loads source control and provides correct subtext if provider is defined and token specified', function () {
      getByIdDeferred.resolve({ name: 'applicationName', id: APPLICATION_ID });
      loadProductFeaturesDefer.resolve({});
      getSourceControlDeferred.resolve({
        provider: 'github',
        token: {
          value: 'token',
          parentName: 'Root Organization',
          parentValue: 'token',
        },
      });

      $scope.$digest();

      expect(vm.ownerName).toEqual('applicationName');
      expect(vm.error).toBeUndefined();
      expect(vm.sourceControl.provider).toEqual('github');
      expect(vm.itemText).toEqual('Repository URL needed');
      expect(vm.itemSubText).toEqual(
        'Provides default access token for applicationName (GitHub)'
      );
    });

    it('loads the source control and provides the correct text if provider is not defined', function () {
      getByIdDeferred.resolve({ name: 'applicationName', id: APPLICATION_ID });
      loadProductFeaturesDefer.resolve({});
      getSourceControlDeferred.resolve({ provider: null, token: {} });

      $scope.$digest();

      expect(vm.ownerName).toEqual('applicationName');
      expect(vm.error).toBeUndefined();
      expect(vm.sourceControl.provider).toBeNull();
      expect(vm.itemText).toEqual('');
      expect(vm.itemSubText).toEqual('Source Control not configured');
    });

    it('loads source control and provides correct text if provider is defined and no url', function () {
      getByIdDeferred.resolve({ name: 'applicationName', id: APPLICATION_ID });
      loadProductFeaturesDefer.resolve({});
      getSourceControlDeferred.resolve({
        provider: 'github',
        token: { value: null, parentName: null, parentValue: null },
      });

      $scope.$digest();

      expect(vm.ownerName).toEqual('applicationName');
      expect(vm.error).toBeUndefined();
      expect(vm.sourceControl.provider).toEqual('github');
      expect(vm.itemText).toEqual('Repository URL needed');
      expect(vm.itemSubText).toEqual('Inherit access token (GitHub)');
    });

    it('loads source control and provides correct text if provider is defined and has url', function () {
      getByIdDeferred.resolve({ name: 'applicationName', id: APPLICATION_ID });
      loadProductFeaturesDefer.resolve({});
      getSourceControlDeferred.resolve({
        provider: 'github',
        token: { value: null, parentName: null, parentValue: null },
        repositoryUrl: 'url',
      });

      $scope.$digest();

      expect(vm.ownerName).toEqual('applicationName');
      expect(vm.error).toBeUndefined();
      expect(vm.sourceControl.provider).toEqual('github');
      expect(vm.itemText).toEqual('url');
      expect(vm.itemSubText).toEqual('Inherit access token (GitHub)');
    });
  });
});
