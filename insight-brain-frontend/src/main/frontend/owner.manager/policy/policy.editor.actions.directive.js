/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './policy.editor.actions.directive.html';

export default function PolicyEditorActionsDirective() {
  return {
    template,
    controller: 'policy.editor.actions.controller',
    controllerAs: 'vm',
    scope: {
      actions: '=',
      disabled: '=?editorDisabled',
    },
    bindToController: true,
  };
}
