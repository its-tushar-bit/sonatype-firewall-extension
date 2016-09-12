describe('ldap.server.list.controller.spec.js', function() {

  beforeEach(module('ldap.module'));

  var vm,
      $rootScope,
      $httpBackend,
      CLMLocations,
      ProductFeatures;

  beforeEach(inject(function(_$rootScope_, _$httpBackend_, $controller, _CLMLocations_, _ProductFeatures_) {
    $rootScope = _$rootScope_;
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    ProductFeatures = _ProductFeatures_;
  }));

  describe('Authorized', function() {

    beforeEach(inject(function($controller) {
      vm = $controller('ldap.server.list.controller', {
        isAuthorized: true
      });
    }));

    afterEach(function() {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    it('Properly loads ldap servers', function() {
      expect(vm.ldapList).toBeUndefined();

      $httpBackend.expectGET(CLMLocations.getLdapConfig()).respond([
        {
          "id": "123",
          "name": "ldap1",
          "nameLowercaseNoWhitespace": "ldap1"
        },
        {
          "id": "456",
          "name": "ldap2",
          "nameLowercaseNoWhitespace": "ldap2"
        }
      ]);
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['multiple-ldap-servers-enabled']);

      $httpBackend.flush();

      expect(vm.ldapList).toBeDefined();
      expect(vm.ldapList.length).toBe(2);
      expect(vm.ldapList[0].id).toBe('123');
      expect(vm.ldapList[0].name).toBe('ldap1');
      expect(vm.ldapList[1].id).toBe('456');
      expect(vm.ldapList[1].name).toBe('ldap2');
      expect(vm.isMultipleLdapServersEnabled()).toBe(true);
    });

    it('Properly loads single ldap server', function() {
      expect(vm.ldapList).toBeUndefined();

      $httpBackend.expectGET(CLMLocations.getLdapConfig()).respond([
        {
          "id": "123",
          "name": "ldap1",
          "nameLowercaseNoWhitespace": "ldap1"
        }
      ]);
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);

      $httpBackend.flush();

      expect(vm.ldapList).toBeDefined();
      expect(vm.ldapList.length).toBe(1);
      expect(vm.ldapList[0].id).toBe('123');
      expect(vm.ldapList[0].name).toBe('ldap1');
      expect(vm.isMultipleLdapServersEnabled()).toBe(false);
    });

    it('fails to load ldap server data', function() {
      $httpBackend.expectGET(CLMLocations.getLdapConfig()).respond(500, "foo");
      $httpBackend.whenGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      $httpBackend.flush();
      expect(vm.error.data).toEqual('foo');

      //make sure reload clears error
      vm.doLoad();
      $httpBackend.expectGET(CLMLocations.getLdapConfig()).respond([]);
      $httpBackend.whenGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      $httpBackend.flush();
      expect(vm.error).toBeFalsy();
    });
  });

  describe('Not authorized', function() {
    beforeEach(inject(function($controller) {
      vm = $controller('ldap.server.list.controller', {
        isAuthorized: false
      });
    }));

    it('Should not trigger HTTP request', function() {
      $httpBackend.verifyNoOutstandingRequest()

      expect(vm.ldapList).toBeUndefined();
    });
  });

});
