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
package com.sumologic.sumobot.http_frontend

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model.HttpMethods.{GET, HEAD}
import org.apache.pekko.http.scaladsl.model.headers._
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.apache.pekko.stream.ActorMaterializer
import org.apache.pekko.testkit.TestKit
import com.sumologic.sumobot.test.annotated.SumoBotTestKit
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._

class RoutingHelperTest extends SumoBotTestKit(ActorSystem("RoutingHelperTest"))
  with BeforeAndAfterAll {
  private val origin = "https://www.sumologic.com"
  private val routingHelper = RoutingHelper(origin)

  private val wildcardRoutingHelper = RoutingHelper("*")

  private val singleResponseRoute: PartialFunction[HttpRequest, HttpResponse] = {
    case _: HttpRequest =>
      HttpResponse(entity = "hello!", headers = List(RawHeader("test", "testing")))
  }

  private val singleResponseRouteWithAllowOrigin: PartialFunction[HttpRequest, HttpResponse] = {
    case _: HttpRequest =>
      HttpResponse(entity = "hello!", headers = List(`Access-Control-Allow-Origin`("https://www.example.com"), RawHeader("test", "testing")))
  }

  private val rootRoute: PartialFunction[HttpRequest, HttpResponse] = {
    case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
      HttpResponse(entity = "hello!", headers = List(RawHeader("test", "testing")))

    case HttpRequest(HEAD, Uri.Path("/head"), _, _, _) =>
      HttpResponse(entity = "OK!")
  }

  private val emptyRequest = HttpRequest()
  private val getRootRequest = HttpRequest(GET, Uri("/"))
  private val unknownRequest = HttpRequest(GET, Uri("/invalid"))
  private val headRequest = HttpRequest(HEAD, Uri("/"))
  private val existingHeadRequest = HttpRequest(HEAD, Uri("/head"))

  "RoutingHelper" can {
    "withAllowOriginHeader" should {
      "add new AllowOrigin header" in {
        val headers = routingHelper.withAllowOriginHeader(singleResponseRoute)(emptyRequest).headers

        headers should contain(`Access-Control-Allow-Origin`(origin))
        headers should contain(RawHeader("test", "testing"))
      }

      "replace old AllowOrigin header" in {
        val headers = routingHelper.withAllowOriginHeader(singleResponseRouteWithAllowOrigin)(emptyRequest).headers

        headers should contain(`Access-Control-Allow-Origin`(origin))
        headers should contain(RawHeader("test", "testing"))
      }

      "handle wildcard origin" in {
        val headers = wildcardRoutingHelper.withAllowOriginHeader(singleResponseRoute)(emptyRequest).headers

        headers should contain(`Access-Control-Allow-Origin`.*)
        headers should contain(RawHeader("test", "testing"))
      }
    }

    "withForbiddenFallback" should {
      "not modify valid requests" in {
        val response = routingHelper.withForbiddenFallback(rootRoute)(getRootRequest).entity

        entityToString(response) should be ("hello!")
      }

      "respond with 403" in {
        val response = routingHelper.withForbiddenFallback(rootRoute)(unknownRequest)

        response.status should be (StatusCodes.Forbidden)
        response.headers should not contain RawHeader("test", "testing")
      }
    }

    "withHeadRequests" should {
      "return valid HEAD response" in {
        val response = routingHelper.withHeadRequests(rootRoute)(headRequest)

        response.status should be (StatusCodes.OK)
        entityToString(response.entity).isEmpty should be (true)

        response.headers should contain(RawHeader("test", "testing"))
      }

      "passthrough existing HEAD respones" in {
        val response = routingHelper.withHeadRequests(rootRoute)(existingHeadRequest)

        response.status should be (StatusCodes.OK)
        entityToString(response.entity) should be ("OK!")
      }
    }
  }

  private def entityToString(httpEntity: HttpEntity): String = {
    Await.result(Unmarshal(httpEntity).to[String], 5.seconds)
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
