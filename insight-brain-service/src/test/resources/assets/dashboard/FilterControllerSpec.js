/* global describe, beforeEach, afterEach, module, inject, it, angular, expect */
describe('FilterController', function() {
  "use strict";
  var $scope, stageTypeData = [
    {
      id: 'release',
      name: 'Release'
    },
    {
      id: 'stage-release',
      name: 'Stage-Release'
    },
    {
      id: 'build',
      name: 'Build'
    }
  ], applicationData = [
    {
      id: 'applicationIdZ',
      publicId: 'applicationPublicIdZ',
      name: 'ApplicationZ <b style="woah" class=\'evenmorewoah\'>&nbsp;shouldnotbebold</b>',
      organizationId: 'orgId1'
    },
    {
      id: 'applicationIdA',
      publicId: 'applicationPublicIdA',
      name: 'ApplicationA',
      organizationId: 'orgId2'
    },
    {
      id: 'applicationIdQ',
      publicId: 'applicationPublicIdQ',
      name: 'ApplicationQ',
      organizationId: 'orgId2'
    }
  ], organizationData = [
    {
      id: 'orgId1',
      name: 'OrganizationOne'
    },
    {
      id: 'orgId2',
      name: 'OrganizationTwo'
    }
  ], tagData = [
    {
      id: "tagId1",
      organizationId: 'orgId1',
      name: "TagOne",
      nameLowercaseNoWhitespace: "tagone",
      description: "Tag One Description"
    },
    {
      id: "tagId2",
      organizationId: 'orgId2',
      name: "TagTwo",
      nameLowercaseNoWhitespace: "tagtwo",
      description: "Tag Two Description"
    }
  ], filterData = {
    policyThreatCategoryFilters: ['QUALITY', 'OTHER', 'SECURITY'],
    stageTypeFilters: ['release', 'stage-release', 'build'],
    tagFilters: ['tagId1', 'tagId2'],
    applicationFilters: ['applicationIdZ', 'applicationIdA', 'applicationIdQ'],
    minPolicyThreatLevel: 3,
    maxPolicyThreatLevel: 6
  };

  beforeEach(module('FilterModule'));

  beforeEach(inject(function($rootScope, $httpBackend, $controller, CLMLocations) {
    $scope = $rootScope.$new();
    $httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(stageTypeData);
    $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(applicationData);
    $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(organizationData);
    $httpBackend.expectGET(CLMLocations.getApplicationTagsUrl()).respond(tagData);
    $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond(filterData);
    expect($scope.filtersLoaded).toBeFalsy();
    $controller('FilterController', { $scope: $scope});
    expect($scope.filtersLoaded).toBeFalsy();
    $httpBackend.flush();
    expect($scope.filtersLoaded).toBe(true);
  }));

  afterEach(inject(function($httpBackend) {
    if ($scope) {
      $scope.$destroy();
    }
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  it('data loaded and placed in $scope', inject(function() {
    expect($scope.filters.policyThreatTypes).toEqual(['QUALITY', 'OTHER', 'SECURITY']);
    expect($scope.filters.stageTypeIds).toEqual(['release', 'stage-release', 'build']);
    expect($scope.filters.applicationTagIds).toEqual(['tagId1', 'tagId2']);
    expect($scope.filters.applicationIds).toEqual(['applicationIdZ', 'applicationIdA', 'applicationIdQ']);
    expect($scope.filters.policyThreatLevel).toEqual([3, 6]);
    expect($scope.applications.length).toBe(applicationData.length);
    expect($scope.applications[0].id).toBe(applicationData[0].id);
    expect($scope.applications[1].id).toBe(applicationData[1].id);
    expect($scope.stageTypes.length).toBe(stageTypeData.length);
    expect($scope.stageTypes[0].id).toBe(stageTypeData[0].id);
    expect($scope.stageTypes[1].id).toBe(stageTypeData[1].id);
    expect($scope.applicationTags.length).toBe(tagData.length);
    expect($scope.applicationTags[0].id).toBe(tagData[0].id);
    expect($scope.applicationTags[0].owner).toBe(organizationData[0].name);
    //make sure the html encoding done
    expect($scope.applicationsTooltip).toBe('ApplicationA<br/>ApplicationQ<br/>ApplicationZ &lt;b style="woah" class=\'evenmorewoah\'&gt;&amp;nbsp;shouldnotbebold&lt;/b&gt;');
    expect($scope.applicationTagsTooltip).toBe('TagOne<br/>TagTwo');
    expect($scope.stageTypesTooltip).toBe('Build<br/>Stage-Release<br/>Release');
    expect($scope.policyTypesTooltip).toBe('Security<br/>Quality<br/>Other');
    expect($scope.policyThreatLevelsTooltip).toBe('Policy threat levels 3 through 6');
  }));

  it('validate filter and dirty filter usage in scope', inject(function($httpBackend, CLMLocations) {
    //make sure updating the text fields doesn't update the filter (at least prior to hitting save)
    $scope.$apply(function() {
      $scope.dirtyFilters.applicationIds = ['applicationIdA'];
    });
    expect($scope.filters.applicationIds).toEqual(['applicationIdZ', 'applicationIdA', 'applicationIdQ']);

    //make sure cancel cleans out the dirty filter
    $scope.$apply(function() {
      $scope.cancel();
    });
    expect($scope.dirtyFilters.applicationIds).toEqual(['applicationIdZ', 'applicationIdA', 'applicationIdQ']);

    //make sure save puts data into the filter (from dirty filter)
    $scope.$apply(function() {
      $scope.dirtyFilters.applicationIds = ['applicationIdA'];
    });
    var data = angular.copy(filterData);
    data.applicationFilters = ['applicationIdA'];
    $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), data).respond(200);
    $scope.save();
    $httpBackend.flush();
    expect($scope.dirtyFilters.applicationIds).toEqual(['applicationIdA']);
    expect($scope.filters.applicationIds).toEqual(['applicationIdA']);

    //make sure reset isn't automatically put in effect
    $scope.$apply(function() {
      $scope.reset();
      expect($scope.dirtyFilters.applicationIds).toEqual([]);
      expect($scope.filters.applicationIds).toEqual(['applicationIdA']);
    });
  }));

  it('validate alert created on save error', inject(function($httpBackend, CLMLocations){
    expect($scope.alerts).not.toBeDefined();
    $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), filterData).respond(500);
    $scope.save();
    $httpBackend.flush();
    expect($scope.alerts).toBeDefined();
  }));
});

describe('FilterController load errors', function() {
  "use strict";
  var $scope;

  function validateErrorRequest($httpBackend, $controller, CLMLocations, actionResponse, appResponse, orgResponse, appTagResponse, filterResponse) {
    $httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(actionResponse);
    $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(appResponse);
    $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(orgResponse);
    $httpBackend.expectGET(CLMLocations.getApplicationTagsUrl()).respond(appTagResponse);
    $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond(filterResponse);
    $controller('FilterController', { $scope: $scope});
    expect($scope.fatalError).toBeNull();
    $httpBackend.flush();
    expect($scope.fatalError).not.toBeNull();
  }

  beforeEach(module('FilterModule'));

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

  it('validate action stage error is handled properly', inject(function($httpBackend, $controller, CLMLocations){
    validateErrorRequest($httpBackend, $controller, CLMLocations, 500, {}, {}, {}, {});
  }));

  it('validate application error is handled properly', inject(function($httpBackend, $controller, CLMLocations){
    validateErrorRequest($httpBackend, $controller, CLMLocations, {}, 500, {}, {}, {});
  }));

  it('validate organization error is handled properly', inject(function($httpBackend, $controller, CLMLocations){
    validateErrorRequest($httpBackend, $controller, CLMLocations, {}, {}, 500, {}, {});
  }));

  it('validate application tag error is handled properly', inject(function($httpBackend, $controller, CLMLocations){
    validateErrorRequest($httpBackend, $controller, CLMLocations, {}, {}, {}, 500, {});
  }));

  it('validate filter error is handled properly', inject(function($httpBackend, $controller, CLMLocations){
    validateErrorRequest($httpBackend, $controller, CLMLocations, {}, {}, {}, {}, 500);
  }));
});

