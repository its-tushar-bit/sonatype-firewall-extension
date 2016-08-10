describe('modalHelp.directive.spec', function() {

  var divFoo, divBar, divBaz, scope, modal;

  beforeEach(module('dashboard.utils'));

  beforeEach(inject(function($rootScope, $compile, $modal) {
    modal = $modal;
    spyOn(modal, 'open');

    scope = $rootScope.$new();
    var page = angular.element('<html><script type="text/ng-template" id="foo"><div>Foo</div></script>'
        + '<script type="text/ng-template" id="bar"><div>Bar</div></script>'
        + '<div id="divFoo" modal-help="foo">click</div>'
        + '<div id="divBar" modal-help="bar" modal-help-trigger="mouseover">mouseover</div>'
        + '<div id="divBaz" modal-help="bar" modal-help-trigger="mouseover"'
        + 'modal-help-class="test-class">mouseover</div></html>');
    $compile(page)(scope);

    divFoo = page[2];
    divBar = page[3];
    divBaz = page[4];
  }));

  it('click on foo div to open foo modal', function() {
    expect(divFoo).toBeDefined();

    angular.element(divFoo).click();

    expect(modal.open).toHaveBeenCalled();
  });

  it('mouseover on bar div to open bar modal', function() {
    expect(divBar).toBeDefined();

    angular.element(divBar).mouseover();

    expect(modal.open).toHaveBeenCalled();
  });

  it('mouseover on baz div to open bar modal that has a custom class', function() {
    expect(divBaz).toBeDefined();

    angular.element(divBaz).mouseover();

    expect(modal.open).toHaveBeenCalled()
    expect(modal.open.mostRecentCall.args[0].templateUrl).toBe('bar');
    expect(modal.open.mostRecentCall.args[0].windowClass).toBe('clm-modal test-class');
  });
});
