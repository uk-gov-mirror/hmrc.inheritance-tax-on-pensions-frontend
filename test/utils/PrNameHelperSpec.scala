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
import pages.{IndividualNamePage, PrTypePage}
import base.SpecBase
import utils.PrNameHelper.fromUserAnswers
import models.{IndividualName, JourneyRole, PrType}

class PrNameHelperSpec extends AnyFreeSpec with SpecBase {

  "when inidividual" - {
    "fromUserAnswers" - {

      "must return the pr first name and surname if inividual when the name has been answered" in {

        val userAnswers = emptyUserAnswers
          .set(PrTypePage, PrType.Individual)
          .get
          .set(
            IndividualNamePage(JourneyRole.PrIndividual),
            IndividualName(
              title = Some("Dr"),
              firstForename = "Firstname",
              secondForename = Some("Middlename"),
              surname = "Surname"
            )
          )
          .get

        fromUserAnswers(userAnswers) mustBe Some("Firstname Surname")
      }
      "must return None when the PR name has not been answered" in {

        fromUserAnswers(emptyUserAnswers) mustBe None
      }
    }

    "withName" - {

      "must run the success block when the PR name has been answered" in {

        val userAnswers = emptyUserAnswers
          .set(
            IndividualNamePage(JourneyRole.PrIndividual),
            IndividualName(
              title = Some("Mr"),
              firstForename = "Firstname",
              secondForename = Some("Middlename"),
              surname = "Surname"
            )
          )
          .success
          .value

        PrNameHelper.withName(userAnswers)("missing")(name => s"found $name") mustBe "found Firstname Surname"
      }

      "must run the fallback block when the individual name has not been answered" in {

        PrNameHelper.withName(emptyUserAnswers)("missing")(name => s"found $name") mustBe "missing"
      }
    }
  }
  "when organisation" - {
    "fromUserAnswers" - {
      "must return the PR first name and surname when the organisation PR name has been answered" in {
        val userAnswers = emptyUserAnswers
          .set(PrTypePage, PrType.Organisation)
          .get
          .set(
            IndividualNamePage(JourneyRole.PrOrganisation),
            IndividualName(Some("Mrs"), "Firstnamethree", Some("Middlenametwo"), "Surnametwo")
          )
          .get

        fromUserAnswers(userAnswers) mustBe Some("Firstnamethree Surnametwo")
      }

      "must return None when the organisation PR name has not been answered" in {
        val userAnswers = emptyUserAnswers.set(PrTypePage, PrType.Organisation).get

        fromUserAnswers(userAnswers) mustBe None
      }
    }
  }
}
