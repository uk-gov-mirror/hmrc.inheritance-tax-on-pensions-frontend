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

package connectors

import org.mockito.Mockito.{verify, when}
import config.FrontendAppConfig
import models.addresslookup._
import base.SpecBase
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import models.{JourneyRole, NormalMode}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}

import scala.concurrent.Future

class AddressLookupFrontendConnectorSpec extends SpecBase {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val editPageLabels = AlfEditPageLabels(
    title = "Edit title",
    heading = "Edit heading",
    line1Label = "Line 1",
    line2Label = "Line 2",
    line3Label = "Line 3",
    townLabel = "Town",
    postcodeLabel = "Postcode",
    countryLabel = "Country",
    submitLabel = "Continue"
  )

  private val journeyConfig = AlfJourneyConfig(
    options = AlfOptions(
      continueUrl = "/continue",
      signOutHref = "/sign-out",
      deskProServiceName = "inheritance-tax-on-pensions-frontend",
      phaseFeedbackLink = "/feedback",
      manualAddressEntryConfig = AlfManualAddressEntryConfig(AlfMandatoryFields())
    ),
    labels = AlfLabelsConfig(
      AlfLabels(
        appLevelLabels = AlfAppLabels("Service name", "Phase banner"),
        countryPickerLabels = AlfCountryPickerLabels("Country title", "Country heading", "Country label", "Continue"),
        selectPageLabels = AlfSelectPageLabels("Select title", "Select heading", "Continue", "Edit address"),
        lookupPageLabels = AlfLookupPageLabels("Lookup title", "Lookup heading", "Filter", "Postcode", "Continue"),
        confirmPageLabels = AlfConfirmPageLabels("Confirm title", "Confirm heading", "Use this address"),
        editPageLabels = editPageLabels,
        international = AlfInternationalLabels(editPageLabels)
      )
    )
  )

  private lazy val journeyRoleTestCases = Seq(
    JourneyRole.PrIndividual,
    JourneyRole.PrOrganisation
  )

  "initJourney" - {

    journeyRoleTestCases.foreach { journeyRole =>

      s"must return the Location header from ALF for ${journeyRole.name} journey" in new SetUp {

        val responseWithLocation = mock[HttpResponse]
        when(responseWithLocation.header("Location")).thenReturn(Some("/lookup-address"))

        when(mockConfig.addressLookupFrontendBaseUrl).thenReturn("http://address-lookup-frontend")
        when(mockHttpClient.post(any())(using any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(eqTo(Json.toJson(journeyConfig)))(using any(), any(), any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](using any(), any()))
          .thenReturn(Future.successful(responseWithLocation))

        connector
          .initJourney(srn, NormalMode, journeyConfig, journeyRole)
          .futureValue mustBe "/lookup-address"

        verify(mockHttpClient).post(eqTo(url"http://address-lookup-frontend/api/init"))(using any())
      }

      s"must return the local continue URL when ALF does not return a Location header for ${journeyRole.name} journey" in new SetUp {

        when(mockConfig.addressLookupFrontendBaseUrl).thenReturn("http://address-lookup-frontend")
        when(mockConfig.addressLookupContinueUrl(eqTo(srn), eqTo(NormalMode), eqTo(journeyRole)))
          .thenReturn("/local-continue")
        when(mockHttpClient.post(any())(using any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(eqTo(Json.toJson(journeyConfig)))(using any(), any(), any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](using any(), any()))
          .thenReturn(Future.successful(HttpResponse(202, "")))

        connector
          .initJourney(srn, NormalMode, journeyConfig, journeyRole)
          .futureValue mustBe "/local-continue"
      }
    }
  }

  "getAddress" - {

    "must return the confirmed address from ALF" in new SetUp {

      val expectedAddress = AlfAddressData(
        id = Some("GB123"),
        address = AlfAddress(
          organisation = None,
          lines = Seq("33 Fake Street"),
          town = Some("ABville"),
          postcode = Some("ZZ1 1ZZ"),
          country = AlfCountry("GB", "United Kingdom")
        )
      )

      when(mockConfig.addressLookupFrontendBaseUrl).thenReturn("http://address-lookup-frontend")
      when(mockHttpClient.get(any())(using any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[AlfAddressData](using any(), any()))
        .thenReturn(Future.successful(expectedAddress))

      connector.getAddress("GB123").futureValue mustBe expectedAddress

      verify(mockHttpClient).get(eqTo(url"http://address-lookup-frontend/api/confirmed?id=GB123"))(using any())
    }
  }

  class SetUp {
    val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
    val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
    val connector = new AddressLookupFrontendConnector(mockConfig, mockHttpClient)
  }
}
