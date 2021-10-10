import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.5.5"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("jvm") version "1.5.31"
	kotlin("plugin.spring") version "1.5.31"
//	kotlin("kapt") version "1.5.31"
}

group = "com.joelcwe.lowyat"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
	implementation("org.jsoup:jsoup:1.14.1")
	implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.github.microutils:kotlin-logging:1.12.5")
	implementation(("org.springframework.boot:spring-boot-configuration-processor"))
//	Kapt not working in newer JDKs > 1.8
//	kapt("org.springframework.boot:spring-boot-configuration-processor")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
	testImplementation("org.mockito:mockito-junit-jupiter:3.11.2")
	testImplementation("io.mockk:mockk:1.12.0")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.1")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.5.1")
	testImplementation("app.cash.turbine:turbine:0.6.0")
	testImplementation("com.ninja-squad:springmockk:3.0.1")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf(
			"-Xjsr305=strict",
			"-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
			"-Xuse-experimental=kotlinx.coroutines.ObsoleteCoroutinesApi"
		)
		jvmTarget = "11"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.compileTestKotlin {
	kotlinOptions {
		freeCompilerArgs += listOf(
			"-Xopt-in=kotlin.time.ExperimentalTime",
		)
	}
}
