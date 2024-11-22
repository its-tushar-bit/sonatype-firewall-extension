/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import {
  selectClaimServerData,
  selectClaimId,
  selectClaimInputFieldsData,
  selectClaimInputFieldsValues,
  selectClaimRequestData,
} from 'MainRoot/componentDetails/claim/claimSelectors';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('claimSelectors', () => {
  let componentDetailsClaim, mockState;

  beforeEach(() => {
    componentDetailsClaim = {
      loading: false,
      loadError: null,
      inputFields: {
        artifactId: initUserInput('artifactId new'),
        classifier: initUserInput('new '),
        extension: initUserInput('extension new'),
        groupId: initUserInput('groupId new'),
        version: initUserInput('23.5'),
        comment: initUserInput('some meaningful text 2'),
        createTime: initUserInput(''),
      },
      serverData: {
        id: '404',
        hash: '44444',
        comment: 'some meaningful text',
        createTime: null,
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'artifactId',
            classifier: '',
            extension: 'extension',
            groupId: 'groupId',
            version: '23.4',
          },
        },
      },
      isDirty: false,
      claimMaskState: null,
      claimError: null,
      validationError: null,
    };

    mockState = {
      router: {
        currentParams: {
          hash: 'some-component-hash',
        },
      },
      applicationReport: {
        selectedReport: {
          allEntries: [
            {
              hash: 'some-component-hash',
            },
          ],
        },
      },
      componentDetailsClaim,
    };
  });

  describe('selectClaimServerData', () => {
    it('selects serverData object from componentDetailsClaim slice', () => {
      const actualSelection = selectClaimServerData(mockState);
      expect(actualSelection).toEqual(componentDetailsClaim.serverData);
    });
  });

  describe('selectClaimId', () => {
    it('selects claim id if claim exists', () => {
      const actualSelection = selectClaimId(mockState);
      expect(actualSelection).toBe('404');
    });

    it('selects undefined if claim does not exist', () => {
      componentDetailsClaim.serverData = null;
      const actualSelection = selectClaimId(mockState);
      expect(actualSelection).toBe(undefined);
    });
  });

  describe('selectClaimInputFieldsData', () => {
    it('selects inputFields object from componentDetailsClaim slice', () => {
      const actualSelection = selectClaimInputFieldsData(mockState);
      expect(actualSelection).toEqual(componentDetailsClaim.inputFields);
    });
  });

  describe('selectClaimInputFieldsValues', () => {
    it('selects inputFields trimmed values as object from componentDetailsClaim slice', () => {
      const expectedSelection = {
        artifactId: 'artifactId new',
        classifier: 'new',
        extension: 'extension new',
        groupId: 'groupId new',
        version: '23.5',
        comment: 'some meaningful text 2',
        createTime: '',
      };

      const actualSelection = selectClaimInputFieldsValues(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });
  });

  describe('selectClaimRequestData', () => {
    it('selects claim requiest data from componentDetailsClaim slice', () => {
      const expectedSelection = {
        hash: 'some-component-hash',
        comment: 'some meaningful text 2',
        createTime: null,
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'artifactId new',
            classifier: 'new',
            extension: 'extension new',
            groupId: 'groupId new',
            version: '23.5',
          },
        },
      };

      const actualSelection = selectClaimRequestData(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });
  });
});
