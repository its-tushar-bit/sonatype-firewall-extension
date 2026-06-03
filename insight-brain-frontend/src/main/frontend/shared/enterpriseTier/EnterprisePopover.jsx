/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useRef, useMemo } from 'react';
import ReactDOM from 'react-dom';
import PropTypes from 'prop-types';
import { useSelector, useDispatch } from 'react-redux';
import { NxTextLink, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faTimes } from '@fortawesome/pro-solid-svg-icons';
import { sendGainsightCustomEvent } from 'MainRoot/util/gainsightUtils';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { selectIsPopoverDismissed } from 'MainRoot/productFeatures/productFeaturesSelectors';

import './EnterprisePopover.scss';

/**
 * Hover-triggered enterprise popover (Pattern D from UX spec).
 * Matches POC design: fixed bottom-right, slide-in animation, portaled to document.body.
 *
 * Dismissal is Redux-ephemeral — the popover re-shows on page refresh.
 *
 * @param {string} featureId - Unique ID for dismissal state + telemetry (e.g., 'constraints')
 */
export default function EnterprisePopover({ content, highlightText, linkText, onLinkClick, featureId, children }) {
  const dispatch = useDispatch();
  const selectDismissed = useMemo(() => (featureId ? selectIsPopoverDismissed(featureId) : () => false), [featureId]);
  const dismissed = useSelector(selectDismissed);
  const [showPopover, setShowPopover] = useState(false);
  const triggerRef = useRef(null);

  const handleMouseEnter = () => {
    if (triggerRef.current && !dismissed) {
      setShowPopover(true);
    }
  };

  const markDismissed = () => {
    if (featureId) {
      dispatch(productFeaturesActions.dismissPopover(featureId));
    }
  };

  const handleClosePopover = () => {
    setShowPopover(false);
    markDismissed();
    if (featureId) {
      sendGainsightCustomEvent('enterprise-upsell-popover-close', { feature: featureId });
    }
  };

  const handleLinkClick = () => {
    setShowPopover(false);
    markDismissed();
    if (featureId) {
      sendGainsightCustomEvent('enterprise-upsell-popover-link-click', { feature: featureId });
    }
    if (onLinkClick) {
      onLinkClick();
    }
  };

  return (
    <div ref={triggerRef} onMouseEnter={handleMouseEnter}>
      {children}
      {showPopover &&
        triggerRef.current &&
        ReactDOM.createPortal(
          <div className="iq-enterprise-popover">
            <button className="iq-enterprise-popover__close" onClick={handleClosePopover} aria-label="Close">
              <NxFontAwesomeIcon icon={faTimes} />
            </button>
            <p className="iq-enterprise-popover__text">
              {highlightText && <span className="iq-enterprise-popover__highlight">{highlightText}</span>} {content}{' '}
              {linkText && <NxTextLink onClick={handleLinkClick}>{linkText}</NxTextLink>}
            </p>
          </div>,
          document.body
        )}
    </div>
  );
}

EnterprisePopover.propTypes = {
  content: PropTypes.string.isRequired,
  highlightText: PropTypes.string,
  linkText: PropTypes.string,
  onLinkClick: PropTypes.func,
  featureId: PropTypes.string,
  children: PropTypes.node.isRequired,
};
