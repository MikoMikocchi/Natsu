package io.mikoshift.natsu.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.jupiter.api.Test

class LayerArchitectureTest {
    private val productionClasses: JavaClasses =
        ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.mikoshift.natsu")

    @Test
    fun domainShouldNotDependOnOuterLayers() {
        noClasses()
            .that()
            .resideInAnyPackage("..core.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "android..",
                "androidx..",
                "..data..",
                "..ui..",
                "..feature..",
                "..navigation..",
            )
            .check(productionClasses)
    }

    @Test
    fun domainShouldNotUseAndroidFrameworks() {
        noClasses()
            .that()
            .resideInAnyPackage("..core.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "retrofit2..",
                "okhttp3..",
                "androidx.room..",
            )
            .check(productionClasses)
    }

    @Test
    fun modelShouldNotDependOnOtherLayers() {
        noClasses()
            .that()
            .resideInAnyPackage("..core.model..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..core.domain..",
                "..data..",
                "..ui..",
                "..feature..",
                "android..",
                "androidx..",
            )
            .check(productionClasses)
    }

    @Test
    fun commonShouldNotDependOnOtherLayers() {
        noClasses()
            .that()
            .resideInAnyPackage("..core.common..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..core.domain..",
                "..core.model..",
                "..data..",
                "..ui..",
                "..feature..",
                "android..",
                "androidx..",
            )
            .check(productionClasses)
    }

    @Test
    fun featuresShouldNotDependOnDataLayer() {
        noClasses()
            .that()
            .resideInAnyPackage("..ui..", "..feature..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..data..")
            .check(productionClasses)
    }

    @Test
    fun featuresShouldNotDependOnEachOther() {
        slices()
            .matching("io.mikoshift.natsu.ui.(auth|library|profile|reader)..")
            .should()
            .notDependOnEachOther()
            .check(productionClasses)
    }

    @Test
    fun viewModelsShouldNotUseRetrofitOrRoom() {
        noClasses()
            .that()
            .haveSimpleNameEndingWith("ViewModel")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "retrofit2..",
                "okhttp3..",
                "androidx.room..",
            )
            .check(productionClasses)
    }

    @Test
    fun domainRepositoriesShouldBeInterfaces() {
        classes()
            .that()
            .resideInAnyPackage("..core.domain.repository..")
            .and()
            .haveSimpleNameNotEndingWith("DefaultImpls")
            .should()
            .beInterfaces()
            .check(productionClasses)
    }

    @Test
    fun repositoryImplementationsShouldResideInDataLayer() {
        classes()
            .that()
            .haveSimpleNameEndingWith("RepositoryImpl")
            .should()
            .resideInAnyPackage("..data.repository..")
            .check(productionClasses)
    }
}
