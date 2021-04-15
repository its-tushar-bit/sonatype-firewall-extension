/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './userDetailsModal.html';
import { sort } from 'ramda';

export default {
  template,
  controller: userDetailsModalController,
  controllerAs: 'vm',
  bindings: {
    close: '&',
    currentUser: '<',
  },
};

function userDetailsModalController() {
  const vm = this;

  Object.assign(vm, {
    getGroups() {
      return sort((a, b) => a.localeCompare(b), vm.currentUser.groups).join(', ');
    },
  });
}
