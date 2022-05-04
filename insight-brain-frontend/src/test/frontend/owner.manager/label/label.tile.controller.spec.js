/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import { mapStateToThis } from 'MainRoot/owner.manager/label/label.tile.controller';

describe('label.tile.controller', () => {
  var vm, scope, EventNameConstant;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, ($provide) => {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(($rootScope, $injector, $controller) => {
    scope = $rootScope.$new();
    EventNameConstant = $injector.get('event.name.constant');

    vm = $controller('LabelTileController', {
      $scope: scope,
    });
    scope.vm = vm;
  }));

  describe('mapStateToThis', () => {
    it('sets applicableLabels, error, loading and ownerName', () => {
      const state = {
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              name: 'ownerName',
            },
          },
          labels: {
            loading: false,
            loadError: null,
            applicableLabels: [
              {
                ownerId: '6b365e8a8000449aa924f194a7ed0d27',
                ownerName: 'dfgdf',
                ownerType: 'application',
                labels: [
                  {
                    color: 'light-green',
                    description: null,
                    id: 'ae63051b2e304c3bbabf94c2443b03fb',
                    label: 'n3',
                    ownerId: '6b365e8a8000449aa924f194a7ed0d21',
                    ownerType: 'APPLICATION',
                  },
                ],
                inherited: false,
              },
            ],
          },
        },
      };

      const output = mapStateToThis(state);

      expect(output.ownerName).toBe('ownerName');
      expect(output.loading).toBeFalse();
      expect(output.error).toBeNull();
      expect(output.applicableLabels).toEqual(state.orgsAndPolicies.labels.applicableLabels);
    });
  });

  describe('on component init', () => {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    it('calls loadApplicableLabels', () => {
      expect(vm.loadApplicableLabels).toHaveBeenCalled();
    });
  });

  describe('on $destroy()', () => {
    it('unsubscribes from redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      scope.$destroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('broadcast events', () => {
    it('calls loadApplicableLabels on policy.imported event', () => {
      expect(vm.loadApplicableLabels).toHaveBeenCalledTimes(1);
      scope.$emit(EventNameConstant.POLICY_IMPORTED);
      expect(vm.loadApplicableLabels).toHaveBeenCalledTimes(2);
    });

    it('calls loadApplicableLabels on broadcasted owner summary reload event', () => {
      expect(vm.loadApplicableLabels).toHaveBeenCalledTimes(1);
      scope.$emit(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);
      expect(vm.loadApplicableLabels).toHaveBeenCalledTimes(2);
    });
  });

  describe('editLabel', () => {
    it('calls goToEditLabel if label can be edited', () => {
      expect(vm.goToEditLabel).not.toHaveBeenCalled();
      vm.editLabel('labelId', false);
      expect(vm.goToEditLabel).toHaveBeenCalledTimes(1);
    });

    it('does not call goToEditLabel if label can not be edited', () => {
      expect(vm.goToEditLabel).not.toHaveBeenCalled();
      vm.editLabel('labelId', true);
      expect(vm.goToEditLabel).not.toHaveBeenCalled();
    });
  });
});
