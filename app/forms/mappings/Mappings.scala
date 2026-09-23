/*
 * Copyright 2025 HM Revenue & Customs
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

package forms.mappings

import play.api.data.Forms.of
import uk.gov.hmrc.domain.Nino
import models.Enumerable
import play.api.i18n.Messages
import play.api.data.{FieldMapping, Mapping}

import java.time.LocalDate

trait Mappings extends Formatters with Constraints with Regex {

  private val reasonForNoNinoMaxLength = 160

  protected def text(errorKey: String = "error.required", args: Seq[String] = Seq.empty): FieldMapping[String] =
    of(using stringFormatter(errorKey, args))

  protected def nino(
    requiredKey: String,
    invalidKey: String,
    args: Seq[String] = Seq.empty
  ): Mapping[Nino] =
    text(requiredKey, args)
      .verifying(invalidKey, s => Nino.isValid(s.filterNot(_.isWhitespace).toUpperCase))
      .transform[Nino](s => Nino(s.filterNot(_.isWhitespace).toUpperCase), _.nino.filterNot(_.isWhitespace).toUpperCase)

  protected def validatedText(
    requiredKey: String,
    invalidKey: String,
    maxLengthKey: String,
    regex: String,
    maximum: Int,
    args: Seq[String] = Seq.empty
  ): Mapping[String] =
    text(requiredKey, args)
      .transform[String](_.trim, identity)
      .verifying(maxLength(maximum, maxLengthKey))
      .verifying(regexp(regex, invalidKey))

  protected def reasonForNoNino(
    requiredKey: String,
    invalidKey: String,
    maxLengthKey: String,
    args: Seq[String] = Seq.empty
  ): Mapping[String] =
    validatedText(
      requiredKey = requiredKey,
      invalidKey = invalidKey,
      maxLengthKey = maxLengthKey,
      regex = reasonForNoNinoRegex,
      maximum = reasonForNoNinoMaxLength,
      args = args
    )

  protected def int(
    requiredKey: String = "error.required",
    wholeNumberKey: String = "error.wholeNumber",
    nonNumericKey: String = "error.nonNumeric",
    args: Seq[String] = Seq.empty
  ): FieldMapping[Int] =
    of(using intFormatter(requiredKey, wholeNumberKey, nonNumericKey, args))

  protected def boolean(
    requiredKey: String = "error.required",
    invalidKey: String = "error.boolean",
    args: Seq[String] = Seq.empty
  ): FieldMapping[Boolean] =
    of(using booleanFormatter(requiredKey, invalidKey, args))

  protected def enumerable[A](
    requiredKey: String = "error.required",
    invalidKey: String = "error.invalid",
    args: Seq[String] = Seq.empty
  )(implicit ev: Enumerable[A]): FieldMapping[A] =
    of(using enumerableFormatter[A](requiredKey, invalidKey, args))

  protected def localDate(
    invalidKey: String,
    allRequiredKey: String,
    twoRequiredKey: String,
    requiredKey: String,
    args: Seq[String] = Seq.empty
  )(implicit messages: Messages): FieldMapping[LocalDate] =
    of(using new LocalDateFormatter(invalidKey, allRequiredKey, twoRequiredKey, requiredKey, args))

  protected def currency(
    requiredKey: String = "error.required",
    invalidNumeric: String = "error.invalidNumeric",
    nonNumericKey: String = "error.nonNumeric",
    args: Seq[String] = Seq.empty
  ): FieldMapping[BigDecimal] =
    of(using currencyFormatter(requiredKey, invalidNumeric, nonNumericKey, args))
}
