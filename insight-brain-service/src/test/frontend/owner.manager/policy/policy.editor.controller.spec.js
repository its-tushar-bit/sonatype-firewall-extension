describe('policy.editor.controller.spec.js', function() {

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  beforeEach(module('ResourceModule'));

  function createTests(type, owner) {

    var vm,
        $q,
        scope,
        $timeout,
        CLMAppLocations,
        deleteServiceResourceDefer,
        isApp = type === 'application',
        mockDeleteService,
        mockCategoryOwners,
        mockPolicyTags,
        $httpBackend,
        $controller,
        SameOwnerStateNavigationService = {
          goEdit: angular.noop
        },
        mockPolicyStore = StoreUtils().createMockStore('PolicyHierarchyStore'),
        mockPolicyStoreData = StoreUtils().createMockHierarchyStoreData(PolicyResourceMockData.getApplicablePolicies(),
            'policiesByOwner'),
        mockPolicy = ResourceUtils().createMockResource();

    beforeEach(inject(function($rootScope, _$q_, _$timeout_, _$controller_, _$httpBackend_, _CLMAppLocations_)
    {
      scope = $rootScope.$new();
      $q = _$q_;
      $timeout = _$timeout_;
      $httpBackend = _$httpBackend_;
      $controller = _$controller_;
      CLMAppLocations = _CLMAppLocations_;

      deleteServiceResourceDefer = $q.defer();

      mockDeleteService = {
        deleteResource: function() {
          return deleteServiceResourceDefer.promise;
        }
      };

      mockCategoryOwners = TagResourceMockData.getTagsUrl();
      mockPolicyTags = TagResourceMockData.getPolicyTagUrl();
      spyOn(CLMAppLocations, 'isApplication').andReturn(isApp);
      spyOn(CLMAppLocations, 'getEntityId').andReturn(isApp ? owner.publicId : owner.id);
    }));

    afterEach(function() {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    it('Creates new on load', inject(function($controller) {
      vm = $controller('policy.editor.controller', {$scope: scope});

      resolveLoadData(mockPolicyStoreData, undefined);

      expect(mockPolicyStoreData[0].store.create).toHaveBeenCalled();
    }));

    it('Captures siblings', inject(function($controller) {
      vm = $controller('policy.editor.controller', {$scope: scope});

      resolveLoadData(mockPolicyStoreData, undefined);

      expect(vm.siblings.length).toBe(1);
      expect(vm.siblings).toContain(mockPolicyStoreData[0].policies[0]);
    }));

    it('Updates siblings list after creating new', inject(function($controller) {
      vm = $controller('policy.editor.controller', {$scope: scope});

      resolveLoadData(mockPolicyStoreData, undefined);

      mockPolicy.id = 123;
      mockPolicy.$new = true;
      mockPolicy.isDirty = function() {
        return true;
      };

      vm.dirtyPolicy = mockPolicy;
      vm.policyEditor = {
        $valid: true,
        $setPristine: angular.noop
      };
      vm.policyEditorMask = {wrap: SpecUtil.promiseWrapper($q)};

      vm.save();
      resolveSaveData('123');

      expect(vm.siblings.length).toBe(2);
      expect(mockPolicyStoreData[0].store.create).toHaveBeenCalled();
      expect(vm.siblings).toContain(mockPolicy);
    }));

    it('Finds match with URL parameter', inject(function($controller) {
      vm = $controller('policy.editor.controller',
          {$scope: scope, $stateParams: {policyId: '456'}});
      mockPolicy.id = '456';
      mockPolicy.ownerId = 'orgownerid';

      resolveLoadData([{policies: [mockPolicy, {id: '123'}], policyTags: [], store: {create: angular.noop}}],
          '123');

      expect(vm.dirtyPolicy.$clone).toHaveBeenCalled();
      expect(vm.dirtyPolicy.id).toBe('456');
    }));

    it('Errors if no match found', inject(function($controller) {
      vm = $controller('policy.editor.controller',
          {$scope: scope, $stateParams: {policyId: '456'}});

      resolveLoadData([{policies: [{id: '123'}, {id: '123'}], policyTags: [], store: {create: angular.noop}}],
          '123');

      expect(vm.dirtyPolicy).toBeUndefined();
      expect(vm.loadError).toBe('Unable to locate Policy.');
    }));

    it('Unsuccessful save sets error message', inject(function($controller) {
      vm = $controller('policy.editor.controller', {$scope: scope});

      resolveLoadData(mockPolicyStoreData);

      mockPolicy.isDirty = function() {
        return true;
      };

      vm.dirtyPolicy = mockPolicy;
      vm.policyEditor = {
        $valid: true
      };
      vm.policyEditorMask = {wrap: SpecUtil.promiseWrapper($q)};

      vm.save();
      mockPolicy.rejectSave('dagnabbit');

      $timeout.flush();
      expect(vm.submitError).toBe('dagnabbit');
    }));

    it('After delete goes to create new policy', inject(function($controller) {
      spyOn(SameOwnerStateNavigationService, 'goEdit');
      vm = $controller('policy.editor.controller', {
        $scope: scope,
        SameOwnerStateNavigationService: SameOwnerStateNavigationService,
        $stateParams: {policyId: '1'},
        DeleteModalService: mockDeleteService
      });

      mockPolicy.id = '1';
      resolveLoadData([{policies: [mockPolicy, {id: '123'}], policyTags: [], store: {create: angular.noop}}],
          '123');

      vm.deletePolicy();
      deleteServiceResourceDefer.resolve();
      $timeout.flush();

      expect(SameOwnerStateNavigationService.goEdit).toHaveBeenCalledWith('create-policy');
      expect(mockPolicy.$revert).toHaveBeenCalled();
    }));

    it('Properly loads categories', inject(function($controller) {
      vm = $controller('policy.editor.controller',
          {$scope: scope, $stateParams: {organizationId: 'orgownerid', policyId: '456'}});
      mockPolicy.id = '456';

      resolveLoadData([{policies: [mockPolicy, {id: '123'}], policyTags: [], store: {create: angular.noop}}],
          '123');

      if (!isApp) {
        expect(vm.ownerName).toEqual(owner.name);
        expect(vm.categories.length).toBe(2);
        expect(vm.categories.length).toEqual(mockCategoryOwners.tagsByOwner[0].tags.length);
        var mockOrgCategories = mockCategoryOwners.tagsByOwner[0].tags;
        vm.categories.forEach(function(category, index) {
          expect(category.name).toEqual(mockOrgCategories[index].name);
          expect(category.id).toEqual(mockOrgCategories[index].id);
          expect(category.color).toEqual(mockOrgCategories[index].color);
        });
        expect(vm.categories.length).toBe(mockPolicyTags.length);
        expect(vm.hasPolicyCategories).toBeTruthy();
      }
      else {
        expect(vm.categories.length).toBe(0);
        expect(vm.hasPolicyCategories).toBeFalsy();
      }
    }));

    it('Sets readOnly', inject(function($controller) {
      vm = $controller('policy.editor.controller',
          {$scope: scope, $stateParams: {policyId: '456'}});
      mockPolicy.id = '456';

      expect(vm.readOnly).toBeUndefined();

      resolveLoadData([{policies: [mockPolicy, {id: '123'}], policyTags: [], store: {create: angular.noop}}],
          '123');

      expect(vm.readOnly).toBeDefined();
    }));

    describe('Page Changes', function() {
      beforeEach(inject(function($controller) {
        vm = $controller('policy.editor.controller', {
          $scope: scope
        });

        resolveLoadData(mockPolicyStoreData, undefined);
      }));

      it('clean', function() {
        spyOn(vm, 'isPolicyDirty').andReturn(false);

        SpecUtil.expectStateChangeNotPrevented(scope);
        expect(vm.isPolicyDirty).toHaveBeenCalled();
      });

      it('dirty', function() {
        spyOn(vm, 'isPolicyDirty').andReturn(true);

        SpecUtil.expectStateChangePrevented(scope);
        expect(vm.isPolicyDirty).toHaveBeenCalled();
      });
    });

    function resolveLoadData(policyStoreData, policyId) {
      mockPolicyStore.resolveGet(policyStoreData);

      if (!isApp) {
        $httpBackend.expectGET(CLMAppLocations.getTagsUrl()).respond(mockCategoryOwners);
        if (policyId) {
          $httpBackend.expectGET(CLMAppLocations.getPolicyTagUrl(mockPolicy.id)).respond(mockPolicyTags);
        }
        $httpBackend.flush();
      }

      $timeout.flush();
    }

    function resolveSaveData(policyId) {
      mockPolicy.resolveSave();
      if (!isApp) {
        $httpBackend.expectPUT(CLMAppLocations.getPolicyTagUrl(policyId)).respond(mockPolicyTags);
      }
      $timeout.flush();
      if (!isApp) {
        $httpBackend.flush();
      }
    }
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
