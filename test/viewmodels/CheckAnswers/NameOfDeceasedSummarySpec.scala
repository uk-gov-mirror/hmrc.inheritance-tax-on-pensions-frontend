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

import pages.IndividualNamePage
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import models.{CheckMode, IndividualName, JourneyRole}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import org.scalatest.freespec.AnyFreeSpec
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import base.SpecBase

class NameOfDeceasedSummarySpec extends AnyFreeSpec with SpecBase {

  "NameOfDeceasedSummary" - {
    implicit val messages: Messages = stubMessages()

    "must return None when data is not present" in {

      val userAnswers = emptyUserAnswers

      val result = NameOfDeceasedSummary.row(srn, userAnswers)

      result mustBe None
    }

    "must return a row when data is present with all fields" in {

      val userAnswers = emptyUserAnswers
        .set(
          IndividualNamePage(JourneyRole.Deceased),
          IndividualName(
            title = Some("Mr"),
            firstForename = "Firstname",
            secondForename = Some("Middlename"),
            surname = "Surname"
          )
        )
        .success
        .value

      val result = NameOfDeceasedSummary.row(srn, userAnswers)

      result mustBe defined
      result.get.key.content mustBe Text(messages("nameOfDeceased.checkYourAnswersLabel"))
      result.get.value.content mustBe HtmlContent("Mr Firstname Middlename Surname")
      result.get.actions.get.items.head.href mustBe
        controllers.routes.IndividualNameController.onPageLoad(srn, CheckMode, JourneyRole.Deceased).url
    }

    "must format full name correctly with all fields" in {

      val userAnswers = emptyUserAnswers
        .set(
          IndividualNamePage(JourneyRole.Deceased),
          IndividualName(
            title = Some("Mr"),
            firstForename = "Firstname",
            secondForename = Some("Middlename"),
            surname = "Surname"
          )
        )
        .success
        .value

      val result = NameOfDeceasedSummary.row(srn, userAnswers)

      result mustBe defined
      result.get.value.content mustBe HtmlContent("Mr Firstname Middlename Surname")
    }

    "must format full name correctly without title" in {

      val userAnswers = emptyUserAnswers
        .set(
          IndividualNamePage(JourneyRole.Deceased),
          IndividualName(
            title = None,
            firstForename = "Firstname",
            secondForename = Some("Middlename"),
            surname = "Surname"
          )
        )
        .success
        .value

      val result = NameOfDeceasedSummary.row(srn, userAnswers)

      result mustBe defined
      result.get.value.content mustBe HtmlContent("Firstname Middlename Surname")
    }

    "must format full name correctly without middle names" in {

      val userAnswers = emptyUserAnswers
        .set(
          IndividualNamePage(JourneyRole.Deceased),
          IndividualName(
            title = Some("Mr"),
            firstForename = "Firstname",
            secondForename = None,
            surname = "Surname"
          )
        )
        .success
        .value

      val result = NameOfDeceasedSummary.row(srn, userAnswers)

      result mustBe defined
      result.get.value.content mustBe HtmlContent("Mr Firstname Surname")
    }

    "must format full name correctly with only required fields" in {

      val userAnswers = emptyUserAnswers
        .set(
          IndividualNamePage(JourneyRole.Deceased),
          IndividualName(
            title = None,
            firstForename = "Firstname",
            secondForename = None,
            surname = "Surname"
          )
        )
        .success
        .value

      val result = NameOfDeceasedSummary.row(srn, userAnswers)

      result mustBe defined
      result.get.value.content mustBe HtmlContent("Firstname Surname")
    }
  }
}
