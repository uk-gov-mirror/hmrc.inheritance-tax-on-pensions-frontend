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
import play.api.mvc._
import pages._
import controllers.actions._
import play.api.libs.json._
import forms.IndividualNameFormProvider
import models._
import play.api.i18n.MessagesApi
import views.html.IndividualNameView
import models.SchemeId.Srn

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

import javax.inject.Inject

class IndividualNameController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: IndividualNameFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: IndividualNameView
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  def onPageLoad(srn: Srn, mode: Mode, journeyRole: JourneyRole): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        journeyRole match {
          case JourneyRole.Unknown =>
            logAndJourneyRecovery("unknown journeyRole, cannot load the page")

          case _ =>
            val form = formProvider(journeyRole)
            val preparedForm = request.userAnswers.get(IndividualNamePage(journeyRole)) match {
              case None => form
              case Some(individualName) => form.fill(individualName)
            }

            Ok(view(preparedForm, srn, mode, journeyRole, organisationName(request.userAnswers, journeyRole)))
        }
      }

  def onSubmit(srn: Srn, mode: Mode, journeyRole: JourneyRole): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        journeyRole match {
          case JourneyRole.Unknown =>
            Future.successful(logAndJourneyRecovery("unknown journeyRole, cannot submit the page"))

          case _ =>
            formProvider(journeyRole)
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      view(formWithErrors, srn, mode, journeyRole, organisationName(request.userAnswers, journeyRole))
                    )
                  ),
                individualName =>
                  for {
                    updatedAnswers <- Future
                      .fromTry(addIndividualName(request.userAnswers, journeyRole, individualName))
                    _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
                  } yield Redirect(nextPage(srn, mode, journeyRole, updatedAnswers))
              )
        }
      }

  private[controllers] def addIndividualName(
    userAnswers: UserAnswers,
    journeyRole: JourneyRole,
    individualName: IndividualName
  ): Try[UserAnswers] =
    journeyRole match {
      case JourneyRole.PrIndividual =>
        addPrName(userAnswers, "individual", individualName)
      case JourneyRole.PrOrganisation =>
        addPrName(userAnswers, "organisation", individualName)
      case _ =>
        userAnswers.set(IndividualNamePage(journeyRole), individualName)
    }

  private def addPrName(
    userAnswers: UserAnswers,
    prTypeKey: String,
    individualName: IndividualName
  ): Try[UserAnswers] =
    Success(
      userAnswers.copy(
        data = userAnswers.data.deepMerge(
          Json.obj(
            "prDetails" -> Json.obj(
              prTypeKey -> Json.obj(
                "title" -> optionalString(individualName.title),
                "firstForename" -> individualName.firstForename,
                "secondForename" -> optionalString(individualName.secondForename),
                "surname" -> individualName.surname
              )
            )
          )
        )
      )
    )

  private def organisationName(userAnswers: UserAnswers, journeyRole: JourneyRole): Option[String] =
    Option.when(journeyRole == JourneyRole.PrOrganisation)(userAnswers.get(OrganisationNamePage)).flatten

  private def optionalString(value: Option[String]): JsValue =
    value.filter(_.nonEmpty).map(JsString.apply).getOrElse(JsNull)

  private[controllers] def nextPage(
    srn: Srn,
    mode: Mode,
    journeyRole: JourneyRole,
    userAnswers: UserAnswers
  ): Call =
    mode match {
      case NormalMode =>
        journeyRole match {
          case JourneyRole.PrIndividual =>
            routes.AddressLookupStartController.start(srn, NormalMode, journeyRole)
          case JourneyRole.PrOrganisation =>
            routes.AddressLookupStartController.start(srn, NormalMode, journeyRole)
          case JourneyRole.Deceased =>
            routes.HasNinoController.onPageLoad(srn, NormalMode)
          case _ => routes.JourneyRecoveryController.onPageLoad()
        }
      case CheckMode =>
        journeyRole match {
          case JourneyRole.PrIndividual if userAnswers.get(PrIndividualAddressPage).isEmpty =>
            routes.AddressLookupStartController.start(srn, CheckMode, journeyRole)
          case JourneyRole.PrOrganisation if userAnswers.get(PrOrganisationAddressPage).isEmpty =>
            routes.AddressLookupStartController.start(srn, CheckMode, journeyRole)
          case _ => routes.CheckYourAnswersController.onPageLoad(srn)
        }
    }
}
