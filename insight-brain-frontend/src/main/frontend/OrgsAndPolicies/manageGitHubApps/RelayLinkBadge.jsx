/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import PropTypes from 'prop-types';
import React from 'react';
import { NxFontAwesomeIcon, NxTooltip } from '@sonatype/react-shared-components';
import { faCheckCircle } from '@fortawesome/pro-solid-svg-icons';
import { faExclamationTriangle, faTimesCircle, faSyncAlt } from '@fortawesome/pro-regular-svg-icons';

/**
 * Mirrors the four-state machine in the backend's RelayLinkState. Strings are part of the
 * wire contract returned by GET /api/v2/githubApp.
 */
export const RELAY_LINK_STATES = Object.freeze({
  OK: 'OK',
  UNREGISTERED: 'UNREGISTERED',
  ERROR: 'ERROR',
  FAILED: 'FAILED',
});

// Mirrors RelayLinkState.MAX_ATTEMPTS on the server. Hard-coded for v1 — see follow-up note
// in the backend design doc about lifting these into config properties.
const MAX_ATTEMPTS = 10;

/**
 * Per-row health badge for the GitHub Apps list. Reads `relayLinkState` and `relayLinkAttempts`
 * from the App's wire payload.
 *
 * <ul>
 *   <li>OK -- subtle "Relay link OK" tooltip on a check icon. Could also render nothing; we
 *       render a tiny icon so admins can confirm the link is healthy at a glance.</li>
 *   <li>UNREGISTERED -- amber "Pending registration"; transient state right after install.</li>
 *   <li>ERROR -- amber "Retrying (N/10)" with attempt counter so the operator can see
 *       progress without tailing logs.</li>
 *   <li>FAILED -- red "Failed -- re-register" with a hint that the manual re-register UI is
 *       the recovery path. The hourly slow-sweep flips this back to ERROR automatically too.</li>
 * </ul>
 */
const RelayLinkBadge = ({ state, attempts }) => {
  // Older servers (pre-relay-link-health) don't return the field at all; show no badge so
  // the row stays clean.
  if (!state) {
    return null;
  }

  switch (state) {
    case RELAY_LINK_STATES.OK:
      return (
        <NxTooltip title="Relay link OK">
          <span className="iq-relay-link-badge iq-relay-link-badge--ok" data-testid="relay-link-badge-ok">
            <NxFontAwesomeIcon icon={faCheckCircle} />
          </span>
        </NxTooltip>
      );
    case RELAY_LINK_STATES.UNREGISTERED:
      return (
        <NxTooltip title="Pending relay registration">
          <span className="nx-counter nx-counter--info" data-testid="relay-link-badge-unregistered">
            <NxFontAwesomeIcon icon={faSyncAlt} /> Pending registration
          </span>
        </NxTooltip>
      );
    case RELAY_LINK_STATES.ERROR: {
      // Clamp to [0, MAX_ATTEMPTS] so a server returning a value outside that range (backend
      // bug, counter underflow, or a future MAX_ATTEMPTS increase deployed before the frontend)
      // doesn't render "Retrying (15/10)" or "Retrying (-1/10)".
      const safeAttempts = Math.max(0, Math.min(Number.isFinite(attempts) ? attempts : 0, MAX_ATTEMPTS));
      return (
        <NxTooltip title="The relay registration failed; the polling cycle will retry automatically.">
          <span className="nx-counter nx-counter--info" data-testid="relay-link-badge-error">
            <NxFontAwesomeIcon icon={faExclamationTriangle} /> Retrying ({safeAttempts}/{MAX_ATTEMPTS})
          </span>
        </NxTooltip>
      );
    }
    case RELAY_LINK_STATES.FAILED:
      return (
        <NxTooltip title="Retry budget exhausted; click Reconfigure to retry, or wait for the hourly recovery sweep.">
          <span className="nx-counter nx-counter--negative" data-testid="relay-link-badge-failed">
            <NxFontAwesomeIcon icon={faTimesCircle} /> Failed — re-register
          </span>
        </NxTooltip>
      );
    default:
      // Unknown state from a future server version; render nothing rather than a misleading
      // badge.
      return null;
  }
};

RelayLinkBadge.propTypes = {
  state: PropTypes.oneOf([
    RELAY_LINK_STATES.OK,
    RELAY_LINK_STATES.UNREGISTERED,
    RELAY_LINK_STATES.ERROR,
    RELAY_LINK_STATES.FAILED,
    null,
  ]),
  attempts: PropTypes.number,
};

RelayLinkBadge.defaultProps = {
  state: null,
  attempts: 0,
};

export default RelayLinkBadge;
