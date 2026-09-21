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
import pages.{IndividualNamePage, OrganisationNamePage, PrOrganisationAddressPage}
import controllers.actions._
import forms.OrganisationNameFormProvider
import models._
import play.api.data.Form
import views.html.OrganisationNameView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class OrganisationNameController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: OrganisationNameFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: OrganisationNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        val preparedForm = request.userAnswers.get(OrganisationNamePage) match {
          case None => form
          case Some(value) => form.fill(value)
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
            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(OrganisationNamePage, value))
                _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
              } yield Redirect(nextPage(srn, mode, updatedAnswers))
          )
      }

  private def nextPage(srn: Srn, mode: Mode, userAnswers: UserAnswers) =
    mode match {
      case NormalMode => routes.IndividualNameController.onPageLoad(srn, NormalMode, JourneyRole.PrOrganisation)
      case CheckMode if userAnswers.get(IndividualNamePage(JourneyRole.PrOrganisation)).isEmpty =>
        routes.IndividualNameController.onPageLoad(srn, CheckMode, JourneyRole.PrOrganisation)
      case CheckMode if userAnswers.get(PrOrganisationAddressPage).isEmpty =>
        routes.AddressLookupStartController.start(srn, CheckMode, JourneyRole.PrOrganisation)
      case CheckMode => routes.CheckYourAnswersController.onPageLoad(srn)
    }
}
