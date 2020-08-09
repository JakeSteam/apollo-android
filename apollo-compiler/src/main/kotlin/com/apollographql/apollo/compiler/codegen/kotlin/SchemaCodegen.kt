package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ast.CustomTypes
import com.apollographql.apollo.compiler.ast.EnumType
import com.apollographql.apollo.compiler.ast.InputType
import com.apollographql.apollo.compiler.ast.ObjectType
import com.apollographql.apollo.compiler.ast.OperationType
import com.apollographql.apollo.compiler.ast.SchemaVisitor
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.patchKotlinNativeOptionalArrayProperties
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

internal class SchemaCodegen(
    private val typesPackageName: String,
    private val fragmentsPackageName: String,
    private val generateAsInternal: Boolean = false,
    private val kotlinMultiPlatformProject: Boolean,
    private val writeFragmentsAndTypes: Boolean = true,
    private val enumAsSealedClassPatternFilters: List<Regex>
) : SchemaVisitor {
  private var fileSpecs: List<FileSpec> = emptyList()

  override fun visit(customTypes: CustomTypes) {
    if (writeFragmentsAndTypes) {
      fileSpecs = fileSpecs + customTypes.typeSpec(generateAsInternal).fileSpec(typesPackageName)
    }
  }

  override fun visit(enumType: EnumType) {
    if (writeFragmentsAndTypes) {
      fileSpecs = fileSpecs + enumType.typeSpec(
          generateAsInternal = generateAsInternal,
          enumAsSealedClassPatternFilters = enumAsSealedClassPatternFilters
      ).fileSpec(typesPackageName)
    }
  }

  override fun visit(inputType: InputType) {
    if (writeFragmentsAndTypes) {
      val inputTypeSpec = inputType.typeSpec(generateAsInternal)
      fileSpecs = fileSpecs + inputTypeSpec.fileSpec(typesPackageName)
    }
  }

  override fun visit(fragmentType: ObjectType) {
    if (writeFragmentsAndTypes) {
      val fragmentTypeSpec = fragmentType.typeSpec(generateAsInternal).let {
        if (kotlinMultiPlatformProject) {
          it.patchKotlinNativeOptionalArrayProperties()
        } else it
      }
      fileSpecs = fileSpecs + fragmentTypeSpec.fileSpec(fragmentsPackageName)
    }
  }

  override fun visit(operationType: OperationType) {
    val targetPackage = operationType.packageName
    val operationTypeSpec = operationType.typeSpec(
        targetPackage = targetPackage,
        generateAsInternal = generateAsInternal
    ).let {
      if (kotlinMultiPlatformProject) {
        it.patchKotlinNativeOptionalArrayProperties()
      } else it
    }
    fileSpecs = fileSpecs + operationTypeSpec.fileSpec(targetPackage)
  }

  fun writeTo(outputDir: File) {
    fileSpecs.forEach { it.writeTo(outputDir) }
  }

  private fun TypeSpec.fileSpec(packageName: String) =
      FileSpec
          .builder(packageName, name!!)
          .addType(this)
          .addComment("AUTO-GENERATED FILE. DO NOT MODIFY.\n\n" +
              "This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.\n" +
              "It should not be modified by hand.\n"
          )
          .build()
}
