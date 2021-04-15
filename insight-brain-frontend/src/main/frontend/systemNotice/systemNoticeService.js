/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
var DEFAULT_SYSTEM_NOTICE = {
  message: 'Error: could not get the system notice from the server',
  enabled: true,
};

export default function systemNoticeService($http, $q, CLMLocations) {
  return {
    getSystemNotice: getSystemNotice,
    getDefaultSystemNotice: getDefaultSystemNotice,
    saveSystemNotice: saveSystemNotice,
  };

  function getSystemNotice() {
    return $http.get(CLMLocations.getSystemNoticeFetchUrl()).then(function (response) {
      return response.data;
    });
  }

  function getDefaultSystemNotice() {
    return DEFAULT_SYSTEM_NOTICE;
  }

  function saveSystemNotice(systemNotice) {
    return $http.put(CLMLocations.getSystemNoticeUrl(), systemNotice).then(function (response) {
      return response.data;
    });
  }
}

systemNoticeService.$inject = ['$http', '$q', 'CLMLocations'];
