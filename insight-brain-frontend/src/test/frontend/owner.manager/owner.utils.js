/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default {
  runTestsForOwnerTypes: function(createTestFunction) {
    describe('Organization', function () {
      createTestFunction('organization', 'OrganizationStore', { id: 'abcd', name: 'My Org' });
    });

    describe('Application', function () {
      createTestFunction('application', 'ApplicationStore', { publicId: 'abcd', id: '0000abcd', name: 'My App' });
    });
  },
  runTestsForAllOwnerTypes: function(createTestFunction) {
    describe('Organization', function () {
      createTestFunction('organization', 'OrganizationStore', { id: 'abcd', name: 'My Org' });
    });

    describe('Application', function () {
      createTestFunction('application', 'ApplicationStore', { publicId: 'abcd', id: '0000abcd', name: 'My App' });
    });

    describe('Repositories', function () {
      createTestFunction('repositories', null, { name: 'Repositories' });
    });
  }
};
