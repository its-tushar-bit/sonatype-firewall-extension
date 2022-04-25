/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import OwnerUtils from '../owner.utils';
import { mapStateToThis } from 'MainRoot/owner.manager/category/application.category.tile.controller.app';
import * as applicationSelectors from 'MainRoot/OrgsAndPolicies/applicationsSelectors';
import * as assignApplicationCategoriesSelectors from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSelectors';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

describe('application.category.tile.controller.app.spec.js', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  function createTests(type) {
    let controller;
    let scope;
    let isApp = type === 'application';
    let $rootScope;
    let EventNameConstant;

    beforeEach(inject(function (_$rootScope_, $controller, $injector) {
      scope = _$rootScope_.$new();
      $rootScope = _$rootScope_;
      EventNameConstant = $injector.get('event.name.constant');
      scope.isApp = true;
      controller = $controller;
      $controller('ApplicationCategoryTileControllerApp', {
        $scope: scope,
      });
    }));

    if (isApp) {
      describe('on initialization', () => {
        it('subscribes to the redux store', () => {
          expect(scope.unsubscribe).toBeDefined();
        });
        it('calls loadApplicablePolicyMonitoring', () => {
          expect(scope.loadApplications).toHaveBeenCalledTimes(1);
          expect(scope.loadApplicableCategories).toHaveBeenCalledTimes(1);
          expect(scope.loadAppliedCategories).toHaveBeenCalledTimes(1);
        });
        it('does not call loadApplicablePolicyMonitoring', () => {
          scope.isApp = false;
          controller('application.category.editor.controller', {
            $scope: scope,
          });
          expect(scope.loadApplications).not.toHaveBeenCalled();
          expect(scope.loadApplicableCategories).not.toHaveBeenCalled();
          expect(scope.loadAppliedCategories).not.toHaveBeenCalled();
        });
      });

      describe('on $destroy()', () => {
        it('unsubscribes from redux store', () => {
          expect(scope.unsubscribe).not.toHaveBeenCalled();
          scope.$destroy();
          expect(scope.unsubscribe).toHaveBeenCalledTimes(1);
        });
      });

      describe('mapStateToThis', () => {
        it('sets shouldShowQuarantineWarning to component', () => {
          spyOn(orgsAndPoliciesSelectors, 'selectOwnerName').and.returnValue('OwnerName');
          spyOn(applicationSelectors, 'selectLoadingApplications').and.returnValue(false);
          spyOn(assignApplicationCategoriesSelectors, 'selectLoadingApplicableCategories').and.returnValue(false);
          spyOn(assignApplicationCategoriesSelectors, 'selectLoadingAppliedCategories').and.returnValue(false);
          spyOn(applicationSelectors, 'selectLoadApplicationsError').and.returnValue(null);
          spyOn(assignApplicationCategoriesSelectors, 'selectLoadApplicableCategoriesError').and.returnValue(null);
          spyOn(assignApplicationCategoriesSelectors, 'selectLoadAppliedCategoriesError').and.returnValue('some error');
          spyOn(assignApplicationCategoriesSelectors, 'selectAppliedCategories').and.returnValue([
            {
              id: '13dfce231ca24289bec319fddf4bef88',
              organizationId: 'ROOT_ORGANIZATION_ID',
              name: 'Internal',
              description: 'Applications that are used only by your employees',
              color: 'dark-green',
            },
          ]);
          spyOn(assignApplicationCategoriesSelectors, 'selectAreAnyCategoriesDefined').and.returnValue(true);
          spyOn(routerSelectors, 'selectIsApplication').and.returnValue(true);

          const { ownerName, loading, error, appliedCategories, areAnyCategoriesDefined, isApp } = mapStateToThis({});

          expect(ownerName).toBe('OwnerName');
          expect(loading).toBeFalse();
          expect(error).toBe('some error');
          expect(appliedCategories).toEqual([
            {
              id: '13dfce231ca24289bec319fddf4bef88',
              organizationId: 'ROOT_ORGANIZATION_ID',
              name: 'Internal',
              description: 'Applications that are used only by your employees',
              color: 'dark-green',
            },
          ]);
          expect(areAnyCategoriesDefined).toBeTrue();
          expect(isApp).toBeTrue();
        });
      });

      it('Reloads on broadcasted owner summary reload event', function () {
        expect(scope.loadApplications).toHaveBeenCalledTimes(1);
        expect(scope.loadApplicableCategories).toHaveBeenCalledTimes(1);
        expect(scope.loadAppliedCategories).toHaveBeenCalledTimes(1);

        $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

        expect(scope.loadApplications).toHaveBeenCalledTimes(2);
        expect(scope.loadApplicableCategories).toHaveBeenCalledTimes(2);
        expect(scope.loadAppliedCategories).toHaveBeenCalledTimes(2);
      });

      it('Reloads on policy.imported event', function () {
        expect(scope.loadApplications).toHaveBeenCalledTimes(1);
        expect(scope.loadApplicableCategories).toHaveBeenCalledTimes(1);
        expect(scope.loadAppliedCategories).toHaveBeenCalledTimes(1);

        $rootScope.$broadcast('policy.imported');

        expect(scope.loadApplications).toHaveBeenCalledTimes(2);
        expect(scope.loadApplicableCategories).toHaveBeenCalledTimes(2);
        expect(scope.loadAppliedCategories).toHaveBeenCalledTimes(2);
      });

      it('Updates Owner name on broadcasted updated owner event', function () {
        $rootScope.$broadcast(EventNameConstant.OWNER_UPDATED, { name: 'Bob' });
        expect(scope.updatedOwnerHandlerAction).toHaveBeenCalledOnceWith('Bob');
      });
    }
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
