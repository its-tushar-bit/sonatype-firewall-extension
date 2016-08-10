describe('valueBars.directive.spec', function() {

  var scope;

  beforeEach(module('dashboard.utils'));

  afterEach(function() {
    scope.$destroy();
  });

  describe('Value bar chart with both positive and negative values', function() {
    var element,
        height = 25;
    beforeEach(inject(function($rootScope, $compile) {
      scope = $rootScope.$new();
      scope.barData = [-50, 0, 50];
      element = $compile(angular.element('<div value-bars data="barData"></div>'))(scope);
    }));

    it('creates an SVG element based on the data', function() {
      expect(element.find('svg')).toBeTruthy();
    });

    it('creates a bar for each of the data points', function() {
      expect(element.find('svg').find('rect').length).toBe(scope.barData.length);
    });

    it('sets the correct style and size for values below zero', function() {
      var negativeValue = angular.element(element.find('svg').find('rect')[0]);
      expect(negativeValue.attr('class')).toBe('bar down');
      expect(negativeValue.attr('height')).toEqual('' + height / 2); //half of chart below zero
      expect(negativeValue.attr('y')).toEqual('' + height / 2);  //starts in the middle between high/low
    });

    it('sets the correct style and size for zero values', function() {
      var zero = angular.element(element.find('svg').find('rect')[1]);
      expect(zero.attr('class')).toBe('bar down');
      expect(zero.attr('height')).toBe('0'); //no height
      expect(zero.attr('y')).toEqual('' + height / 2);  //starts in the middle
    });

    it('sets the correct style and size for positive values', function() {
      var positiveValue = angular.element(element.find('svg').find('rect')[2]);
      expect(positiveValue.attr('class')).toBe('bar up');
      expect(positiveValue.attr('height')).toEqual('' + height / 2); //half of chart above zero
      expect(positiveValue.attr('y')).toBe('0');  //starts at the top
    });

    it('sets the correct width for the baseline', function() {
      var first = angular.element(element.find('svg').find('rect')[0]);
      var last = angular.element(element.find('svg').find('rect')[scope.barData.length - 1]);
      var baseline = angular.element(element.find('svg').find('line')[0]);
      expect(baseline.attr('x1')).toEqual(first.attr('x'));
      expect(baseline.attr('x2')).toEqual((parseInt(last.attr('x')) + parseInt(last.attr('width'))).toFixed());
    });

  });

  describe('Value bar chart with only positive values', function() {
    var element,
        height = 25;
    beforeEach(inject(function($rootScope, $compile) {
      scope = $rootScope.$new();
      scope.barData = [0, 25, 50];
      element = $compile(angular.element('<div value-bars data="barData"></div>'))(scope);
    }));

    it('sets the correct style and size for zero values', function() {
      var zero = angular.element(element.find('svg').find('rect')[0]);
      expect(zero.attr('class')).toBe('bar down');
      expect(zero.attr('height')).toBe('0'); //no height
      expect(parseFloat(zero.attr('y'))).toBe(height - 0.5); //baseline is fudged so it doesn't render outside the svg element
    });

    it('sets the correct style and size for intermediate positive value', function() {
      var positiveValue = angular.element(element.find('svg').find('rect')[1]);
      expect(positiveValue.attr('class')).toBe('bar up');
      expect(positiveValue.attr('height')).toBe('' + height / 2); //entire height
      expect(positiveValue.attr('y')).toBe('' + height / 2);  //starts in the middle
    });

    it('sets the correct style and size for maximum positive value', function() {
      var positiveValue = angular.element(element.find('svg').find('rect')[2]);
      expect(positiveValue.attr('class')).toBe('bar up');
      expect(positiveValue.attr('height')).toBe('' + height); //entire height
      expect(positiveValue.attr('y')).toBe('0');  //starts at the top
    });

  });
});
