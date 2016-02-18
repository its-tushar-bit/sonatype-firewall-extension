describe('initial.value.dropdown.selector.directive.spec.js', function() {
  var element,
      $httpBackend;

  beforeEach(module('utility'));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  beforeEach(inject(function($compile, $rootScope, _$httpBackend_) {
    var scope = $rootScope.$new();
    $httpBackend = _$httpBackend_;

    SpecUtil.respondWithTemplate($httpBackend, 'utility/widgets/dropdown.selector.directive.tpl.html');

    scope = angular.extend(scope, {
      testModel: null,
      optionNameParam: 'name',
      options: [{name: 'cherry'}, {name: 'orange'}, {name: 'raspberry'}]
    });

    element = $compile('<form class="clm-form"><dropdown-selector ng-model="testModel" options="options" ' +
        'option-name-param="{{optionNameParam}}"></dropdown-selector></form>')(scope).children();

    $httpBackend.flush();
  }));

  it('Directive adds and removes initial-value class', function() {
    var scope = element.scope(),
        vm = element.isolateScope().vm;

    expect(element.hasClass('initial-value')).toBeTruthy();

    vm.selectItem(vm.options[1]);
    scope.$digest();

    expect(element.find('.selected-item').text()).toEqual(vm.options[1].name);
    expect(element.hasClass('initial-value')).toBeFalsy();
  });
});
