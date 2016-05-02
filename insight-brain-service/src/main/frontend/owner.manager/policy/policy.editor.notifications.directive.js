/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function PolicyEditorNotificationsDirective() {
    return {
      templateUrl: 'owner.manager/policy/policy.editor.notifications.directive.html',
      controller: 'policy.editor.notifications.controller',
      controllerAs: 'vm',
      scope: {
        notifications: '=',
        disabled: '=?editorDisabled'
      },
      bindToController: true
    };
  }

  angular //
      .module('owner.manager.module') //
      .directive('policyEditorNotifications', PolicyEditorNotificationsDirective);

}(angular));
