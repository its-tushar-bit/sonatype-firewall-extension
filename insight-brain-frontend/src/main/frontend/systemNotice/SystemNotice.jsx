/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';

export default function SystemNotice(props) {
  const { loadConfiguration } = props;
  const { message } = props;

  useEffect(() => {
    loadConfiguration();
  }, []);

  return (
    message && (
      <div id="system-notice" className="iq-alert iq-alert--system-notice">
        {message}
      </div>
    )
  );
}

SystemNotice.propTypes = {
  loadConfiguration: PropTypes.func.isRequired,
  message: PropTypes.string,
};
