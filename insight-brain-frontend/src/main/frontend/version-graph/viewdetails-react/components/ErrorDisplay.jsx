/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxLoadError } from '@sonatype/react-shared-components';

/**
 * Component for displaying error messages with proper styling and actions
 */
export default function ErrorDisplay({ error, onRetry }) {
  const { status, data, headers } = error;

  return (
    <NxLoadError
      error={<ErrorMessage data={data} status={status} headers={headers} />}
      retryHandler={onRetry}
      titleMessage=""
    />
  );
}

ErrorDisplay.propTypes = {
  error: PropTypes.shape({
    status: PropTypes.number,
    data: PropTypes.any,
    headers: PropTypes.object,
  }).isRequired,
  onRetry: PropTypes.func.isRequired,
};

function ErrorMessage({ data, status, headers = {} }) {
  if (status === 0 || status >= 1000) {
    return 'Network error while contacting server';
  } else if (data && headers['content-type'] && headers['content-type'].indexOf('text/plain') >= 0) {
    return data;
  } else if (status === 401) {
    return `
      Authentication with the Sonatype IQ Server failed. Please verify you supplied the proper credentials in
      the plugin configuration. Once the issue has been resolved, you will need to close and re-open this page.
    `;
  } else if (status === 403) {
    return `
      This component has been associated with an invalid application ID. Please verify your plugin configuration or
      contact your Sonatype IQ administrator to verify your permissions. Once the issue has been resolved, you will need
      to close and re-open this page.
    `;
  } else if (status === 502) {
    return 'Bad Gateway';
  } else if (status === 503) {
    return 'Service Unavailable';
  } else if (status === 504) {
    return 'Gateway Timeout';
  } else {
    return (
      <>
        Unfortunately an error occurred while contacting the Sonatype IQ Server, if the problem persists please contact
        support.
        <br />
        Error {status}
      </>
    );
  }
}

ErrorMessage.propTypes = {
  data: PropTypes.any, // Error data which might be text/plain content
  status: PropTypes.number, // HTTP status code
  headers: PropTypes.object, // Response headers
};
