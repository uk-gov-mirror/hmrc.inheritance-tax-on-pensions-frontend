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

package base

import generators.Generators
import models.beneficiary.BeneficiaryOrganisationDetails
import models._

import java.time.LocalDate

trait TestValues extends Generators {
  val srn: SchemeId.Srn = srnGen.sample.get
  val schemeName = "testSchemeName"
  val email = "testEmail@test.com"
  val paymentReference = "A123456/25A629671"
  val testDateOfBirth: LocalDate = LocalDate.of(1950, 1, 1)
  val testDateOfDeath: LocalDate = LocalDate.of(2020, 1, 1)
  val testPaymentNoticeDate: LocalDate = LocalDate.of(2026, 2, 2)
  val testIndex: Int = 0
  val testInvalidBeneficiaryIndexes: List[Int] = List(-1, 30)
  val testUuid = "test-uuid"

  val defaultSchemeDetails: SchemeDetails = SchemeDetails(
    schemeName = schemeName,
    pstr = "testPSTR",
    schemeStatus = SchemeStatus.Open,
    schemeType = "testSchemeType",
    authorisingPSAID = Some("A1234567"),
    establishers = List(Establisher(SensitiveString("testFirstName testLastName"), EstablisherKind.Individual))
  )

  val individualDetails: IndividualDetails = IndividualDetails("testFirstName", Some("testMiddleName"), "testLastName")

  val defaultMinimalDetails: MinimalDetails = MinimalDetails(
    SensitiveString(email),
    isPsaSuspended = false,
    None,
    Some(
      SensitiveIndividualDetails(
        SensitiveString(individualDetails.firstName),
        individualDetails.middleName.map(SensitiveString(_)),
        SensitiveString(individualDetails.lastName)
      )
    ),
    rlsFlag = false,
    deceasedFlag = false
  )
  val individualName: IndividualName = IndividualName(
    title = Some("Mr"),
    firstForename = "Firstname",
    secondForename = Some("Middlename"),
    surname = "Lastname"
  )
  val organisationName = "Testdata Company Ltd"
  val beneficiaryHmrcReferenceNumber = "K1234567890"
  val beneficiaryOrganisationDetails: BeneficiaryOrganisationDetails =
    BeneficiaryOrganisationDetails(organisationName, beneficiaryHmrcReferenceNumber)
  val testPrAddress: PrAddress =
    PrAddress("1 ABCDE Street", None, None, Some("FGHIJ Town"), Some("AA1 1AA"), "GB")
  val individualNameFormatted: String = s"${individualName.firstForename} ${individualName.surname}"

}
