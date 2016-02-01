describe('component.information.panel.directive', function() {
  var scope, vm;

  beforeEach(module('component.information.panel'));

  beforeEach(inject(function($httpBackend, $compile, $rootScope) {
    $httpBackend.whenGET('cip/component.information.panel.directive.html').respond('');
    scope = $rootScope.$new();

    vm = $compile('<div component-information-panel/>')(scope);
    $httpBackend.flush();
  }));

  afterEach(function() {
    scope.$destroy();
  });

  it('Reset CIP tab', inject(function(SelectedComponent) {
    var component = {
      hash: 'abcd',
      componentIdentifier: {
        name: 'bar'
      }
    };
    expect(scope.vm.selectedTab).toBeFalsy();

    SelectedComponent.toggle(component);
    scope.$digest();

    expect(scope.vm.selectedTab).toEqual(scope.vm.tabs[0]);

    SelectedComponent.toggle(component);
    scope.$digest();

    expect(scope.vm.selectedTab).toBeFalsy();
  }));
});
