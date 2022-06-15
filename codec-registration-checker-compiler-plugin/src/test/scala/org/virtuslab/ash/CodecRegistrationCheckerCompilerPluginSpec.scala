package org.virtuslab.ash

import better.files.Dsl.SymbolicOperations
import better.files.File
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

class CodecRegistrationCheckerCompilerPluginSpec extends AnyWordSpecLike with should.Matchers {
  private def getSerializerAsString(name: String) =
    (File(getClass.getClassLoader.getResource("serializers")) / (name + ".scala")).contentAsString

  private val dataSourceCode =
    (for (f <- File(getClass.getClassLoader.getResource("data")).children) yield f.contentAsString).toList

  private val sourceCodeDirectory = File(getClass.getClassLoader.getResource(".")).path.toString
  println(s"dir is $sourceCodeDirectory")

  private val serializersCode = Array(
    "CorrectSerializer",
    "EmptySerializer",
    "IncompleteSerializer",
    "InvalidAnnotationSerializer",
    "InvalidClassSerializer",
    "ObjectSerializer",
    "MacroRegexSerializer").map(getSerializerAsString)

  "Codec registration checker compiler plugin" should {
    "detect correct registration for all kinds of classes" in {
      File.usingTemporaryDirectory() { directory =>
        val out = CodecRegistrationCheckerCompiler.compileCode(
          serializersCode(0) :: dataSourceCode,
          List(s"${directory.toJava.getAbsolutePath}", s"--source-code-directory=$sourceCodeDirectory"))
        out should be("")
      }
    }

    "detect serializer as object" in {
      File.usingTemporaryDirectory() { directory =>
        val out = CodecRegistrationCheckerCompiler.compileCode(
          serializersCode(5) :: dataSourceCode,
          List(s"${directory.toJava.getAbsolutePath}", s"--source-code-directory=$sourceCodeDirectory"))
        out should include("error")
      }
    }

    "raise an error" when {
      "types don't match filter regex" in {
        File.usingTemporaryDirectory() { directory =>
          val out = CodecRegistrationCheckerCompiler.compileCode(
            serializersCode(1) :: dataSourceCode,
            List(s"${directory.toJava.getAbsolutePath}", s"--source-code-directory=$sourceCodeDirectory"))
          out should include("error")
        }
      }

      "types are missing in serializer definition" in {
        File.usingTemporaryDirectory() { directory =>
          val out = CodecRegistrationCheckerCompiler.compileCode(
            serializersCode(2) :: dataSourceCode,
            List(s"${directory.toJava.getAbsolutePath}", s"--source-code-directory=$sourceCodeDirectory"))
          out should include("error")
          (out should not).include("literal")
        }
      }

      "annotation value is not a compile time literal" in {
        File.usingTemporaryDirectory() { directory =>
          val out = CodecRegistrationCheckerCompiler.compileCode(
            serializersCode(3) :: dataSourceCode,
            List(s"${directory.toJava.getAbsolutePath}", s"--source-code-directory=$sourceCodeDirectory"))
          out should include("error")
        }
      }

      "class in annotation is not annotated with @SerializabilityTrait" in {
        File.usingTemporaryDirectory() { directory =>
          val out = CodecRegistrationCheckerCompiler.compileCode(
            serializersCode(4) :: dataSourceCode,
            List(s"${directory.toJava.getAbsolutePath}", s"--source-code-directory=$sourceCodeDirectory"))
          out should include("error")
        }
      }
    }

    "work with no serializer" in {
      File.usingTemporaryDirectory() { directory =>
        val out =
          CodecRegistrationCheckerCompiler.compileCode(
            dataSourceCode,
            List(s"${directory.toJava.getAbsolutePath}", s"--source-code-directory=$sourceCodeDirectory"))
        out should be("")
      }
    }

    "create cache file when missing" in {
      File.usingTemporaryDirectory() { directory =>
        val out =
          CodecRegistrationCheckerCompiler.compileCode(
            dataSourceCode,
            List(s"${directory.toJava.getAbsolutePath}", s"--source-code-directory=$sourceCodeDirectory"))
        out should be("")
        val cacheFile =
          (directory / CodecRegistrationCheckerCompilerPlugin.directClassDescendantsCacheFileName).contentAsString
        cacheFile should be("""org.random.project.SerializableTrait,org.random.project.GenericData
                              |org.random.project.SerializableTrait,org.random.project.IndirectData
                              |org.random.project.SerializableTrait,org.random.project.StdData""".stripMargin)
      }
    }

    "use existing from cache file" in {
      File.usingTemporaryDirectory() { directory =>
        val cacheFile = directory / CodecRegistrationCheckerCompilerPlugin.directClassDescendantsCacheFileName
        cacheFile < "org.random.project.SerializableTrait,org.random.project.MissingData"
        val out =
          CodecRegistrationCheckerCompiler.compileCode(
            dataSourceCode,
            List(s"${directory.toJava.getAbsolutePath}", s"--source-code-directory=$sourceCodeDirectory"))
        out should be("")
        val cacheFileAfter = cacheFile.contentAsString
        cacheFileAfter should be("""org.random.project.SerializableTrait,org.random.project.GenericData
                                   |org.random.project.SerializableTrait,org.random.project.IndirectData
                                   |org.random.project.SerializableTrait,org.random.project.MissingData
                                   |org.random.project.SerializableTrait,org.random.project.StdData""".stripMargin)
      }
    }

    "fail to initialise" when {
      "integrity of cache file is compromised" in {
        assertThrows[RuntimeException] {
          File.usingTemporaryDirectory() { directory =>
            val cacheFile = directory / CodecRegistrationCheckerCompilerPlugin.directClassDescendantsCacheFileName
            cacheFile < "org.random.project.SerializableTrait"
            CodecRegistrationCheckerCompiler.compileCode(
              dataSourceCode,
              List(s"${directory.toJava.getAbsolutePath}", s"--source-code-directory=$sourceCodeDirectory"))
          }
        }
      }

      "path in argument contains in invalid" in {
        assertThrows[RuntimeException] {
          File.usingTemporaryDirectory() { directory =>
            CodecRegistrationCheckerCompiler.compileCode(
              dataSourceCode,
              List(s"${directory.toJava.getAbsolutePath}\u0000", s"--source-code-directory=$sourceCodeDirectory"))
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
          List(serializersCode(6), dataSourceCode.find(_.contains("@SerializabilityTrait")).get),
          List(s"${directory.toJava.getAbsolutePath}", s"--source-code-directory=$sourceCodeDirectory"))
        out should be("")
      }
    }
  }
}
