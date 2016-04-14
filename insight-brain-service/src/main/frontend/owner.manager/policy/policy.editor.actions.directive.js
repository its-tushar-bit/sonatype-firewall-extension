/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function PolicyEditorActionsDirective() {
    return {
      templateUrl: 'owner.manager/policy/policy.editor.actions.directive.html',
      controller: 'policy.editor.actions.controller',
      controllerAs: 'vm',
      scope: {
        actions: '=',
        disabled: '=?editorDisabled'
      },
      bindToController: true
    };
  }

  angular //
      .module('owner.manager.module') //
      .directive('policyEditorActions', PolicyEditorActionsDirective);

}(angular));
