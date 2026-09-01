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

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, Json}

class IndividualNameSpec extends SpecBase {

  "IndividualName" - {

    "must successfully read from json" in {

      val json = Json.obj(
        "title" -> "Mr",
        "firstForename" -> "Firstname",
        "secondForename" -> "Middlename",
        "surname" -> "Surname"
      )

      val result = json.validate[IndividualName]

      result mustBe JsSuccess(
        IndividualName(
          title = Some("Mr"),
          firstForename = "Firstname",
          secondForename = Some("Middlename"),
          surname = "Surname"
        )
      )
    }

    "must successfully write to json" in {

      val individualName = IndividualName(
        title = Some("Mr"),
        firstForename = "Firstname",
        secondForename = Some("Middlename"),
        surname = "Surname"
      )

      val json = Json.toJson(individualName)

      (json \ "title").as[String] mustBe "Mr"
      (json \ "firstForename").as[String] mustBe "Firstname"
      (json \ "secondForename").as[String] mustBe "Middlename"
      (json \ "surname").as[String] mustBe "Surname"
    }

    "must handle missing optional fields when reading from json" in {

      val json = Json.obj(
        "firstForename" -> "Firstname",
        "surname" -> "Surname"
      )

      val result = json.validate[IndividualName]

      result mustBe JsSuccess(
        IndividualName(
          title = None,
          firstForename = "Firstname",
          secondForename = None,
          surname = "Surname"
        )
      )
    }

    "must fail when required field firstForename is missing" in {

      val json = Json.obj(
        "title" -> "Mr",
        "surname" -> "Surname"
      )

      val result = json.validate[IndividualName]

      result mustBe a[JsError]
    }

    "must fail when required field surname is missing" in {

      val json = Json.obj(
        "title" -> "Mr",
        "firstForename" -> "Firstname"
      )

      val result = json.validate[IndividualName]

      result mustBe a[JsError]
    }

    "must fail when firstForename is wrong type" in {

      val json = Json.obj(
        "title" -> "Mr",
        "firstForename" -> 123,
        "surname" -> "Surname"
      )

      val result = json.validate[IndividualName]

      result mustBe a[JsError]
    }

    "must fail when surname is wrong type" in {

      val json = Json.obj(
        "title" -> "Mr",
        "firstForename" -> "Firstname",
        "surname" -> 123
      )

      val result = json.validate[IndividualName]

      result mustBe a[JsError]
    }

    "must handle empty string for optional title" in {

      val json = Json.obj(
        "title" -> "",
        "firstForename" -> "Firstname",
        "surname" -> "Surname"
      )

      val result = json.validate[IndividualName]

      result mustBe JsSuccess(
        IndividualName(
          title = Some(""),
          firstForename = "Firstname",
          secondForename = None,
          surname = "Surname"
        )
      )
    }

    "must handle empty string for optional secondForename" in {

      val json = Json.obj(
        "firstForename" -> "Firstname",
        "secondForename" -> "",
        "surname" -> "Surname"
      )

      val result = json.validate[IndividualName]

      result mustBe JsSuccess(
        IndividualName(
          title = None,
          firstForename = "Firstname",
          secondForename = Some(""),
          surname = "Surname"
        )
      )
    }

    "must write to json with only present fields" in {

      val individualName = IndividualName(
        title = None,
        firstForename = "Firstname",
        secondForename = None,
        surname = "Surname"
      )

      val json = Json.toJson(individualName)

      (json \ "firstForename").as[String] mustBe "Firstname"
      (json \ "surname").as[String] mustBe "Surname"
      (json \ "title").toOption mustBe None
      (json \ "secondForename").toOption mustBe None
    }
  }
}
