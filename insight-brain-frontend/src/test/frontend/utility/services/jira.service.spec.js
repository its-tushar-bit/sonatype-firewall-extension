/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityServicesModule from '../../../../main/frontend/utility/services/utility.services.module';

describe('jira.service.js', function() {
  var $httpBackend,
      CLMLocations;

  beforeEach(angular.mock.module(utilityServicesModule.name));

  beforeEach(inject(function(_$httpBackend_, _CLMLocations_) {
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('checks if jira service is enabled', inject([
    'jira.service', function(jiraService) {
      $httpBackend.expectGET(CLMLocations.getIsJiraEnabledUrl()).respond(true);

      jiraService.isEnabled().then(function(isEnabled) {
        expect(isEnabled).toBe(true);
      });

      $httpBackend.flush();
    }
  ]));

  it('caches jira service is enabled result', inject([
    'jira.service', '$timeout', function(jiraService, $timeout) {
      var isEnabledCalledAgain;
      $httpBackend.expectGET(CLMLocations.getIsJiraEnabledUrl()).respond(true);

      jiraService.isEnabled().then(function(isEnabled) {
        expect(isEnabled).toBe(true);
      });

      $httpBackend.flush();

      jiraService.isEnabled().then(function(isEnabled) {
        expect(isEnabled).toBe(true);
        isEnabledCalledAgain = true;
      });
      $timeout.flush();

      expect(isEnabledCalledAgain).toBeTruthy();
    }
  ]));

  it('returns a list of jira projects', inject([
    'jira.service', function(jiraService) {
      var expectedProjects = JiraServiceMockData.getJiraProjectsUrl();
      $httpBackend.expectGET(CLMLocations.getJiraProjectsUrl()).respond(expectedProjects);

      jiraService.getJiraProjects().then(function(projects) {
        expect(projects).toEqual(expectedProjects);
      });

      $httpBackend.flush();
    }
  ]));

  it('caches jira service is get list of projects result', inject([
    'jira.service', '$timeout', function(jiraService, $timeout) {
      var getProjectsIsCalledAgain;
      var expectedProjects = JiraServiceMockData.getJiraProjectsUrl();
      $httpBackend.expectGET(CLMLocations.getJiraProjectsUrl()).respond(expectedProjects);

      jiraService.getJiraProjects().then(function(projects) {
        expect(projects).toEqual(expectedProjects);
      });

      $httpBackend.flush();

      jiraService.getJiraProjects().then(function(projects) {
        expect(projects).toEqual(expectedProjects);
        getProjectsIsCalledAgain = true;
      });
      $timeout.flush();

      expect(getProjectsIsCalledAgain).toBeTruthy();
    }
  ]));
});
