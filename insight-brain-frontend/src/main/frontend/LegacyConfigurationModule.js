/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function restoreLegacyBehavior($qProvider, $compileProvider, $locationProvider) {
  $qProvider.errorOnUnhandledRejections(false);
  $compileProvider.preAssignBindingsEnabled(true);
  $locationProvider.hashPrefix('');
}

restoreLegacyBehavior.$inject = ['$qProvider', '$compileProvider', '$locationProvider'];

/**
 * This module configures various angular providers in a way that restores behaviors which existed before
 * the upgrade to Angular 1.6.x and ui.router 1.x
 */
export default angular
  .module('legacyConfiguration', ['ui.router', 'ui.router.state.events'])
  .config(restoreLegacyBehavior);
