/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function () {
  return {
    scope: {
      licenses: '=',
      status: '@',
      emptyText: '@',
    },
    template:
      '<span ng-repeat="license in licenses" class="license">{{license.licenseName}}{{!$last ? "," : ""}}</span>' +
      '<span ng-if="licenses.length == 0">{{emptyText}}</span>' +
      '<span ng-if="status" class="clm-license-status {{status | lowercase}}">{{status}}</span>',
  };
}
