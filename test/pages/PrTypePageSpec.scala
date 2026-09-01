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

package pages

import base.SpecBase
import play.api.libs.json.{JsPath, Json}
import models.{IndividualName, JourneyRole, PrType}

class PrTypePageSpec extends SpecBase {

  "PrTypePage" - {

    "must use the correct path" in {
      PrTypePage.path mustEqual JsPath \ "prType"
    }

    "must use the correct page name" in {
      PrTypePage.toString mustEqual "prType"
    }

    "must call super.cleanup when value is None" in {
      val userAnswers = emptyUserAnswers

      val result = PrTypePage.cleanup(None, userAnswers)

      result.isSuccess mustBe true
    }

    "must remove individual PR details when Organisation is selected" in {
      val userAnswers = emptyUserAnswers
        .set(IndividualNamePage(JourneyRole.PrIndividual), IndividualName(Some("Mr"), "Firstname", None, "Surname"))
        .success
        .value

      val result = PrTypePage.cleanup(Some(PrType.Organisation), userAnswers).success.value

      result.get(IndividualNamePage(JourneyRole.PrIndividual)) mustBe None
    }

    "must remove organisation PR details when Individual is selected" in {
      val userAnswers = emptyUserAnswers.copy(
        data = Json.obj(
          "prDetails" -> Json.obj(
            "organisation" -> Json.obj(
              "organisationName" -> "Test Organisation",
              "title" -> "Mr",
              "firstForename" -> "Firstname",
              "surname" -> "Surname"
            )
          )
        )
      )

      val result = PrTypePage.cleanup(Some(PrType.Individual), userAnswers).success.value

      result.get(OrganisationNamePage) mustBe None
      result.get(IndividualNamePage(JourneyRole.PrOrganisation)) mustBe None
    }
  }
}
