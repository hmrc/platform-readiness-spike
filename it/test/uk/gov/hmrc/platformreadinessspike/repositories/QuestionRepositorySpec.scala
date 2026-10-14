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
import uk.gov.hmrc.platformreadinessspike.models.Question

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class QuestionRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[Question]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

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

  protected override val repository: QuestionRepository = new QuestionRepository(
    mongoComponent = mongoComponent,
    clock          = stubClock
  )

  ".getCurrentQuestions" - {

    "when there are no questions for this service" - {
      "must return an empty Seq" in {
        repository.getCurrentQuestions("Service0").futureValue mustEqual Seq()
      }
    }

    "when there are questions for this service, but no historic records" - {
      "get the records" in {
        val q1 = question
        val q2 = question.copy(questionId = "Question2")

        List(q1, q2).foreach(q => insert(q).futureValue)

        val result = repository.getCurrentQuestions("Service1").futureValue
        val expectedResult = Seq(q1, q2)

        result mustEqual expectedResult
      }

      "not return questions from other services" in {
        val q1 = question
        val q2 = question.copy(questionId = "Question2")
        val q3 = q1.copy(service = "Service2")
        val q4 = q2.copy(service = "Service2")

        List(q1, q2, q3, q4).foreach(q => insert(q).futureValue)

        val result = repository.getCurrentQuestions("Service1").futureValue
        val expectedResult = Seq(q1, q2)

        result mustEqual expectedResult
      }
    }

    "when there are questions for this service, and historic records exist" - {
      "get the records" in {
        val q1 = question
        val q2 = question.copy(questionId = "Question2")
        val historicQ1 = q1.copy(lastUpdated = instant.minus(1, ChronoUnit.DAYS))
        val historicQ2 = q2.copy(lastUpdated = instant.minus(1, ChronoUnit.DAYS))
        val historic2Q2 = q2.copy(lastUpdated = instant.minus(2, ChronoUnit.DAYS))

        List(q1, q2, historicQ1, historicQ2, historic2Q2).foreach(q => insert(q).futureValue)

        val result = repository.getCurrentQuestions("Service1").futureValue
        val expectedResult = Seq(q1, q2)

        result mustEqual expectedResult
      }
    }

    mustPreserveMdc(repository.getCurrentQuestions("Service1"))

  }

  ".getQuestionHistory" - {

    "when there is no records for the questionId" - {
      "must return an empty Seq" in {
        val q2 = question.copy(questionId = "Question2")

        List(q2).foreach(q => insert(q).futureValue)

        repository.getQuestionHistory("Service1", "Question1").futureValue mustEqual Seq()
      }
    }

    "when the question exists for the service and only has one result" - {
      "get the records" in {
        val q1 = question
        val q2 = question.copy(questionId = "Question2")

        List(q1, q2).foreach(q => insert(q).futureValue)

        val result = repository.getQuestionHistory("Service1", "Question1").futureValue
        val expectedResult = Seq(q1)

        result mustEqual expectedResult
      }

      "not return questions from other services" in {
        val q1 = question
        val q3 = q1.copy(service = "Service2")

        List(q1, q3).foreach(q => insert(q).futureValue)

        val result = repository.getQuestionHistory("Service1", "Question1").futureValue
        val expectedResult = Seq(q1)

        result mustEqual expectedResult
      }
    }

    "when the question exists for the service and there are multiple results" - {

      "not return questions from other services" in {
        val q1 = question
        val q2 = question.copy(questionId = "Question2")
        val historicQ1 = q1.copy(lastUpdated = instant.minus(1, ChronoUnit.DAYS))
        val historicQ2 = q2.copy(lastUpdated = instant.minus(1, ChronoUnit.DAYS))
        val historic2Q2 = q2.copy(lastUpdated = instant.minus(2, ChronoUnit.DAYS))

        List(q1, q2, historicQ1, historicQ2, historic2Q2).foreach(q => insert(q).futureValue)

        val result = repository.getQuestionHistory("Service1", "Question2").futureValue
        val expectedResult = Seq(q2, historicQ2, historic2Q2)

        result mustEqual expectedResult
      }
    }

    mustPreserveMdc(repository.getQuestionHistory("Service1", "Question2"))

  }

  ".insertQuestion" - {

    "when a record is entered" - {

      "it should be retrievable" in {
        repository.insertQuestion(question).futureValue
        val result = repository.getQuestionHistory("Service1", "Question1").futureValue
        result mustEqual Seq(question)
      }

      "if previous entries exist they won't be overwritten" in {
        val previousEntry = question.copy(lastUpdated = instant.minus(1, ChronoUnit.DAYS))
        insert(previousEntry).futureValue

        repository.insertQuestion(question).futureValue
        val result = repository.getQuestionHistory("Service1", "Question1").futureValue
        result mustEqual Seq(question, previousEntry)
      }
    }

    mustPreserveMdc(repository.insertQuestion(question))
  }


  private def mustPreserveMdc[A](f: => Future[A])(implicit pos: Position): Unit =
    "must preserve MDC" in {

      MDC.put("test", "foo")

      f.map { _ =>
        MDC.get("test") mustEqual "foo"
      }.futureValue
    }
}
