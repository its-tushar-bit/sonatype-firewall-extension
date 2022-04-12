/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import { mapStateToThis } from 'MainRoot/owner.manager/label/label.editor.controller';

describe('label.editor.controller', () => {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, ($provide) => {
      SpecUtil.mockNgRedux($provide);
    })
  );

  let vm, scope;

  beforeEach(inject((_$rootScope_, $controller) => {
    scope = _$rootScope_.$new();

    vm = $controller('label.editor.controller', {
      $scope: scope,
    });
  }));

  describe('mapStateToThis', () => {
    it('sets dirtyLabel, loadError, submitError, isEditMode, siblings, isDirty, loading', () => {
      const state = {
        router: {
          currentParams: {
            labelId: 'ae63051b2e304c3bbabf94c2443b03fb',
          },
        },
        orgsAndPolicies: {
          labels: {
            loading: false,
            loadError: null,
            submitError: null,
            isDirty: false,
            currentLabel: {
              color: 'light-green',
              description: null,
              id: 'ae63051b2e304c3bbabf94c2443b03fb',
              label: 'n3',
              ownerId: '6b365e8a8000449aa924f194a7ed0d21',
              ownerType: 'APPLICATION',
            },
            siblings: [
              {
                color: 'light-green',
                description: null,
                id: 'ae63051b2e304c3bbabf94c2443b03fb',
                label: 'n3',
                ownerId: '6b365e8a8000449aa924f194a7ed0d21',
                ownerType: 'APPLICATION',
              },
            ],
          },
        },
      };

      const output = mapStateToThis(state);

      expect(output.loading).toBeFalse();
      expect(output.loadError).toBeNull();
      expect(output.submitError).toBeNull();
      expect(output.isDirty).toBeFalse();
      expect(output.isEditMode).toBeTrue();
      expect(output.siblings).toEqual(state.orgsAndPolicies.labels.siblings);
      expect(output.dirtyLabel).toEqual(state.orgsAndPolicies.labels.currentLabel);
    });
  });

  describe('on component init', () => {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    it('calls loadLabelsEditor', () => {
      expect(vm.loadLabelsEditor).toHaveBeenCalledTimes(1);
    });
  });

  describe('on $destroy()', () => {
    it('unsubscribes from redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      scope.$destroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('Page Changes', () => {
    it('navigates away if form is not dirty', () => {
      vm.isDirty = false;
      SpecUtil.expectStateChangeNotPrevented(scope);
    });

    it('does not navigate away if form is dirty', () => {
      vm.isDirty = true;
      SpecUtil.expectStateChangePrevented(scope);
    });
  });

  describe('on save', () => {
    beforeEach(() => {
      vm.dirtyLabel = {
        color: 'light-green',
        description: null,
        id: 'ae63051b2e304c3bbabf94c2443b03fb',
        label: 'n3',
        ownerId: '6b365e8a8000449aa924f194a7ed0d21',
        ownerType: 'APPLICATION',
      };
      vm.saveLabel = jasmine.createSpy('vm.saveLabel');
      vm.isEditMode = true;
    });

    it('calls saveLabels', () => {
      vm.labelEditorMask = {
        wrap: jasmine
          .createSpy('wrap')
          .and.callFake(() => Promise.resolve({ payload: { label: { label: 'name', color: 'dark-green' } } })),
      };

      vm.save();

      expect(vm.saveLabel).toHaveBeenCalledTimes(1);
    });
  });

  describe('on remove', () => {
    beforeEach(() => {
      vm.dirtyLabel = {
        color: 'light-green',
        description: null,
        id: 'ae63051b2e304c3bbabf94c2443b03fb',
        label: 'n3',
        ownerId: '6b365e8a8000449aa924f194a7ed0d21',
        ownerType: 'APPLICATION',
      };
    });

    it('calls resetDeleteModalState', () => {
      vm.deleteLabel();
      expect(vm.resetDeleteModalState).toHaveBeenCalledTimes(1);
    });
  });

  describe('on edit', () => {
    beforeEach(() => {
      vm.dirtyLabel = {
        description: 'description',
        color: 'dark-green',
        label: 'label name',
      };
    });

    it('calls setLabelDescription on label description change', () => {
      expect(vm.setLabelDescription).not.toHaveBeenCalled();
      vm.onDescriptionChange();
      expect(vm.setLabelDescription).toHaveBeenCalledOnceWith('description');
    });

    it('calls setLabelName on label name change', () => {
      expect(vm.setLabelName).not.toHaveBeenCalled();
      vm.onNameChange();
      expect(vm.setLabelName).toHaveBeenCalledOnceWith('label name');
    });

    it('calls setLabelColor on label color change', () => {
      expect(vm.setLabelColor).not.toHaveBeenCalled();
      vm.onColorChange();
      expect(vm.setLabelColor).toHaveBeenCalledOnceWith('dark-green');
    });
  });
});
