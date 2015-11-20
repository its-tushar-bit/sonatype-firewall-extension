describe('ltg.threat.level.selector.directive.spec.js', function() {
  var $compile,
      $httpBackend,
      element;

  beforeEach(module('utility'));
  beforeEach(inject(function(_$compile_, $rootScope, _$httpBackend_) {
    var scope = $rootScope.$new();

    $compile = _$compile_;
    $httpBackend = _$httpBackend_;

    SpecUtil.respondWithTemplate($httpBackend, 'utility/widgets/ltg.threat.level.selector.directive.html');

    element = $compile('<ltg-threat-level-selector ng-model="testLevel"></ltg-threat-level-selector>')(scope);
    scope.testLevel = 0;
    $httpBackend.flush();
  }));

  it('Directive creates full list of possible threat levels', function() {
    expect(element.find('.dropdown-menu li').length).toBe(11);
    element.find('.dropdown-menu li a').each(function(index) {
      // We convert classList in an array since it is a DOMTokentList. Jasmine allows array-like objects as of v2.3.4.
      expect(Array.prototype.slice.apply(this.classList, [0])).toContain('threat-level-' + (10 - index));
    });
  });

  it('Directive switches threat levels properly via selectLevel method', function() {
    var isolatedScope = element.isolateScope();

    for (var i = 0; i <= 10; i++) {
      isolatedScope.vm.selectLevel(i);
      isolatedScope.$apply();

      expect(Array.prototype.slice.apply(element.find('a.selected-threat-level').get(0).classList,
          [0])).toContain('threat-level-' + i);
    }
  });
});
