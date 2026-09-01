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

package pages.beneficiary

import base.SpecBase
import play.api.libs.json.JsPath
import models.beneficiary.BeneficiaryType
import models.JourneyRole

class BeneficiaryTypePageSpec extends SpecBase {

  "BeneficiaryTypePage" - {

    "must use the correct path" in {
      BeneficiaryTypePage(testIndex).path mustEqual (JsPath \ "beneficiaries")(testIndex) \ "beneficiaryType"
    }

    "must use the correct page name" in {
      BeneficiaryTypePage(testIndex).toString mustEqual "beneficiaryType"
    }

    "must remove individual beneficiary details when Organisation is selected" in {
      val userAnswers = emptyUserAnswers
        .set(BeneficiaryNamePage(testIndex, JourneyRole.BeneficiaryIndividual), individualName)
        .success
        .value
        .set(BeneficiaryHasNinoPage(testIndex), true)
        .success
        .value

      val updatedAnswers = userAnswers.set(BeneficiaryTypePage(testIndex), BeneficiaryType.Organisation).success.value

      updatedAnswers.get(BeneficiaryNamePage(testIndex, JourneyRole.BeneficiaryIndividual)) mustBe None
      updatedAnswers.get(BeneficiaryHasNinoPage(testIndex)) mustBe None
    }

    "must remove organisation beneficiary details when Individual is selected" in {
      val userAnswers = emptyUserAnswers
        .set(BeneficiaryOrganisationDetailsPage(testIndex), beneficiaryOrganisationDetails)
        .success
        .value

      val updatedAnswers = userAnswers.set(BeneficiaryTypePage(testIndex), BeneficiaryType.Individual).success.value

      updatedAnswers.get(BeneficiaryOrganisationDetailsPage(testIndex)) mustBe None
    }

  }
}
