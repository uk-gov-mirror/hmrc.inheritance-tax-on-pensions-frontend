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
import controllers.IhtpBaseController
import models.SchemeId.Srn
import views.html.beneficiary.BeneficiaryTrustNameView
import controllers.actions._
import forms.beneficiary.BeneficiaryTrustNameFormProvider
import models.{Mode, UserAnswers}
import pages.beneficiary.BeneficiaryTrustNamePage
import play.api.i18n.MessagesApi

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class BeneficiaryTrustNameController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: BeneficiaryTrustNameFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: BeneficiaryTrustNameView
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  private val form = formProvider()

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        val preparedForm = request.userAnswers.get(BeneficiaryTrustNamePage(index)) match {
          case Some(name) => form.fill(name)
          case None => form
        }

        Ok(view(preparedForm, srn, index, mode))
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
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, srn, index, mode))),
            name =>
              for {
                updatedAnswers <- saveName(request.userAnswers, index, name)
                _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
              } yield Redirect(nextPage(srn, mode))
          )
      }

  private def nextPage(srn: Srn, mode: Mode) =
    mode match {
      case models.NormalMode => routes.BeneficiaryListController.onPageLoad(srn)
      case models.CheckMode =>
        controllers.routes.CheckYourAnswersController.onPageLoad(srn)
    }

  private def saveName(
    userAnswers: UserAnswers,
    index: Int,
    name: String
  ): Future[UserAnswers] =
    Future.fromTry(userAnswers.set(BeneficiaryTrustNamePage(index), name))
}
