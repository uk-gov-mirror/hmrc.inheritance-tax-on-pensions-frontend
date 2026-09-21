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

package controllers.beneficiary

import services.UserAnswersService
import utils.BeneficiaryNameHelper
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.IhtpBaseController
import models.SchemeId.Srn
import views.html.beneficiary.BeneficiaryHasNinoView
import controllers.actions._
import forms.beneficiary.BeneficiaryHasNinoFormProvider
import models.{CheckMode, Mode, NormalMode}
import pages.beneficiary.BeneficiaryHasNinoPage
import play.api.i18n.MessagesApi

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class BeneficiaryHasNinoController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: BeneficiaryHasNinoFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: BeneficiaryHasNinoView
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  private val form = formProvider()

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        BeneficiaryNameHelper.withName(request.userAnswers, index)(
          logAndJourneyRecovery("Beneficiary name is missing, cannot load the beneficiary NINO page")
        ) { beneficiaryName =>
          val preparedForm = request.userAnswers.get(BeneficiaryHasNinoPage(index)) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, srn, index, mode, beneficiaryName))
        }
      }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        BeneficiaryNameHelper.withName(request.userAnswers, index) {
          Future.successful(
            logAndJourneyRecovery("Beneficiary name is missing, cannot submit the beneficiary NINO page")
          )
        } { beneficiaryName =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, srn, index, mode, beneficiaryName))),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(BeneficiaryHasNinoPage(index), value))
                  _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
                } yield Redirect(nextPage(srn, mode))
            )
        }
      }

  private def nextPage(srn: Srn, mode: Mode) =
    mode match {
      case NormalMode => routes.BeneficiaryListController.onPageLoad(srn)
      case CheckMode => controllers.routes.CheckYourAnswersController.onPageLoad(srn)
    }
}
