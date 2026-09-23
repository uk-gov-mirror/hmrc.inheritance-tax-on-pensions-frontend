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
import forms.mappings.Regex
import play.api.data.FormError

class BeneficiaryTrustNameFormProviderSpec extends SpecBase with Regex {

  private val form = new BeneficiaryTrustNameFormProvider()()

  private val validData = Map("value" -> trustName)

  "BeneficiaryTrustNameFormProvider" - {

    "must bind valid data" in {
      form.bind(validData).value.value mustEqual trustName
    }

    "must accept punctuation and ampersand in the name" in {
      val name = s"$trustName & Co."
      val result = form.bind(Map("value" -> name))

      result.value.value mustEqual name
    }

    "must reject a blank organisation or trust name" in {
      val result = form.bind(validData.updated("value", ""))

      result.errors must contain(
        FormError("value", "beneficiaryTrustName.error.required")
      )
    }

    "must reject an organisation or trust name over 160 characters" in {
      val result = form.bind(validData.updated("value", "a" * 161))

      result.errors must contain(
        FormError("value", "beneficiaryTrustName.error.length", Seq(160))
      )
    }

    Seq(
      "%" -> "percent sign",
      "$" -> "dollar sign",
      "£" -> "pound sign",
      "a\rb" -> "carriage return",
      "a\nb" -> "newline",
      "\"" -> "quote"
    ).foreach { case (invalidCharacter, description) =>
      s"must reject a $description" in {
        val result = form.bind(Map("value" -> invalidCharacter))

        result.errors must contain(FormError("value", "beneficiaryTrustName.error.invalid", Seq(orgAndTrustNameRegex)))
      }
    }
  }
}
