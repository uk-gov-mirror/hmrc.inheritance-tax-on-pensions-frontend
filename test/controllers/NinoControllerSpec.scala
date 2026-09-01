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
import pages.{IndividualNamePage, NinoPage}
import views.html.NinoView
import base.SpecBase
import forms.NinoFormProvider
import models._
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.when
import play.api.inject
import uk.gov.hmrc.domain.Nino

import scala.concurrent.Future

class NinoControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new NinoFormProvider()
  private val form = formProvider()
  private val nameOfDeceased: IndividualName = IndividualName(
    title = Some("Mr"),
    firstForename = "Firstname",
    secondForename = Some("Middlename"),
    surname = "Surname"
  )
  private val deceasedName: String = s"${nameOfDeceased.firstForename} ${nameOfDeceased.surname}"
  private val userAnswersWithDeceasedName: UserAnswers = emptyUserAnswers
    .set(IndividualNamePage(JourneyRole.Deceased), nameOfDeceased)
    .success
    .value
  private lazy val ninoRoute = routes.NinoController.onPageLoad(srn, NormalMode).url

  "NinoPage Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, ninoRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NinoView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, NormalMode, deceasedName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET" in {

      val userAnswers = userAnswersWithDeceasedName
        .set(NinoPage, "AA123456A")
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, ninoRoute)

        val view = application.injector.instanceOf[NinoView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(Nino("AA123456A")), srn, NormalMode, deceasedName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(userAnswersWithDeceasedName)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true)
        .overrides(
          inject.bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, ninoRoute)
            .withFormUrlEncodedBody(("value", "AA123456A"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.BirthDeathDatesController.onPageLoad(srn, NormalMode).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, ninoRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[NinoView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode, deceasedName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, ninoRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, ninoRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if the deceased name has not been answered" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, ninoRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if the deceased name has not been answered" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, ninoRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
