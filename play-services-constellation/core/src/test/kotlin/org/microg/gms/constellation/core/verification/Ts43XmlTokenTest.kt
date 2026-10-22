package org.microg.gms.constellation.core.verification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Exercises local XML parsing only. No SIM, carrier connection or mock token backend. */
class Ts43XmlTokenTest {
    @Test
    fun readsTokenFromOrdinaryWapXml() {
        val xml = """<wap-provisioningdoc><characteristic type="TOKEN"><parm name="token" value="example-value"/></characteristic></wap-provisioningdoc>"""
        assertEquals("example-value", Ts43XmlToken.extract(xml))
    }

    @Test
    fun parsesGsmaTs43Version13WapProvisioningStructure() {
        // The token placeholder and document shape appear in GSMA TS.43 v13,
        // table 10 (the source contains no DOCTYPE declaration).
        val xml = """<?xml version="1.0"?>
<wap-provisioningdoc version="1.1">
  <characteristic type="VERS">
    <parm name="version" value="X"/>
    <parm name="validity" value="Y"/>
  </characteristic>
  <characteristic type="TOKEN">
    <parm name="token" value="U"/>
    <parm name="validity" value="V"/>
  </characteristic>
  <characteristic type="APPLICATION"><parm name="AppID" value="ap2004"/></characteristic>
</wap-provisioningdoc>"""
        assertEquals("U", Ts43XmlToken.extract(xml))
    }

    @Test
    fun decodesProperlyEscapedXmlAttributeCharacters() {
        val xml = """<root><parm value="alpha&amp;beta" name="token"/></root>"""
        assertEquals("alpha&beta", Ts43XmlToken.extract(xml))
    }

    @Test
    fun absentOrEmptyTokenIsNotAccepted() {
        assertNull(Ts43XmlToken.extract("""<root><parm name="other" value="x"/></root>"""))
        assertNull(Ts43XmlToken.extract("""<root><parm name="token" value=""/></root>"""))
        assertNull(Ts43XmlToken.extract("<root>"))
    }

    @Test
    fun externalEntityDocumentIsRejectedBeforeParserCanReadIt() {
        val xml = """<!DOCTYPE root [<!ENTITY e SYSTEM "file:///nonexistent/private">]><root><parm name="token" value="&e;"/></root>"""
        assertNull(Ts43XmlToken.extract(xml))
    }

    @Test
    fun internalEntityDocumentIsAlsoRejected() {
        val xml = """<!DOCTYPE root [<!ENTITY e "expanded">]><root><parm name="token" value="&e;"/></root>"""
        assertNull(Ts43XmlToken.extract(xml))
    }

    @Test
    fun excessivelyLargeXmlIsRejectedWithoutParsing() {
        assertNull(Ts43XmlToken.extract("<root>" + " ".repeat(1_048_576) + "</root>"))
    }
}
