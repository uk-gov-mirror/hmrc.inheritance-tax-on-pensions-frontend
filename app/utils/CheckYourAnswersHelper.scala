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

package utils

import models.JourneyRole._
import viewmodels.implicits._
import play.api.mvc.Call
import pages._
import models.SchemeId.Srn
import uk.gov.hmrc.govukfrontend.views.Aliases.Actions
import viewmodels.govuk.all.{ActionItemViewModel, CardViewModel, SummaryListViewModel}
import models.beneficiary.{Beneficiaries, BeneficiaryType}
import models._
import pages.beneficiary.BeneficiariesPage
import play.api.i18n.{Lang, Messages, MessagesApi}
import viewmodels.CheckAnswers.beneficiary._
import controllers.routes
import viewmodels.CheckAnswers._
import viewmodels.govuk.summarylist._

object CheckYourAnswersHelper {

  private case class ContinuationPage(
    isUnanswered: UserAnswers => Boolean,
    call: Call
  )

  def buildSummaryLists(userAnswers: UserAnswers, srn: Srn, countryName: String => String, messagesApi: MessagesApi)(
    implicit messages: Messages
  ): CheckYourAnswersSummaryLists = {
    val deceasedDetailsSummaryList = SummaryListViewModel(
      rows = Seq(
        InheritanceTaxReferenceSummary.row(srn, userAnswers),
        NameOfDeceasedSummary.row(srn, userAnswers),
        HasNinoSummary.row(srn, userAnswers),
        NinoSummary.row(srn, userAnswers),
        NoNinoReasonSummary.row(srn, userAnswers),
        BirthDeathDatesSummary.row(srn, userAnswers)
      ).flatten
    )
    val prDetailsSummaryList = SummaryListViewModel(
      rows = Seq(
        PrTypeSummary.row(srn, userAnswers),
        PrIndividualNameSummary.row(srn, userAnswers),
        PrOrganisationNameSummary.row(srn, userAnswers),
        PrOrganisationPrNameSummary.row(srn, userAnswers),
        PrIndividualCountrySummary.row(srn, userAnswers, countryName),
        PrIndividualAddressSummary.row(srn, userAnswers),
        PrOrganisationCountrySummary.row(srn, userAnswers, countryName),
        PrOrganisationAddressSummary.row(srn, userAnswers)
      ).flatten
    )
    val paymentNoticeDetailsSummaryList = SummaryListViewModel(
      rows = Seq(
        DidPrSubmitSummary.row(srn, userAnswers),
        PaymentNoticeDateSummary.row(srn, userAnswers),
        AreBeneficiariesKnownSummary.row(srn, userAnswers),
        IhtPayableSummary.row(srn, userAnswers),
        NumberOfBeneficiariesSummary.row(srn, userAnswers)
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
                viewmodels.CheckAnswers.beneficiary.BeneficiaryTrustNameSummary.row(
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
                Some(
                  Actions(
                    items = Seq(
                      ActionItemViewModel(
                        "site.remove",
                        controllers.beneficiary.routes.RemoveBeneficiaryController
                          .onPageLoad(srn, CheckMode, index)
                          .url
                      )
                        .withVisuallyHiddenText(
                          messagesApi(
                            "checkYourAnswers.beneficiary.details.card.remove.hidden",
                            BeneficiaryNameHelper.fromUserAnswers(userAnswers, index).getOrElse(index)
                          )(using
                            Lang.defaultLang
                          )
                        )
                    )
                  )
                )
              )
            )
          }
      )
      .getOrElse(List())

    CheckYourAnswersSummaryLists(
      deceasedDetailsSummaryList,
      prDetailsSummaryList,
      paymentNoticeDetailsSummaryList,
      beneficiaryList
    )
  }

  def findPageToContinue(userAnswers: UserAnswers, srn: Srn): Option[Call] = {
    val allPages = getDeceasedPages(srn) :++ getPrPages(srn) :++ getBeneficiariesPages(userAnswers, srn)
    val found = allPages
      .find(_.isUnanswered(userAnswers))
      .map(_.call)
    found
  }

  private def getDeceasedPages(srn: Srn) =
    Seq(
      ContinuationPage(
        answers => answers.get(IndividualNamePage(Deceased)).isEmpty,
        routes.IndividualNameController.onPageLoad(srn, NormalMode, Deceased)
      ),
      ContinuationPage(
        answers => answers.get(HasNinoPage).isEmpty,
        routes.HasNinoController.onPageLoad(srn, NormalMode)
      ),
      ContinuationPage(
        answers => answers.get(HasNinoPage).get && answers.get(NinoPage).isEmpty,
        routes.NinoController.onPageLoad(srn, NormalMode)
      ),
      ContinuationPage(
        answers => !answers.get(HasNinoPage).get && answers.get(NoNinoReasonPage).isEmpty,
        routes.NoNinoReasonController.onPageLoad(srn, NormalMode)
      ),
      ContinuationPage(
        answers => answers.get(BirthDeathDatesPage).isEmpty,
        routes.BirthDeathDatesController.onPageLoad(srn, NormalMode)
      )
    )

  private def getPrPages(srn: Srn) =
    Seq(
      ContinuationPage(
        answers => answers.get(PrTypePage).isEmpty,
        routes.PrTypeController.onPageLoad(srn, NormalMode)
      ),
      ContinuationPage(
        answers =>
          answers.get(PrTypePage).contains(PrType.Individual) && answers.get(IndividualNamePage(PrIndividual)).isEmpty,
        routes.IndividualNameController.onPageLoad(srn, NormalMode, PrIndividual)
      ),
      ContinuationPage(
        answers => answers.get(PrTypePage).contains(PrType.Individual) && answers.get(PrIndividualAddressPage).isEmpty,
        routes.AddressLookupStartController.start(srn, NormalMode, PrIndividual)
      ),
      ContinuationPage(
        answers => answers.get(PrTypePage).contains(PrType.Organisation) && answers.get(OrganisationNamePage).isEmpty,
        routes.OrganisationNameController.onPageLoad(srn, NormalMode)
      ),
      ContinuationPage(
        answers =>
          answers.get(PrTypePage).contains(PrType.Organisation) && answers
            .get(IndividualNamePage(PrOrganisation))
            .isEmpty,
        routes.IndividualNameController.onPageLoad(srn, NormalMode, PrOrganisation)
      ),
      ContinuationPage(
        answers =>
          answers.get(PrTypePage).contains(PrType.Organisation) && answers.get(PrOrganisationAddressPage).isEmpty,
        routes.AddressLookupStartController.start(srn, NormalMode, PrOrganisation)
      ),
      ContinuationPage(
        answers => answers.get(DidPrSubmitPage).isEmpty,
        routes.DidPrSubmitController.onPageLoad(srn, NormalMode)
      ),
      ContinuationPage(
        answers => answers.get(DidPrSubmitPage).get && answers.get(AreBeneficiariesKnownPage).isEmpty,
        routes.AreBeneficiariesKnownController.onPageLoad(srn, NormalMode)
      ),
      ContinuationPage(
        answers => answers.get(PaymentNoticeDatePage).isEmpty,
        routes.PaymentNoticeDateController.onPageLoad(srn, NormalMode)
      ),
      ContinuationPage(
        answers =>
          answers.get(AreBeneficiariesKnownPage).getOrElse(false) &&
            answers.get(pages.beneficiary.BeneficiaryTypePage(0)).isEmpty,
        controllers.beneficiary.routes.BeneficiaryTypeController.onPageLoad(srn, 0, NormalMode)
      )
    )

  private def getBeneficiaryPages(srn: Srn, i: Int) =
    Seq(
      ContinuationPage(
        answers =>
          answers.get(pages.beneficiary.BeneficiaryTypePage(i)).contains(BeneficiaryType.Individual) &&
            answers.get(pages.beneficiary.BeneficiaryNamePage(i, BeneficiaryIndividual)).isEmpty,
        controllers.beneficiary.routes.BeneficiaryNameController.onPageLoad(srn, NormalMode, i)
      ),
      ContinuationPage(
        answers =>
          answers.get(pages.beneficiary.BeneficiaryTypePage(i)).contains(BeneficiaryType.Trust) &&
            answers.get(pages.beneficiary.BeneficiaryTrustNamePage(i)).isEmpty,
        controllers.beneficiary.routes.BeneficiaryTrustNameController.onPageLoad(srn, i, NormalMode)
      ),
      ContinuationPage(
        answers =>
          answers.get(pages.beneficiary.BeneficiaryTypePage(i)).contains(BeneficiaryType.Individual) &&
            answers.get(pages.beneficiary.BeneficiaryHasNinoPage(i)).isEmpty,
        controllers.beneficiary.routes.BeneficiaryHasNinoController.onPageLoad(srn, i, NormalMode)
      )
    )

  private def getBeneficiariesPages(answers: UserAnswers, srn: Srn) = {
    val numberOfBeneficiaries =
      answers.get(pages.beneficiary.BeneficiariesPage()).map(_.beneficiaries.size).getOrElse(0)

    (0 until numberOfBeneficiaries).flatMap(i => getBeneficiaryPages(srn, i))
  }
}
