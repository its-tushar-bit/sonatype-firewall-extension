var OwnerUtils = {
  runTestsForOwnerTypes: function(createTestFunction) {
    describe('Organization', function () {
      createTestFunction('organization', 'OrganizationStore', { id : 'abcd', name : 'My Org' });
    });

    describe('Application', function () {
      createTestFunction('application', 'ApplicationStore', { publicId : 'abcd', id : '0000abcd', name : 'My App' });
    });
  }
};
