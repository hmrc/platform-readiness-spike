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
import uk.gov.hmrc.platformreadinessspike.models.{Question, QuestionResponse}
import uk.gov.hmrc.platformreadinessspike.repositories.QuestionRepository

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class QuestionControllerSpec
  extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockQuestionRepository)
  }

  private val mockQuestionRepository = mock[QuestionRepository]

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val controller = new QuestionController(mockQuestionRepository, Helpers.stubControllerComponents())

  private val question = Question(
    service = "Service1",
    questionId = "Question1",
    lastUpdated = instant,
    teamComment = Some("This looks really good"),
    teamStatus = "pass",
    teamMemberUsername = Some("user"),
    reviewerComment = Some("Not sure about this"),
    reviewerStatus = "fail",
    reviewerUsername = Some("Reviewer")
  )

  "GET /current-questions/:session" should {
    "return 200" in {

      when(mockQuestionRepository.getCurrentQuestions("Service1")) thenReturn Future.successful(Seq(question))

      val fakeRequest =
        FakeRequest("GET", "/current-questions/Service1")
      val result = controller.getCurrentQuestions("Service1")(fakeRequest)
      status(result) shouldBe Status.OK
      val body = contentAsJson(result)
      body shouldBe Json.toJson(QuestionResponse("Service1", Seq(question)))
    }

    "return 200 for an empty list" in {
      when(mockQuestionRepository.getCurrentQuestions("Service1")) thenReturn Future.successful(Seq())

      val fakeRequest =
        FakeRequest("GET", "/current-questions/Service1")
      val result = controller.getCurrentQuestions("Service1")(fakeRequest)
      status(result) shouldBe Status.OK
      val body = contentAsJson(result)
      body shouldBe Json.toJson(QuestionResponse("Service1", Seq()))
    }
  }

  "POST /question/" should {
    "return 202" in {
      when(mockQuestionRepository.insertQuestion(question)) thenReturn Future.unit

      val fakeRequest = FakeRequest(
        method = "POST",
        uri = "/question/",
        headers = FakeHeaders(Seq()),
        body = Json.toJson(question)
      )
      val result = controller.insertQuestion()(fakeRequest)
      status(result) shouldBe Status.CREATED
    }
  }
}
