import utilityModule from '../../../../main/frontend/utility/utility.module';
import legacyConfigurationModule from '../../../../main/frontend/LegacyConfigurationModule';

describe('age.in.days.input.directive.spec.js', function() {
  var element,
      scope,
      isolatedScope,
      vm,
      $compile,
      $httpBackend;

  beforeEach(angular.mock.module(utilityModule.name, legacyConfigurationModule.name));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  beforeEach(inject(function(_$compile_, $rootScope, _$httpBackend_) {
    scope = $rootScope.$new();
    $httpBackend = _$httpBackend_;
    $compile = _$compile_;

    scope = angular.extend(scope, {
      ageModel: '3'
    });

    element = $compile('<form name="testform"><age-in-days-input ng-model="ageModel"></age-in-days-input></form>')(scope).children();

    isolatedScope = element.isolateScope();
    vm = isolatedScope.vm;
  }));

  it('Directive formats days into the correct age and parses age into days', function() {
    scope.$digest();
    expect(element.find('input[type="number"]').val()).toEqual('3');

    expect(vm.modifierTypes.length).toBe(4);
    vm.modifierTypes.forEach(function(modifierType) {
      vm.modifier = modifierType.modifier;
      isolatedScope.$digest();
      scope.$digest();
      expect(scope.ageModel).toEqual((3 * vm.modifier).toString());
      expect(element.find('input[type="number"]').val()).toEqual('3');
    });
  });

  it('Directive parses age into correct number of days after changing input', function() {
    expect(scope.ageModel).toEqual('3');
    vm.modifier = 30;
    isolatedScope.$digest();
    element.find('input[type="number"]').val(10).trigger('input');
    expect(element.find('input[type="number"]').val()).toEqual('10');
    expect(scope.ageModel).toEqual('300');
  });

  it('Directive formats days into the correct age after changing model', function() {
    vm.modifier = 30;
    isolatedScope.$digest();
    scope.ageModel = '3000';
    scope.$digest();
    expect(element.find('input[type="number"]').val()).toEqual('100');
    expect(vm.ageInDaysModel).toEqual('3000');
  });
});
