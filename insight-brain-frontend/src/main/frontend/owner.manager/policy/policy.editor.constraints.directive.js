/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './policy.editor.constraints.directive.html';

export default function PolicyEditorConstraintsDirective() {
  return {
    template,
    controller: 'policy.editor.constraints.controller',
    controllerAs: 'vm',
    scope: {
      constraints: '=',
      disabled: '=?editorDisabled',
    },
    bindToController: true,
  };
}
