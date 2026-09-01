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
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import play.api.libs.json.Json
import models.{JourneyRole, PrAddress}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class PrOrganisationAddressSummarySpec extends SpecBase {

  "PrOrganisationAddressSummary" - {
    implicit val messages: Messages = stubMessages()

    "must return None when data is not present" in {

      PrOrganisationAddressSummary.row(srn, emptyUserAnswers) mustBe None
    }

    "must return a row when data is present" in {

      val address = PrAddress(
        addressLine1 = "33 AB Street",
        addressLine2 = Some("AB Area"),
        addressLine3 = Some("AB County"),
        addressLine4 = Some("ABville"),
        ukPostcode = Some("AA1 1AA"),
        country = "GB"
      )

      val userAnswers = emptyUserAnswers.copy(
        data = Json.obj(
          "prDetails" -> Json.obj(
            "organisation" -> Json.toJson(address)
          )
        )
      )

      val result = PrOrganisationAddressSummary.row(srn, userAnswers)

      result mustBe defined
      result.value.key.content mustBe Text(messages("prOrganisationAddress.checkYourAnswersLabel"))
      result.value.value.content mustBe HtmlContent(
        "33 AB Street<br>AB Area<br>AB County<br>ABville<br>AA1 1AA"
      )
      result.value.actions.value.items.head.href mustBe
        controllers.routes.ChangePrAddressController.onPageLoad(srn, JourneyRole.PrOrganisation).url
    }

    "must show only address line 1 when the optional address fields are absent" in {

      val address = PrAddress(
        addressLine1 = "33 AB Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        ukPostcode = None,
        country = "GB"
      )

      val userAnswers = emptyUserAnswers.copy(
        data = Json.obj(
          "prDetails" -> Json.obj(
            "organisation" -> Json.toJson(address)
          )
        )
      )

      val result = PrOrganisationAddressSummary.row(srn, userAnswers)

      result mustBe defined
      result.value.value.content mustBe HtmlContent("33 AB Street")
    }

  }
}
