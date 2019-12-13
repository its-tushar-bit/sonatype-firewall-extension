/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import componentsModule from '../../../main/frontend/components/module';

describe('iqTreeViewRadioSelect', function() {

  var getVm, onChange;

  var available = [
    {id: 'foo', name: 'Foo'},
    {id: 'bar', name: 'Bar'},
    {id: 'baz', name: 'Baz'},
    {id: null, name: 'NULL'}
  ];

  beforeEach(angular.mock.module(componentsModule.name));

  beforeEach(inject(function($componentController) {
    onChange = jasmine.createSpy('onChange');
    getVm = function(selectedId) {
      return $componentController('iqTreeViewRadioSelect', null, {
        available: available,
        selectedId: selectedId,
        onChange: onChange
      });
    };
  }));

  describe('getSelectedName()', function() {
    it('returns selected option\'s name', function() {
      var vm = getVm('bar');
      expect(vm.getSelectedName()).toBe('Bar');
    });

    it('returns undefined if nothing is selected', function() {
      var vm = getVm();
      expect(vm.getSelectedName()).toBeUndefined();
    });

    it('works with selectedId being null', function() {
      var vm = getVm(null);
      expect(vm.getSelectedName()).toBe('NULL');
    });
  });

  describe('select()', function() {
    it('triggers onChange callback', function() {
      var vm = getVm('foo');
      vm.select('bar');
      expect(onChange).toHaveBeenCalledWith({selected: 'bar'});
    });
  });

  describe('isSelected()', function() {
    it('returns true if provided option id is selected', function() {
      var vm = getVm('foo');
      expect(vm.isSelected('foo')).toBe(true);
    });

    it('returns false if provided option id is not selected', function() {
      var vm = getVm('bar');
      expect(vm.isSelected('foo')).toBe(false);
    });
  });

  describe('shouldShowSelected()', function() {
    it('returns true if an option is selected', function() {
      var vm = getVm('foo');
      expect(vm.shouldShowSelected()).toBe(true);
    });

    it('returns false if no option is selected', function() {
      var vm = getVm();
      expect(vm.shouldShowSelected()).toBe(false);
    });
  });
});
