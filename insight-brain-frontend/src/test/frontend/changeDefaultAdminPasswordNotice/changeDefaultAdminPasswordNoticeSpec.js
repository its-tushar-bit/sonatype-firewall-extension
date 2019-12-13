/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import changeDefaultAdminPasswordNoticeModule from '../../../main/frontend/changeDefaultAdminPasswordNotice/module';

describe('changeDefaultAdminPasswordNotice component', () => {
  let vm;

  beforeEach(angular.mock.module(changeDefaultAdminPasswordNoticeModule.name, ($provide) => {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(($componentController) => {
    vm = $componentController('changeDefaultAdminPasswordNotice');
    vm.$onInit();
  }));

  describe('$onInit', () => {
    it('subscribes to ngRedux', () => {
      vm.$onInit();
      expect(vm.unsubscribe).toBeDefined();
    });
  });

  describe('$onDestroy', () => {
    it('unsubscribes from ngRedux', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });
});
