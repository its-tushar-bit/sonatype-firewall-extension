describe('dashboard.filter.controller', function() {
  "use strict";

  var $rootScope, $scope, vm, $httpBackend, CLMLocations;

  beforeEach(module('dashboard.module'));

  beforeEach(inject(function(_$rootScope_, _$httpBackend_, $controller, _CLMLocations_) {
    $rootScope = _$rootScope_;
    $scope = $rootScope.$new();
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;

    vm = $controller('dashboard.filter.controller', {
      $scope: $scope
    });
    $scope.vm = vm; // needed to be able to test scope.$watch

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
          id: "tagId1",
          organizationId: 'orgId1',
          name: "TagOne",
          nameLowercaseNoWhitespace: "tagone",
          description: "Tag One Description"
        }, {
          id: "tagId2",
          organizationId: 'orgId2',
          name: "TagTwo",
          nameLowercaseNoWhitespace: "tagtwo",
          description: "Tag Two Description"
        }
      ],
      filterData = {
        organizationFilters: ['orgId1', 'orgId2'],
        policyThreatCategoryFilters: ['QUALITY', 'OTHER', 'SECURITY'],
        stageTypeFilters: ['release', 'stage-release', 'build'],
        tagFilters: ['tagId1', 'tagId2'],
        applicationFilters: ['applicationIdZ', 'applicationIdA', 'applicationIdQ'],
        minPolicyThreatLevel: 3,
        maxPolicyThreatLevel: 6
      };

  describe('successful load', function() {

    beforeEach(function() {
      $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(applicationData);
      $httpBackend.expectGET(CLMLocations.getDashboardStageUrl()).respond(MockData.getDashboardStageData());
      $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(organizationData);
      $httpBackend.expectGET(CLMLocations.getApplicationTagsUrl()).respond(tagData);
      $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond(filterData);
      $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond([]);
      $httpBackend.flush();
    });

    it('load populates state', function() {
      expect(vm.selected.policyTypes).toEqual({'QUALITY': true, 'OTHER': true, 'SECURITY': true});
      expect(vm.selected.stages).toEqual({'release': true, 'stage-release': true, 'build': true});
      expect(vm.selected.categories).toEqual({'tagId1': true, 'tagId2': true});
      expect(vm.selected.organizations).toEqual({'orgId1': true, 'orgId2': true});
      expect(vm.selected.applications).toEqual(
          {'applicationIdZ': true, 'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true});
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
      expect(vm.categories.length).toBe(tagData.length);
      expect(vm.categories[0].id).toBe(tagData[0].id);
      expect(vm.categories[0].owner).toBe(organizationData[0].name);
    });

    it('watches organizations to update selected applications', function() {
      expect(vm.selected.policyTypes).toEqual({'QUALITY': true, 'OTHER': true, 'SECURITY': true});
      expect(vm.selected.stages).toEqual({'release': true, 'stage-release': true, 'build': true});
      expect(vm.selected.categories).toEqual({'tagId1': true, 'tagId2': true});
      expect(vm.selected.organizations).toEqual({'orgId1': true, 'orgId2': true});
      expect(vm.selected.applications).toEqual(
          {'applicationIdZ': true, 'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true});
      expect(vm.selected.policyThreatLevels).toEqual([3, 6]);

      //remove org1 apps
      delete vm.selected.organizations['orgId1'];
      // copy the map to trigger watches
      vm.selected.organizations = angular.copy(vm.selected.organizations);
      $scope.$digest();

      expect(vm.selected.applications).toEqual(
          {'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true});
      expect(vm.selected.organizations).toEqual({'orgId2': true});

      // add org 1 apps
      vm.selected.organizations['orgId1'] = true;
      vm.selected.organizations = angular.copy(vm.selected.organizations);
      $scope.$digest();

      expect(vm.selected.applications).toEqual(
          {'applicationIdZ': true, 'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true});
      expect(vm.selected.organizations).toEqual({'orgId2': true, 'orgId1': true});

      // remove all
      delete vm.selected.organizations['orgId1'];
      delete vm.selected.organizations['orgId2'];
      vm.selected.organizations = angular.copy(vm.selected.organizations);
      $scope.$digest();

      expect(vm.selected.organizations).toEqual({});
      expect(vm.selected.applications).toEqual({});

      //add all
      vm.selected.organizations['orgId1'] = true;
      vm.selected.organizations['orgId2'] = true;
      vm.selected.organizations['noPermissionOrgId'] = true;
      vm.selected.organizations = angular.copy(vm.selected.organizations);
      $scope.$digest();

      expect(vm.selected.applications).toEqual({
        'applicationIdZ': true,
        'applicationIdA': true,
        'applicationIdQ': true,
        'applicationIdR': true,
        'applicationIdS': true
      });
      expect(vm.selected.organizations).toEqual({'orgId1': true, 'orgId2': true, 'noPermissionOrgId': true});
    });

    it('watches applications to update selected organizations', function() {
      expect(vm.selected.policyTypes).toEqual({'QUALITY': true, 'OTHER': true, 'SECURITY': true});
      expect(vm.selected.stages).toEqual({'release': true, 'stage-release': true, 'build': true});
      expect(vm.selected.categories).toEqual({'tagId1': true, 'tagId2': true});
      expect(vm.selected.organizations).toEqual({'orgId1': true, 'orgId2': true});
      expect(vm.selected.applications).toEqual(
          {'applicationIdZ': true, 'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true});
      expect(vm.selected.policyThreatLevels).toEqual([3, 6]);

      //remove applicationIdQ
      delete vm.selected.applications['applicationIdQ'];
      vm.selected.applications = angular.copy(vm.selected.applications);
      $scope.$digest();

      expect(vm.selected.applications).toEqual(
          {'applicationIdZ': true, 'applicationIdA': true, 'applicationIdR': true});
      expect(vm.selected.organizations).toEqual({'orgId1': true});

      // add applicationIdQ
      vm.selected.applications['applicationIdQ'] = true;
      vm.selected.applications = angular.copy(vm.selected.applications);
      $scope.$digest();

      expect(vm.selected.applications).toEqual(
          {'applicationIdZ': true, 'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true});
      expect(vm.selected.organizations).toEqual({'orgId2': true, 'orgId1': true});

      // remove all
      delete vm.selected.applications['applicationIdZ'];
      delete vm.selected.applications['applicationIdA'];
      delete vm.selected.applications['applicationIdQ'];
      delete vm.selected.applications['applicationIdR'];
      vm.selected.applications = angular.copy(vm.selected.applications);
      $scope.$digest();

      expect(vm.selected.organizations).toEqual({});
      expect(vm.selected.applications).toEqual({});

      //add all
      vm.selected.applications['applicationIdZ'] = true;
      vm.selected.applications['applicationIdA'] = true;
      vm.selected.applications['applicationIdQ'] = true;
      vm.selected.applications['applicationIdR'] = true;
      vm.selected.applications['applicationIdS'] = true;
      vm.selected.applications = angular.copy(vm.selected.applications);
      $scope.$digest();

      expect(vm.selected.applications).toEqual({
        'applicationIdZ': true,
        'applicationIdA': true,
        'applicationIdQ': true,
        'applicationIdR': true,
        'applicationIdS': true
      });
      expect(vm.selected.organizations).toEqual({'orgId1': true, 'orgId2': true, 'noPermissionOrgId': true});
    });
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
      $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond(filterData);
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
    });

    describe('clear()', function() {
      it('clears to empty state', function() {
        vm.clear();

        expect(vm.selected.policyTypes).toEqual({});
        expect(vm.selected.stages).toEqual({});
        expect(vm.selected.categories).toEqual({});
        expect(vm.selected.organizations).toEqual({});
        expect(vm.selected.applications).toEqual({});
        expect(vm.selected.policyThreatLevels).toEqual([2, 10]);
      });

      it('clears loadErrorFilterName', function() {
        vm.loadErrorFilterName = 'test filter';
        vm.clear();
        expect(vm.loadErrorFilterName).toBeUndefined();
      });

      it('clears saved filter', function() {
        vm.activeFilterName = 'test saved filter';
        vm.clear();
        expect(vm.activeFilterName).toBeUndefined();
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
        vm.selected.policyThreatLevels[0] = 0;
        vm.revert();
        expect(vm.selected).toEqual(expected);
      });

      it('reverts after applyCurrentFilter()', inject([
        'event.name.constant', function(EventNameConstant) {
          spyOn($rootScope, '$broadcast');

          var expected = angular.copy(filterData);
          expected.policyThreatCategoryFilters.splice(0, 1); // remove QUALITY
          expected.stageTypeFilters.splice(0, 1); // remove release
          expected.tagFilters.splice(0, 1); // remove tagId1
          expected.applicationFilters.splice(0, 1); // remove applicationIdZ
          expected.applicationFilters.push(applicationData[3].id); // pickup orgId2 application which wasn't in the original filter
          expected.minPolicyThreatLevel = 0;
          $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), expected).respond(expected);

          delete vm.selected.applications.applicationIdZ;
          delete vm.selected.stages.release;
          delete vm.selected.categories.tagId1;
          delete vm.selected.policyTypes.QUALITY;
          vm.selected.policyThreatLevels[0] = 0;

          vm.applyCurrentFilter();
          $httpBackend.flush();
          expect($rootScope.$broadcast).toHaveBeenCalledWith(EventNameConstant.UPDATE_DASHBOARD_FILTERS, expected);

          expected = angular.copy(vm.selected);
          delete vm.selected.policyTypes.OTHER;

          vm.revert();

          expect(vm.selected).toEqual(expected);
        }
      ]));

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
        $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), savedFilter.filter).respond(savedFilter.filter);
        vm.applySavedFilter(savedFilter);
        $httpBackend.flush();
        expect(vm.activeFilterName).toEqual('test filter');
        vm.clear();
        vm.revert();
        expect(vm.activeFilterName).toEqual('test filter');
      });
    });

    describe('applySavedFilter()', function() {
      var savedFilter = {
        filter: {
          minPolicyThreatLevel: 9,
          maxPolicyThreatLevel: 10
        },
        name: 'test filter'
      };

      it('saves filter as applied, updates results and populates filter UI', inject([
        'event.name.constant', function(EventNameConstant) {
          spyOn(vm, 'loadFilterFromJson');
          spyOn($rootScope, '$broadcast');
          $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), savedFilter.filter).respond(savedFilter.filter);
          vm.applySavedFilter(savedFilter);
          $httpBackend.flush();
          expect(vm.activeFilterName).toEqual('test filter');
          expect(vm.loadFilterFromJson).toHaveBeenCalledWith(savedFilter.filter);
          expect(vm.isDirty()).toBe(false);
          expect($rootScope.$broadcast).toHaveBeenCalledWith(EventNameConstant.UPDATE_DASHBOARD_FILTERS,
              savedFilter.filter);
        }
      ]));

      it('handles error', function() {
        $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), savedFilter.filter).respond(500);
        vm.applySavedFilter(savedFilter);
        $httpBackend.flush();
        expect(vm.loadErrorFilterName).toBe('test filter');
      });

      it('clears loadErrorFilterName', function() {
        vm.loadErrorFilterName = 'test filter';
        $httpBackend.whenPUT(CLMLocations.getDashboardFilters(), savedFilter.filter).respond([]);
        vm.applySavedFilter(savedFilter);
        $httpBackend.flush();
        expect(vm.loadErrorFilterName).toBeUndefined();
      });
    });

    describe('openSaveFilterModal()', function() {
      var saveFilterModal, $q;
      beforeEach(inject(['save.filter.modal', '$q', function(SaveFilterModal, _$q_) {
        saveFilterModal = SaveFilterModal;
        $q = _$q_;
      }]));

      it('passes filter json and refreshes saved filter on success', function() {
        var expectedFilterJson = angular.copy(filterData);
        expectedFilterJson.policyThreatCategoryFilters.splice(0, 1); // remove QUALITY
        expectedFilterJson.stageTypeFilters.splice(0, 1); // remove release
        expectedFilterJson.tagFilters.splice(0, 1); // remove tagId1
        expectedFilterJson.applicationFilters.splice(0, 1); // remove applicationIdZ
        expectedFilterJson.applicationFilters.push(applicationData[3].id); // pickup orgId2 application which wasn't in the original filter
        expectedFilterJson.minPolicyThreatLevel = 0;

        delete vm.selected.applications.applicationIdZ;
        delete vm.selected.stages.release;
        delete vm.selected.categories.tagId1;
        delete vm.selected.policyTypes.QUALITY;
        vm.selected.policyThreatLevels[0] = 0;

        var $event = jasmine.createSpyObj('$event', ['stopPropagation']);
        spyOn(saveFilterModal, 'open').andReturn($q.resolve());
        $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond('saved filters');
        spyOn(vm, 'isDirty').andReturn(false);

        vm.openSaveFilterModal($event);
        expect($event.stopPropagation).not.toHaveBeenCalled();
        expect(saveFilterModal.open).toHaveBeenCalledWith(expectedFilterJson);

        $httpBackend.flush();
        expect(vm.savedNamedFilters).toBe('saved filters');
      });

      it('does nothing if filter changes were not applied', function() {
        var $event = jasmine.createSpyObj('$event', ['stopPropagation']);
        spyOn(saveFilterModal, 'open').andReturn($q.resolve());
        // change filter
        delete vm.selected.applications.applicationIdZ;
        vm.openSaveFilterModal($event);
        expect($event.stopPropagation).toHaveBeenCalled();
        expect(saveFilterModal.open).not.toHaveBeenCalled();
      });

      it('save modal filter name matches the saved filter name', function() {
        var $event = jasmine.createSpyObj('$event', ['stopPropagation']);
        $httpBackend.expectGET(CLMLocations.getDashboardSavedFilters()).respond('saved filters');
        spyOn(vm, 'isDirty').andReturn(false);
        spyOn(saveFilterModal, 'open').andReturn($q.resolve("TestFilterName"));
        vm.openSaveFilterModal($event);
        $httpBackend.flush();
        expect(vm.savedNamedFilters).toBe('saved filters');
        expect(vm.activeFilterName).toEqual('TestFilterName');
      });
    });
  });

  describe('loadFilterFromJson()', function() {
    var emptyFilterData = {
      minPolicyThreatLevel: 2,
      maxPolicyThreatLevel: 10
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
      expect(vm.selected.policyThreatLevels).toEqual([2, 10]);

      vm.loadFilterFromJson(filterData);

      expect(vm.selected.organizations).toEqual({'orgId1': true, 'orgId2': true});
      expect(vm.selected.policyTypes).toEqual({'QUALITY': true, 'OTHER': true, 'SECURITY': true});
      expect(vm.selected.stages).toEqual({'release': true, 'stage-release': true, 'build': true});
      expect(vm.selected.categories).toEqual({'tagId1': true, 'tagId2': true});
      expect(vm.selected.applications).toEqual(
          {'applicationIdZ': true, 'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true});
      expect(vm.selected.policyThreatLevels).toEqual([3, 6]);
    });
  });
});
