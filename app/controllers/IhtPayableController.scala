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
import pages.{AreBeneficiariesKnownPage, IhtPayablePage}
import controllers.actions._
import forms.IhtPayableFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import views.html.IhtPayableView
import models.SchemeId.Srn

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class IhtPayableController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: IhtPayableFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: IhtPayableView
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  private val form = formProvider()

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      if (request.userAnswers.get(AreBeneficiariesKnownPage).contains(false)) {
        val preparedForm = request.userAnswers.get(IhtPayablePage).fold(form)(form.fill)
        Ok(view(preparedForm, srn, mode))
      } else {
        logAndJourneyRecovery("Beneficiaries must be unknown to enter the total IHT payable")
      }
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
      if (request.userAnswers.get(AreBeneficiariesKnownPage).contains(false)) {
        form
          .bindFromRequest()
          .fold(
            errors => Future.successful(BadRequest(view(errors, srn, mode))),
            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(IhtPayablePage, value))
                saved <- userAnswersService.set(updatedAnswers)(using hc, request.request)
              } yield saved.fold(
                _ => logAndJourneyRecovery("Unable to save the IHT payable amount"),
                _ => Redirect(routes.CheckYourAnswersController.onPageLoad(srn))
              )
          )
      } else {
        Future.successful(logAndJourneyRecovery("Beneficiaries must be unknown to save the total IHT payable"))
      }
    }
}
