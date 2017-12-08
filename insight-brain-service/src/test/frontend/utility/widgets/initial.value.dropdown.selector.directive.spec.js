describe('initial.value.dropdown.selector.directive.spec.js', function() {
  var element;

  beforeEach(module('utility', 'legacyConfiguration'));

  beforeEach(inject(function($compile, $rootScope) {
    var scope = $rootScope.$new();

    scope = angular.extend(scope, {
      testModel: null,
      optionNameParam: 'name',
      options: [{name: 'cherry'}, {name: 'orange'}, {name: 'raspberry'}]
    });

    element = $compile('<form class="clm-form"><dropdown-selector ng-model="testModel" options="options" ' +
        'option-name-param="{{optionNameParam}}"></dropdown-selector></form>')(scope).children();

    scope.$digest();
  }));

  it('Directive adds and removes initial-value class', function() {
    var vm = element.isolateScope().vm;

    expect(element.hasClass('initial-value')).toBeTruthy();

    vm.selectItem(vm.options[1]);

    expect(element.find('.iq-form-select__item').text()).toEqual(vm.options[1].name);
    expect(element.hasClass('initial-value')).toBeFalsy();
  });
});
