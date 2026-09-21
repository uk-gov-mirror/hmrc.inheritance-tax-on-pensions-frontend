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
import pages.{AreBeneficiariesKnownPage, DidPrSubmitPage}
import controllers.actions._
import forms.AreBeneficiariesKnownFormProvider
import models.{CheckMode, Mode, NormalMode}
import play.api.i18n.MessagesApi
import views.html.AreBeneficiariesKnownView
import models.SchemeId.Srn

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class AreBeneficiariesKnownController @Inject() (
  override val messagesApi: MessagesApi,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: AreBeneficiariesKnownFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: AreBeneficiariesKnownView
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  private val form = formProvider()

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        request.userAnswers.get(DidPrSubmitPage) match {
          case Some(_) =>
            val preparedForm = request.userAnswers.get(AreBeneficiariesKnownPage) match {
              case None => form
              case Some(value) => form.fill(value)
            }
            Ok(view(preparedForm, srn, mode))
          case None =>
            logAndJourneyRecovery("PR payment notice answer is missing, cannot load the beneficiaries known page")
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
            form
              .bindFromRequest()
              .fold(
                formWithErrors => Future.successful(BadRequest(view(formWithErrors, srn, mode))),
                value =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(AreBeneficiariesKnownPage, value))
                    _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
                  } yield Redirect(nextPage(srn, mode, value))
              )
          case None =>
            Future.successful(
              logAndJourneyRecovery("PR payment notice answer is missing, cannot submit the beneficiaries known page")
            )
        }
      }

  private def nextPage(srn: Srn, mode: Mode, value: Boolean) =
    if (!value) {
      routes.IhtPayableController.onPageLoad(srn, mode)
    } else {
      mode match {
        case NormalMode => controllers.beneficiary.routes.BeneficiaryTypeController.onPageLoad(srn, 0, NormalMode)
        case CheckMode => routes.CheckYourAnswersController.onPageLoad(srn)
      }
    }
}
