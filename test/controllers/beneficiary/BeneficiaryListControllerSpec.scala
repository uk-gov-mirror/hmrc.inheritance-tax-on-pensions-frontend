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
import play.api.test.Helpers._
import views.html.beneficiary.BeneficiaryListView
import base.SpecBase
import forms.beneficiary.BeneficiaryListFormProvider
import viewmodels.beneficiary.BeneficiaryListItem
import models.beneficiary.BeneficiaryType
import models.{CheckMode, JourneyRole, NormalMode}
import pages.beneficiary.{BeneficiaryNamePage, BeneficiaryOrganisationDetailsPage, BeneficiaryTypePage}

class BeneficiaryListControllerSpec extends SpecBase {

  private val form = new BeneficiaryListFormProvider()()
  private lazy val routeUrl = routes.BeneficiaryListController.onPageLoad(srn).url
  private val answersWithBeneficiary = emptyUserAnswers
    .set(BeneficiaryTypePage(testIndex), BeneficiaryType.Individual)
    .success
    .value
    .set(BeneficiaryNamePage(testIndex, JourneyRole.BeneficiaryIndividual), individualName)
    .success
    .value

  private lazy val listItems = Seq(
    BeneficiaryListItem(
      name = individualNameFormatted,
      changeUrl = routes.BeneficiaryNameController
        .onPageLoad(srn, CheckMode, testIndex)
        .url,
      removeUrl = routes.RemoveBeneficiaryController.onPageLoad(srn, testIndex).url
    )
  )

  "BeneficiaryListController" - {
    "must return OK and display the beneficiary list for a GET" in {
      val application = applicationBuilder(userAnswers = Some(answersWithBeneficiary), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result = route(application, request).value
        val view = application.injector.instanceOf[BeneficiaryListView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form, srn, listItems, 1)(using request, messages(application)).toString
      }
    }

    "must return OK with an empty list when no beneficiaries have been added" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result = route(application, request).value
        val view = application.injector.instanceOf[BeneficiaryListView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form, srn, Seq.empty, 0)(using request, messages(application)).toString
      }
    }

    "must display an organisation beneficiary with the correct Change link" in {
      val userAnswers = emptyUserAnswers
        .set(BeneficiaryTypePage(testIndex), BeneficiaryType.Organisation)
        .success
        .value
        .set(BeneficiaryOrganisationDetailsPage(testIndex), beneficiaryOrganisationDetails)
        .success
        .value
      val application = applicationBuilder(userAnswers = Some(userAnswers), usesSession = true).build()

      running(application) {
        val request = FakeRequest(GET, routeUrl)
        val result = route(application, request).value
        val view = application.injector.instanceOf[BeneficiaryListView]
        val organisationItem = BeneficiaryListItem(
          name = organisationName,
          changeUrl = routes.BeneficiaryOrganisationDetailsController.onPageLoad(srn, testIndex, CheckMode).url,
          removeUrl = routes.RemoveBeneficiaryController.onPageLoad(srn, testIndex).url
        )

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form, srn, Seq(organisationItem), 1)(using request, messages(application)).toString
      }
    }

    "must route Yes to the beneficiary type page at the next index" in {
      val application = applicationBuilder(userAnswers = Some(answersWithBeneficiary), usesSession = true).build()

      running(application) {
        val request = FakeRequest(POST, routeUrl).withFormUrlEncodedBody("value" -> "true")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          routes.BeneficiaryTypeController.onPageLoad(srn, testIndex + 1, NormalMode).url
      }
    }

    "must route No to Check Your Answers" in {
      val application = applicationBuilder(userAnswers = Some(answersWithBeneficiary), usesSession = true).build()

      running(application) {
        val request = FakeRequest(POST, routeUrl).withFormUrlEncodedBody("value" -> "false")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.CheckYourAnswersController.onPageLoad(srn).url
      }
    }

    "must return a Bad Request when no option is selected" in {
      val application = applicationBuilder(userAnswers = Some(answersWithBeneficiary), usesSession = true).build()

      running(application) {
        val request = FakeRequest(POST, routeUrl).withFormUrlEncodedBody("value" -> "")
        val result = route(application, request).value
        val view = application.injector.instanceOf[BeneficiaryListView]

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual
          view(form.bind(Map("value" -> "")), srn, listItems, 1)(using request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery when a beneficiary is incomplete" in {
      val incompleteAnswers = emptyUserAnswers
        .set(BeneficiaryTypePage(testIndex), BeneficiaryType.Individual)
        .success
        .value
      val application = applicationBuilder(userAnswers = Some(incompleteAnswers), usesSession = true).build()

      running(application) {
        val result = route(application, FakeRequest(GET, routeUrl)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
