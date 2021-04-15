/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import template from './changeDefaultAdminPasswordNotice.html';

export default {
  template,
  controller: changeDefaultAdminPasswordNoticeController,
  controllerAs: 'vm',
};

function changeDefaultAdminPasswordNoticeController($ngRedux, actions) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, actions)(vm);
    },

    $onDestroy() {
      vm.unsubscribe();
    },
  });
}

function mapStateToThis({ user }) {
  return pick(['isDefaultUser', 'shouldDisplayNotice'], user);
}

changeDefaultAdminPasswordNoticeController.$inject = ['$ngRedux', 'userActions'];
