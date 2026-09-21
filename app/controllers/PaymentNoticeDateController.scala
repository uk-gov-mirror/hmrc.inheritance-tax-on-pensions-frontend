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
import forms.PaymentNoticeDateFormProvider
import models._
import play.api.i18n.MessagesApi
import views.html.PaymentNoticeDateView
import models.SchemeId.Srn

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class PaymentNoticeDateController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: PaymentNoticeDateFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: PaymentNoticeDateView
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        request.userAnswers.get(DidPrSubmitPage) match {
          case Some(_) =>
            val form = formProvider()
            val preparedForm = request.userAnswers.get(PaymentNoticeDatePage) match {
              case None => form
              case Some(value) => form.fill(value)
            }

            Ok(view(preparedForm, srn, mode, request.request.schemeDetails.schemeName))
          case _ =>
            logAndJourneyRecovery("PR payment notice answer is missing, cannot load the page")
        }
      }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        request.userAnswers.get(DidPrSubmitPage) match {
          case Some(_) =>
            val form = formProvider()

            formProvider
              .validate(
                form.bindFromRequest(),
                request.userAnswers.get(BirthDeathDatesPage).map(_.dateOfDeath)
              )
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(view(formWithErrors, srn, mode, request.request.schemeDetails.schemeName))
                  ),
                value =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(PaymentNoticeDatePage, value))
                    _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
                  } yield Redirect(nextPage(srn, mode, updatedAnswers))
              )
          case _ =>
            Future.successful(
              logAndJourneyRecovery("PR payment notice answer is missing, cannot submit the page")
            )
        }
      }

  private def nextPage(srn: Srn, mode: Mode, userAnswers: UserAnswers) =
    mode match {
      case NormalMode if userAnswers.get(DidPrSubmitPage).contains(true) =>
        routes.AreBeneficiariesKnownController.onPageLoad(srn, NormalMode)
      case NormalMode =>
        controllers.beneficiary.routes.BeneficiaryTypeController.onPageLoad(srn, 0, NormalMode)
      case CheckMode if userAnswers.get(AreBeneficiariesKnownPage).isEmpty =>
        routes.AreBeneficiariesKnownController.onPageLoad(srn, CheckMode)
      case CheckMode => routes.CheckYourAnswersController.onPageLoad(srn)
    }
}
