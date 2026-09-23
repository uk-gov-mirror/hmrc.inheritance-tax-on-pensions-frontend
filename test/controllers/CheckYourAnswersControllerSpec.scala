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

import play.api.test.Helpers._
import pages._
import viewmodels.CheckAnswers.beneficiary.{BeneficiaryHasNinoSummary, BeneficiaryTypeSummary}
import views.html.CheckYourAnswersView
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Actions, SummaryList}
import viewmodels.govuk.all.{ActionItemViewModel, CardViewModel, SummaryListViewModel}
import play.api.libs.json.Json
import models._
import viewmodels.CheckAnswers._
import models.JourneyRole.{Deceased, PrIndividual}
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

import scala.jdk.CollectionConverters._

import java.time.LocalDate

class CheckYourAnswersControllerSpec extends SpecBase {

  private val validNino: String = ninoGen.sample.value
  private val emptySummaryList = SummaryListViewModel(rows = Seq())
  private val emptyBeneficiarySummaryListViewModel = List[SummaryList]()

  private def onPageLoadFakeRequest =
    FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(srn).url)
  private def onPageLoadContinueFakeRequest =
    FakeRequest(GET, routes.CheckYourAnswersController.onPageLoadContinueMode(srn).url)

  "CheckYourAnswers Controller" - {
    "onPageLoad redirects to onPageLoadContinueMode when data is incomplete" in {
      val userAnswers = emptyUserAnswers
        .set(InheritanceTaxReferencePage, "A123456/25A")
        .success
        .value
        .set(
          IndividualNamePage(JourneyRole.Deceased),
          IndividualName(
            title = Some("Mr"),
            firstForename = "Firstname",
            secondForename = Some("Middlename"),
            surname = "Surname"
          )
        )
        .get

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          onPageLoadFakeRequest

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoadContinueMode(srn).url
      }
    }

    "onPageLoadContinueMode" - {
      Seq(PrType.Individual -> JourneyRole.PrIndividual, PrType.Organisation -> JourneyRole.PrOrganisation).foreach {
        case (prType, role) =>
          Seq(true, false).foreach { submittedByPr =>
            s"must display the updated payment notice summary for $prType with submittedByPr=$submittedByPr" in {
              val userAnswers = emptyUserAnswers
                .set(PrTypePage, prType)
                .success
                .value
                .set(IndividualNamePage(role), IndividualName(Some("Dr"), "Firstname", Some("Middlename"), "Surname"))
                .success
                .value
                .set(DidPrSubmitPage, submittedByPr)
                .success
                .value
              val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

              running(application) {
                val result =
                  route(
                    application,
                    onPageLoadContinueFakeRequest
                  ).value
                status(result) mustBe OK
                val document = org.jsoup.Jsoup.parse(contentAsString(result))
                val row = document
                  .select(".govuk-summary-list__row")
                  .asScala
                  .find(_.select(".govuk-summary-list__key").text() == "Who submitted the payment notice?")
                  .value
                row.select(".govuk-summary-list__value").text() mustBe (if (submittedByPr) "Firstname Surname"
                                                                        else "Someone else")
                row.select("a").attr("href") mustBe routes.DidPrSubmitController.onPageLoad(srn, CheckMode).url
                row.select("a").text() mustBe "Change who submitted the payment notice"
              }
            }
          }
      }

      "must show IHT payable directly below beneficiaries known with a Change link" in {
        val answers = emptyUserAnswers
          .set(AreBeneficiariesKnownPage, false)
          .success
          .value
          .set(IhtPayablePage, BigDecimal("1234.50"))
          .success
          .value
        val application = applicationBuilder(Some(answers)).build()
        running(application) {
          val result = route(
            application,
            onPageLoadContinueFakeRequest
          ).value
          status(result) mustBe OK
          val document = org.jsoup.Jsoup.parse(contentAsString(result))
          val rows = document.select(".govuk-summary-list__row")
          rows.size mustBe 2
          rows.get(0).select(".govuk-summary-list__key").text mustBe "Are the beneficiaries known?"
          val amountRow = rows.get(1)
          amountRow.select(".govuk-summary-list__key").text mustBe "Amount of IHT payable"
          amountRow.select(".govuk-summary-list__value").text mustBe "\u00a31,234.50"
          amountRow.select("a").attr("href") mustBe routes.IhtPayableController.onPageLoad(srn, CheckMode).url
          amountRow.select("a .govuk-visually-hidden").text mustBe "the amount of IHT payable"
        }
      }

      "must return OK and the correct view for a GET" in {
        val userAnswers = emptyUserAnswers
          .set(InheritanceTaxReferencePage, "A123456/25A")
          .success
          .value
          .set(
            IndividualNamePage(JourneyRole.Deceased),
            IndividualName(
              title = Some("Mr"),
              firstForename = "Firstname",
              secondForename = Some("Middlename"),
              surname = "Surname"
            )
          )
          .get
          .set(HasNinoPage, true)
          .get
          .set(NinoPage, validNino)
          .get
          .set(BirthDeathDatesPage, BirthDeathDates(testDateOfBirth, testDateOfDeath))
          .get
          .set(PrTypePage, PrType.Individual)
          .get
          .set(
            IndividualNamePage(JourneyRole.PrIndividual),
            IndividualName(
              title = Some("Mr"),
              firstForename = "Firstname",
              secondForename = Some("Middlename"),
              surname = "Surname"
            )
          )
          .get
          .set(DidPrSubmitPage, true)
          .get

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            onPageLoadContinueFakeRequest

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val deceasedDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              InheritanceTaxReferenceSummary.row(srn, userAnswers)(using messages(application)).get,
              NameOfDeceasedSummary.row(srn, userAnswers)(using messages(application)).get,
              HasNinoSummary.row(srn, userAnswers)(using messages(application)).get,
              NinoSummary.row(srn, userAnswers)(using messages(application)).get,
              BirthDeathDatesSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )
          val prDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              PrTypeSummary.row(srn, userAnswers)(using messages(application)).get,
              PrIndividualNameSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )
          val paymentNoticeDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              DidPrSubmitSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            srn,
            deceasedDetailsSummaryList,
            prDetailsSummaryList,
            paymentNoticeDetailsSummaryList,
            emptyBeneficiarySummaryListViewModel,
            Some(routes.AddressLookupStartController.start(srn = srn, mode = NormalMode, journeyRole = PrIndividual))
          )(using
            request,
            messages(application)
          ).toString
        }
      }

      "must return OK and the correct view for a GET with No Nino reason" in {
        val userAnswers = emptyUserAnswers
          .set(InheritanceTaxReferencePage, "A123456/25A")
          .success
          .value
          .set(
            IndividualNamePage(JourneyRole.Deceased),
            IndividualName(
              title = Some("Mr"),
              firstForename = "Firstname",
              secondForename = Some("Middlename"),
              surname = "Surname"
            )
          )
          .get
          .set(HasNinoPage, false)
          .get
          .set(NoNinoReasonPage, "Firstname Surname reason")
          .get
          .set(BirthDeathDatesPage, BirthDeathDates(testDateOfBirth, testDateOfDeath))
          .get
          .set(PrTypePage, PrType.Individual)
          .get
          .set(
            IndividualNamePage(JourneyRole.PrIndividual),
            IndividualName(
              title = Some("Mr"),
              firstForename = "Firstname",
              secondForename = Some("Middlename"),
              surname = "Surname"
            )
          )
          .get
          .set(DidPrSubmitPage, true)
          .get

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            onPageLoadContinueFakeRequest

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val deceasedDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              InheritanceTaxReferenceSummary.row(srn, userAnswers)(using messages(application)).get,
              NameOfDeceasedSummary.row(srn, userAnswers)(using messages(application)).get,
              HasNinoSummary.row(srn, userAnswers)(using messages(application)).get,
              NoNinoReasonSummary.row(srn, userAnswers)(using messages(application)).get,
              BirthDeathDatesSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )
          val prDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              PrTypeSummary.row(srn, userAnswers)(using messages(application)).get,
              PrIndividualNameSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )
          val paymentNoticeDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              DidPrSubmitSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            srn,
            deceasedDetailsSummaryList,
            prDetailsSummaryList,
            paymentNoticeDetailsSummaryList,
            emptyBeneficiarySummaryListViewModel,
            Some(routes.AddressLookupStartController.start(srn = srn, mode = NormalMode, journeyRole = PrIndividual))
          )(using
            request,
            messages(application)
          ).toString
        }
      }

      "must return OK and include the payment notice date row when present" in {
        val userAnswers = emptyUserAnswers
          .set(DidPrSubmitPage, false)
          .success
          .value
          .set(PaymentNoticeDatePage, LocalDate.of(2026, 3, 27))
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            onPageLoadContinueFakeRequest

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val deceasedDetailsSummaryList = emptySummaryList
          val prDetailsSummaryList = emptySummaryList
          val paymentNoticeDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              DidPrSubmitSummary.row(srn, userAnswers)(using messages(application)).get,
              PaymentNoticeDateSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            srn,
            deceasedDetailsSummaryList,
            prDetailsSummaryList,
            paymentNoticeDetailsSummaryList,
            emptyBeneficiarySummaryListViewModel,
            Some(routes.IndividualNameController.onPageLoad(srn = srn, mode = NormalMode, Deceased))
          )(using
            request,
            messages(application)
          ).toString
        }
      }

      "must return OK and the correct view for a GET with individual PR details and address" in {
        val userAnswers = emptyUserAnswers
          .copy(
            data = Json.obj(
              "prDetails" -> Json.obj(
                "individual" -> Json.obj(
                  "title" -> "Ms",
                  "firstForename" -> "Firstnametwo",
                  "secondForename" -> "Middlenametwo",
                  "surname" -> "Surname",
                  "addressline1" -> "33 Fake Street",
                  "addressline2" -> "AB Area",
                  "addressline3" -> "Some District",
                  "addressline4" -> "Anytown",
                  "ukPostcode" -> "ZZ1 1ZZ",
                  "country" -> "GB"
                )
              )
            )
          )
          .set(PrTypePage, PrType.Individual)
          .success
          .value
          .set(DidPrSubmitPage, true)
          .get

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            onPageLoadContinueFakeRequest

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val deceasedDetailsSummaryList = emptySummaryList
          val prDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              PrTypeSummary.row(srn, userAnswers)(using messages(application)).get,
              PrIndividualNameSummary.row(srn, userAnswers)(using messages(application)).get,
              PrIndividualCountrySummary
                .row(srn, userAnswers, (_: String) => "United Kingdom")(using messages(application))
                .get,
              PrIndividualAddressSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )
          val paymentNoticeDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              DidPrSubmitSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            srn,
            deceasedDetailsSummaryList,
            prDetailsSummaryList,
            paymentNoticeDetailsSummaryList,
            emptyBeneficiarySummaryListViewModel,
            Some(routes.IndividualNameController.onPageLoad(srn = srn, mode = NormalMode, Deceased))
          )(using
            request,
            messages(application)
          ).toString
        }
      }

      "must return OK and the correct view for a GET with organisation PR details and address" in {
        val userAnswers = emptyUserAnswers
          .copy(
            data = Json.obj(
              "prDetails" -> Json.obj(
                "organisation" -> Json.obj(
                  "organisationName" -> "Test Organisation",
                  "title" -> "Ms",
                  "firstForename" -> "Firstnametwo",
                  "secondForename" -> "Middlenametwo",
                  "surname" -> "Surname",
                  "addressline1" -> "33 Fake Street",
                  "addressline2" -> "AB Area",
                  "addressline3" -> "Some District",
                  "addressline4" -> "Anytown",
                  "ukPostcode" -> "ZZ1 1ZZ",
                  "country" -> "GB"
                )
              )
            )
          )
          .set(PrTypePage, PrType.Organisation)
          .success
          .value
          .set(DidPrSubmitPage, true)
          .get

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            onPageLoadContinueFakeRequest

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val deceasedDetailsSummaryList = emptySummaryList
          val prDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              PrTypeSummary.row(srn, userAnswers)(using messages(application)).get,
              PrOrganisationNameSummary.row(srn, userAnswers)(using messages(application)).get,
              PrOrganisationPrNameSummary.row(srn, userAnswers)(using messages(application)).get,
              PrOrganisationCountrySummary
                .row(srn, userAnswers, (_: String) => "United Kingdom")(using messages(application))
                .get,
              PrOrganisationAddressSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )
          val paymentNoticeDetailsSummaryList = SummaryListViewModel(
            rows = Seq(
              DidPrSubmitSummary.row(srn, userAnswers)(using messages(application)).get
            )
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            srn,
            deceasedDetailsSummaryList,
            prDetailsSummaryList,
            paymentNoticeDetailsSummaryList,
            emptyBeneficiarySummaryListViewModel,
            Some(routes.IndividualNameController.onPageLoad(srn = srn, mode = NormalMode, Deceased))
          )(using
            request,
            messages(application)
          ).toString
        }
      }

      "must return OK and view with beneficiaries for a GET" in {
        val userAnswers = emptyUserAnswers
          .copy(
            data = Json.obj(
              "beneficiaries" -> Json.arr(
                Json.obj(
                  "beneficiaryType" -> "individual",
                  "hasNino" -> true
                ),
                Json.obj(
                  "beneficiaryType" -> "individual"
                )
              )
            )
          )

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            onPageLoadContinueFakeRequest

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val beneficiarySummaryList = List(
            SummaryListViewModel(
              rows = Seq(
                BeneficiaryTypeSummary.row(srn, 0, userAnswers)(using messages(application)).get,
                BeneficiaryHasNinoSummary.row(srn, 0, userAnswers)(using messages(application)).get
              ),
              card = CardViewModel(
                "Beneficiary 1",
                2,
                Some(
                  Actions(
                    items = Seq(
                      ActionItemViewModel(
                        Text("Remove"),
                        s"""/inheritance-tax-on-pensions/${srn.value}/change-remove-beneficiary/0"""
                      ).copy(visuallyHiddenText = Some("0"))
                    )
                  )
                )
              )
            ),
            SummaryListViewModel(
              rows = Seq(
                BeneficiaryTypeSummary.row(srn, 1, userAnswers)(using messages(application)).get
              ),
              card = CardViewModel(
                "Beneficiary 2",
                2,
                Some(
                  Actions(
                    items = Seq(
                      ActionItemViewModel(
                        Text("Remove"),
                        s"""/inheritance-tax-on-pensions/${srn.value}/change-remove-beneficiary/1"""
                      ).copy(visuallyHiddenText = Some("1"))
                    )
                  )
                )
              )
            )
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            srn,
            emptySummaryList,
            emptySummaryList,
            emptySummaryList,
            beneficiarySummaryList,
            Some(routes.IndividualNameController.onPageLoad(srn = srn, mode = NormalMode, Deceased))
          )(using
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            onPageLoadContinueFakeRequest

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must return OK when UUID is not in query parameter" in {

        val userAnswers = emptyUserAnswers
          .set(InheritanceTaxReferencePage, "A123456/25A")
          .success
          .value
          .set(
            IndividualNamePage(JourneyRole.Deceased),
            IndividualName(
              title = Some("Mr"),
              firstForename = "Firstname",
              secondForename = Some("Middlename"),
              surname = "Surname"
            )
          )
          .get
          .set(HasNinoPage, true)
          .get
          .set(NinoPage, validNino)
          .get
          .set(BirthDeathDatesPage, BirthDeathDates(testDateOfBirth, testDateOfDeath))
          .get
          .set(PrTypePage, PrType.Individual)
          .get
          .set(
            IndividualNamePage(JourneyRole.PrIndividual),
            IndividualName(
              title = Some("Mr"),
              firstForename = "Firstname",
              secondForename = Some("Middlename"),
              surname = "Surname"
            )
          )
          .get
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            onPageLoadContinueFakeRequest

          val result = route(application, request).value

          status(result) mustEqual OK
        }
      }
    }
    "onSubmit" - {
      "must redirect to the psa confirmation page on post for psa" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(srn).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.PsaDeclarationController.onPageLoad(srn).url
        }
      }

      "must redirect to the psp confirmation page on post for psp" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), isPsa = false).build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(srn).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.PspDeclarationController.onPageLoad(srn).url
        }
      }
    }
  }
}
