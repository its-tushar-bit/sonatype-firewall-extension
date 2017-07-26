/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function PolicyEditorConstraintsDirective() {
  return {
    templateUrl: 'owner.manager/policy/policy.editor.constraints.directive.html',
    controller: 'policy.editor.constraints.controller',
    controllerAs: 'vm',
    scope: {
      constraints: '=',
      isNewPolicy: '=',
      disabled: '=?editorDisabled'
    },
    bindToController: true
  };
}

angular //
    .module('owner.manager.module') //
    .directive('policyEditorConstraints', PolicyEditorConstraintsDirective);
