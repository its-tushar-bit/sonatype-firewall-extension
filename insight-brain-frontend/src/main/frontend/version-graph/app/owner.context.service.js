/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global clmEndpoint*/

export default function OwnerContext($rootScope) {
  var ownerContext = {
    ownerType: 'application',
    ownerId: null,
    setApplicationId: function (newApplicationId) {
      if (clmEndpoint.selectApplication) {
        if (newApplicationId) {
          var date = new Date();
          date.setTime(date.getTime() + 60 * 60 * 24 * 365);
          document.cookie = 'clmAppId=' + newApplicationId + '; expires=' + date.toGMTString();
        } else {
          document.cookie = 'clmAppId=; expires=Thu, 01-Jan-70 00:00:01 GMT;';
        }
      }
      ownerContext.ownerId = newApplicationId;
    },
  };

  if (clmEndpoint.selectApplication) {
    $rootScope.$watch(
      function () {
        return document.cookie;
      },
      function () {
        var clmAppId = null;

        $.each(document.cookie.split(';'), function (index, cookie) {
          cookie = cookie.split('=');
          if (cookie[0].trim() === 'clmAppId') {
            clmAppId = cookie[1].trim();
            return false;
          }
        });

        ownerContext.ownerId = clmAppId;
      }
    );
  }

  return ownerContext;
}
OwnerContext.$inject = ['$rootScope'];
