/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { any, propEq } from 'ramda';
import axios from 'axios';

import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';
import legacyConfigurationModule from '../../../../main/frontend/LegacyConfigurationModule';
import OwnerUtils from '../owner.utils';
import PolicyResourceMockData from '../mock.data/policy.resource.mock.data';
import TagResourceMockData from '../mock.data/tag.resource.mock.data';

describe('policy.editor.controller', function () {
  var $state;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, legacyConfigurationModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });

      $state = {
        current: {
          name: '',
        },
        params: {},
        reload: angular.noop,
      };
      $provide.value('$state', $state);
      $provide.value('$stateParams', $state.params);
      SpecUtil.mockNgRedux($provide);
    })
  );

  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  function createTests(type, storeName, owner) {
    var vm,
      $q,
      scope,
      $timeout,
      CLMContextLocations,
      deleteServiceResourceDefer,
      ownerProperties,
      isApp = type === 'application',
      mockDeleteService,
      mockCategoryOwners,
      mockPolicyTags,
      $httpBackend,
      SameOwnerStateNavigationService = {
        goEdit: angular.noop,
      },
      mockPolicyStore = StoreUtils().createMockStore('PolicyHierarchyStore'),
      mockPolicyStoreData = StoreUtils().createMockHierarchyStoreData(
        PolicyResourceMockData.getApplicablePolicies(type, owner.id, owner.name),
        'policiesByOwner'
      ),
      mockPolicy = ResourceUtils().createMockResource();

    beforeEach(inject(function ($rootScope, _$q_, _$timeout_, _$httpBackend_, _CLMContextLocations_, CLMLocations) {
      scope = $rootScope.$new();
      $q = _$q_;
      $timeout = _$timeout_;
      $httpBackend = _$httpBackend_;
      CLMContextLocations = _CLMContextLocations_;

      deleteServiceResourceDefer = $q.defer();

      mockDeleteService = {
        deleteResource: function () {
          return deleteServiceResourceDefer.promise;
        },
      };

      mockCategoryOwners = TagResourceMockData.getApplicationCategoriesUrl(type, owner.id);
      mockPolicyTags = TagResourceMockData.getPolicyTagUrl();
      spyOn(CLMContextLocations, 'isApplication').and.returnValue(isApp);
      spyOn(CLMContextLocations, 'getEntityId').and.returnValue(isApp ? owner.publicId : owner.id);
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);

      $state.current.name = type;
      if (type === 'application') {
        $state.params.applicationPublicId = owner.publicId;
      } else if (type === 'organization') {
        $state.params.organizationId = owner.id;
      }
      ownerProperties = {
        ownerType: type,
        ownerId: owner.id,
      };
    }));

    afterEach(function () {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();

      if (scope && scope.$destroy) {
        scope.$destroy();
      }
    });

    it('Creates new on load', inject(function ($controller) {
      vm = $controller('policy.editor.controller', { $scope: scope });

      resolveLoadData(mockPolicyStoreData, undefined);

      expect(mockPolicyStoreData[0].store.create).toHaveBeenCalled();
    }));

    it('Captures siblings', inject(function ($controller) {
      vm = $controller('policy.editor.controller', { $scope: scope });

      resolveLoadData(mockPolicyStoreData, undefined);

      expect(vm.siblings.length).toBe(1);
      expect(vm.siblings).toContain(mockPolicyStoreData[0].policies[0]);
    }));

    it('Updates siblings list after creating new', inject(function ($controller) {
      vm = $controller('policy.editor.controller', { $scope: scope });

      resolveLoadData(mockPolicyStoreData, undefined);

      mockPolicy.id = 123;
      mockPolicy.$new = true;
      mockPolicy.isDirty = function () {
        return true;
      };

      vm.dirtyPolicy = mockPolicy;
      vm.policyEditor = {
        $valid: true,
        $setPristine: angular.noop,
      };
      vm.policyEditorMask = { wrap: SpecUtil.promiseWrapper($q) };

      vm.save();
      resolveSaveData('123');

      expect(mockPolicyStoreData[0].store.create).toHaveBeenCalled();
    }));

    it('$state.reload called after creating new', inject(function ($controller) {
      vm = $controller('policy.editor.controller', { $scope: scope });

      resolveLoadData(mockPolicyStoreData, undefined);

      mockPolicy.id = 123;
      mockPolicy.$new = true;
      mockPolicy.isDirty = function () {
        return true;
      };

      vm.dirtyPolicy = mockPolicy;
      vm.policyEditor = {
        $valid: true,
        $setPristine: angular.noop,
      };
      vm.policyEditorMask = { wrap: SpecUtil.promiseWrapper($q) };

      spyOn($state, 'reload');
      vm.save();
      resolveSaveData('123');

      expect($state.reload).toHaveBeenCalled();
    }));

    it('Finds match with URL parameter', inject(function ($controller) {
      $state.params.policyId = '456';

      vm = $controller('policy.editor.controller', { $scope: scope });
      vm.ownerProperties = ownerProperties;
      mockPolicy.id = '456';
      mockPolicy.ownerId = owner.id;
      mockPolicy.actions = [];

      resolveLoadData(
        [
          {
            policies: [mockPolicy, { id: '123', actions: [] }],
            policyTags: [],
            store: { create: angular.noop },
            ownerType: type,
            ownerId: owner.id,
          },
        ],
        '456'
      );

      expect(vm.dirtyPolicy.$clone).toHaveBeenCalled();
      expect(vm.dirtyPolicy.id).toBe('456');
    }));

    it('Errors if no match found', inject(function ($controller) {
      $state.params.policyId = '456';

      vm = $controller('policy.editor.controller', { $scope: scope });

      resolveLoadData(
        [
          {
            policies: [{ id: '123' }, { id: '123' }],
            policyTags: [],
            store: { create: angular.noop },
            ownerType: type,
            ownerId: owner.id,
          },
        ],
        '456',
        true
      );

      expect(vm.dirtyPolicy).toBeUndefined();
      expect(vm.loadError).toBe('some error');
    }));

    it('Unsuccessful save sets error message', inject(function ($controller) {
      vm = $controller('policy.editor.controller', { $scope: scope });

      resolveLoadData(mockPolicyStoreData);

      mockPolicy.isDirty = function () {
        return true;
      };

      vm.dirtyPolicy = mockPolicy;
      vm.policyEditor = {
        $valid: true,
      };
      vm.policyEditorMask = { wrap: SpecUtil.promiseWrapper($q) };

      vm.save();
      mockPolicy.rejectSave('dagnabbit');

      $timeout.flush();
      expect(vm.submitError).toBe('dagnabbit');
    }));

    it('After delete goes to create new policy', inject(function ($controller) {
      $state.params.policyId = '1';

      spyOn(SameOwnerStateNavigationService, 'goEdit');
      vm = $controller('policy.editor.controller', {
        $scope: scope,
        SameOwnerStateNavigationService: SameOwnerStateNavigationService,
        DeleteModalService: mockDeleteService,
      });

      vm.ownerProperties = ownerProperties;

      mockPolicy.id = '1';
      mockPolicy.actions = [];
      resolveLoadData(
        [
          {
            policies: [mockPolicy, { id: '123' }],
            policyTags: [],
            store: { create: angular.noop },
            ownerType: type,
            ownerId: owner.id,
          },
        ],
        '1'
      );

      vm.deletePolicy();
      deleteServiceResourceDefer.resolve();
      $timeout.flush();

      expect(SameOwnerStateNavigationService.goEdit).toHaveBeenCalledWith('create-policy');
      expect(mockPolicy.$revert).toHaveBeenCalled();
    }));

    it('Properly loads categories', inject(function ($controller) {
      $state.params.policyId = '456';
      vm = $controller('policy.editor.controller', { $scope: scope });
      vm.ownerProperties = ownerProperties;
      mockPolicy.id = '456';
      mockPolicy.actions = [];

      resolveLoadData(
        [
          {
            policies: [mockPolicy, { id: '123' }],
            policyTags: [],
            store: { create: angular.noop },
            ownerType: type,
            ownerId: owner.id,
            ownerName: owner.name,
          },
        ],
        '456'
      );

      if (!isApp) {
        expect(vm.owner.name).toEqual(owner.name);
        expect(vm.categories.length).toBe(3);
        var mockOrgCategories = mockCategoryOwners.applicationCategoriesByOwner[0].applicationCategories;
        var mockRootCategories = mockCategoryOwners.applicationCategoriesByOwner[1].applicationCategories;

        mockOrgCategories.forEach(function (category, index) {
          expect(vm.categories[index].name).toEqual(category.name);
          expect(vm.categories[index].id).toEqual(category.id);
          expect(vm.categories[index].color).toEqual(category.color);
        });
        expect(vm.categories[2].name).toEqual(mockRootCategories[0].name);
        expect(vm.categories[2].id).toEqual(mockRootCategories[0].id);
        expect(vm.categories[2].color).toEqual(mockRootCategories[0].color);

        expect(vm.hasPolicyCategories).toBeTruthy();
      } else {
        expect(vm.categories.length).toBe(0);
        expect(vm.hasPolicyCategories).toBeFalsy();
      }
    }));

    it('Sets readOnly', inject(function ($controller) {
      $state.params.policyId = '456';

      vm = $controller('policy.editor.controller', { $scope: scope });
      vm.ownerProperties = ownerProperties;

      mockPolicy.id = '456';
      mockPolicy.actions = [];

      expect(vm.readOnly).toBeUndefined();

      resolveLoadData(
        [
          {
            policies: [mockPolicy, { id: '123' }],
            policyTags: [],
            store: { create: angular.noop },
            ownerType: type,
            ownerId: owner.id,
          },
        ],
        '456'
      );

      expect(vm.readOnly).toBeDefined();
    }));

    describe('Page Changes', function () {
      beforeEach(inject(function ($controller) {
        vm = $controller('policy.editor.controller', {
          $scope: scope,
        });

        resolveLoadData(mockPolicyStoreData, undefined);
      }));

      it('clean', function () {
        spyOn(vm, 'isPolicyDirty').and.returnValue(false);

        SpecUtil.expectStateChangeNotPrevented(scope);
        expect(vm.isPolicyDirty).toHaveBeenCalled();
      });

      it('dirty', function () {
        spyOn(vm, 'isPolicyDirty').and.returnValue(true);

        SpecUtil.expectStateChangePrevented(scope);
        expect(vm.isPolicyDirty).toHaveBeenCalled();
      });
    });

    if (!isApp) {
      it('Proper ownerName and ownerType get set loading hierarchy', inject(function ($controller) {
        $state.params.policyId = '456';

        vm = $controller('policy.editor.controller', { $scope: scope });
        vm.ownerProperties = ownerProperties;
        mockPolicy.id = '456';

        resolveLoadData(
          [
            {
              ownerId: '1',
              ownerName: 'appName',
              ownerType: 'application',
              policies: [{ id: '123' }],
              policyTags: [],
              store: { create: angular.noop },
            },
            {
              ownerId: owner.id,
              ownerName: 'orgName',
              ownerType: 'organization',
              policies: [mockPolicy],
              policyTags: [],
              store: { create: angular.noop },
            },
            {
              ownerId: 'ROOT_ORGANIZATION_ID',
              ownerName: 'rootOrgName',
              ownerType: 'organization',
              policies: [{ id: '789' }],
              policyTags: [],
              store: { create: angular.noop },
            },
          ],
          '456'
        );

        expect(vm.owner.name).toBe('orgName');
        expect(vm.isOrgOwner).toBe(true);
      }));
    }

    function resolveLoadData(policyStoreData, policyId, expectError) {
      mockPolicyStore.resolveGet(policyStoreData);

      if (policyId) {
        var found = false;
        policyStoreData.some(function (owner) {
          owner.policies.some(function (policy) {
            if (policy.id === policyId) {
              mockPolicyStore.resolveGetById(policy);
              return (found = true);
            }
          });
        });
        if (!found) {
          mockPolicyStore.rejectGetById('some error');
        }
      }

      const respondWithCategories =
        !expectError &&
        (!isApp || (policyStoreData.length > 1 && any(propEq('id', policyId), policyStoreData[1].policies)));
      if (respondWithCategories) {
        const url = `${CLMContextLocations.getCategoriesUrl()}${isApp ? '' : '/applicable'}`;
        mockAxiosCalls({
          get: {
            [url]: { data: mockCategoryOwners },
          },
        });

        if (policyId) {
          $httpBackend.expectGET(CLMContextLocations.getPolicyTagUrl(mockPolicy.id)).respond(mockPolicyTags);
        }
      }
      $timeout.flush();
      $httpBackend.flush();
    }

    function resolveSaveData(policyId) {
      mockPolicy.resolveSave();
      if (!isApp) {
        $httpBackend.expectPUT(CLMContextLocations.getPolicyTagUrl(policyId)).respond(mockPolicyTags);
      }
      $timeout.flush();
      if (!isApp) {
        $httpBackend.flush();
      }
    }
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
