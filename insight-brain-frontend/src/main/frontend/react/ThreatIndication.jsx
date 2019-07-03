import React from 'react';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';

import getPolicyThreatIndicatorLevel from '../util/getPolicyThreatIndicatorLevel';

export default function ThreatIndication({ policyThreatLevel }) {
  const className = classnames('iq-threat-indication', getPolicyThreatIndicatorLevel(policyThreatLevel));

  return <span className={className}/>;
}

ThreatIndication.propTypes = {
  policyThreatLevel: PropTypes.number.isRequired
};
