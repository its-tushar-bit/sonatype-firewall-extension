/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
const accessMockData = {
  getRoleMappings: function () {
    return {
      membersByRole: [
        {
          roleId: '2cb71b3468d649789163ea2e212b5411',
          roleName: 'Test Role',
          roleDescription: 'Evaluates applications and views policy violation summary results.',
          membersByOwner: [
            {
              ownerId: 'asdf',
              ownerName: 'Test App',
              ownerType: 'application',
              members: [
                {
                  type: 'USER',
                  internalName: 'userTest1',
                  displayName: 'User Test1',
                  email: 'userTest1@sonatype.com',
                  realm: 'CLM',
                },
                {
                  type: 'USER',
                  internalName: 'userTest2',
                  displayName: 'User Test2',
                  email: 'userTest2@sonatype.com',
                  realm: 'CLM',
                },
              ],
            },
            {
              ownerId: 'f3c2f4468f1e408b8cb2724ce8c676c2',
              ownerName: 'Org',
              ownerType: 'organization',
              members: [
                {
                  type: 'USER',
                  internalName: 'userTest1',
                  displayName: 'User Test1',
                  email: 'userTest1@sonatype.com',
                  realm: 'CLM',
                },
              ],
            },
            {
              ownerId: 'ROOT_ORGANIZATION_ID',
              ownerName: 'Root Organization',
              ownerType: 'organization',
              members: [],
            },
          ],
        },
      ],
      groupSearchEnabled: true,
    };
  },

  getQueryResults: function () {
    // intentionally includes a user and group with the same internalName and a user who is in the RoleMappings data
    return {
      members: [
        {
          type: 'USER',
          internalName: 'Administrators',
          displayName: 'Administrators a',
          email: 'a@a.com',
          realm: 'IQ Server',
        },
        {
          type: 'USER',
          internalName: 'admin',
          displayName: 'Admin BuiltIn',
          email: 'admin@localhost',
          realm: 'IQ Server',
        },
        {
          type: 'GROUP',
          internalName: 'Administrators',
          displayName: 'Administrators',
          email: null,
          realm: 'asdf',
        },
        {
          type: 'USER',
          internalName: 'userTest1',
          displayName: 'User Test1',
          email: 'userTest1@sonatype.com',
          realm: 'CLM',
        },
      ],
      error: null,
    };
  },

  getMoreRoleMappings: function () {
    var base = accessMockData.getRoleMappings();
    base.membersByRole[1] = {
      roleId: 'abcdef',
      roleName: 'Another Test Role',
      roleDescription: 'Yet another test role.',
      membersByOwner: [
        {
          ownerId: 'asdf',
          ownerName: 'Test App',
          ownerType: 'application',
          members: [],
        },
        {
          ownerId: 'f3c2f4468f1e408b8cb2724ce8c676c2',
          ownerName: 'Org',
          ownerType: 'organization',
          members: [],
        },
        {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
          members: [],
        },
      ],
    };
    return base;
  },
};

export default accessMockData;
