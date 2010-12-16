import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info) {
  // Compile options

  override def compileOptions = super.compileOptions ++
    Seq("-deprecation",
        "-Xmigration",
        "-Xcheckinit",
        "-Xwarninit",
        "-encoding", "utf8")
        .map(x => CompileOption(x))

  override def javaCompileOptions = JavaCompileOption("-Xlint:unchecked") :: super.javaCompileOptions.toList

  // Repos ---------------------------------------------------------------------

  // For Servlet 3.0 API
  val javanet = "java.net" at "http://download.java.net/maven/2/"

  override def libraryDependencies =
    Set(
      // For scanning all Controllers to build routes
      "com.impetus" % "annovention"    % "1.0-SNAPSHOT" from "http://cloud.github.com/downloads/ngocdaothanh/annovention/annovention-1.0-SNAPSHOT.jar",
      "javassist"   % "javassist"      % "3.10.0.GA",  // annovention's only dependency

      // Projects using S3m must provide a concrete SLF4J implentation (Logback etc.)
      "org.slf4j"   % "slf4j-api"      % "1.6.1" % "provided",

      // Servlet 3.0 API
      "javax"       % "javaee-web-api" % "6.0"   % "provided"
    ) ++ super.libraryDependencies
}
