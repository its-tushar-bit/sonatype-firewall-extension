describe('ComponentDisplay', function() {
  var $compile, scope;

  beforeEach(module('ComponentDisplay'));

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

    describe('renderToString()', function() {
      it('concatenates displayName into a string', function() {
        expect(util.renderToString({
          parts: [
            {field: 'any', value: 'foo'},
            {value: ' : '},
            {field: 'any', value: 'bar'}
          ]
        })).toBe('foo : bar');
      });
    });
    
    describe('deriveComponentName()', function() {
      it('renders displayName to string if displayName available', function() {
        var component = {
          displayName: {
            parts: [
              {field: 'any', value: 'foo'},
              {value: ' : '},
              {field: 'any', value: 'bar'}
            ]
          }
        };
        expect(util.deriveComponentName(component)).toBe('foo : bar');
      });

      it('uses first entry in pathnames if displayName is not available', function() {
        var component = {
          pathnames: [
              'path/to/foo.jar',
              'path/to/bar.jar'
          ]
        };
        expect(util.deriveComponentName(component)).toBe('foo.jar');
      });

      it('returns "Unknown" if nor displayName neither pathnames are available', function() {
        expect(util.deriveComponentName({})).toBe('Unknown');
      });
    });
    
  });

  describe('periodDelimiter Filter', function() {
    it('properly adds zero-width space', inject(function($filter) {
      var periodDelimiterFilter = $filter('periodDelimiter'),
          zeroWidthSpace = '%E2%80%8B';

      expect(periodDelimiterFilter).toBeDefined();
      expect(encodeURI(periodDelimiterFilter('org.apache.geronimo.framework:geronimo-security:2.1'))).toEqual(
          'org.' + zeroWidthSpace + 'apache.' + zeroWidthSpace + 'geronimo.' + zeroWidthSpace +
          'framework:geronimo-security:2.1');
    }));
  })
});
