/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import $ from 'jquery';

const escapeHtmlString = str => $('<div/>').text(str).html();
export default escapeHtmlString;
