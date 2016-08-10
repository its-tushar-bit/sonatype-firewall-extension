describe('sparkline.directive.spec', function() {
  var compile, scope;

  beforeEach(module('dashboard.utils'));

  beforeEach(inject(function($compile, $rootScope) {
    compile = $compile;
    scope = $rootScope.$new();
  }));

  it('sparkline will have class', function() {
    var element = angular.element('<div sparkline></div>');
    element = compile(element)(scope);

    var svg = element.find('svg');
    expect(svg).toBeDefined();
    expect(svg.attr('class')).toBe('chart');
  });

  it('sparkline should render the line and fill for the base color', function() {
    var element = angular.element('<div sparkline data="[0,1,2,1,2]"></div>');
    element = compile(element)(scope);

    // expect each point, plus the 'move to' zero path command, plus each point on the base of the fill
    var fill = element.find('.fill.base');
    expect(fill.attr('d').split(',').length).toBe(9);

    // expect each point, plus the 'move to' zero path command
    var line = element.find('.line.base');
    expect(line.attr('d').split(',').length).toBe(5);
  });

  it('sparkline should render the line and fill for the trailing color', function() {
    var element = angular.element('<div sparkline data="[0,1,2,1,2]"></div>');
    element = compile(element)(scope);

    // expect each point, plus the 'move to' zero path command, plus each point on the base of the fill
    var fill = element.find('.fill.up');
    expect(fill.attr('d').split(',').length).toBe(5);

    // expect each point, plus the 'move to' zero path command
    var line = element.find('.line.up');
    expect(line.attr('d').split(',').length).toBe(3);
  });

  it('sparkline renders trailing colors inverted when inverse is enabled', function() {
    var element = angular.element('<div sparkline data="[0,1,2,1,2]" inverse-green="true"></div>');
    element = compile(element)(scope);

    var fill = element.find('.fill.down');
    expect(fill.length).toBe(0);

    fill = element.find('.fill.up');
    expect(fill.length).toBe(1);
  });
});
