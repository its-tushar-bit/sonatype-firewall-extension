/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import {
  NxTableCell,
  NxTableRow,
  NxThreatIndicator,
  NxTooltip,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import ComponentDisplay from '../../../../main/frontend/ComponentDisplay/ReactComponentDisplay';
import ReportTableRow from '../../../../main/frontend/applicationReport/react/ReportTableRow';

describe('ReportTableRow component', function () {
  let getShallowComponent;

  beforeEach(function () {
    const minimalProps = {
      index: 0,
      component: {
        derivedComponentName: 'Component B',
        policyName: 'Security-High',
        policyThreatLevel: 9,
      },
    };

    getShallowComponent = enzymeUtils.getShallowComponent(
      ReportTableRow,
      minimalProps
    );
  });

  it('renders a NxTableRow', function () {
    const shallowComponent = getShallowComponent();
    expect(shallowComponent).toMatchSelector(NxTableRow);
  });

  it('renders table cell', function () {
    const shallowComponent = getShallowComponent();
    const cell = shallowComponent.find(NxTableCell);
    expect(cell).toExist();
  });

  it('renders minimal properties', function () {
    const props = {
        index: 0,
        component: {
          derivedComponentName: 'Component A',
          policyName: 'None',
          policyThreatLevel: 0,
          derivedDependencyType: 'unknown',
        },
      },
      shallowComponent = getShallowComponent(props),
      rows = shallowComponent.find(NxTableRow),
      cells = rows.find(NxTableCell),
      firstTd = cells.at(0).find(NxTableCell),
      secondTd = cells.at(1).find(NxTableCell),
      thirdTd = cells.at(2).find(NxTableCell);

    assertMinimalProperties(
      rows,
      cells,
      firstTd,
      secondTd,
      thirdTd,
      props.component
    );
  });

  it('renders properties with direct dependency type', function () {
    const props = {
        index: 0,
        component: {
          derivedComponentName: 'Component A',
          policyName: 'None',
          policyThreatLevel: 0,
          derivedDependencyType: 'direct',
        },
      },
      shallowComponent = getShallowComponent(props),
      rows = shallowComponent.find(NxTableRow);

    assertWithDependencyType(rows, props.component, true);
  });

  it('renders properties with transitive dependency type', function () {
    const props = {
        index: 0,
        component: {
          derivedComponentName: 'Component A',
          policyName: 'None',
          policyThreatLevel: 0,
          derivedDependencyType: 'transitive',
        },
      },
      shallowComponent = getShallowComponent(props),
      rows = shallowComponent.find(NxTableRow);

    assertWithDependencyType(rows, props.component, false);
  });

  it('renders properties waived', function () {
    const props = {
        index: 0,
        component: {
          derivedComponentName: 'Component A',
          policyName: 'None',
          policyThreatLevel: 0,
          derivedDependencyType: 'unknown',
          waived: true,
        },
      },
      shallowComponent = getShallowComponent(props),
      rows = shallowComponent.find(NxTableRow),
      cells = rows.find(NxTableCell),
      firstTd = cells.at(0).find(NxTableCell),
      secondTd = cells.at(1).find(NxTableCell),
      thirdTd = cells.at(2).find(NxTableCell);

    assertMinimalProperties(
      rows,
      cells,
      firstTd,
      secondTd,
      thirdTd,
      props.component
    );
    expect(thirdTd.find(NxFontAwesomeIcon)).toExist();
    expect(thirdTd.find('span').first()).toHaveClassName(
      'iq-text-indicator iq-text-indicator--waived iq-pull-right'
    );
    expect(thirdTd.find('span').last()).toHaveText('Waived');
  });

  it('renders properties grandfathered', function () {
    const props = {
        index: 0,
        component: {
          derivedComponentName: 'Component A',
          policyName: 'None',
          policyThreatLevel: 0,
          derivedDependencyType: 'unknown',
          grandfathered: true,
        },
      },
      shallowComponent = getShallowComponent(props),
      rows = shallowComponent.find(NxTableRow),
      cells = rows.find(NxTableCell),
      firstTd = cells.at(0).find(NxTableCell),
      secondTd = cells.at(1).find(NxTableCell),
      thirdTd = cells.at(2).find(NxTableCell);

    assertMinimalProperties(
      rows,
      cells,
      firstTd,
      secondTd,
      thirdTd,
      props.component
    );
    expect(thirdTd.find(NxFontAwesomeIcon)).toExist();
    expect(thirdTd.find('span').first()).toHaveClassName(
      'iq-text-indicator iq-text-indicator--grandfathered iq-pull-right'
    );
    expect(thirdTd.find('span').last()).toHaveText('Grandfathered');
  });

  const assertMinimalProperties = (
    rows,
    cells,
    firstTd,
    secondTd,
    thirdTd,
    component
  ) => {
    expect(rows).toExist();
    expect(cells).toExist();
    expect(firstTd).toExist();
    expect(secondTd).toExist();

    expect(firstTd.find(NxThreatIndicator)).toHaveProp('policyThreatLevel', 0);
    expect(firstTd.find('.nx-threat-number')).toHaveText('0');
    expect(secondTd.find('span').first()).toHaveText('None');
    expect(thirdTd.find(ComponentDisplay)).toHaveProp('component', component);
  };

  const assertWithDependencyType = (rows, component, direct) => {
    const cells = rows.find(NxTableCell),
      firstTd = cells.at(0).find(NxTableCell),
      secondTd = cells.at(1).find(NxTableCell),
      thirdTd = cells.at(2).find(NxTableCell);
    assertMinimalProperties(rows, cells, firstTd, secondTd, thirdTd, component);
    let toolTip = thirdTd.find(NxTooltip);
    if (direct) {
      expect(toolTip).toHaveProp('title', 'Direct Dependency');
      expect(toolTip.find('span').first()).toHaveText('D');
      expect(toolTip.find('.iq-dependency-indicator.direct')).toExist();
    } else {
      expect(toolTip).toHaveProp('title', 'Transitive Dependency');
      expect(toolTip.find('span').first()).toHaveText('T');
      expect(toolTip.find('.iq-dependency-indicator.transitive')).toExist();
    }
  };
});
