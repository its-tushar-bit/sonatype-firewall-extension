/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default {
  controllerAs: 'vm',
  bindings: {
    component: '<',
    truncate: '<'
  },
  template: `
    <div>
       <div ng-if="vm.component.displayName" ng-class="{'truncate-ellipsis': vm.truncate}">
         <component-name name="vm.component.displayName"></component-name>
       </div>
       <div ng-if="!vm.component.displayName && (vm.component.filename || vm.component.filenames)"
            ng-class="{'truncate-ellipsis': vm.truncate}">
         <filename-display component="vm.component"></filename-display>
       </div>
       <div ng-if="!vm.component.displayName && !vm.component.filename" ng-class="{'truncate-ellipsis': vm.truncate}">
         <div><em>Unknown</em></div>
       </div>
    </div>`
};
