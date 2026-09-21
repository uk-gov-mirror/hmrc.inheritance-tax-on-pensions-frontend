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

import models.JourneyRole.{Deceased, PrIndividual}
import org.scalatest.freespec.AnyFreeSpec
import pages.*
import controllers.routes
import base.SpecBase
import play.api.libs.json.Json
import models.beneficiary.BeneficiaryType
import models.*

class CheckYourAnswersHelperSpec extends AnyFreeSpec with SpecBase {

  "findPageToContinue" - {
    val baseUserAnswers = emptyUserAnswers
      .set(InheritanceTaxReferencePage, inheritanceTaxReference)
      .get
      .set(IndividualNamePage(Deceased), individualName)
      .get

    val deceasedPrCompleteUserAnswers =
      emptyUserAnswers // TODO make this complete and remove individual elements - write more tests
        .copy(
          data = Json.obj(
            "inheritanceTaxReference" -> "F123456/25A",
            "nameOfDeceased" -> Json.obj(
              "firstForename" -> "dec",
              "surname" -> "name"
            ),
            "hasNino" -> false,
            "reasonForNoNino" -> "no nino",
            "birthDeathDates" -> Json.obj(
              "dateOfBirth" -> "1920-01-01",
              "dateOfDeath" -> "2026-01-01"
            ),
            "prType" -> "individual",
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

    "must return the hasNino page if a value is missing" in {

      CheckYourAnswersHelper.findPageToContinue(baseUserAnswers, srn).value.url must endWith(
        routes.HasNinoController.onPageLoad(srn, NormalMode).url
      )
    }

    "must return the nino page if a value is missing and hasNino is true" in {
      val userAnswers = baseUserAnswers
        .set(HasNinoPage, true)
        .success
        .value

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.NinoController.onPageLoad(srn, NormalMode).url
      )
    }

    "must return the noNinoReason page if a value is missing and hasNino is false" in {
      val userAnswers = baseUserAnswers
        .set(HasNinoPage, false)
        .success
        .value

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.NoNinoReasonController.onPageLoad(srn, NormalMode).url
      )
    }

    "must return the individualName page if a value is missing" in {
      val userAnswers = baseUserAnswers
        .set(HasNinoPage, false)
        .success
        .value
        .set(NoNinoReasonPage, "No Nino Reason")
        .get
        .set(BirthDeathDatesPage, BirthDeathDates(testDateOfBirth, testDateOfDeath))
        .get
        .set(PrTypePage, PrType.Individual)
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        routes.IndividualNameController.onPageLoad(srn, NormalMode, PrIndividual).url
      )
    }

    "must return the first beneficiary type page if are beneficiaries are know is yes and type of first is missing" in {
      val userAnswers = deceasedPrCompleteUserAnswers
        .set(DidPrSubmitPage, true)
        .get
        .set(PaymentNoticeDatePage, testPaymentNoticeDate)
        .get
        .set(AreBeneficiariesKnownPage, true)
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        controllers.beneficiary.routes.BeneficiaryTypeController.onPageLoad(srn, 0, NormalMode).url
      )
    }

    "must return the BeneficiaryName page if a value is missing" in {
      val userAnswers = deceasedPrCompleteUserAnswers
        .set(AreBeneficiariesKnownPage, true)
        .get
        .set(pages.beneficiary.BeneficiaryTypePage(0), BeneficiaryType.Individual)
        .get
        .set(PrTypePage, PrType.Individual)
        .get
        .set(DidPrSubmitPage, true)
        .get
        .set(PaymentNoticeDatePage, testPaymentNoticeDate)
        .get

      CheckYourAnswersHelper.findPageToContinue(userAnswers, srn).value.url must endWith(
        controllers.beneficiary.routes.BeneficiaryNameController.onPageLoad(srn, NormalMode, 0).url
      )
    }
  }
}
