/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
// eslint-disable-next-line no-unused-vars
var StoreMockData = {
  getOrganizations: function () {
    return [
      {
        id: 'org_1',
        name: 'org_ONE',
        nameLowercaseNoWhitespace: 'org_one',
        parentOrganizationId: 'rootOrg',
      },
      {
        id: 'org_2',
        name: 'org_TWO',
        nameLowercaseNoWhitespace: 'org_two',
        parentOrganizationId: 'rootOrg',
      },
      {
        id: 'rootOrg',
        name: 'Root org',
        nameLowercaseNoWhitespace: 'root_org',
      },
    ];
  },
  newOrganization: function () {
    return {
      id: 'org_3',
      name: 'org_THREE',
      nameLowercaseNoWhitespace: 'org_three',
      parentOrganizationId: 'rootOrg',
    };
  },
  getApplications: function () {
    return [
      {
        id: 'app_10',
        publicId: 'app_public_ten',
        name: 'app_TEN',
        organizationId: 'org_1',
        organizationName: 'org_TEN',
        contact: {
          internalName: 'admin',
          displayName: 'Admin BuiltIn',
          email: 'admin@localhost',
          realm: 'CLM',
          error: null,
        },
      },
      {
        id: 'app_20',
        publicId: 'app_public_twenty',
        name: 'app_TWENTY',
        organizationId: 'org_1',
        organizationName: 'org_ONE',
        contact: null,
      },
      // Application without parent org to simulate user without permission to parent
      {
        id: 'app_30',
        publicId: 'app_public_thirty',
        name: 'app_THIRTY',
        organizationId: 'org_4',
        organizationName: 'org_FOUR',
        contact: null,
      },
    ];
  },
  newApplication: function () {
    return {
      id: 'app_40',
      publicId: 'app_public_forty',
      name: 'app_FORTY',
      organizationId: 'org_2',
      organizationName: 'org_TWO',
      contact: null,
    };
  },
};
