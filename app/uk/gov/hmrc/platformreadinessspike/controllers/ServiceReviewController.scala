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
import uk.gov.hmrc.platformreadinessspike.models.ServiceReview
import uk.gov.hmrc.platformreadinessspike.repositories.ServiceReviewRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class ServiceReviewController @Inject()(
  serviceReviewRepository: ServiceReviewRepository,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
  extends BaseController(cc) {

  def getServiceReview(service: String): Action[AnyContent] = Action.async { implicit request =>
    serviceReviewRepository.getServiceReview(service).map{
      case Some(serviceReview)  => Ok(Json.toJson(serviceReview))
      case None                 => NotFound("Service review not found")
    }
  }

  def setServiceReview(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withValidJson[ServiceReview] { serviceReview =>
      serviceReviewRepository.setServiceReview(serviceReview).map(_ => NoContent)
    }
  }

}
