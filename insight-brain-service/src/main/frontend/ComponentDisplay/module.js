/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import AngularCommonModule from '../util/AngularCommon';
import componentName from './componentName';
import componentDisplay from './componentDisplay';
import periodDelimiter from './periodDelimiter';

export default angular.module('ComponentDisplay', [AngularCommonModule.name])
    .run(cacheTemplates)
    .directive('componentName', componentName)
    .directive('componentDisplay', componentDisplay)
    .filter('periodDelimiter', periodDelimiter);

function cacheTemplates($templateCache) {
  $templateCache.put('displayname',
      '<span ng-repeat="part in displayName.parts">' +
      '<span ng-if="part.field">{{ part.value | periodDelimiter }}</span>' +
      '<span ng-if="!part.field" class="wrap-force-break">{{ part.value | periodDelimiter }}</span>' +
      '</span>'
  );
  $templateCache.put('pathnames-display',
      '<div class="pathname">' +
      '<em>{{component.pathnames[0] | fileName}}</em>' +
      '</div>'
  );
  $templateCache.put('unknown-display',
      '<div>' +
      '<em>Unknown</em>' +
      '</div>'
  );
  $templateCache.put('component-display',
      '<div>' +
      '<div ng-if="component.displayName" ng-class="{\'truncate-ellipsis\': truncate}"><span component-name="component.displayName"></span></div>' +
      '<div ng-if="!component.displayName && component.pathnames" ng-include="\'pathnames-display\'" ng-class="{\'truncate-ellipsis\': truncate}"></div>' +
      '<div ng-if="!component.displayName && !component.pathnames" ng-include="\'unknown-display\'" ng-class="{\'truncate-ellipsis\': truncate}"></div>' +
      '</div>'
  );
  $templateCache.put('linked-component-display',
      '<a ui-sref="dashboard.component({ hash: component.hash })">' +
      '<div ng-include="\'component-display\'"></div>' +
      '</a>'
  );
}

cacheTemplates.$inject = ['$templateCache'];
