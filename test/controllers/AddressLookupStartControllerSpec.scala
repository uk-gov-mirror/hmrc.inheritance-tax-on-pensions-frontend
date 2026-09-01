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

import services.AddressLookupFrontendService
import pages.{IndividualNamePage, OrganisationNamePage}
import play.api.inject.bind
import base.SpecBase
import models._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import models.JourneyRole.PrIndividual
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito.{never, verify, when}

import scala.concurrent.Future

class AddressLookupStartControllerSpec extends SpecBase {

  private case class JourneyRoleTestCase(
    journeyRole: JourneyRole,
    userAnswers: UserAnswers,
    name: String
  )

  private lazy val journeyRoleTestCases = Seq(
    JourneyRoleTestCase(
      JourneyRole.PrIndividual,
      emptyUserAnswers
        .set(
          IndividualNamePage(PrIndividual),
          IndividualName(Some("Mr"), "Firstname", Some("Middlename"), "Surname")
        )
        .success
        .value,
      "Firstname Surname"
    ),
    JourneyRoleTestCase(
      JourneyRole.PrOrganisation,
      emptyUserAnswers
        .set(
          OrganisationNamePage,
          "Test Org"
        )
        .success
        .value,
      "Test Org"
    )
  )

  "AddressLookupStartController" - {

    journeyRoleTestCases.foreach { testCase =>
      val journeyRole = testCase.journeyRole

      s"must start the ALF journey and redirect to the returned ALF URL for ${journeyRole.name} journey" in {

        val userAnswers = testCase.userAnswers
        val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]

        when(
          mockAddressLookupFrontendService.initJourney(
            eqTo(srn),
            eqTo(NormalMode),
            eqTo(testCase.name),
            eqTo(journeyRole)
          )(using
            any(),
            any()
          )
        )
          .thenReturn(Future.successful("/lookup-address"))

        val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true)
          .overrides(bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService))
          .build()

        running(application) {
          val request =
            FakeRequest(GET, routes.AddressLookupStartController.start(srn, NormalMode, journeyRole).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual "/lookup-address"
          verify(mockAddressLookupFrontendService).initJourney(
            eqTo(srn),
            eqTo(NormalMode),
            eqTo(testCase.name),
            eqTo(journeyRole)
          )(using
            any(),
            any()
          )
        }
      }

      s"must start a new ALF journey in CheckMode for ${journeyRole.name} journey" in {

        val userAnswers = testCase.userAnswers
        val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]

        when(
          mockAddressLookupFrontendService.initJourney(
            eqTo(srn),
            eqTo(CheckMode),
            eqTo(testCase.name),
            eqTo(journeyRole)
          )(using
            any(),
            any()
          )
        )
          .thenReturn(Future.successful("/lookup-address"))

        val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true)
          .overrides(bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService))
          .build()

        running(application) {
          val request =
            FakeRequest(GET, routes.AddressLookupStartController.start(srn, CheckMode, journeyRole).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual "/lookup-address"
          verify(mockAddressLookupFrontendService).initJourney(
            eqTo(srn),
            eqTo(CheckMode),
            eqTo(testCase.name),
            eqTo(journeyRole)
          )(using
            any(),
            any()
          )
        }
      }

      s"must redirect to journey recovery when the ${journeyRole.name} name is missing for ${journeyRole.name} journey" in {

        val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
          .overrides(bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService))
          .build()

        running(application) {
          val request =
            FakeRequest(GET, routes.AddressLookupStartController.start(srn, NormalMode, journeyRole).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(mockAddressLookupFrontendService, never).initJourney(any(), any(), any(), any())(using any(), any())
        }
      }
    }

    "must redirect to journey recovery when the journey is not individual or organisation" in {

      val mockAddressLookupFrontendService = mock[AddressLookupFrontendService]

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(bind[AddressLookupFrontendService].toInstance(mockAddressLookupFrontendService))
        .build()

      running(application) {
        val request = FakeRequest(GET, "/test-only/unknown-address-lookup-start")

        val controller = application.injector.instanceOf[AddressLookupStartController]

        val result = controller.start(srn, NormalMode, JourneyRole.Unknown)(request)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
