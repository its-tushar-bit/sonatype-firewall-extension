describe('defaultAdminPasswordChangedService', function() {
  beforeEach(module('defaultAdminPasswordChangedServiceModule'));

  describe('shouldDisplayDefaultPasswordWarning', function() {
    var $httpBackend,
        $rootScope,
        defaultAdminPasswordChangedService,
        CLMLocations;

    beforeEach(inject(function(_$httpBackend_, _$rootScope_, _defaultAdminPasswordChangedService_, _CLMLocations_) {
      $httpBackend = _$httpBackend_;
      defaultAdminPasswordChangedService = _defaultAdminPasswordChangedService_;
      $rootScope = _$rootScope_;
      CLMLocations = _CLMLocations_;
    }));

    afterEach(function() {
      $httpBackend.verifyNoOutstandingRequest();
      $httpBackend.verifyNoOutstandingExpectation();
    });

    it('returns a promise resolving to true if the REST API returns the string "true"', function() {
      var success = jasmine.createSpy('success'),
          failure = jasmine.createSpy('failure');

      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning()).respond('true');

      defaultAdminPasswordChangedService.shouldDisplayDefaultPasswordWarning().then(success).catch(failure);

      expect(success).not.toHaveBeenCalled();

      $httpBackend.flush();
      $rootScope.$digest();

      expect(success).toHaveBeenCalledWith(true);
      expect(failure).not.toHaveBeenCalled();
    });

    it('returns a promise resolving to false if the REST API returns a string other than "true"', function() {
      var success = jasmine.createSpy('success'),
          failure = jasmine.createSpy('failure');

      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning()).respond('foo');

      defaultAdminPasswordChangedService.shouldDisplayDefaultPasswordWarning().then(success).catch(failure);

      expect(success).not.toHaveBeenCalled();

      $httpBackend.flush();
      $rootScope.$digest();

      expect(success).toHaveBeenCalledWith(false);
      expect(failure).not.toHaveBeenCalled();
    });

    it('returns a rejected promise if the REST call fails', function() {
      var success = jasmine.createSpy('success'),
          failure = jasmine.createSpy('failure');

      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning()).respond(500, 'Error');

      defaultAdminPasswordChangedService.shouldDisplayDefaultPasswordWarning().then(success).catch(failure);

      $httpBackend.flush();
      $rootScope.$digest();

      expect(success).not.toHaveBeenCalled();
      expect(failure).toHaveBeenCalledWith(jasmine.objectContaining({ data: 'Error' }));
    });

    it('caches the result and does not re-invoke the REST call if called multiple times', function() {
      var success1 = jasmine.createSpy('success1'),
          failure1 = jasmine.createSpy('failure1'),
          success2 = jasmine.createSpy('success2'),
          failure2 = jasmine.createSpy('failure2');

      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning()).respond('true');

      defaultAdminPasswordChangedService.shouldDisplayDefaultPasswordWarning().then(success1).catch(failure1);

      expect(success1).not.toHaveBeenCalled();

      $httpBackend.flush();
      $rootScope.$digest();

      expect(success1).toHaveBeenCalledWith(true);
      expect(failure1).not.toHaveBeenCalled();

      // note that we aren't setting up another expectGET
      defaultAdminPasswordChangedService.shouldDisplayDefaultPasswordWarning().then(success2).catch(failure2);

      $rootScope.$digest();

      expect(success2).toHaveBeenCalledWith(true);
      expect(failure2).not.toHaveBeenCalled();
    });
  });
});
