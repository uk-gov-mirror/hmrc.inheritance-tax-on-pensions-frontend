/*
 * Copyright 2025 HM Revenue & Customs
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
import services.SubmissionListService
import connectors.InheritanceTaxOnPensionsConnector
import play.api.inject.bind
import views.html.SubmissionListView
import base.SpecBase
import uk.gov.hmrc.http.UpstreamErrorResponse
import models._
import viewmodels.SubmissionListPagination
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.when

import scala.concurrent.Future

import java.time.{Instant, LocalDate}

class SubmissionListControllerSpec extends SpecBase {
  lazy val onPageLoadUrl: String = routes.SubmissionListController.onPageLoad(srn).url
  lazy val onAmendUrl: String = routes.SubmissionListController.onAmend(srn, testUuid).url

  private val overviewReport = IhtpOverviewReport(
    uuid = None,
    fbNumber = Some("119000004320"),
    submissionDate = Some(Instant.parse("2026-04-10T16:12:49Z")),
    paymentDueDate = Some(LocalDate.of(2026, 2, 2)),
    ihtVersion = "001",
    inheritanceTaxReference = "A123456/25A",
    paymentReference = Some("A123456/25A629671"),
    title = Some("Dr"),
    firstForename = Some("Firstname"),
    secondForename = Some("M"),
    surname = Some("Surname"),
    nino = None,
    ihtpStatus = "Not reconciled"
  )

  private val overviewResponse =
    IhtpOverviewResponse(IhtpOverviewSuccess(Seq(overviewReport)))

  private val firstPagePagination = SubmissionListPagination(currentPage = 1, pageSize = 15, totalReports = 1)
  private val psaSchemeDashboardUrl =
    s"http://localhost:8204/manage-pension-schemes/pension-scheme-summary/${srn.value}"
  private val pspSchemeDashboardUrl =
    s"http://localhost:8204/manage-pension-schemes/${srn.value}/dashboard/pension-scheme-details"

  "onPageLoad" - {

    "must return OK and the correct view for a GET" in {
      val mockSubmissionListService = mock[SubmissionListService]
      when(mockSubmissionListService.getSubmissionList()(using any(), any()))
        .thenReturn(Future.successful(Right(overviewResponse)))

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[SubmissionListService].toInstance(mockSubmissionListService))
        .build()

      running(application) {
        val request = FakeRequest(GET, onPageLoadUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmissionListView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(
          srn,
          schemeName,
          Seq(overviewReport),
          firstPagePagination,
          psaSchemeDashboardUrl
        )(using
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK when accessed as PSP" in {
      val mockSubmissionListService = mock[SubmissionListService]
      when(mockSubmissionListService.getSubmissionList()(using any(), any()))
        .thenReturn(Future.successful(Right(overviewResponse)))

      val application = applicationBuilder(userAnswers = None, isPsa = false)
        .overrides(bind[SubmissionListService].toInstance(mockSubmissionListService))
        .build()

      running(application) {
        val request = FakeRequest(GET, onPageLoadUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmissionListView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(
          srn,
          schemeName,
          Seq(overviewReport),
          firstPagePagination,
          pspSchemeDashboardUrl
        )(using
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to journey recovery when the submission list cannot be retrieved" in {
      val mockSubmissionListService = mock[SubmissionListService]
      when(mockSubmissionListService.getSubmissionList()(using any(), any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("failed", INTERNAL_SERVER_ERROR))))

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[SubmissionListService].toInstance(mockSubmissionListService))
        .build()

      running(application) {
        val request = FakeRequest(GET, onPageLoadUrl)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return OK with no reports when no submission list records are returned" in {
      val mockSubmissionListService = mock[SubmissionListService]
      when(mockSubmissionListService.getSubmissionList()(using any(), any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("No records", UNPROCESSABLE_ENTITY))))

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[SubmissionListService].toInstance(mockSubmissionListService))
        .build()

      running(application) {
        val request = FakeRequest(GET, onPageLoadUrl)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmissionListView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(
          srn,
          schemeName,
          Seq.empty,
          SubmissionListPagination(currentPage = 1, pageSize = 15, totalReports = 0),
          psaSchemeDashboardUrl
        )(using
          request,
          messages(application)
        ).toString
      }
    }

    "must paginate reports and return the requested page" in {
      val mockSubmissionListService = mock[SubmissionListService]
      val reports = (1 to 16).map { index =>
        overviewReport.copy(
          fbNumber = Some(f"1190000043$index%02d"),
          firstForename = Some(if (index % 2 == 0) "Firstnametwo" else "Firstname"),
          secondForename = Some(s"Middle$index"),
          paymentReference = Some(f"PR$index%09d")
        )
      }
      when(mockSubmissionListService.getSubmissionList()(using any(), any()))
        .thenReturn(
          Future.successful(Right(IhtpOverviewResponse(IhtpOverviewSuccess(reports))))
        )

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[SubmissionListService].toInstance(mockSubmissionListService))
        .build()

      running(application) {
        val request = FakeRequest(GET, s"$onPageLoadUrl?page=2")

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmissionListView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(
          srn,
          schemeName,
          Seq(reports(15)),
          SubmissionListPagination(currentPage = 2, pageSize = 15, totalReports = 16),
          psaSchemeDashboardUrl
        )(using
          request,
          messages(application)
        ).toString
      }
    }
  }

  "onAmend" - {
    "must redirect to the right page" in {
      val mockInheritanceTaxOnPensionsConnector = mock[InheritanceTaxOnPensionsConnector]
      when(mockInheritanceTaxOnPensionsConnector.setUserAnswers(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[InheritanceTaxOnPensionsConnector].toInstance(mockInheritanceTaxOnPensionsConnector)
        )
        .build()

      running(application) {
        val postRequest = FakeRequest(GET, onAmendUrl)
          .withFormUrlEncodedBody()

        val result = route(application, postRequest).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.InheritanceTaxReferenceController
          .onPageLoad(srn, NormalMode)
          .url
      }
    }
  }
}
