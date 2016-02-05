describe('proprietaryConfigEditor', function() {
  'use strict';

  var vm, origElement;

  beforeEach(module('proprietary.configuration.module'));

  beforeEach(inject(function($rootScope, $controller) {
    vm = $controller('proprietary.configuration.editor.controller');
    origElement = angular.element;
  }));

  afterEach(function() {
    angular.element = origElement;
  });

  describe('Validation of Inputs', function() {
    it('Good package inputs', function() {
      expect(vm.validatePackage('com.sonatype')).toEqual({ invalidPrefix : true, wildcards : true });
    });

    //see CLM-1097
    it('Should treat an empty entry as valid', function(){
      expect(vm.validatePackage('')).toEqual({ invalidPrefix : true, wildcards : true });
    });

    it('Bad package inputs', function() {
      expect(vm.validatePackage('com sonatype')).toEqual({ invalidPrefix : false, wildcards : true });
      expect(vm.validatePackage('com/sonatype')).toEqual({ invalidPrefix : false, wildcards : true });
      expect(vm.validatePackage('com.sonatype.')).toEqual({ invalidPrefix : false, wildcards : true });
      expect(vm.validatePackage('.com.sonatype')).toEqual({ invalidPrefix : false, wildcards : true });
      expect(vm.validatePackage('com.sonatype.*')).toEqual({ invalidPrefix : true, wildcards : false });
      expect(vm.validatePackage('com.sonatype.**')).toEqual({ invalidPrefix : true, wildcards : false });
      expect(vm.validatePackage('com.sona*')).toEqual({ invalidPrefix : true, wildcards : false });
      expect(vm.validatePackage('*.sonatype')).toEqual({ invalidPrefix : true, wildcards : false });
    });

    it('Expect field to be cleared on add', function(){
      spyOn(angular, 'element').andReturn({
        controller: function(){
          return {
            $setPristine: function(){}
          };
        }
      });

      vm.component = {
        prefix: 'foo',
        regex: 'bar'
      };
      vm.add({}, 'foo', []);
      expect(vm.component.prefix).toEqual('');
      expect(vm.component.regex).toEqual('');
    });
  });
});
