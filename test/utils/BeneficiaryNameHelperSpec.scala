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

import org.scalatest.freespec.AnyFreeSpec
import base.SpecBase
import models.beneficiary.BeneficiaryType
import models.{IndividualName, JourneyRole}
import pages.beneficiary.{BeneficiaryNamePage, BeneficiaryOrganisationDetailsPage, BeneficiaryTypePage}

class BeneficiaryNameHelperSpec extends AnyFreeSpec with SpecBase {

  private val name = IndividualName(
    title = Some("Mr"),
    firstForename = "John",
    secondForename = Some("William"),
    surname = "Doe"
  )

  "fromUserAnswers" - {

    "must return the beneficiary's first name and surname when the name has been answered" in {
      val userAnswers = emptyUserAnswers
        .set(BeneficiaryNamePage(testIndex, JourneyRole.BeneficiaryIndividual), name)
        .success
        .value

      BeneficiaryNameHelper.fromUserAnswers(userAnswers, testIndex) mustBe Some("John Doe")
    }

    "must return None when the beneficiary name has not been answered" in {
      BeneficiaryNameHelper.fromUserAnswers(emptyUserAnswers, testIndex) mustBe None
    }

    "must return an organisation beneficiary's name" in {
      val userAnswers = emptyUserAnswers
        .set(BeneficiaryTypePage(testIndex), BeneficiaryType.Organisation)
        .success
        .value
        .set(BeneficiaryOrganisationDetailsPage(testIndex), beneficiaryOrganisationDetails)
        .success
        .value

      BeneficiaryNameHelper.fromUserAnswers(userAnswers, testIndex) mustBe Some(organisationName)
    }
  }

  "withName" - {

    "must run the success block when the beneficiary name has been answered" in {
      val userAnswers = emptyUserAnswers
        .set(BeneficiaryNamePage(testIndex, JourneyRole.BeneficiaryIndividual), name)
        .success
        .value

      BeneficiaryNameHelper.withName(userAnswers, testIndex)("missing")(name => s"found $name") mustBe
        "found John Doe"
    }

    "must run the fallback block when the beneficiary name has not been answered" in {
      BeneficiaryNameHelper.withName(emptyUserAnswers, testIndex)("missing")(name => s"found $name") mustBe "missing"
    }
  }
}
