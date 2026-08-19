/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import PendoService from './PendoService';
import SanitizeUrlService from './SanitizeUrlService';

/**
 * This module exports a singleton instance of PendoService hooked up to the main IQ bundle's SanitizeUrlService.
 * setUrlService must be called on application init in order to specify the `urlService` and get the PendoService
 * initialized
 */
let pendoService = null;

export function setUrlService(urlService) {
  const sanitizeUrlService = new SanitizeUrlService(urlService);
  pendoService = new PendoService(sanitizeUrlService);
}

export { pendoService as default };
