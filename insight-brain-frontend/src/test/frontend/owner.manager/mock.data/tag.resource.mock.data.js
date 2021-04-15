/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default {
  getApplicationTagUrl: function (orgId) {
    return [
      {
        color: 'black',
        description: 'Description 1',
        id: 'c824e5d5c20d48e4a202dec55e2905cd',
        name: 'Category 1',
        organizationId: orgId || 'f3cea033acf84984ae08d9250db4aa7b',
      },
      {
        color: 'blue',
        description: 'Description 2',
        id: 'cfe4d9c29b9a443d98c7e37669553eab',
        name: 'Category 2',
        organizationId: orgId || 'f3cea033acf84984ae08d9250db4aa7b',
      },
    ];
  },
  getApplicationCategoriesUrl: function (ownerType, ownerId, ownerName) {
    return {
      applicationCategoriesByOwner: [
        {
          ownerId: ownerId || 'orgownerid',
          ownerName: ownerName || 'orgname',
          ownerType: ownerType || 'organization',
          applicationCategories: [
            {
              color: 'black',
              description: 'Description 1',
              id: 'appCategoryId_1',
              name: 'Category 1',
              organizationId: 'orgownerid',
            },
            {
              color: 'black',
              description: 'Description 2',
              id: 'appCategoryId_2',
              name: 'Category 2',
              organizationId: 'orgownerid',
            },
          ],
        },
        {
          ownerId: 'rootorgownerid',
          ownerName: 'rootorgname',
          ownerType: 'organization',
          applicationCategories: [
            {
              color: 'red',
              description: 'Description 3',
              id: 'appCategoryId_3',
              name: 'Category 3',
              organizationId: 'rootorgownerid',
            },
          ],
        },
      ],
    };
  },
  getPolicyTagUrl: function () {
    return [
      {
        id: 'appCategoryId_1',
        ownerId: 'orgownerid',
        name: 'Category 1',
        description: 'Category1',
      },
      {
        id: 'appCategoryId_2',
        ownerId: 'orgownerid',
        name: 'Category 2',
        description: 'Category2',
      },
    ];
  },
  getApplicableOrganizationTags: function (orgId) {
    return [
      {
        color: 'black',
        description: 'Description 1',
        id: 'c824e5d5c20d48e4a202dec55e2905cd',
        name: 'Category 1',
        organizationId: orgId || 'f3cea033acf84984ae08d9250db4aa7b',
      },
      {
        color: 'blue',
        description: 'Description 2',
        id: 'cfe4d9c29b9a443d98c7e37669553eab',
        name: 'Category 2',
        organizationId: orgId || 'f3cea033acf84984ae08d9250db4aa7b',
      },
      {
        color: 'red',
        description: 'Description 2',
        id: 'cfe4d9c29b9a443d98c7e37669553eef',
        name: 'Category 2',
        organizationId: orgId || 'f3cea033acf84984ae08d9250db4aa7b',
      },
    ];
  },
};
