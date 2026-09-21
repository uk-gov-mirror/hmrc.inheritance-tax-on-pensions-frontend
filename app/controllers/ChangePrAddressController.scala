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

import services.UserAnswersService
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages._
import controllers.actions._
import play.api.libs.json.{JsObject, JsSuccess, Json}
import forms.PrAddressFormProvider
import models._
import views.html.ChangePrAddressView
import models.SchemeId.Srn

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class ChangePrAddressController @Inject() (
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: PrAddressFormProvider,
  userAnswersService: UserAnswersService,
  val controllerComponents: MessagesControllerComponents,
  view: ChangePrAddressView
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  def onPageLoad(srn: Srn, journeyRole: JourneyRole): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        pageData(request.userAnswers, journeyRole) match {
          case Right((address, displayName)) =>
            Ok(
              view(
                formProvider(address.country).fill(address),
                srn,
                journeyRole,
                displayName,
                formProvider.isUkAddress(address)
              )
            )
          case Left(logMessage) =>
            logAndJourneyRecovery(logMessage)
        }
      }

  def onSubmit(srn: Srn, journeyRole: JourneyRole): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        pageData(request.userAnswers, journeyRole) match {
          case Right((existingAddress, displayName)) =>
            formProvider(existingAddress.country)
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      view(
                        formWithErrors,
                        srn,
                        journeyRole,
                        displayName,
                        formProvider.isUkAddress(existingAddress)
                      )
                    )
                  ),
                address => {
                  val updatedAnswers = addPrAddress(request.userAnswers, journeyRole, address)

                  userAnswersService
                    .set(updatedAnswers)(using hc, request.request)
                    .map(_ => Redirect(routes.CheckYourAnswersController.onPageLoad(srn)))
                }
              )
          case Left(logMessage) =>
            Future.successful(logAndJourneyRecovery(logMessage))
        }
      }

  private[controllers] def addPrAddress(
    userAnswers: UserAnswers,
    journeyRole: JourneyRole,
    address: PrAddress
  ): UserAnswers =
    journeyRole match {
      case JourneyRole.PrIndividual =>
        replaceAddress(userAnswers, PrIndividualAddressPage.path, "individual", address)
      case JourneyRole.PrOrganisation =>
        replaceAddress(userAnswers, PrOrganisationAddressPage.path, "organisation", address)
      case _ =>
        userAnswers
    }

  private def replaceAddress(
    userAnswers: UserAnswers,
    path: play.api.libs.json.JsPath,
    prTypeKey: String,
    address: PrAddress
  ): UserAnswers =
    userAnswers.data
      .setObject(path, prWithoutAddressFields(userAnswers, prTypeKey) ++ Json.toJsObject(address)) match {
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

  private def pageData(
    userAnswers: UserAnswers,
    journeyRole: JourneyRole
  ): Either[String, (PrAddress, String)] =
    journeyRole match {
      case JourneyRole.PrIndividual =>
        for {
          address <- userAnswers
            .get(PrIndividualAddressPage)
            .toRight("PR individual address is missing, cannot load the change address page")
          name <- userAnswers
            .get(IndividualNamePage(JourneyRole.PrIndividual))
            .toRight("PR individual name is missing, cannot load the change address page")
        } yield (address, s"${name.firstForename} ${name.surname}")
      case JourneyRole.PrOrganisation =>
        for {
          address <- userAnswers
            .get(PrOrganisationAddressPage)
            .toRight("PR organisation address is missing, cannot load the change address page")
          name <- userAnswers
            .get(OrganisationNamePage)
            .toRight("PR organisation name is missing, cannot load the change address page")
        } yield (address, name)
      case _ =>
        Left("unsupported journey role, cannot load the change address page")
    }
}
