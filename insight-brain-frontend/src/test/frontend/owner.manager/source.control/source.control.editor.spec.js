import sourceControlModule from '../../../../main/frontend/owner.manager/source.control/module';
import utilityModule from '../../../../main/frontend/utility/utility.module';
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('source.control.editor.spec', function() {
  const ROOT_ORGANIZATION_ID = 'rootOrganizationId';
  const SUB_ORGANIZATION_ID = 'subOrganizationId';
  const APPLICATION_ID = 'applicationId';
  const REPOSITORY_URL = 'https://a.com/b/c';

  let $rootScope,
      $scope,
      $q,
      $componentController,
      mockCLMContextLocations,
      mockOrganizationStore,
      mockApplicationStore,
      getByIdDeferred,
      vm,
      mockSourceControlService,
      getSourceControlDeferred,
      deleteServiceResourceDefer,
      saveResourceDefer,
      mockDeleteService = {
        deleteCustom: function(headerText, bodyText, maskText, continueAction) {
          continueAction();
          return deleteServiceResourceDefer.promise;
        }
      },
      mockSameOwnerStateNavigationService = {
        goEdit: jasmine.createSpy()
      },
      $timeout,
      mockProductFeatures,
      loadProductFeaturesDefer;

  beforeEach(angular.mock.module(ownerManagerModule.name, function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  beforeEach(angular.mock.module(sourceControlModule.name, utilityModule.name));

  beforeEach(inject(function(_$rootScope_, $injector, _$componentController_, _$q_, _$timeout_) {
    $rootScope = _$rootScope_;
    $scope = $rootScope.$new();

    mockCLMContextLocations = jasmine.createSpyObj('CLMContextLocations',
        ['isOrganization', 'getEntityId', 'isApplication', 'isRootOrg']);
    mockOrganizationStore = jasmine.createSpyObj('mockOrganizationStore', ['getById']);
    mockApplicationStore = jasmine.createSpyObj('mockApplicationsStore', ['getById']);
    mockSourceControlService = jasmine.createSpyObj('mockSourceControlService',
        [
          'getCompositeSourceControlRecord', 'getProviderTypes', 'deleteSourceControlRecord', 'addSourceControlRecord',
          'updateSourceControlRecord', 'getProviderTypesMap'
        ]);
    $componentController = _$componentController_;
    $q = _$q_;
    getByIdDeferred = $q.defer();
    getSourceControlDeferred = $q.defer();
    deleteServiceResourceDefer = $q.defer();
    saveResourceDefer = $q.defer();
    loadProductFeaturesDefer = $q.defer();
    $timeout = _$timeout_;
    mockProductFeatures = jasmine.createSpyObj('mockProductFeatures', ['isAvailable', 'load']);
    mockProductFeatures.load.and.returnValue(loadProductFeaturesDefer.promise);
    mockProductFeatures.isAvailable.and.callFake(function(feature) {
      return feature === 'notifications' || feature === 'automation';
    });
  }));

  describe('root organization', function() {
    const compositeSourceControl = {
      token: {
        value: null,
        parentName: null,
        parentValue: null
      },
      ownerId: ROOT_ORGANIZATION_ID,
      id: 'ID',
      provider: 'github',
      repositoryUrl: null,
      baseBranch: {
        value: 'BASE_BRANCH',
        parentName: null,
        parentValue: null
      },
      enableStatusChecks: {
        value: true,
        parentName: null,
        parentValue: null
      },
      enablePullRequests: {
        value: null,
        parentName: null,
        parentValue: null
      }
    };

    const sourceControlModel = {
      tokenInherit: false,
      token: null,
      tokenInheritFrom: null,
      baseBranchInherit: false,
      baseBranch: 'BASE_BRANCH',
      baseBranchInheritFrom: null,
      baseBranchInheritedValue: null,
      ownerId: ROOT_ORGANIZATION_ID,
      id: 'ID',
      provider: 'github',
      repositoryUrl: null,
      enablePullRequests: null,
      enablePullRequestsInheritedValue: null,
      enablePullRequestsInheritFrom: null,
      enableStatusChecks: true,
      enableStatusChecksInheritedValue: null,
      enableStatusChecksInheritFrom: null
    };

    beforeEach(inject(function() {
      mockCLMContextLocations.isOrganization.and.returnValue(true);
      mockCLMContextLocations.isRootOrg.and.returnValue(true);
      mockCLMContextLocations.isApplication.and.returnValue(false);
      mockCLMContextLocations.getEntityId.and.returnValue(ROOT_ORGANIZATION_ID);
      mockSourceControlService.updateSourceControlRecord.and.returnValue(saveResourceDefer.promise);
      mockSourceControlService.addSourceControlRecord.and.returnValue(saveResourceDefer.promise);
      mockSourceControlService.getProviderTypesMap.and.returnValue({
        'github': 'GitHub',
        'gitlab': 'GitLab'
      });
      mockOrganizationStore.getById.and.callFake(function(id) {
        return id === ROOT_ORGANIZATION_ID ? getByIdDeferred.promise : null;
      });
      mockSourceControlService.getCompositeSourceControlRecord.and.callFake(function(ownerType, id) {
        return ownerType === 'organization' && id === ROOT_ORGANIZATION_ID ? getSourceControlDeferred.promise : null;
      });

      vm = $componentController('sourceControlEditor', {
        $scope: $scope,
        CLMContextLocations: mockCLMContextLocations,
        OrganizationStore: mockOrganizationStore,
        ApplicationStore: mockApplicationStore,
        SourceControlService: mockSourceControlService,
        DeleteModalService: mockDeleteService,
        SameOwnerStateNavigationService: mockSameOwnerStateNavigationService,
        ProductFeatures: mockProductFeatures
      });
      vm.sourceControlEditor = {
        $setPristine: function() {
        }
      };
      vm.sourceControlEditorMask = {wrap: SpecUtil.promiseWrapper($q)};
    }));

    describe('doLoad', function() {
      it('sets the proper org and app owner flags', function() {
        expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
        expect(mockCLMContextLocations.isRootOrg).toHaveBeenCalled();
        expect(mockCLMContextLocations.isApplication).toHaveBeenCalled();
        expect(vm.isOrg).toBe(true);
        expect(vm.isRootOrg).toBe(true);
        expect(vm.isApp).toBe(false);

        mockCLMContextLocations.isOrganization.and.returnValue(false);
        vm = $componentController('sourceControlEditor', {
          $scope: $scope,
          CLMContextLocations: mockCLMContextLocations,
          OrganizationStore: mockOrganizationStore,
          ApplicationStore: mockApplicationStore,
          SourceControlService: mockSourceControlService
        });
        expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
        expect(mockCLMContextLocations.isRootOrg).toHaveBeenCalled();
        expect(mockCLMContextLocations.isApplication).toHaveBeenCalled();
        expect(vm.isOrg).toBe(false);
      });

      it('loads the root org owner name and reports on success', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        $scope.$digest();

        expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
        expect(mockOrganizationStore.getById).toHaveBeenCalledWith(ROOT_ORGANIZATION_ID);
        expect(vm.ownerName).toBe('rootOrganizationName');
        expect(vm.loadError).toBeUndefined();
      });

      it('sets the error message on failure for root organization owner id', function() {
        getByIdDeferred.reject({status: 404, data: 'not found'});

        $scope.$digest();

        expect(vm.ownerName).toBeUndefined();
        expect(vm.loadError).toEqual('not found');
      });

      it('sets the error message on failure for the root organization source control', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.reject({status: 400, data: 'bad request'});

        $scope.$digest();

        expect(vm.ownerName).toEqual('rootOrganizationName');
        expect(vm.loadError).toEqual('bad request');
      });

      it('sets the source control and does not report an error for the root organization', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('rootOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });

      it('sets the base branch to master if empty for the root organization', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.value = null;
        let sourceControlModelCopy = angular.copy(sourceControlModel);
        sourceControlModelCopy.baseBranch = 'master';

        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();

        expect(vm.ownerName).toEqual('rootOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);
        expect(vm.dirtySourceControl.baseBranch).toEqual('master');
      });
    });

    describe('deleteSourceControl', function() {
      it('deletes the existing entry without error', function() {

        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('rootOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        // when
        vm.deleteSourceControl();
        deleteServiceResourceDefer.resolve();
        expect(mockSourceControlService.deleteSourceControlRecord).toHaveBeenCalledWith('organization',
            ROOT_ORGANIZATION_ID);

        $scope.$digest();

        // then
        expect(mockSameOwnerStateNavigationService.goEdit).toHaveBeenCalledWith('edit-source-control');
        expect(vm.loadError).toBeUndefined();
      });
    });

    describe('save', function() {
      it('creates a new entry if source control is not configured', function() {

        const expectedSourceControlForSave = {
          provider: 'github',
          token: null,
          ownerId: ROOT_ORGANIZATION_ID,
          id: null,
          baseBranch: 'BASE_BRANCH',
          enablePullRequests: null,
          enableStatusChecks: true
        };

        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('rootOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl = angular.copy(sourceControlModel);
        vm.dirtySourceControl.id = null;
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        // when
        vm.save();
        $scope.$digest();

        // then
        expect(mockSourceControlService.addSourceControlRecord).toHaveBeenCalledWith('organization',
            ROOT_ORGANIZATION_ID, expectedSourceControlForSave);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });

      it('updates the existing entry if source control is configured', function() {

        const expectedSourceControlForSave = {
          provider: 'gitlab',
          token: null,
          ownerId: ROOT_ORGANIZATION_ID,
          id: 'ID',
          baseBranch: 'BASE_BRANCH',
          enablePullRequests: true,
          enableStatusChecks: true
        };

        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('rootOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        vm.dirtySourceControl = sourceControlModelCopy;
        vm.dirtySourceControl.provider = 'gitlab';
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        compositeSourceControlCopy.provider = 'gitlab';
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        // when
        vm.save();
        $scope.$digest();

        // then
        expect(mockSourceControlService.updateSourceControlRecord).toHaveBeenCalledWith('organization',
            ROOT_ORGANIZATION_ID, expectedSourceControlForSave);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);
      });

      it('returns an error for unsuccessful save', function() {
        const expectedSourceControlForSave = {
          provider: 'gitlab',
          token: null,
          ownerId: ROOT_ORGANIZATION_ID,
          id: 'ID',
          baseBranch: 'BASE_BRANCH',
          enablePullRequests: true,
          enableStatusChecks: true
        };

        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('rootOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        vm.dirtySourceControl = sourceControlModelCopy;
        vm.dirtySourceControl.provider = 'gitlab';
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.reject({status: '400', data: 'bad request'});

        // when
        vm.save();
        $scope.$digest();

        // then
        expect(mockSourceControlService.updateSourceControlRecord).toHaveBeenCalledWith('organization',
            ROOT_ORGANIZATION_ID, expectedSourceControlForSave);
        expect(vm.submitError).toEqual('bad request');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });
    });

    describe('isDirty', function() {
      it('returns true when changes have been applied to provider', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('rootOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.provider = 'gitlab';

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to token', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('rootOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.token = 'new_token';

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to baseBranch', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('rootOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.baseBranch = 'new_branch';

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to enablePullRequests', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('rootOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.enablePullRequests = 'false';
        vm.dirtySourceControl.provider = 'github';

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns false after save', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('rootOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.provider = 'gitlab';
        compositeSourceControl.provider = 'github';
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        // when
        vm.save();
        $scope.$digest();

        // then
        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns false after delete', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('rootOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        $timeout.flush();

        // when
        vm.deleteSourceControl();
        deleteServiceResourceDefer.resolve();
        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns false after load', function() {

        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('rootOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        expect(vm.isDirty()).toBeFalsy();
      });
    });

    describe('isAccessTokenRequiredOnNode', function() {
      it('should return false for root organization', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();
        expect(vm.isAccessTokenRequiredOnNode()).toBeFalsy();
      });
    });

    describe('showAdvanced', function() {
      it('should return true for root organization', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();
        expect(vm.showAdvanced).toBeTruthy();
      });
    });

    describe('shouldShowAccessTokenWarning', function() {
      it('should return false for root organization', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();
        expect(vm.shouldShowAccessTokenWarning).toBeFalsy();
      });
    });

    describe('canCollapseAdvanced', function() {
      it('should return false for root organization', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();
        expect(vm.canCollapseAdvanced()).toBeFalsy();
      });
    });

    describe('isPullRequestsSupported', function() {
      it('should return true if licence supports automation and provider is github', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();
        expect(vm.isPullRequestsSupported()).toBeTruthy();

        vm.dirtySourceControl.provider = 'gitlab';
        expect(vm.isPullRequestsSupported()).toBeFalsy();
      });

      it('should return false if licence does not support automation', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);
        mockProductFeatures.isAvailable.and.callFake(function(feature) {
          return feature === 'notifications';
        });

        $scope.$digest();
        expect(vm.isPullRequestsSupported()).toBeFalsy();

        vm.dirtySourceControl.provider = 'gitlab';
        expect(vm.isPullRequestsSupported()).toBeFalsy();
      });
    });

    describe('getPullRequestsNotAvailableMessage', function() {
      it('should return message for gitlab if licence supports automation', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();
        expect(vm.getPullRequestsNotAvailableMessage()).toEqual('');

        vm.dirtySourceControl.provider = 'gitlab';
        expect(vm.getPullRequestsNotAvailableMessage()).toEqual('This feature is not currently supported for GitLab');
      });

      it('should return licencing message if licence does not support automation', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);
        mockProductFeatures.isAvailable.and.callFake(function(feature) {
          return feature === 'notifications';
        });

        $scope.$digest();
        expect(vm.getPullRequestsNotAvailableMessage()).toEqual('This feature is not supported by your licence');

        vm.dirtySourceControl.provider = 'gitlab';
        expect(vm.getPullRequestsNotAvailableMessage()).toEqual('This feature is not supported by your licence');
      });
    });

    describe('isProviderSpecifiedAndPullRequestsSupported', function() {
      it('should return true for github if licence supports automation', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();
        expect(vm.isProviderSpecifiedAndPullRequestsSupported()).toBeTruthy();

        vm.dirtySourceControl.provider = 'gitlab';
        expect(vm.isProviderSpecifiedAndPullRequestsSupported()).toBeFalsy();

        vm.dirtySourceControl.provider = null;
        expect(vm.isProviderSpecifiedAndPullRequestsSupported()).toBeFalsy();
      });

      it('should return false if licence does not support automation', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);
        mockProductFeatures.isAvailable.and.callFake(function(feature) {
          return feature === 'notifications';
        });

        $scope.$digest();
        expect(vm.isProviderSpecifiedAndPullRequestsSupported()).toBeFalsy();

        vm.dirtySourceControl.provider = 'gitlab';
        expect(vm.isProviderSpecifiedAndPullRequestsSupported()).toBeFalsy();

        vm.dirtySourceControl.provider = null;
        expect(vm.isProviderSpecifiedAndPullRequestsSupported()).toBeFalsy();
      });
    });

    describe('isSourceControlSupported', function() {
      it('returns true if notifications are supported', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);
        mockProductFeatures.isAvailable.and.callFake(function(feature) {
          return feature === 'notifications';
        });
        $scope.$digest();
        expect(vm.isSourceControlSupported).toBeTruthy();
      });

      it('returns true if automation is supported', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);
        mockProductFeatures.isAvailable.and.callFake(function(feature) {
          return feature === 'automation';
        });
        $scope.$digest();
        expect(vm.isSourceControlSupported).toBeTruthy();
      });

      it('returns false if notifications and automation is not supported', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);
        mockProductFeatures.isAvailable.and.callFake(function() {
          return false;
        });
        $scope.$digest();
        expect(vm.isSourceControlSupported).toBeFalsy();
      });
    });

    describe('isAutomationSupported', function() {
      it('returns true if automation is supported', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);
        $scope.$digest();
        expect(vm.isAutomationSupported).toBeTruthy();
      });

      it('returns false if automation is not supported', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);
        mockProductFeatures.isAvailable.and.callFake(function(feature) {
          return feature === 'notifications';
        });
        $scope.$digest();
        expect(vm.isAutomationSupported).toBeFalsy();
      });
    });

    describe('vm.loading', function() {
      it('is set to false when all calls success', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();
        expect(vm.loading).toBeFalsy();
      });

      it('is set to false when owner identifier cannot be retrieved', function() {
        getByIdDeferred.reject({status: 404, data: 'not found'});

        $scope.$digest();
        expect(vm.loadError).toEqual('not found');
        expect(vm.loading).toBeFalsy();
      });

      it('is set to false when product features cannot be retrieved', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.reject({status: 404, data: 'not found'});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();
        expect(vm.loadError).toEqual('not found');
        expect(vm.loading).toBeFalsy();
      });

      it('is set to false when composite source control cannot be retrieved', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.reject({status: 404, data: 'not found'});

        $scope.$digest();
        expect(vm.loadError).toEqual('not found');
        expect(vm.loading).toBeFalsy();
      });

      it('is set to true while waiting for owner identifier', function() {
        $scope.$digest();
        expect(vm.loadError).toEqual(undefined);
        expect(vm.loading).toBeTruthy();
      });

      it('is set to true while waiting for product features', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        $scope.$digest();
        expect(vm.loadError).toEqual(undefined);
        expect(vm.loading).toBeTruthy();
      });

      it('is set to true while waiting for composite source control', function() {
        getByIdDeferred.resolve({name: 'rootOrganizationName', id: ROOT_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        $scope.$digest();
        expect(vm.loadError).toEqual(undefined);
        expect(vm.loading).toBeTruthy();
      });
    });
  });

  describe('organization', function() {
    const compositeSourceControl = {
      token: {
        value: null,
        parentName: null,
        parentValue: null
      },
      ownerId: SUB_ORGANIZATION_ID,
      id: 'ID',
      provider: 'gitlab',
      repositoryUrl: null,
      baseBranch: {
        value: 'BASE_BRANCH',
        parentName: 'Root Organization',
        parentValue: 'PARENT_BRANCH'
      },
      enableStatusChecks: {
        value: true,
        parentName: null,
        parentValue: null
      },
      enablePullRequests: {
        value: null,
        parentName: 'Root Organization',
        parentValue: true
      }
    };

    const sourceControlModel = {
      tokenInherit: true,
      token: null,
      tokenInheritFrom: null,
      baseBranchInherit: false,
      baseBranch: 'BASE_BRANCH',
      baseBranchInheritFrom: 'Root Organization',
      baseBranchInheritedValue: 'PARENT_BRANCH',
      ownerId: SUB_ORGANIZATION_ID,
      id: 'ID',
      provider: 'gitlab',
      repositoryUrl: null,
      enablePullRequests: null,
      enablePullRequestsInheritedValue: true,
      enablePullRequestsInheritFrom: 'Root Organization',
      enableStatusChecks: true,
      enableStatusChecksInheritedValue: null,
      enableStatusChecksInheritFrom: null
    };

    beforeEach(inject(function() {
      mockCLMContextLocations.isOrganization.and.returnValue(true);
      mockCLMContextLocations.isRootOrg.and.returnValue(false);
      mockCLMContextLocations.isApplication.and.returnValue(false);
      mockCLMContextLocations.getEntityId.and.returnValue(SUB_ORGANIZATION_ID);
      mockSourceControlService.updateSourceControlRecord.and.returnValue(saveResourceDefer.promise);
      mockSourceControlService.addSourceControlRecord.and.returnValue(saveResourceDefer.promise);
      mockOrganizationStore.getById.and.callFake(function(id) {
        return id === SUB_ORGANIZATION_ID ? getByIdDeferred.promise : null;
      });
      mockSourceControlService.getCompositeSourceControlRecord.and.callFake(function(ownerType, id) {
        return ownerType === 'organization' && id === SUB_ORGANIZATION_ID ? getSourceControlDeferred.promise : null;
      });

      vm = $componentController('sourceControlEditor', {
        $scope: $scope,
        CLMContextLocations: mockCLMContextLocations,
        OrganizationStore: mockOrganizationStore,
        ApplicationStore: mockApplicationStore,
        SourceControlService: mockSourceControlService,
        DeleteModalService: mockDeleteService,
        SameOwnerStateNavigationService: mockSameOwnerStateNavigationService,
        ProductFeatures: mockProductFeatures
      });
      vm.sourceControlEditor = {
        $setPristine: function() {
        }
      };
      vm.sourceControlEditorMask = {wrap: SpecUtil.promiseWrapper($q)};
    }));

    describe('doLoad', function() {
      it('sets the proper org and app owner flags', function() {
        expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
        expect(mockCLMContextLocations.isRootOrg).toHaveBeenCalled();
        expect(mockCLMContextLocations.isApplication).toHaveBeenCalled();
        expect(vm.isOrg).toBe(true);
        expect(vm.isRootOrg).toBe(false);
        expect(vm.isApp).toBe(false);

        mockCLMContextLocations.isOrganization.and.returnValue(false);
        vm = $componentController('sourceControlEditor', {
          $scope: $scope,
          CLMContextLocations: mockCLMContextLocations,
          OrganizationStore: mockOrganizationStore,
          ApplicationStore: mockApplicationStore,
          SourceControlService: mockSourceControlService
        });
        expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
        expect(mockCLMContextLocations.isRootOrg).toHaveBeenCalled();
        expect(mockCLMContextLocations.isApplication).toHaveBeenCalled();
        expect(vm.isOrg).toBe(false);
      });

      it('loads the owner name of the sub organization and reports on success', function() {
        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});

        $scope.$digest();

        expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
        expect(mockOrganizationStore.getById).toHaveBeenCalledWith(SUB_ORGANIZATION_ID);
        expect(vm.ownerName).toBe('subOrganizationName');
        expect(vm.loadError).toBeUndefined();
      });

      it('sets the error message on failure for the sub organization owner id', function() {
        getByIdDeferred.reject({status: 404, data: 'not found'});

        $scope.$digest();

        expect(vm.ownerName).toBeUndefined();
        expect(vm.loadError).toEqual('not found');
      });

      it('sets the error message on failure for the sub organization source control', function() {
        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.reject({status: 400, data: 'bad request'});

        $scope.$digest();

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toEqual('bad request');
      });

      it('sets the source control and does not report an error for the sub organization', function() {
        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });

      it('does not set the base branch to master if empty for the sub organization', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.value = null;
        let sourceControlModelCopy = angular.copy(sourceControlModel);
        sourceControlModelCopy.baseBranch = null;
        sourceControlModelCopy.baseBranchInherit = true;

        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);
        expect(vm.dirtySourceControl.baseBranch).toBeNull();
      });
    });

    describe('save', function() {
      it('creates a new entry if source control is not configured', function() {

        const retrievedSourceControlModel = {
          tokenInherit: true,
          token: null,
          tokenInheritFrom: null,
          baseBranchInherit: true,
          baseBranch: null,
          baseBranchInheritFrom: 'Root Organization',
          baseBranchInheritedValue: 'PARENT_BRANCH',
          ownerId: SUB_ORGANIZATION_ID,
          id: null,
          provider: 'gitlab',
          repositoryUrl: null,
          enablePullRequests: null,
          enablePullRequestsInheritedValue: true,
          enablePullRequestsInheritFrom: 'Root Organization',
          enableStatusChecks: null,
          enableStatusChecksInheritedValue: null,
          enableStatusChecksInheritFrom: null
        };

        const retrievedCompositeSourceControl = {
          token: {
            value: null,
            parentName: null,
            parentValue: null
          },
          ownerId: SUB_ORGANIZATION_ID,
          id: null,
          provider: 'gitlab',
          repositoryUrl: null,
          baseBranch: {
            value: null,
            parentName: 'Root Organization',
            parentValue: 'PARENT_BRANCH'
          },
          enableStatusChecks: {
            value: null,
            parentName: null,
            parentValue: null
          },
          enablePullRequests: {
            value: null,
            parentName: 'Root Organization',
            parentValue: true
          }
        };

        const savedSourceControl = {
          token: null,
          baseBranch: null,
          ownerId: SUB_ORGANIZATION_ID,
          id: null,
          enablePullRequests: true,
          enableStatusChecks: true
        };

        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(retrievedCompositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(retrievedSourceControlModel);
        expect(vm.originalSourceControl).toEqual(retrievedSourceControlModel);

        vm.dirtySourceControl = angular.copy(retrievedSourceControlModel);
        vm.dirtySourceControl.enablePullRequests = true;
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();
        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        // when
        vm.save();
        $scope.$digest();

        // then
        expect(mockSourceControlService.addSourceControlRecord).toHaveBeenCalledWith('organization',
            SUB_ORGANIZATION_ID, savedSourceControl);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });

      it('updates the existing entry if source control is configured', function() {

        const savedSourceControl = {
          token: null,
          ownerId: SUB_ORGANIZATION_ID,
          id: 'ID',
          baseBranch: 'BASE_BRANCH',
          enablePullRequests: true,
          enableStatusChecks: true
        };

        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        vm.dirtySourceControl = sourceControlModelCopy;
        vm.dirtySourceControl.enablePullRequests = true;
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();
        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        compositeSourceControlCopy.enablePullRequests.value = true;
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        // when
        vm.save();
        $scope.$digest();

        // then
        expect(mockSourceControlService.updateSourceControlRecord).toHaveBeenCalledWith('organization',
            SUB_ORGANIZATION_ID, savedSourceControl);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);
      });

      it('returns an error for unsuccessful save', function() {
        const savedSourceControl = {
          token: null,
          ownerId: SUB_ORGANIZATION_ID,
          id: 'ID',
          baseBranch: 'BASE_BRANCH',
          enablePullRequests: true,
          enableStatusChecks: true
        };

        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        vm.dirtySourceControl = sourceControlModelCopy;
        vm.dirtySourceControl.enablePullRequests = true;
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.reject({status: '400', data: 'bad request'});

        // when
        vm.save();
        $scope.$digest();

        // then
        expect(mockSourceControlService.updateSourceControlRecord).toHaveBeenCalledWith('organization',
            SUB_ORGANIZATION_ID, savedSourceControl);
        expect(vm.submitError).toEqual('bad request');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });
    });

    describe('isDirty', function() {
      it('returns false when changes have been applied to token and inherit is true', function() {
        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.token = 'new_token';

        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns false when changes have been applied to token and inherit is false', function() {
        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.token = 'new_token';
        vm.dirtySourceControl.tokenInherit = false;

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to tokenInherit', function() {
        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.tokenInherit = false;

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to baseBranch and inherit is false', function() {
        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.baseBranch = 'new_branch';
        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns false when changes have been applied to baseBranch and inherit is true', function() {
        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.value = null;
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toBeUndefined();

        vm.dirtySourceControl.baseBranch = 'new_branch';
        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns true when changes have been applied to baseBranchInherit', function() {
        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.baseBranchInherit = true;

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to enablePullRequests', function() {
        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.enablePullRequests = 'false';

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns false after save', function() {
        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.provider = 'github';
        compositeSourceControl.provider = 'gitlab';
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();
        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        // when
        vm.save();
        $scope.$digest();

        // then
        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns false after delete', function() {
        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        $timeout.flush();

        // when
        vm.deleteSourceControl();
        deleteServiceResourceDefer.resolve();
        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns false after load', function() {

        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        expect(vm.isDirty()).toBeFalsy();
      });
    });

    describe('statusChecksInheritText', function() {
      it('should return "Inherit (Not Configured)" if not defined elsewhere', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.enableStatusChecks.parentName = null;
        compositeSourceControlCopy.enableStatusChecks.parentValue = null;

        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.statusChecksInheritText).toEqual('Inherit (Not Configured)');
      });

      it('should return "Inherit from Org (Enabled)" if enabled on org', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.enableStatusChecks.parentName = 'Org';
        compositeSourceControlCopy.enableStatusChecks.parentValue = true;

        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.statusChecksInheritText).toEqual('Inherit from Org (Enabled)');
      });

      it('should return "Inherit from Org (Disabled)" if disabled on org', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.enableStatusChecks.parentName = 'Org';
        compositeSourceControlCopy.enableStatusChecks.parentValue = false;

        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.statusChecksInheritText).toEqual('Inherit from Org (Disabled)');
      });
    });

    describe('pullRequestsInheritText', function() {
      it('should return "Inherit (Not Configured)" if not defined elsewhere', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.enablePullRequests.parentName = null;
        compositeSourceControlCopy.enablePullRequests.parentValue = null;

        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.pullRequestsInheritText).toEqual('Inherit (Not Configured)');
      });

      it('should return "Inherit from Org (Enabled)" if enabled on org', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.enablePullRequests.parentName = 'Org';
        compositeSourceControlCopy.enablePullRequests.parentValue = true;

        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.pullRequestsInheritText).toEqual('Inherit from Org (Enabled)');
      });

      it('should return "Inherit from Org (Disabled)" if disabled on org', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.enablePullRequests.parentName = 'Org';
        compositeSourceControlCopy.enablePullRequests.parentValue = false;

        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.pullRequestsInheritText).toEqual('Inherit from Org (Disabled)');
      });
    });

    describe('baseBranchInheritText', function() {
      it('should return "Inherit (Not Configured)" if not defined elsewhere', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.parentName = null;
        compositeSourceControlCopy.baseBranch.parentValue = null;

        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.baseBranchInheritText).toEqual('Inherit (Not Configured)');
      });

      it('should return "Inherit from Org (value)" if set on org', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.parentName = 'Org';
        compositeSourceControlCopy.baseBranch.parentValue = 'value';

        getByIdDeferred.resolve({name: 'subOrganizationName', id: SUB_ORGANIZATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.baseBranchInheritText).toEqual('Inherit from Org (value)');
      });
    });
  });

  describe('application', function() {
    const compositeSourceControl = {
      token: {
        value: null,
        parentName: null,
        parentValue: null
      },
      ownerId: APPLICATION_ID,
      id: 'ID',
      provider: 'gitlab',
      repositoryUrl: null,
      baseBranch: {
        value: 'BASE_BRANCH',
        parentName: 'Root Organization',
        parentValue: 'PARENT_BRANCH'
      },
      enableStatusChecks: {
        value: true,
        parentName: null,
        parentValue: null
      },
      enablePullRequests: {
        value: null,
        parentName: 'Root Organization',
        parentValue: true
      }
    };

    const sourceControlModel = {
      tokenInherit: false,
      token: null,
      tokenInheritFrom: null,
      baseBranchInherit: false,
      baseBranch: 'BASE_BRANCH',
      baseBranchInheritFrom: 'Root Organization',
      baseBranchInheritedValue: 'PARENT_BRANCH',
      ownerId: APPLICATION_ID,
      id: 'ID',
      provider: 'gitlab',
      repositoryUrl: null,
      enablePullRequests: null,
      enablePullRequestsInheritedValue: true,
      enablePullRequestsInheritFrom: 'Root Organization',
      enableStatusChecks: true,
      enableStatusChecksInheritedValue: null,
      enableStatusChecksInheritFrom: null
    };

    beforeEach(inject(function() {
      mockCLMContextLocations.isOrganization.and.returnValue(false);
      mockCLMContextLocations.isRootOrg.and.returnValue(false);
      mockCLMContextLocations.isApplication.and.returnValue(true);
      mockCLMContextLocations.getEntityId.and.returnValue(APPLICATION_ID);
      mockSourceControlService.updateSourceControlRecord.and.returnValue(saveResourceDefer.promise);
      mockSourceControlService.addSourceControlRecord.and.returnValue(saveResourceDefer.promise);
      mockApplicationStore.getById.and.callFake(function(id) {
        return id === APPLICATION_ID ? getByIdDeferred.promise : null;
      });
      mockSourceControlService.getCompositeSourceControlRecord.and.callFake(function(ownerType, id) {
        return ownerType === 'application' && id === APPLICATION_ID ? getSourceControlDeferred.promise : null;
      });

      vm = $componentController('sourceControlEditor', {
        $scope: $scope,
        CLMContextLocations: mockCLMContextLocations,
        OrganizationStore: mockOrganizationStore,
        ApplicationStore: mockApplicationStore,
        SourceControlService: mockSourceControlService,
        DeleteModalService: mockDeleteService,
        SameOwnerStateNavigationService: mockSameOwnerStateNavigationService,
        ProductFeatures: mockProductFeatures
      });

      vm.sourceControlEditor = {
        $setPristine: function() {
        }
      };
      vm.sourceControlEditorMask = {wrap: SpecUtil.promiseWrapper($q)};
    }));

    describe('doLoad', function() {
      it('sets the proper org and app owner flags', function() {
        expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
        expect(mockCLMContextLocations.isRootOrg).toHaveBeenCalled();
        expect(mockCLMContextLocations.isApplication).toHaveBeenCalled();
        expect(vm.isOrg).toBe(false);
        expect(vm.isRootOrg).toBe(false);
        expect(vm.isApp).toBe(true);

        mockCLMContextLocations.isApplication.and.returnValue(false);
        vm = $componentController('sourceControlEditor', {
          $scope: $scope,
          CLMContextLocations: mockCLMContextLocations,
          OrganizationStore: mockOrganizationStore,
          ApplicationStore: mockApplicationStore,
          SourceControlService: mockSourceControlService
        });
        expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
        expect(mockCLMContextLocations.isRootOrg).toHaveBeenCalled();
        expect(mockCLMContextLocations.isApplication).toHaveBeenCalled();
        expect(vm.isApp).toBe(false);
      });

      it('loads the owner name of the application and reports on success', function() {
        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});

        $scope.$digest();

        expect(mockCLMContextLocations.getEntityId).toHaveBeenCalled();
        expect(mockApplicationStore.getById).toHaveBeenCalledWith(APPLICATION_ID);
        expect(vm.ownerName).toBe('applicationName');
        expect(vm.loadError).toBeUndefined();
      });

      it('sets the error message on failure for the application owner id', function() {
        getByIdDeferred.reject({status: 404, data: 'not found'});

        $scope.$digest();

        expect(vm.ownerName).toBeUndefined();
        expect(vm.loadError).toEqual('not found');
      });

      it('sets the error message on failure for the application source control', function() {
        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.reject({status: 400, data: 'bad request'});

        $scope.$digest();

        expect(vm.ownerName).toEqual('applicationName');
        expect(vm.loadError).toEqual('bad request');
      });

      it('sets the source control and does not report an error for the application', function() {
        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('applicationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });

      it('does not ses the base branch to master if empty for the application', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.value = null;
        let sourceControlModelCopy = angular.copy(sourceControlModel);
        sourceControlModelCopy.baseBranch = null;
        sourceControlModelCopy.baseBranchInherit = true;

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();

        expect(vm.ownerName).toEqual('applicationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);
        expect(vm.dirtySourceControl.baseBranch).toBeNull();
      });
    });

    describe('save', function() {
      it('creates a new entry if source control is not configured', function() {

        const retrievedSourceControlModel = {
          tokenInherit: false,
          token: null,
          tokenInheritFrom: null,
          baseBranchInherit: true,
          baseBranch: null,
          baseBranchInheritFrom: 'Root Organization',
          baseBranchInheritedValue: 'PARENT_BRANCH',
          ownerId: APPLICATION_ID,
          id: null,
          provider: 'gitlab',
          repositoryUrl: null,
          enablePullRequests: null,
          enablePullRequestsInheritedValue: true,
          enablePullRequestsInheritFrom: 'Sub Organization',
          enableStatusChecks: null,
          enableStatusChecksInheritedValue: null,
          enableStatusChecksInheritFrom: null
        };

        const retrievedCompositeSourceControl = {
          token: {
            value: null,
            parentName: null,
            parentValue: null
          },
          ownerId: APPLICATION_ID,
          id: null,
          provider: 'gitlab',
          repositoryUrl: null,
          baseBranch: {
            value: null,
            parentName: 'Root Organization',
            parentValue: 'PARENT_BRANCH'
          },
          enableStatusChecks: {
            value: null,
            parentName: null,
            parentValue: null
          },
          enablePullRequests: {
            value: null,
            parentName: 'Sub Organization',
            parentValue: true
          }
        };

        const savedSourceControl = {
          token: null,
          repositoryUrl: REPOSITORY_URL,
          baseBranch: null,
          ownerId: APPLICATION_ID,
          id: null,
          enablePullRequests: true,
          enableStatusChecks: true
        };

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(retrievedCompositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('applicationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(retrievedSourceControlModel);
        expect(vm.originalSourceControl).toEqual(retrievedSourceControlModel);

        vm.dirtySourceControl = angular.copy(retrievedSourceControlModel);
        vm.dirtySourceControl.enablePullRequests = true;
        vm.dirtySourceControl.repositoryUrl = REPOSITORY_URL;
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();
        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        // when
        vm.save();
        $scope.$digest();

        // then
        expect(mockSourceControlService.addSourceControlRecord).toHaveBeenCalledWith('application',
            APPLICATION_ID, savedSourceControl);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });

      it('updates the existing entry if source control is configured', function() {

        const savedSourceControl = {
          token: null,
          ownerId: APPLICATION_ID,
          id: 'ID',
          baseBranch: 'BASE_BRANCH',
          enablePullRequests: true,
          enableStatusChecks: true,
          repositoryUrl: REPOSITORY_URL
        };

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('applicationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        vm.dirtySourceControl = sourceControlModelCopy;
        vm.dirtySourceControl.enablePullRequests = true;
        vm.dirtySourceControl.repositoryUrl = REPOSITORY_URL;
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();
        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        compositeSourceControlCopy.enablePullRequests.value = true;
        compositeSourceControlCopy.repositoryUrl = REPOSITORY_URL;
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        // when
        vm.save();
        $scope.$digest();

        // then
        expect(mockSourceControlService.updateSourceControlRecord).toHaveBeenCalledWith('application',
            APPLICATION_ID, savedSourceControl);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);
      });

      it('returns an error for unsuccessful save', function() {
        const savedSourceControl = {
          token: null,
          ownerId: APPLICATION_ID,
          id: 'ID',
          baseBranch: 'BASE_BRANCH',
          enablePullRequests: true,
          enableStatusChecks: true,
          repositoryUrl: REPOSITORY_URL
        };

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('applicationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        vm.dirtySourceControl = sourceControlModelCopy;
        vm.dirtySourceControl.enablePullRequests = true;
        vm.dirtySourceControl.repositoryUrl = REPOSITORY_URL;
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.reject({status: '400', data: 'bad request'});

        // when
        vm.save();
        $scope.$digest();

        // then
        expect(mockSourceControlService.updateSourceControlRecord).toHaveBeenCalledWith('application',
            APPLICATION_ID, savedSourceControl);
        expect(vm.submitError).toEqual('bad request');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });
    });

    describe('isDirty', function() {
      it('returns false when changes have been applied to token and inherit is true', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = null;
        compositeSourceControlCopy.token.parentName = 'Root Organization';
        compositeSourceControlCopy.token.parentValue = 'TOKEN';

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        sourceControlModelCopy.token = null;
        sourceControlModelCopy.tokenInherit = true;
        sourceControlModelCopy.tokenInheritFrom = 'Root Organization';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();

        expect(vm.ownerName).toEqual('applicationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);

        vm.dirtySourceControl.token = 'new_token';

        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns false when changes have been applied to token and inherit is false', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = null;
        compositeSourceControlCopy.token.parentName = 'Root Organization';
        compositeSourceControlCopy.token.parentValue = 'TOKEN';

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        sourceControlModelCopy.token = null;
        sourceControlModelCopy.tokenInherit = true;
        sourceControlModelCopy.tokenInheritFrom = 'Root Organization';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();

        expect(vm.ownerName).toEqual('applicationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);

        vm.dirtySourceControl.token = 'new_token';
        vm.dirtySourceControl.tokenInherit = false;

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to tokenInherit', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = null;
        compositeSourceControlCopy.token.parentName = 'Root Organization';
        compositeSourceControlCopy.token.parentValue = 'TOKEN';

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        sourceControlModelCopy.token = null;
        sourceControlModelCopy.tokenInherit = true;
        sourceControlModelCopy.tokenInheritFrom = 'Root Organization';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();

        expect(vm.ownerName).toEqual('applicationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);

        vm.dirtySourceControl.tokenInherit = false;

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to baseBranch and inherit is false', function() {
        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('applicationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.baseBranch = 'new_branch';
        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns false when changes have been applied to baseBranch and inherit is true', function() {
        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.value = null;
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();

        expect(vm.ownerName).toEqual('applicationName');
        expect(vm.loadError).toBeUndefined();

        vm.dirtySourceControl.baseBranch = 'new_branch';
        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns true when changes have been applied to baseBranchInherit', function() {
        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('applicationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.baseBranchInherit = true;

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to enablePullRequests', function() {
        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('applicationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.enablePullRequests = 'false';

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to repositoryUrl', function() {
        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('applicationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.repositoryUrl = REPOSITORY_URL;

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns false after save', function() {
        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('applicationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.provider = 'github';
        compositeSourceControl.provider = 'gitlab';
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();
        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        // when
        vm.save();
        $scope.$digest();

        // then
        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns false after delete', function() {
        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('applicationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        $timeout.flush();

        // when
        vm.deleteSourceControl();
        deleteServiceResourceDefer.resolve();
        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns false after load', function() {

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();

        expect(vm.ownerName).toEqual('applicationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        expect(vm.isDirty()).toBeFalsy();
      });
    });

    describe('isAccessTokenRequiredOnNode', function() {
      it('should return true if token cannot be inherited', function() {
        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();
        expect(vm.isAccessTokenRequiredOnNode()).toBeTruthy();
      });

      it('should return false if token can be inherited', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.parentName = 'Root Organizaation';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.isAccessTokenRequiredOnNode()).toBeFalsy();
      });

      it('should return true if token is specified and not inheritable', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = 'TOKEN';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.isAccessTokenRequiredOnNode()).toBeTruthy();
      });
    });

    describe('showAdvanced', function() {
      it('should return true if token cannot be inherited', function() {
        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();
        expect(vm.showAdvanced).toBeTruthy();
      });

      it('should return false if token is inherited', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.parentName = 'Root Organizaation';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.showAdvanced).toBeFalsy();
      });

      it('should return false if token is specified and not inheritable', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = 'TOKEN';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.showAdvanced).toBeFalsy();
      });
    });

    describe('shouldShowAccessTokenWarning', function() {
      it('should return true if token cannot be inherited', function() {
        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControl);

        $scope.$digest();
        expect(vm.shouldShowAccessTokenWarning).toBeTruthy();
      });

      it('should return false if token is inherited', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.parentName = 'Root Organizaation';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.shouldShowAccessTokenWarning).toBeFalsy();
      });

      it('should return false if token is specified and not inheritable', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = 'TOKEN';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.shouldShowAccessTokenWarning).toBeFalsy();
      });
    });

    describe('canCollapseAdvanced', function() {
      it('should return true if token and branch is inherited', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.parentName = 'Root Organizaation';
        compositeSourceControlCopy.baseBranch.parentName = 'Root Organizaation';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.canCollapseAdvanced()).toBeTruthy();
      });

      it('should return true if token is specified and not inheritable and base branch is inherited', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = 'TOKEN';
        compositeSourceControlCopy.baseBranch.parentName = 'Root Organizaation';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.canCollapseAdvanced()).toBeTruthy();
      });

      it('should return true if token is specified and not inheritable and base branch is specified', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = 'TOKEN';
        compositeSourceControlCopy.baseBranch.value = 'branch';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.canCollapseAdvanced()).toBeTruthy();
      });

      it('should return true if token is inherited and base branch is specified', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.value = 'branch';
        compositeSourceControlCopy.token.parentName = 'Root Organization';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.canCollapseAdvanced()).toBeTruthy();
      });

      it('should return false if token is overridden and not specified and branch is specified', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.value = 'branch';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.canCollapseAdvanced()).toBeFalsy();
      });

      it('should return false if token is overridden and not specified and branch is inherited', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.parent = 'Root Organization';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.canCollapseAdvanced()).toBeFalsy();
      });

      it('should return false if branch is overridden and not specified and token is specified', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = 'TOKEN';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        vm.dirtySourceControl.baseBranchInherit = false;
        vm.dirtySourceControl.baseBranch = null;

        expect(vm.canCollapseAdvanced()).toBeFalsy();
      });

      it('should return false if branch is overridden and not specified and token is inherited', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.parentName = 'Root Organization';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        vm.dirtySourceControl.baseBranchInherit = false;
        vm.dirtySourceControl.baseBranch = null;

        expect(vm.canCollapseAdvanced()).toBeFalsy();
      });
    });

    describe('statusChecksInheritText', function() {
      it('should return "Inherit (Not Configured)" if not defined elsewhere', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.enableStatusChecks.parentName = null;
        compositeSourceControlCopy.enableStatusChecks.parentValue = null;

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.statusChecksInheritText).toEqual('Inherit (Not Configured)');
      });

      it('should return "Inherit from Org (Enabled)" if enabled on org', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.enableStatusChecks.parentName = 'Org';
        compositeSourceControlCopy.enableStatusChecks.parentValue = true;

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.statusChecksInheritText).toEqual('Inherit from Org (Enabled)');
      });

      it('should return "Inherit from Org (Disabled)" if disabled on org', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.enableStatusChecks.parentName = 'Org';
        compositeSourceControlCopy.enableStatusChecks.parentValue = false;

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.statusChecksInheritText).toEqual('Inherit from Org (Disabled)');
      });
    });

    describe('pullRequestsInheritText', function() {
      it('should return "Inherit (Not Configured)" if not defined elsewhere', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.enablePullRequests.parentName = null;
        compositeSourceControlCopy.enablePullRequests.parentValue = null;

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.pullRequestsInheritText).toEqual('Inherit (Not Configured)');
      });

      it('should return "Inherit from Org (Enabled)" if enabled on org', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.enablePullRequests.parentName = 'Org';
        compositeSourceControlCopy.enablePullRequests.parentValue = true;

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.pullRequestsInheritText).toEqual('Inherit from Org (Enabled)');
      });

      it('should return "Inherit from Org (Disabled)" if disabled on org', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.enablePullRequests.parentName = 'Org';
        compositeSourceControlCopy.enablePullRequests.parentValue = false;

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.pullRequestsInheritText).toEqual('Inherit from Org (Disabled)');
      });
    });

    describe('baseBranchInheritText', function() {
      it('should return "Inherit (Not Configured)" if not defined elsewhere', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.parentName = null;
        compositeSourceControlCopy.baseBranch.parentValue = null;

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.baseBranchInheritText).toEqual('Inherit (Not Configured)');
      });

      it('should return "Inherit from Org (value)" if set on org', function() {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.parentName = 'Org';
        compositeSourceControlCopy.baseBranch.parentValue = 'value';

        getByIdDeferred.resolve({name: 'applicationName', id: APPLICATION_ID});
        loadProductFeaturesDefer.resolve({});
        getSourceControlDeferred.resolve(compositeSourceControlCopy);

        $scope.$digest();
        expect(vm.baseBranchInheritText).toEqual('Inherit from Org (value)');
      });
    });
  });
});
