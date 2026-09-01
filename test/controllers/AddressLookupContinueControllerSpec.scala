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
import services.{AddressLookupFrontendService, UserAnswersService}
import play.api.inject.bind
import models.addresslookup.{AlfAddress, AlfAddressData, AlfCountry}
import base.SpecBase
import play.api.libs.json.{JsObject, Json}
import models._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.{never, verify, when}

import scala.concurrent.Future

class AddressLookupContinueControllerSpec extends SpecBase {

  private val addressId = "address-id"
  private val validAddressData = AlfAddressData(
    id = Some(addressId),
    address = AlfAddress(
      organisation = None,
      lines = Seq("33 AB Street", "AB Area"),
      town = Some("ABville"),
      postcode = Some("ZZ1 1ZZ"),
      country = AlfCountry("GB", "United Kingdom")
    )
  )

  private lazy val journeyRoleTestCases = Seq(
    JourneyRole.PrIndividual,
    JourneyRole.PrOrganisation
  )

  "AddressLookupContinueController" - {

    journeyRoleTestCases.foreach { journeyRole =>

      List(
        (NormalMode, routes.DidPrSubmitController.onPageLoad(srn, NormalMode).url),
        (CheckMode, routes.CheckYourAnswersController.onPageLoad(srn).url)
      ).foreach { (modeTested, expectedRedirectLocation) =>
        s"must save the selected address and redirect to the next page when ALF returns a valid address in $modeTested for ${journeyRole.name} journey" in {

          val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]
          val mockUserAnswersService = mock[UserAnswersService]

          when(mockAddressLookupFrontendService.getAddress(any())(using any()))
            .thenReturn(Future.successful(validAddressData))
          when(mockUserAnswersService.set(any())(using any(), any()))
            .thenReturn(Future.successful(Right(emptyUserAnswers)))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
            .overrides(
              bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService),
              bind[UserAnswersService].toInstance(mockUserAnswersService)
            )
            .build()

          running(application) {
            val request =
              FakeRequest(
                GET,
                routes.AddressLookupContinueController.continue(srn, modeTested, journeyRole, addressId).url
              )

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value must endWith(expectedRedirectLocation)

            val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(mockUserAnswersService).set(userAnswersCaptor.capture())(using any(), any())

            (userAnswersCaptor.getValue.data \ "prDetails" \ journeyRole.name).as[PrAddress] mustBe
              PrAddress(
                addressLine1 = "33 AB Street",
                addressLine2 = Some("AB Area"),
                addressLine3 = None,
                addressLine4 = Some("ABville"),
                ukPostcode = Some("ZZ1 1ZZ"),
                country = "GB"
              )
          }
        }
      }

      s"must redirect to journey recovery when no address id is returned from ALF for ${journeyRole.name} journey" in {

        val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]
        val mockUserAnswersService = mock[UserAnswersService]

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
          .overrides(
            bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService),
            bind[UserAnswersService].toInstance(mockUserAnswersService)
          )
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            routes.AddressLookupContinueController.continue(srn, NormalMode, journeyRole, " ").url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(mockAddressLookupFrontendService, never).getAddress(any())(using any())
          verify(mockUserAnswersService, never).set(any())(using any(), any())
        }
      }

      s"must redirect to journey recovery and not save when ALF returns no address lines for ${journeyRole.name} journey" in {

        val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]
        val mockUserAnswersService = mock[UserAnswersService]

        when(mockAddressLookupFrontendService.getAddress(any())(using any()))
          .thenReturn(
            Future.successful(validAddressData.copy(address = validAddressData.address.copy(lines = Seq.empty)))
          )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
          .overrides(
            bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService),
            bind[UserAnswersService].toInstance(mockUserAnswersService)
          )
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            routes.AddressLookupContinueController.continue(srn, NormalMode, journeyRole, addressId).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(mockUserAnswersService, never).set(any())(using any(), any())
        }
      }

      s"must redirect to journey recovery and not save when ALF returns a blank first address line for ${journeyRole.name} journey" in {

        val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]
        val mockUserAnswersService = mock[UserAnswersService]

        when(mockAddressLookupFrontendService.getAddress(any())(using any()))
          .thenReturn(
            Future.successful(validAddressData.copy(address = validAddressData.address.copy(lines = Seq(" "))))
          )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
          .overrides(
            bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService),
            bind[UserAnswersService].toInstance(mockUserAnswersService)
          )
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            routes.AddressLookupContinueController.continue(srn, NormalMode, journeyRole, addressId).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(mockUserAnswersService, never).set(any())(using any(), any())
        }
      }
    }

    "must replace stale address fields when changing to an address with fewer lines for pr-individual journey" in {

      val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]
      val mockUserAnswersService = mock[UserAnswersService]

      val existingUserAnswers = emptyUserAnswers.copy(
        data = Json.obj(
          "prDetails" -> Json.obj(
            "individual" -> Json.obj(
              "firstForename" -> "Firstname",
              "surname" -> "Surname",
              "addressLine1" -> "33 AB Street",
              "addressLine2" -> "AB Area",
              "addressLine3" -> "Some District",
              "addressLine4" -> "Anytown",
              "ukPostcode" -> "ZZ1 1ZZ",
              "country" -> "GB"
            )
          )
        )
      )

      val newAddressData = AlfAddressData(
        id = Some(addressId),
        address = AlfAddress(
          organisation = None,
          lines = Seq("11 A Boulevard", "ABville"),
          town = Some("ABville"),
          postcode = Some("ZZ1 1ZZ"),
          country = AlfCountry("GB", "United Kingdom")
        )
      )

      when(mockAddressLookupFrontendService.getAddress(any())(using any()))
        .thenReturn(Future.successful(newAddressData))
      when(mockUserAnswersService.set(any())(using any(), any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(existingUserAnswers), usesSession = true)
        .overrides(
          bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService),
          bind[UserAnswersService].toInstance(mockUserAnswersService)
        )
        .build()

      running(application) {
        val request = FakeRequest(
          GET,
          routes.AddressLookupContinueController.continue(srn, NormalMode, JourneyRole.PrIndividual, addressId).url
        )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.DidPrSubmitController.onPageLoad(srn, NormalMode).url

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersService).set(userAnswersCaptor.capture())(using any(), any())

        val updatedIndividual = (userAnswersCaptor.getValue.data \ "prDetails" \ "individual").as[JsObject]

        updatedIndividual mustBe Json.obj(
          "firstForename" -> "Firstname",
          "surname" -> "Surname",
          "addressLine1" -> "11 A Boulevard",
          "addressLine2" -> "ABville",
          "ukPostcode" -> "ZZ1 1ZZ",
          "country" -> "GB"
        )
      }
    }

    "must replace stale address fields when changing to an address with fewer lines for pr-organisation journey" in {

      val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]
      val mockUserAnswersService = mock[UserAnswersService]

      val existingUserAnswers = emptyUserAnswers.copy(
        data = Json.obj(
          "prDetails" -> Json.obj(
            "organisation" -> Json.obj(
              "organisationName" -> "AB Org",
              "addressLine1" -> "33 AB Street",
              "addressLine2" -> "AB Area",
              "addressLine3" -> "Some District",
              "addressLine4" -> "Anytown",
              "ukPostcode" -> "ZZ1 1ZZ",
              "country" -> "GB"
            )
          )
        )
      )

      val newAddressData = AlfAddressData(
        id = Some(addressId),
        address = AlfAddress(
          organisation = None,
          lines = Seq("11 A Boulevard", "ABville"),
          town = Some("ABville"),
          postcode = Some("ZZ1 1ZZ"),
          country = AlfCountry("GB", "United Kingdom")
        )
      )

      when(mockAddressLookupFrontendService.getAddress(any())(using any()))
        .thenReturn(Future.successful(newAddressData))
      when(mockUserAnswersService.set(any())(using any(), any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(existingUserAnswers), usesSession = true)
        .overrides(
          bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService),
          bind[UserAnswersService].toInstance(mockUserAnswersService)
        )
        .build()

      running(application) {
        val request = FakeRequest(
          GET,
          routes.AddressLookupContinueController.continue(srn, NormalMode, JourneyRole.PrOrganisation, addressId).url
        )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.DidPrSubmitController.onPageLoad(srn, NormalMode).url

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersService).set(userAnswersCaptor.capture())(using any(), any())

        val updatedOrganisation = (userAnswersCaptor.getValue.data \ "prDetails" \ "organisation").as[JsObject]

        updatedOrganisation mustBe Json.obj(
          "organisationName" -> "AB Org",
          "addressLine1" -> "11 A Boulevard",
          "addressLine2" -> "ABville",
          "ukPostcode" -> "ZZ1 1ZZ",
          "country" -> "GB"
        )
      }
    }

    "must redirect to journey recovery when the journey is not individual or organisation" in {

      val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]
      val mockUserAnswersService = mock[UserAnswersService]

      when(mockAddressLookupFrontendService.getAddress(any())(using any()))
        .thenReturn(Future.successful(validAddressData))
      when(mockUserAnswersService.set(any())(using any(), any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(
          bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService),
          bind[UserAnswersService].toInstance(mockUserAnswersService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, "/test-only/unknown-address-lookup-continue")

        val controller = application.injector.instanceOf[AddressLookupContinueController]

        val result = controller.continue(srn, NormalMode, JourneyRole.Unknown, addressId)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
