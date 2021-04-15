/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function SameOwnerEditSref($compile, SameOwnerStateNavigationService) {
  return {
    restrict: 'A',
    link: function (scope, element, attrs) {
      var parsedState = parseStateRef(attrs.sameOwnerEditSref),
        params = JSON.parse(parsedState.paramExpr),
        newState = SameOwnerStateNavigationService.refactorStateParams.edit(parsedState.state, params),
        newParamString = newState.params ? '(' + JSON.stringify(newState.params) + ')' : '';

      element.removeAttr('same-owner-edit-sref');
      element.attr('ui-sref', newState.to + newParamString);

      $compile(element)(scope);

      // parse function taken from angular-ui-router
      // ref: https://github.com/angular-ui/ui-router/blob/master/src/stateDirectives.js#L1.
      function parseStateRef(ref) {
        var parsed = ref.replace(/\n/g, ' ').match(/^([^(]+?)\s*(\((.*)\))?$/);

        if (!parsed || parsed.length !== 4) {
          throw new Error("Invalid state ref '" + ref + "'");
        }

        return { state: parsed[1], paramExpr: parsed[3] || null };
      }
    },
  };
}

SameOwnerEditSref.$inject = ['$compile', 'SameOwnerStateNavigationService'];
