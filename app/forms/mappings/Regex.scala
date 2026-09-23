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

package forms.mappings

import scala.util.matching

trait Regex {
  val addresslineRegex: String = "^[a-zA-ZÀ-ÖØ-öø-ÿ0-9 \\-,.'\\/#:;º@_\\[\\]\\?]+$"

  val ukPostcodeRegex: String = """^GIR ?0AA$|^[A-Z]{1,2}[0-9][0-9A-Z]? ?[0-9][A-Z]{2}$"""

  val reasonForNoNinoRegex: String = """^[a-zA-Z0-9\- \t,./()]+$"""

  val nameRegex: String = "^[A-Za-zÀ-ÖØ-öø-ÿ]+(?:[ '-][A-Za-zÀ-ÖØ-öø-ÿ]+)*$"

  val ihtReferenceNumberRegex: matching.Regex = "^[A-Z]\\d{6}/\\d{2}[A-Z]$".r

  val schemeAdminIdRegex: matching.Regex = "^(A[0-9]{7})$".r

}
