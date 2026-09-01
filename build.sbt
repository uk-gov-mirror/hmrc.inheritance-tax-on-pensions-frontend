import play.sbt.routes.RoutesKeys
import sbt.Def
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val appName: String = "inheritance-tax-on-pensions-frontend"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.7.1"

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(inConfig(Test)(testSettings): _*)
  .settings(ThisBuild / useSuperShell := false)
  .settings(
    name := appName,
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    RoutesKeys.routesImport ++= Seq(
      "models._",
      "models.SchemeId.*",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl",
      "config.Binders.*"
    ),
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.config._",
      "views.ViewUtils._",
      "models.Mode",
      "controllers.routes._",
      "viewmodels.govuk.all._"
    ),
    PlayKeys.playDefaultPort := 10711,
    scalacOptions ++= Seq(
      "-feature",
      "-Wconf:msg=Flag.*repeatedly:s",
      "-Wconf:msg=unused import&src=.*views/.*:s",
      "-Wconf:msg=unused&src=.*Routes\\.scala:s",
      "-Wconf:msg=match may not be exhaustive.&src=.*routes:s",
      "-Xmax-inlines",
      "100",
      "-language:implicitConversions"
    ),
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    pipelineStages := Seq(digest),
    Assets / pipelineStages := Seq(concat),
    scalafmtOnCompile := true,
    scalafixOnCompile := true
  )
  .settings(CodeCoverageSettings.settings: _*)

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork := true,
  unmanagedSourceDirectories += baseDirectory.value / "test-utils"
)

lazy val it =
  (project in file("it"))
    .enablePlugins(PlayScala)
    .dependsOn(microservice % "test->test")
    .settings(
      scalacOptions ++= Seq(
        "-Wconf:msg=Flag.*repeatedly:s"
      )
    )
