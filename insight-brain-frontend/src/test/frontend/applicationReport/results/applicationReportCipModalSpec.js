/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import applicationReportModule from '../../../../main/frontend/applicationReport/module';
import { mapStateToThis } from '../../../../main/frontend/applicationReport/results/cipModal/applicationReportCipModal';

describe('applicationReportCipModal', function () {
  let vm, scope;

  beforeEach(
    angular.mock.module(applicationReportModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function ($componentController, $rootScope) {
    scope = $rootScope.$new();
    vm = $componentController('applicationReportCipModal', {
      $scope: scope,
    });
    scope.vm = vm;
    vm.$onInit();
  }));

  describe('$onDestroy()', function () {
    it('unsubscribes from redux store', function () {
      expect(vm.unsubscribeFromReduxStore).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribeFromReduxStore).toHaveBeenCalledTimes(1);
    });
  });

  describe('mapStateToThis', () => {
    describe('when selectedRootAncestor is not set', function () {
      it('sets selectedComponent using selectedComponentIndex and does not set previousComponent', function () {
        const selectedComponent = { foo: 'bar' };
        const state = {
          applicationReport: {
            selectedRootAncestor: null,
            selectedComponentIndex: 1,
            selectedComponent: selectedComponent,
            selectedReport: {
              displayedEntries: [{}, selectedComponent, {}],
            },
          },
        };

        const output = mapStateToThis(state);
        expect(output.selectedComponent).toBe(selectedComponent);
        expect(output.previousComponent).toBeNull();
      });
    });

    describe('when selectedRootAncestor is set', function () {
      it('sets selectedComponent to selectedRootAncestor and sets previousComponent using selectedComponentIndex', function () {
        const selectedComponent = {
          displayName: {
            parts: [
              { field: 'Group', value: 'org.springframework' },
              { value: ' : ' },
              { field: 'Artifact', value: 'spring-expression' },
              { value: ' : ' },
              { field: 'Version', value: '3.2.4.RELEASE' },
            ],
          },
        };
        const selectedRootAncestor = { foo: 'baz' };
        const state = {
          applicationReport: {
            selectedRootAncestor,
            selectedComponentIndex: 0,
            selectedComponent: selectedComponent,
            selectedReport: {
              displayedEntries: [selectedComponent],
            },
          },
        };

        const output = mapStateToThis(state);
        expect(output.selectedComponent).toBe(selectedRootAncestor);
        expect(output.previousComponent).toBe(selectedComponent);
      });
    });
  });
});
