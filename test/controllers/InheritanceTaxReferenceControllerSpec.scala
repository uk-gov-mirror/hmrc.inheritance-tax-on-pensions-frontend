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
import play.api.inject.bind
import views.html.InheritanceTaxReferenceView
import base.SpecBase
import forms.InheritanceTaxReferenceFormProvider
import models._
import play.api.data.Form
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito._

import scala.concurrent.Future

class InheritanceTaxReferenceControllerSpec extends SpecBase {

  val formProvider = new InheritanceTaxReferenceFormProvider()
  val form: Form[String] = formProvider()

  lazy val inheritanceTaxReferenceRoute: String =
    routes.InheritanceTaxReferenceController.onPageLoad(srn, NormalMode).url

  "InheritanceTaxReferenceController Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, inheritanceTaxReferenceRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[InheritanceTaxReferenceView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, NormalMode)(using request, messages(application)).toString
      }
    }

    "must redirect to name-of-deceased when valid data is submitted" in {

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
          FakeRequest(POST, inheritanceTaxReferenceRoute)
            .withFormUrlEncodedBody(("value", "A123456/25A"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.IndividualNameController
          .onPageLoad(srn, NormalMode, JourneyRole.Deceased)
          .url

        verify(mockInheritanceTaxOnPensionsConnector, times(1))
          .setUserAnswers(any(), any(), any(), any(), any())(using any())
      }
    }

    "must redirect to Check Your Answers when valid data is submitted in CheckMode" in {

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
          FakeRequest(POST, routes.InheritanceTaxReferenceController.onSubmit(srn, CheckMode).url)
            .withFormUrlEncodedBody(("value", "A123456/25A"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController
          .onPageLoad(srn)
          .url

        verify(mockInheritanceTaxOnPensionsConnector, times(1))
          .setUserAnswers(any(), any(), any(), any(), any())(using any())
      }
    }

    "must return BadRequest when userAnswersService.set returns Left" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Left(uk.gov.hmrc.http.UpstreamErrorResponse("Error", 500))))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
        .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, inheritanceTaxReferenceRoute)
            .withFormUrlEncodedBody(("value", "A123456/25A"))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, inheritanceTaxReferenceRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[InheritanceTaxReferenceView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode)(using
          request,
          messages(application)
        ).toString
      }
    }
  }
}
