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

package controllers.beneficiary

import play.api.test.FakeRequest
import connectors.InheritanceTaxOnPensionsConnector
import play.api.inject.bind
import views.html.beneficiary.BeneficiaryTrustNameView
import base.SpecBase
import forms.beneficiary.BeneficiaryTrustNameFormProvider
import models._
import pages.beneficiary.BeneficiaryTrustNamePage
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.{times, verify, when}

import scala.concurrent.Future

class BeneficiaryTrustNameControllerSpec extends SpecBase {

  private val form = new BeneficiaryTrustNameFormProvider()()
  private lazy val routeUrl =
    routes.BeneficiaryTrustNameController.onPageLoad(srn, testIndex, NormalMode).url

  private val validFormData = Seq("value" -> trustName)

  "BeneficiaryTrustNameController" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result = route(application, request).value
        val view = application.injector.instanceOf[BeneficiaryTrustNameView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form, srn, testIndex, NormalMode)(using request, messages(application)).toString
      }
    }

    "must populate the view when the trust name has already been answered" in {
      val userAnswers = emptyUserAnswers
        .set(BeneficiaryTrustNamePage(testIndex), trustName)
        .success
        .value
      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result = route(application, request).value
        val view = application.injector.instanceOf[BeneficiaryTrustNameView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form.fill(trustName), srn, testIndex, NormalMode)(using
            request,
            messages(application)
          ).toString
      }
    }

    Seq(NormalMode, CheckMode).foreach { mode =>
      s"must save the trust name and redirect to the correct next page in $mode" in {
        val mockConnector = mock[InheritanceTaxOnPensionsConnector]
        when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
          .thenReturn(Future.successful(Right(emptyUserAnswers)))
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true)
          .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
          .build()

        running(application) {
          val request = FakeRequest(
            POST,
            routes.BeneficiaryTrustNameController.onSubmit(srn, testIndex, mode).url
          ).withFormUrlEncodedBody(validFormData*)
          val result = route(application, request).value
          val expectedUrl = mode match {
            case NormalMode => routes.BeneficiaryListController.onPageLoad(srn).url
            case CheckMode =>
              controllers.routes.CheckYourAnswersController.onPageLoad(srn).url
          }

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual expectedUrl

          val answersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(mockConnector, times(1))
            .setUserAnswers(answersCaptor.capture(), any(), any(), any(), any())(using any())
          answersCaptor.getValue.get(BeneficiaryTrustNamePage(testIndex)).value mustEqual trustName
        }
      }
    }

    "must return a Bad Request when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val data = Map("value" -> "")
        val request = FakeRequest(POST, routeUrl).withFormUrlEncodedBody(data.toSeq*)
        val result = route(application, request).value
        val view = application.injector.instanceOf[BeneficiaryTrustNameView]

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual
          view(form.bind(data), srn, testIndex, NormalMode)(using request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery when no user answers are available" in {
      val application = applicationBuilder(userAnswers = None, usesSession = true).build()

      running(application) {
        val result = route(application, FakeRequest(GET, routeUrl)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
