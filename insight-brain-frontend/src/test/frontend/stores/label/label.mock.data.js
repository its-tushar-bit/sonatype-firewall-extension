/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
window.LabelMockData = {
  getApplicableLabels: function() {
    return {
      'labelsByOwner': [
        {
          'ownerId': 'appownerid',
          'ownerName': 'appname',
          'ownerType': 'application',
          'labels': [
            {
              'id': 'applabelid',
              'ownerId': 'appownerid',
              'label': 'AppLabel',
              'color': 'red'
            },
            {
              'id': 'applabelid_01',
              'ownerId': 'appownerid',
              'label': 'AnotherAppLabel',
              'color': 'red'
            }
          ]
        },
        {
          'ownerId': 'orgownerid',
          'ownerName': 'orgname',
          'ownerType': 'organization',
          'labels': [
            {
              'id': 'orglabelid',
              'ownerId': 'orgownerid',
              'label': 'OrgLabel',
              'color': 'red'
            }
          ]
        }
      ]
    };
  }
};
