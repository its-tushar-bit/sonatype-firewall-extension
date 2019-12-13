/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default {
  getApplicableLicenseGroupsUrl: function() {
    return {
      licenseThreatGroupsByOwner: [
        {
          licenseThreatGroups: [
            {
              id: '29a73283622245d59fd49cbf963d192e',
              licenses: [
                {
                  licenseId: 'AAL',
                  licenseThreatGroupId: '29a73283622245d59fd49cbf963d192e',
                  ownerId: 'f3cea033acf84984ae08d9250db4aa7b'
                }, {
                  licenseId: 'Adobe',
                  licenseThreatGroupId: '29a73283622245d59fd49cbf963d192e',
                  ownerId: 'f3cea033acf84984ae08d9250db4aa7b'
                }, {
                  licenseId: 'Adobe-AFM',
                  licenseThreatGroupId: '29a73283622245d59fd49cbf963d192e',
                  ownerId: 'f3cea033acf84984ae08d9250db4aa7b'
                }
              ],
              name: 'LTG1',
              threatLevel: 6
            }
          ],
          ownerId: 'f3cea033acf84984ae08d9250db4aa7b',
          ownerName: 'Org1 Heh',
          ownerType: 'organization'
        }
      ]
    };
  }
};
