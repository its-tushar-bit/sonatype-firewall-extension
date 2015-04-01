/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import bootstrap._
import assertions._

class LdapLoginSimulation
    extends Simulation
{
  val userCount: Int = Integer.getInteger("userCount", 100)

  val rampTime: Int = Integer.getInteger("rampTime", 60)

  val repeatTimes: Int = Integer.getInteger("repeatTimes", 50)

  val users = csv("users.csv").circular

  val httpConf = httpConfig
      .baseURL("http://localhost:10070")
      .disableCaching

  val login = {
    scenario("Login with LDAP users")
        .feed(users).repeat(repeatTimes) {
      exec(http("login as ${username}").post("/rest/user/session").basicAuth("${username}", "${password}"))
          .exec(http("logout as ${username}").delete("/rest/user/session/logout"))
          .exitHereIfFailed
    }
  }

  setUp(
    login.users(userCount).ramp(rampTime).protocolConfig(httpConf)
  )

  assertThat(global.failedRequests.percent.is(0))
}
