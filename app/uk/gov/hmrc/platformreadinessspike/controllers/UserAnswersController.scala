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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.platformreadinessspike.models.UserAnswers
import uk.gov.hmrc.platformreadinessspike.repositories.SessionRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class UserAnswersController @Inject()(
   sessionRepository: SessionRepository,
   cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController(cc) {

  //look into getting userId from AUTH
  def getUserAnswers(userId: String): Action[AnyContent] = Action.async { implicit request =>
    sessionRepository.get(userId).map{
      case Some(userAnswers) => Ok(Json.toJson(userAnswers))
      case None              => NotFound("User Answer not found")
    }
  }
  def setUserAnswers(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withValidJson[UserAnswers] { submission =>
      sessionRepository.set(submission).map(_ => NoContent)
    }
  }
}
