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

package uk.gov.hmrc.platformreadinessspike.repositories

import org.scalactic.source.Position
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.MDC
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.platformreadinessspike.models.ServiceReview

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ServiceReviewRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[ServiceReview]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val serviceReview = ServiceReview(
    service = "Service1",
    lastReviewed = instant,
    reviewStatus = "pass",
    reviewerUsername = "Reviewer"
  )

  protected override val repository: ServiceReviewRepository = new ServiceReviewRepository(
    mongoComponent = mongoComponent,
    clock = stubClock
  )

  ".getServiceReview" - {

    "when there are no questions for this service" - {
      "must return an empty Seq" in {
        val expectedResult = None
        repository.getServiceReview("Service0").futureValue mustEqual expectedResult
      }
    }

    "when a record exists for this service" - {
      "get the records" in {
        insert(serviceReview).futureValue

        val result = repository.getServiceReview("Service1").futureValue
        val expectedResult = Some(serviceReview)

        result mustEqual expectedResult
      }
    }

    mustPreserveMdc(repository.getServiceReview("Service1"))

  }

  ".setServiceReview" - {

    "when there is no existing review for this service" - {
      "must insert the record" in {
        repository.setServiceReview(serviceReview).futureValue

        val result = repository.getServiceReview("Service1").futureValue
        val expectedResult = Some(serviceReview)

        result mustEqual expectedResult
      }
    }

    "when a record exists for this service" - {
      "must update the record" in {
        val oldRecord = serviceReview.copy(lastReviewed = instant.minus(1, ChronoUnit.DAYS))
        insert(oldRecord).futureValue

        repository.setServiceReview(oldRecord).futureValue

        val result = repository.getServiceReview("Service1").futureValue
        val expectedResult = Some(serviceReview)

        result mustEqual expectedResult
      }
    }

    mustPreserveMdc(repository.getServiceReview("Service1"))

  }


  private def mustPreserveMdc[A](f: => Future[A])(implicit pos: Position): Unit =
    "must preserve MDC" in {

      MDC.put("test", "foo")

      f.map { _ =>
        MDC.get("test") mustEqual "foo"
      }.futureValue
    }
}
