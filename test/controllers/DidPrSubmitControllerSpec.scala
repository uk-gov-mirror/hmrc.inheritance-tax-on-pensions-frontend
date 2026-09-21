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

class DidPrSubmitControllerSpec extends SpecBase with MockitoSugar {

  private def assertSubmitterOptions(html: String, prName: String): Unit = {
    val document = org.jsoup.Jsoup.parse(html)
    document.title() must startWith("Who submitted the payment notice?")
    document.select("h1").text() mustBe "Who submitted the payment notice?"
    document.select("label[for=value]").text() mustBe prName
    document.select("label[for=value-no]").text() mustBe "Someone else"
    document.select("input#value").attr("value") mustBe "true"
    document.select("input#value-no").attr("value") mustBe "false"
    document.select("button[type=submit]").text() mustBe "Save and continue"
  }

  private val formProvider = new DidPrSubmitFormProvider()
  private val form = formProvider()

  private lazy val didPrSubmitRoute: String = routes.DidPrSubmitController.onPageLoad(srn, NormalMode).url
  val userAnswersWithPrName: UserAnswers = emptyUserAnswers
    .set(PrTypePage, PrType.Individual)
    .get
    .set(IndividualNamePage(JourneyRole.PrIndividual), individualName)
    .get

  val userAnswersWithOrganisationPrName: UserAnswers = emptyUserAnswers
    .set(PrTypePage, PrType.Organisation)
    .get
    .set(IndividualNamePage(JourneyRole.PrOrganisation), individualName)
    .get

  "DidPrSubmit Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithPrName), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, didPrSubmitRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DidPrSubmitView]

        status(result) mustEqual OK
        assertSubmitterOptions(contentAsString(result), individualNameFormatted)
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
        assertSubmitterOptions(contentAsString(result), individualNameFormatted)
        contentAsString(result) mustEqual view(form, srn, NormalMode, individualNameFormatted)(using
          request,
          messages(application)
        ).toString
      }
    }

    Seq(true, false).foreach { answer =>
      s"must populate the view correctly on a GET when the previous answer is $answer" in {

        val userAnswers = userAnswersWithPrName.set(DidPrSubmitPage, answer).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

        running(application) {
          val request = FakeRequest(GET, didPrSubmitRoute)

          val view = application.injector.instanceOf[DidPrSubmitView]

          val result = route(application, request).value

          status(result) mustEqual OK
          org.jsoup.Jsoup.parse(contentAsString(result)).select("input[checked]").attr("value") mustBe answer.toString
          contentAsString(result) mustEqual view(form.fill(answer), srn, NormalMode, individualNameFormatted)(using
            request,
            messages(application)
          ).toString
        }
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
        .set(PaymentNoticeDatePage, testPaymentNoticeDate)
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
        userAnswersCaptor.getValue.get(PaymentNoticeDatePage).value mustEqual testPaymentNoticeDate
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
        .set(PaymentNoticeDatePage, testPaymentNoticeDate)
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
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController
          .onPageLoad(srn)
          .url
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
