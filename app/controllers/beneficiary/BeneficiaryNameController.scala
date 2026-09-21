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
import play.api.mvc._
import controllers.{routes, IhtpBaseController}
import models.SchemeId.Srn
import views.html.beneficiary.BeneficiaryNameView
import controllers.actions._
import forms.beneficiary.BeneficiaryNameFormProvider
import models._
import pages.beneficiary.{BeneficiaryHasNinoPage, BeneficiaryNamePage}
import play.api.i18n.MessagesApi

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import javax.inject.Inject

class BeneficiaryNameController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: BeneficiaryNameFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: BeneficiaryNameView
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  def onPageLoad(srn: Srn, mode: Mode, index: Int, journeyRole: JourneyRole): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        journeyRole match {
          case JourneyRole.Unknown =>
            logAndJourneyRecovery("unknown journeyRole, cannot load the page")

          case _ =>
            val form = formProvider(journeyRole)
            val preparedForm = request.userAnswers.get(BeneficiaryNamePage(index, journeyRole)) match {
              case None => form
              case Some(individualName) => form.fill(individualName)
            }

            Ok(view(preparedForm, srn, index, mode, journeyRole))
        }
      }

  def onSubmit(srn: Srn, mode: Mode, index: Int, journeyRole: JourneyRole): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        journeyRole match {
          case JourneyRole.Unknown =>
            Future.successful(logAndJourneyRecovery("unknown journeyRole, cannot submit the page"))

          case _ =>
            formProvider(journeyRole)
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      view(formWithErrors, srn, index, mode, journeyRole)
                    )
                  ),
                individualName =>
                  for {
                    updatedAnswers <- Future
                      .fromTry(addIndividualName(index, request.userAnswers, journeyRole, individualName))
                    _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
                  } yield Redirect(nextPage(srn, index, mode, journeyRole, updatedAnswers))
              )
        }
      }

  private[controllers] def addIndividualName(
    index: Int,
    userAnswers: UserAnswers,
    journeyRole: JourneyRole,
    individualName: IndividualName
  ): Try[UserAnswers] =
    userAnswers.set(BeneficiaryNamePage(index, journeyRole), individualName)

  private[controllers] def nextPage(
    srn: Srn,
    index: Int,
    mode: Mode,
    journeyRole: JourneyRole,
    userAnswers: UserAnswers
  ): Call =
    journeyRole match {
      case JourneyRole.BeneficiaryIndividual =>
        mode match {
          case NormalMode =>
            controllers.beneficiary.routes.BeneficiaryHasNinoController.onPageLoad(srn, index, NormalMode)
          case CheckMode if userAnswers.get(BeneficiaryHasNinoPage(index)).isEmpty =>
            controllers.beneficiary.routes.BeneficiaryHasNinoController.onPageLoad(srn, index, CheckMode)
          case CheckMode => routes.CheckYourAnswersController.onPageLoad(srn)
        }
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
}
