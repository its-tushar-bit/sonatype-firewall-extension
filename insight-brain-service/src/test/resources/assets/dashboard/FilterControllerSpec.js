/* global describe, beforeEach, afterEach, module, inject, it, angular, expect */
describe('FilterController', function() {
  "use strict";
  var $scope, stageTypeData = [
    {
      id: 'type1',
      name: 'Type 1'
    },
    {
      id: 'type2',
      name: 'Type 2'
    }
  ], applicationData = [
    {
      id: 'applicationId1',
      publicId: 'applicationPublicId1',
      name: 'ApplicationOne',
      organizationId: 'orgId1'
    },
    {
      id: 'applicationId2',
      publicId: 'applicationPublicId2',
      name: 'ApplicationTwo',
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
    policyThreatCategoryFilters: ['SECURITY', 'OTHER'],
    stageTypeFilters: ['type1', 'type2'],
    tagFilters: ['tagId1', 'tagId2'],
    applicationFilters: ['applicationId1', 'applicationId2'],
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
    $controller('FilterController', { $scope: $scope});
    //TODO: NEWFILTER: remove when old filter panel is removed
    $scope.doLoad();
    $httpBackend.flush();
  }));

  afterEach(inject(function($httpBackend) {
    if ($scope) {
      $scope.$destroy();
    }
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  it('data loaded and placed in $scope', inject(function() {
    expect($scope.filters.policyThreatTypes).toEqual(['SECURITY', 'OTHER']);
    expect($scope.filters.stageTypeIds).toEqual(['type1', 'type2']);
    expect($scope.filters.applicationTagIds).toEqual(['tagId1', 'tagId2']);
    expect($scope.filters.applicationIds).toEqual(['applicationId1', 'applicationId2']);
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
    expect($scope.applicationsTooltip).toBe('ApplicationOne<br/>ApplicationTwo');
    expect($scope.applicationTagsTooltip).toBe('TagOne<br/>TagTwo');
    expect($scope.stageTypesTooltip).toBe('Type 1<br/>Type 2');
    expect($scope.policyTypesTooltip).toBe('Security<br/>Other');
  }));

  it('validate filter and dirty filter usage in scope', inject(function($httpBackend, CLMLocations) {
    //make sure updating the text fields doesn't update the filter (at least prior to hitting save)
    $scope.$apply(function() {
      $scope.dirtyFilters.applicationIds = ['applicationId1'];
    });
    expect($scope.filters.applicationIds).toEqual(['applicationId1', 'applicationId2']);

    //make sure cancel cleans out the dirty filter
    $scope.$apply(function() {
      $scope.cancel();
    });
    expect($scope.dirtyFilters.applicationIds).toEqual(['applicationId1', 'applicationId2']);

    //make sure save puts data into the filter (from dirty filter)
    $scope.$apply(function() {
      $scope.dirtyFilters.applicationIds = ['applicationId1'];
    });
    var data = angular.copy(filterData);
    data.applicationFilters = ['applicationId1'];
    $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), data).respond(200);
    $scope.save();
    $httpBackend.flush();
    expect($scope.dirtyFilters.applicationIds).toEqual(['applicationId1']);
    expect($scope.filters.applicationIds).toEqual(['applicationId1']);

    //make sure reset isn't automatically put in effect
    $scope.$apply(function() {
      $scope.reset();
      expect($scope.dirtyFilters.applicationIds).toEqual([]);
      expect($scope.filters.applicationIds).toEqual(['applicationId1']);
    });
  }));
});