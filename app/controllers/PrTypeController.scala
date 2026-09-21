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
import forms.PrTypeFormProvider
import models._
import play.api.data.Form
import views.html.PrTypeView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class PrTypeController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: PrTypeFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: PrTypeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[PrType] = formProvider()

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        val preparedForm = request.userAnswers.get(PrTypePage) match {
          case None => form
          case Some(prType) => form.fill(prType)
        }

        Ok(view(preparedForm, srn, mode))
      }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, srn, mode))),
            prType =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(PrTypePage, prType))
                _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
              } yield Redirect(nextPage(srn, mode, prType, updatedAnswers))
          )
      }

  private def nextPage(srn: Srn, mode: Mode, answer: PrType, userAnswers: UserAnswers) =
    mode match {
      case NormalMode =>
        answer match {
          case PrType.Individual =>
            routes.IndividualNameController.onPageLoad(srn, NormalMode, JourneyRole.PrIndividual)
          case PrType.Organisation =>
            routes.OrganisationNameController.onPageLoad(srn, NormalMode)
        }
      case CheckMode =>
        answer match {
          case PrType.Individual if userAnswers.get(IndividualNamePage(JourneyRole.PrIndividual)).isEmpty =>
            routes.IndividualNameController.onPageLoad(srn, CheckMode, JourneyRole.PrIndividual)
          case PrType.Individual if userAnswers.get(PrIndividualAddressPage).isEmpty =>
            routes.AddressLookupStartController.start(srn, CheckMode, JourneyRole.PrIndividual)
          case PrType.Organisation if userAnswers.get(OrganisationNamePage).isEmpty =>
            routes.OrganisationNameController.onPageLoad(srn, CheckMode)
          case PrType.Organisation if userAnswers.get(IndividualNamePage(JourneyRole.PrOrganisation)).isEmpty =>
            routes.IndividualNameController.onPageLoad(srn, CheckMode, JourneyRole.PrOrganisation)
          case PrType.Organisation if userAnswers.get(PrOrganisationAddressPage).isEmpty =>
            routes.AddressLookupStartController.start(srn, CheckMode, JourneyRole.PrOrganisation)
          case _ =>
            routes.CheckYourAnswersController.onPageLoad(srn)
        }
    }
}
