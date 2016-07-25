describe('coordinates.input.directive.spec', function() {
  var scope,
      directiveScope;

  beforeEach(module('owner.manager.module'));

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
    it('groupId only', function() {
      initialize('maven:com.apache.axis')
      expect(directiveScope.vm.coordinates.format).toEqual('maven');
      expect(directiveScope.vm.coordinates.groupId).toEqual('com.apache.axis');
    });

    it('groupId, artifactId', function() {
      initialize('maven:com.apache.axis:axis')
      expect(directiveScope.vm.coordinates.format).toEqual('maven');
      expect(directiveScope.vm.coordinates.groupId).toEqual('com.apache.axis');
      expect(directiveScope.vm.coordinates.artifactId).toEqual('axis');
    });

    it('groupId, artifactId, version', function() {
      initialize('maven:com.apache.axis:axis:1.4')
      expect(directiveScope.vm.coordinates.format).toEqual('maven');
      expect(directiveScope.vm.coordinates.groupId).toEqual('com.apache.axis');
      expect(directiveScope.vm.coordinates.artifactId).toEqual('axis');
      expect(directiveScope.vm.coordinates.version).toEqual('1.4');
    });
  });

  describe('parses a-name coordinates', function() {
    it('name only', function() {
      initialize('a-name:jquery')
      expect(directiveScope.vm.coordinates.format).toEqual('a-name');
      expect(directiveScope.vm.coordinates.name).toEqual('jquery');
    });

    it('name, qualifier', function() {
      initialize('a-name:jquery:min')
      expect(directiveScope.vm.coordinates.format).toEqual('a-name');
      expect(directiveScope.vm.coordinates.name).toEqual('jquery');
      expect(directiveScope.vm.coordinates.qualifier).toEqual('min');
    });

    it('name, qualifier, version', function() {
      initialize('a-name:jquery:min:1.4')
      expect(directiveScope.vm.coordinates.format).toEqual('a-name');
      expect(directiveScope.vm.coordinates.name).toEqual('jquery');
      expect(directiveScope.vm.coordinates.qualifier).toEqual('min');
      expect(directiveScope.vm.coordinates.version).toEqual('1.4');
    });
  });

  describe('serialization', function() {
    it('a-name', function() {
      initialize();

      directiveScope.vm.coordinates.format = 'a-name';
      directiveScope.$apply();
      expect(scope.value).toEqual(undefined);

      // test that we don't simply serialize the format
      directiveScope.vm.coordinates.name = 'jquery';
      directiveScope.$apply();
      directiveScope.vm.coordinates.name = '';
      directiveScope.$apply();
      expect(scope.value).toEqual(undefined);

      directiveScope.vm.coordinates.name = 'jquery';
      directiveScope.$apply();
      expect(scope.value).toEqual('a-name:jquery');

      directiveScope.vm.coordinates.qualifier = 'min';
      directiveScope.$apply();
      expect(scope.value).toEqual('a-name:jquery:min');

      directiveScope.vm.coordinates.version = '1.4';
      directiveScope.$apply();
      expect(scope.value).toEqual('a-name:jquery:min:1.4');
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
      expect(scope.value).toEqual(undefined);


      directiveScope.vm.coordinates.groupId = 'org.apache.axis';
      directiveScope.$apply();
      expect(scope.value).toEqual('maven:org.apache.axis');

      directiveScope.vm.coordinates.artifactId = 'axis';
      directiveScope.$apply();
      expect(scope.value).toEqual('maven:org.apache.axis:axis');

      directiveScope.vm.coordinates.version = '1.4';
      directiveScope.$apply();
      expect(scope.value).toEqual('maven:org.apache.axis:axis:1.4');
    });
  });
});
