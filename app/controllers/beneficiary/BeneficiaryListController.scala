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

import utils.BeneficiaryNameHelper
import play.api.mvc._
import controllers.IhtpBaseController
import models.SchemeId.Srn
import views.html.beneficiary.BeneficiaryListView
import controllers.actions._
import forms.beneficiary.BeneficiaryListFormProvider
import viewmodels.beneficiary.BeneficiaryListItem
import models.beneficiary.{Beneficiaries, BeneficiaryType}
import models.{CheckMode, UserAnswers}
import pages.beneficiary.BeneficiariesPage
import play.api.i18n.MessagesApi

import javax.inject.Inject

class BeneficiaryListController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: BeneficiaryListFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: BeneficiaryListView
) extends IhtpBaseController {

  private val form = formProvider()

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        beneficiariesAndItems(srn, request.userAnswers) match {
          case Right((beneficiaries, items)) =>
            Ok(view(form, srn, items, beneficiaries.beneficiaries.size))
          case Left(result) => result
        }
      }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        beneficiariesAndItems(srn, request.userAnswers) match {
          case Right((beneficiaries, items)) =>
            form
              .bindFromRequest()
              .fold(
                formWithErrors => BadRequest(view(formWithErrors, srn, items, beneficiaries.beneficiaries.size)),
                addAnother => Redirect(nextPage(srn, addAnother, beneficiaries.beneficiaries.size))
              )
          case Left(result) => result
        }
      }

  private def nextPage(srn: Srn, addAnother: Boolean, nextIndex: Int) =
    if (addAnother) {
      routes.BeneficiaryTypeController.onPageLoad(srn, nextIndex, models.NormalMode)
    } else {
      controllers.routes.CheckYourAnswersController.onPageLoad(srn)
    }

  private def beneficiariesAndItems(
    srn: Srn,
    userAnswers: UserAnswers
  ): Either[Result, (Beneficiaries, Seq[BeneficiaryListItem])] = {
    val beneficiaries = userAnswers.get(BeneficiariesPage()).getOrElse(Beneficiaries(Nil))
    val items = beneficiaries.beneficiaries.zipWithIndex.flatMap { case (beneficiary, index) =>
      BeneficiaryNameHelper.fromUserAnswers(userAnswers, index).map { name =>
        val changeUrl = beneficiary.beneficiaryType match {
          case BeneficiaryType.Individual =>
            routes.BeneficiaryNameController
              .onPageLoad(srn, CheckMode, index)
              .url
          case BeneficiaryType.Organisation =>
            routes.BeneficiaryOrganisationDetailsController.onPageLoad(srn, index, CheckMode).url
        }

        BeneficiaryListItem(
          name = name,
          changeUrl = changeUrl,
          removeUrl = routes.RemoveBeneficiaryController.onPageLoad(srn, index).url
        )
      }
    }

    if (items.size == beneficiaries.beneficiaries.size) {
      Right((beneficiaries, items))
    } else {
      Left(logAndJourneyRecovery("Beneficiary details are incomplete, cannot load or submit the beneficiary list page"))
    }
  }
}
