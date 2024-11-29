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

import com.sumologic.sumobot.core.model.IncomingMessage
import com.sumologic.sumobot.core.util.TimeHelpers
import com.sumologic.sumobot.plugins.BotPlugin

import scala.util.Random


class Beer extends BotPlugin with TimeHelpers {

  override protected def help: String = "I'll voice my opinion about certain beverages when appropriate."

  private val BeerMention = matchText(".*(beer[s]?).*")

  private val BeerPhrases = List(
    "Ohh, what I wouldn't give for a beer.",
    "I'm getting another beer.",
    "Robots are made out of old beer cans.",
    "Ah, Jeez, let's just pray I have the energy to get myself another beer.",
    "Hmmm... okay, but I'll need ten kegs of beer, a continuous tape of \"Louie, Louie,\" and a regulation two-story panty-raid ladder.",
    "Ah, beer. So many choices, and it makes so little difference.",
    "Hey, that was my last beer! You bastard! I'll kill you!"
  )
}
class Beer extends BotPlugin {

  override protected def help: String = "I'll voice my opinion about certain beverages when appropriate."

  private val BeerMention = matchText(".*(beer[s]?).*")

  private var lastChimedIn = 0L

  override protected def receiveIncomingMessage: ReceiveIncomingMessage = {
    case message@IncomingMessage(BeerMention(beer), _, _, _, _, _, _) =>
      if (now - lastChimedIn > 60000 && Random.nextInt(10) < 8) {
        lastChimedIn = now
        message.say(chooseRandom(Beer.BeerPhrases: _*))
      }
  }
}

