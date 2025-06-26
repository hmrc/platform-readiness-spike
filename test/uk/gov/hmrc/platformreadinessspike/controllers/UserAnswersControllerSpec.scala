/*
 * Copyright 2025 HM Revenue & Customs
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

import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.*
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import uk.gov.hmrc.platformreadinessspike.models.UserAnswers
import uk.gov.hmrc.platformreadinessspike.repositories.SessionRepository

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserAnswersControllerSpec
  extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockSessionRepository)
  }

  val mockSessionRepository = mock[SessionRepository]

  private val fakeRequest = FakeRequest("GET", "/")
  private val controller = new UserAnswersController(mockSessionRepository,Helpers.stubControllerComponents())
  val testUserAnswers = UserAnswers("123", lastUpdated = LocalDateTime.of(2022, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC))

  "GET /user-answers/:userId" should {
    "return 200" in {

      when(mockSessionRepository.get("123")) thenReturn Future.successful(Some(testUserAnswers))

      val fakeRequest =
        FakeRequest("GET", "/user-answers/123")
      val result = controller.getUserAnswers("123")(fakeRequest)
      status(result) shouldBe Status.OK
      val body = contentAsJson(result)
      body shouldBe Json.toJson(testUserAnswers)
    }

    "return 404" in {
      when(mockSessionRepository.get("123")) thenReturn Future.successful(None)

      val fakeRequest =
        FakeRequest("GET", "/user-answers/123")
      val result = controller.getUserAnswers("123")(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "PUT /user-answers/:userId" should {
    "return 204" in {
      when(mockSessionRepository.set(testUserAnswers)) thenReturn Future.successful(true)

      val fakeRequest = FakeRequest(
        method = "PUT",
        uri = "/user-answers/123",
        headers = FakeHeaders(Seq()),
        body = Json.toJson(testUserAnswers)
      )
      val result = controller.setUserAnswers()(fakeRequest)
      status(result) shouldBe Status.NO_CONTENT
    }
  }
}
