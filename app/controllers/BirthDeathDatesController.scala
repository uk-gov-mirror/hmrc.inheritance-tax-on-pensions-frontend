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

import services.UserAnswersService
import utils.DeceasedNameHelper
import play.api.mvc._
import pages.BirthDeathDatesPage
import controllers.actions._
import forms.BirthDeathDatesFormProvider
import models.{CheckMode, Mode, NormalMode}
import play.api.i18n.MessagesApi
import views.html.BirthDeathDatesView
import models.SchemeId.Srn

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class BirthDeathDatesController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: BirthDeathDatesFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: BirthDeathDatesView
)(implicit ec: ExecutionContext)
    extends IhtpBaseController {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        DeceasedNameHelper.withName(request.userAnswers)(
          logAndJourneyRecovery("deceased name is missing, cannot load the page")
        ) { deceasedName =>
          val form = formProvider()

          val preparedForm = request.userAnswers.get(BirthDeathDatesPage) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, srn, mode, deceasedName))
        }
      }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        DeceasedNameHelper.withName(request.userAnswers) {
          Future.successful(logAndJourneyRecovery("deceased name is missing, cannot submit the page"))
        } { deceasedName =>
          val form = formProvider()

          formProvider
            .validate(form.bindFromRequest())
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, srn, mode, deceasedName))),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(BirthDeathDatesPage, value))
                  _ <- userAnswersService.set(updatedAnswers)(using hc, request.request)
                } yield Redirect(nextPage(srn, mode))
            )
        }
      }

  private def nextPage(srn: Srn, mode: Mode) =
    mode match {
      case NormalMode => routes.PrTypeController.onPageLoad(srn, NormalMode)
      case CheckMode => routes.CheckYourAnswersController.onPageLoad(srn)
    }
}
