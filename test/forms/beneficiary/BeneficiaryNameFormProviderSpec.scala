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

import forms.beneficiary.BeneficiaryNameFormProvider
import models.{IndividualName, JourneyRole}
import play.api.data.FormError

class BeneficiaryNameFormProviderSpec extends forms.behaviours.StringFieldBehaviours {

  private val form = new BeneficiaryNameFormProvider()(JourneyRole.BeneficiaryIndividual)
  private val organisationForm = new BeneficiaryNameFormProvider()(JourneyRole.BeneficiaryOrganisation)

  "BeneficiaryNameFormProvider" - {

    "must bind valid data" in {

      val data = Map(
        "title" -> "Mr",
        "firstForename" -> "Firstname",
        "secondForename" -> "Middlename",
        "surname" -> "Surname"
      )

      val result = form.bind(data)

      result.errors mustBe empty
      result.value mustBe Some(
        IndividualName(
          title = Some("Mr"),
          firstForename = "Firstname",
          secondForename = Some("Middlename"),
          surname = "Surname"
        )
      )
    }

    "must fail when firstForename is blank" in {

      val result = form.bind(Map("firstForename" -> "", "surname" -> "Surname"))

      result.errors must contain(FormError("firstForename", "beneficiaryIndividualName.error.firstForename.required"))
    }

    "must fail when surname is blank" in {

      val result = form.bind(Map("firstForename" -> "Firstname", "surname" -> ""))

      result.errors must contain(FormError("surname", "beneficiaryIndividualName.error.surname.required"))
    }

    "must fail when fields exceed the maximum length" in {

      val result = form.bind(
        Map(
          "title" -> "Title",
          "firstForename" -> ("A" * 36),
          "secondForename" -> ("A" * 36),
          "surname" -> ("A" * 36)
        )
      )

      result.errors must contain(FormError("title", "beneficiaryIndividualName.error.title.length", Seq(4)))
      result.errors must contain(
        FormError("firstForename", "beneficiaryIndividualName.error.firstForename.length", Seq(35))
      )
      result.errors must contain(
        FormError("secondForename", "beneficiaryIndividualName.error.secondForename.length", Seq(35))
      )
      result.errors must contain(FormError("surname", "beneficiaryIndividualName.error.surname.length", Seq(35)))
    }

    "must fail when fields contain invalid characters" in {

      val result = form.bind(
        Map(
          "title" -> "M12",
          "firstForename" -> "Firstname1",
          "secondForename" -> "Middlename1",
          "surname" -> "Surname1"
        )
      )

      (result.errors.map(_.message) must contain).allOf(
        "beneficiaryIndividualName.error.title.pattern",
        "beneficiaryIndividualName.error.firstForename.pattern",
        "beneficiaryIndividualName.error.secondForename.pattern",
        "beneficiaryIndividualName.error.surname.pattern"
      )
    }

    "must only return the highest priority error for each field" in {

      val tooLongAndInvalidName = s"${"A" * 36}1"
      val result = form.bind(
        Map(
          "title" -> "Title1",
          "firstForename" -> tooLongAndInvalidName,
          "secondForename" -> tooLongAndInvalidName,
          "surname" -> tooLongAndInvalidName
        )
      )

      result.errors.map(error => error.key -> error.message) mustBe Seq(
        "title" -> "beneficiaryIndividualName.error.title.pattern",
        "firstForename" -> "beneficiaryIndividualName.error.firstForename.pattern",
        "secondForename" -> "beneficiaryIndividualName.error.secondForename.pattern",
        "surname" -> "beneficiaryIndividualName.error.surname.pattern"
      )
    }

    "must trim whitespace" in {

      val result = form.bind(
        Map(
          "title" -> " Mr ",
          "firstForename" -> " Firstname ",
          "secondForename" -> " Middlename ",
          "surname" -> " Surname "
        )
      )

      result.errors mustBe empty
      result.value mustBe Some(
        IndividualName(
          title = Some("Mr"),
          firstForename = "Firstname",
          secondForename = Some("Middlename"),
          surname = "Surname"
        )
      )
    }

    "must use organisation PR error keys when firstForename and surname are blank" in {

      val result = organisationForm.bind(Map("firstForename" -> "", "surname" -> ""))

      result.errors must contain(FormError("firstForename", "beneficiaryOrganisationName.error.firstForename.required"))
      result.errors must contain(FormError("surname", "beneficiaryOrganisationName.error.surname.required"))
    }

    "must use organisation PR error keys when fields exceed the maximum length" in {

      val result = organisationForm.bind(
        Map(
          "title" -> "Title",
          "firstForename" -> ("A" * 36),
          "secondForename" -> ("A" * 36),
          "surname" -> ("A" * 36)
        )
      )

      result.errors must contain(FormError("title", "beneficiaryOrganisationName.error.title.length", Seq(4)))
      result.errors must contain(
        FormError("firstForename", "beneficiaryOrganisationName.error.firstForename.length", Seq(35))
      )
      result.errors must contain(
        FormError("secondForename", "beneficiaryOrganisationName.error.secondForename.length", Seq(35))
      )
      result.errors must contain(FormError("surname", "beneficiaryOrganisationName.error.surname.length", Seq(35)))
    }

    "must use organisation PR error keys when fields contain invalid characters" in {

      val result = organisationForm.bind(
        Map(
          "title" -> "M12",
          "firstForename" -> "Firstname1",
          "secondForename" -> "Middlename1",
          "surname" -> "Surname1"
        )
      )

      (result.errors.map(_.message) must contain).allOf(
        "beneficiaryOrganisationName.error.title.pattern",
        "beneficiaryOrganisationName.error.firstForename.pattern",
        "beneficiaryOrganisationName.error.secondForename.pattern",
        "beneficiaryOrganisationName.error.surname.pattern"
      )
    }
  }
}
