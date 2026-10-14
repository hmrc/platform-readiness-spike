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

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.*
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.platformreadinessspike.models.Question
import uk.gov.hmrc.platformreadinessspike.repositories.QuestionRepository.MongoException
import play.api.Logging

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.model.Accumulators._
import org.mongodb.scala.model.Filters._

@Singleton
class QuestionRepository @Inject()(
                                   mongoComponent: MongoComponent,
                                   clock: Clock
                                 )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[Question](
    collectionName = "service-assessment-questions",
    mongoComponent = mongoComponent,
    domainFormat   = Question.format,
    indexes        = Seq(
      IndexModel(
        Indexes.compoundIndex(
          Indexes.ascending("service", "questionId"),
          Indexes.descending("lastUpdated")
        )
      )
    )
  ) with Logging {

  //We want to store a history of all previous answers so we can trace resolutions
  override lazy val requiresTtlIndex = false

  def getCurrentQuestions(service: String): Future[Seq[Question]] =
    collection
      .aggregate[Question](Seq(
        `match`(equal("service", service)),
        sort(orderBy(
          ascending("questionId"),
          descending("lastUpdated")
        )),
        group("$questionId", first("doc", "$$ROOT")),
        replaceRoot("$doc")
      ))
      .toFuture()

  def getQuestionHistory(service: String, questionId: String): Future[Seq[Question]] =
    collection
      .find(and(
        equal("service", service),
        equal("questionId", questionId)
      ))
      .toFuture()

  def insertQuestion(question: Question): Future[Unit] = {
    val updatedQuestion = question.copy(lastUpdated = Instant.now(clock))

    collection.insertOne(updatedQuestion).toFuture().flatMap { result =>
      if (result.wasAcknowledged()) {
        Future.unit
      } else {
        logger.error(s"Insertion of question failed: $question. Message: $result")
        Future.failed(new MongoException(s"Insertion of question failed: $question. Message: $result"))
      }
    }
  }
}

object QuestionRepository {
  class MongoException(message: String) extends Exception(message: String)
}