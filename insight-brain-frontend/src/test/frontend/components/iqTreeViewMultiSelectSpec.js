/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import componentsModule from '../../../main/frontend/components/module';

describe('iqTreeViewMultiSelect', function() {

  var vm, onChange;

  var available = [
    {id: 'foo', name: 'Foo'},
    {id: 'bar', name: 'Bar'},
    {id: 'baz', name: 'Baz'}
  ];

  var allSelected = new Set(['foo', 'bar', 'baz']);

  beforeEach(angular.mock.module(componentsModule.name));

  beforeEach(inject(function($componentController) {
    onChange = jasmine.createSpy('onChange');
    vm = $componentController('iqTreeViewMultiSelect', null, {
      available: available,
      selected: new Set(),
      onChange: onChange
    });
  }));

  describe('initialization', function() {
    it('sets selected to empty Set', inject(function($componentController) {
      var vmWithNoBindings = $componentController('iqTreeViewMultiSelect', null, null);
      expect(vmWithNoBindings.selected instanceof Set).toBe(true);
      expect(vmWithNoBindings.selected.size).toBe(0);
    }));
  });

  describe('$onChanges()', function() {
    it('clones selected', function() {
      expect(vm.selected.size).toBe(0);
      var updatedSelected = new Set(['baz']);
      vm.$onChanges({selected: {currentValue: updatedSelected}});
      expect(vm.selected.size).toBe(1);
      expect(vm.selected).toEqual(new Set(['baz']));
      expect(vm.selected).not.toBe(updatedSelected);
    });

    describe('filterThreshold parameter', function() {
      it('sets filterThreshold', function() {
        expect(vm.filterThreshold).toBe(10);
        vm.$onChanges({providedFilterThreshold: {currentValue: 5}});
        expect(vm.filterThreshold).toBe(5);
      });

      it('invalid filterThreshold parameter is ignored', function() {
        expect(vm.filterThreshold).toBe(10);
        vm.$onChanges({providedFilterThreshold: {currentValue: 'foo'}});
        expect(vm.filterThreshold).toBe(10);
      });

      it('empty filterThreshold parameter is ignored', function() {
        expect(vm.filterThreshold).toBe(10);
        vm.$onChanges({providedFilterThreshold: {currentValue: ''}});
        expect(vm.filterThreshold).toBe(10);
      });
    });
  });

  describe('showFilter()', function() {
    it('returns false if number of options is less then filterThreshold', function() {
      expect(vm.filterThreshold).toBe(10);
      expect(vm.showFilter()).toBe(false);
    });

    it('returns true if number of options is greater then filterThreshold', function() {
      vm.$onChanges({providedFilterThreshold: {currentValue: 2}});
      expect(vm.filterThreshold).toBe(2);
      expect(vm.showFilter()).toBe(true);
    });

    it('returns false if number of options is the same as filterThreshold', function() {
      vm.$onChanges({providedFilterThreshold: {currentValue: 3}});
      expect(vm.showFilter()).toBe(false);
    });
  });

  describe('allSelected()', function() {
    describe('when entities are not filtered', function() {
      it('returns true when all options are selected', function() {
        expect(vm.allSelected()).toBe(false);
        vm.$onChanges({selected: {currentValue: new Set(['baz'])}});
        expect(vm.allSelected()).toBe(false);
        vm.$onChanges({selected: {currentValue: allSelected}});
        expect(vm.allSelected()).toBe(true);
      });
    });

    describe('when entities are filtered', function() {
      it('returns true when all filtered options are selected', function() {
        vm.$onChanges({selected: {currentValue: new Set(['foo'])}});
        expect(vm.allSelected()).toBe(false);
        vm.filter = 'f';
        expect(vm.allSelected()).toBe(true);
      });
    });
  });

  describe('generateCheckboxId()', function() {
    it('returns generated string', function() {
      expect(vm.generateCheckboxId('Parent', 'child 2')).toBe('iq-tree-view-checkbox-parent-child-2');
    });
  });

  describe('toggle()', function() {
    it('when selected, calls onChange with the updated selected Set and toggledId', function() {
      expect(vm.selected.has('foo')).toBe(false);
      vm.toggle('foo');
      expect(onChange).toHaveBeenCalledWith({selected: new Set(['foo']), toggledId: 'foo'});
    });

    it('when unselected, calls onChange with the updated selected Set and toggledId', function() {
      vm.$onChanges({selected: {currentValue: new Set(['foo'])}});
      expect(vm.selected.has('foo')).toBe(true);
      vm.toggle('foo');
      expect(onChange).toHaveBeenCalledWith({selected: new Set(), toggledId: 'foo'});
    });
  });

  describe('toggleSelectAll()', function() {
    describe('when entities are not filtered', function() {
      describe('when all entities are selected', function() {
        it('calls onChange with empty object', function() {
          vm.$onChanges({selected: {currentValue: allSelected}});
          vm.toggleSelectAll();
          expect(onChange).toHaveBeenCalledWith({selected: new Set()});
        });
      });

      describe('when not all entities are selected', function() {
        it('calls onChange with all-selected state', function() {
          vm.$onChanges({selected: {currentValue: new Set(['foo', 'bar'])}});
          vm.toggleSelectAll();
          expect(onChange).toHaveBeenCalledWith({selected: allSelected});
        });
      });
    });

    describe('when entities are filtered', function() {
      describe('when all entities are selected', function() {
        it('calls onChange with filtered IDs removed', function() {
          vm.$onChanges({selected: {currentValue: allSelected}});
          vm.filter = 'b';
          vm.toggleSelectAll();
          expect(onChange).toHaveBeenCalledWith({selected: new Set(['foo'])});
        });
      });

      describe('when not all filtered entities are selected', function() {
        it('calls onChange with filtered entities selected', function() {
          vm.$onChanges({selected: {currentValue: new Set(['baz'])}});
          vm.filter = 'b';
          vm.toggleSelectAll();
          expect(onChange).toHaveBeenCalledWith({selected: new Set(['baz', 'bar'])});
        });
      });
    });
  });
});
