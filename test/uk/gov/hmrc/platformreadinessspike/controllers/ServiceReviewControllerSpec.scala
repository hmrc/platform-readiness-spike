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
import uk.gov.hmrc.platformreadinessspike.models.ServiceReview
import uk.gov.hmrc.platformreadinessspike.repositories.ServiceReviewRepository

import java.time.temporal.ChronoUnit
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ServiceReviewControllerSpec
  extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockServiceReviewRepository)
  }

  private val mockServiceReviewRepository = mock[ServiceReviewRepository]

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val controller = new ServiceReviewController(mockServiceReviewRepository, Helpers.stubControllerComponents())

  private val serviceReview = ServiceReview(
    service = "Service1",
    lastReviewed = instant,
    reviewStatus = "pass",
    reviewerUsername = "Reviewer"
  )

  "GET /service-review/:service" should {
    "return 200" in {
      when(mockServiceReviewRepository.getServiceReview("Service1")) thenReturn Future.successful(Some(serviceReview))

      val fakeRequest =
        FakeRequest("GET", "/current-questions/Service1")
      val result = controller.getServiceReview("Service1")(fakeRequest)
      status(result) shouldBe Status.OK
      val body = contentAsJson(result)
      body shouldBe Json.toJson(serviceReview)
    }

    "return 404" in {
      when(mockServiceReviewRepository.getServiceReview("Service1")) thenReturn Future.successful(None)

      val fakeRequest =
        FakeRequest("GET", "/current-questions/Service1")
      val result = controller.getServiceReview("Service1")(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "POST /service-review/" should {
    "return 204" in {
      when(mockServiceReviewRepository.setServiceReview(serviceReview)) thenReturn Future.unit
      val fakeRequest = FakeRequest(
        method = "PUT",
        uri = "/service-review/",
        headers = FakeHeaders(Seq()),
        body = Json.toJson(serviceReview)
      )
      val result = controller.setServiceReview()(fakeRequest)
      status(result) shouldBe Status.NO_CONTENT
    }
  }
}
