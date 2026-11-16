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

import viewmodels.implicits._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import models.SchemeId.Srn
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import models.UserAnswers
import pages.beneficiary.BeneficiaryAddressPage
import play.api.i18n.Messages
import viewmodels.govuk.summarylist._

object BeneficiaryAddressSummary {
  def row(srn: Srn, index: Int, answers: UserAnswers)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    answers.get(BeneficiaryAddressPage(index)).map { answer =>
      val address = Seq(
        Some(answer.addressLine1),
        answer.addressLine2,
        answer.addressLine3,
        answer.addressLine4,
        answer.addressLine5,
        answer.postCode
      ).flatten
        .map(line => HtmlFormat.escape(line).toString)
        .mkString("<br>")

      SummaryListRowViewModel(
        key = "beneficiaryAddress.checkYourAnswersLabel",
        value = ValueViewModel(HtmlContent(HtmlFormat.raw(address))),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            controllers.beneficiary.routes.ChangeBeneficiaryAddressController.onPageLoad(srn, index).url
          ).withVisuallyHiddenText(
            messages(
              "beneficiaryAddress.change.hidden",
              utils.BeneficiaryNameHelper.fromUserAnswers(answers, index).getOrElse((index + 1).toString)
            )
          )
        )
      )
    }
}
