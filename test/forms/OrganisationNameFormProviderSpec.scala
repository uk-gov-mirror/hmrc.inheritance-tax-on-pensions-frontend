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

package forms

import forms.mappings.Regex
import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class OrganisationNameFormProviderSpec extends StringFieldBehaviours with Regex {

  val requiredKey = "organisationName.error.required"
  val lengthKey = "organisationName.error.length"
  val invalidKey = "organisationName.error.invalid"
  val maxLength = 160

  val form = new OrganisationNameFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave.like(
      fieldThatBindsValidData(
        form,
        fieldName,
        "Org & Son Ltd. (and so)"
      )
    )

    "must fail when value exceeds the maximum length" in {
      val result = form.bind(Map("value" -> ("A" * (maxLength + 1))))
      result.errors must contain(FormError("value", lengthKey, Seq(maxLength)))
    }

    behave.like(
      mandatoryField(
        form,
        fieldName,
        requiredError = FormError(fieldName, requiredKey)
      )
    )

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

        result.errors must contain(FormError("value", invalidKey, Seq(orgAndTrustNameRegex)))
      }
    }
  }
}
