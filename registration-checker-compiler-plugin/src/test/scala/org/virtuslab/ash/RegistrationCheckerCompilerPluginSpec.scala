package org.virtuslab.ash

import better.files.Dsl.SymbolicOperations
import better.files.File
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

class RegistrationCheckerCompilerPluginSpec extends AnyWordSpecLike with should.Matchers {
  private def getSerializerAsString(name: String) =
    (File(getClass.getClassLoader.getResource("serializers")) / (name + ".scala")).contentAsString

  private val dataSourceCode =
    (for (f <- File(getClass.getClassLoader.getResource("data")).children) yield f.contentAsString).toList

  private val serializersCode = Array(
    "CorrectSerializer",
    "EmptySerializer",
    "IncompleteSerializer",
    "InvalidAnnotationSerializer",
    "InvalidClassSerializer").map(getSerializerAsString)

  "Registration checker compiler plugin" should {
    "detect correct registration for all kinds of classes" in {
      File.usingTemporaryDirectory() { directory =>
        val out = RegistrationCheckerCompiler.compileCode(
          serializersCode(0) :: dataSourceCode,
          List(s"${directory.toJava.getAbsolutePath}"))
        out should be("")
      }
    }

    "raise an error" when {
      "types are missing in serializer definition" in {
        File.usingTemporaryDirectory() { directory =>
          val outEmpty = RegistrationCheckerCompiler.compileCode(
            serializersCode(1) :: dataSourceCode,
            List(s"${directory.toJava.getAbsolutePath}"))
          outEmpty should include("error")

          val outIncomplete = RegistrationCheckerCompiler.compileCode(
            serializersCode(2) :: dataSourceCode,
            List(s"${directory.toJava.getAbsolutePath}"))
          outIncomplete should include("error")
        }
      }

      "annotation value is not a compile time literal" in {
        File.usingTemporaryDirectory() { directory =>
          val out = RegistrationCheckerCompiler.compileCode(
            serializersCode(3) :: dataSourceCode,
            List(s"${directory.toJava.getAbsolutePath}"))
          out should include("error")
        }
      }

      "class in annotation is not annotated with @SerializabilityTrait" in {
        File.usingTemporaryDirectory() { directory =>
          val out = RegistrationCheckerCompiler.compileCode(
            serializersCode(4) :: dataSourceCode,
            List(s"${directory.toJava.getAbsolutePath}"))
          out should include("error")
        }
      }
    }

    "work with no serializer" in {
      File.usingTemporaryDirectory() { directory =>
        val out = RegistrationCheckerCompiler.compileCode(dataSourceCode, List(s"${directory.toJava.getAbsolutePath}"))
        out should be("")
      }
    }

    "create cache file when missing" in {
      File.usingTemporaryDirectory() { directory =>
        val out = RegistrationCheckerCompiler.compileCode(dataSourceCode, List(s"${directory.toJava.getAbsolutePath}"))
        out should be("")
        val cacheFile = (directory / RegistrationCheckerCompilerPlugin.cacheFileName).contentAsString
        cacheFile should be("""org.random.project.SerializableTrait,org.random.project.GenericData
                              |org.random.project.SerializableTrait,org.random.project.IndirectData
                              |org.random.project.SerializableTrait,org.random.project.StdData""".stripMargin)
      }
    }

    "use existing from cache file" in {
      File.usingTemporaryDirectory() { directory =>
        val cacheFile = directory / RegistrationCheckerCompilerPlugin.cacheFileName
        cacheFile < "org.random.project.SerializableTrait,org.random.project.MissingData"
        val out = RegistrationCheckerCompiler.compileCode(dataSourceCode, List(s"${directory.toJava.getAbsolutePath}"))
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
            val cacheFile = directory / RegistrationCheckerCompilerPlugin.cacheFileName
            cacheFile < "org.random.project.SerializableTrait"
            RegistrationCheckerCompiler.compileCode(dataSourceCode, List(s"${directory.toJava.getAbsolutePath}"))
          }
        }
      }

      "path in argument contains in invalid" in {
        assertThrows[RuntimeException] {
          File.usingTemporaryDirectory() { directory =>
            RegistrationCheckerCompiler.compileCode(dataSourceCode, List(s"${directory.toJava.getAbsolutePath}\u0000"))
          }
        }
      }

      "no path is specified" in {
        assertThrows[RuntimeException] {
          RegistrationCheckerCompiler.compileCode(dataSourceCode, Nil)
        }
      }
    }
  }

}
