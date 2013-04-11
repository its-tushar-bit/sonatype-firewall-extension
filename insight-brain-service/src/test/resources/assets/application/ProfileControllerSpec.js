describe('ProfileController', function() {
	function toRegExp(getUrl) {
		return new RegExp(getUrl + '\\?timestamp=[0-9]+');
	}

	function getController(controllerName, scope) {
		var controller = null;

		inject(function ($controller, $httpBackend) {
			controller = $controller(controllerName, {$scope: scope});
		});
		return  controller;
	}

	var pageScope,
	    profileScope,
	    pageController,
	    profileController;

    angular.module('ApplicationId',[]).service('ApplicationId', function () {
		return {
			encoded : 'organization'
		};
    });

    beforeEach(module('Profile', 'ApplicationId'));
	beforeEach(inject(function ($rootScope, $httpBackend, CLMLocations) {
		$httpBackend.expectGET(toRegExp(CLMLocations.getProfilesUrl())).respond([{ id : 'profile_01', name : 'Super Profile'}]);
		$httpBackend.expectGET(toRegExp(CLMLocations.getPolicyUrl())).respond([{ id : 'policy_01', name : 'Super Policy'}]);

		pageScope = $rootScope.$new();
		pageController = getController('ProfilePageController', pageScope);

		profileScope = pageScope.$new();
		profileController = getController('ProfileController', profileScope);
		$httpBackend.flush();
	}));

	it('Create New Profile', inject(function ($httpBackend, CLMLocations) {
		pageScope.editProfile();
		profileScope.$digest(); // for $on behaviour
		pageScope.selectedProfile.name = 'A New Profile';
		profileScope.addProfilePolicy(profileScope.policies[0]);

		$httpBackend.expectPOST(toRegExp(CLMLocations.getProfilesUrl())).respond(function () {
			var obj = angular.fromJson(arguments[2]);
			expect(obj.name).toEqual('A New Profile');
			expect(obj.id).toEqual(null);
			return [200, { id : 'profile_02', name : obj.name}, {}];
		});
		$httpBackend.expectPUT(CLMLocations.getApplicationProfilePoliciesUrl('profile_02')).respond(function () {
			var obj = angular.fromJson(arguments[2]);
			expect(obj.length).toEqual(1);
			expect(obj[0]).toEqual('policy_01');
			return [200, obj, {}];
		});
		
		profileScope.submitProfile();
		$httpBackend.flush();
		
		expect(pageScope.profiles.length).toEqual(2);
		expect(pageScope.profiles[1].id).toEqual('profile_02');
		expect(pageScope.profiles[1].name).toEqual('A New Profile');
	}));

	it('Update Existing Profile', inject(function ($httpBackend, CLMLocations) {
		pageScope.editProfile(pageScope.profiles[0]);
		$httpBackend.expectGET(CLMLocations.getApplicationProfilePoliciesUrl('profile_01')).respond([]);
		profileScope.$digest(); // for $on behaviour
		$httpBackend.flush();

		pageScope.selectedProfile.name = 'An Updated Profile';
		profileScope.addProfilePolicy(profileScope.policies[0]);

		$httpBackend.expectPUT(toRegExp(CLMLocations.getProfilesUrl())).respond(function () {
			var obj = angular.fromJson(arguments[2]);
			expect(obj.name).toEqual('An Updated Profile');
			expect(obj.id).toEqual('profile_01');
			return [200, obj, {}];
		});
		$httpBackend.expectPUT(CLMLocations.getApplicationProfilePoliciesUrl('profile_01')).respond(function () {
			var obj = angular.fromJson(arguments[2]);
			expect(obj.length).toEqual(1);
			expect(obj[0]).toEqual('policy_01');
			return [200, obj, {}];
		});

		profileScope.submitProfile();
		$httpBackend.flush();
		
		expect(pageScope.profiles.length).toEqual(1);
		expect(pageScope.profiles[0].id).toEqual('profile_01');
		expect(pageScope.profiles[0].name).toEqual('An Updated Profile');
	}));

	it('Delete Profile', inject(function ($httpBackend, CLMLocations) {
		pageScope.editProfile(pageScope.profiles[0]);
		$httpBackend.expectGET(CLMLocations.getApplicationProfilePoliciesUrl('profile_01')).respond([]);
		profileScope.$digest(); // for $on behaviour
		$httpBackend.flush();

		$httpBackend.expectDELETE(CLMLocations.getDeleteProfileUrl(pageScope.profiles[0])).respond(function () {
			return [200, '', []];
		});

		pageScope.deleteProfile();
		$httpBackend.flush();

		expect(pageScope.profiles.length).toEqual(0);
	}));
});