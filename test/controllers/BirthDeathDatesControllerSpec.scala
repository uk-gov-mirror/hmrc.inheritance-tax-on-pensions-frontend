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
import pages.{BirthDeathDatesPage, IndividualNamePage}
import play.api.inject.bind
import views.html.BirthDeathDatesView
import base.SpecBase
import forms.BirthDeathDatesFormProvider
import models._
import play.api.i18n.Messages
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.{times, verify, when}
import org.mockito.ArgumentCaptor
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class BirthDeathDatesControllerSpec extends SpecBase with MockitoSugar {

  private implicit val messages: Messages = stubMessages()

  private val formProvider = new BirthDeathDatesFormProvider()
  private def form = formProvider()
  private val nameOfDeceased = IndividualName(
    title = Some("Mr"),
    firstForename = "Firstname",
    secondForename = Some("Middlename"),
    surname = "Surname"
  )
  private val deceasedName: String = s"${nameOfDeceased.firstForename} ${nameOfDeceased.surname}"

  private val validAnswer = BirthDeathDates(
    dateOfBirth = testDateOfBirth,
    dateOfDeath = testDateOfDeath
  )

  lazy val birthDeathDatesRoute: String = routes.BirthDeathDatesController.onPageLoad(srn, NormalMode).url

  override val emptyUserAnswers = UserAnswers(userAnswersId, srnGen.sample.value.toString, testUuid)
  private val userAnswersWithDeceasedName = emptyUserAnswers
    .set(IndividualNamePage(JourneyRole.Deceased), nameOfDeceased)
    .success
    .value

  def getRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, birthDeathDatesRoute)

  def postRequest(url: String = birthDeathDatesRoute): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, url)
      .withFormUrlEncodedBody(
        "dateOfBirth.day" -> validAnswer.dateOfBirth.getDayOfMonth.toString,
        "dateOfBirth.month" -> validAnswer.dateOfBirth.getMonthValue.toString,
        "dateOfBirth.year" -> validAnswer.dateOfBirth.getYear.toString,
        "dateOfDeath.day" -> validAnswer.dateOfDeath.getDayOfMonth.toString,
        "dateOfDeath.month" -> validAnswer.dateOfDeath.getMonthValue.toString,
        "dateOfDeath.year" -> validAnswer.dateOfDeath.getYear.toString
      )

  "BirthDeathDates Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true).build()

      running(application) {
        val result = route(application, getRequest).value

        val view = application.injector.instanceOf[BirthDeathDatesView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, srn, NormalMode, deceasedName)(using
          getRequest,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithDeceasedName.set(BirthDeathDatesPage, validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val view = application.injector.instanceOf[BirthDeathDatesView]

        val result = route(application, getRequest).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAnswer), srn, NormalMode, deceasedName)(using
          getRequest,
          messages(application)
        ).toString
      }
    }

    "must redirect to PR type when valid data is submitted" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]

      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true)
          .overrides(
            bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
          )
          .build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PrTypeController.onPageLoad(srn, NormalMode).url
      }
    }

    "must redirect to Check Your Answers when valid data is submitted in CheckMode" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]

      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true)
          .overrides(
            bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
          )
          .build()

      running(application) {
        val result =
          route(application, postRequest(routes.BirthDeathDatesController.onSubmit(srn, CheckMode).url)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(srn).url
      }
    }

    "must save birth and death dates without whitespace when valid data is submitted" in {

      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]

      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true)
          .overrides(
            bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, birthDeathDatesRoute)
            .withFormUrlEncodedBody(
              "dateOfBirth.day" -> " 1 ",
              "dateOfBirth.month" -> " Jan uary ",
              "dateOfBirth.year" -> " 1 950 ",
              "dateOfDeath.day" -> " 1 ",
              "dateOfDeath.month" -> " J an ",
              "dateOfDeath.year" -> " 2 020 "
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockInheritanceTaxOnPensionsConnector, times(1))
          .setUserAnswers(userAnswersCaptor.capture(), any(), any(), any(), any())(using any())

        userAnswersCaptor.getValue.get(BirthDeathDatesPage).value mustEqual validAnswer
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true).build()

      val data = Map(
        "dateOfBirth.day" -> "",
        "dateOfBirth.month" -> "",
        "dateOfBirth.year" -> "",
        "dateOfDeath.day" -> "",
        "dateOfDeath.month" -> "",
        "dateOfDeath.year" -> ""
      )

      val request =
        FakeRequest(POST, birthDeathDatesRoute)
          .withFormUrlEncodedBody(data.toSeq*)

      running(application) {
        val boundForm = formProvider.validate(form.bind(data))

        val view = application.injector.instanceOf[BirthDeathDatesView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, srn, NormalMode, deceasedName)(using
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request with formatted date values when valid date fields include whitespace" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithDeceasedName), usesSession = true).build()

      val data = Map(
        "dateOfBirth.day" -> " 1 ",
        "dateOfBirth.month" -> " Jan uary ",
        "dateOfBirth.year" -> " 2 020 ",
        "dateOfDeath.day" -> " 1 ",
        "dateOfDeath.month" -> " J an ",
        "dateOfDeath.year" -> " 2 020 "
      )

      val formattedData = Map(
        "dateOfBirth.day" -> "1",
        "dateOfBirth.month" -> "January",
        "dateOfBirth.year" -> "2020",
        "dateOfDeath.day" -> "1",
        "dateOfDeath.month" -> "Jan",
        "dateOfDeath.year" -> "2020"
      )

      val request =
        FakeRequest(POST, birthDeathDatesRoute)
          .withFormUrlEncodedBody(data.toSeq*)

      running(application) {
        val boundForm = formProvider.validate(form.bind(formattedData))

        val view = application.injector.instanceOf[BirthDeathDatesView]

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
        val result = route(application, getRequest).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if the deceased name has not been answered" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

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

    "must redirect to Journey Recovery for a POST if the deceased name has not been answered" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
