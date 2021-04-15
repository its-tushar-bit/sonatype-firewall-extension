/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function UnauthenticatedRequestQueueService() {
  var service = {
    addRequest: addRequest,
    clearRequests: clearRequests,
    getRequests: getRequests,
    getPromises: getPromises,
  };
  var requests = [],
    promises = [];

  function addRequest(request) {
    requests.push(request);
  }

  function clearRequests() {
    requests.length = 0;
    promises.length = 0;
  }

  function getRequests() {
    return requests;
  }

  function getPromises() {
    if (promises.length) {
      return promises;
    }

    requests.forEach(function (request) {
      promises.push(request());
    });
    return promises;
  }

  return service;
}
