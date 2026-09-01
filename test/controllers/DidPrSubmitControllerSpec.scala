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
import views.html.DidPrSubmitView
import base.SpecBase
import forms.DidPrSubmitFormProvider
import models._
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.{verify, when}
import org.mockito.ArgumentCaptor
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

import java.time.LocalDate

class DidPrSubmitControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new DidPrSubmitFormProvider()
  private val form = formProvider()

  private lazy val didPrSubmitRoute: String = routes.DidPrSubmitController.onPageLoad(srn, NormalMode).url
  val userAnswersWithPrName: UserAnswers = emptyUserAnswers
    .set(PrTypePage, PrType.Individual)
    .get
    .set(IndividualNamePage(JourneyRole.PrIndividual), individualName)
    .get

  private val organisationPrName = IndividualName(
    title = Some("Mrs"),
    firstForename = "Firstnamethree",
    secondForename = Some("Middlenametwo"),
    surname = "Surnametwo"
  )

  val userAnswersWithOrganisationPrName: UserAnswers = emptyUserAnswers
    .set(PrTypePage, PrType.Organisation)
    .get
    .set(IndividualNamePage(JourneyRole.PrOrganisation), organisationPrName)
    .get

  "DidPrSubmit Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithPrName), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, didPrSubmitRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DidPrSubmitView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, NormalMode, individualNameFormatted)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET when the organisation PR name has been answered" in {
      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithOrganisationPrName), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, didPrSubmitRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DidPrSubmitView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, NormalMode, "Firstnamethree Surnametwo")(using
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithPrName.set(DidPrSubmitPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, didPrSubmitRoute)

        val view = application.injector.instanceOf[DidPrSubmitView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), srn, NormalMode, individualNameFormatted)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the payment notice date page when Yes is submitted in NormalMode" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(userAnswersWithPrName)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithPrName), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, didPrSubmitRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PaymentNoticeDateController.onPageLoad(srn, NormalMode).url
      }
    }

    "must redirect to the payment notice date page when Yes is submitted in CheckMode" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(userAnswersWithPrName)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithPrName), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.DidPrSubmitController.onSubmit(srn, CheckMode).url)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PaymentNoticeDateController.onPageLoad(srn, CheckMode).url
      }
    }

    "must redirect to the payment notice date page and keep the payment notice date when No is submitted in NormalMode" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(userAnswersWithPrName)))

      val userAnswers = userAnswersWithPrName
        .set(PaymentNoticeDatePage, LocalDate.of(2026, 3, 27))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, didPrSubmitRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PaymentNoticeDateController.onPageLoad(srn, NormalMode).url

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockInheritanceTaxOnPensionsConnector).setUserAnswers(
          userAnswersCaptor.capture(),
          any(),
          any(),
          any(),
          any()
        )(using any())
        userAnswersCaptor.getValue.get(DidPrSubmitPage).value mustEqual false
        userAnswersCaptor.getValue.get(PaymentNoticeDatePage).value mustEqual LocalDate.of(2026, 3, 27)
      }
    }

    "must redirect to the payment notice date page when No is submitted in CheckMode and the payment notice date is missing" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(userAnswersWithPrName)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithPrName), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.DidPrSubmitController.onSubmit(srn, CheckMode).url)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PaymentNoticeDateController.onPageLoad(srn, CheckMode).url
      }
    }

    "must redirect to Check Your Answers when No is submitted in CheckMode and the payment notice date is present" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(userAnswersWithPrName)))

      val userAnswers = userAnswersWithPrName
        .set(PaymentNoticeDatePage, LocalDate.of(2026, 3, 27))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.DidPrSubmitController.onSubmit(srn, CheckMode).url)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(srn).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithPrName), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, didPrSubmitRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DidPrSubmitView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode, individualNameFormatted)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, didPrSubmitRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, didPrSubmitRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if name is missing" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, didPrSubmitRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if name is missing" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request =
          FakeRequest(POST, didPrSubmitRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

}
