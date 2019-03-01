import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('coordinates.input.directive.spec', function() {
  var scope,
      directiveScope;

  beforeEach(angular.mock.module(ownerManagerModule.name));

  function initialize(value) {
    inject(function($compile, $rootScope) {
      scope = $rootScope.$new();
      scope.value = value;
      $compile('<coordinates-input value="value">')(scope);

      directiveScope = scope.$$childHead;
      directiveScope.$apply(); // triggers watch etc.
    });
  }

  afterEach(function() {
    scope.$destroy();
  });

  it('new constraint defaults to maven', function() {
    initialize();
    expect(directiveScope.vm.coordinates.format).toEqual('maven');
  });

  describe('parses maven coordinates', function() {
    it('default', function() {
      initialize('maven');
      expect(directiveScope.vm.coordinates.format).toEqual('maven');
      expect(directiveScope.vm.coordinates.groupId).toBeUndefined();
      expect(directiveScope.vm.coordinates.artifactId).toBeUndefined();
      expect(directiveScope.vm.coordinates.version).toBeUndefined();
      expect(directiveScope.vm.coordinates.extension).toEqual('*');
      expect(directiveScope.vm.coordinates.classifier).toEqual('*');
    });

    it('groupId, artifactId, version, extension specific values', function() {
      initialize('maven:com.apache.axis:axis:1.4:jar:');
      expect(directiveScope.vm.coordinates.format).toEqual('maven');
      expect(directiveScope.vm.coordinates.groupId).toEqual('com.apache.axis');
      expect(directiveScope.vm.coordinates.artifactId).toEqual('axis');
      expect(directiveScope.vm.coordinates.version).toEqual('1.4');
      expect(directiveScope.vm.coordinates.extension).toEqual('jar');
      expect(directiveScope.vm.coordinates.classifier).toEqual('');
    });

    it('groupId, artifactId, version, extension wildcard values', function() {
      initialize('maven:*:*:*:*:');
      expect(directiveScope.vm.coordinates.format).toEqual('maven');
      expect(directiveScope.vm.coordinates.groupId).toEqual('*');
      expect(directiveScope.vm.coordinates.artifactId).toEqual('*');
      expect(directiveScope.vm.coordinates.version).toEqual('*');
      expect(directiveScope.vm.coordinates.extension).toEqual('*');
      expect(directiveScope.vm.coordinates.classifier).toEqual('');
    });

    it('groupId, artifactId, version, extension, classifier specific values', function() {
      initialize('maven:com.apache.axis:axis:1.4:jar:docs');
      expect(directiveScope.vm.coordinates.format).toEqual('maven');
      expect(directiveScope.vm.coordinates.groupId).toEqual('com.apache.axis');
      expect(directiveScope.vm.coordinates.artifactId).toEqual('axis');
      expect(directiveScope.vm.coordinates.version).toEqual('1.4');
      expect(directiveScope.vm.coordinates.extension).toEqual('jar');
      expect(directiveScope.vm.coordinates.classifier).toEqual('docs');
    });

    it('groupId, artifactId, version, extension, classifier wildcard values', function() {
      initialize('maven:*:*:*:*:*');
      expect(directiveScope.vm.coordinates.format).toEqual('maven');
      expect(directiveScope.vm.coordinates.groupId).toEqual('*');
      expect(directiveScope.vm.coordinates.artifactId).toEqual('*');
      expect(directiveScope.vm.coordinates.version).toEqual('*');
      expect(directiveScope.vm.coordinates.extension).toEqual('*');
      expect(directiveScope.vm.coordinates.classifier).toEqual('*');
    });
  });

  describe('parses a-name coordinates', function() {
    it('default', function() {
      initialize('a-name');
      expect(directiveScope.vm.coordinates.format).toEqual('a-name');
      expect(directiveScope.vm.coordinates.name).toBeUndefined();
      expect(directiveScope.vm.coordinates.qualifier).toEqual('*');
      expect(directiveScope.vm.coordinates.version).toBeUndefined();
    });

    it('name, version specific values', function() {
      initialize('a-name:jquery::1.4');
      expect(directiveScope.vm.coordinates.format).toEqual('a-name');
      expect(directiveScope.vm.coordinates.name).toEqual('jquery');
      expect(directiveScope.vm.coordinates.qualifier).toEqual('');
      expect(directiveScope.vm.coordinates.version).toEqual('1.4');
    });

    it('name, version wildcard values', function() {
      initialize('a-name:*::*');
      expect(directiveScope.vm.coordinates.format).toEqual('a-name');
      expect(directiveScope.vm.coordinates.name).toEqual('*');
      expect(directiveScope.vm.coordinates.qualifier).toEqual('');
      expect(directiveScope.vm.coordinates.version).toEqual('*');
    });

    it('name, qualifier, version specific values', function() {
      initialize('a-name:jquery:min:1.4');
      expect(directiveScope.vm.coordinates.format).toEqual('a-name');
      expect(directiveScope.vm.coordinates.name).toEqual('jquery');
      expect(directiveScope.vm.coordinates.qualifier).toEqual('min');
      expect(directiveScope.vm.coordinates.version).toEqual('1.4');
    });

    it('name, qualifier, version wildcard values', function() {
      initialize('a-name:*:*:*');
      expect(directiveScope.vm.coordinates.format).toEqual('a-name');
      expect(directiveScope.vm.coordinates.name).toEqual('*');
      expect(directiveScope.vm.coordinates.qualifier).toEqual('*');
      expect(directiveScope.vm.coordinates.version).toEqual('*');
    });
  });

  describe('parses pypi coordinates', function() {
    it('default', function() {
      initialize('pypi');
      expect(directiveScope.vm.coordinates.format).toEqual('pypi');
      expect(directiveScope.vm.coordinates.name).toBeUndefined();
      expect(directiveScope.vm.coordinates.version).toBeUndefined();
      expect(directiveScope.vm.coordinates.qualifier).toEqual('*');
      expect(directiveScope.vm.coordinates.extension).toEqual('*');
    });

    it('name, version, extension specific values', function() {
      initialize('pypi:MarkupSafe:1.1.0::tar.gz');
      expect(directiveScope.vm.coordinates.format).toEqual('pypi');
      expect(directiveScope.vm.coordinates.name).toEqual('MarkupSafe');
      expect(directiveScope.vm.coordinates.version).toEqual('1.1.0');
      expect(directiveScope.vm.coordinates.qualifier).toEqual('');
      expect(directiveScope.vm.coordinates.extension).toEqual('tar.gz');
    });

    it('name, version, extension wildcard values', function() {
      initialize('pypi:*:*::*');
      expect(directiveScope.vm.coordinates.format).toEqual('pypi');
      expect(directiveScope.vm.coordinates.name).toEqual('*');
      expect(directiveScope.vm.coordinates.version).toEqual('*');
      expect(directiveScope.vm.coordinates.qualifier).toEqual('');
      expect(directiveScope.vm.coordinates.extension).toEqual('*');
    });

    it('name, version, qualifier, extension specific values', function() {
      initialize('pypi:MarkupSafe:1.1.0:cp37:tar.gz');
      expect(directiveScope.vm.coordinates.format).toEqual('pypi');
      expect(directiveScope.vm.coordinates.name).toEqual('MarkupSafe');
      expect(directiveScope.vm.coordinates.version).toEqual('1.1.0');
      expect(directiveScope.vm.coordinates.qualifier).toEqual('cp37');
      expect(directiveScope.vm.coordinates.extension).toEqual('tar.gz');
    });

    it('name, version, qualifier, extension wildcard values', function() {
      initialize('pypi:*:*:*:*');
      expect(directiveScope.vm.coordinates.format).toEqual('pypi');
      expect(directiveScope.vm.coordinates.name).toEqual('*');
      expect(directiveScope.vm.coordinates.version).toEqual('*');
      expect(directiveScope.vm.coordinates.qualifier).toEqual('*');
      expect(directiveScope.vm.coordinates.extension).toEqual('*');
    });
  });

  describe('serialization', function() {
    it('a-name', function() {
      initialize();

      directiveScope.vm.coordinates.format = 'a-name';
      directiveScope.$apply();
      expect(scope.value).toBeUndefined();

      // test that we don't simply serialize the format
      directiveScope.vm.coordinates.name = 'jquery';
      directiveScope.$apply();
      directiveScope.vm.coordinates.name = '';
      directiveScope.$apply();
      expect(scope.value).toEqual('a-name::*:');

      directiveScope.vm.coordinates.name = 'jquery';
      directiveScope.$apply();
      expect(scope.value).toEqual('a-name:jquery:*:');

      directiveScope.vm.coordinates.qualifier = 'min';
      directiveScope.$apply();
      expect(scope.value).toEqual('a-name:jquery:min:');

      directiveScope.vm.coordinates.version = '1.4';
      directiveScope.$apply();
      expect(scope.value).toEqual('a-name:jquery:min:1.4');
    });

    it('pypi', function() {
      initialize();

      directiveScope.vm.coordinates.format = 'pypi';
      directiveScope.$apply();
      expect(scope.value).toBeUndefined();

      // test that we don't simply serialize the format
      directiveScope.vm.coordinates.name = 'MarkupSafe';
      directiveScope.$apply();
      directiveScope.vm.coordinates.name = '';
      directiveScope.$apply();
      expect(scope.value).toEqual('pypi:::*:*');

      directiveScope.vm.coordinates.name = 'MarkupSafe';
      directiveScope.$apply();
      expect(scope.value).toEqual('pypi:MarkupSafe::*:*');

      directiveScope.vm.coordinates.version = '1.1.0';
      directiveScope.$apply();
      expect(scope.value).toEqual('pypi:MarkupSafe:1.1.0:*:*');

      directiveScope.vm.coordinates.extension = 'tar.gz';
      directiveScope.$apply();
      expect(scope.value).toEqual('pypi:MarkupSafe:1.1.0:*:tar.gz');

      directiveScope.vm.coordinates.qualifier = 'cp37';
      directiveScope.$apply();
      expect(scope.value).toEqual('pypi:MarkupSafe:1.1.0:cp37:tar.gz');
    });

    it('maven', function() {
      initialize();

      directiveScope.vm.coordinates.format = 'maven';
      directiveScope.$apply();
      expect(scope.value).toEqual(undefined);

      // test that we don't simply serialize the format
      directiveScope.vm.coordinates.groupId = 'ggg';
      directiveScope.$apply();
      directiveScope.vm.coordinates.groupId = '';
      directiveScope.$apply();
      expect(scope.value).toEqual('maven::::*:*');

      directiveScope.vm.coordinates.groupId = 'org.apache.axis';
      directiveScope.$apply();
      expect(scope.value).toEqual('maven:org.apache.axis:::*:*');

      directiveScope.vm.coordinates.artifactId = 'axis';
      directiveScope.$apply();
      expect(scope.value).toEqual('maven:org.apache.axis:axis::*:*');

      directiveScope.vm.coordinates.version = '1.4';
      directiveScope.$apply();
      expect(scope.value).toEqual('maven:org.apache.axis:axis:1.4:*:*');
    });
  });
});
