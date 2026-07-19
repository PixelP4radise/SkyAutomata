plugins {
	id("net.fabricmc.fabric-loom-remap")
	`maven-publish`
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

repositories {
	// Add repositories to retrieve artifacts from in here.
	// You should only use this when depending on other mods because
	// Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
	// See https://docs.gradle.org/current/userguide/declaring_repositories.html
	// for more information about repositories.
	maven {
		name = "ParchmentMC"
		url = uri("https://maven.parchmentmc.org")
	}
	maven {
		name = "DevAuth"
		url = uri("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
	}
}

loom {
	splitEnvironmentSourceSets()

	mods {
		register("sky-automata") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.getByName("client"))
		}
	}

	runs {
		named("client") {
			// Lets runClient log into a real Microsoft account instead of an offline/fake one.
			// DevAuth is modRuntimeOnly below, so it never ships in the built mod jar.
			// vmArgs() (like every other RunConfigSettings mutator) logs a Loom deprecation
			// warning on this pinned loom_version=1.17-SNAPSHOT — Loom is mid-migration to a
			// new RunConfiguration API that isn't exposed on LoomGradleExtensionAPI yet, so
			// there's currently no non-deprecated way to set this; harmless until Loom ships
			// the replacement entry point.
			vmArgs("-Ddevauth.enabled=true")
		}
	}
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	mappings(loom.layered {
		officialMojangMappings()
		parchment("org.parchmentmc.data:parchment-${providers.gradleProperty("minecraft_version").get()}:${providers.gradleProperty("parchment_version").get()}@zip")
	})
	modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// Fabric API. This is technically optional, but you probably want it anyway.
	modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")

	// Lets the dev client log into a real Microsoft account (see the loom.runs.client
	// block above) instead of joining as an offline/fake account. Dev-only: runtimeOnly
	// so it's never bundled into the built mod jar.
	modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:${providers.gradleProperty("devauth_version").get()}")

	// Baritone: client-only automation/pathing bot. A pre-release build from the Baritone
	// team for 1.21.11 (matches "Fabric-Minecraft-Version" in its manifest; verified via
	// checksums.txt), not yet published to a maven repo, hence the local file dependency.
	// modClientImplementation (not modImplementation) keeps it out of src/main entirely —
	// leaking it into common code crashes a dedicated server with NoClassDefFoundError.
	"modClientImplementation"(files("baritone-api-fabric-1.15.0-8-gbc3dcde2.jar"))

	// Baritone embeds nether-pathfinder-1.6.jar (dev.babbaj.pathfinder.NetherPathfinder) as a
	// Fabric jar-in-jar nested dependency (see "jars" in its fabric.mod.json). That nested jar
	// has no fabric.mod.json of its own, and Fabric Loader only unpacks jar-in-jar nested jars
	// for mods discovered from the mods/ folder — mods pulled in via modClientImplementation
	// sit directly on the dev classpath and skip that extraction, so Baritone crashes at
	// startup with NoClassDefFoundError: dev/babbaj/pathfinder/NetherPathfinder unless this is
	// declared explicitly. Extracted straight out of the Baritone jar
	// (unzip -p baritone-api-fabric-1.15.0-8-gbc3dcde2.jar META-INF/jars/nether-pathfinder-1.6.jar)
	// to guarantee the exact version this Baritone build was compiled against. Plain
	// clientImplementation, not modClientImplementation — it's not a Fabric mod and touches no
	// Minecraft classes, so it needs no remapping.
	"clientImplementation"(files("nether-pathfinder-1.6.jar"))
}

tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 21
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

// configure the maven publication
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}
