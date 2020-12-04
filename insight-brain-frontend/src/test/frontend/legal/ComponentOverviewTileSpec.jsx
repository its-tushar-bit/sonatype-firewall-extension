/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import ComponentOverviewTile from '../../../main/frontend/legal/ComponentOverviewTile';

describe('ComponentOverviewTile component', function () {

  let getShallowComponent;

  const minimalProps = {
    licenseLegalMetadata: [
      {
        licenseName: 'License-1.0',
        obligations: [
          {
            'licenseObligationStatus': 0
          },
          {
            'licenseObligationStatus': 0
          }
        ]
      },
      {
        licenseName: 'License-2.0',
        obligations: [
          {
            'licenseObligationStatus': 0
          }
        ]
      }
    ]
  };

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(ComponentOverviewTile, minimalProps);
  });

  it('renders the count of obligations', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find('dd.obligations-count')).toHaveText('3');
  });

  it('renders the licenses', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find('dd.license-names')).toHaveText('License-1.0, License-2.0');
  });
});
