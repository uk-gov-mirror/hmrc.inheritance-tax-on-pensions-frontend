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

package forms.beneficiary

import base.SpecBase
import play.api.data.FormError

class BeneficiaryOrganisationDetailsFormProviderSpec extends SpecBase {

  private val form = new BeneficiaryOrganisationDetailsFormProvider()()

  private val validData = Map(
    "beneficiaryTrstName" -> organisationName,
    "hmrcReferenceNumber" -> beneficiaryOrganisationDetails.hmrcReferenceNumber
  )

  "BeneficiaryOrganisationDetailsFormProvider" - {

    "must bind valid data" in {
      form.bind(validData).value.value mustEqual beneficiaryOrganisationDetails
    }

    "must accept punctuation in the name and preserve the HMRC reference" in {
      val name = s"$organisationName %.$$£,"
      val reference = " Any reference %.$£ "
      val result = form.bind(
        Map(
          "beneficiaryTrstName" -> name,
          "hmrcReferenceNumber" -> reference
        )
      )

      result.value.value.beneficiaryTrstName mustEqual name
      result.value.value.hmrcReferenceNumber mustEqual reference
    }

    "must reject a blank organisation or trust name" in {
      val result = form.bind(validData.updated("beneficiaryTrstName", ""))

      result.errors must contain(
        FormError("beneficiaryTrstName", "beneficiaryOrganisationDetails.error.name.required")
      )
    }

    "must reject an organisation or trust name over 160 characters" in {
      val result = form.bind(validData.updated("beneficiaryTrstName", "a" * 161))

      result.errors must contain(
        FormError("beneficiaryTrstName", "beneficiaryOrganisationDetails.error.name.length", Seq(160))
      )
    }

    "must reject a blank HMRC reference number" in {
      val result = form.bind(validData.updated("hmrcReferenceNumber", ""))

      result.errors must contain(
        FormError("hmrcReferenceNumber", "beneficiaryOrganisationDetails.error.hmrcReference.required")
      )
    }

  }
}
