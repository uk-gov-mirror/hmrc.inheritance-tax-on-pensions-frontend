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

package services

import play.api.test.FakeRequest
import connectors.AddressLookupFrontendConnector
import controllers.routes
import config.FrontendAppConfig
import models.addresslookup._
import base.SpecBase
import uk.gov.hmrc.http.HeaderCarrier
import models.{JourneyRole, NormalMode}
import play.api.i18n.MessagesApi
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import play.api.test.Helpers.{running, GET}
import org.mockito.Mockito.{verify, when}

import scala.concurrent.Future

class AddressLookupFrontendServiceSpec extends SpecBase {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val request: FakeRequest[?] =
    FakeRequest(GET, "/inheritance-tax-on-pensions/S2400000001/check-your-answers")

  private lazy val journeyRoleTestCases = Seq(
    JourneyRole.PrIndividual,
    JourneyRole.PrOrganisation
  )

  "initJourney" - {

    journeyRoleTestCases.foreach { journeyRole =>
      s"must build the ALF journey config and return the connector URL for ${journeyRole.name} journey" in {

        val mockConnector = mock[AddressLookupFrontendConnector]

        val application = applicationBuilder().build()

        running(application) {
          val config = application.injector.instanceOf[FrontendAppConfig]
          val messagesApi = application.injector.instanceOf[MessagesApi]
          val countryService = application.injector.instanceOf[CountryService]
          val service = new AddressLookupFrontendService(config, mockConnector, messagesApi, countryService)

          when(mockConnector.initJourney(eqTo(srn), eqTo(NormalMode), any(), any())(using any()))
            .thenReturn(Future.successful("/lookup-address"))

          val result = service.initJourney(srn, NormalMode, "Firstname Surname", journeyRole).futureValue

          result mustBe "/lookup-address"

          val configCaptor: ArgumentCaptor[AlfJourneyConfig] = ArgumentCaptor.forClass(classOf[AlfJourneyConfig])
          verify(mockConnector).initJourney(eqTo(srn), eqTo(NormalMode), configCaptor.capture(), any())(using any())

          val journeyConfig = configCaptor.getValue
          journeyConfig.options.continueUrl must include(
            routes.AddressLookupContinueController.continue(srn, NormalMode, journeyRole).url
          )
          journeyConfig.options.signOutHref mustBe config.signOutSurveyUrl
          journeyConfig.options.phaseFeedbackLink mustBe config.feedbackUrl
          journeyConfig.options.allowedCountryCodes mustBe Some(countryService.countries.map(_.code))
          journeyConfig.options.selectPageConfig.showNoneOfTheseOption mustBe false
          journeyConfig.options.manualAddressEntryConfig.mandatoryFields.addressLine1 mustBe true
          journeyConfig.options.manualAddressEntryConfig.mandatoryFields.addressLine2 mustBe true
          journeyConfig.options.manualAddressEntryConfig.showOrganisationName mustBe false
          journeyConfig.labels.en.countryPickerLabels.heading must include("Firstname Surname")
          journeyConfig.labels.en.appLevelLabels.phaseBannerHtml mustBe
            messagesApi.preferred(Seq.empty)("addressLookup.phaseBannerHtml")
          journeyConfig.labels.en.lookupPageLabels.heading must include("Firstname Surname")
          journeyConfig.labels.en.international.editPageLabels.heading must include("Firstname Surname")
        }
      }
    }
  }

  "getAddress" - {

    "must return the address from the connector" in {

      val mockConfig = mock[FrontendAppConfig]
      val mockConnector = mock[AddressLookupFrontendConnector]
      val mockMessagesApi = mock[MessagesApi]
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

      when(mockConnector.getAddress(eqTo("GB123"))(using any()))
        .thenReturn(Future.successful(expectedAddress))

      val mockCountryService = mock[CountryService]
      val service = new AddressLookupFrontendService(mockConfig, mockConnector, mockMessagesApi, mockCountryService)

      service.getAddress("GB123").futureValue mustBe expectedAddress
      verify(mockConnector).getAddress(eqTo("GB123"))(using any())
    }
  }
}
