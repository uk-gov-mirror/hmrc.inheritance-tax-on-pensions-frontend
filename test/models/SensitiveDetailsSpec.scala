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

package models

import uk.gov.hmrc.crypto.SymmetricCryptoFactory
import base.SpecBase
import play.api.libs.json.{JsString, Json}

class SensitiveDetailsSpec extends SpecBase {

  private val testKey = "teStTesttE5TtesT3TEsTtEsttESTTest5TEsTtE5t1="

  "SensitiveString" - {

    "must serialize and deserialize with plain format" in {
      val sensitiveString = SensitiveString("test@example.com")
      val json = Json.toJson(sensitiveString)(using SensitiveDetails.sensitiveStringWrites)
      val result = json.as[SensitiveString](using SensitiveDetails.sensitiveStringReads)

      result.decryptedValue mustBe "test@example.com"
      json mustBe JsString("test@example.com")
    }

    "must serialize and deserialize with encrypted format" in {
      implicit val crypto: uk.gov.hmrc.crypto.Encrypter & uk.gov.hmrc.crypto.Decrypter =
        SymmetricCryptoFactory.aesGcmCrypto(testKey)
      val sensitiveString = SensitiveString("test@example.com")
      val json = Json.toJson(sensitiveString)(using SensitiveDetails.sensitiveStringFormat)
      val result = json.as[SensitiveString](using SensitiveDetails.sensitiveStringFormat)

      result.decryptedValue mustBe "test@example.com"
      (json.as[String] must not).equal("test@example.com")
    }

    "must encrypt different values differently" in {
      implicit val crypto: uk.gov.hmrc.crypto.Encrypter & uk.gov.hmrc.crypto.Decrypter =
        SymmetricCryptoFactory.aesGcmCrypto(testKey)
      val string1 = SensitiveString("firstnametwo surname")
      val string2 = SensitiveString("firstname surname")

      val json1 = Json.toJson(string1)(using SensitiveDetails.sensitiveStringFormat)
      val json2 = Json.toJson(string2)(using SensitiveDetails.sensitiveStringFormat)

      (json1 must not).equal(json2)
    }

    "must implement equals correctly" in {
      val string1 = SensitiveString("test@example.com")
      val string2 = SensitiveString("test@example.com")
      val string3 = SensitiveString("different@example.com")

      string1.equals(string2) mustBe true
      string1.equals(string3) mustBe false
      string1.equals("some string") mustBe false
      string1.equals(null) mustBe false
    }

    "must implement hashCode correctly" in {
      val string1 = SensitiveString("test@example.com")
      val string2 = SensitiveString("test@example.com")
      val string3 = SensitiveString("different@example.com")

      string1.hashCode mustEqual string2.hashCode
      (string1.hashCode must not).equal(string3.hashCode)
    }
  }

  "SensitiveIndividualDetails" - {

    "must serialize and deserialize with plain format" in {
      val details = SensitiveIndividualDetails(
        firstName = SensitiveString("Firstname"),
        middleName = Some(SensitiveString("Middlename")),
        lastName = SensitiveString("Surname")
      )

      val json = Json.toJson(details)(using SensitiveDetails.sensitiveIndividualDetailsWrites)
      val result = json.as[SensitiveIndividualDetails](using SensitiveDetails.sensitiveIndividualDetailsReads)

      result.firstName.decryptedValue mustBe "Firstname"
      result.middleName.map(_.decryptedValue) mustBe Some("Middlename")
      result.lastName.decryptedValue mustBe "Surname"
    }

    "must serialize and deserialize with encrypted format" in {
      implicit val crypto: uk.gov.hmrc.crypto.Encrypter & uk.gov.hmrc.crypto.Decrypter =
        SymmetricCryptoFactory.aesGcmCrypto(testKey)
      val details = SensitiveIndividualDetails(
        firstName = SensitiveString("Firstname"),
        middleName = Some(SensitiveString("Middlename")),
        lastName = SensitiveString("Surname")
      )

      val json = Json.toJson(details)(using SensitiveDetails.sensitiveIndividualDetailsFormat)
      val result = json.as[SensitiveIndividualDetails](using SensitiveDetails.sensitiveIndividualDetailsFormat)

      result.firstName.decryptedValue mustBe "Firstname"
      result.middleName.map(_.decryptedValue) mustBe Some("Middlename")
      result.lastName.decryptedValue mustBe "Surname"

      val jsonString = Json.stringify(json)
      jsonString must not be empty
      (jsonString must not).include("Firstname")
      (jsonString must not).include("Middlename")
      (jsonString must not).include("Surname")
    }

    "must handle missing middle name" in {
      implicit val crypto: uk.gov.hmrc.crypto.Encrypter & uk.gov.hmrc.crypto.Decrypter =
        SymmetricCryptoFactory.aesGcmCrypto(testKey)
      val details = SensitiveIndividualDetails(
        firstName = SensitiveString("Firstname"),
        middleName = None,
        lastName = SensitiveString("Surname")
      )

      val json = Json.toJson(details)(using SensitiveDetails.sensitiveIndividualDetailsFormat)
      val result = json.as[SensitiveIndividualDetails](using SensitiveDetails.sensitiveIndividualDetailsFormat)

      result.firstName.decryptedValue mustBe "Firstname"
      result.middleName mustBe None
      result.lastName.decryptedValue mustBe "Surname"
    }

    "must generate full name" in {
      val details = SensitiveIndividualDetails(
        firstName = SensitiveString("Firstname"),
        middleName = Some(SensitiveString("Middlename")),
        lastName = SensitiveString("Surname")
      )
      details.fullName mustBe "Firstname Middlename Surname"
    }

    "must generate full name without middle name" in {
      val details = SensitiveIndividualDetails(
        firstName = SensitiveString("Firstname"),
        middleName = None,
        lastName = SensitiveString("Surname")
      )
      details.fullName mustBe "Firstname  Surname"
    }

    "must implement equals correctly" in {
      val details1 = SensitiveIndividualDetails(
        firstName = SensitiveString("Firstname"),
        middleName = None,
        lastName = SensitiveString("Surname")
      )
      val details2 = SensitiveIndividualDetails(
        firstName = SensitiveString("Firstname"),
        middleName = None,
        lastName = SensitiveString("Surname")
      )
      val details3 = SensitiveIndividualDetails(
        firstName = SensitiveString("Firstnametwo"),
        middleName = None,
        lastName = SensitiveString("Surname")
      )

      details1 mustEqual details2
      (details1 must not).equal(details3)
    }

    "must not equal non-SensitiveIndividualDetails" in {
      val details = SensitiveIndividualDetails(
        firstName = SensitiveString("Firstname"),
        middleName = None,
        lastName = SensitiveString("Surname")
      )
      (details must not).equal("some string")
      (details must not).equal(null)
    }

    "must implement hashCode correctly" in {
      val details1 = SensitiveIndividualDetails(
        firstName = SensitiveString("Firstname"),
        middleName = None,
        lastName = SensitiveString("Surname")
      )
      val details2 = SensitiveIndividualDetails(
        firstName = SensitiveString("Firstname"),
        middleName = None,
        lastName = SensitiveString("Surname")
      )
      val details3 = SensitiveIndividualDetails(
        firstName = SensitiveString("Firstnametwo"),
        middleName = None,
        lastName = SensitiveString("Surname")
      )

      details1.hashCode mustEqual details2.hashCode
      (details1.hashCode must not).equal(details3.hashCode)
    }
  }
}
