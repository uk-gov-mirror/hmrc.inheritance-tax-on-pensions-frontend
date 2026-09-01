/*
 * Copyright 2025 HM Revenue & Customs
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

import services.CountryService
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import com.google.inject.Inject
import viewmodels.CheckAnswers.beneficiary.{
  BeneficiaryHasNinoSummary,
  BeneficiaryIndividualNameSummary,
  BeneficiaryTypeSummary
}
import play.i18n.Lang
import controllers.actions._
import models.beneficiary.Beneficiaries
import models.UserAnswers
import pages.beneficiary.BeneficiariesPage
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.CheckAnswers._
import viewmodels.govuk.summarylist._

class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  countryService: CountryService
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>

      val userAnswers: UserAnswers = request.userAnswers

      val list = SummaryListViewModel(
        rows = Seq(
          InheritanceTaxReferenceSummary.row(srn, userAnswers),
          NameOfDeceasedSummary.row(srn, userAnswers),
          HasNinoSummary.row(srn, userAnswers),
          NinoSummary.row(srn, userAnswers),
          NoNinoReasonSummary.row(srn, userAnswers),
          BirthDeathDatesSummary.row(srn, userAnswers),
          PrTypeSummary.row(srn, userAnswers),
          PrIndividualNameSummary.row(srn, userAnswers),
          PrOrganisationNameSummary.row(srn, userAnswers),
          PrOrganisationPrNameSummary.row(srn, userAnswers),
          PrIndividualCountrySummary.row(srn, userAnswers, countryService.nameForCode),
          PrIndividualAddressSummary.row(srn, userAnswers),
          PrOrganisationCountrySummary.row(srn, userAnswers, countryService.nameForCode),
          PrOrganisationAddressSummary.row(srn, userAnswers),
          DidPrSubmitSummary.row(srn, userAnswers),
          PaymentNoticeDateSummary.row(srn, userAnswers),
          AreBeneficiariesKnownSummary.row(srn, userAnswers)
        ).flatten
      )

      val beneficiaryList = userAnswers
        .get[Beneficiaries](BeneficiariesPage())
        .map(
          _.beneficiaries.zipWithIndex
            .map { case (_, index) =>
              SummaryListViewModel(
                rows = Seq(
                  BeneficiaryTypeSummary.row(srn, index, userAnswers),
                  BeneficiaryIndividualNameSummary.row(srn, index, userAnswers),
                  viewmodels.CheckAnswers.beneficiary.BeneficiaryOrganisationNameSummary.row(
                    srn,
                    index,
                    userAnswers
                  ),
                  viewmodels.CheckAnswers.beneficiary.BeneficiaryOrganisationHmrcReferenceSummary.row(
                    srn,
                    index,
                    userAnswers
                  ),
                  BeneficiaryHasNinoSummary.row(srn, index, userAnswers)
                ).flatten
              ).withCard(
                CardViewModel(
                  messagesApi("checkYourAnswers.beneficiary.details.card.title", index + 1)(using
                    Lang.defaultLang
                  ),
                  2,
                  None
                )
              )
            }
        )
        .getOrElse(List())

      val result = Ok(view(srn, list, beneficiaryList))
      val uuidFromQuery = request.request.getQueryString("uuid")

      uuidFromQuery match {
        case Some(uuidFromQuery) => result.addingToSession("uuid" -> uuidFromQuery)
        case _ => result
      }
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      val uuidFromQuery = request.request.getQueryString("uuid")
      val redirect = if (request.request.pensionSchemeId.isPSP) {
        Redirect(routes.PspDeclarationController.onPageLoad(srn))
      } else {
        Redirect(routes.PsaDeclarationController.onPageLoad(srn))
      }
      uuidFromQuery match {
        case Some(uuid) => redirect.addingToSession("uuid" -> uuid)
        case None => redirect
      }
    }
}
