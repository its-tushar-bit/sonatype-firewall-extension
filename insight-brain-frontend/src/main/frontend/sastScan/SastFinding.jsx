/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxCard, NxH3, NxTableCell, NxTableRow, NxThreatIndicator } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { capitalizeFirstLetter, isNilOrEmpty } from 'MainRoot/util/jsUtil';

export default function SastFinding({ finding }) {
  return (
    <NxTableRow>
      <NxTableCell colSpan={4}>
        <SastFindingTitle finding={finding} />
        <div className="iq_sast_scan_finding__content">
          <SastFindingSummary finding={finding} />
          <SastFindingDetails finding={finding} />
        </div>
      </NxTableCell>
    </NxTableRow>
  );
}

function SastFindingTitle({ finding }) {
  return (
    <div className="iq_sast_scan_finding__title">
      <NxThreatIndicator
        className="iq_sast_scan_finding__title-indicator"
        threatLevelCategory={resolveThreatLevelCategory({ finding })}
      />
      <div>
        Severity <strong>{capitalizeFirstLetter(finding.severity)}</strong>
      </div>
      <NxH3 className="nx-h3">{finding.ruleName}</NxH3>
    </div>
  );
}

function SastFindingSummary({ finding }) {
  return (
    <div className="iq_sast_scan_finding__summary">
      <SummaryItem itemName="Coordinate Namespace" itemValue={finding.coordinate.namespace} />
      <SummaryItem itemName="Coordinate Name" itemValue={finding.coordinate.name} />
      <SummaryItem itemName="Coordinate Method Name" itemValue={finding.coordinate.methodName} />
      <SummaryItem itemName="Line Number" itemValue={finding.lineNumber} />
      <SummaryItem itemName="Confidence" itemValue={capitalizeFirstLetter(finding.confidence)} />
    </div>
  );
}

function SastFindingDetails({ finding }) {
  if (isNilOrEmpty(finding.description) && isNilOrEmpty(finding.remediations)) {
    return null;
  }

  return (
    <NxCard.Container>
      <NxCard className="iq_sast_scan_finding__details">
        <NxCard.Header>
          <NxH3>Details</NxH3>
        </NxCard.Header>
        <NxCard.Content className="iq_sast_scan_finding__details__content">
          {!isNilOrEmpty(finding.description) && <DescriptionSection finding={finding} />}
          {!isNilOrEmpty(finding.remediations) && <RemediationSection finding={finding} />}
        </NxCard.Content>
      </NxCard>
    </NxCard.Container>
  );
}

function DescriptionSection({ finding }) {
  let className = 'iq_sast_scan_finding__description';
  if (isNilOrEmpty(finding.remediations)) {
    className += ' iq_sast_scan_finding__description--only';
  }
  return <p className={className}>{finding.description}</p>;
}

function RemediationSection({ finding }) {
  return (
    <p className="iq_sast_scan_finding__remediation">
      {finding.remediations.map((element, index) => {
        return (
          <div key={element.sastRemediationId}>
            <strong>Remediation {index + 1}:</strong> {element.content}
          </div>
        );
      })}
    </p>
  );
}

function SummaryItem({ itemName, itemValue }) {
  if (isNilOrEmpty(itemValue)) {
    return null;
  }

  return (
    <span>
      <strong>{itemName}:</strong> {itemValue}
    </span>
  );
}

function resolveThreatLevelCategory({ finding }) {
  const severity = finding.severity.toLowerCase();
  if (severity === 'unknown') {
    return 'unspecified';
  } else if (severity === 'medium') {
    return 'moderate';
  } else if (severity === 'high') {
    return 'severe';
  }
  return severity;
}

SummaryItem.propTypes = {
  itemName: PropTypes.string.isRequired,
  itemValue: PropTypes.any.isRequired,
};

const sharedPropTypes = {
  finding: PropTypes.object.isRequired,
};

SastFinding.propTypes = sharedPropTypes;
SastFindingTitle.propTypes = sharedPropTypes;
SastFindingSummary.propTypes = sharedPropTypes;
SastFindingDetails.propTypes = sharedPropTypes;
DescriptionSection.propTypes = sharedPropTypes;
RemediationSection.propTypes = sharedPropTypes;
