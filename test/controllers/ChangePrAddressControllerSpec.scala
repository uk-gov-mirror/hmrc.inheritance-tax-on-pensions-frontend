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

package controllers

import play.api.test.FakeRequest
import services.UserAnswersService
import pages.{IndividualNamePage, OrganisationNamePage}
import play.api.inject.bind
import views.html.ChangePrAddressView
import base.SpecBase
import play.api.libs.json.{JsObject, Json}
import forms.PrAddressFormProvider
import models._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.{never, verify, when}

import scala.concurrent.Future

class ChangePrAddressControllerSpec extends SpecBase {

  private val formProvider = new PrAddressFormProvider()
  private val address = PrAddress(
    addressLine1 = "1 Street Road",
    addressLine2 = Some("2 Cathedral Square"),
    addressLine3 = Some("Newcastle upon Tyne"),
    addressLine4 = Some("Tyne and Wear"),
    ukPostcode = Some("NE1 1EH"),
    country = "GB"
  )

  private val individualAnswers = emptyUserAnswers
    .copy(
      data = Json.obj(
        "prDetails" -> Json.obj(
          "individual" -> (Json.toJsObject(address) ++ Json.obj(
            "title" -> "Mrs",
            "firstForename" -> "Firstnamethree",
            "secondForename" -> "Middlenametwo",
            "surname" -> "Surnametwo"
          ))
        )
      )
    )

  private val organisationAnswers = emptyUserAnswers.copy(
    data = Json.obj(
      "prDetails" -> Json.obj(
        "organisation" -> (Json.toJsObject(address) ++ Json.obj(
          "organisationName" -> "Standard Pension",
          "title" -> "Mrs",
          "firstForename" -> "Firstnamethree",
          "secondForename" -> "Middlenametwo",
          "surname" -> "Surnametwo"
        ))
      )
    )
  )

  private val roleCases = Seq(
    (JourneyRole.PrIndividual, individualAnswers, "Firstnamethree Surnametwo"),
    (JourneyRole.PrOrganisation, organisationAnswers, "Standard Pension")
  )

  "ChangePrAddressController" - {

    roleCases.foreach { case (journeyRole, userAnswers, displayName) =>
      s"must show the existing address for ${journeyRole.name}" in {
        val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

        running(application) {
          val request =
            FakeRequest(GET, routes.ChangePrAddressController.onPageLoad(srn, journeyRole).url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[ChangePrAddressView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            formProvider(address.country).fill(address),
            srn,
            journeyRole,
            displayName,
            isUkAddress = true
          )(using request, messages(application)).toString
        }
      }

      s"must save the edited address, retain the country and PR details, and return to CYA for ${journeyRole.name}" in {
        val mockUserAnswersService = mock[UserAnswersService]
        when(mockUserAnswersService.set(any())(using any(), any()))
          .thenReturn(Future.successful(Right(emptyUserAnswers)))

        val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true)
          .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, routes.ChangePrAddressController.onSubmit(srn, journeyRole).url)
              .withFormUrlEncodedBody(
                "addressLine1" -> "10 New Street",
                "addressLine2" -> "",
                "addressLine3" -> "",
                "addressLine4" -> "Newcastle upon Tyne",
                "ukPostcode" -> "NE2 2AA"
              )

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.CheckYourAnswersController.onPageLoad(srn).url

          val answersCaptor: ArgumentCaptor[UserAnswers] =
            ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(mockUserAnswersService).set(answersCaptor.capture())(using any(), any())

          val updatedPrDetails =
            (answersCaptor.getValue.data \ "prDetails" \ journeyRole.name).as[JsObject]
          updatedPrDetails.as[PrAddress] mustBe PrAddress(
            addressLine1 = "10 New Street",
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = Some("Newcastle upon Tyne"),
            ukPostcode = Some("NE2 2AA"),
            country = "GB"
          )

          journeyRole match {
            case JourneyRole.PrIndividual =>
              updatedPrDetails.as[IndividualName] mustBe
                IndividualName(Some("Mrs"), "Firstnamethree", Some("Middlenametwo"), "Surnametwo")
            case JourneyRole.PrOrganisation =>
              updatedPrDetails.value("organisationName").as[String] mustBe "Standard Pension"
            case _ => fail("Unexpected journey role in test")
          }
        }
      }

      s"must return Bad Request for invalid data for ${journeyRole.name}" in {
        val mockUserAnswersService = mock[UserAnswersService]
        val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true)
          .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, routes.ChangePrAddressController.onSubmit(srn, journeyRole).url)
              .withFormUrlEncodedBody(
                "addressLine1" -> "%",
                "addressLine2" -> "",
                "addressLine3" -> "",
                "addressLine4" -> "",
                "ukPostcode" -> ""
              )

          val result = route(application, request).value

          status(result) mustBe BAD_REQUEST
          verify(mockUserAnswersService, never).set(any())(using any(), any())
        }
      }
    }

    "must use the Postal code label for a non-UK address" in {
      val nonUkAddress = address.copy(country = "FR", ukPostcode = Some("75001"))
      val userAnswers = individualAnswers.copy(
        data = individualAnswers.data.deepMerge(
          Json.obj(
            "prDetails" -> Json.obj(
              "individual" -> Json.toJsObject(nonUkAddress)
            )
          )
        )
      )
      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(GET, routes.ChangePrAddressController.onPageLoad(srn, JourneyRole.PrIndividual).url)
        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) must include("Postal code (optional)")
        (contentAsString(result) must not).include("Postcode (optional)")
      }
    }

    "must redirect to Journey Recovery when the address is missing" in {
      val userAnswers = emptyUserAnswers
        .set(
          IndividualNamePage(JourneyRole.PrIndividual),
          IndividualName(None, "Firstnamethree", None, "Surnametwo")
        )
        .success
        .value
      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(GET, routes.ChangePrAddressController.onPageLoad(srn, JourneyRole.PrIndividual).url)
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when the organisation name is missing" in {
      val userAnswers = organisationAnswers.remove(OrganisationNamePage).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(GET, routes.ChangePrAddressController.onPageLoad(srn, JourneyRole.PrOrganisation).url)
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when user answers are missing" in {
      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(GET, routes.ChangePrAddressController.onPageLoad(srn, JourneyRole.PrIndividual).url)
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
