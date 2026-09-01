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

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import config.NoOpCrypto
import base.SpecBase
import play.api.libs.json.{JsString, Json}

class SchemeDetailsSpec extends SpecBase with ScalaCheckPropertyChecks {

  "SchemeDetails" - {

    "successfully read from json" in {
      forAll(schemeDetailsGen) { details =>
        val json = Json.toJson(details)(using SchemeDetails.writes(using NoOpCrypto))
        val ob = json.as[SchemeDetails](using SchemeDetails.reads)

        ob.schemeName mustBe details.schemeName
        ob.pstr mustBe details.pstr
        ob.schemeStatus mustBe details.schemeStatus
        ob.schemeType mustBe details.schemeType
        ob.authorisingPSAID mustBe details.authorisingPSAID

        val obNames =
          ob.establishers.map(_.name.decryptedValue.split(" ").map(_.replaceAll("^\"|\"$", "")).mkString(" "))
        val detailsNames = details.establishers.map(_.name.decryptedValue)
        obNames mustBe detailsNames

        ob.establishers.map(_.kind) mustBe details.establishers.map(_.kind)
      }
    }

    "must handle empty establishers" in {
      val json = Json.obj(
        "schemeName" -> "Test Scheme",
        "pstr" -> "12345678",
        "schemeStatus" -> "Open",
        "schemeType" -> Json.obj("name" -> "trust"),
        "pspDetails" -> Json.obj("authorisingPSAID" -> "PSA123"),
        "establishers" -> Json.arr()
      )
      val result = json.as[SchemeDetails]

      result.establishers mustBe Nil
    }

    "Establisher" - {

      "must implement equals correctly" in {
        val establisher1 = Establisher(SensitiveString("Test Company"), EstablisherKind.Company)
        val establisher2 = Establisher(SensitiveString("Test Company"), EstablisherKind.Company)
        val establisher3 = Establisher(SensitiveString("Different Company"), EstablisherKind.Company)

        establisher1.equals(establisher2) mustBe true
        establisher1.equals(establisher3) mustBe false
        establisher1.equals("some string") mustBe false
      }

      "must implement hashCode correctly" in {
        val establisher1 = Establisher(SensitiveString("Test Company"), EstablisherKind.Company)
        val establisher2 = Establisher(SensitiveString("Test Company"), EstablisherKind.Company)
        val establisher3 = Establisher(SensitiveString("Different Company"), EstablisherKind.Company)

        establisher1.hashCode mustEqual establisher2.hashCode
        (establisher1.hashCode must not).equal(establisher3.hashCode)
      }

      "must write Company establisher" in {
        val establisher = Establisher(SensitiveString("Test Company"), EstablisherKind.Company)
        val json = Json.toJson(establisher)(using Establisher.writes(using NoOpCrypto))

        (json \ "companyDetails" \ "companyName").as[String] mustBe "Test Company"
      }

      "must write Partnership establisher" in {
        val establisher = Establisher(SensitiveString("Test Partnership"), EstablisherKind.Partnership)
        val json = Json.toJson(establisher)(using Establisher.writes(using NoOpCrypto))

        (json \ "partnershipDetails" \ "name").as[String] mustBe "Test Partnership"
      }

      "must write Individual establisher" in {
        val establisher = Establisher(SensitiveString("Firstname M Surname"), EstablisherKind.Individual)
        val json = Json.toJson(establisher)(using Establisher.writes(using NoOpCrypto))

        (json \ "establisherDetails" \ "firstName").as[String] mustBe "Firstname"
        (json \ "establisherDetails" \ "lastName").as[String] mustBe "Surname"
        (json \ "establisherDetails" \ "middleName").as[String] mustBe "M"
      }
    }

    "SchemeStatus" - {

      "successfully read from json" in {
        forAll(schemeStatusGen) { status =>
          Json.toJson(status).as[SchemeStatus] mustBe status
        }
      }

      "return a JsError" - {
        "Scheme status is unknown" in {
          forAll(nonEmptyString) { status =>
            JsString(status).asOpt[SchemeStatus] mustBe None
          }
        }
      }
    }

    "ListSchemeDetails" - {

      "successfully read from json" in {
        forAll(listMinimalSchemeDetailsGen) { listMinimalSchemeDetails =>
          Json.toJson(listMinimalSchemeDetails).as[ListMinimalSchemeDetails] mustBe listMinimalSchemeDetails
        }
      }
    }
  }
}
