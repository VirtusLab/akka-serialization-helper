package org.virtuslab.psh

import better.files.Dsl.SymbolicOperations
import better.files.File
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

class CodecRegistrationCheckerCompilerPluginSpec extends AnyWordSpecLike with should.Matchers {
  private def getSerializerAsString(name: String) =
    (File(getClass.getClassLoader.getResource("serializers")) / (name + ".scala")).contentAsString

  private val dataSourceCode =
    (for (f <- File(getClass.getClassLoader.getResource("data")).children) yield f.contentAsString).toList

  private val CORRECT_SERIALIZER_CODE = getSerializerAsString("CorrectSerializer")
  private val EMPTY_SERIALIZER_CODE = getSerializerAsString("EmptySerializer")
  private val INCOMPLETE_SERIALIZER_ONE_CODE = getSerializerAsString("IncompleteSerializer")
  private val INCOMPLETE_SERIALIZER_TWO_CODE = getSerializerAsString("IncompleteSerializerTwo")
  private val INVALID_ANNOTATION_SERIALIZER_CODE = getSerializerAsString("InvalidAnnotationSerializer")
  private val INVALID_CLASS_SERIALIZER_CODE = getSerializerAsString("InvalidClassSerializer")
  private val OBJECT_SERIALIZER_CODE = getSerializerAsString("ObjectSerializer")
  private val MACRO_REGEX_SERIALIZER_CODE = getSerializerAsString("MacroRegexSerializer")

  "Codec registration checker compiler plugin" should {
    "detect correct registration for all kinds of classes" in {
      File.usingTemporaryDirectory() { directory =>
        val out = CodecRegistrationCheckerCompiler.compileCode(
          CORRECT_SERIALIZER_CODE :: dataSourceCode,
          List(s"${directory.toJava.getAbsolutePath}"))
        out should be("")
      }
    }

    "detect serializer as object" in {
      File.usingTemporaryDirectory() { directory =>
        val out = CodecRegistrationCheckerCompiler.compileCode(
          OBJECT_SERIALIZER_CODE :: dataSourceCode,
          List(s"${directory.toJava.getAbsolutePath}"))
        out should include("error")
      }
    }

    "raise an error" when {
      "types don't match type regex pattern" in {
        File.usingTemporaryDirectory() { directory =>
          val out = CodecRegistrationCheckerCompiler.compileCode(
            EMPTY_SERIALIZER_CODE :: dataSourceCode,
            List(s"${directory.toJava.getAbsolutePath}"))
          out should include("error")
        }
      }

      "types are missing in serializer definition" in {
        File.usingTemporaryDirectory() { directory =>
          val out = CodecRegistrationCheckerCompiler.compileCode(
            INCOMPLETE_SERIALIZER_ONE_CODE :: dataSourceCode,
            List(s"${directory.toJava.getAbsolutePath}"))
          out should include("error")
          (out should not).include("literal")
        }
      }

      "annotation value is not a compile time literal" in {
        File.usingTemporaryDirectory() { directory =>
          val out = CodecRegistrationCheckerCompiler.compileCode(
            INVALID_ANNOTATION_SERIALIZER_CODE :: dataSourceCode,
            List(s"${directory.toJava.getAbsolutePath}"))
          out should include("error")
        }
      }

      "class in annotation is not annotated with @SerializabilityTrait" in {
        File.usingTemporaryDirectory() { directory =>
          val out = CodecRegistrationCheckerCompiler.compileCode(
            INVALID_CLASS_SERIALIZER_CODE :: dataSourceCode,
            List(s"${directory.toJava.getAbsolutePath}"))
          out should include("error")
        }
      }

      "type that is missing in serializer definition is wrapped in an object" in {
        File.usingTemporaryDirectory() { directory =>
          val out = CodecRegistrationCheckerCompiler.compileCode(
            INCOMPLETE_SERIALIZER_TWO_CODE :: dataSourceCode,
            List(s"${directory.toJava.getAbsolutePath}"))
          out should include("error")
          (out should not).include("literal")
        }
      }
    }

    "work with no serializer" in {
      File.usingTemporaryDirectory() { directory =>
        val out =
          CodecRegistrationCheckerCompiler.compileCode(dataSourceCode, List(s"${directory.toJava.getAbsolutePath}"))
        out should be("")
      }
    }

    "create cache file when missing" in {
      File.usingTemporaryDirectory() { directory =>
        val out =
          CodecRegistrationCheckerCompiler.compileCode(dataSourceCode, List(s"${directory.toJava.getAbsolutePath}"))
        out should be("")
        val cacheFile =
          (directory / CodecRegistrationCheckerCompilerPlugin.directClassDescendantsCacheFileName).contentAsString
        cacheFile should be("""org.random.project.SerializableTrait,org.random.project.GenericData
                              |org.random.project.SerializableTrait,org.random.project.IndirectData
                              |org.random.project.SerializableTrait,org.random.project.StdData
                              |org.random.project.SerializableTrait,org.random.project.Wrapper.NestedData""".stripMargin)
      }
    }

    "use existing from cache file" in {
      File.usingTemporaryDirectory() { directory =>
        val cacheFile = directory / CodecRegistrationCheckerCompilerPlugin.directClassDescendantsCacheFileName
        cacheFile < "org.random.project.SerializableTrait,org.random.project.MissingData"
        val out =
          CodecRegistrationCheckerCompiler.compileCode(dataSourceCode, List(s"${directory.toJava.getAbsolutePath}"))
        out should be("")
        val cacheFileAfter = cacheFile.contentAsString
        cacheFileAfter should be("""org.random.project.SerializableTrait,org.random.project.GenericData
                                   |org.random.project.SerializableTrait,org.random.project.IndirectData
                                   |org.random.project.SerializableTrait,org.random.project.MissingData
                                   |org.random.project.SerializableTrait,org.random.project.StdData
                                   |org.random.project.SerializableTrait,org.random.project.Wrapper.NestedData""".stripMargin)
      }
    }

    "fail to initialise" when {
      "integrity of cache file is compromised" in {
        assertThrows[RuntimeException] {
          File.usingTemporaryDirectory() { directory =>
            val cacheFile = directory / CodecRegistrationCheckerCompilerPlugin.directClassDescendantsCacheFileName
            cacheFile < "org.random.project.SerializableTrait"
            CodecRegistrationCheckerCompiler.compileCode(dataSourceCode, List(s"${directory.toJava.getAbsolutePath}"))
          }
        }
      }

      "path in argument contains in invalid" in {
        assertThrows[RuntimeException] {
          File.usingTemporaryDirectory() { directory =>
            CodecRegistrationCheckerCompiler.compileCode(dataSourceCode, List(s"${directory.toJava.getAbsolutePath}\u0000"))
          }
        }
      }

      "no path is specified" in {
        assertThrows[RuntimeException] {
          CodecRegistrationCheckerCompiler.compileCode(dataSourceCode, Nil)
        }
      }
    }

    "compile with REGISTRATION_REGEX macro" in {
      File.usingTemporaryDirectory() { directory =>
        val out = CodecRegistrationCheckerCompiler.compileCode(
          List(MACRO_REGEX_SERIALIZER_CODE, dataSourceCode.find(_.contains("@SerializabilityTrait")).get),
          List(s"${directory.toJava.getAbsolutePath}"))
        out should be("")
      }
    }

    "compile without error when there were outdated type names in the cache file before start and remove them from the file" in {
      File.usingTemporaryDirectory() { directory =>
        val cacheFile = directory / CodecRegistrationCheckerCompilerPlugin.directClassDescendantsCacheFileName
        // hydra.test.TestPekkoSerializable and hydra.test.ConcreteClasses do not exist in the code anymore (outdated types)
        cacheFile.write(
          "org.random.project.SerializableTrait,hydra.test.TestPekkoSerializable\n" +
            "hydra.test.TestPekkoSerializable,hydra.test.ConcreteClasses")
        val out = CodecRegistrationCheckerCompiler.compileCode(
          CORRECT_SERIALIZER_CODE :: dataSourceCode,
          List(s"${directory.toJava.getAbsolutePath}"))
        out should be("")
        cacheFile.contentAsString should be("""org.random.project.SerializableTrait,org.random.project.GenericData
                                              |org.random.project.SerializableTrait,org.random.project.IndirectData
                                              |org.random.project.SerializableTrait,org.random.project.StdData
                                              |org.random.project.SerializableTrait,org.random.project.Wrapper.NestedData""".stripMargin)
      }
    }

  }
}
