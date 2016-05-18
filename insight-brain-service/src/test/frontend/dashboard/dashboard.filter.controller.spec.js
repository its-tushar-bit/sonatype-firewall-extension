/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
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
      expect(vm.filtersLoaded).toBeFalsy();
      $httpBackend.flush();
      expect(vm.filtersLoaded).toBe(true);
    }));

    afterEach(inject(function($httpBackend) {
      if ($scope) {
        $scope.$destroy();
      }
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    }));

    it('data loaded and placed in vm',inject(function() {
      expect(vm.filters.policyThreatTypes).toEqual(['QUALITY', 'OTHER', 'SECURITY']);
      expect(vm.filters.stageTypeIds).toEqual(['release', 'stage-release', 'build']);
      expect(vm.filters.applicationTagIds).toEqual(['tagId1', 'tagId2']);
      expect(vm.filters.applicationIds).toEqual(['applicationIdZ', 'applicationIdA', 'applicationIdQ']);
      expect(vm.filters.policyThreatLevel).toEqual([3, 6]);
      expect(vm.applications.length).toBe(applicationData.length);
      expect(vm.applications[0].id).toBe(applicationData[0].id);
      expect(vm.applications[1].id).toBe(applicationData[1].id);
      expect(vm.stageTypes.length).toBe(MockData.getDashboardStageData().length);
      expect(vm.stageTypes[0].stageTypeId).toBe(MockData.getDashboardStageData()[0].stageTypeId);
      expect(vm.stageTypes[1].stageTypeId).toBe(MockData.getDashboardStageData()[1].stageTypeId);
      expect(vm.applicationTags.length).toBe(tagData.length);
      expect(vm.applicationTags[0].id).toBe(tagData[0].id);
      expect(vm.applicationTags[0].owner).toBe(organizationData[0].name);
    }));

    it('validate filter and dirty filter usage in scope', inject(function($httpBackend, CLMLocations) {
      // make sure updating the text fields doesn't update the filter (at least prior to hitting save)
      $scope.$apply(function() {
        vm.dirtyFilters.applicationIds = ['applicationIdA'];
      });
      expect(vm.filters.applicationIds).toEqual(['applicationIdZ', 'applicationIdA', 'applicationIdQ']);

      // make sure cancel cleans out the dirty filter
      $scope.$apply(function() {
        vm.cancel();
      });
      expect(vm.dirtyFilters.applicationIds).toEqual(['applicationIdZ', 'applicationIdA', 'applicationIdQ']);

      // make sure save puts data into the filter (from dirty filter)
      $scope.$apply(function() {
        vm.dirtyFilters.applicationIds = ['applicationIdA'];
      });
      var data = angular.copy(filterData);
      data.applicationFilters = ['applicationIdA'];
      $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), data).respond(200);
      vm.save();
      $httpBackend.flush();
      expect(vm.dirtyFilters.applicationIds).toEqual(['applicationIdA']);
      expect(vm.filters.applicationIds).toEqual(['applicationIdA']);

      // make sure reset isn't automatically put in effect
      $scope.$apply(function() {
        vm.reset();
        expect(vm.dirtyFilters.applicationIds).toEqual([]);
        expect(vm.filters.applicationIds).toEqual(['applicationIdA']);
      });
    }));

    it('validate alert created on save error', inject(function($httpBackend, CLMLocations) {
      expect(vm.alerts).not.toBeDefined();
      $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), filterData).respond(500);
      vm.save();
      $httpBackend.flush();
      expect(vm.alerts).toBeDefined();
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

      expect(vm.fatalError).toBeFalsy();
      $httpBackend.flush();
      expect(vm.fatalError).toBeTruthy();
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
