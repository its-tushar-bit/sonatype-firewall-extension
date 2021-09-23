/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';

export default function SourceControlService($http, CLMLocations) {
  return {
    addSourceControlRecord: addSourceControlRecord,
    updateSourceControlRecord: updateSourceControlRecord,
    deleteSourceControlRecord: deleteSourceControlRecord,
    getProviderTypesMap: getProviderTypesMap,
    getProviderTypes: getProviderTypes,
    getCompositeSourceControlRecord: getCompositeSourceControlRecord,
    validateCompositeSCMConfig: validateCompositeSCMConfig,
    getSourceControlMetrics: getSourceControlMetrics,
  };

  /**
   * @param ownerId
   * @returns source control record with inherited fields
   */
  function getCompositeSourceControlRecord(ownerType, ownerId) {
    return $http.get(CLMLocations.getCompositeSourceControlUrl(ownerType, ownerId)).then(prop('data'));
  }

  function validateCompositeSCMConfig(ownerType, ownerId) {
    return $http.get(CLMLocations.getValidateScmConfigUrl(ownerType, ownerId)).then(prop('data'));
  }

  /**
   * @param ownerType   only applicable to APPLICATION
   * @param ownerId
   * @returns source control metrics associated with application
   */
  function getSourceControlMetrics(ownerType, ownerId) {
    return $http.get(CLMLocations.getSourceControlMetricsUrl(ownerType, ownerId)).then(prop('data'));
  }

  /**
   * Creates a source control record
   * @param ownerType Either application or organization
   * @param ownerId
   * @param sourceControl the source control object to be added
   * @returns source control record
   */
  function addSourceControlRecord(ownerType, ownerId, sourceControl) {
    let url = CLMLocations.getSourceControlUrl(ownerType, ownerId);
    let data = getDataFromSourceControl(ownerType, ownerId, sourceControl);
    return $http.post(url, data).then(prop('data'));
  }

  /**
   * Updates a source control record
   * @param ownerType Either application or organization
   * @param ownerId
   * @param sourceControl the source control object to be added
   * @returns source control record
   */
  function updateSourceControlRecord(ownerType, ownerId, sourceControl) {
    let url = CLMLocations.getSourceControlUrl(ownerType, ownerId);
    let data = getDataFromSourceControl(ownerType, ownerId, sourceControl);
    return $http.put(url, data).then(prop('data'));
  }

  /**
   * Deletes a source control record
   * @param ownerType Either application or organization
   * @param ownerId
   * @returns 204 on success
   */
  function deleteSourceControlRecord(ownerType, ownerId) {
    return $http.delete(CLMLocations.getSourceControlUrl(ownerType, ownerId)).then(prop('data'));
  }

  function getProviderTypesMap() {
    let providerTypesMap = {};
    getProviderTypes().forEach(function (providerType) {
      providerTypesMap[providerType.value] = providerType.name;
    });

    return providerTypesMap;
  }

  function getProviderTypes() {
    return [
      { name: 'Azure DevOps', value: 'azure' },
      { name: 'Bitbucket', value: 'bitbucket' },
      { name: 'GitHub', value: 'github' },
      { name: 'GitLab', value: 'gitlab' },
    ];
  }
}

function getDataFromSourceControl(ownerType, ownerId, sourceControl) {
  let data = {
    provider: sourceControl.provider,
    username: sourceControl.username,
    token: sourceControl.token,
    baseBranch: sourceControl.baseBranch,
    remediationPullRequestsEnabled: sourceControl.remediationPullRequestsEnabled,
    statusChecksEnabled: sourceControl.statusChecksEnabled,
    pullRequestCommentingEnabled: sourceControl.pullRequestCommentingEnabled,
    sourceControlScansEnabled: sourceControl.sourceControlScansEnabled,
    sourceControlScanTarget: sourceControl.sourceControlScanTarget,
  };
  if (ownerType === 'application') {
    data.repositoryUrl = sourceControl.repositoryUrl;
  }
  return data;
}

SourceControlService.$inject = ['$http', 'CLMLocations'];
