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

import models.beneficiary.BeneficiaryType
import models.{IndividualName, JourneyRole, UserAnswers}
import pages.beneficiary.{BeneficiaryNamePage, BeneficiaryOrganisationDetailsPage, BeneficiaryTypePage}

object BeneficiaryNameHelper {

  def fromUserAnswers(userAnswers: UserAnswers, index: Int): Option[String] =
    userAnswers.get(BeneficiaryTypePage(index)) match {
      case Some(BeneficiaryType.Organisation) =>
        userAnswers
          .get(BeneficiaryOrganisationDetailsPage(index))
          .map(_.beneficiaryTrstName)
      case _ =>
        userAnswers
          .get(BeneficiaryNamePage(index, JourneyRole.BeneficiaryIndividual))
          .map(displayName)
    }

  def withName[A](userAnswers: UserAnswers, index: Int)(ifMissing: => A)(f: String => A): A =
    fromUserAnswers(userAnswers, index).fold(ifMissing)(f)

  def displayName(name: IndividualName): String =
    s"${name.firstForename} ${name.surname}"
}
