/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import applicationReportModule from '../../../main/frontend/applicationReport/module';

describe('applicationReportRoot', function() {

  let createController;

  beforeEach(angular.mock.module(applicationReportModule.name));

  beforeEach(angular.mock.module(function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function($componentController) {
    createController = function(publicId, scanId, unknownJs, embeddable) {
      const vm = $componentController('applicationReportRoot', {
        $state: {
          params: {
            publicId,
            scanId,
            unknownJs,
            embeddable
          }
        }
      });

      vm.$onInit();

      return vm;
    };
  }));

  describe('$onInit()', function() {
    it('subscribes to the redux store', () => {
      const vm = createController();

      expect(vm.unsubscribe).toBeDefined();
    });

    it('calls setReportParameters with the correct parameters', function() {
      const vm = createController('testApp', 'testReport', false, true);

      expect(vm.setReportParameters).toHaveBeenCalledWith('testApp', 'testReport', false, true);
    });
  });

  describe('$onDestroy()', function() {
    it('unsubscribes from redux store', function() {
      const vm = createController();

      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });
});
