describe('copied.tooltip.directive.spec.js', function() {
  var element,
      scope;

  beforeEach(module('utility.directives'));

  beforeEach(inject(function($rootScope, $compile) {
    scope = $rootScope.$new();
    element = $compile('<div copied-tooltip="tooltip"></div>')(scope);
    spyOn($.fn, 'tooltip').andReturn(element);
  }));

  it('tooltip is not shown initially', function() {
    expect($.fn.tooltip).not.toHaveBeenCalled();
  });

  it('tooltip is shown when showTooltip is called', function() {
    expect(scope.tooltip).toBeDefined();
    expect(scope.tooltip.showTooltip).toBeDefined();

    scope.tooltip.showTooltip();
    expect($.fn.tooltip.calls.length).toBe(2);
    expect($.fn.tooltip).toHaveBeenCalledWith('show');
  });

  it('tooltip is removed on mouseleave', function() {
    scope.tooltip.showTooltip();
    element.trigger('mouseleave');
    expect($.fn.tooltip).toHaveBeenCalledWith('destroy');
  });
});
