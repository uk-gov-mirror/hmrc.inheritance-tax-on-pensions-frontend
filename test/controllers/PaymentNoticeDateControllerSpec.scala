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
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import connectors.InheritanceTaxOnPensionsConnector
import pages._
import play.api.inject.bind
import views.html.PaymentNoticeDateView
import base.SpecBase
import forms.PaymentNoticeDateFormProvider
import models._
import play.api.i18n.Messages
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.{times, verify, when}
import org.mockito.ArgumentCaptor
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

import java.time.LocalDate

class PaymentNoticeDateControllerSpec extends SpecBase with MockitoSugar {

  private implicit val messages: Messages = stubMessages()

  private val formProvider = new PaymentNoticeDateFormProvider()
  private def form = formProvider()
  private val fieldKey = "dateThePensionSchemeReceivedNoticeToPay"
  private val validAnswer = LocalDate.of(2026, 1, 1)
  private val dateOfDeath = LocalDate.of(2026, 1, 2)
  private val userAnswersWithDidPrSubmit = emptyUserAnswers.set(DidPrSubmitPage, true).success.value
  private val userAnswersWithDidPrNotSubmit = emptyUserAnswers.set(DidPrSubmitPage, false).success.value
  private val userAnswersWithDateOfDeath = userAnswersWithDidPrSubmit
    .set(BirthDeathDatesPage, BirthDeathDates(LocalDate.of(2000, 1, 1), dateOfDeath))
    .success
    .value

  lazy val paymentNoticeDateRoute: String = routes.PaymentNoticeDateController.onPageLoad(srn, NormalMode).url

  def getRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, paymentNoticeDateRoute)

  def postRequest(url: String = paymentNoticeDateRoute): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, url)
      .withFormUrlEncodedBody(
        s"$fieldKey.day" -> validAnswer.getDayOfMonth.toString,
        s"$fieldKey.month" -> validAnswer.getMonthValue.toString,
        s"$fieldKey.year" -> validAnswer.getYear.toString
      )

  "PaymentNoticeDate Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithDidPrSubmit), usesSession = true).build()

      running(application) {
        val result = route(application, getRequest).value

        val view = application.injector.instanceOf[PaymentNoticeDateView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, NormalMode, schemeName)(using
          getRequest,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET when PR submit payment notice has been answered No" in {
      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithDidPrNotSubmit), usesSession = true).build()

      running(application) {
        val result = route(application, getRequest).value

        val view = application.injector.instanceOf[PaymentNoticeDateView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, NormalMode, schemeName)(using
          getRequest,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET when PR submit payment notice has not been answered" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val result = route(application, getRequest).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST when PR submit payment notice has not been answered" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = userAnswersWithDidPrSubmit.set(PaymentNoticeDatePage, validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val view = application.injector.instanceOf[PaymentNoticeDateView]

        val result = route(application, getRequest).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAnswer), srn, NormalMode, schemeName)(using
          getRequest,
          messages(application)
        ).toString
      }
    }

    "must redirect to the beneficiaries known page when valid data is submitted and PR submit payment notice has been answered Yes" in {
      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]

      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDidPrSubmit), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.AreBeneficiariesKnownController.onPageLoad(srn, NormalMode).url
      }
    }

    "must redirect to the beneficiary individual or org page when valid data is submitted and PR submit payment notice has been answered Yes" in {
      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]

      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDidPrNotSubmit), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.beneficiary.routes.BeneficiaryTypeController
          .onPageLoad(srn, 0, NormalMode)
          .url
      }
    }

    "must redirect to the beneficiaries known page when valid data is submitted" in {
      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]

      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDidPrSubmit), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.AreBeneficiariesKnownController.onPageLoad(srn, NormalMode).url
      }
    }

    "must redirect to the beneficiaries known page when valid data is submitted in CheckMode and beneficiaries known is missing" in {
      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]

      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDidPrSubmit), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val result =
          route(application, postRequest(routes.PaymentNoticeDateController.onSubmit(srn, CheckMode).url)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.AreBeneficiariesKnownController.onPageLoad(srn, CheckMode).url
      }
    }

    "must redirect to Check Your Answers when valid data is submitted in CheckMode and beneficiaries known has already been answered" in {
      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]

      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val userAnswers = userAnswersWithDidPrSubmit
        .set(AreBeneficiariesKnownPage, true)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val result =
          route(application, postRequest(routes.PaymentNoticeDateController.onSubmit(srn, CheckMode).url)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController
          .onPageLoad(srn)
          .url
      }
    }

    "must save payment notice date without whitespace when valid data is submitted" in {
      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]

      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDidPrSubmit), usesSession = true)
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, paymentNoticeDateRoute)
            .withFormUrlEncodedBody(
              s"$fieldKey.day" -> " 1 ",
              s"$fieldKey.month" -> " Jan uary ",
              s"$fieldKey.year" -> " 2 026 "
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockInheritanceTaxOnPensionsConnector, times(1))
          .setUserAnswers(userAnswersCaptor.capture(), any(), any(), any(), any())(using any())

        userAnswersCaptor.getValue.get(PaymentNoticeDatePage).value mustEqual validAnswer
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithDidPrSubmit), usesSession = true).build()

      val data = Map(
        s"$fieldKey.day" -> "",
        s"$fieldKey.month" -> "",
        s"$fieldKey.year" -> ""
      )

      val request =
        FakeRequest(POST, paymentNoticeDateRoute)
          .withFormUrlEncodedBody(data.toSeq*)

      running(application) {
        val boundForm = formProvider.validate(form.bind(data))

        val view = application.injector.instanceOf[PaymentNoticeDateView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode, schemeName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request when the payment notice date is not after the date of death" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithDateOfDeath), usesSession = true).build()

      val data = Map(
        s"$fieldKey.day" -> validAnswer.getDayOfMonth.toString,
        s"$fieldKey.month" -> validAnswer.getMonthValue.toString,
        s"$fieldKey.year" -> validAnswer.getYear.toString
      )

      val request =
        FakeRequest(POST, paymentNoticeDateRoute)
          .withFormUrlEncodedBody(data.toSeq*)

      running(application) {
        val boundForm = formProvider.validate(form.bind(data), Some(dateOfDeath))

        val view = application.injector.instanceOf[PaymentNoticeDateView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode, schemeName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val result = route(application, getRequest).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
