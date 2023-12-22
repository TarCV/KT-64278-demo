import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import java.util.stream.Collectors
import kotlin.system.measureTimeMillis
import kotlin.test.DefaultAsserter.assertEquals
import kotlin.test.assertTrue


class SeleniumDemoTests {
    private val scriptToPinMap = mutableMapOf<String, ScriptKey>()

    private fun <R> WebDriver.executeKtJs(resourcePath: String): R {
        val pin = getPinForBlock(resourcePath)
        val seleniumResult = (this as JavascriptExecutor).executeScript(pin)
        @Suppress("UNCHECKED_CAST")
        return seleniumResult as R
    }

    private fun <T, R> WebDriver.executeKtJs(arg1: T, resourcePath: String): R {
        val pin = getPinForBlock(resourcePath)
        val seleniumResult = (this as JavascriptExecutor).executeScript(pin, arg1)
        @Suppress("UNCHECKED_CAST")
        return seleniumResult as R
    }

    @Test
    fun readColumnTexts(): Unit = ChromeDriver(ChromeOptions().addArguments("--headless=new")).runAndQuit {
        (1..2).map {
            listOf(
                assertAndMeasure("KtJs comparable") { cells ->
                    executeKtJs(cells, "comparable.js")
                },

                assertAndMeasure("KtJs optimal") { _ ->
                    executeKtJs("optimal.js")
                },

                assertAndMeasure("Selenium comparable") { cells ->
                    cells.map { it.getDomProperty("textContent") }
                },

                assertAndMeasure("Selenium getText") { cells ->
                    cells.map { it.text }
                }
            )
        }
            .flatten()
            .groupBy { it.first }
            .mapValues {
                it.value.stream()
                    .collect(Collectors.summarizingLong { it.second })
            }
            .forEach {
                println("=== ${it.key} ===")
                println(it.value)
            }
    }

    private inline fun ChromeDriver.assertAndMeasure(name: String, textGetter: SeleniumDemoTests.(List<WebElement>) -> List<String>): Pair<String, Long> {
        val result: List<String>

        get("https://the-internet.herokuapp.com/large")
        val cells = findElements(By.className("column-15"))

        val time = measureTimeMillis {
            result = textGetter(cells)
        }
        assertEquals(
            "($name) Should get correct element texts",
            listOf("15") + (1..50).map { "$it.15" },
            result
        )
        assertTrue(result.all { it.endsWith("15") }, "($name) Unexpected values: $result")
        return name to time
    }

    private inline fun <T : RemoteWebDriver, R> T.runAndQuit(block: T.() -> R): R = try {
        block()
    } finally {
        quit()
    }

    private fun WebDriver.getPinForBlock(resourcePath: String): ScriptKey {
        this as JavascriptExecutor
        var pin = scriptToPinMap[resourcePath]
        pin = if (pin != null && pin in pinnedScripts) {
            pin
        } else {
            println("NOTE: Script $resourcePath is not pinned yet")
            val moduleJs = this@SeleniumDemoTests::class.java.getResourceAsStream(resourcePath)
                .run {
                    requireNotNull(this) { "Failed to read the compiled fragment $resourcePath" }
                }
                .bufferedReader()
                .use {
                    it.readText()
                }
            val fragmentName = "KT-64278-demo"
            @Language("JavaScript") val runnableJs = """
                            const e = {};
                            (function () { $moduleJs }).call(e);
                            const entryModule = e["$fragmentName"];
                            return entryModule.entrypoint.apply(entryModule, arguments)
                        """.trimIndent()

            val newPin: ScriptKey = (this as JavascriptExecutor).pin(runnableJs)
            scriptToPinMap[resourcePath] = newPin
            newPin
        }
        return pin
    }
}