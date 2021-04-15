/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function SameOwnerViewSref(
  $compile,
  SameOwnerStateNavigationService
) {
  return {
    restrict: 'A',
    link: function (scope, element) {
      var newState = SameOwnerStateNavigationService.refactorStateParams.view(),
        newParamString = newState.params
          ? '(' + JSON.stringify(newState.params) + ')'
          : '';

      element.removeAttr('same-owner-view-sref');
      element.attr('ui-sref', newState.to + newParamString);

      $compile(element)(scope);
    },
  };
}

SameOwnerViewSref.$inject = ['$compile', 'SameOwnerStateNavigationService'];
