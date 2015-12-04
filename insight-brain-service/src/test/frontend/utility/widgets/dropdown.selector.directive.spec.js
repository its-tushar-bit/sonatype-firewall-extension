describe('dropdown.selector.directive.spec.js', function() {
  var element;

  beforeEach(module('utility'));
  beforeEach(inject(function($compile, $rootScope, $httpBackend) {
    var scope = $rootScope.$new();

    SpecUtil.respondWithTemplate($httpBackend, 'utility/widgets/dropdown.selector.directive.html');

    scope = angular.extend(scope, {
      testModel: null,
      optionNameParam: 'name',
      options: [{name: 'cherry'}, {name: 'orange'}, {name: 'raspberry'}],
      emptyOptionString: null,
      disabled: false
    });

    element = $compile('<dropdown-selector ng-model="testModel" options="options" ng-disabled="disabled" ' +
        'empty-option-string="{{emptyOptionString}}" option-name-param="{{optionNameParam}}"></dropdown-selector>')(scope);

    $httpBackend.flush();
  }));

  it('Directive creates full list of options', function() {
    var scope = element.scope();

    expect(element.find('.selected-item').text()).toEqual('-- None --');

    expect(element.find('.dropdown-menu li').length).toBe(scope.options.length);
    element.find('.dropdown-menu li a').each(function(index) {
      expect(this.text).toEqual(scope.options[index].name);
    });
  });

  it('Directive displays custom empty selection string', function() {
    var scope = element.scope();
    scope.emptyOptionString = 'Nothing is Selected';
    scope.$digest();

    expect(element.find('.selected-item').text()).toEqual('Nothing is Selected');
  });

  it('Directive properly selects items', function() {
    var isolatedScope = element.isolateScope(),
        vm = isolatedScope.vm;

    for (var i = 0; i < vm.options.length; i++) {
      vm.selectItem(vm.options[i]);
      isolatedScope.$apply();

      expect(element.find('.selected-item').text()).toEqual(vm.options[i].name);
    }
  });
});
