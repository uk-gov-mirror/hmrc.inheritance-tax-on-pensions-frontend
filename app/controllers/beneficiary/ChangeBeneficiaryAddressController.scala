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
import pages._
import controllers.IhtpBaseController
import models.SchemeId.Srn
import views.html.beneficiary.ChangeBeneficiaryAddressView
import controllers.actions._
import forms.beneficiary.BeneficiaryAddressFormProvider
import models.beneficiary.BeneficiaryType
import models._
import pages.beneficiary.{BeneficiaryAddressPage, _}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class ChangeBeneficiaryAddressController @Inject() (
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  beneficiaryAccess: BeneficiaryAccessAction,
  formProvider: BeneficiaryAddressFormProvider,
  userAnswersService: UserAnswersService,
  val controllerComponents: MessagesControllerComponents,
  view: ChangeBeneficiaryAddressView
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  def onPageLoad(srn: Srn, index: Int): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .andThen(beneficiaryAccess) { implicit request =>
        pageData(request.userAnswers, index) match {
          case Some((address, journeyRole, displayName)) =>
            Ok(
              view(
                formProvider(address.country).fill(address),
                srn,
                index,
                displayName,
                formProvider.isUkAddress(address)
              )
            )
          case None => logAndJourneyRecovery("Beneficiary details are incomplete for change address")
        }
      }

  def onSubmit(srn: Srn, index: Int): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .andThen(beneficiaryAccess)
      .async { implicit request =>
        pageData(request.userAnswers, index) match {
          case Some((existingAddress, journeyRole, displayName)) =>
            formProvider(existingAddress.country)
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      view(
                        formWithErrors,
                        srn,
                        index,
                        displayName,
                        formProvider.isUkAddress(existingAddress)
                      )
                    )
                  ),
                address =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(BeneficiaryAddressPage(index), address))
                    _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
                  } yield Redirect(controllers.routes.CheckYourAnswersController.onPageLoad(srn))
              )
          case None =>
            Future.successful(logAndJourneyRecovery("Beneficiary details are incomplete for change address"))
        }
      }

  private def pageData(
    answers: UserAnswers,
    index: Int
  ): Option[(PrAddress, JourneyRole, String)] =
    if (
      index < 0 || index >= 30 ||
      !answers.get(AreBeneficiariesKnownPage).getOrElse(answers.get(DidPrSubmitPage).contains(false))
    ) None
    else
      for {
        role <- answers.get(BeneficiaryTypePage(index)) match {
          case Some(BeneficiaryType.Individual) if answers.get(BeneficiaryHasNinoPage(index)).exists {
                case true => answers.get(BeneficiaryNinoPage(index)).isDefined
                case false => answers.get(BeneficiaryNoNinoReasonPage(index)).isDefined
              } =>
            Some(JourneyRole.BeneficiaryIndividual)
          case Some(BeneficiaryType.Trust)
              if answers.get(BeneficiaryNamePage(index, JourneyRole.BeneficiaryTrust)).isDefined =>
            Some(JourneyRole.BeneficiaryTrust)
          case _ => None
        }
        address <- role match {
          case JourneyRole.BeneficiaryIndividual =>
            answers.get(BeneficiaryAddressPage(index))
          case JourneyRole.BeneficiaryTrust =>
            answers.get(BeneficiaryAddressPage(index))
          case _ => None
        }
        name <- BeneficiaryNameHelper.fromUserAnswers(answers, index)
      } yield (address, role, name)
}
