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

import play.api.data.format.Formatter
import forms.mappings.{Mappings, Regex}
import play.api.data.Forms.{mapping, of}
import play.api.data.validation.{Constraint, Valid}
import models.PrAddress
import play.api.data.{Form, FormError, Mapping}

import javax.inject.Inject

class BeneficiaryAddressFormProvider @Inject() extends Mappings with Regex {

  private val addressLineMaxLength = 35
  private val ukPostcodeMaxLength = 8
  private val optionalStringFormatter: Formatter[Option[String]] = new Formatter[Option[String]] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[String]] =
      Right(data.get(key).map(_.trim).filter(_.nonEmpty))

    override def unbind(key: String, value: Option[String]): Map[String, String] =
      Map(key -> value.getOrElse(""))
  }

  def apply(country: String): Form[PrAddress] =
    Form(
      mapping(
        "addressLine1" -> text("changePrAddress.error.addressLine1.required")
          .transform[String](_.trim, identity)
          .verifying(
            firstError(
              nonBlank("changePrAddress.error.addressLine1.required"),
              regexp(addressLineRegex, "changePrAddress.error.addressLine1.invalid"),
              maxLength(addressLineMaxLength, "changePrAddress.error.addressLine1.length")
            )
          ),
        "addressLine2" -> optionalAddressField(
          "changePrAddress.error.addressLine2.invalid",
          "changePrAddress.error.addressLine2.length"
        ),
        "addressLine3" -> optionalAddressField(
          "changePrAddress.error.addressLine3.invalid",
          "changePrAddress.error.addressLine3.length"
        ),
        "addressLine4" -> optionalAddressField(
          "changePrAddress.error.addressLine4.invalid",
          "changePrAddress.error.addressLine4.length"
        ),
        "postCode" -> optionalUkPostcode(
          "changePrAddress.error.postCode.invalid",
          "changePrAddress.error.postCode.length"
        ),
        "addressLine5" -> optionalAddressField(
          "changePrAddress.error.addressLine5.invalid",
          "changePrAddress.error.addressLine5.length"
        )
      )((addressLine1, addressLine2, addressLine3, addressLine4, postCode, addressLine5) =>
        PrAddress(addressLine1, addressLine2, addressLine3, addressLine4, postCode, country, addressLine5)
      )(address =>
        Some(
          (
            address.addressLine1,
            address.addressLine2,
            address.addressLine3,
            address.addressLine4,
            address.postCode,
            address.addressLine5
          )
        )
      )
    )

  def isUkAddress(address: PrAddress): Boolean =
    address.country == "GB"

  def isUkAddress(country: String): Boolean =
    country == "GB"

  private def optionalAddressField(invalidKey: String, lengthKey: String): Mapping[Option[String]] =
    of(using optionalStringFormatter)
      .verifying(
        firstError(
          optionalConstraint(regexp(addressLineRegex, invalidKey)),
          optionalConstraint(maxLength(addressLineMaxLength, lengthKey))
        )
      )

  private def optionalUkPostcode(invalidKey: String, lengthKey: String): Mapping[Option[String]] =
    of(using optionalStringFormatter)
      .transform(_.map(_.toUpperCase()), identity)
      .verifying(
        firstError(
          optionalConstraint(regexp(ukPostcodeEtmpsRegularExpr, invalidKey)),
          optionalConstraint(maxLength(ukPostcodeMaxLength, lengthKey))
        )
      )

  private def optionalConstraint(constraint: Constraint[String]): Constraint[Option[String]] =
    Constraint(_.map(constraint.apply).getOrElse(Valid))
}
