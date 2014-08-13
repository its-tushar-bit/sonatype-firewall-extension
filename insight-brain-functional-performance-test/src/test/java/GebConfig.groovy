/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*
 This is the Geb configuration file.
 
 See: http://www.gebish.org/manual/current/configuration.html
*/
import static org.openqa.selenium.phantomjs.PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY

reportsDir = "target/test-reports/geb"
baseUrl = System.getProperty('geb.build.baseUrl', 'http://abd02-1:8080/')

//enable waitFor behaviour for all 'at' checks
atCheckWaiting = true
waiting {
  timeout = 20    //maximum allowable value for page loads, expected to get smaller as we fix performance problems
  retryInterval = 0.5
}

if (System.getProperty('geb.env', 'firefox') == 'phantom') {
  String phantomJsBinary
  new File('../insight-brain-service/target/phantomjs-maven-plugin').eachFileRecurse { File file ->
    if (file.isFile() && file.name.matches('phantomjs(.exe)?')) {
      phantomJsBinary = file.absolutePath
      return false
    }
  }
  if(!phantomJsBinary) {
    throw new IllegalStateException("Please run phantomjs:install from the insight-brain-service directory first")
  }

  System.setProperty(PHANTOMJS_EXECUTABLE_PATH_PROPERTY, phantomJsBinary)
}


