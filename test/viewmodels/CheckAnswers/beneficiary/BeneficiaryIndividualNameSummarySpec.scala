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
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import models.{CheckMode, IndividualName, JourneyRole}
import pages.beneficiary.BeneficiaryNamePage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class BeneficiaryIndividualNameSummarySpec extends org.scalatest.freespec.AnyFreeSpec with SpecBase {

  "BeneficiaryIndividualNameSummary" - {
    implicit val messages: Messages = stubMessages()

    "must return None when data is not present" in {

      val result = BeneficiaryIndividualNameSummary.row(srn, testIndex, emptyUserAnswers)

      result mustBe None
    }

    "must return a row when data is present" in {

      val userAnswers = emptyUserAnswers
        .set(
          BeneficiaryNamePage(testIndex, JourneyRole.BeneficiaryIndividual),
          IndividualName(
            title = Some("Mr"),
            firstForename = "Firstname",
            secondForename = Some("Middlename"),
            surname = "Surname"
          )
        )
        .success
        .value

      val result = BeneficiaryIndividualNameSummary.row(srn, testIndex, userAnswers)

      result mustBe defined
      result.get.key.content mustBe Text(messages("beneficiaryIndividualName.checkYourAnswersLabel"))
      result.get.value.content mustBe HtmlContent("Mr Firstname Middlename Surname")
      result.get.actions.get.items.head.href mustBe
        controllers.beneficiary.routes.BeneficiaryNameController
          .onPageLoad(srn, CheckMode, testIndex)
          .url
    }
  }
}
