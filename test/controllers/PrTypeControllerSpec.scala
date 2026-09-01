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
import connectors.InheritanceTaxOnPensionsConnector
import pages._
import play.api.inject.bind
import views.html.PrTypeView
import base.SpecBase
import play.api.libs.json.Json
import models._
import play.api.data.Form
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, argThat}
import play.api.test.Helpers._
import org.mockito.Mockito.{times, verify, when}
import repositories.SessionMinimalDetailsRepository
import forms.PrTypeFormProvider

import scala.concurrent.Future

class PrTypeControllerSpec extends SpecBase {

  val formProvider = new PrTypeFormProvider()
  val form: Form[PrType] = formProvider()
  lazy val prTypeRoute: String = routes.PrTypeController.onPageLoad(srn, NormalMode).url

  "PrTypeController Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, prTypeRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PrTypeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, NormalMode)(using request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(PrTypePage, PrType.Individual).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, prTypeRoute)

        val view = application.injector.instanceOf[PrTypeView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(PrType.Individual), srn, NormalMode)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the PR individual name page when Individual is submitted" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, prTypeRoute)
            .withFormUrlEncodedBody(("value", PrType.Individual.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.IndividualNameController
          .onPageLoad(srn, NormalMode, JourneyRole.PrIndividual)
          .url

        verify(mockInheritanceTaxOnPensionsConnector, times(1))
          .setUserAnswers(any(), any(), any(), any(), any())(using any())
      }
    }

    "must redirect to Organisation name page when Organisation is submitted" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, prTypeRoute)
            .withFormUrlEncodedBody(("value", PrType.Organisation.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.OrganisationNameController.onPageLoad(srn, NormalMode).url
      }
    }

    "must clear PR individual details when Organisation is submitted" in {

      val userAnswers = emptyUserAnswers.copy(
        data = Json.obj(
          "prDetails" -> Json.obj(
            "individual" -> (Json.toJsObject(individualName) ++ Json.toJsObject(testPrAddress))
          )
        )
      )

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, prTypeRoute)
            .withFormUrlEncodedBody(("value", PrType.Organisation.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockInheritanceTaxOnPensionsConnector, times(1))
          .setUserAnswers(userAnswersCaptor.capture(), any(), any(), any(), any())(using any())

        userAnswersCaptor.getValue.get(IndividualNamePage(JourneyRole.PrIndividual)) mustBe None
        userAnswersCaptor.getValue.get(PrIndividualAddressPage) mustBe None
      }
    }

    "must redirect to the organisation name page when Organisation is submitted in CheckMode and organisation name details are missing" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.PrTypeController.onSubmit(srn, CheckMode).url)
            .withFormUrlEncodedBody(("value", PrType.Organisation.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.OrganisationNameController
          .onPageLoad(srn, CheckMode)
          .url
      }
    }

    "must redirect to address lookup when Organisation data is submitted in CheckMode and the address is missing" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val userAnswersWithOrgDetails = emptyUserAnswers
        .copy(
          data = Json.obj(
            "prDetails" -> Json.obj(
              "organisation" -> (Json.obj("organisationName" -> organisationName)
                ++ Json.toJsObject(individualName))
            )
          )
        )

      val application = applicationBuilder(userAnswers = Some(userAnswersWithOrgDetails), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.PrTypeController.onSubmit(srn, CheckMode).url)
            .withFormUrlEncodedBody(("value", PrType.Organisation.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.AddressLookupStartController
          .start(srn, CheckMode, JourneyRole.PrOrganisation)
          .url
      }
    }

    "must redirect to Check Your Answers when complete Organisation data is submitted in CheckMode" in {

      val mockConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val userAnswers = emptyUserAnswers.copy(
        data = Json.obj(
          "prDetails" -> Json.obj(
            "organisation" -> (Json.obj("organisationName" -> organisationName)
              ++ Json.toJsObject(individualName)
              ++ Json.toJsObject(testPrAddress))
          )
        )
      )

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true)
        .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.PrTypeController.onSubmit(srn, CheckMode).url)
          .withFormUrlEncodedBody(("value", PrType.Organisation.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(srn).url
      }
    }

    "must redirect to the PR individual name page when Individual is submitted in CheckMode and name details are missing" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.PrTypeController.onSubmit(srn, CheckMode).url)
            .withFormUrlEncodedBody(("value", PrType.Individual.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.IndividualNameController
          .onPageLoad(srn, CheckMode, JourneyRole.PrIndividual)
          .url
      }
    }

    "must redirect to address lookup when Individual data is submitted in CheckMode and the address is missing" in {

      val userAnswers = emptyUserAnswers
        .set(IndividualNamePage(JourneyRole.PrIndividual), individualName)
        .success
        .value

      val mockConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true)
        .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.PrTypeController.onSubmit(srn, CheckMode).url)
          .withFormUrlEncodedBody(("value", PrType.Individual.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.AddressLookupStartController
          .start(srn, CheckMode, JourneyRole.PrIndividual)
          .url
      }
    }

    "must clear Organisation details when switching from Organisation to Individual" in {
      val mockSessionRepository = mock[SessionMinimalDetailsRepository]
      val mockConnector = mock[InheritanceTaxOnPensionsConnector]

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val userAnswersWithOrgName = emptyUserAnswers
        .copy(
          data = Json.obj(
            "prDetails" -> Json.obj(
              "organisation" -> (Json.obj("organisationName" -> organisationName)
                ++ Json.toJsObject(individualName)
                ++ Json.toJsObject(testPrAddress))
            )
          )
        )
        .set(PrTypePage, PrType.Organisation)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithOrgName), usesSession = true)
        .overrides(
          bind[SessionMinimalDetailsRepository].toInstance(mockSessionRepository),
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.PrTypeController.onPageLoad(srn, NormalMode).url)
          .withFormUrlEncodedBody(("value", "individual"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.IndividualNameController
          .onPageLoad(srn, NormalMode, JourneyRole.PrIndividual)
          .url

        verify(mockConnector).setUserAnswers(
          argThat { userAnswers =>
            userAnswers.get(OrganisationNamePage).isEmpty &&
            userAnswers.get(IndividualNamePage(JourneyRole.PrOrganisation)).isEmpty &&
            userAnswers.get(PrOrganisationAddressPage).isEmpty
          },
          any(),
          any(),
          any(),
          any()
        )(using any())
      }
    }

    "must redirect to Check Your Answers when valid Individual data is submitted in CheckMode and name and address details are present" in {

      val userAnswers = emptyUserAnswers.copy(
        data = Json.obj(
          "prDetails" -> Json.obj(
            "individual" -> (Json.toJsObject(individualName) ++ Json.toJsObject(testPrAddress))
          )
        )
      )

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.PrTypeController.onSubmit(srn, CheckMode).url)
            .withFormUrlEncodedBody(("value", PrType.Individual.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(srn).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, prTypeRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[PrTypeView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, prTypeRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, prTypeRoute)
            .withFormUrlEncodedBody(("value", PrType.Individual.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
