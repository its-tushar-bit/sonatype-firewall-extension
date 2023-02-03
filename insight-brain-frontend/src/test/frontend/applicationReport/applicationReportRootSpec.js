/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import applicationReportModule from '../../../main/frontend/applicationReport/module';

describe('applicationReportRoot', function () {
  let createController;

  beforeEach(angular.mock.module(applicationReportModule.name));

  beforeEach(
    angular.mock.module(function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function ($componentController) {
    createController = function (publicId, scanId, unknownJs, embeddable, policyViolationId, componentHash, tabId) {
      const vm = $componentController('applicationReportRoot', {
        $state: {
          params: {
            publicId,
            scanId,
            unknownJs,
            embeddable,
            policyViolationId,
            componentHash,
            tabId,
          },
        },
      });

      vm.$onInit();

      return vm;
    };
  }));

  describe('$onInit()', function () {
    it('subscribes to the redux store', () => {
      const vm = createController();

      expect(vm.unsubscribe).toBeDefined();
    });

    it('calls setReportParameters with the correct parameters', function () {
      const vm = createController('testApp', 'testReport', false, true, undefined, undefined, undefined, true);
      expect(vm.setReportParameters).toHaveBeenCalledWith(
        'testApp',
        'testReport',
        false,
        true,
        undefined,
        undefined,
        undefined,
        true
      );
    });

    it('calls setReportParameters with the correct parameters when returning from addWaiver', function () {
      const vm = createController(
        'testApp',
        'testReport',
        false,
        true,
        'policyViolationId',
        undefined,
        undefined,
        true
      );

      expect(vm.setReportParameters).toHaveBeenCalledWith(
        'testApp',
        'testReport',
        false,
        true,
        'policyViolationId',
        undefined,
        undefined,
        true
      );
    });

    it('calls setReportParameters with the correct parameters when returning from the transitive violations page', function () {
      const vm = createController(
        'testApp',
        'testReport',
        false,
        true,
        'policyViolationId',
        'componentHash',
        'tabId',
        true
      );

      expect(vm.setReportParameters).toHaveBeenCalledWith(
        'testApp',
        'testReport',
        false,
        true,
        'policyViolationId',
        'componentHash',
        'tabId',
        true
      );
    });
  });

  describe('$onDestroy()', function () {
    it('unsubscribes from redux store', function () {
      const vm = createController();

      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });
});
