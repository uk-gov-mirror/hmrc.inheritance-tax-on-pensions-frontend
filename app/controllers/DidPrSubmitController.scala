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
import utils.PrNameHelper
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.{DidPrSubmitPage, PaymentNoticeDatePage}
import controllers.actions._
import forms.DidPrSubmitFormProvider
import models._
import play.api.i18n.MessagesApi
import views.html.DidPrSubmitView
import models.SchemeId.Srn

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class DidPrSubmitController @Inject() (
  override val messagesApi: MessagesApi,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DidPrSubmitFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: DidPrSubmitView
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  val form = formProvider()

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        PrNameHelper.withName(request.userAnswers)(logAndJourneyRecovery("PR name is missing, cannot load the page")) {
          prName =>
            val preparedForm = request.userAnswers.get(DidPrSubmitPage) match {
              case None => form
              case Some(value) =>
                form.fill(value)
            }
            Ok(view(preparedForm, srn, mode, prName))
        }
      }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        PrNameHelper.withName(request.userAnswers) {
          Future.successful(logAndJourneyRecovery("PR name is missing, cannot submit the page"))
        } { prName =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, srn, mode, prName))),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(DidPrSubmitPage, value))
                  _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
                } yield Redirect(nextPage(srn, mode, updatedAnswers))
            )
        }
      }

  private def nextPage(srn: Srn, mode: Mode, userAnswers: UserAnswers) =
    mode match {
      case NormalMode => routes.PaymentNoticeDateController.onPageLoad(srn, NormalMode)
      case CheckMode if userAnswers.get(PaymentNoticeDatePage).isEmpty =>
        routes.PaymentNoticeDateController.onPageLoad(srn, CheckMode)
      case CheckMode => routes.CheckYourAnswersController.onPageLoad(srn)
    }
}
