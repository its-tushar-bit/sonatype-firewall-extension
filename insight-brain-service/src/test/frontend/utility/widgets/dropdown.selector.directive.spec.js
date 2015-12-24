describe('dropdown.selector.directive.spec.js', function() {
  var element,
      $httpBackend;

  beforeEach(module('utility'));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('with Object Options', function() {
    beforeEach(inject(function($compile, $rootScope, _$httpBackend_) {
      var scope = $rootScope.$new();
      $httpBackend = _$httpBackend_;

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

  describe('with String Options', function() {
    beforeEach(inject(function($compile, $rootScope, _$httpBackend_) {
      var scope = $rootScope.$new();
      $httpBackend = _$httpBackend_;

      SpecUtil.respondWithTemplate($httpBackend, 'utility/widgets/dropdown.selector.directive.html');

      scope = angular.extend(scope, {
        testModel: null,
        options: ['cherry', 'orange', 'raspberry']
      });

      element = $compile('<dropdown-selector ng-model="testModel" options="options"></dropdown-selector>')(scope);

      $httpBackend.flush();
    }));

    it('Directive creates full list of string options', function() {
      var scope = element.scope();

      expect(element.find('.selected-item').text()).toEqual('-- None --');

      expect(element.find('.dropdown-menu li').length).toBe(scope.options.length);
      element.find('.dropdown-menu li a').each(function(index) {
        expect(this.text).toEqual(scope.options[index]);
      });
    });

    it('Directive properly selects string options', function() {
      var isolatedScope = element.isolateScope(),
          vm = isolatedScope.vm;

      expect(vm.optionNameParam).toBeUndefined();

      for (var i = 0; i < vm.options.length; i++) {
        vm.selectItem(vm.options[i]);
        isolatedScope.$apply();

        expect(element.find('.selected-item').text()).toEqual(vm.options[i]);
      }
    });
  });
});
