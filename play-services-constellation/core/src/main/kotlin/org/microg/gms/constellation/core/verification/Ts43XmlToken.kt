package org.microg.gms.constellation.core.verification

import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Read a token from a TS.43 WAP provisioning document.
 *
 * Android's DocumentBuilderFactory does not implement standard JAXP
 * FEATURE_SECURE_PROCESSING / disallow-doctype-decl configuration. Reject DTDs
 * before parsing, and also refuse all external entity resolution. Do not
 * silently fall back to an unprotected parser.
 */
internal object Ts43XmlToken {
    private const val MAX_DOCUMENT_CHARS = 1_048_576

    fun extract(xml: String): String? {
        if (xml.length > MAX_DOCUMENT_CHARS || xml.contains("<!DOCTYPE") || xml.contains("<!ENTITY")) {
            return null
        }

        return runCatching {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isValidating = false
            factory.isNamespaceAware = false
            factory.isExpandEntityReferences = false
            val builder = factory.newDocumentBuilder()
            builder.setEntityResolver { _, _ ->
                throw SAXException("External XML entities are not permitted in TS.43")
            }
            builder.setErrorHandler(object : DefaultHandler() {
                override fun error(e: SAXParseException) { throw e }
                override fun fatalError(e: SAXParseException) { throw e }
            })
            val doc = builder.parse(InputSource(StringReader(xml)))
            val parms = doc.getElementsByTagName("parm")
            for (i in 0 until parms.length) {
                val attrs = parms.item(i).attributes ?: continue
                if (attrs.getNamedItem("name")?.nodeValue == "token") {
                    return@runCatching attrs.getNamedItem("value")?.nodeValue?.takeIf { it.isNotEmpty() }
                }
            }
            null
        }.getOrNull()
    }
}
