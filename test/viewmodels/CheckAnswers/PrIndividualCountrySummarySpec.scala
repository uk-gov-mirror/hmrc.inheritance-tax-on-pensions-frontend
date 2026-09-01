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

package viewmodels.CheckAnswers

import play.api.test.Helpers.stubMessages
import play.api.libs.json.Json
import models.{CheckMode, JourneyRole, PrAddress}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class PrIndividualCountrySummarySpec extends SpecBase {

  implicit val messages: Messages = stubMessages()

  "PrIndividualCountrySummary" - {

    "must return None when data is not present" in {
      PrIndividualCountrySummary.row(srn, emptyUserAnswers) mustBe None
    }

    "must show the country name and link to the address lookup journey" in {
      val address = PrAddress("1 Street Road", None, None, None, Some("AA1 1AA"), "GB")
      val userAnswers = emptyUserAnswers.copy(
        data = Json.obj(
          "prDetails" -> Json.obj(
            "individual" -> Json.toJson(address)
          )
        )
      )

      val result =
        PrIndividualCountrySummary.row(srn, userAnswers, _ => "United Kingdom").value

      result.key.content mustBe Text(messages("prIndividualCountry.checkYourAnswersLabel"))
      result.value.content mustBe Text("United Kingdom")
      result.actions.value.items.head.href mustBe
        controllers.routes.AddressLookupStartController
          .start(srn, CheckMode, JourneyRole.PrIndividual)
          .url
    }
  }
}
