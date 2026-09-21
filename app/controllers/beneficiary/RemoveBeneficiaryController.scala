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
import views.html.beneficiary.RemoveBeneficiaryView
import controllers.actions._
import forms.beneficiary.RemoveBeneficiaryFormProvider
import models.{CheckMode, Mode, NormalMode}
import pages.beneficiary.{BeneficiariesPage, BeneficiaryElementPage}
import play.api.i18n.MessagesApi

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class RemoveBeneficiaryController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: RemoveBeneficiaryFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: RemoveBeneficiaryView
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  private val form = formProvider()

  def onPageLoad(srn: Srn, mode: Mode, index: Int): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        BeneficiaryNameHelper.withName(request.userAnswers, index)(
          logAndJourneyRecovery("Beneficiary name is missing, cannot load the remove beneficiary page")
        ) { beneficiaryName =>
          Ok(view(form, srn, index, mode, beneficiaryName))
        }
      }

  def onSubmit(srn: Srn, mode: Mode, index: Int): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        BeneficiaryNameHelper.withName(request.userAnswers, index)(
          Future.successful(
            logAndJourneyRecovery("Beneficiary name is missing, cannot submit the remove beneficiary page")
          )
        ) { beneficiaryName =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, srn, index, mode, beneficiaryName))),
              removeBeneficiary =>
                if (removeBeneficiary) {
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.remove(BeneficiaryElementPage(index)))
                    _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
                  } yield {
                    val nextPage = (updatedAnswers.get(BeneficiariesPage()).map(_.beneficiaries), mode) match {
                      case (Some(remainingBeneficiaries), NormalMode) if remainingBeneficiaries.nonEmpty =>
                        routes.BeneficiaryListController.onPageLoad(srn)
                      case (Some(remainingBeneficiaries), CheckMode) if remainingBeneficiaries.nonEmpty =>
                        controllers.routes.CheckYourAnswersController.onPageLoad(srn)
                      case _ =>
                        controllers.routes.AreBeneficiariesKnownController.onPageLoad(srn, NormalMode)
                    }

                    Redirect(nextPage)
                  }
                } else {
                  mode match {
                    case CheckMode =>
                      Future.successful(
                        Redirect(
                          controllers.routes.CheckYourAnswersController.onPageLoad(srn)
                        )
                      )
                    case _ => Future.successful(Redirect(routes.BeneficiaryListController.onPageLoad(srn)))
                  }
                }
            )
        }
      }
}
