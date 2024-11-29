/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sumologic.sumobot.plugins.beer

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestProbe
import com.sumologic.sumobot.brain.InMemoryBrain
import com.sumologic.sumobot.core.model.OutgoingMessage
import com.sumologic.sumobot.plugins.BotPlugin.InitializePlugin
import com.sumologic.sumobot.test.BotPluginTestKit
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import scala.concurrent.duration._

class BeerTest
  extends BotPluginTestKit(ActorSystem("BeerTest")) {

  val beerRef = system.actorOf(Props(classOf[Beer]), "beer")
  val brainRef = system.actorOf(Props(classOf[InMemoryBrain]), "brain")
  beerRef ! InitializePlugin(null, beerRef, null)

  "beer" should {
    "return one of specified beer responses" in {
      val otherPlugin = TestProbe()
      system.eventStream.subscribe(otherPlugin.ref, classOf[OutgoingMessage])
      send(instantMessage("Some beer would be great right about now"))
      eventually(Timeout(5.seconds)) {
        val messages = otherPlugin.expectMsgAllClassOf(classOf[OutgoingMessage])
        messages.foreach(msg => println(msg.text))
        messages.exists(m => Beer.BeerPhrases.contains(m.text))
      }
    }
  }
}