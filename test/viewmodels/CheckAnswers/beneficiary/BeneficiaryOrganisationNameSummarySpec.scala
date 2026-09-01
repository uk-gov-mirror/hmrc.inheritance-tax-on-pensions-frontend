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

package viewmodels.CheckAnswers.beneficiary

import play.api.test.Helpers.stubMessages
import models.CheckMode
import pages.beneficiary.BeneficiaryOrganisationDetailsPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class BeneficiaryOrganisationNameSummarySpec extends org.scalatest.freespec.AnyFreeSpec with SpecBase {

  "BeneficiaryOrganisationNameSummary" - {
    implicit val messages: Messages = stubMessages()

    "must return None when data is not present" in {
      BeneficiaryOrganisationNameSummary.row(srn, testIndex, emptyUserAnswers) mustBe None
    }

    "must return the organisation or trust name" in {
      val userAnswers = emptyUserAnswers
        .set(BeneficiaryOrganisationDetailsPage(testIndex), beneficiaryOrganisationDetails)
        .success
        .value

      val result = BeneficiaryOrganisationNameSummary.row(srn, testIndex, userAnswers).value

      result.key.content mustBe Text(messages("beneficiaryOrganisationDetails.name.checkYourAnswersLabel"))
      result.value.content mustBe Text(beneficiaryOrganisationDetails.beneficiaryTrstName)
      result.actions.value.items.head.href mustBe
        controllers.beneficiary.routes.BeneficiaryOrganisationDetailsController
          .onPageLoad(srn, testIndex, CheckMode)
          .url
    }
  }
}
