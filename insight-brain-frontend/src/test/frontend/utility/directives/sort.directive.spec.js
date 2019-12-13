/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityDirectivesModule from '../../../../main/frontend/utility/directives/utility.directives.module';

describe('sort.directive.spec.js', function() {
  var vm;

  beforeEach(angular.mock.module(utilityDirectivesModule.name));

  beforeEach(inject(function($controller) {
    vm = $controller('sort.controller');
  }));

  describe('extractSortField()', function() {
    it('strips hyphen and returns the rest of the string', function() {
      expect(vm.extractSortField('foo')).toBe('foo');
      expect(vm.extractSortField('-bar')).toBe('bar');
    });
    it('handles null or undefined', function() {
      expect(vm.extractSortField(null)).toBeFalsy();
      expect(vm.extractSortField(undefined)).toBeFalsy();
    });
  });

  describe('setSort()', function() {
    beforeEach(function() {
      vm.onSortChange = jasmine.createSpy('onSortChange');
    });

    describe('new sort fields differ from current', function() {
      it('calls onSortChange callback with new sort fields', function() {
        vm.sortFields = ['foo', 'bar'];
        vm.setSort(['foo', 'baz']);
        expect(vm.onSortChange).toHaveBeenCalledWith({sortFields: ['foo', 'baz']});
      });
    });

    describe('new sort fields same as current', function() {
      describe('when first sort field is descending', function() {
        it('flips first field to ascending and calls onSortChange callback', function() {
          vm.sortFields = ['-foo', '-bar'];
          vm.setSort(['-foo', '-bar']);
          expect(vm.onSortChange).toHaveBeenCalledWith({sortFields: ['foo', '-bar']});
        });
      });
      describe('when first sort field is ascending', function() {
        it('flips first field to descending and calls onSortChange callback', function() {
          vm.sortFields = ['foo', 'bar'];
          vm.setSort(['foo', 'bar']);
          expect(vm.onSortChange).toHaveBeenCalledWith({sortFields: ['-foo', 'bar']});
        });
      });
    });
  });
});
