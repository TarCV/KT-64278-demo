import org.w3c.dom.Node

@JsExport fun entrypoint(e: Array<Node>) = run {
    e.map { it.textContent ?: "" }
        .toTypedArray()
}