/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import classnames from 'classnames';
import { categoryByPolicyThreatLevel } from '@sonatype/react-shared-components/util/threatLevels';
import { capitalizeFirstLetter } from 'MainRoot/util/jsUtil';
import { selectPolicyViolationDetailsDrawer } from '../componentDetailsSelector';

const sectionClasses = classnames('sbom-manager-violation-details', 'sbom-manager-violation-details-popover-section');

export default function SbomManagerViolationDetailsTile() {
  const policyViolationDetailsDrawerState = useSelector(selectPolicyViolationDetailsDrawer);
  const violationDetails = policyViolationDetailsDrawerState.violationDetails;
  const threatLevelCategory = categoryByPolicyThreatLevel[violationDetails.threatLevel];
  const threatLevelClassName = classnames(
    'iq-read-only-data',
    'iq-threat-level',
    `iq-threat-level--${threatLevelCategory}`
  );

  const uiRouterState = useRouterState();

  return (
    <section id="sbom-manager-violation-details-tile" className={sectionClasses}>
      <div className={classnames('nx-grid-row', 'sbom-manager-violations-details-info')}>
        <dl className="nx-form-group iq-read-only nx-grid-col sbom-manager-violation-details__left-details">
          <div className="sbom-manager-violation-details__threat-level">
            <dt id="sbom-manager-violation-details__threat-level">Threat Level</dt>
            <dd aria-labelledby="sbom-manager-violation-details__threat-level" className={threatLevelClassName}>
              {violationDetails.threatLevel}
            </dd>
          </div>
          <div className="sbom-manager-violation-details__policy-type">
            <dt id="sbom-manager-violation-details__policy-type">Policy Type</dt>
            <dd
              aria-labelledby="sbom-manager-violation-details__policy-type"
              className="sbom-manager-read-only-data--horizontal"
            >
              {capitalizeFirstLetter(violationDetails.policyThreatCategory)}
            </dd>
          </div>
        </dl>
      </div>
    </section>
  );
}
