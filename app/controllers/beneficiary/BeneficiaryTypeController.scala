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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.DidPrSubmitPage
import controllers.IhtpBaseController
import models.SchemeId.Srn
import views.html.beneficiary.BeneficiaryTypeView
import controllers.actions._
import forms.beneficiary.BeneficiaryTypeFormProvider
import models.beneficiary.BeneficiaryType
import models._
import pages.beneficiary.{BeneficiaryNamePage, BeneficiaryTrustNamePage, BeneficiaryTypePage}
import play.api.i18n.MessagesApi
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class BeneficiaryTypeController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: BeneficiaryTypeFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: BeneficiaryTypeView
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  val form: Form[BeneficiaryType] = formProvider()

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        val preparedForm: Form[BeneficiaryType] = request.userAnswers.get(BeneficiaryTypePage(index)) match {
          case None => form
          case Some(beneficiaryType) => form.fill(beneficiaryType)
        }

        Ok(view(preparedForm, srn, index, mode, request.userAnswers.get(DidPrSubmitPage).contains(true)))
      }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  view(formWithErrors, srn, index, mode, request.userAnswers.get(DidPrSubmitPage).contains(true))
                )
              ),
            beneficiaryType =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(BeneficiaryTypePage(index), beneficiaryType))
                _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
              } yield Redirect(nextPage(srn, index, mode, beneficiaryType, updatedAnswers))
          )
      }

  private def nextPage(srn: Srn, index: Int, mode: Mode, answer: BeneficiaryType, userAnswers: UserAnswers) =
    mode match {
      case NormalMode =>
        answer match {
          case BeneficiaryType.Individual =>
            routes.BeneficiaryNameController.onPageLoad(
              srn,
              NormalMode,
              index
            )
          case BeneficiaryType.Trust =>
            routes.BeneficiaryTrustNameController.onPageLoad(srn, index, NormalMode)
        }
      case CheckMode =>
        answer match {
          case BeneficiaryType.Individual
              if userAnswers.get(BeneficiaryNamePage(index, JourneyRole.BeneficiaryIndividual)).isEmpty =>
            routes.BeneficiaryNameController.onPageLoad(
              srn,
              CheckMode,
              index
            )
          case BeneficiaryType.Trust if userAnswers.get(BeneficiaryTrustNamePage(index)).isEmpty =>
            routes.BeneficiaryTrustNameController.onPageLoad(srn, index, CheckMode)
          case _ =>
            controllers.routes.CheckYourAnswersController.onPageLoad(srn)
        }
    }
}
