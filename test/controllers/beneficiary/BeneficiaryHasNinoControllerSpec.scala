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
import views.html.beneficiary.BeneficiaryHasNinoView
import base.SpecBase
import forms.beneficiary.BeneficiaryHasNinoFormProvider
import models._
import pages.beneficiary.{BeneficiaryHasNinoPage, BeneficiaryNamePage}
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.{times, verify, when}

import scala.concurrent.Future

class BeneficiaryHasNinoControllerSpec extends SpecBase {

  private val form = new BeneficiaryHasNinoFormProvider()()
  private val beneficiaryName = IndividualName(Some("Mr"), "Firstname", Some("Middlename"), "Surname")

  private lazy val beneficiaryHasNinoRoute: String =
    controllers.beneficiary.routes.BeneficiaryHasNinoController.onPageLoad(srn, testIndex, NormalMode).url

  private val answersWithName = emptyUserAnswers
    .set(
      BeneficiaryNamePage(testIndex, JourneyRole.BeneficiaryIndividual),
      beneficiaryName
    )
    .success
    .value

  "BeneficiaryHasNinoController" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(answersWithName), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, beneficiaryHasNinoRoute)
        val result = route(application, request).value
        val view = application.injector.instanceOf[BeneficiaryHasNinoView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form, srn, testIndex, NormalMode, "Firstname Surname")(using request, messages(application)).toString
      }
    }

    "must populate the view on a GET when the question has previously been answered" in {
      val userAnswers = answersWithName.set(BeneficiaryHasNinoPage(testIndex), true).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, beneficiaryHasNinoRoute)
        val result = route(application, request).value
        val view = application.injector.instanceOf[BeneficiaryHasNinoView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form.fill(true), srn, testIndex, NormalMode, "Firstname Surname")(using
            request,
            messages(application)
          ).toString
      }
    }

    Seq(true, false).foreach { answer =>
      s"must save a $answer answer and redirect to the beneficiary list in NormalMode" in {
        val mockConnector = mock[InheritanceTaxOnPensionsConnector]
        when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
          .thenReturn(Future.successful(Right(answersWithName)))

        val application = applicationBuilder(userAnswers = Some(answersWithName), usesSession = true)
          .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
          .build()

        running(application) {
          val request = FakeRequest(POST, beneficiaryHasNinoRoute).withFormUrlEncodedBody("value" -> answer.toString)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.BeneficiaryListController.onPageLoad(srn).url
          verify(mockConnector, times(1)).setUserAnswers(any(), any(), any(), any(), any())(using any())
        }
      }

      s"must save a $answer answer and redirect to CYA in CheckMode" in {
        val mockConnector = mock[InheritanceTaxOnPensionsConnector]
        when(mockConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
          .thenReturn(Future.successful(Right(answersWithName)))

        val application = applicationBuilder(userAnswers = Some(answersWithName), usesSession = true)
          .overrides(bind[InheritanceTaxOnPensionsConnector].toInstance(mockConnector))
          .build()

        running(application) {
          val request = FakeRequest(
            POST,
            routes.BeneficiaryHasNinoController.onSubmit(srn, testIndex, CheckMode).url
          ).withFormUrlEncodedBody("value" -> answer.toString)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.CheckYourAnswersController.onPageLoad(srn).url
          verify(mockConnector, times(1)).setUserAnswers(any(), any(), any(), any(), any())(using any())
        }
      }
    }

    "must return a Bad Request and errors when no option is selected" in {
      val application = applicationBuilder(userAnswers = Some(answersWithName), usesSession = true).build()

      running(application) {
        val request = FakeRequest(POST, beneficiaryHasNinoRoute).withFormUrlEncodedBody("value" -> "")
        val result = route(application, request).value
        val view = application.injector.instanceOf[BeneficiaryHasNinoView]
        val boundForm = form.bind(Map("value" -> ""))

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual
          view(boundForm, srn, testIndex, NormalMode, "Firstname Surname")(using
            request,
            messages(application)
          ).toString
      }
    }

    "must redirect to Journey Recovery on a GET when the beneficiary name is missing" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val result = route(application, FakeRequest(GET, beneficiaryHasNinoRoute)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery on a POST when the beneficiary name is missing" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(POST, beneficiaryHasNinoRoute).withFormUrlEncodedBody("value" -> "true")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    testInvalidBeneficiaryIndexes.foreach { invalidIndex =>
      s"must return Not Found for invalid index $invalidIndex" in {
        val application = applicationBuilder(userAnswers = Some(answersWithName), usesSession = true).build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.beneficiary.routes.BeneficiaryHasNinoController
              .onPageLoad(srn, invalidIndex, NormalMode)
              .url
          )
          val result = route(application, request).value

          status(result) mustEqual NOT_FOUND
        }
      }
    }
  }
}
