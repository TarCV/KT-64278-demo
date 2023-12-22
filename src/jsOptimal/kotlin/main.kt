import org.w3c.dom.asList

@JsExport fun entrypoint() = run {
    kotlinx.browser.document.querySelectorAll(".column-15")
        .asList()
        .map { it.textContent ?: "" }
        .toTypedArray()
}