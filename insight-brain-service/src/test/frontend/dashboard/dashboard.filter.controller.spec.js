describe('dashboard.filter.controller',function() {
  "use strict";

  var $scope, vm;

  beforeEach(module('dashboard.module'));

  describe('successful load', function() {
    var applicationData = [{
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
        }],
        organizationData = [{
          id: 'orgId1',
          name: 'OrganizationOne'
        }, {
          id: 'orgId2',
          name: 'OrganizationTwo'
        }],
        tagData = [{
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
        }],
        filterData = {
                policyThreatCategoryFilters: ['QUALITY', 'OTHER', 'SECURITY'],
                stageTypeFilters: ['release', 'stage-release', 'build'],
                tagFilters: ['tagId1', 'tagId2'],
                applicationFilters: ['applicationIdZ', 'applicationIdA', 'applicationIdQ'],
                minPolicyThreatLevel: 3,
                maxPolicyThreatLevel: 6
        };

    beforeEach(inject(function($rootScope, $httpBackend, $controller, CLMLocations) {
      $scope = $rootScope.$new();
      $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(applicationData);
      $httpBackend.expectGET(CLMLocations.getDashboardStageUrl()).respond(MockData.getDashboardStageData());
      $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(organizationData);
      $httpBackend.expectGET(CLMLocations.getApplicationTagsUrl()).respond(tagData);
      $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond(filterData);

      vm = $controller('dashboard.filter.controller', {
        $scope: $scope
      });
      $httpBackend.flush();
    }));

    afterEach(inject(function($httpBackend) {
      if ($scope) {
        $scope.$destroy();
      }
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    }));

    it('load populates state',inject(function() {
      expect(vm.selected.policyTypes).toEqual({'QUALITY': true, 'OTHER': true, 'SECURITY': true});
      expect(vm.selected.stages).toEqual({'release': true, 'stage-release': true, 'build': true});
      expect(vm.selected.categories).toEqual({'tagId1': true, 'tagId2': true});
      expect(vm.selected.applications).toEqual({'applicationIdZ': true, 'applicationIdA': true, 'applicationIdQ': true});
      expect(vm.selected.policyThreatLevels).toEqual([3, 6]);

      expect(vm.applications.length).toBe(applicationData.length);
      expect(vm.applications[0].id).toBe(applicationData[0].id);
      expect(vm.applications[1].id).toBe(applicationData[1].id);
      expect(vm.stages.length).toBe(MockData.getDashboardStageData().length);
      expect(vm.stages[0].stageTypeId).toBe(MockData.getDashboardStageData()[0].stageTypeId);
      expect(vm.stages[1].stageTypeId).toBe(MockData.getDashboardStageData()[1].stageTypeId);
      expect(vm.categories.length).toBe(tagData.length);
      expect(vm.categories[0].id).toBe(tagData[0].id);
      expect(vm.categories[0].owner).toBe(organizationData[0].name);
    }));

    it('clears to empty state', function() {
      vm.clear();

      expect(vm.selected.policyTypes).toEqual({});
      expect(vm.selected.stages).toEqual({});
      expect(vm.selected.categories).toEqual({});
      expect(vm.selected.applications).toEqual({});
      expect(vm.selected.policyThreatLevels).toEqual([2, 10]);
    });

    it('handles error on save', inject(function($httpBackend, CLMLocations) {
      expect(vm.saveError).not.toBeDefined();
      vm.selected.applications.fakeId = true;
      $httpBackend.expectPUT(CLMLocations.getDashboardFilters()).respond(500);
      vm.save();
      $httpBackend.flush();
      expect(vm.saveError).toBeDefined();
    }));

    it('revert', function() {
      var expected = angular.copy(vm.selected);

      delete vm.selected.applications.applicationIdZ;
      delete vm.selected.stages.release;
      delete vm.selected.categories.tagId1;
      delete vm.selected.policyTypes.QUALITY;
      vm.selected.policyThreatLevels[0] = 0;

      vm.revert();

      expect(vm.selected).toEqual(expected);
    });

    it('save + revert', inject([
      '$rootScope', '$httpBackend', 'CLMLocations', 'event.name.constant',
      function($rootScope, $httpBackend, CLMLocations, EventNameConstant) {
        spyOn($rootScope, '$broadcast');

        var expected = angular.copy(filterData);
        expected.policyThreatCategoryFilters.splice(0, 1); // remove QUALITY
        expected.stageTypeFilters.splice(0, 1); // remove release
        expected.tagFilters.splice(0, 1); // remove tagId1
        expected.applicationFilters.splice(0, 1); // remove applicationIdZ
        expected.minPolicyThreatLevel = 0;
        $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), expected).respond(expected);

        delete vm.selected.applications.applicationIdZ;
        delete vm.selected.stages.release;
        delete vm.selected.categories.tagId1;
        delete vm.selected.policyTypes.QUALITY;
        vm.selected.policyThreatLevels[0] = 0;

        vm.save();
        $httpBackend.flush();
        expect($rootScope.$broadcast).toHaveBeenCalledWith(EventNameConstant.UPDATE_DASHBOARD_FILTERS, expected);

        expected = angular.copy(vm.selected);
        delete vm.selected.policyTypes.OTHER;

        vm.revert();

        expect(vm.selected).toEqual(expected);
      }
    ]));
  });

  describe('load errors', function() {

    function validateErrorRequest($httpBackend, $controller, CLMLocations, actionResponse, appResponse, orgResponse,
            appTagResponse, filterResponse) {
      $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(appResponse);
      $httpBackend.expectGET(CLMLocations.getDashboardStageUrl()).respond(actionResponse);
      $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(orgResponse);
      $httpBackend.expectGET(CLMLocations.getApplicationTagsUrl()).respond(appTagResponse);
      $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond(filterResponse);

      vm = $controller('dashboard.filter.controller', {
        $scope: $scope
      });

      expect(vm.loadError).toBeFalsy();
      $httpBackend.flush();
      expect(vm.loadError).toBeTruthy();
    }

    beforeEach(inject(function($rootScope, $httpBackend, $controller, CLMLocations) {
      $scope = $rootScope.$new();
    }));

    afterEach(inject(function($httpBackend) {
      if ($scope) {
        $scope.$destroy();
      }
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    }));

    it('validate action stage error is handled properly', inject(function($httpBackend, $controller, CLMLocations) {
      validateErrorRequest($httpBackend, $controller, CLMLocations, 500, {}, {}, {}, {});
    }));

    it('validate application error is handled properly', inject(function($httpBackend, $controller, CLMLocations) {
      validateErrorRequest($httpBackend, $controller, CLMLocations, {}, 500, {}, {}, {});
    }));

    it('validate organization error is handled properly', inject(function($httpBackend, $controller, CLMLocations) {
      validateErrorRequest($httpBackend, $controller, CLMLocations, {}, {}, 500, {}, {});
    }));

    it('validate application tag error is handled properly', inject(function($httpBackend, $controller, CLMLocations) {
      validateErrorRequest($httpBackend, $controller, CLMLocations, {}, {}, {}, 500, {});
    }));

    it('validate filter error is handled properly', inject(function($httpBackend, $controller, CLMLocations) {
      validateErrorRequest($httpBackend, $controller, CLMLocations, {}, {}, {}, {}, 500);
    }));
  });
});
