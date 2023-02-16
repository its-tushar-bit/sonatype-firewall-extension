/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxThreatIndicator } from '@sonatype/react-shared-components';

import * as enzymeUtils from '../enzymeUtils';
import QuarantineComponentOverviewTile from 'MainRoot/quarantinedComponentReport/componentOverviewTile/QuarantineComponentOverviewTile';

describe('QuarantineComponentOverviewTile', () => {
  let minimalProps, getShallowComponent;

  beforeEach(function () {
    minimalProps = {
      componentOverview: {
        componentDisplayName: 'a : b : 1',
        isQuarantined: true,
        quarantinedPolicyViolationsCount: 3,
        quarantinedDate: '2022-01-23T21:29:13.162+0000',
        repositoryName: 'maven-central',
      },
    };

    getShallowComponent = enzymeUtils.getShallowComponent(QuarantineComponentOverviewTile, minimalProps);
  });

  describe('renders a threat level indicator based on the quarantine status', () => {
    it('renders a threat level indicator as critical when the quarantine status is true', function () {
      const threatIndicator = getShallowComponent().find(NxThreatIndicator);
      expect(threatIndicator).toExist();
      expect(threatIndicator).toHaveProp('threatLevelCategory', 'critical');
    });

    it('renders a threat level indicator as none when the quarantine status is not true', function () {
      const threatIndicator = getShallowComponent({
        componentOverview: {
          ...minimalProps.componentOverview,
          isQuarantined: false,
        },
      }).find(NxThreatIndicator);
      expect(threatIndicator).toExist();
      expect(threatIndicator).toHaveProp('threatLevelCategory', 'none');
    });
  });
});
