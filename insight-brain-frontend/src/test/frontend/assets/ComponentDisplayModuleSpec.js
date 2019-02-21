import componentDisplayModule from '../../../main/frontend/ComponentDisplay/module';

describe('ComponentDisplay', function() {
  var $compile, scope;

  beforeEach(angular.mock.module(componentDisplayModule.name));

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
      var element = $compile(angular.element('<component-display component="component"></component-display>'))(scope);
      scope.$digest();
      expect(element.text().trim().replace(/\s+/g, ' ')).toContain('foo : bar : 1.0');
    });

    it('Can show an element with a filename', function() {
      scope.component = {
        displayName: null,
        filename: 'foo.jar'
      };
      var element = $compile(angular.element('<component-display component="component"></component-display>'))(scope);
      scope.$digest();
      expect(element.text()).toContain('foo.jar');
    });

    it('Can show an element with a list of filenames', function() {
      scope.component = {
        displayName: null,
        filenames: ['foo.jar', 'bar.jar']
      };
      var element = $compile(angular.element('<component-display component="component"></component-display>'))(scope);
      scope.$digest();
      expect(element.text()).toContain('foo.jar, bar.jar');
    });

    it('Can show an element with no identifiers', function() {
      scope.component = { };
      var element = $compile(angular.element('<component-display component="component"></component-display>'))(scope);
      scope.$digest();
      expect(element.text()).toContain('Unknown');
    });

    it('properly adds zero-width space', function() {
      scope.component = {
        displayName: {
          parts: [
            {field: 'Group', value: 'foo.bar'},
            {value: ' : '},
            {field: 'Artifact', value: 'baz'},
            {value: ' : '},
            {field: 'Version', value: '1.0'}
          ]
        },
        pathnames: []
      };
      var element = $compile(angular.element('<component-display component="component"></component-display>'))(scope);
      scope.$digest();

      // the zero-width space character is present in the expected string below, right before "bar"
      expect(element.text().trim()).toBe('foo.​bar : baz : 1.0');
    });
  });
});
