/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers

import services.{AddressLookupFrontendService, UserAnswersService}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.{PrIndividualAddressPage, PrOrganisationAddressPage}
import models.SchemeId.Srn
import controllers.actions._
import play.api.libs.json.{JsObject, JsSuccess, Json}
import models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class AddressLookupContinueController @Inject() (
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  addressLookupFrontendService: AddressLookupFrontendService,
  userAnswersService: UserAnswersService,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  def continue(srn: Srn, mode: Mode, journeyRole: JourneyRole, id: String): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        if (id.trim.isEmpty) {
          Future.successful(logAndJourneyRecovery("no id from address lookup, unable to continue"))
        } else {
          for {
            addressData <- addressLookupFrontendService.getAddress(id)
            result <-
              if (PrAddress.hasValidFirstAddressLine(addressData)) {
                journeyRole match {
                  case JourneyRole.PrIndividual =>
                    val updatedAnswers =
                      addPrAddressIndividual(request.userAnswers, PrAddress.fromAlfAddressData(addressData))
                    userAnswersService
                      .set(updatedAnswers)(using hc, request.request)
                      .map(_ => Redirect(nextPage(srn, mode)))
                  case JourneyRole.PrOrganisation =>
                    val updatedAnswers =
                      addPrAddressOrganisation(request.userAnswers, PrAddress.fromAlfAddressData(addressData))
                    userAnswersService
                      .set(updatedAnswers)(using hc, request.request)
                      .map(_ => Redirect(nextPage(srn, mode)))
                  case _ =>
                    Future.successful(logAndJourneyRecovery("unknown journeyRole, cannot load the page"))
                }
              } else {
                Future.successful(logAndJourneyRecovery("no address lines in address, unable to continue"))
              }
          } yield result
        }
      }

  private def nextPage(srn: Srn, mode: Mode) =
    mode match {
      case NormalMode => routes.DidPrSubmitController.onPageLoad(srn, NormalMode)
      case CheckMode => routes.CheckYourAnswersController.onPageLoad(srn)
    }

  private[controllers] def addPrAddressIndividual(
    userAnswers: UserAnswers,
    address: PrAddress
  ): UserAnswers =
    userAnswers.data
      .setObject(
        PrIndividualAddressPage.path,
        prWithoutAddressFields(userAnswers, "individual") ++ Json.toJsObject(address)
      ) match {
      case JsSuccess(data, _) => userAnswers.copy(data = data)
      case _ => userAnswers
    }

  private[controllers] def addPrAddressOrganisation(
    userAnswers: UserAnswers,
    address: PrAddress
  ): UserAnswers =
    userAnswers.data
      .setObject(
        PrOrganisationAddressPage.path,
        prWithoutAddressFields(userAnswers, "organisation") ++ Json.toJsObject(address)
      ) match {
      case JsSuccess(data, _) => userAnswers.copy(data = data)
      case _ => userAnswers
    }

  private def prWithoutAddressFields(userAnswers: UserAnswers, prTypeKey: String): JsObject =
    Seq(
      "addressline1",
      "addressline2",
      "addressline3",
      "addressline4",
      "addressline5",
      "ukPostcode",
      "country"
    ).foldLeft(
      (userAnswers.data \ "prDetails" \ prTypeKey)
        .asOpt[JsObject]
        .getOrElse(Json.obj())
    )(_ - _)
}
