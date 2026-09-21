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
import services.UserAnswersService
import org.jsoup.Jsoup
import pages.{AreBeneficiariesKnownPage, IhtPayablePage}
import play.api.inject.bind
import base.SpecBase
import uk.gov.hmrc.http.UpstreamErrorResponse
import models.{CheckMode, NormalMode, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.{verify, verifyNoInteractions, when}

import scala.concurrent.Future

class IhtPayableControllerSpec extends SpecBase {
  private val answers = emptyUserAnswers.set(AreBeneficiariesKnownPage, false).success.value

  "IhtPayableController" - {
    Seq(NormalMode, CheckMode).foreach { mode =>
      Seq(true, false).foreach { isPsa =>
        s"must render the currency input in $mode for isPsa=$isPsa" in {
          val app = applicationBuilder(Some(answers), isPsa = isPsa, usesSession = true).build()
          running(app) {
            val result = route(app, FakeRequest(GET, routes.IhtPayableController.onPageLoad(srn, mode).url)).value
            status(result) mustBe OK
            val doc = Jsoup.parse(contentAsString(result))
            doc.select("h1").text mustBe "Enter the amount of Inheritance Tax payable"
            doc.select("label.govuk-label--l").size mustBe 1
            doc.select("label.govuk-label--xl").size mustBe 0
            doc.select(".govuk-input__prefix").text mustBe "\u00a3"
            doc.select("input#value").hasClass("govuk-input--width-10") mustBe true
            doc.select("input#value").attr("inputmode") mustBe "decimal"
            doc.select("form").attr("action") mustBe routes.IhtPayableController.onSubmit(srn, mode).url
          }
        }

        s"must save the amount and return to CYA in $mode for isPsa=$isPsa" in {
          val service = mock[UserAnswersService]
          when(service.set(any())(using any(), any())).thenReturn(Future.successful(Right(answers)))
          val app = applicationBuilder(Some(answers), isPsa = isPsa, usesSession = true)
            .overrides(bind[UserAnswersService].toInstance(service))
            .build()
          running(app) {
            val result = route(
              app,
              FakeRequest(POST, routes.IhtPayableController.onSubmit(srn, mode).url)
                .withFormUrlEncodedBody("value" -> "1,234.56")
            ).value
            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe routes.CheckYourAnswersController
              .onPageLoad(srn)
              .url
            val saved = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(service).set(saved.capture())(using any(), any())
            saved.getValue mustBe answers.set(IhtPayablePage, BigDecimal("1234.56")).success.value
          }
        }
      }
    }

    "must populate the saved amount" in {
      val app =
        applicationBuilder(Some(answers.set(IhtPayablePage, BigDecimal("1234.56")).success.value), usesSession = true)
          .build()
      running(app) {
        val result = route(app, FakeRequest(GET, routes.IhtPayableController.onPageLoad(srn, CheckMode).url)).value
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).select("input#value").attr("value") mustBe "1234.56"
      }
    }

    Seq("" -> "Enter the Inheritance Tax payable amount", "10%" -> "Enter a valid Inheritance Tax payable amount")
      .foreach { case (input, error) =>
        s"must show $error without saving" in {
          val service = mock[UserAnswersService]
          val app = applicationBuilder(Some(answers), usesSession = true)
            .overrides(bind[UserAnswersService].toInstance(service))
            .build()
          running(app) {
            val result = route(
              app,
              FakeRequest(POST, routes.IhtPayableController.onSubmit(srn, NormalMode).url)
                .withFormUrlEncodedBody("value" -> input)
            ).value
            status(result) mustBe BAD_REQUEST
            val doc = Jsoup.parse(contentAsString(result))
            doc.select(".govuk-error-summary a").text mustBe error
            doc.select(".govuk-error-summary a").attr("href") mustBe "#value"
            doc.select("input#value").attr("value") mustBe input
            verifyNoInteractions(service)
          }
        }
      }

    Seq(
      None,
      Some(emptyUserAnswers),
      Some(emptyUserAnswers.set(AreBeneficiariesKnownPage, true).success.value)
    ).zipWithIndex.foreach { case (data, index) =>
      Seq(GET, POST).foreach { method =>
        s"must recover for invalid journey state $index on $method" in {
          val service = mock[UserAnswersService]
          val app =
            applicationBuilder(data, usesSession = true).overrides(bind[UserAnswersService].toInstance(service)).build()
          running(app) {
            val result = route(
              app,
              FakeRequest(method, routes.IhtPayableController.onPageLoad(srn, NormalMode).url)
                .withFormUrlEncodedBody("value" -> "12.34")
            ).value
            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
            verifyNoInteractions(service)
          }
        }
      }
    }

    "must not continue to CYA when saving fails" in {
      val service = mock[UserAnswersService]
      when(service.set(any())(using any(), any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("failed", 500))))
      val app = applicationBuilder(Some(answers), usesSession = true)
        .overrides(bind[UserAnswersService].toInstance(service))
        .build()
      running(app) {
        val result = route(
          app,
          FakeRequest(POST, routes.IhtPayableController.onSubmit(srn, NormalMode).url)
            .withFormUrlEncodedBody("value" -> "12.34")
        ).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
