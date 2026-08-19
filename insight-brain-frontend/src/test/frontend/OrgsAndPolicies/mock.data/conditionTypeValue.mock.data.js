/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default {
  getConditionValueTypeUrl: function () {
    return [
      {
        id: 'AgeInDaysValueType',
        dataType: 'Integer',
        availableValues: null,
        allowMultiple: false,
      },
      {
        id: 'CoordinatesValueType',
        dataType: 'String',
        availableValues: null,
        allowMultiple: false,
      },
      {
        id: 'FloatValueType',
        dataType: 'Float',
        availableValues: null,
        allowMultiple: false,
      },
      {
        id: 'IntegerValueType',
        dataType: 'Integer',
        availableValues: null,
        allowMultiple: false,
      },
      {
        id: 'IdentificationSourceValueType',
        dataType: 'IdentificationSource',
        availableValues: [
          { id: 'Sonatype', name: 'Sonatype' },
          { id: 'Manual', name: 'Manual' },
        ],
        allowMultiple: false,
      },
      {
        id: 'LabelValueType',
        dataType: 'Label',
        availableValues: [
          {
            id: '6be0f524314245c7aded40b3d4ac8112',
            ownerId: 'b6b265d098db41b7aabb3687a3235be7',
            label: 'App Component Label',
            description: 'Description',
            color: 'light-purple',
          },
          {
            id: 'a8c63510015f4a4fadb52f4cfcd653ef',
            ownerId: 'ROOT_ORGANIZATION_ID',
            label: 'Root Org Label',
            description: 'Bleh Bleh Blah 1',
            color: 'dark-green',
          },
        ],
        allowMultiple: false,
      },
      {
        id: 'LicenseStatusValueType',
        dataType: 'LicenseStatus',
        availableValues: [
          { id: 'OPEN', name: 'Open' },
          { id: 'ACKNOWLEDGED', name: 'Acknowledged' },
          { id: 'OVERRIDDEN', name: 'Overridden' },
          { id: 'SELECTED', name: 'Selected' },
          { id: 'CONFIRMED', name: 'Confirmed' },
        ],
        allowMultiple: false,
      },
      {
        id: 'LicenseThreatGroupValueType',
        dataType: 'LicenseThreatGroup',
        availableValues: [
          {
            id: '7cb1d5c8fe4a4f92893dded54f28d88a',
            ownerId: 'ROOT_ORGANIZATION_ID',
            name: 'Copyleft',
            nameLowercaseNoWhitespace: 'copyleft',
            threatLevel: 9,
          },
          {
            id: 'd341ca90a4ea4971aa84376148892c7d',
            ownerId: 'ROOT_ORGANIZATION_ID',
            name: 'Liberal',
            nameLowercaseNoWhitespace: 'liberal',
            threatLevel: 0,
          },
          {
            id: 'f7905b077e7749de8e83e533c1ccfd80',
            ownerId: 'ROOT_ORGANIZATION_ID',
            name: 'Non Standard',
            nameLowercaseNoWhitespace: 'nonstandard',
            threatLevel: 6,
          },
          {
            id: '5bb92f5c57594742adcdd4270d634e27',
            ownerId: 'ROOT_ORGANIZATION_ID',
            name: 'Weak Copyleft',
            nameLowercaseNoWhitespace: 'weakcopyleft',
            threatLevel: 2,
          },
        ],
        allowMultiple: false,
      },
      {
        id: 'LicenseValueType',
        dataType: 'License',
        availableValues: [
          { id: 'JSON', shortDisplayName: 'JSON', longDisplayName: 'JSON' },
          {
            id: 'LGPL-UNSPECIFIED',
            shortDisplayName: 'LGPL',
            longDisplayName: 'LGPL-Style License Not Identifiable by Sonatype',
          },
          {
            id: 'LGPL-2.0',
            shortDisplayName: 'LGPL-2.0',
            longDisplayName: 'GNU Library General Public License v2 only',
          },
          {
            id: 'LGPL-2.1',
            shortDisplayName: 'LGPL-2.1',
            longDisplayName: 'GNU Lesser General Public License v2.1 only',
          },
          {
            id: 'LGPL-3.0',
            shortDisplayName: 'LGPL-3.0',
            longDisplayName: 'GNU Lesser General Public License v3.0 only',
          },
        ],
        allowMultiple: false,
      },
      {
        id: 'MatchStateValueType',
        dataType: 'MatchState',
        availableValues: [
          { id: 'exact', name: 'Exact' },
          { id: 'similar', name: 'Similar' },
          { id: 'unknown', name: 'Unknown' },
        ],
        allowMultiple: false,
      },
      {
        id: 'PercentageValueType',
        dataType: 'Integer',
        availableValues: null,
        allowMultiple: false,
      },
      {
        id: 'SecurityVulnerabilityStatusValueType',
        dataType: 'SecurityVulnerabilityStatus',
        availableValues: [
          { id: 'OPEN', name: 'Open' },
          { id: 'ACKNOWLEDGED', name: 'Acknowledged' },
          { id: 'NOT_APPLICABLE', name: 'Not Applicable' },
          { id: 'CONFIRMED', name: 'Confirmed' },
        ],
        allowMultiple: false,
      },
    ];
  },
};
