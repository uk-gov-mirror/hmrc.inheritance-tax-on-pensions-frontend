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

import pages.QuestionPage
import play.api.libs.json.JsPath
import models.beneficiary.BeneficiaryType
import models.{JourneyRole, UserAnswers}

import scala.util.Try

case class BeneficiaryTypePage(index: Int) extends QuestionPage[BeneficiaryType] {

  override def path: JsPath = (JsPath \ "beneficiaries")(index) \ toString

  override def toString: String = "beneficiaryType"

  override def cleanup(value: Option[BeneficiaryType], userAnswers: UserAnswers): Try[UserAnswers] =
    value match {
      case Some(BeneficiaryType.Organisation) =>
        for {
          withoutIndividualName <- userAnswers.remove(BeneficiaryNamePage(index, JourneyRole.BeneficiaryIndividual))
          withoutIndividualDetails <- withoutIndividualName.remove(BeneficiaryHasNinoPage(index))
        } yield withoutIndividualDetails
      case Some(BeneficiaryType.Individual) =>
        userAnswers.remove(BeneficiaryOrganisationDetailsPage(index))
      case _ => super.cleanup(value, userAnswers)
    }
}
