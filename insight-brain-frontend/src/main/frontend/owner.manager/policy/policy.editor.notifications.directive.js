/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './policy.editor.notifications.directive.html';

export default function PolicyEditorNotificationsDirective() {
  return {
    template,
    controller: 'policy.editor.notifications.controller',
    controllerAs: 'vm',
    scope: {
      notifications: '=',
      disabled: '=?editorDisabled',
    },
    bindToController: true,
  };
}
