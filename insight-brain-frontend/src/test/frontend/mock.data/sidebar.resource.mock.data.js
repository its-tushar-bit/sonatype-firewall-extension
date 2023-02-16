/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
window.SidebarResourceMockData = {
  getOwnerListUrl: function () {
    return {
      topParentOrganizationId: 'ROOT_ORGANIZATION_ID',
      ownersMap: {
        organizationOneID: {
          id: 'organizationOneID',
          name: 'Organization One Name',
          parentOrganizationId: 'ROOT_ORGANIZATION_ID',
          synthetic: false,
          applicationIds: [
            'applicationOnePublicID',
            'applicationTwoPublicID',
            'applicationAnalyticsGateway151ID',
            'applicationAnalyticsGateway150PublicID',
            'applicationAnalyticsGateway161PublicID',
            'applicationAnalyticsGateway220PublicID',
            'applicationZamarchiveWebappPublicID',
          ],
          organizationsIds: [],
        },
        applicationOnePublicID: {
          id: 'applicationOneID',
          publicId: 'applicationOnePublicID',
          organizationId: 'organizationOneID',
          name: 'Application One Name',
        },
        applicationTwoPublicID: {
          id: 'applicationTwoID',
          publicId: 'applicationTwoPublicID',
          organizationId: 'organizationOneID',
          name: 'Application Two Name',
        },
        applicationAnalyticsGateway151ID: {
          id: 'applicationAnalyticsGateway151ID',
          publicId: 'applicationAnalyticsGateway151PublicID',
          organizationId: 'organizationOneID',
          name: 'analytics-gateway-1.5.1',
        },
        applicationAnalyticsGateway150ID: {
          id: 'applicationAnalyticsGateway150ID',
          publicId: 'applicationAnalyticsGateway150PublicID',
          organizationId: 'organizationOneID',
          name: 'analytics-gateway-1.5.0',
        },
        applicationAnalyticsGateway161ID: {
          id: 'applicationAnalyticsGateway161ID',
          publicId: 'applicationAnalyticsGateway161PublicID',
          organizationId: 'organizationOneID',
          name: 'analytics-gateway-1.6.1',
        },
        applicationAnalyticsGateway220ID: {
          id: 'applicationAnalyticsGateway220ID',
          publicId: 'applicationAnalyticsGateway220PublicID',
          organizationId: 'organizationOneID',
          name: 'analytics-gateway-2.2.0',
        },
        applicationZamarchiveWebappPublicID: {
          id: 'zamarchive-webapp',
          publicId: 'applicationZamarchiveWebappPublicID',
          organizationId: 'organizationOneID',
          name: 'zamarchive-webapp',
        },
        applicationThreeID: {
          id: 'applicationThreeID',
          publicId: 'applicationThreePublicID',
          organizationId: 'organizationTwoID',
          name: 'Application Three Name',
        },
        organizationTwoID: {
          id: 'organizationTwoID',
          name: 'Organization Two Name',
          parentOrganizationId: 'ROOT_ORGANIZATION_ID',
          synthetic: true,
          applicationIds: ['applicationThreeID'],
          organizationIds: [],
        },
        ROOT_ORGANIZATION_ID: {
          id: 'ROOT_ORGANIZATION_ID',
          name: 'Root Organization',
          synthetic: false,
          applicationIds: [],
          organizationIds: ['', ''],
        },
      },
      organizations: [
        {
          id: 'applicationThreePublicID',
          name: 'Organization One Name',
          synthetic: false,
          applications: [
            {
              id: 'applicationOneID',
              publicId: 'applicationOnePublicID',
              organizationId: 'organizationOneID',
              name: 'Application One Name',
            },
            {
              id: 'applicationTwoID',
              publicId: 'applicationTwoPublicID',
              organizationId: 'organizationOneID',
              name: 'Application Two Name',
            },
            {
              id: 'applicationAnalyticsGateway151ID',
              publicId: 'applicationAnalyticsGateway151PublicID',
              organizationId: 'organizationOneID',
              name: 'analytics-gateway-1.5.1',
            },
            {
              id: 'applicationAnalyticsGateway150ID',
              publicId: 'applicationAnalyticsGateway150PublicID',
              organizationId: 'organizationOneID',
              name: 'analytics-gateway-1.5.0',
            },
            {
              id: 'applicationAnalyticsGateway161ID',
              publicId: 'applicationAnalyticsGateway161PublicID',
              organizationId: 'organizationOneID',
              name: 'analytics-gateway-1.6.1',
            },
            {
              id: 'applicationAnalyticsGateway220ID',
              publicId: 'applicationAnalyticsGateway220PublicID',
              organizationId: 'organizationOneID',
              name: 'analytics-gateway-2.2.0',
            },
            {
              id: 'zamarchive-webapp',
              publicId: 'applicationZamarchiveWebappPublicID',
              organizationId: 'organizationOneID',
              name: 'zamarchive-webapp',
            },
          ],
        },
        {
          id: 'organizationTwoID',
          name: 'Organization Two Name',
          synthetic: true,
          applications: [
            {
              id: 'applicationThreeID',
              publicId: 'applicationThreePublicID',
              organizationId: 'organizationTwoID',
              name: 'Application Three Name',
            },
          ],
        },
        {
          id: 'ROOT_ORGANIZATION_ID',
          name: 'Root Organization',
          synthetic: false,
          applications: [],
        },
      ],
    };
  },

  getOwnerListUrl_noRoot: function () {
    return {
      organizations: [
        {
          id: 'organizationOneID',
          name: 'Organization One Name',
          synthetic: true,
          applications: [
            {
              id: 'applicationOneID',
              publicId: 'applicationOnePublicID',
              organizationId: 'organizationOneID',
              name: 'Application One Name',
            },
            {
              id: 'applicationTwoID',
              publicId: 'applicationTwoPublicID',
              organizationId: 'organizationOneID',
              name: 'Application Two Name',
            },
          ],
        },
        {
          id: 'nonSynthOrgID',
          name: 'Organization Two Name',
          synthetic: false,
          applications: [
            {
              id: 'applicationThreeID',
              publicId: 'applicationThreePublicID',
              organizationId: 'organizationTwoID',
              name: 'Application Three Name',
            },
          ],
        },
      ],
    };
  },

  getOwnerListUrl_onlySynthetic: function () {
    return {
      organizations: [
        {
          id: 'organizationOneID',
          name: 'Organization One Name',
          synthetic: true,
          applications: [
            {
              id: 'applicationOneID',
              publicId: 'applicationOnePublicID',
              organizationId: 'organizationOneID',
              name: 'Application One Name',
            },
            {
              id: 'applicationTwoID',
              publicId: 'applicationTwoPublicID',
              organizationId: 'organizationOneID',
              name: 'Application Two Name',
            },
          ],
        },
        {
          id: 'organizationTwoID',
          name: 'Organization Two Name',
          synthetic: true,
          applications: [
            {
              id: 'applicationThreeID',
              publicId: 'applicationThreePublicID',
              organizationId: 'organizationTwoID',
              name: 'Application Three Name',
            },
          ],
        },
      ],
    };
  },

  getOwnerDetailsUrl: function () {
    return {
      labels: [],
      licenseThreatGroups: [],
      policies: [],
      roles: [],
      tags: [],
    };
  },
};
