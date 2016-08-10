describe('pathnamesPopover.directive.spec', function() {

  var divElement, scope;

  beforeEach(module('dashboard.utils'));

  beforeEach(inject(function ($rootScope, $compile) {
    jasmine.Clock.useMock();

    scope = $rootScope.$new();
    var page = angular
        .element('<div pathnames-popover="[\'pathname1\', \'pathname2\']">empty div</div>');
    $compile(page)(scope);

    divElement = angular.element(page[0]);

    scope.$digest();
  }));

  afterEach(function () {
    $('.popover').remove();
  });

  it('popover displayed when hovering over div', function () {
    spyOn($.fn, 'popover').andCallThrough();
    spyOn($.fn, 'is').andReturn(true);

    // Mouse enter and hover.
    divElement.mouseover();
    jasmine.Clock.tick(51);
    expect(divElement.popover).toHaveBeenCalledWith('show');

    // Ensure the contents are correct, just the first pathname.
    var popover = $('.popover');
    expect(popover.html()).toContain('pathname1');
    expect(popover.html()).not.toContain('pathname2');

    // Mouse leave.
    // Set the 'is' check to false because leaving the element (div)
    // checks that we are hovering over the popover, which we want to return false.
    $.fn.is.andReturn(false);
    divElement.mouseleave();
    jasmine.Clock.tick(101);
    expect(divElement.popover).toHaveBeenCalledWith('hide');
  });

  it('popover functions modally', function () {
    spyOn($.fn, 'popover').andCallThrough();
    spyOn($.fn, 'is').andReturn(true);

    // Mouse enter and hover.
    divElement.mouseover();
    jasmine.Clock.tick(51);
    expect(divElement.popover).toHaveBeenCalledWith('show');

    // Hover over the popover instead of the element.
    var popover = $('.popover');
    divElement.mouseleave();
    popover.mouseover();
    // Skip forward 101 ms to ensure that the close fires from leaving the element.
    jasmine.Clock.tick(101);
    expect($.fn.is.callCount).toEqual(2);
    // Even though the close event fired the popover is still open because we are hovering over it.
    expect($('.popover').length).toEqual(1);
    // Leave the popover.
    popover.mouseleave();
    expect(divElement.popover).toHaveBeenCalledWith('hide');
  });
});
