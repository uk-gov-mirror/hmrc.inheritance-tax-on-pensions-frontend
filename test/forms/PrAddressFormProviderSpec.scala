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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import forms.mappings.Regex
import models.PrAddress
import play.api.data.FormError

class PrAddressFormProviderSpec extends AnyFreeSpec with Matchers with Regex {

  private val country = "GB"
  private val nonGbCountry = "BF"
  private val form = new PrAddressFormProvider()(country)
  private val formNonGb = new PrAddressFormProvider()(nonGbCountry)
  private val validData = Map(
    "addressline1" -> "Line 1",
    "addressline2" -> "Line 2",
    "addressline3" -> "City",
    "addressline4" -> "",
    "addressline5" -> "Line 5",
    "ukPostcode" -> ""
  )
  "PrAddressFormProvider" - {

    "must bind valid data, trim the fields and retain the existing country" in {
      val result = form.bind(
        validData.updated("addressline1", "  Line 1  ").updated("addressline4", "   ")
      )

      result.errors mustBe empty
      result.value.get mustBe PrAddress(
        addressline1 = "Line 1",
        addressline2 = Some("Line 2"),
        addressline3 = Some("City"),
        addressline4 = None,
        addressline5 = Some("Line 5"),
        ukPostcode = None,
        country = country
      )
    }
    "must bind valid data, trim the fields and retain the existing Non-GB country" in {
      val result = formNonGb.bind(
        validData.updated("addressline1", "  Line 1  ").updated("addressline4", "   ")
      )

      result.errors mustBe empty
      result.value.get mustBe PrAddress(
        addressline1 = "Line 1",
        addressline2 = Some("Line 2"),
        addressline3 = Some("City"),
        addressline4 = None,
        addressline5 = Some("Line 5"),
        ukPostcode = None,
        country = nonGbCountry
      )
    }

    "must require address line 1" in {
      val result = form.bind(validData.updated("addressline1", "   "))

      result.errors must contain(
        FormError("addressline1", "changePrAddress.error.addressline1.required")
      )
    }

    "must accept characters other than percent, dollar and pound signs" in {
      val result = form.bind(validData.updated("addressline1", "Flat #2: Rear @ Block_B; [A]?"))

      result.errors mustBe empty
    }

    Seq(
      ("addressline1", "changePrAddress.error.addressline1.invalid"),
      ("addressline2", "changePrAddress.error.addressline2.invalid"),
      ("addressline3", "changePrAddress.error.addressline3.invalid"),
      ("addressline4", "changePrAddress.error.addressline4.invalid")
    ).foreach { case (field, errorKey) =>
      Seq(
        "%" -> "percent sign",
        "$" -> "dollar sign",
        "£" -> "pound sign",
        "\r" -> "carriage return",
        "\n" -> "newline",
        "\"" -> "quote",
        "&" -> "ampersand"
      ).foreach { case (invalidCharacter, description) =>
        s"must reject a $description in $field" in {
          val result = form.bind(validData.updated(field, s"Invalid${invalidCharacter}Value"))

          result.errors must contain(FormError(field, errorKey, Seq(addresslineRegex)))
        }
      }
    }

    Seq(
      ("addressline1", "changePrAddress.error.addressline1.length"),
      ("addressline2", "changePrAddress.error.addressline2.length"),
      ("addressline3", "changePrAddress.error.addressline3.length"),
      ("addressline4", "changePrAddress.error.addressline4.length")
    ).foreach { case (field, errorKey) =>
      s"must reject $field when it is longer than 35 characters" in {
        val result = form.bind(validData.updated(field, "A" * 36))

        result.errors must contain(FormError(field, errorKey, Seq(35)))
      }
    }

    "must show only the higher-priority invalid-format error for one field" in {
      val result = form.bind(validData.updated("addressline1", "%" * 36))

      result.errors.filter(_.key == "addressline1") mustBe Seq(
        FormError(
          "addressline1",
          "changePrAddress.error.addressline1.invalid",
          Seq(addresslineRegex)
        )
      )
    }

    Seq(
      "postcode%",
      "postcode",
      "INVALID",
      "12345",
      "SW1A",
      "SW1A 2A",
      "SW1A 2AAA",
      "T11YEE0"
    ).foreach { postcode =>
      s"must reject invalid UK postcode $postcode" in {
        val result = form.bind(validData.updated("ukPostcode", postcode))
        result.errors must not be empty
        result.errors.exists(_.message == "changePrAddress.error.ukPostcode.invalid") mustBe true
      }
    }

    Seq(
      "AB1 1BA",
      "AB11BA",
      "ab1 1ba",
      "ab11ba",
      "ab121ba",
      "GIR 0AA",
      "gir0aa",
      "FX11XX",
      "W12DN",
      "DE128HJ"
    ).foreach { postcode =>
      s"must accept valid UK postcode $postcode" in {
        val result = form.bind(validData.updated("ukPostcode", postcode))
        result.errors mustBe empty
      }
    }
  }
}
