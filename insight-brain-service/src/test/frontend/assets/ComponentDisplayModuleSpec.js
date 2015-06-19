describe('ComponentDisplay', function() {
  var $compile, scope;

  beforeEach(module('ComponentDisplay', 'ComponentName'));

  beforeEach(inject(function(_$compile_, $rootScope) {
    $compile = _$compile_;
    scope = $rootScope.$new();
  }));
  
  afterEach(function () {
    scope.$destroy();
  });

  describe('componentDisplay', function() {
    it('Can show an element with a displayName', function() {
      scope.component = {
        displayName: {
          parts: [
            {field: 'Group', value: 'foo'},
            {value: ' : '},
            {field: 'Artifact', value: 'bar'},
            {value: ' : '},
            {field: 'Version', value: '1.0'}
          ]
        },
        pathnames: []
      };
      var element = $compile(angular.element("<div component-display component='component'></div>"))(scope);
      scope.$digest();
      expect(element.text()).toContain("foo : bar : 1.0");
    });

    it('Can show an element with a pathname', function() {
      scope.component = {
        displayName: null,
        pathnames: ['foo.jar', 'bar.jar']
      };
      var element = $compile(angular.element("<div component-display component='component'></div>"))(scope);
      scope.$digest();
      expect(element.text()).toContain("foo.jar");
    });

    it('Can show an element with no identifiers', function() {
      scope.component = { };
      var element = $compile(angular.element("<div component-display component='component'></div>"))(scope);
      scope.$digest();
      expect(element.text()).toContain("Unknown");
    });


  });

  describe('ComponentDisplayNameUtil', function() {
    var util;

    beforeEach(function() {
      inject(function($injector) {
        util = $injector.get('ComponentDisplayNameUtil');
      });
    });

    it('Can concatenate displayName into a string', function() {
      expect(util.renderToString({
        parts: [
          {field: 'any', value: 'foo'},
          {value: ' : '},
          {field: 'any', value: 'bar'},
        ]
      })).toBe('foo : bar');
    });
  });
});
