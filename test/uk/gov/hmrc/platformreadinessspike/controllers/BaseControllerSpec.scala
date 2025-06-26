/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.platformreadinessspike.controllers

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.mvc.Results.*
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import uk.gov.hmrc.platformreadinessspike.models.UserAnswers

import scala.concurrent.Future

class BaseControllerSpec extends AnyWordSpec with Matchers with ScalaFutures {

  object TestController extends BaseController(Helpers.stubControllerComponents())

  val testUserAnswers: UserAnswers = UserAnswers("123")

  "withValidJson" should {
    "call f when valid json is passed in" in {
      implicit val fakeRequest: FakeRequest[JsValue] = FakeRequest(
        method = "PUT",
        uri = "/user-answers/123",
        headers = FakeHeaders(Seq.empty),
        body = Json.toJson(testUserAnswers)
      )
      val result               = TestController.withValidJson[UserAnswers](_ => Future.successful(Ok("Succeeded")))
      result.futureValue shouldEqual Ok("Succeeded")
    }

    "return a BadRequest where invalid json is passed in" in {
      implicit val fakeRequest =
        FakeRequest(method = "GET", uri = "/user-answers/123", headers = FakeHeaders(Seq.empty), body = JsString("1234"))
      val result               = TestController.withValidJson[UserAnswers](_ => Future.successful(Ok("Succeeded")))
      result.futureValue shouldEqual BadRequest("Invalid JSON")
    }
  }

}
