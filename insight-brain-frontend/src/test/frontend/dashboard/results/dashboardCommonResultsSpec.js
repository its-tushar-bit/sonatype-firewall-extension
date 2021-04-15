/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import dashboardResultsModule from '../../../../main/frontend/dashboard/results/module';
import dashboardModule from '../../../../main/frontend/dashboard/dashboard.module';
import dashboardUtilsModule from '../../../../main/frontend/dashboard/utils/dashboard.utils.module';

describe('dashboardCommonResultsSpec', function () {
  var vm, dialogMock, applicationStoreMock, $ngRedux, dashboardFilterActionsMock;

  beforeEach(
    angular.mock.module(
      dashboardResultsModule.name,
      dashboardModule.name,
      dashboardUtilsModule.name,
      function ($provide) {
        $provide.service('$ngRedux', function () {
          return jasmine.createSpyObj('$ngRedux', ['dispatch']);
        });
      }
    )
  );

  beforeEach(inject(function ($componentController, _$ngRedux_) {
    $ngRedux = _$ngRedux_;
    dialogMock = jasmine.createSpyObj('Dialog', ['open']);
    applicationStoreMock = jasmine.createSpyObj('ApplicationStore', ['refresh']);
    dashboardFilterActionsMock = jasmine.createSpyObj('dashboardFilterActions', ['loadFilter']);
    vm = $componentController('dashboardCommonResults', {
      Dialog: dialogMock,
      ApplicationStore: applicationStoreMock,
      dashboardFilterActions: dashboardFilterActionsMock,
    });
    vm.maxResults = 1;
    vm.needsAcknowledgement = false;
  }));

  describe('loadCommonResults', function () {
    it('returns true when nothing available', function () {
      expect(vm.results).toBeUndefined();
      expect(vm.needsAcknowledgement).toBe(false);
      expect(vm.maxResults).toBe(1);
      expect(vm.loadCommonResults()).toBe(true);
    });

    it('returns true when results array is empty', function () {
      vm.results = [];
      expect(vm.loadCommonResults()).toBe(true);
    });

    it('returns true when results array is not empty', function () {
      vm.results = [{}, {}];
      vm.numResults = 2;
      expect(vm.loadCommonResults()).toBe(true);
    });

    it('returns true when results array length is greater than maxResults', function () {
      vm.results = [{}, {}];
      vm.numResults = 2;
      expect(vm.maxResults).toBe(1);
      expect(vm.loadCommonResults()).toBe(true);
    });

    it('returns false when results array length is not greater than maxResults', function () {
      vm.results = [{}];
      vm.numResults = 1;
      expect(vm.maxResults).toBe(1);
      expect(vm.loadCommonResults()).toBe(false);
    });

    it('returns true when results array length is not greater than maxResults but needsAcknowledgement is true', () => {
      vm.results = [{}];
      vm.numResults = 1;
      vm.needsAcknowledgement = true;
      expect(vm.maxResults).toBe(1);
      expect(vm.loadCommonResults()).toBe(true);
    });
  });

  describe('$onChanges()', function () {
    it('opens "Filter invalid" dialog if got 403 error', function () {
      vm.$onChanges({ error: { currentValue: { status: 403 } } });
      expect(dialogMock.open).toHaveBeenCalled();
      expect(vm.errorMessage).toBeUndefined();
    });

    it('sets vm.errorMessage if got non 403 error', function () {
      vm.$onChanges({ error: { currentValue: { status: 404 } } });
      expect(dialogMock.open).not.toHaveBeenCalled();
      expect(vm.errorMessage).toBe('Error 404');
    });

    describe('"Filter invalid" dialog', function () {
      it('reloads filter when OK button is clicked', function () {
        vm.$onChanges({ error: { currentValue: { status: 403 } } });
        expect(dialogMock.open).toHaveBeenCalled();
        expect(dialogMock.open.calls.count()).toEqual(1);
        var dialogConfig = dialogMock.open.calls.argsFor(0)[0];
        dashboardFilterActionsMock.loadFilter.and.returnValue('load filter action');
        // click OK button in the dialog
        dialogConfig.buttons[0].click();
        expect(applicationStoreMock.refresh).toHaveBeenCalled();
        expect($ngRedux.dispatch).toHaveBeenCalledWith('load filter action');
      });
    });
  });
});
