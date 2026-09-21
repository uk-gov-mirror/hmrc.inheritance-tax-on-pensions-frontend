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

import services.SubmissionListService
import utils.SubmissionListUtil
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.FrontendAppConfig
import controllers.actions.{AllowAccessActionProvider, IdentifierAction}
import views.html.SubmissionListView
import models.SchemeId.Srn
import play.api.i18n.I18nSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

import javax.inject.Inject

class SubmissionListController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider, // Invalidate the authorisation cache and re-authenticate
  submissionListService: SubmissionListService,
  appConfig: FrontendAppConfig,
  submissionListUtil: SubmissionListUtil,
  view: SubmissionListView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).async { implicit request =>
      submissionListService.getSubmissionList().map { response =>
        submissionListUtil.prepare(response, paid = false, request.getQueryString("page")) match {
          case Right((reports, pagination)) =>
            Ok(
              view(
                srn,
                request.schemeDetails.schemeName,
                reports,
                pagination,
                appConfig.schemeDashboardUrl(srn, request.pensionSchemeId)
              )
            )
          case Left(_) => Redirect(routes.JourneyRecoveryController.onPageLoad())
        }
      }
    }

  def onAmend(srn: Srn, uuid: String): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn)) { implicit request =>
        val updatedSession = if (request.session.get("uuid").contains(uuid)) {
          request.session
        } else {
          request.session + ("uuid" -> uuid)
        }

        Redirect(controllers.routes.CheckYourAnswersController.onPageLoad(srn))
          .withSession(updatedSession)
      }

}
