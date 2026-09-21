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

import services.CountryService
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import com.google.inject.Inject
import utils.CheckYourAnswersHelper.{buildSummaryLists, findPageToContinue}
import controllers.actions._
import play.api.Logging
import models.UserAnswers
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  countryService: CountryService
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>

      val userAnswers: UserAnswers = request.userAnswers
      val continuePage = findPageToContinue(userAnswers, srn)
      continuePage match {
        case Some(value) =>
          Redirect(routes.CheckYourAnswersController.onPageLoadContinueMode(srn))
        case None =>
          val checkYourAnswersSummaryLists =
            buildSummaryLists(userAnswers, srn, countryService.nameForCode, messagesApi)
          Ok(
            view(
              srn,
              checkYourAnswersSummaryLists.deceasedDetailsSummaryList,
              checkYourAnswersSummaryLists.prDetailsSummaryList,
              checkYourAnswersSummaryLists.paymentNoticeDetailsSummaryList,
              checkYourAnswersSummaryLists.beneficiaryList,
              continuePage
            )
          )
      }
    }

  def onPageLoadContinueMode(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>

      val userAnswers: UserAnswers = request.userAnswers
      val continuePage = findPageToContinue(userAnswers, srn)
      val checkYourAnswersSummaryLists = buildSummaryLists(userAnswers, srn, countryService.nameForCode, messagesApi)
      Ok(
        view(
          srn,
          checkYourAnswersSummaryLists.deceasedDetailsSummaryList,
          checkYourAnswersSummaryLists.prDetailsSummaryList,
          checkYourAnswersSummaryLists.paymentNoticeDetailsSummaryList,
          checkYourAnswersSummaryLists.beneficiaryList,
          continuePage
        )
      )
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      if (request.request.pensionSchemeId.isPSP) {
        Redirect(routes.PspDeclarationController.onPageLoad(srn))
      } else {
        Redirect(routes.PsaDeclarationController.onPageLoad(srn))
      }
    }
}
