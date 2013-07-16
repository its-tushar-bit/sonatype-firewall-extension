var clmTimestamp = '';

describe('OrganizationController', function () {
  var scope, httpBackend, rootScope, state, mockOrganization;

  function toRegExp(url) {
    return new RegExp(url + '\\?timestamp=[0-9]+');
  }

  beforeEach(module('OrganizationModule'));
  beforeEach(module(function ($provide) {
    $provide.value('OrganizationId', {
      encoded: function () {
        return '1';
      }
    });
  }));

  beforeEach(inject(function ($httpBackend, $rootScope, $controller, $state, CLMAppLocations) {
    httpBackend = $httpBackend;
    rootScope = $rootScope;

    $state.current.name = 'management.organization';

    var organizationsData = OrganizationMockData.getGETResponse();
    mockOrganization = organizationsData[0];
    httpBackend.whenGET(toRegExp(CLMAppLocations.getEntitiesUrl())).respond(organizationsData);

    scope = $rootScope.$new();
    state = $state;

    $controller('OrganizationController', {
      $scope: scope,
      $state: state
    });

    httpBackend.flush();
  }));

  it('loads organizations.', function () {
    expect(scope.organizations).not.toBeUndefined();
    expect(scope.organizations.length).toEqual(3);
    expect(scope.organizations[0].id).toEqual('1');
    expect(scope.organizations[0].name).toEqual('org1');
    expect(scope.organizations[1].id).toEqual('2');
    expect(scope.organizations[1].name).toEqual('org2');
    expect(scope.organizations[2].id).toEqual('3');
    expect(scope.organizations[2].name).toEqual('org3');
  });

  it('switches organizations.', inject(function ($timeout) {
    expect(scope.selectedOrganization).toEqual(null);
    scope.$apply(function () {
      state.params.organizationId = '2';
    });
    $timeout.flush();
    expect(scope.selectedOrganization).not.toBeUndefined();
    expect(scope.selectedOrganization.name).toEqual('org2');
  }));

  it('switch to new organization', inject(function ($timeout) {
    expect(scope.selectedOrganization).toEqual(null);
    scope.$apply(function () {
      state.params.organizationId = '_new_';
    });
    $timeout.flush();
    expect(scope.selectedOrganization).not.toBeUndefined();
    expect(scope.selectedOrganization.name).toEqual(null);
  }));
});

describe('OrganizationEditorController', function () {
  var scope, httpBackend, rootScope, state, mockOrganization, originalMockOrganization, mockOrganization;

  function toRegExp(getUrl) {
    return new RegExp(getUrl + '\\?timestamp=[0-9]+');
  }

  beforeEach(module('OrganizationModule'));
  beforeEach(inject(function ($httpBackend, $rootScope, $controller, $state, CLMLocations, CLMAppLocations, OrganizationStore) {
    httpBackend = $httpBackend;
    rootScope = $rootScope;

    $state.current.name = 'management.organization';

    var organizationsData = OrganizationMockData.getGETResponse();
    mockOrganization = organizationsData[0];
    httpBackend.whenGET(toRegExp(CLMLocations.getOrganizationsUrl())).respond(organizationsData);

    scope = $rootScope.$new();
    state = $state;

    originalMockOrganization = angular.copy(mockOrganization);

    scope.selectedOrganization = OrganizationStore.create();
    scope.selectedOrganization.$updateOriginal(originalMockOrganization);
    angular.extend(scope.selectedOrganization, mockOrganization);

    scope.organizations = [mockOrganization];

    $controller('OrganizationEditorController', {
      $scope: scope,
      $state: state
    });
  }));

  it('generates an icon', function () {
    scope.generateIcon();

    expect(scope.robotHash).not.toBeUndefined();
    expect(scope.robotHash).not.toEqual('');
    expect(scope.hasRobotSource).toBeTruthy();
  });

  it('checks if the form is dirty', function () {
    var isDirty = scope.isFormDirty();
    expect(isDirty).not.toBeTruthy();

    var originalName = scope.selectedOrganization.name;
    scope.selectedOrganization.name = "newName";
    isDirty = scope.isFormDirty();
    expect(isDirty).toBeTruthy();

    scope.selectedOrganization.name = originalName;
    isDirty = scope.isFormDirty();
    expect(isDirty).not.toBeTruthy();

    scope.generateIcon();
    isDirty = scope.isFormDirty();
    expect(isDirty).toBeTruthy();
  });

  it('cancels edits', function () {
    scope.selectedOrganization.name = "newName";
    scope.generateIcon();

    httpBackend.expectGET('../assets/management.html').respond('<div></div>');
    httpBackend.expectGET('../organization-assets/components/organization-navigator.html').respond('<div></div>');

    scope.cancelClick();

    expect(angular.equals(scope.selectedOrganization, originalMockOrganization)).toBeTruthy();
  });

  it('saves an organization', inject(function (CLMAppLocations) {
    scope.organizationEditor = {};
    scope.organizationEditor.$valid = true;

    scope.selectedOrganization.name = "newName";
    scope.generateIcon();

    httpBackend.expectPUT(toRegExp(CLMAppLocations.getEntitiesUrl())).respond(mockOrganization);

    var hasFormData = window.FormData;
    window.FormData = false;

    scope.saveClick();

    httpBackend.flush();

    window.FormData = hasFormData;
  }));

  it('validates organization name', inject(function () {
    scope.organizationEditor = {
      $valid: true
    };
    scope.selectedOrganization = {
      "id": "4",
      "name": "org4",
    };

    expect(scope.validateName('org1')).toEqual('Name is already in use');
    expect(scope.organizationEditor.$invalid).toBeTruthy();

    expect(scope.validateName('new name')).toBeUndefined();
    expect(scope.organizationEditor.$invalid).toBeFalsy();

    expect(scope.validateName('new  name')).toBeDefined();
    expect(scope.organizationEditor.$invalid).toBeTruthy();

    expect(scope.validateName(' new name')).toBeDefined();
    expect(scope.organizationEditor.$invalid).toBeTruthy();

    expect(scope.validateName('new name ')).toBeDefined();
    expect(scope.organizationEditor.$invalid).toBeTruthy();
  }));

  it('adds an organization', inject(function (CLMLocations, OrganizationStore) {
    scope.$state.params.organizationId = '_new_';
    scope.selectedOrganization = OrganizationStore.create();
    scope.selectedOrganization.name = 'name';
    scope.organizationEditor = {}

    httpBackend.expectPOST(toRegExp(CLMLocations.getOrganizationsUrl()), {
      id: null,
      name: 'name'
    }).respond(OrganizationMockData.getPOSTResponse('name'));

    var hasFormData = window.FormData;
    window.FormData = false;

    scope.saveClick();

    httpBackend.expectGET('../assets/management.html').respond('<div></div>');
    httpBackend.expectGET('../organization-assets/components/organization-navigator.html').respond('<div></div>');
    httpBackend.expectGET('../organization-assets/components/organization-editor.html').respond('<div></div>');
    httpBackend.expectGET('../policy-assets/components/policy/policy.html').respond('<div></div>');

    httpBackend.flush();

    window.FormData = hasFormData;
  }));
});
