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
  val javanet = "java.net" at
    "http://download.java.net/maven/2/"

  val reflections = "Reflections" at
    "http://reflections.googlecode.com/svn/repo"

  override def libraryDependencies =
    Set(
      // Projects using S3m must provide a concrete SLF4J implentation (Logback etc.)
      "org.slf4j"       % "slf4j-api"      % "1.6.1" % "provided",

      "javax"           % "javaee-web-api" % "6.0"   % "provided",

      // For scanning all Controllers to build routes
      "org.reflections" % "reflections"    % "0.9.5-RC2"  // Uses Javassist
    ) ++ super.libraryDependencies
}
