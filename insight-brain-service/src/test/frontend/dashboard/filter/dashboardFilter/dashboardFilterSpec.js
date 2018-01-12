describe('dashboard.filter.controller', function() {

  var $rootScope, $scope, $componentController, vm, $httpBackend, CLMLocations, mockState;

  beforeEach(module('dashboardFilter'));

  beforeEach(module(function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function(_$rootScope_, _$httpBackend_, _$componentController_, _CLMLocations_) {
    $rootScope = _$rootScope_;
    $scope = $rootScope.$new();
    $componentController = _$componentController_;
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    mockState = {
      params: {},
      $current: { name: ''}
    };

    vm = $componentController('dashboardFilter', {
      $scope: $scope,
      $state: mockState
    });
    $scope.vm = vm; // needed to be able to test scope.$watch
    vm.$onInit();
  }));

  afterEach(function() {
    if ($scope) {
      $scope.$destroy();
    }
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  var applicationData = [
        {
          id: 'applicationIdZ',
          publicId: 'applicationPublicIdZ',
          name: 'ApplicationZ <b style="woah" class=\'evenmorewoah\'>&nbsp;shouldnotbebold</b>',
          organizationId: 'orgId1'
        }, {
          id: 'applicationIdA',
          publicId: 'applicationPublicIdA',
          name: 'ApplicationA',
          organizationId: 'orgId2'
        }, {
          id: 'applicationIdQ',
          publicId: 'applicationPublicIdQ',
          name: 'ApplicationQ',
          organizationId: 'orgId2'
        }, {
          id: 'applicationIdR',
          publicId: 'applicationPublicIdR',
          name: 'ApplicationR',
          organizationId: 'orgId2'
        }, {
          id: 'applicationIdS',
          publicId: 'applicationPublicIdS',
          name: 'ApplicationS',
          organizationId: 'noPermissionOrgId',
          organizationName: 'No Permission'
        }
      ],
      organizationData = [
        {
          id: 'orgId1',
          name: 'OrganizationOne'
        }, {
          id: 'orgId2',
          name: 'OrganizationTwo'
        }
      ],
      tagData = [
        {
          id: 'tagId1',
          organizationId: 'orgId1',
          name: 'TagOne',
          nameLowercaseNoWhitespace: 'tagone',
          description: 'Tag One Description'
        }, {
          id: 'tagId2',
          organizationId: 'orgId2',
          name: 'TagTwo',
          nameLowercaseNoWhitespace: 'tagtwo',
          description: 'Tag Two Description'
        }
      ],
      filterData = {
        organizationFilters: ['orgId1', 'orgId2'],
        policyThreatCategoryFilters: ['QUALITY', 'OTHER', 'SECURITY'],
        stageTypeFilters: ['release', 'stage-release', 'build'],
        tagFilters: ['tagId1', 'tagId2', null],
        applicationFilters: ['applicationIdZ', 'applicationIdA', 'applicationIdQ'],
        policyViolationStates: ['OPEN', 'WAIVED'],
        maxDaysOld: 90,
        minPolicyThreatLevel: 3,
        maxPolicyThreatLevel: 6
      },
      savedFilterData = [
        {
          'name': 'Test1',
          'filter': filterData
        }
      ],
      appliedDirtyFilterData = {
        'name': '',
        'basedOnFilterName': 'Test1',
        'filter': filterData,
        'needsAcknowledgement': false
      };

  describe('successful load', function() {

    beforeEach(function() {
      $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(applicationData);
      $httpBackend.expectGET(CLMLocations.getDashboardStageUrl()).respond(MockData.getDashboardStageData());
      $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(organizationData);
      $httpBackend.expectGET(CLMLocations.getApplicationTagsUrl()).respond(tagData);
      $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond(appliedDirtyFilterData);
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond(savedFilterData);
      $httpBackend.flush();
    });

    it('load populates state', function() {
      expect(vm.selected.policyTypes).toEqual({'QUALITY': true, 'OTHER': true, 'SECURITY': true});
      expect(vm.selected.stages).toEqual({'release': true, 'stage-release': true, 'build': true});
      expect(vm.selected.categories).toEqual({'tagId1': true, 'tagId2': true, 'null': true});
      expect(vm.selected.organizations).toEqual({'orgId1': true, 'orgId2': true});
      expect(vm.selected.applications).toEqual(
          {'applicationIdZ': true, 'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true});
      expect(vm.selected.policyViolationStates).toEqual({'OPEN': true, 'WAIVED': true});
      expect(vm.selected.age).toEqual({maxDaysOld: 90, name: 'past 90 days'});
      expect(vm.selected.policyThreatLevels).toEqual([3, 6]);

      expect(vm.applications.length).toBe(applicationData.length);
      expect(vm.applications[0].id).toBe(applicationData[0].id);
      expect(vm.applications[1].id).toBe(applicationData[1].id);

      // since we have an application but no permissions to the org add 1
      expect(vm.organizations.length).toBe(organizationData.length + 1);
      expect(vm.organizations[0].id).toBe(organizationData[0].id);
      expect(vm.organizations[1].id).toBe(organizationData[1].id);
      // no permission to org scenario
      expect(vm.organizations[2].id).toBe(applicationData[4].organizationId);
      expect(vm.organizations[2].name).toBe(applicationData[4].organizationName);

      expect(vm.stages.length).toBe(MockData.getDashboardStageData().length);
      expect(vm.stages[0].stageTypeId).toBe(MockData.getDashboardStageData()[0].stageTypeId);
      expect(vm.stages[1].stageTypeId).toBe(MockData.getDashboardStageData()[1].stageTypeId);
      expect(vm.categories.length).toBe(tagData.length + 1); // one extra for No Category
      expect(vm.categories[0].id).toBe(tagData[0].id);
      expect(vm.categories[0].owner).toBe(organizationData[0].name);
      expect(vm.categories[2].id).toBe(null);
      expect(vm.categories[2].name).toBe('No Category');

      expect(vm.activeFilterName).toBe(appliedDirtyFilterData.basedOnFilterName);
      expect(vm.showDirtyAsterisk).toBe(false);
    });
  });

  it('skips selected category ids that do not exist in vm.categories', function() {
    $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(applicationData);
    $httpBackend.expectGET(CLMLocations.getDashboardStageUrl()).respond(MockData.getDashboardStageData());
    $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(organizationData);

    // note that we are not including the second tag in the tag data
    $httpBackend.expectGET(CLMLocations.getApplicationTagsUrl()).respond(tagData.slice(0, 1));
    $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond(appliedDirtyFilterData);
    $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond(savedFilterData);
    $httpBackend.flush();

    expect(vm.selected.categories).toEqual({tagId1: true, 'null': true});
  });

  describe('load errors', function() {

    function validateErrorRequest(actionResponse, appResponse, orgResponse, appTagResponse, filterResponse) {
      $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(appResponse);
      $httpBackend.expectGET(CLMLocations.getDashboardStageUrl()).respond(actionResponse);
      $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(orgResponse);
      $httpBackend.expectGET(CLMLocations.getApplicationTagsUrl()).respond(appTagResponse);
      $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond(filterResponse);
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond([]);

      expect(vm.loadError).toBeFalsy();
      $httpBackend.flush();
      expect(vm.loadError).toBeTruthy();
    }

    it('validate action stage error is handled properly', function() {
      validateErrorRequest(500, {}, {}, {}, {});
    });

    it('validate application error is handled properly', function() {
      validateErrorRequest({}, 500, {}, {}, {});
    });

    it('validate organization error is handled properly', function() {
      validateErrorRequest({}, {}, 500, {}, {});
    });

    it('validate application tag error is handled properly', function() {
      validateErrorRequest({}, {}, {}, 500, {});
    });

    it('validate filter error is handled properly', function() {
      validateErrorRequest({}, {}, {}, {}, 500);
    });
  });

  describe('controller actions', function() {

    beforeEach(function() {
      $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(applicationData);
      $httpBackend.expectGET(CLMLocations.getDashboardStageUrl()).respond(MockData.getDashboardStageData());
      $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(organizationData);
      $httpBackend.expectGET(CLMLocations.getApplicationTagsUrl()).respond(tagData);
      $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond(appliedDirtyFilterData);
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond([]);
      $httpBackend.flush();
    });

    describe('applyCurrentFilter()', function() {
      it('handles error on save', function() {
        expect(vm.saveError).not.toBeDefined();
        vm.selected.applications.fakeId = true;
        $httpBackend.expectPUT(CLMLocations.getDashboardFilters()).respond(500);
        vm.applyCurrentFilter();
        $httpBackend.flush();
        expect(vm.saveError).toBeDefined();
      });

      it('clears loadErrorFilterName', function() {
        vm.loadErrorFilterName = 'test filter';
        vm.applyCurrentFilter();
        expect(vm.loadErrorFilterName).toBeUndefined();
      });

      it('passes name of active filter as \'basedOnFilterName\'', function() {
        delete vm.selected.applications.applicationIdR;
        vm.applyCurrentFilter();

        $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), {filter: filterData, basedOnFilterName: 'Test1'}).respond('ok');
        $httpBackend.flush();
      });

      it('fires UPDATE_FILTERS_REQUESTED action if filter is dirty', function() {
        expect(vm.updateFilters.calls.count()).toBe(1);
        delete vm.selected.applications.applicationIdR;
        vm.applyCurrentFilter();
        expect(vm.updateFilters.calls.count()).toBe(2);
      });

      it('fires UPDATE_FILTERS_FULFILLED action on response', function() {
        vm.updateFiltersFulfilled = jasmine.createSpy('updateFiltersFulfilled');
        $httpBackend.expectPUT(CLMLocations.getDashboardFilters()).respond('ok');

        expect(vm.updateFiltersFulfilled).not.toHaveBeenCalled();
        delete vm.selected.applications.applicationIdR;
        vm.applyCurrentFilter();
        expect(vm.updateFiltersFulfilled).not.toHaveBeenCalled();
        $httpBackend.flush();
        expect(vm.updateFiltersFulfilled).toHaveBeenCalled();
      });
    });

    describe('clear()', function() {
      it('clears to default state', function() {
        vm.clear();

        expect(vm.selected.policyTypes).toEqual({});
        expect(vm.selected.stages).toEqual({});
        expect(vm.selected.categories).toEqual({});
        expect(vm.selected.organizations).toEqual({});
        expect(vm.selected.applications).toEqual({});
        expect(vm.selected.policyViolationStates).toEqual({'OPEN': true});
        expect(vm.selected.age).toEqual({maxDaysOld: 30, name: 'past 30 days'});
        expect(vm.selected.policyThreatLevels).toEqual([2, 10]);
      });

      it('clears loadErrorFilterName', function() {
        vm.loadErrorFilterName = 'test filter';
        vm.clear();
        expect(vm.loadErrorFilterName).toBeUndefined();
      });

      it('clears saved filter', function() {
        var savedFilter = {
          filter: {
            minPolicyThreatLevel: 9,
            maxPolicyThreatLevel: 10
          },
          name: 'test saved filter'
        };

        $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), savedFilter).respond(savedFilter.filter);
        vm.onFilterSelected(savedFilter);
        $httpBackend.flush();
        vm.clear();
        expect(vm.activeFilterName).toBeUndefined();
        expect(vm.showDirtyAsterisk).toBe(false);
      });
    });

    describe('revert()', function() {
      it('reverts to loaded state', function() {
        var expected = angular.copy(vm.selected);
        delete vm.selected.organizations.orgId1;
        delete vm.selected.applications.applicationIdA;
        delete vm.selected.stages.release;
        delete vm.selected.categories.tagId1;
        delete vm.selected.policyTypes.QUALITY;
        delete vm.selected.policyViolationStates.OPEN;
        delete vm.selected.age;
        vm.selected.policyThreatLevels[0] = 0;
        vm.revert();
        expect(vm.selected).toEqual(expected);
      });

      it('reverts after applyCurrentFilter()', function() {
        spyOn($rootScope, '$broadcast');
        vm.updateFiltersFulfilled = jasmine.createSpy('updateFiltersFulfilled');

        var expected = {
          basedOnFilterName: null,
          filter: angular.copy(filterData)
        };
        expected.filter.policyThreatCategoryFilters.splice(0, 1); // remove QUALITY
        expected.filter.stageTypeFilters.splice(0, 1); // remove release
        expected.filter.tagFilters.splice(0, 1); // remove tagId1
        expected.filter.applicationFilters.splice(0, 1); // remove applicationIdZ
        expected.filter.applicationFilters.push(applicationData[3].id); // pickup orgId2 application which wasn't in the original filter
        expected.filter.policyViolationStates.splice(0, 1); // remove OPEN
        expected.filter.maxDaysOld = 7;
        expected.filter.minPolicyThreatLevel = 0;
        expected.basedOnFilterName = 'Test1';
        $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), expected).respond(expected);

        delete vm.selected.applications.applicationIdZ;
        delete vm.selected.stages.release;
        delete vm.selected.categories.tagId1;
        delete vm.selected.policyTypes.QUALITY;
        delete vm.selected.policyViolationStates.OPEN;
        vm.selected.age = {maxDaysOld: 7, name: 'past 7 days'};
        vm.selected.policyThreatLevels[0] = 0;

        expect(vm.showDirtyAsterisk).toBe(false);
        vm.applyCurrentFilter();
        $httpBackend.flush();
        expect(vm.showDirtyAsterisk).toBe(true);
        expect(vm.updateFiltersFulfilled).toHaveBeenCalledWith(expected, false, 'Test1');

        expected = angular.copy(vm.selected);
        delete vm.selected.policyTypes.OTHER;

        vm.revert();

        expect(vm.showDirtyAsterisk).toBe(true);
        expect(vm.selected).toEqual(expected);
      });

      it('clears loadErrorFilterName', function() {
        vm.loadErrorFilterName = 'test filter';
        vm.revert();
        expect(vm.loadErrorFilterName).toBeUndefined();
      });

      it('after first saving filter then clear()', function() {
        var savedFilter = {
          filter: {
            minPolicyThreatLevel: 9,
            maxPolicyThreatLevel: 10
          },
          name: 'test filter'
        };
        spyOn(vm, 'isDirty').and.returnValue(true);
        vm.showDirtyAsterisk = true;
        $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), savedFilter).respond(savedFilter.filter);
        vm.onFilterSelected(savedFilter);
        $httpBackend.flush();
        expect(vm.activeFilterName).toEqual('test filter');
        vm.clear();
        vm.revert();
        expect(vm.showDirtyAsterisk).toBe(false);
        expect(vm.activeFilterName).toEqual('test filter');
      });
    });

    describe('onFilterSelected()', function() {
      var savedFilter = {
        filter: {
          minPolicyThreatLevel: 9,
          maxPolicyThreatLevel: 10
        },
        name: 'test filter'
      };

      it('saves filter as applied, updates results and populates filter UI', function() {
        vm.updateFiltersFulfilled = jasmine.createSpy('updateFiltersFulfilled');
        spyOn(vm, 'loadFilterFromJson');
        spyOn($rootScope, '$broadcast');
        var savedFilterWithBasedOnFlag = angular.copy(savedFilter);
        savedFilterWithBasedOnFlag.basedOnFilterName = savedFilter.name;
        $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), savedFilterWithBasedOnFlag).respond(
            savedFilter.filter);
        vm.onFilterSelected(savedFilter);
        $httpBackend.flush();
        expect(vm.activeFilterName).toEqual('test filter');
        expect(vm.loadFilterFromJson).toHaveBeenCalledWith(savedFilter.filter);
        expect(vm.isDirty()).toBe(false);
        expect(vm.showDirtyAsterisk).toBe(false);
        expect(vm.updateFiltersFulfilled).toHaveBeenCalledWith(savedFilter.filter, false, 'test filter');
      });

      it('handles error', function() {
        $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), savedFilter).respond(500);
        vm.onFilterSelected(savedFilter);
        $httpBackend.flush();
        expect(vm.loadErrorFilterName).toBe('test filter');
      });

      it('clears loadErrorFilterName', function() {
        vm.loadErrorFilterName = 'test filter';

        $httpBackend.whenPUT(CLMLocations.getDashboardFilters(), savedFilter).respond([]);
        vm.onFilterSelected(savedFilter);
        $httpBackend.flush();
        expect(vm.loadErrorFilterName).toBeUndefined();
      });

      it('fires UPDATE_FILTERS_REQUESTED action', function() {
        expect(vm.updateFilters.calls.count()).toBe(1);
        vm.onFilterSelected(savedFilter);
        expect(vm.updateFilters.calls.count()).toBe(2);
      });

      it('fires UPDATE_FILTERS_FULFILLED action on response', function() {
        vm.updateFiltersFulfilled = jasmine.createSpy('updateFiltersFulfilled');
        $httpBackend.expectPUT(CLMLocations.getDashboardFilters()).respond('ok');

        expect(vm.updateFiltersFulfilled).not.toHaveBeenCalled();
        vm.onFilterSelected(savedFilter);
        expect(vm.updateFiltersFulfilled).not.toHaveBeenCalled();
        $httpBackend.flush();
        expect(vm.updateFiltersFulfilled).toHaveBeenCalled();
      });
    });

    it('onActiveFilterDeleted()', function() {
      vm.activeFilterName = 'Test1';
      vm.onActiveFilterDeleted();
      expect(vm.activeFilterName).toBe(undefined);
    });

    it('onFilterSaved()', function() {
      vm.activeFilterName = 'Test1';
      vm.onFilterSaved('Test2');
      expect(vm.activeFilterName).toBe('Test2');
      expect(vm.showDirtyAsterisk).toBe(false);
    });
  });

  describe('loadFilterFromJson()', function() {
    var emptyFilterData = {
      filter: {
        minPolicyThreatLevel: 2,
        maxPolicyThreatLevel: 10
      }
    };

    beforeEach(function() {
      $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(applicationData);
      $httpBackend.expectGET(CLMLocations.getDashboardStageUrl()).respond(MockData.getDashboardStageData());
      $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(organizationData);
      $httpBackend.expectGET(CLMLocations.getApplicationTagsUrl()).respond(tagData);
      $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond(emptyFilterData);
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond([]);
      $httpBackend.flush();
    });

    it('properly populates vm.selected', function() {
      expect(vm.selected.organizations).toEqual({});
      expect(vm.selected.policyTypes).toEqual({});
      expect(vm.selected.stages).toEqual({});
      expect(vm.selected.categories).toEqual({});
      expect(vm.selected.applications).toEqual({});
      expect(vm.selected.policyViolationStates).toEqual({'OPEN': true});
      expect(vm.selected.age).toEqual({maxDaysOld: 30, name: 'past 30 days'});
      expect(vm.selected.policyThreatLevels).toEqual([2, 10]);

      vm.loadFilterFromJson(filterData);

      expect(vm.selected.organizations).toEqual({'orgId1': true, 'orgId2': true});
      expect(vm.selected.policyTypes).toEqual({'QUALITY': true, 'OTHER': true, 'SECURITY': true});
      expect(vm.selected.stages).toEqual({'release': true, 'stage-release': true, 'build': true});
      expect(vm.selected.categories).toEqual({'tagId1': true, 'tagId2': true, 'null': true});
      expect(vm.selected.policyViolationStates).toEqual({ 'OPEN': true, 'WAIVED': true });
      expect(vm.selected.applications).toEqual(
          {'applicationIdZ': true, 'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true});
      expect(vm.selected.age).toEqual({maxDaysOld: 90, name: 'past 90 days'});
      expect(vm.selected.policyThreatLevels).toEqual([3, 6]);
    });
  });

  describe('age filter', function() {

    var dashboardFilterWithoutParam, dashboardFilterWithParam, dashboardFilterWithParamWrongState;

    beforeEach(function() {
      $httpBackend.whenGET(CLMLocations.getApplicationsUrl()).respond([]);
      $httpBackend.whenGET(CLMLocations.getDashboardStageUrl()).respond([]);
      $httpBackend.whenGET(CLMLocations.getOrganizationsUrl()).respond([]);
      $httpBackend.whenGET(CLMLocations.getApplicationTagsUrl()).respond([]);
      $httpBackend.whenGET(CLMLocations.getDashboardSavedFilters()).respond([]);

      var mockViolationsStateWithoutParam = {
            params: {timeFilterFeature: undefined},
            $current: {name: 'dashboard.overview.violations'}
          },
          mockViolationsStateWithParam = {
            params: {timeFilterFeature: 'true'},
            $current: {name: 'dashboard.overview.violations'}
          },
          mockComponentsStateWithParam = {
            params: {timeFilterFeature: 'true'},
            $current: {name: 'dashboard.overview.components'}
          };
      dashboardFilterWithoutParam = $componentController('dashboardFilter', {
        $state: mockViolationsStateWithoutParam,
        $scope: $rootScope.$new()
      });
      dashboardFilterWithoutParam.$onInit();

      dashboardFilterWithParam = $componentController('dashboardFilter', {
        $state: mockViolationsStateWithParam,
        $scope: $rootScope.$new()
      });
      dashboardFilterWithParam.$onInit();

      dashboardFilterWithParamWrongState = $componentController('dashboardFilter', {
        $state: mockComponentsStateWithParam,
        $scope: $rootScope.$new()
      });
      dashboardFilterWithParamWrongState.$onInit();
    });

    it('only shows the age filter when the flag is turned on', function() {
      $httpBackend.whenGET(CLMLocations.getDashboardFilters()).respond({filter: {maxDaysOld: 30}});
      $httpBackend.flush(15); // (3 controllers here + 1 at the top scope) * 3 endpoint calls + 3 store calls

      expect(dashboardFilterWithoutParam.showAgeFilter).toBeFalsy();
      expect(dashboardFilterWithoutParam.isAgeFilterReadOnly).toBeTruthy();
      expect(dashboardFilterWithParam.showAgeFilter).toBeTruthy();
      expect(dashboardFilterWithParam.isAgeFilterReadOnly).toBeFalsy();
      expect(dashboardFilterWithParamWrongState.showAgeFilter).toBeFalsy();
      expect(dashboardFilterWithParamWrongState.isAgeFilterReadOnly).toBeFalsy();
    });

    it('also shows read-only age filter when loading a filter with non-default age', function() {
      $httpBackend.whenGET(CLMLocations.getDashboardFilters()).respond({filter: {maxDaysOld: 90}});
      $httpBackend.flush(15); // (3 controllers here + 1 at the top scope) * 3 endpoint calls + 3 store calls

      expect(dashboardFilterWithoutParam.showAgeFilter).toBeTruthy();
      expect(dashboardFilterWithoutParam.isAgeFilterReadOnly).toBeTruthy();
      expect(dashboardFilterWithParam.showAgeFilter).toBeTruthy();
      expect(dashboardFilterWithParam.isAgeFilterReadOnly).toBeFalsy();
      expect(dashboardFilterWithParamWrongState.showAgeFilter).toBeFalsy();
      expect(dashboardFilterWithParamWrongState.isAgeFilterReadOnly).toBeFalsy();
    });

  });

  describe('isDirty()', function() {
    it('defaults to false and changes properly', function() {
      $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(applicationData);
      $httpBackend.expectGET(CLMLocations.getDashboardStageUrl()).respond(MockData.getDashboardStageData());
      $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(organizationData);
      $httpBackend.expectGET(CLMLocations.getApplicationTagsUrl()).respond(tagData);
      $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond(appliedDirtyFilterData);
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond(savedFilterData);
      $httpBackend.flush();

      expect(vm.isDirty()).toBe(false);
      expect(vm.needsAcknowledgement).toBe(false);
      delete vm.selected.organizations.orgId1;
      expect(vm.isDirty()).toBe(true);
    });

    it('defaults to false for needsAcknowledgement and vm.needsAcknowledgement changes after apply', function() {
      var needsAcknowledgementFilterData = angular.copy(appliedDirtyFilterData);
      needsAcknowledgementFilterData.needsAcknowledgement = true;
      vm.updateFiltersFulfilled = jasmine.createSpy('updateFiltersFulfilled');

      $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(applicationData);
      $httpBackend.expectGET(CLMLocations.getDashboardStageUrl()).respond(MockData.getDashboardStageData());
      $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(organizationData);
      $httpBackend.expectGET(CLMLocations.getApplicationTagsUrl()).respond(tagData);
      $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond(needsAcknowledgementFilterData);
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond(savedFilterData);
      $httpBackend.flush();

      expect(vm.isDirty()).toBe(false);
      expect(vm.needsAcknowledgement).toBe(true);

      var savedFilter = {
        filter: {
          minPolicyThreatLevel: 9,
          maxPolicyThreatLevel: 10
        }
      };

      spyOn(vm, 'loadFilterFromJson');
      spyOn($rootScope, '$broadcast');

      // apply filter
      $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), savedFilter).respond(savedFilter.filter);
      vm.onFilterSelected(savedFilter);
      $httpBackend.flush();
      expect(vm.updateFiltersFulfilled).toHaveBeenCalledWith(savedFilter.filter, false, undefined);
      expect(vm.isDirty()).toBe(false);
      expect(vm.needsAcknowledgement).toBe(false);
    });

    it('causes an updateFiltersDirtiness action to fire whenever it changes', function() {
      vm.updateFiltersDirtiness = jasmine.createSpy('updateFiltersDirtiness');

      $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(applicationData);
      $httpBackend.expectGET(CLMLocations.getDashboardStageUrl()).respond(MockData.getDashboardStageData());
      $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(organizationData);
      $httpBackend.expectGET(CLMLocations.getApplicationTagsUrl()).respond(tagData);
      $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond(appliedDirtyFilterData);
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond(savedFilterData);
      $httpBackend.flush();

      var orgId1 = vm.selected.organizations.orgId1;
      delete vm.selected.organizations.orgId1;
      $rootScope.$digest();
      expect(vm.updateFiltersDirtiness).toHaveBeenCalledWith(true);

      vm.selected.organizations.orgId1 = orgId1;
      $rootScope.$digest();
      expect(vm.updateFiltersDirtiness).toHaveBeenCalledWith(false);
    });
  });

  describe('$onInit', function() {
    it('fired UPDATE_FILTERS_REQUESTED action', function() {
      var myVm = $componentController('dashboardFilter');
      myVm.$onInit();
      expect(myVm.updateFilters).toHaveBeenCalled();
    });
  });

  describe('$onDestroy()', function() {
    it('unsubscribes from redux store', function() {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });
});
