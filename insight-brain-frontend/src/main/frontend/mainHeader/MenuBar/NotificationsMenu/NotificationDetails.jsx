/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';

export const NotificationDetails = ({ notification }) => {
  return (
    <div className="iq-dropdown-submenu">
      <div className="iq-dropdown-submenu__title">{notification.summaryText}</div>
      <div
        className="iq-dropdown-submenu__container iq-scrollable"
        dangerouslySetInnerHTML={{ __html: notification.detailHtml }}
      />
    </div>
  );
};

NotificationDetails.propTypes = {
  notification: PropTypes.shape({
    summaryText: PropTypes.string,
    detailHtml: PropTypes.string,
  }),
};
