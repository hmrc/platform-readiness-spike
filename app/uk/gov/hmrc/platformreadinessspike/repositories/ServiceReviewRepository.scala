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

import org.mongodb.scala.model.*
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.platformreadinessspike.models.ServiceReview
import uk.gov.hmrc.platformreadinessspike.repositories.QuestionRepository.MongoException

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ServiceReviewRepository @Inject()(
                                   mongoComponent: MongoComponent,
                                   clock: Clock
                                 )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[ServiceReview](
    collectionName = "service-assessment-reviews",
    mongoComponent = mongoComponent,
    domainFormat   = ServiceReview.format,
    indexes        = Seq(
      IndexModel(Indexes.ascending("service"))
    )
  ) with Logging {

  //We need to retain the current review data for each service
  override lazy val requiresTtlIndex = false

  def getServiceReview(service: String): Future[Option[ServiceReview]] =
    collection
      .find(Filters.equal("service", service))
      .headOption()

  def setServiceReview(serviceReview: ServiceReview): Future[Unit] = {

    val updatedServiceReview = serviceReview.copy(lastReviewed = Instant.now(clock))

    collection
      .replaceOne(
        filter = Filters.equal("service", serviceReview.service),
        replacement = updatedServiceReview,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .flatMap { result =>
        if (result.wasAcknowledged()) {
          Future.unit
        } else {
          logger.error(s"Upserting of service review failed: $serviceReview. Message: $result")
          Future.failed(new MongoException(s"Insertion of question failed: $serviceReview. Message: $result"))
        }
      }
  }
}

object ServiceReviewRepository {
  class MongoException(message: String) extends Exception(message: String)
}