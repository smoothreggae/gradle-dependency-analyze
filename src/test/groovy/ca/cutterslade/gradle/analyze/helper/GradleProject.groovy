package ca.cutterslade.gradle.analyze.helper

class GradleProject {

    final String name
    final boolean rootProject

    Set<GradleProject> subProjects = []
    Set<GroovyClass> mainClasses = []
    Set<GroovyClass> testClasses = []
    Set<GroovyClass> testFixturesClasses = []
    Set<String> plugins = []
    Set<GradleDependency> dependencies = []
    String repositories
    String platformConfiguration = ""

    GradleProject(String name, boolean rootProject = false) {
        this.name = name
        this.rootProject = rootProject
    }

    def withSubProject(GradleProject project) {
        subProjects.add(project)
        this
    }

    def withMainClass(GroovyClass clazz) {
        mainClasses.add(clazz)
        this
    }

    def withTestClass(GroovyClass clazz) {
        testClasses.add(clazz)
        this
    }

    def withTestFixturesClass(GroovyClass clazz) {
        testFixturesClasses.add(clazz)
        this
    }

    def withPlugin(String plugin) {
        plugins.add(plugin)
        this
    }

    def withDependency(GradleDependency dep) {
        dependencies.add(dep)
        this
    }

    def withGradleDependency(String configuration) {
        dependencies.add(new GradleDependency(configuration: configuration, reference: 'localGroovy()'))
        this
    }

    def withAggregator(GradleDependency aggregator) {
        dependencies.add(aggregator)
        this
    }

    def withMavenRepositories() {
        repositories = "repositories {\n" +
                "    mavenLocal()\n" +
                "    mavenCentral()\n" +
                "}\n"
        this
    }

    def applyPlatformConfiguration() {
        platformConfiguration = "" +
                "configurations {\n" +
                "    myPlatform {\n" +
                "        canBeResolved = false\n" +
                "        canBeConsumed = false\n" +
                "    }\n" +
                "}\n" +
                "configurations.all {\n" +
                "    if (canBeResolved) {\n" +
                "        extendsFrom(configurations.myPlatform)\n" +
                "    }\n" +
                "}\n" +
                "dependencies {\n" +
                "    myPlatform platform(project(':platform'))" +
                "}\n"
        this
    }

    void create(File root) {
        root.mkdirs()
        subProjects.each { it.create(new File(root, it.name)) }

        createBuildGradle(root)
        createSettingsGradle(root)

        if (!mainClasses.empty) {
            createClasses(root, "src/main/groovy", mainClasses)
        }
        if (!testClasses.empty) {
            createClasses(root, "src/test/groovy", testClasses)
        }
        if (!testFixturesClasses.empty) {
            createClasses(root, "src/testFixtures/groovy", testFixturesClasses)
        }
    }

    private static void createClasses(File root, String dir, Set<GroovyClass> classes) {

        def sourceDir = new File(root, dir)
        if (!sourceDir.mkdirs()) {
            throw new IllegalStateException("Could not create source dir ${sourceDir}")
        }

        for (def clazz : classes) {
            clazz.create(sourceDir)
        }
    }

    private void createSettingsGradle(File root) {
        def settingsGradle = ""
        if (name != null) {
            settingsGradle += "rootProject.name = '${name}'\n"
        }

        for (def subProject : subProjects) {
            settingsGradle += "include(':${subProject.name}')\n"
        }

        if (!settingsGradle.isEmpty()) {
            new File(root, "settings.gradle").text = settingsGradle
        }
    }

    private void createBuildGradle(File root) {
        def buildGradle = ""
        if (!plugins.isEmpty()) {
            buildGradle += "plugins {\n"
            for (def plugin : plugins) {
                buildGradle += "  id '${plugin}'\n"
            }
            buildGradle += "}\n"
        }
        if (plugins.contains('java-platform')) {
            buildGradle += "javaPlatform {\n" +
                    "    allowDependencies()\n" +
                    "}\n"
        }
        buildGradle += repositories ?: ''
        if (!dependencies.isEmpty()) {
            buildGradle += "dependencies {\n"
            for (def dep : dependencies) {
                buildGradle += "  ${dep.get()}\n"
            }
            buildGradle += "}\n"
        }
        buildGradle += platformConfiguration

        new File(root, "build.gradle").text = buildGradle
    }
}
