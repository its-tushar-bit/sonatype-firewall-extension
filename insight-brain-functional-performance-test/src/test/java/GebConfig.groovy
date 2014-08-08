/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*
 This is the Geb configuration file.
 
 See: http://www.gebish.org/manual/current/configuration.html
*/

reportsDir = "target/test-reports/geb"
baseUrl = System.getProperty('geb.build.baseUrl', 'http://abd02-1:8080/')

//enable waitFor behaviour for all 'at' checks
atCheckWaiting = true
waiting {
  timeout = 20    //maximum allowable value for page loads, expected to get smaller as we fix performance problems
  retryInterval = 0.5
}

