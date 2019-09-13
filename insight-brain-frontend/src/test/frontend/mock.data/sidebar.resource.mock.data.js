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
            },
            {
              'id': 'applicationAnalyticsGateway151ID',
              'publicId': 'applicationAnalyticsGateway151PublicID',
              'organizationId': 'organizationOneID',
              'name': 'analytics-gateway-1.5.1'
            },
            {
              'id': 'applicationAnalyticsGateway150ID',
              'publicId': 'applicationAnalyticsGateway150PublicID',
              'organizationId': 'organizationOneID',
              'name': 'analytics-gateway-1.5.0'
            },
            {
              'id': 'applicationAnalyticsGateway161ID',
              'publicId': 'applicationAnalyticsGateway161PublicID',
              'organizationId': 'organizationOneID',
              'name': 'analytics-gateway-1.6.1'
            },
            {
              'id': 'applicationAnalyticsGateway220ID',
              'publicId': 'applicationAnalyticsGateway220PublicID',
              'organizationId': 'organizationOneID',
              'name': 'analytics-gateway-2.2.0'
            },
            {
              'id': 'zamarchive-webapp',
              'publicId': 'applicationAnalyticsGateway220PublicID',
              'organizationId': 'organizationOneID',
              'name': 'zamarchive-webapp'
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
