package io.mikoshift.natsu.architecture

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices

@AnalyzeClasses(
    packages = ["io.mikoshift.natsu"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class LayerArchitectureTest {
    @ArchTest
    val domainShouldNotDependOnOuterLayers: ArchRule =
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

    @ArchTest
    val domainShouldNotUseAndroidFrameworks: ArchRule =
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

    @ArchTest
    val modelShouldNotDependOnOtherLayers: ArchRule =
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

    @ArchTest
    val commonShouldNotDependOnOtherLayers: ArchRule =
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

    @ArchTest
    val featuresShouldNotDependOnDataLayer: ArchRule =
        noClasses()
            .that()
            .resideInAnyPackage("..ui..", "..feature..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..data..")

    @ArchTest
    val featuresShouldNotDependOnEachOther: ArchRule =
        slices()
            .matching("io.mikoshift.natsu.ui.(auth|library|profile|reader)..")
            .should()
            .notDependOnEachOther()

    @ArchTest
    val viewModelsShouldNotUseRetrofitOrRoom: ArchRule =
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

    @ArchTest
    val domainRepositoriesShouldBeInterfaces: ArchRule =
        classes()
            .that()
            .resideInAnyPackage("..core.domain.repository..")
            .and()
            .haveSimpleNameNotEndingWith("DefaultImpls")
            .should()
            .beInterfaces()

    @ArchTest
    val repositoryImplementationsShouldResideInDataLayer: ArchRule =
        classes()
            .that()
            .haveSimpleNameEndingWith("RepositoryImpl")
            .should()
            .resideInAnyPackage("..data.repository..")
}
