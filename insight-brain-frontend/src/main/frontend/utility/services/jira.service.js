/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function JiraService($http, $q, CLMLocations) {
  var isEnabledDeferred, getProjectsDeferred;

  var service = {
    isEnabled: isEnabled,
    getJiraProjects: getJiraProjects,
  };

  function isEnabled() {
    if (!isEnabledDeferred) {
      isEnabledDeferred = $q.defer();
      $http.get(CLMLocations.getIsJiraEnabledUrl()).then(
        function (isJiraEnabled) {
          isEnabledDeferred.resolve(isJiraEnabled.data);
        },
        function (error) {
          isEnabledDeferred.reject(error);
          // Allow for retrying request
          isEnabledDeferred = null;
        }
      );
    }
    return isEnabledDeferred.promise;
  }

  function getJiraProjects() {
    if (!getProjectsDeferred) {
      getProjectsDeferred = $q.defer();
      $http.get(CLMLocations.getJiraProjectsUrl()).then(
        function (jiraProjects) {
          getProjectsDeferred.resolve(jiraProjects.data);
        },
        function (error) {
          getProjectsDeferred.reject(error);
          // Allow for retrying request
          getProjectsDeferred = null;
        }
      );
    }
    return getProjectsDeferred.promise;
  }

  return service;
}

JiraService.$inject = ['$http', '$q', 'CLMLocations'];
