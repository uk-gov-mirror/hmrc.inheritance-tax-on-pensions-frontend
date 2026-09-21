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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.InheritanceTaxReferencePage
import controllers.actions._
import forms.InheritanceTaxReferenceFormProvider
import models._
import play.api.data.Form
import views.html.InheritanceTaxReferenceView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class InheritanceTaxReferenceController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionWithSessionCacheProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: InheritanceTaxReferenceFormProvider,
  val controllerComponents: MessagesControllerComponents,
  userAnswersService: UserAnswersService,
  view: InheritanceTaxReferenceView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData) { implicit request =>
        request.userAnswers.get(InheritanceTaxReferencePage) match {
          case Some(input) => Ok(view(form.fill(input), srn, mode))
          case _ => Ok(view(form, srn, mode))
        }
      }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(getData)
      .andThen(requireData)
      .async { implicit request =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, srn, mode))),
            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(InheritanceTaxReferencePage, value))
                result <- userAnswersService.set(updatedAnswers)(using hc, request.request)
              } yield result match {
                case Right(returnedUserAnswers) =>
                  val newUuid = returnedUserAnswers.uuid
                  val updatedSession = if (request.session.get("uuid").contains(newUuid)) {
                    request.session
                  } else {
                    request.session + ("uuid" -> newUuid)
                  }
                  mode match {
                    case CheckMode =>
                      Redirect(routes.CheckYourAnswersController.onPageLoad(srn))
                        .withSession(updatedSession)
                    case NormalMode =>
                      Redirect(routes.IndividualNameController.onPageLoad(srn, NormalMode, JourneyRole.Deceased))
                        .withSession(updatedSession)
                  }
                case Left(_) =>
                  BadRequest(view(form.fill(value), srn, mode))
              }
          )
      }
}
