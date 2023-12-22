import org.w3c.dom.Node

@JsExport fun entrypoint(e: Array<Node>) {
    e.map { it.textContent ?: "" }
}