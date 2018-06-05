window.SidebarResourceMockData = {
  getOwnerListUrl: function() {
    return {
      'organizations': [
        {
          'id': 'organizationOneID',
          'name': 'Organization One Name',
          'synthetic': false,
          'applications': [
            {
              'id': 'applicationOneID',
              'publicId': 'applicationOnePublicID',
              'organizationId': 'organizationOneID',
              'name': 'Application One Name'
            },
            {
              'id': 'applicationTwoID',
              'publicId': 'applicationTwoPublicID',
              'organizationId': 'organizationOneID',
              'name': 'Application Two Name'
            }
          ]
        },
        {
          'id': 'organizationTwoID',
          'name': 'Organization Two Name',
          'synthetic': true,
          'applications': [
            {
              'id': 'applicationThreeID',
              'publicId': 'applicationThreePublicID',
              'organizationId': 'organizationTwoID',
              'name': 'Application Three Name'
            }
          ]
        },
        {
          'id': 'ROOT_ORGANIZATION_ID',
          'name': 'Root Organization',
          'synthetic': false,
          'applications': []
        }
      ]
    };
  },

  getOwnerListUrl_noRoot: function() {
    return {
      'organizations': [
        {
          'id': 'organizationOneID',
          'name': 'Organization One Name',
          'synthetic': true,
          'applications': [
            {
              'id': 'applicationOneID',
              'publicId': 'applicationOnePublicID',
              'organizationId': 'organizationOneID',
              'name': 'Application One Name'
            },
            {
              'id': 'applicationTwoID',
              'publicId': 'applicationTwoPublicID',
              'organizationId': 'organizationOneID',
              'name': 'Application Two Name'
            }
          ]
        },
        {
          'id': 'nonSynthOrgID',
          'name': 'Organization Two Name',
          'synthetic': false,
          'applications': [
            {
              'id': 'applicationThreeID',
              'publicId': 'applicationThreePublicID',
              'organizationId': 'organizationTwoID',
              'name': 'Application Three Name'
            }
          ]
        }
      ]
    };
  },

  getOwnerListUrl_onlySynthetic: function() {
    return {
      'organizations': [
        {
          'id': 'organizationOneID',
          'name': 'Organization One Name',
          'synthetic': true,
          'applications': [
            {
              'id': 'applicationOneID',
              'publicId': 'applicationOnePublicID',
              'organizationId': 'organizationOneID',
              'name': 'Application One Name'
            },
            {
              'id': 'applicationTwoID',
              'publicId': 'applicationTwoPublicID',
              'organizationId': 'organizationOneID',
              'name': 'Application Two Name'
            }
          ]
        },
        {
          'id': 'organizationTwoID',
          'name': 'Organization Two Name',
          'synthetic': true,
          'applications': [
            {
              'id': 'applicationThreeID',
              'publicId': 'applicationThreePublicID',
              'organizationId': 'organizationTwoID',
              'name': 'Application Three Name'
            }
          ]
        }
      ]
    };
  },

  getOwnerDetailsUrl: function() {
    return {
      labels: [],
      licenseThreatGroups: [],
      policies: [],
      roles: [],
      tags: []
    };
  }
};
