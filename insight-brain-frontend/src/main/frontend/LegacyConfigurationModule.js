/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function restoreLegacyBehavior($qProvider, $compileProvider) {
  $qProvider.errorOnUnhandledRejections(false);
  $compileProvider.preAssignBindingsEnabled(true);
  // Note: hashPrefix('') equivalent is handled by React ui-router's hashLocationPlugin default
}

restoreLegacyBehavior.$inject = ['$qProvider', '$compileProvider'];

/**
 * This module configures various angular providers in a way that restores behaviors which existed before
 * the upgrade to Angular 1.6.x
 */
export default angular.module('legacyConfiguration', []).config(restoreLegacyBehavior);
