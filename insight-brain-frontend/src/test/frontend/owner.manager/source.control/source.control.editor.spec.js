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

describe('source.control.editor', function () {
  const ROOT_ORG_ID = 'rootOrganizationId';
  const ROOT_ORG_NAME = 'rootOrganizationName';
  const SUB_ORG_ID = 'subOrganizationId';
  const SUB_ORG_NAME = 'subOrganizationName';
  const APPLICATION_ID = 'applicationId';
  const APPLICATION_NAME = 'applicationName';
  const REPOSITORY_URL = 'https://a.com/b/c';
  const SSH_REPOSITORY_URL = 'ssh://a.com/b/c';
  const NOTIFICATIONS = 'notifications';
  const AUTOMATION = 'automation';

  let $rootScope,
    $scope,
    $q,
    $componentController,
    mockCLMContextLocations,
    getByIdDeferred,
    vm,
    mockSourceControlService,
    getSourceControlDeferred,
    deleteServiceResourceDefer,
    updateUrlDefer,
    saveResourceDefer,
    mockDeleteService = {
      deleteCustom: function (headerText, bodyText, maskText, continueAction) {
        continueAction();
        return deleteServiceResourceDefer.promise;
      },
    },
    mockUpdateUrlService = {
      updateSourceControl: function (continueAction) {
        continueAction();
        return updateUrlDefer.promise;
      },
    },
    mockSameOwnerStateNavigationService = {
      goEdit: jasmine.createSpy(),
    },
    $timeout;
  let loadApplicationsSpy;

  let setExpectations = function (sourceControlName, sourceControlId, sourceControlResult) {
    getByIdDeferred.resolve({
      payload: [
        {
          name: sourceControlName,
          id: sourceControlId,
        },
      ],
    });

    if (sourceControlResult) {
      if (sourceControlResult.reject) {
        getSourceControlDeferred.reject(sourceControlResult.reject);
      } else {
        getSourceControlDeferred.resolve(sourceControlResult);
      }
    }
  };

  let digest = function (sourceControlName, sourceControlId, sourceControlResult) {
    setExpectations(sourceControlName, sourceControlId, sourceControlResult);
    $scope.$digest();
  };

  let digestAfterSave = function (sourceControlName, sourceControlId, sourceControlResult) {
    setExpectations(sourceControlName, sourceControlId, sourceControlResult);
    vm.save();
    $scope.$digest();
  };

  let digestAfterSaveAndUpdateUrl = function (sourceControlName, sourceControlId, sourceControlResult) {
    setExpectations(sourceControlName, sourceControlId, sourceControlResult);
    vm.save();
    updateUrlDefer.resolve();
    $scope.$digest();
  };

  let getSourceControl = function (scOwnerId, scProvider, scBaseBranch, scRepositoryUrl) {
    return {
      username: null,
      token: null,
      provider: scProvider ? scProvider : null,
      repositoryUrl: scRepositoryUrl ? scRepositoryUrl : null,
      baseBranch: scBaseBranch ? scBaseBranch : null,
      ownerId: scOwnerId ? scOwnerId : null,
      id: null,
      pullRequestCommentingEnabled: null,
      remediationPullRequestsEnabled: null,
      sourceControlEvaluationsEnabled: null,
      statusChecksEnabled: true,
      sshEnabled: null,
    };
  };

  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(angular.mock.module(sourceControlModule.name, utilityModule.name));

  beforeEach(inject(function (_$rootScope_, $injector, _$componentController_, _$q_, _$timeout_) {
    $rootScope = _$rootScope_;
    $scope = $rootScope.$new();

    mockCLMContextLocations = jasmine.createSpyObj('CLMContextLocations', [
      'isOrganization',
      'getEntityId',
      'isApplication',
      'isRootOrg',
    ]);
    mockSourceControlService = jasmine.createSpyObj('mockSourceControlService', [
      'getCompositeSourceControlRecord',
      'getProviderTypes',
      'deleteSourceControlRecord',
      'addSourceControlRecord',
      'updateSourceControlRecord',
      'getProviderTypesMap',
      'getSourceControlMetrics',
    ]);
    $componentController = _$componentController_;
    $q = _$q_;
    getByIdDeferred = $q.defer();
    getSourceControlDeferred = $q.defer();
    deleteServiceResourceDefer = $q.defer();
    updateUrlDefer = $q.defer();
    saveResourceDefer = $q.defer();
    $timeout = _$timeout_;

    spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });
    spyOn(organizationsActions, 'loadOrganizations').and.returnValue(getByIdDeferred.promise);
    loadApplicationsSpy = spyOn(applicationActions, 'loadApplications').and.returnValue({
      payload: [
        {
          contact: null,
          id: APPLICATION_ID,
          name: APPLICATION_NAME,
          organizationId: '0a4ca3e6b672406892170481ef79799e',
          organizationName: 'org',
          publicId: APPLICATION_ID,
        },
      ],
    });

    mockSourceControlService.getProviderTypesMap.and.returnValue({
      azure: 'Azure DevOps',
      bitbucket: 'Bitbucket',
      github: 'GitHub',
      gitlab: 'GitLab',
    });
  }));

  describe('root organization', function () {
    const compositeSourceControl = {
      username: {
        value: null,
        parentName: null,
        parentValue: null,
      },
      token: {
        value: null,
        parentName: null,
        parentValue: null,
      },
      ownerId: ROOT_ORG_ID,
      id: 'ID',
      provider: {
        value: 'github',
        parentValue: null,
        parentName: null,
      },
      repositoryUrl: null,
      baseBranch: {
        value: 'BASE_BRANCH',
        parentName: null,
        parentValue: null,
      },
      pullRequestCommentingEnabled: {
        value: null,
        parentName: null,
        parentValue: null,
      },
      statusChecksEnabled: {
        value: true,
        parentName: null,
        parentValue: null,
      },
      remediationPullRequestsEnabled: {
        value: null,
        parentName: null,
        parentValue: null,
      },
      sourceControlEvaluationsEnabled: {
        value: null,
        parentName: null,
        parentValue: null,
      },
      sshEnabled: {
        value: null,
        parentName: null,
        parentValue: null,
      },
    };

    const sourceControlModel = {
      usernameInherit: false,
      credentialsInherit: false,
      username: null,
      usernameInheritFrom: null,
      usernameInheritedValue: null,
      tokenInherit: false,
      token: null,
      tokenInheritFrom: null,
      baseBranchInherit: false,
      baseBranch: 'BASE_BRANCH',
      baseBranchInheritFrom: null,
      baseBranchInheritedValue: null,
      ownerId: ROOT_ORG_ID,
      id: 'ID',
      provider: 'github',
      providerInherit: false,
      providerInheritFrom: null,
      providerInheritValue: null,
      pullRequestCommentingEnabled: true,
      pullRequestCommentingEnabledInheritFrom: null,
      pullRequestCommentingEnabledInheritedValue: null,
      repositoryUrl: null,
      remediationPullRequestsEnabled: false,
      remediationPullRequestsEnabledInheritedValue: null,
      remediationPullRequestsEnabledInheritFrom: null,
      sourceControlEvaluationsEnabled: true,
      sourceControlEvaluationsEnabledInheritedValue: null,
      sourceControlEvaluationsEnabledInheritFrom: null,
      statusChecksEnabled: true,
      statusChecksEnabledInheritedValue: null,
      statusChecksEnabledInheritFrom: null,
      sshEnabled: null,
      sshEnabledInheritFrom: null,
      sshEnabledInheritedValue: null,
    };

    beforeEach(inject(function () {
      mockCLMContextLocations.isOrganization.and.returnValue(true);
      mockCLMContextLocations.isRootOrg.and.returnValue(true);
      mockCLMContextLocations.isApplication.and.returnValue(false);
      mockCLMContextLocations.getEntityId.and.returnValue(ROOT_ORG_ID);
      mockSourceControlService.updateSourceControlRecord.and.returnValue(saveResourceDefer.promise);
      mockSourceControlService.addSourceControlRecord.and.returnValue(saveResourceDefer.promise);
      mockSourceControlService.getCompositeSourceControlRecord.and.callFake(function (ownerType, id) {
        return ownerType === 'organization' && id === ROOT_ORG_ID ? getSourceControlDeferred.promise : null;
      });
      mockSourceControlService.getSourceControlMetrics.and.callFake(function () {
        return { results: [] };
      });

      vm = $componentController('sourceControlEditor', {
        $scope: $scope,
        CLMContextLocations: mockCLMContextLocations,
        SourceControlService: mockSourceControlService,
        DeleteModalService: mockDeleteService,
        SameOwnerStateNavigationService: mockSameOwnerStateNavigationService,
        UpdateSourceControlModalService: mockUpdateUrlService,
      });
      vm.isAutomationSupported = true;
      vm.isSourceControlSupported = true;

      vm.sourceControlEditor = {
        $setPristine: function () {},
      };
      vm.sourceControlEditorMask = { wrap: SpecUtil.promiseWrapper($q) };
      vm.ownerName = ROOT_ORG_NAME;
      vm.ownerId = ROOT_ORG_ID;
    }));

    describe('doLoad', function () {
      it('sets the proper org and app owner flags', function () {
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
          SourceControlService: mockSourceControlService,
        });
        vm.isAutomationSupported = true;
        vm.isSourceControlSupported = true;
        expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
        expect(mockCLMContextLocations.isRootOrg).toHaveBeenCalled();
        expect(mockCLMContextLocations.isApplication).toHaveBeenCalled();
        expect(vm.isOrg).toBe(false);
      });

      it('loads the root org owner name and reports on success', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID);

        expect(vm.loadOrganizations).toHaveBeenCalled();
        expect(vm.ownerName).toBe(ROOT_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
      });

      it('sets the error message on failure for root organization owner id', function () {
        vm.ownerName = undefined;
        vm.ownerId = undefined;

        mockCLMContextLocations.getEntityId.and.returnValue(SUB_ORG_ID);
        getByIdDeferred.resolve({ payload: [{ name: ROOT_ORG_NAME, id: ROOT_ORG_ID }] });

        $scope.$digest();

        expect(vm.ownerName).toBeUndefined();
        expect(vm.loadError).toEqual(`Could not find an organization with ID ${SUB_ORG_ID}.`);
      });

      it('sets the error message on failure for the root organization source control', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, { reject: { status: 400, data: 'bad request' } });
        expect(vm.loadError).toEqual('bad request');
      });

      it('sets the source control and does not report an error for the root organization', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(ROOT_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });

      it('sets the base branch to master if empty for the root organization', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.value = null;
        let sourceControlModelCopy = angular.copy(sourceControlModel);
        sourceControlModelCopy.baseBranch = 'master';

        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControlCopy);

        expect(vm.ownerName).toEqual(ROOT_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);
        expect(vm.dirtySourceControl.baseBranch).toEqual('master');
      });
    });

    describe('deleteSourceControl', function () {
      it('deletes the existing entry without error', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(ROOT_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        // when
        vm.deleteSourceControl();
        deleteServiceResourceDefer.resolve();
        expect(mockSourceControlService.deleteSourceControlRecord).toHaveBeenCalledWith('organization', ROOT_ORG_ID);

        $scope.$digest();

        // then
        expect(mockSameOwnerStateNavigationService.goEdit).toHaveBeenCalledWith('edit-source-control');
        expect(vm.loadError).toBeUndefined();
      });
    });

    describe('save', function () {
      it('creates a new entry if source control is not configured', function () {
        const expectedSourceControlForSave = {
          ...getSourceControl(ROOT_ORG_ID, 'github', 'BASE_BRANCH'),
          ...{
            pullRequestCommentingEnabled: true,
            remediationPullRequestsEnabled: false,
            sourceControlEvaluationsEnabled: true,
          },
        };

        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(ROOT_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl = angular.copy(sourceControlModel);
        vm.dirtySourceControl.id = null;
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();

        // when
        digestAfterSave(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);

        // then
        expect(mockSourceControlService.addSourceControlRecord).toHaveBeenCalledWith(
          'organization',
          ROOT_ORG_ID,
          expectedSourceControlForSave
        );
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });

      it('updates the existing entry if source control is configured', function () {
        const expectedSourceControlForSave = {
          ...getSourceControl(ROOT_ORG_ID, 'gitlab', 'BASE_BRANCH'),
          ...{
            id: 'ID',
            pullRequestCommentingEnabled: true,
            remediationPullRequestsEnabled: false,
            sourceControlEvaluationsEnabled: true,
          },
        };

        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(ROOT_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        sourceControlModelCopy.provider = 'gitlab';
        vm.dirtySourceControl = sourceControlModelCopy;

        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.provider = {
          value: 'gitlab',
          parentValue: null,
          parentName: null,
        };

        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();

        // when
        digestAfterSave(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControlCopy);

        // then
        expect(mockSourceControlService.updateSourceControlRecord).toHaveBeenCalledWith(
          'organization',
          ROOT_ORG_ID,
          expectedSourceControlForSave
        );
        expect(vm.loadError).toBeUndefined();
        expect(vm.ownerType).toEqual('organization');
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);
      });

      it('returns an error for unsuccessful save', function () {
        const expectedSourceControlForSave = {
          ...getSourceControl(ROOT_ORG_ID, 'gitlab', 'BASE_BRANCH'),
          ...{
            id: 'ID',
            pullRequestCommentingEnabled: true,
            remediationPullRequestsEnabled: false,
            sourceControlEvaluationsEnabled: true,
          },
        };

        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(ROOT_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        vm.dirtySourceControl = sourceControlModelCopy;
        vm.dirtySourceControl.provider = 'gitlab';
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.reject({ status: '400', data: 'bad request' });

        // when
        vm.save();
        $scope.$digest();

        // then
        expect(mockSourceControlService.updateSourceControlRecord).toHaveBeenCalledWith(
          'organization',
          ROOT_ORG_ID,
          expectedSourceControlForSave
        );
        expect(vm.submitError).toEqual('bad request');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });
    });

    describe('isDirty', function () {
      it('returns true when changes have been applied to provider', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(ROOT_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.provider = 'gitlab';

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to token', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(ROOT_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.token = 'new_token';

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to baseBranch', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(ROOT_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.baseBranch = 'new_branch';

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to remediationPullRequestsEnabled', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(ROOT_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.remediationPullRequestsEnabled = 'false';
        vm.dirtySourceControl.provider = 'github';

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns false after save', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(ROOT_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.provider = 'gitlab';
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();

        // when
        digestAfterSave(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);

        // then
        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns false after delete', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(ROOT_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        $timeout.flush();

        // when
        vm.deleteSourceControl();
        deleteServiceResourceDefer.resolve();
        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns false after load', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(ROOT_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        expect(vm.isDirty()).toBeFalsy();
      });
    });

    describe('isAccessTokenRequiredOnNode', function () {
      it('should return false for root organization', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);
        expect(vm.isAccessTokenRequiredOnNode()).toBeFalsy();
      });
    });

    describe('showAdvanced', function () {
      it('should return true for root organization', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);
        expect(vm.showAdvanced).toBeTruthy();
      });
    });

    describe('showScmValidator', function () {
      it('should return false for root organization', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);
        expect(vm.showScmValidator).toBeFalsy();
      });
    });

    describe('shouldShowAccessTokenWarning', function () {
      it('should return false for root organization', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);
        expect(vm.shouldShowAccessTokenWarning).toBeFalsy();
      });
    });

    describe('canCollapseAdvanced', function () {
      it('should return false for root organization', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);
        expect(vm.canCollapseAdvanced()).toBeFalsy();
      });
    });

    describe('arePullRequestsSupported', function () {
      const testData = [
        { provider: 'bitbucket', isPrSupported: true },
        { provider: 'azure', isPrSupported: true },
        { provider: 'github', isPrSupported: true },
        { provider: 'gitlab', isPrSupported: true },
      ];

      for (var currentTest of testData) {
        it(`should return ${currentTest.isPrSupported} if license supports automation and provider is ${currentTest.provider}`, function () {
          digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);
          vm.dirtySourceControl.provider = currentTest.provider;
          expect(vm.arePullRequestsSupported()).toBe(currentTest.isPrSupported);
        });
      }
      it('should return false if license does not support automation', function () {
        vm.isAutomationSupported = false;
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl, NOTIFICATIONS);
        expect(vm.arePullRequestsSupported()).toBeFalsy();

        vm.dirtySourceControl.provider = 'gitlab';
        expect(vm.arePullRequestsSupported()).toBeFalsy();
      });
    });

    describe('getPullRequestsNotAvailableMessage', function () {
      it('should return message for provider if license supports automation', function () {
        vm.isAutomationSupported = true;
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);
        expect(vm.getPullRequestsNotAvailableMessage()).toEqual('');

        vm.dirtySourceControl.provider = 'gitlab';
        expect(vm.getPullRequestsNotAvailableMessage()).toEqual('');

        vm.dirtySourceControl.provider = 'bitbucket';
        expect(vm.getPullRequestsNotAvailableMessage()).toEqual('');
      });

      it('should return licencing message if license does not support automation', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl, NOTIFICATIONS);
        vm.isAutomationSupported = false;
        expect(vm.getPullRequestsNotAvailableMessage()).toEqual('This feature is not supported by your license');

        vm.dirtySourceControl.provider = 'gitlab';
        expect(vm.getPullRequestsNotAvailableMessage()).toEqual('This feature is not supported by your license');
      });
    });

    describe('isProviderSpecifiedAndPullRequestsSupported', function () {
      it('should return true for provider if license supports automation', function () {
        vm.isAutomationSupported = true;
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);
        expect(vm.isProviderSpecifiedAndPullRequestsSupported()).toBeTruthy();

        vm.dirtySourceControl.provider = 'gitlab';
        expect(vm.isProviderSpecifiedAndPullRequestsSupported()).toBeTruthy();

        vm.dirtySourceControl.provider = 'bitbucket';
        expect(vm.isProviderSpecifiedAndPullRequestsSupported()).toBeTruthy();

        vm.dirtySourceControl.provider = null;
        expect(vm.isProviderSpecifiedAndPullRequestsSupported()).toBeFalsy();
      });

      it('should return false if license does not support automation', function () {
        vm.isAutomationSupported = false;
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl, NOTIFICATIONS);
        expect(vm.isProviderSpecifiedAndPullRequestsSupported()).toBeFalsy();

        vm.dirtySourceControl.provider = 'gitlab';
        expect(vm.isProviderSpecifiedAndPullRequestsSupported()).toBeFalsy();

        vm.dirtySourceControl.provider = null;
        expect(vm.isProviderSpecifiedAndPullRequestsSupported()).toBeFalsy();
      });
    });

    describe('vm.loading', function () {
      it('is set to false when all calls success', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, compositeSourceControl);
        expect(vm.loading).toBeFalsy();
      });

      it('is set to false when owner identifier cannot be retrieved', function () {
        getByIdDeferred.reject({ status: 404, data: 'not found' });

        $scope.$digest();
        expect(vm.loadError).toEqual('not found');
        expect(vm.loading).toBeFalsy();
      });

      it('is set to false when composite source control cannot be retrieved', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID, { reject: { status: 404, data: 'not found' } });
        expect(vm.loadError).toEqual('not found');
        expect(vm.loading).toBeFalsy();
      });

      it('is set to true while waiting for owner identifier', function () {
        $scope.$digest();
        expect(vm.loadError).toEqual(undefined);
        expect(vm.loading).toBeTruthy();
      });

      it('is set to true while waiting for product features', function () {
        getByIdDeferred.resolve({ payload: [{ name: ROOT_ORG_NAME, id: ROOT_ORG_ID }] });
        $scope.$digest();
        expect(vm.loadError).toEqual(undefined);
        expect(vm.loading).toBeTruthy();
      });

      it('is set to true while waiting for composite source control', function () {
        digest(ROOT_ORG_NAME, ROOT_ORG_ID);
        expect(vm.loadError).toEqual(undefined);
        expect(vm.loading).toBeTruthy();
      });
    });
  });

  describe('organization', function () {
    const compositeSourceControl = {
      username: {
        value: null,
        parentName: null,
        parentValue: null,
      },
      token: {
        value: null,
        parentName: null,
        parentValue: null,
      },
      ownerId: SUB_ORG_ID,
      id: 'ID',
      provider: {
        value: null,
        parentValue: 'gitlab',
        parentName: 'Root Organization',
      },
      repositoryUrl: null,
      baseBranch: {
        value: 'BASE_BRANCH',
        parentName: 'Root Organization',
        parentValue: 'PARENT_BRANCH',
      },
      pullRequestCommentingEnabled: {
        value: null,
        parentName: 'Root Organization',
        parentValue: true,
      },
      statusChecksEnabled: {
        value: true,
        parentName: null,
        parentValue: null,
      },
      remediationPullRequestsEnabled: {
        value: null,
        parentName: 'Root Organization',
        parentValue: true,
      },
      sourceControlEvaluationsEnabled: {
        value: null,
        parentName: 'Root Organization',
        parentValue: true,
      },
      sshEnabled: {
        value: null,
        parentName: 'Root Organization',
        parentValue: true,
      },
    };

    const sourceControlModel = {
      usernameInherit: false,
      credentialsInherit: false,
      username: null,
      usernameInheritFrom: null,
      usernameInheritedValue: null,
      tokenInherit: true,
      token: null,
      tokenInheritFrom: null,
      baseBranchInherit: false,
      baseBranch: 'BASE_BRANCH',
      baseBranchInheritFrom: 'Root Organization',
      baseBranchInheritedValue: 'PARENT_BRANCH',
      ownerId: SUB_ORG_ID,
      id: 'ID',
      provider: null,
      providerInherit: true,
      providerInheritFrom: 'Root Organization',
      providerInheritValue: 'gitlab',
      repositoryUrl: null,
      pullRequestCommentingEnabled: null,
      pullRequestCommentingEnabledInheritedValue: true,
      pullRequestCommentingEnabledInheritFrom: 'Root Organization',
      remediationPullRequestsEnabled: null,
      remediationPullRequestsEnabledInheritedValue: true,
      remediationPullRequestsEnabledInheritFrom: 'Root Organization',
      sourceControlEvaluationsEnabled: null,
      sourceControlEvaluationsEnabledInheritedValue: true,
      sourceControlEvaluationsEnabledInheritFrom: 'Root Organization',
      statusChecksEnabled: true,
      statusChecksEnabledInheritedValue: null,
      statusChecksEnabledInheritFrom: null,
      sshEnabled: null,
      sshEnabledInheritedValue: true,
      sshEnabledInheritFrom: 'Root Organization',
    };

    beforeEach(inject(function () {
      mockCLMContextLocations.isOrganization.and.returnValue(true);
      mockCLMContextLocations.isRootOrg.and.returnValue(false);
      mockCLMContextLocations.isApplication.and.returnValue(false);
      mockSourceControlService.updateSourceControlRecord.and.returnValue(saveResourceDefer.promise);
      mockSourceControlService.addSourceControlRecord.and.returnValue(saveResourceDefer.promise);

      mockSourceControlService.getCompositeSourceControlRecord.and.callFake(function (ownerType, id) {
        return ownerType === 'organization' && id === SUB_ORG_ID ? getSourceControlDeferred.promise : null;
      });

      vm = $componentController('sourceControlEditor', {
        $scope: $scope,
        CLMContextLocations: mockCLMContextLocations,
        SourceControlService: mockSourceControlService,
        DeleteModalService: mockDeleteService,
        SameOwnerStateNavigationService: mockSameOwnerStateNavigationService,
        UpdateSourceControlModalService: mockUpdateUrlService,
      });
      vm.isAutomationSupported = true;
      vm.isSourceControlSupported = true;

      vm.sourceControlEditor = {
        $setPristine: function () {},
      };
      vm.sourceControlEditorMask = { wrap: SpecUtil.promiseWrapper($q) };
      vm.ownerName = SUB_ORG_NAME;
      vm.ownerId = SUB_ORG_ID;
    }));

    describe('doLoad', function () {
      it('sets the proper org and app owner flags', function () {
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
          SourceControlService: mockSourceControlService,
        });
        vm.isAutomationSupported = true;
        vm.isSourceControlSupported = true;

        expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
        expect(mockCLMContextLocations.isRootOrg).toHaveBeenCalled();
        expect(mockCLMContextLocations.isApplication).toHaveBeenCalled();
        expect(vm.isOrg).toBe(false);
      });

      it('loads the owner name of the sub organization and reports on success', function () {
        digest(SUB_ORG_NAME, SUB_ORG_ID);
        expect(vm.loadOrganizations).toHaveBeenCalled();
        expect(vm.ownerName).toBe(SUB_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
      });

      it('sets the error message on failure for the sub organization owner id', function () {
        vm.ownerName = undefined;
        getByIdDeferred.reject({ status: 404, data: 'not found' });

        $scope.$digest();

        expect(vm.ownerName).toBeUndefined();
        expect(vm.loadError).toEqual('not found');
      });

      it('sets the error message on failure for the sub organization source control', function () {
        digest(SUB_ORG_NAME, SUB_ORG_ID, { reject: { status: 400, data: 'bad request' } });
        expect(vm.ownerName).toEqual(SUB_ORG_NAME);
        expect(vm.loadError).toEqual('bad request');
      });

      it('sets the source control and does not report an error for the sub organization', function () {
        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControl);
        expect(vm.ownerName).toEqual(SUB_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });

      it('does not set the base branch to master if empty for the sub organization', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.value = null;
        let sourceControlModelCopy = angular.copy(sourceControlModel);
        sourceControlModelCopy.baseBranch = null;
        sourceControlModelCopy.baseBranchInherit = true;

        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);

        expect(vm.ownerName).toEqual(SUB_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);
        expect(vm.dirtySourceControl.baseBranch).toBeNull();
      });

      it('clears token inheritance when provider is set at org and we load the org', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.provider = {
          value: 'gitlab',
          parentValue: 'github',
          parentName: 'Root Organization',
        };
        compositeSourceControlCopy.token = {
          value: null,
          parentValue: 'redacted',
          parentName: 'Root Organization',
        };
        let sourceControlModelCopy = angular.copy(sourceControlModel);
        sourceControlModelCopy.providerInherit = false;
        sourceControlModelCopy.providerInheritFrom = 'Root Organization';
        sourceControlModelCopy.providerInheritValue = 'github';
        sourceControlModelCopy.provider = 'gitlab';

        // because token is set at root, it should be hidden by the provider
        sourceControlModelCopy.token = null;
        sourceControlModelCopy.tokenInherit = true;
        sourceControlModelCopy.tokenInheritFrom = 'Root Organization';

        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);

        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);
      });
    });

    describe('save', function () {
      it('creates a new entry if source control is not configured', function () {
        const retrievedSourceControlModel = {
          credentialsInherit: false,
          usernameInherit: false,
          username: null,
          usernameInheritFrom: null,
          usernameInheritedValue: null,
          tokenInherit: true,
          token: null,
          tokenInheritFrom: null,
          baseBranchInherit: true,
          baseBranch: null,
          baseBranchInheritFrom: 'Root Organization',
          baseBranchInheritedValue: 'PARENT_BRANCH',
          ownerId: SUB_ORG_ID,
          id: null,
          provider: null,
          providerInherit: true,
          providerInheritFrom: 'Root Organization',
          providerInheritValue: 'gitlab',
          repositoryUrl: null,
          pullRequestCommentingEnabled: null,
          pullRequestCommentingEnabledInheritedValue: true,
          pullRequestCommentingEnabledInheritFrom: 'Root Organization',
          remediationPullRequestsEnabled: null,
          remediationPullRequestsEnabledInheritedValue: true,
          remediationPullRequestsEnabledInheritFrom: 'Root Organization',
          sourceControlEvaluationsEnabled: null,
          sourceControlEvaluationsEnabledInheritedValue: true,
          sourceControlEvaluationsEnabledInheritFrom: 'Root Organization',
          statusChecksEnabled: null,
          statusChecksEnabledInheritedValue: null,
          statusChecksEnabledInheritFrom: null,
          sshEnabled: null,
          sshEnabledInheritedValue: null,
          sshEnabledInheritFrom: null,
        };

        const retrievedCompositeSourceControl = {
          username: {
            value: null,
            parentName: null,
            parentValue: null,
          },
          token: {
            value: null,
            parentName: null,
            parentValue: null,
          },
          ownerId: SUB_ORG_ID,
          id: null,
          provider: {
            value: null,
            parentValue: 'gitlab',
            parentName: 'Root Organization',
          },
          repositoryUrl: null,
          baseBranch: {
            value: null,
            parentName: 'Root Organization',
            parentValue: 'PARENT_BRANCH',
          },
          pullRequestCommentingEnabled: {
            value: null,
            parentName: 'Root Organization',
            parentValue: true,
          },
          statusChecksEnabled: {
            value: null,
            parentName: null,
            parentValue: null,
          },
          remediationPullRequestsEnabled: {
            value: null,
            parentName: 'Root Organization',
            parentValue: true,
          },
          sourceControlEvaluationsEnabled: {
            value: null,
            parentName: 'Root Organization',
            parentValue: true,
          },
          sshEnabled: {
            value: null,
            parentName: null,
            parentValue: null,
          },
        };

        const savedSourceControl = {
          ...getSourceControl(SUB_ORG_ID),
          ...{ remediationPullRequestsEnabled: true },
        };

        digest(SUB_ORG_NAME, SUB_ORG_ID, retrievedCompositeSourceControl);

        expect(vm.ownerName).toEqual(SUB_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(retrievedSourceControlModel);
        expect(vm.originalSourceControl).toEqual(retrievedSourceControlModel);

        vm.dirtySourceControl = angular.copy(retrievedSourceControlModel);
        vm.dirtySourceControl.remediationPullRequestsEnabled = true;
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();

        // when
        digestAfterSave(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControl);

        // then
        expect(mockSourceControlService.addSourceControlRecord).toHaveBeenCalledWith(
          'organization',
          SUB_ORG_ID,
          savedSourceControl
        );
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });

      it('updates the existing entry if source control is configured', function () {
        const savedSourceControl = {
          ...getSourceControl(SUB_ORG_ID, null, 'BASE_BRANCH'),
          ...{ id: 'ID', remediationPullRequestsEnabled: true },
        };

        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        vm.dirtySourceControl = sourceControlModelCopy;
        vm.dirtySourceControl.remediationPullRequestsEnabled = true;
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();

        compositeSourceControlCopy.remediationPullRequestsEnabled.value = true;

        // when
        digestAfterSave(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);

        // then
        expect(mockSourceControlService.updateSourceControlRecord).toHaveBeenCalledWith(
          'organization',
          SUB_ORG_ID,
          savedSourceControl
        );
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);
      });

      it('returns an error for unsuccessful save', function () {
        const savedSourceControl = {
          ...getSourceControl(SUB_ORG_ID, null, 'BASE_BRANCH'),
          ...{ id: 'ID', remediationPullRequestsEnabled: true },
        };

        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual('subOrganizationName');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        vm.dirtySourceControl = sourceControlModelCopy;
        vm.dirtySourceControl.remediationPullRequestsEnabled = true;
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.reject({ status: '400', data: 'bad request' });

        // when
        vm.save();
        $scope.$digest();

        // then
        expect(mockSourceControlService.updateSourceControlRecord).toHaveBeenCalledWith(
          'organization',
          SUB_ORG_ID,
          savedSourceControl
        );
        expect(vm.submitError).toEqual('bad request');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });
    });

    describe('isDirty', function () {
      it('returns true when changes have been applied to token and inherit is true', function () {
        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(SUB_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.token = 'new_token';
        vm.dirtySourceControl.tokenInherit = true;

        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns false when changes have been applied to token and inherit is false', function () {
        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(SUB_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.token = 'new_token';
        vm.dirtySourceControl.tokenInherit = false;

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to tokenInherit only after token has changed', function () {
        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(SUB_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.tokenInherit = false;

        expect(vm.isDirty()).toBeFalsy();

        vm.dirtySourceControl.token = 'new_token';

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true for bitbucket when changes have been applied to credentials and inherit is false', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.provider.parentValue = 'bitbucket';

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        sourceControlModelCopy.providerInheritValue = 'bitbucket';
        sourceControlModelCopy.credentialsInherit = true;
        sourceControlModelCopy.usernameInherit = true;

        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);

        expect(vm.ownerName).toEqual(SUB_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);

        // user selects to override credentials but only provide token (not username)
        vm.dirtySourceControl.credentialsInherit = false;
        vm.dirtySourceControl.token = 'new_token';

        expect(vm.isDirty()).toBeFalsy();

        // both username & token are provided
        vm.dirtySourceControl.username = 'new_user';

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to baseBranch and inherit is false', function () {
        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(SUB_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.baseBranch = 'new_branch';
        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns false when changes have been applied to baseBranch and inherit is true', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.value = null;

        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);

        expect(vm.ownerName).toEqual(SUB_ORG_NAME);
        expect(vm.loadError).toBeUndefined();

        vm.dirtySourceControl.baseBranch = 'new_branch';
        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns true when changes have been applied to baseBranchInherit', function () {
        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(SUB_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.baseBranchInherit = true;

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to remediationPullRequestsEnabled', function () {
        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(SUB_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.remediationPullRequestsEnabled = 'false';

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns false after save', function () {
        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(SUB_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.provider = 'github';
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();

        // when
        digestAfterSave(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControl);

        // then
        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns false after delete', function () {
        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(SUB_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        $timeout.flush();

        // when
        vm.deleteSourceControl();
        deleteServiceResourceDefer.resolve();
        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns false after load', function () {
        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(SUB_ORG_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        expect(vm.isDirty()).toBeFalsy();
      });
    });

    describe('statusChecksInheritText', function () {
      it('should return "Inherit (Not Configured)" if not defined elsewhere', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.statusChecksEnabled.parentName = null;
        compositeSourceControlCopy.statusChecksEnabled.parentValue = null;

        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);

        expect(vm.statusChecksInheritText).toEqual('Inherit (Not Configured)');
      });

      it('should return "Inherit from Org (Enabled)" if enabled on org', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.statusChecksEnabled.parentName = 'Org';
        compositeSourceControlCopy.statusChecksEnabled.parentValue = true;

        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);
        expect(vm.statusChecksInheritText).toEqual('Inherit from Org (Enabled)');
      });

      it('should return "Inherit from Org (Disabled)" if disabled on org', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.statusChecksEnabled.parentName = 'Org';
        compositeSourceControlCopy.statusChecksEnabled.parentValue = false;

        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);
        expect(vm.statusChecksInheritText).toEqual('Inherit from Org (Disabled)');
      });
    });

    describe('remediationPullRequestsInheritText', function () {
      it('should return "Inherit (Not Configured)" if not defined elsewhere', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.remediationPullRequestsEnabled.parentName = null;
        compositeSourceControlCopy.remediationPullRequestsEnabled.parentValue = null;

        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);
        expect(vm.remediationPullRequestsEnabledInheritText).toEqual('Inherit (Not Configured)');
      });

      it('should return "Inherit from Org (Enabled)" if enabled on org', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.remediationPullRequestsEnabled.parentName = 'Org';
        compositeSourceControlCopy.remediationPullRequestsEnabled.parentValue = true;

        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);
        expect(vm.remediationPullRequestsEnabledInheritText).toEqual('Inherit from Org (Enabled)');
      });

      it('should return "Inherit from Org (Disabled)" if disabled on org', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.remediationPullRequestsEnabled.parentName = 'Org';
        compositeSourceControlCopy.remediationPullRequestsEnabled.parentValue = false;

        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);
        expect(vm.remediationPullRequestsEnabledInheritText).toEqual('Inherit from Org (Disabled)');
      });
    });

    describe('sourceControlEvaluationsInheritText', function () {
      it('should return "Inherit (Not Configured)" if not defined elsewhere', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.sourceControlEvaluationsEnabled.parentName = null;
        compositeSourceControlCopy.sourceControlEvaluationsEnabled.parentValue = null;

        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);
        expect(vm.sourceControlEvaluationsEnabledInheritText).toEqual('Inherit (Not Configured)');
      });

      it('should return "Inherit from Org (Enabled)" if enabled on org', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.sourceControlEvaluationsEnabled.parentName = 'Org';
        compositeSourceControlCopy.sourceControlEvaluationsEnabled.parentValue = true;

        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);
        expect(vm.sourceControlEvaluationsEnabledInheritText).toEqual('Inherit from Org (Enabled)');
      });

      it('should return "Inherit from Org (Disabled)" if disabled on org', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.sourceControlEvaluationsEnabled.parentName = 'Org';
        compositeSourceControlCopy.sourceControlEvaluationsEnabled.parentValue = false;

        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);
        expect(vm.sourceControlEvaluationsEnabledInheritText).toEqual('Inherit from Org (Disabled)');
      });
    });

    describe('baseBranchInheritText', function () {
      it('should return "Inherit (Not Configured)" if not defined elsewhere', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.parentName = null;
        compositeSourceControlCopy.baseBranch.parentValue = null;

        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);
        expect(vm.baseBranchInheritText).toEqual('Inherit (Not Configured)');
      });

      it('should return "Inherit from Org (value)" if set on org', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.parentName = 'Org';
        compositeSourceControlCopy.baseBranch.parentValue = 'value';

        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);
        expect(vm.baseBranchInheritText).toEqual('Inherit from Org (value)');
      });
    });

    describe('showScmValidator', function () {
      it('should return false for an organization', function () {
        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControl);
        expect(vm.showScmValidator).toBeFalsy();
      });
    });

    describe('effectiveProvider', function () {
      it('should return inherited value when "inherit" is selected', function () {
        // given gitlab at the root
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.provider = {
          value: null,
          parentValue: 'gitlab',
          parentName: 'Root Organization',
        };
        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);

        // and the form provider is different
        vm.dirtySourceControl.provider = 'azure';

        // and inherit is true
        vm.dirtySourceControl.providerInherit = true;

        // then the effective provider is the one at the root, not the one on the form
        expect(vm.effectiveProvider()).toEqual('gitlab');
      });

      it('should return form value when "inherit" is not selected', function () {
        // given gitlab at the root
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.provider = {
          value: null,
          parentValue: 'gitlab',
          parentName: 'Root Organization',
        };
        digest(SUB_ORG_NAME, SUB_ORG_ID, compositeSourceControlCopy);

        // and the form provider is different
        vm.dirtySourceControl.provider = 'azure';

        // and inherit is falsey
        vm.dirtySourceControl.providerInherit = undefined;

        // then the effective provider is the one on the form
        expect(vm.effectiveProvider()).toEqual('azure');
      });
    });
  });

  describe('application not found', function () {
    const UNKNOWN_APP_ID = 'unknown_app_id';

    beforeEach(function () {
      mockCLMContextLocations.isOrganization.and.returnValue(false);
      mockCLMContextLocations.isRootOrg.and.returnValue(false);
      mockCLMContextLocations.isApplication.and.returnValue(true);

      loadApplicationsSpy.and.returnValue({
        error: `Could not find an application with ID ${UNKNOWN_APP_ID}.`,
      });
      vm = $componentController('sourceControlEditor', {
        $scope: $scope,
        CLMContextLocations: mockCLMContextLocations,
        SourceControlService: mockSourceControlService,
        DeleteModalService: mockDeleteService,
        SameOwnerStateNavigationService: mockSameOwnerStateNavigationService,
        UpdateSourceControlModalService: mockUpdateUrlService,
      });
      $scope.$digest();
    });

    it('sets the error message on failure for the application owner id', function () {
      expect(vm.ownerName).toBeUndefined();
      expect(vm.loadError).toEqual(`Could not find an application with ID ${UNKNOWN_APP_ID}.`);
    });
  });

  describe('application', function () {
    const compositeSourceControl = {
      usernameInheritedValue: null,
      credentialsInherit: false,
      username: {
        value: null,
        parentName: null,
        parentValue: null,
      },
      token: {
        value: null,
        parentName: null,
        parentValue: null,
      },
      ownerId: APPLICATION_ID,
      id: 'ID',
      provider: {
        value: null,
        parentValue: 'gitlab',
        parentName: 'Root Organization',
      },
      repositoryUrl: null,
      baseBranch: {
        value: 'BASE_BRANCH',
        parentName: 'Root Organization',
        parentValue: 'PARENT_BRANCH',
      },
      pullRequestCommentingEnabled: {
        value: null,
        parentName: 'Root Organization',
        parentValue: true,
      },
      statusChecksEnabled: {
        value: true,
        parentName: null,
        parentValue: null,
      },
      remediationPullRequestsEnabled: {
        value: null,
        parentName: 'Root Organization',
        parentValue: true,
      },
      sourceControlEvaluationsEnabled: {
        value: null,
        parentName: 'Root Organization',
        parentValue: true,
      },
      sshEnabled: {
        value: true,
        parentName: null,
        parentValue: null,
      },
    };

    const sourceControlModel = {
      credentialsInherit: false,
      usernameInheritedValue: null,
      usernameInherit: false,
      username: null,
      usernameInheritFrom: null,
      tokenInherit: false,
      token: null,
      tokenInheritFrom: null,
      baseBranchInherit: false,
      baseBranch: 'BASE_BRANCH',
      baseBranchInheritFrom: 'Root Organization',
      baseBranchInheritedValue: 'PARENT_BRANCH',
      ownerId: APPLICATION_ID,
      id: 'ID',
      provider: null,
      providerInheritFrom: 'Root Organization',
      providerInherit: true,
      providerInheritValue: 'gitlab',
      repositoryUrl: null,
      pullRequestCommentingEnabled: null,
      pullRequestCommentingEnabledInheritedValue: true,
      pullRequestCommentingEnabledInheritFrom: 'Root Organization',
      remediationPullRequestsEnabled: null,
      remediationPullRequestsEnabledInheritedValue: true,
      remediationPullRequestsEnabledInheritFrom: 'Root Organization',
      sourceControlEvaluationsEnabled: null,
      sourceControlEvaluationsEnabledInheritedValue: true,
      sourceControlEvaluationsEnabledInheritFrom: 'Root Organization',
      statusChecksEnabled: true,
      statusChecksEnabledInheritedValue: null,
      statusChecksEnabledInheritFrom: null,
      sshEnabled: true,
      sshEnabledInheritFrom: null,
      sshEnabledInheritedValue: null,
    };

    beforeEach(inject(function () {
      mockCLMContextLocations.isOrganization.and.returnValue(false);
      mockCLMContextLocations.isRootOrg.and.returnValue(false);
      mockCLMContextLocations.isApplication.and.returnValue(true);
      mockSourceControlService.updateSourceControlRecord.and.returnValue(saveResourceDefer.promise);
      mockSourceControlService.addSourceControlRecord.and.returnValue(saveResourceDefer.promise);
      mockSourceControlService.getCompositeSourceControlRecord.and.callFake(function (ownerType, id) {
        return ownerType === 'application' && id === APPLICATION_ID ? getSourceControlDeferred.promise : null;
      });

      vm = $componentController('sourceControlEditor', {
        $scope: $scope,
        CLMContextLocations: mockCLMContextLocations,
        SourceControlService: mockSourceControlService,
        DeleteModalService: mockDeleteService,
        SameOwnerStateNavigationService: mockSameOwnerStateNavigationService,
        UpdateSourceControlModalService: mockUpdateUrlService,
      });
      vm.isAutomationSupported = true;
      vm.isSourceControlSupported = true;

      vm.sourceControlEditor = {
        $setPristine: function () {},
      };
      vm.sourceControlEditorMask = { wrap: SpecUtil.promiseWrapper($q) };
      vm.ownerName = APPLICATION_NAME;
      vm.ownerId = APPLICATION_ID;
    }));

    describe('doLoad', function () {
      it('sets the proper org and app owner flags', function () {
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
          SourceControlService: mockSourceControlService,
        });
        vm.isAutomationSupported = true;
        vm.isSourceControlSupported = true;

        expect(mockCLMContextLocations.isOrganization).toHaveBeenCalled();
        expect(mockCLMContextLocations.isRootOrg).toHaveBeenCalled();
        expect(mockCLMContextLocations.isApplication).toHaveBeenCalled();
        expect(vm.isApp).toBe(false);
      });

      it('loads the owner name of the application and reports on success', function () {
        digest(APPLICATION_NAME, APPLICATION_ID);

        expect(vm.ownerName).toBe(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
      });

      it('sets the error message on failure for the application source control', function () {
        digest(APPLICATION_NAME, APPLICATION_ID, { reject: { status: 400, data: 'bad request' } });
        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toEqual('bad request');
      });

      it('sets the source control and does not report an error for the application', function () {
        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });

      it('does not set the base branch to master if empty for the application', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.value = null;
        let sourceControlModelCopy = angular.copy(sourceControlModel);
        sourceControlModelCopy.baseBranch = null;
        sourceControlModelCopy.baseBranchInherit = true;

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);
        expect(vm.dirtySourceControl.baseBranch).toBeNull();
      });

      it('clears token inheritance when provider is set at org and we load an app', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.provider = {
          value: null,
          parentValue: 'gitlab',
          parentName: 'non-root org name',
        };
        compositeSourceControlCopy.token = {
          value: null,
          parentValue: 'redacted',
          parentName: 'Root Organization',
        };

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        sourceControlModelCopy.providerInherit = true;
        sourceControlModelCopy.providerInheritFrom = 'non-root org name';
        sourceControlModelCopy.providerInheritValue = 'gitlab';

        // because token is at root and provider is not, tokenInherit should be false
        sourceControlModelCopy.tokenInherit = false;
        sourceControlModelCopy.token = null;

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);

        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);
      });
    });

    describe('save', function () {
      it('creates a new entry if source control is not configured', function () {
        const retrievedSourceControlModel = {
          usernameInheritedValue: null,
          credentialsInherit: false,
          usernameInherit: false,
          username: null,
          usernameInheritFrom: null,
          tokenInherit: false,
          token: null,
          tokenInheritFrom: null,
          baseBranchInherit: true,
          baseBranch: null,
          baseBranchInheritFrom: 'Root Organization',
          baseBranchInheritedValue: 'PARENT_BRANCH',
          ownerId: APPLICATION_ID,
          id: null,
          provider: null,
          providerInheritFrom: 'Root Organization',
          providerInherit: true,
          providerInheritValue: 'gitlab',
          repositoryUrl: null,
          pullRequestCommentingEnabled: null,
          pullRequestCommentingEnabledInheritedValue: true,
          pullRequestCommentingEnabledInheritFrom: 'Sub Organization',
          remediationPullRequestsEnabled: null,
          remediationPullRequestsEnabledInheritedValue: true,
          remediationPullRequestsEnabledInheritFrom: 'Sub Organization',
          sourceControlEvaluationsEnabled: null,
          sourceControlEvaluationsEnabledInheritedValue: true,
          sourceControlEvaluationsEnabledInheritFrom: 'Sub Organization',
          statusChecksEnabled: null,
          statusChecksEnabledInheritedValue: null,
          statusChecksEnabledInheritFrom: null,
          sshEnabled: null,
          sshEnabledInheritedValue: true,
          sshEnabledInheritFrom: 'Sub Organization',
        };

        const retrievedCompositeSourceControl = {
          username: {
            value: null,
            parentName: null,
            parentValue: null,
          },
          token: {
            value: null,
            parentName: null,
            parentValue: null,
          },
          ownerId: APPLICATION_ID,
          id: null,
          provider: {
            value: null,
            parentValue: 'gitlab',
            parentName: 'Root Organization',
          },
          repositoryUrl: null,
          baseBranch: {
            value: null,
            parentName: 'Root Organization',
            parentValue: 'PARENT_BRANCH',
          },
          pullRequestCommentingEnabled: {
            value: null,
            parentName: 'Sub Organization',
            parentValue: true,
          },
          statusChecksEnabled: {
            value: null,
            parentName: null,
            parentValue: null,
          },
          remediationPullRequestsEnabled: {
            value: null,
            parentName: 'Sub Organization',
            parentValue: true,
          },
          sourceControlEvaluationsEnabled: {
            value: null,
            parentName: 'Sub Organization',
            parentValue: true,
          },
          sshEnabled: {
            value: null,
            parentName: 'Sub Organization',
            parentValue: true,
          },
        };

        const savedSourceControl = {
          ...getSourceControl(APPLICATION_ID, null, null, REPOSITORY_URL),
          ...{ remediationPullRequestsEnabled: true },
        };

        digest(APPLICATION_NAME, APPLICATION_ID, retrievedCompositeSourceControl);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(retrievedSourceControlModel);
        expect(vm.originalSourceControl).toEqual(retrievedSourceControlModel);

        vm.dirtySourceControl = angular.copy(retrievedSourceControlModel);
        vm.dirtySourceControl.remediationPullRequestsEnabled = true;
        vm.dirtySourceControl.repositoryUrl = REPOSITORY_URL;
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();

        // when
        digestAfterSave(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);

        // then
        expect(mockSourceControlService.addSourceControlRecord).toHaveBeenCalledWith(
          'application',
          APPLICATION_ID,
          savedSourceControl
        );
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });

      it('updates the existing entry if source control is configured', function () {
        const savedSourceControl = {
          ...getSourceControl(APPLICATION_ID, null, 'BASE_BRANCH', REPOSITORY_URL),
          ...{ id: 'ID', remediationPullRequestsEnabled: true, sshEnabled: true },
        };

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        vm.dirtySourceControl = sourceControlModelCopy;
        vm.dirtySourceControl.remediationPullRequestsEnabled = true;
        vm.dirtySourceControl.repositoryUrl = REPOSITORY_URL;
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();

        compositeSourceControlCopy.remediationPullRequestsEnabled.value = true;
        compositeSourceControlCopy.repositoryUrl = REPOSITORY_URL;

        // when
        digestAfterSaveAndUpdateUrl(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);

        // then
        expect(mockSourceControlService.updateSourceControlRecord).toHaveBeenCalledWith(
          'application',
          APPLICATION_ID,
          savedSourceControl
        );
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);
      });

      it('updates the existing entry if source control is configured - ssh url', function () {
        const saveSourceControl = {
          ...getSourceControl(APPLICATION_ID, null, 'BASE_BRANCH', SSH_REPOSITORY_URL),
          ...{ id: 'ID', remediationPullRequestsEnabled: true, sshEnabled: true },
        };

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        vm.dirtySourceControl = sourceControlModelCopy;
        vm.dirtySourceControl.remediationPullRequestsEnabled = true;
        vm.dirtySourceControl.repositoryUrl = SSH_REPOSITORY_URL;
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();

        compositeSourceControlCopy.remediationPullRequestsEnabled.value = true;
        compositeSourceControlCopy.repositoryUrl = SSH_REPOSITORY_URL;

        // when
        digestAfterSaveAndUpdateUrl(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);

        // then
        expect(mockSourceControlService.updateSourceControlRecord).toHaveBeenCalledWith(
          'application',
          APPLICATION_ID,
          saveSourceControl
        );
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);
      });

      it('returns an error for unsuccessful save', function () {
        const savedSourceControl = {
          ...getSourceControl(APPLICATION_ID, null, 'BASE_BRANCH', REPOSITORY_URL),
          ...{ id: 'ID', remediationPullRequestsEnabled: true, sshEnabled: true },
        };

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        vm.dirtySourceControl = sourceControlModelCopy;
        vm.dirtySourceControl.remediationPullRequestsEnabled = true;
        vm.dirtySourceControl.repositoryUrl = REPOSITORY_URL;
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        updateUrlDefer.reject({ status: '400', data: 'bad request' });
        // when
        vm.save();
        $scope.$digest();

        // then
        expect(mockSourceControlService.updateSourceControlRecord).toHaveBeenCalledWith(
          'application',
          APPLICATION_ID,
          savedSourceControl
        );
        expect(vm.submitError).toEqual('bad request');
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);
      });

      it('clears any SCM test results after a save', function () {
        let sourceControlModelCopy = angular.copy(sourceControlModel);
        vm.dirtySourceControl = sourceControlModelCopy;
        vm.dirtySourceControl.remediationPullRequestsEnabled = true;
        vm.dirtySourceControl.repositoryUrl = REPOSITORY_URL + '-changed';

        vm.scmConfigValidationResult = { body: 'sample' };

        // when
        vm.save();
        $scope.$digest();

        // then
        expect(vm.scmConfigValidationResult).toBeUndefined();
      });

      it('requires confirmation when URL is updated', function () {
        const savedSourceControl = {
          ...getSourceControl(APPLICATION_ID, null, 'BASE_BRANCH', REPOSITORY_URL),
          ...{ id: 'ID', remediationPullRequestsEnabled: true, sshEnabled: true },
        };

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        vm.dirtySourceControl = sourceControlModelCopy;
        vm.dirtySourceControl.remediationPullRequestsEnabled = true;
        vm.dirtySourceControl.repositoryUrl = REPOSITORY_URL;
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();

        compositeSourceControlCopy.remediationPullRequestsEnabled.value = true;
        compositeSourceControlCopy.repositoryUrl = REPOSITORY_URL;

        // when
        digestAfterSaveAndUpdateUrl(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);

        // then
        expect(mockSourceControlService.updateSourceControlRecord).toHaveBeenCalledWith(
          'application',
          APPLICATION_ID,
          savedSourceControl
        );
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);
      });

      it('does not require confirmation when URL is not updated', function () {
        const savedSourceControl = {
          ...getSourceControl(APPLICATION_ID, null, 'BASE_BRANCH', REPOSITORY_URL),
          ...{ id: 'ID', remediationPullRequestsEnabled: true, sshEnabled: true },
        };

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        vm.dirtySourceControl = sourceControlModelCopy;
        vm.dirtySourceControl.remediationPullRequestsEnabled = true;
        vm.dirtySourceControl.repositoryUrl = REPOSITORY_URL;
        vm.originalSourceControl.repositoryUrl = REPOSITORY_URL;
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();

        compositeSourceControlCopy.remediationPullRequestsEnabled.value = true;
        compositeSourceControlCopy.repositoryUrl = REPOSITORY_URL;

        // when
        digestAfterSave(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);

        // then
        expect(mockSourceControlService.updateSourceControlRecord).toHaveBeenCalledWith(
          'application',
          APPLICATION_ID,
          savedSourceControl
        );
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);
      });
    });

    describe('isDirty', function () {
      it('returns false when changes have been applied to token and inherit is true', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = null;
        compositeSourceControlCopy.token.parentName = 'Root Organization';
        compositeSourceControlCopy.token.parentValue = 'TOKEN';

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        sourceControlModelCopy.token = null;
        sourceControlModelCopy.tokenInherit = true;
        sourceControlModelCopy.tokenInheritFrom = 'Root Organization';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);

        vm.dirtySourceControl.token = 'new_token';

        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns false when changes have been applied to token and inherit is false', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = null;
        compositeSourceControlCopy.token.parentName = 'Root Organization';
        compositeSourceControlCopy.token.parentValue = 'TOKEN';

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        sourceControlModelCopy.token = null;
        sourceControlModelCopy.tokenInherit = true;
        sourceControlModelCopy.tokenInheritFrom = 'Root Organization';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);

        vm.dirtySourceControl.token = 'new_token';
        vm.dirtySourceControl.tokenInherit = false;

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to tokenInherit only after token has changed', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = null;
        compositeSourceControlCopy.token.parentName = 'Root Organization';
        compositeSourceControlCopy.token.parentValue = 'TOKEN';

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        sourceControlModelCopy.token = null;
        sourceControlModelCopy.tokenInherit = true;
        sourceControlModelCopy.tokenInheritFrom = 'Root Organization';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);

        vm.dirtySourceControl.tokenInherit = false;

        expect(vm.isDirty()).toBeFalsy();

        vm.dirtySourceControl.token = 'new_token';

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true for bitbucket when changes have been applied to credentials and inherit is false', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.provider = {
          value: null,
          parentValue: 'bitbucket',
          parentName: 'Root Organization',
        };
        compositeSourceControlCopy.token.value = null;
        compositeSourceControlCopy.token.parentName = 'Root Organization';
        compositeSourceControlCopy.token.parentValue = 'TOKEN';
        compositeSourceControlCopy.username.value = null;
        compositeSourceControlCopy.username.parentName = 'Root Organization';
        compositeSourceControlCopy.username.parentValue = 'username';

        let sourceControlModelCopy = angular.copy(sourceControlModel);
        sourceControlModelCopy.providerInherit = true;
        sourceControlModelCopy.providerInheritValue = 'bitbucket';
        sourceControlModelCopy.providerInheritFrom = 'Root Organization';
        sourceControlModelCopy.credentialsInherit = true;
        sourceControlModelCopy.token = null;
        sourceControlModelCopy.tokenInherit = true;
        sourceControlModelCopy.tokenInheritFrom = 'Root Organization';
        sourceControlModelCopy.username = null;
        sourceControlModelCopy.usernameInherit = true;
        sourceControlModelCopy.usernameInheritedValue = 'username';
        sourceControlModelCopy.usernameInheritFrom = 'Root Organization';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModelCopy);
        expect(vm.originalSourceControl).toEqual(sourceControlModelCopy);

        // user selects to override credentials but only provide token (not username)
        vm.dirtySourceControl.credentialsInherit = false;
        vm.dirtySourceControl.token = 'new_token';

        expect(vm.isDirty()).toBeFalsy();

        // both username & token are provided
        vm.dirtySourceControl.username = 'new_user';

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to baseBranch and inherit is false', function () {
        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.baseBranch = 'new_branch';
        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns false when changes have been applied to baseBranch and inherit is true', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.value = null;

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();

        vm.dirtySourceControl.baseBranch = 'new_branch';
        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns true when changes have been applied to baseBranchInherit', function () {
        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.baseBranchInherit = true;

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to remediationPullRequestsEnabled', function () {
        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.remediationPullRequestsEnabled = 'false';

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns true when changes have been applied to repositoryUrl', function () {
        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.repositoryUrl = REPOSITORY_URL;

        expect(vm.isDirty()).toBeTruthy();
      });

      it('returns false after save', function () {
        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        vm.dirtySourceControl.provider = 'github';
        getByIdDeferred = $q.defer();
        getSourceControlDeferred = $q.defer();
        saveResourceDefer.resolve();

        // when
        digestAfterSave(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);

        // then
        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns false after delete', function () {
        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        $timeout.flush();

        // when
        vm.deleteSourceControl();
        deleteServiceResourceDefer.resolve();
        expect(vm.isDirty()).toBeFalsy();
      });

      it('returns false after load', function () {
        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);

        expect(vm.ownerName).toEqual(APPLICATION_NAME);
        expect(vm.loadError).toBeUndefined();
        expect(vm.dirtySourceControl).toEqual(sourceControlModel);
        expect(vm.originalSourceControl).toEqual(sourceControlModel);

        expect(vm.isDirty()).toBeFalsy();
      });
    });

    describe('isAccessTokenRequiredOnNode', function () {
      it('should return true if token cannot be inherited', function () {
        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);
        expect(vm.isAccessTokenRequiredOnNode()).toBeTruthy();
      });

      it('should return false if token can be inherited', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.parentName = 'Root Organization';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.isAccessTokenRequiredOnNode()).toBeFalsy();
      });

      it('should return true if token is specified and not inheritable', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = 'TOKEN';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.isAccessTokenRequiredOnNode()).toBeTruthy();
      });
    });

    describe('showScmValidator', function () {
      it('should return false for an organization', function () {
        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);
        // this should be false until the feature is released and the feature flag is removed
        expect(vm.showScmValidator).toBeFalsy();
      });
    });

    describe('sshEnabled', () => {
      it('returns true when SSH Enabled at the app', () => {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.sshEnabled = {
          value: true,
          parentName: null,
          parentValue: null,
        };

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.sshEnabled()).toBeTrue();
      });

      it('returns true when SSH enabled at the org', () => {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.sshEnabled = {
          value: null,
          parentName: 'Org',
          parentValue: true,
        };

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.sshEnabled()).toBeTrue();
      });

      it('returns false when not enabled', () => {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.sshEnabled = {
          value: null,
          parentName: null,
          parentValue: null,
        };

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.sshEnabled()).toBeFalsy();
      });
    });

    describe('isUsernameRequiredOnNode', function () {
      const testData = [
        { provider: 'bitbucket', usernameRequired: true },
        { provider: 'azure', usernameRequired: true },
        { provider: 'github', usernameRequired: false },
        { provider: 'gitlab', usernameRequired: false },
      ];

      for (var currentTest of testData) {
        it(`should return ${currentTest.usernameRequired} if username cannot be inherited on ${currentTest.provider}`, function () {
          let compositeSourceControlCopy = angular.copy(compositeSourceControl);
          compositeSourceControlCopy.provider.parentValue = currentTest.provider;

          digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
          expect(vm.isUsernameRequiredOnNode()).toBe(currentTest.usernameRequired);
        });

        it(`should return false if username can be inherited on ${currentTest.provider}`, function () {
          let compositeSourceControlCopy = angular.copy(compositeSourceControl);
          compositeSourceControlCopy.token.parentName = 'Root Organization';
          compositeSourceControlCopy.username.parentName = 'Root Organization';
          compositeSourceControlCopy.provider = {
            value: null,
            parentValue: currentTest.provider,
            parentName: 'Root Organization',
          };

          digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
          expect(vm.isUsernameRequiredOnNode()).toBeFalsy();
        });

        it(`should return true if username is not specified on ${currentTest.provider}`, function () {
          let compositeSourceControlCopy = angular.copy(compositeSourceControl);
          compositeSourceControlCopy.provider.parentValue = currentTest.provider;

          digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
          expect(vm.isUsernameRequiredOnNode()).toBe(currentTest.usernameRequired);
        });

        it(`should return true if username is specified, on ${currentTest.provider}`, function () {
          let compositeSourceControlCopy = angular.copy(compositeSourceControl);
          compositeSourceControlCopy.username.value = 'TOKEN';
          compositeSourceControlCopy.provider.parentValue = currentTest.provider;

          digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
          expect(vm.isUsernameRequiredOnNode()).toBe(currentTest.usernameRequired);
        });
      }
    });

    describe('showAdvanced', function () {
      it('should return true if token cannot be inherited', function () {
        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);
        expect(vm.showAdvanced).toBeTruthy();
      });

      it('should return false if token is inherited', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.parentName = 'Root Organization';
        compositeSourceControlCopy.token.parentValue = 'token';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.showAdvanced).toBeFalsy();
      });

      it('should return false if token is specified and not inheritable', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = 'TOKEN';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.showAdvanced).toBeFalsy();
      });

      it('should return false if username and token are specified and provider is bitbucket', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token = {
          value: null,
          parentValue: 'TOKEN',
          parentName: 'Root Organization',
        };
        compositeSourceControlCopy.username.value = 'username';
        compositeSourceControlCopy.provider = {
          value: null,
          parentValue: 'bitbucket',
          parentName: 'Root Organization',
        };

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.showAdvanced).toBeFalsy();
      });

      it('should return true if username is specified, token is null and provider is bitbucket', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.username.value = 'username';
        compositeSourceControlCopy.token.value = null;
        compositeSourceControlCopy.provider = {
          value: null,
          parentValue: 'bitbucket',
        };

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.showAdvanced).toBeTruthy();
      });

      it('should return false if username, token are inherited and provider is bitbucket', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.username.parentName = 'Root Organization';
        compositeSourceControlCopy.username.parentValue = 'parentuser';
        compositeSourceControlCopy.provider = {
          value: null,
          parentValue: 'bitbucket',
          parentName: 'Root Organization',
        };
        compositeSourceControlCopy.token.parentName = 'Root Organization';
        compositeSourceControlCopy.token.parentValue = 'TOKEN';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.showAdvanced).toBeFalsy();
      });

      it('should return true if username (but not token) is inherited and provider is bitbucket', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.username.parentName = 'Root Organization';
        compositeSourceControlCopy.username.parentValue = 'parentuser';
        compositeSourceControlCopy.provider = {
          value: null,
          parentValue: 'bitbucket',
        };

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.showAdvanced).toBeTruthy();
      });
    });

    describe('shouldShowAccessTokenWarning', function () {
      it('should return true if token cannot be inherited', function () {
        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);
        expect(vm.shouldShowAccessTokenWarning).toBeTruthy();
      });

      it('should return false if token is inherited', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.parentName = 'Root Organization';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.shouldShowAccessTokenWarning).toBeFalsy();
      });

      it('should return false if token is specified and not inheritable', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = 'TOKEN';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.shouldShowAccessTokenWarning).toBeFalsy();
      });
    });

    describe('canCollapseAdvanced', function () {
      it('should return true if token and branch is inherited', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.parentName = 'Root Organization';
        compositeSourceControlCopy.token.parentValue = 'token';
        compositeSourceControlCopy.baseBranch.parentName = 'Root Organization';
        compositeSourceControlCopy.baseBranch.parentValue = 'master';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.canCollapseAdvanced()).toBeTruthy();
      });

      it('should return true if token is specified and not inheritable and base branch is inherited', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = 'TOKEN';
        compositeSourceControlCopy.baseBranch.parentName = 'Root Organization';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.canCollapseAdvanced()).toBeTruthy();
      });

      it('should return true if token is specified and not inheritable and base branch is specified', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = 'TOKEN';
        compositeSourceControlCopy.baseBranch.value = 'branch';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.canCollapseAdvanced()).toBeTruthy();
      });

      it('should return true if token is inherited and base branch is specified', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.parentName = 'Root Organization';
        compositeSourceControlCopy.token.parentValue = 'token';
        compositeSourceControlCopy.baseBranch.value = 'master';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.canCollapseAdvanced()).toBeTruthy();
      });

      it('should return false if token is overridden and not specified and branch is specified', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.value = 'branch';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.canCollapseAdvanced()).toBeFalsy();
      });

      it('should return false if token is overridden and not specified and branch is inherited', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.parent = 'Root Organization';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.canCollapseAdvanced()).toBeFalsy();
      });

      it('should return false if branch is overridden and not specified and token is specified', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.value = 'TOKEN';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        vm.dirtySourceControl.baseBranchInherit = false;
        vm.dirtySourceControl.baseBranch = null;

        expect(vm.canCollapseAdvanced()).toBeFalsy();
      });

      it('should return false if branch is overridden and not specified and token is inherited', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.token.parentName = 'Root Organization';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        vm.dirtySourceControl.baseBranchInherit = false;
        vm.dirtySourceControl.baseBranch = null;

        expect(vm.canCollapseAdvanced()).toBeFalsy();
      });
    });

    describe('statusChecksInheritText', function () {
      it('should return "Inherit (Not Configured)" if not defined elsewhere', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.statusChecksEnabled.parentName = null;
        compositeSourceControlCopy.statusChecksEnabled.parentValue = null;

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.statusChecksInheritText).toEqual('Inherit (Not Configured)');
      });

      it('should return "Inherit from Org (Enabled)" if enabled on org', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.statusChecksEnabled.parentName = 'Org';
        compositeSourceControlCopy.statusChecksEnabled.parentValue = true;

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.statusChecksInheritText).toEqual('Inherit from Org (Enabled)');
      });

      it('should return "Inherit from Org (Disabled)" if disabled on org', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.statusChecksEnabled.parentName = 'Org';
        compositeSourceControlCopy.statusChecksEnabled.parentValue = false;

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.statusChecksInheritText).toEqual('Inherit from Org (Disabled)');
      });
    });

    describe('remediationPullRequestsInheritText', function () {
      it('should return "Inherit (Not Configured)" if not defined elsewhere', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.remediationPullRequestsEnabled.parentName = null;
        compositeSourceControlCopy.remediationPullRequestsEnabled.parentValue = null;

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.remediationPullRequestsEnabledInheritText).toEqual('Inherit (Not Configured)');
      });

      it('should return "Inherit from Org (Enabled)" if enabled on org', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.remediationPullRequestsEnabled.parentName = 'Org';
        compositeSourceControlCopy.remediationPullRequestsEnabled.parentValue = true;

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.remediationPullRequestsEnabledInheritText).toEqual('Inherit from Org (Enabled)');
      });

      it('should return "Inherit from Org (Disabled)" if disabled on org', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.remediationPullRequestsEnabled.parentName = 'Org';
        compositeSourceControlCopy.remediationPullRequestsEnabled.parentValue = false;

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.remediationPullRequestsEnabledInheritText).toEqual('Inherit from Org (Disabled)');
      });
    });

    describe('pullRequestCommentingInheritText', function () {
      it('should return "Inherit (Not Configured)" if not defined elsewhere', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.pullRequestCommentingEnabled.parentName = null;
        compositeSourceControlCopy.pullRequestCommentingEnabled.parentValue = null;

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.pullRequestCommentingEnabledInheritText).toEqual('Inherit (Not Configured)');
      });

      it('should return "Inherit from Org (Enabled)" if enabled on org', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.pullRequestCommentingEnabled.parentName = 'Org';
        compositeSourceControlCopy.pullRequestCommentingEnabled.parentValue = true;

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.pullRequestCommentingEnabledInheritText).toEqual('Inherit from Org (Enabled)');
      });

      it('should return "Inherit from Org (Disabled)" if disabled on org', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.pullRequestCommentingEnabled.parentName = 'Org';
        compositeSourceControlCopy.pullRequestCommentingEnabled.parentValue = false;

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.pullRequestCommentingEnabledInheritText).toEqual('Inherit from Org (Disabled)');
      });
    });

    describe('baseBranchInheritText', function () {
      it('should return "Inherit (Not Configured)" if not defined elsewhere', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.parentName = null;
        compositeSourceControlCopy.baseBranch.parentValue = null;

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.baseBranchInheritText).toEqual('Inherit (Not Configured)');
      });

      it('should return "Inherit from Org (value)" if set on org', function () {
        let compositeSourceControlCopy = angular.copy(compositeSourceControl);
        compositeSourceControlCopy.baseBranch.parentName = 'Org';
        compositeSourceControlCopy.baseBranch.parentValue = 'value';

        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControlCopy);
        expect(vm.baseBranchInheritText).toEqual('Inherit from Org (value)');
      });
    });

    describe('getFeatureNotAvailableMessage', function () {
      it('should not return message for provider if license supports automation', function () {
        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);
        expect(vm.getPullRequestsNotAvailableMessage()).toEqual('');
        expect(vm.getPullRequestCommentingNotAvailableMessage()).toEqual('');
        expect(vm.getSourceControlEvaluationsNotAvailableMessage()).toEqual('');

        vm.dirtySourceControl.provider = 'gitlab';
        expect(vm.getPullRequestsNotAvailableMessage()).toEqual('');
        expect(vm.getPullRequestCommentingNotAvailableMessage()).toEqual('');
        expect(vm.getSourceControlEvaluationsNotAvailableMessage()).toEqual('');

        vm.dirtySourceControl.provider = 'bitbucket';
        expect(vm.getPullRequestsNotAvailableMessage()).toEqual('');
        expect(vm.getPullRequestCommentingNotAvailableMessage()).toEqual('');
        expect(vm.getSourceControlEvaluationsNotAvailableMessage()).toEqual('');
      });

      it('should return message if license does not support automation', function () {
        vm.isAutomationSupported = false;
        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl, NOTIFICATIONS);
        expect(vm.getPullRequestsNotAvailableMessage()).toEqual('This feature is not supported by your license');
        expect(vm.getSourceControlEvaluationsNotAvailableMessage()).toEqual(
          'This feature is not supported by your license'
        );
        expect(vm.getPullRequestCommentingNotAvailableMessage()).toEqual(
          'This feature is not supported by your license'
        );

        vm.dirtySourceControl.provider = 'gitlab';
        expect(vm.getPullRequestsNotAvailableMessage()).toEqual('This feature is not supported by your license');
        expect(vm.getSourceControlEvaluationsNotAvailableMessage()).toEqual(
          'This feature is not supported by your license'
        );
        expect(vm.getPullRequestCommentingNotAvailableMessage()).toEqual(
          'This feature is not supported by your license'
        );
      });

      it('should return message if license does not support notifications', function () {
        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl, AUTOMATION);
        expect(vm.getPullRequestsNotAvailableMessage()).toEqual('');
        expect(vm.getSourceControlEvaluationsNotAvailableMessage()).toEqual('');
        expect(vm.getPullRequestCommentingNotAvailableMessage()).toEqual('');

        vm.dirtySourceControl.provider = 'gitlab';
        expect(vm.getPullRequestsNotAvailableMessage()).toEqual('');
        expect(vm.getSourceControlEvaluationsNotAvailableMessage()).toEqual('');
        expect(vm.getPullRequestCommentingNotAvailableMessage()).toEqual('');
      });
    });

    describe('isSshUrl', function () {
      it('returns true when a SSH URL is provided', function () {
        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);

        vm.dirtySourceControl.repositoryUrl = 'ssh://git@github.com/owner/repo.git';
        expect(vm.isSshUrl()).toBeTruthy();

        vm.dirtySourceControl.repositoryUrl = 'ssh://github.com/owner/repo.git';
        expect(vm.isSshUrl()).toBeTruthy();

        vm.dirtySourceControl.repositoryUrl = 'git@github.com:owner/repo.git';
        expect(vm.isSshUrl()).toBeTruthy();
      });

      it('returns false when a non-SSH URL is provided', function () {
        digest(APPLICATION_NAME, APPLICATION_ID, compositeSourceControl);

        vm.dirtySourceControl.repositoryUrl = 'http://github.com/owner/repo.git';
        expect(vm.isSshUrl()).toBeFalsy();

        vm.dirtySourceControl.repositoryUrl = 'ss://github.com/owner/repo.git';
        expect(vm.isSshUrl()).toBeFalsy();

        vm.dirtySourceControl.repositoryUrl = 'ssh:/github.com/owner/repo.git';
        expect(vm.isSshUrl()).toBeFalsy();

        vm.dirtySourceControl.repositoryUrl = 'ssh://not valid';
        expect(vm.isSshUrl()).toBeFalsy();

        vm.dirtySourceControl.repositoryUrl = 'github.com/owner/repo.git';
        expect(vm.isSshUrl()).toBeFalsy();

        vm.dirtySourceControl.repositoryUrl = '@github.com/owner/repo.git';
        expect(vm.isSshUrl()).toBeFalsy();

        vm.dirtySourceControl.repositoryUrl = 'git@github.com/owner/repo.git';
        expect(vm.isSshUrl()).toBeFalsy();
      });
    });

    describe('repoCloneUrl', function () {
      it('matches when a valid HTTP(S) URL', function () {
        let pattern = vm.repoCloneUrl;
        expect('https://github.com/owner/repo.git'.match(pattern)).toBeTruthy();
        expect('http://git@github.com/owner/repo.git'.match(pattern)).toBeTruthy();
        expect('https://git@github.com/owner/repo.git'.match(pattern)).toBeTruthy();
      });

      it('does not match when an invalid HTTP(S) or SSH URL is provided', function () {
        let pattern = vm.repoCloneUrl;
        expect('ssh://git@github.com/owner/repo.git'.match(pattern)).toBeFalsy();
        expect('git@github.com:owner/repo.git'.match(pattern)).toBeFalsy();
        expect('http:git@github.com/owner/repo.git'.match(pattern)).toBeFalsy();
        expect('git@gitlab.com/owner/repo.git'.match(pattern)).toBeFalsy();
        expect('/srv/git/project.git'.match(pattern)).toBeFalsy();
        expect('file:///srv/git/project.git'.match(pattern)).toBeFalsy();
        expect('git://srv/git/project.git'.match(pattern)).toBeFalsy();
        expect('http:/github.com/owner/repo.git'.match(pattern)).toBeFalsy();
        expect('http:github.com/owner/repo.git'.match(pattern)).toBeFalsy();
        expect('github.com/owner/repo.git'.match(pattern)).toBeFalsy();
        expect('not valid'.match(pattern)).toBeFalsy();
      });
    });
  });
});
