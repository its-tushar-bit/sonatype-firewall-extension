/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { faQuestionCircle } from '@fortawesome/pro-solid-svg-icons';
import template from './helpMenu.html';

/* global clmServerVersion */
function HelpMenuController() {
  var vm = this;

  vm.faQuestionCircle = faQuestionCircle;

  vm.majorMinorVersion = clmServerVersion.split('.').splice(0, 2).join('.');
}

export default {
  controller: HelpMenuController,
  controllerAs: 'vm',
  template,
};
