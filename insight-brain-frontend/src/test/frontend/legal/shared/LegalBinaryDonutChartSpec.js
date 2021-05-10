/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import { NxBinaryDonutChart, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import LegalBinaryDonutChart from '../../../../main/frontend/legal/shared/LegalBinaryDonutChart';
import { faCheckCircle } from '@fortawesome/pro-solid-svg-icons';

describe('LegalBinaryDonutChart', function () {
  const minimalProps = {
    className: 'parent-class',
    percent: 0,
  };

  const getShallowComponent = enzymeUtils.getShallowComponent(LegalBinaryDonutChart, minimalProps);

  it('displays the donut chart for 0 percent completion', function () {
    const donut = getShallowComponent();
    expect(donut).toMatchSelector(NxBinaryDonutChart);
    expect(donut).toHaveProp('percent', 0);
  });

  it('displays the donut chart for 25 percent completion', function () {
    let donut = getShallowComponent({ percent: 25 });
    expect(donut).toMatchSelector(NxBinaryDonutChart);
    expect(donut).toHaveProp('percent', 25);
  });

  it('displays the donut chart for -25 percent completion', function () {
    const donut = getShallowComponent({ percent: -25 });
    expect(donut).toMatchSelector(NxBinaryDonutChart);
    expect(donut).toHaveProp('percent', -25);
  });

  it('displays the checkmark for 100 percent completion', function () {
    const donut = getShallowComponent({ percent: 100 });
    expect(donut).toMatchSelector(NxFontAwesomeIcon);
    expect(donut).toHaveProp('icon', faCheckCircle);
  });

  it('displays the empty donut for non-numeric percentage', function () {
    const donut = getShallowComponent({ percent: 'not a percent' });
    expect(donut).toMatchSelector(NxBinaryDonutChart);
  });
});
