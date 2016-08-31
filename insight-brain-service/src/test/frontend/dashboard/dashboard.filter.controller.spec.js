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
                organizationFilters: ['orgId1', 'orgId2'],
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
      $scope.vm = vm; // needed to be able to test scope.$watch

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
      expect(vm.selected.organizations).toEqual({'orgId1': true, 'orgId2': true});
      expect(vm.selected.applications).toEqual({'applicationIdZ': true, 'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true});
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
    }));

    it('clears to empty state', function() {
      vm.clear();

      expect(vm.selected.policyTypes).toEqual({});
      expect(vm.selected.stages).toEqual({});
      expect(vm.selected.categories).toEqual({});
      expect(vm.selected.organizations).toEqual({});
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
      delete vm.selected.organizations.orgId1;
      delete vm.selected.applications.applicationIdA;
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
        expected.applicationFilters.push(applicationData[3].id); // pickup orgId2 application which wasn't in the original filter
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


    it('updates selected applications',inject(function() {
      expect(vm.selected.policyTypes).toEqual({'QUALITY': true, 'OTHER': true, 'SECURITY': true});
      expect(vm.selected.stages).toEqual({'release': true, 'stage-release': true, 'build': true});
      expect(vm.selected.categories).toEqual({'tagId1': true, 'tagId2': true});
      expect(vm.selected.organizations).toEqual({'orgId1': true, 'orgId2': true});
      expect(vm.selected.applications).toEqual({'applicationIdZ': true, 'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true});
      expect(vm.selected.policyThreatLevels).toEqual([3, 6]);

      //remove org1 apps
      delete vm.selected.organizations['orgId1'];
      // copy the map to trigger watches
      vm.selected.organizations = angular.copy(vm.selected.organizations);
      $scope.$digest();

      expect(vm.selected.applications).toEqual({'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true});
      expect(vm.selected.organizations).toEqual({'orgId2': true});

      // add org 1 apps
      vm.selected.organizations['orgId1'] = true;
      vm.selected.organizations = angular.copy(vm.selected.organizations);
      $scope.$digest();

      expect(vm.selected.applications).toEqual({'applicationIdZ': true, 'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true});
      expect(vm.selected.organizations).toEqual({'orgId2': true, 'orgId1': true});


      // remove all
      delete vm.selected.organizations['orgId1'];
      delete vm.selected.organizations['orgId2'];
      vm.selected.organizations = angular.copy(vm.selected.organizations);
      $scope.$digest();

      expect(vm.selected.organizations).toEqual({ });
      expect(vm.selected.applications).toEqual({ });

      //add all
      vm.selected.organizations['orgId1'] = true;
      vm.selected.organizations['orgId2'] = true;
      vm.selected.organizations['noPermissionOrgId'] = true;
      vm.selected.organizations = angular.copy(vm.selected.organizations);
      $scope.$digest();

      expect(vm.selected.applications).toEqual({'applicationIdZ': true, 'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true, 'applicationIdS': true});
      expect(vm.selected.organizations).toEqual({'orgId1': true, 'orgId2': true, 'noPermissionOrgId': true});
    }));
    
    it('updates selected organization',inject(function() {
      expect(vm.selected.policyTypes).toEqual({'QUALITY': true, 'OTHER': true, 'SECURITY': true});
      expect(vm.selected.stages).toEqual({'release': true, 'stage-release': true, 'build': true});
      expect(vm.selected.categories).toEqual({'tagId1': true, 'tagId2': true});
      expect(vm.selected.organizations).toEqual({'orgId1': true, 'orgId2': true});
      expect(vm.selected.applications).toEqual({'applicationIdZ': true, 'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true});
      expect(vm.selected.policyThreatLevels).toEqual([3, 6]);

      //remove applicationIdQ
      delete vm.selected.applications['applicationIdQ'];
      vm.selected.applications = angular.copy(vm.selected.applications);
      $scope.$digest();

      expect(vm.selected.applications).toEqual({'applicationIdZ': true, 'applicationIdA': true, 'applicationIdR': true});
      expect(vm.selected.organizations).toEqual({'orgId1': true});

      // add applicationIdQ
      vm.selected.applications['applicationIdQ'] = true;
      vm.selected.applications = angular.copy(vm.selected.applications);
      $scope.$digest();

      expect(vm.selected.applications).toEqual({'applicationIdZ': true, 'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true});
      expect(vm.selected.organizations).toEqual({'orgId2': true, 'orgId1': true});


      // remove all
      delete vm.selected.applications['applicationIdZ'];
      delete vm.selected.applications['applicationIdA'];
      delete vm.selected.applications['applicationIdQ'];
      delete vm.selected.applications['applicationIdR'];
      vm.selected.applications = angular.copy(vm.selected.applications);
      $scope.$digest();

      expect(vm.selected.organizations).toEqual({ });
      expect(vm.selected.applications).toEqual({ });

      //add all
      vm.selected.applications['applicationIdZ'] = true;
      vm.selected.applications['applicationIdA'] = true;
      vm.selected.applications['applicationIdQ'] = true;
      vm.selected.applications['applicationIdR'] = true;
      vm.selected.applications['applicationIdS'] = true;
      vm.selected.applications = angular.copy(vm.selected.applications);
      $scope.$digest();

      expect(vm.selected.applications).toEqual({'applicationIdZ': true, 'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true, 'applicationIdS': true});
      expect(vm.selected.organizations).toEqual({'orgId1': true, 'orgId2': true, 'noPermissionOrgId': true});
    }));

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
