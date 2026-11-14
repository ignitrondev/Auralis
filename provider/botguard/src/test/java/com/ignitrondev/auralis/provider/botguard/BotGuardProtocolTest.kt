/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.botguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Base64

class BotGuardProtocolTest {
    @Test
    fun parsesPlainChallenge() {
        val raw = """[["msg",["interp-js"],["https://www.google.com/js/th/abcdefghijklmnopqrstuvwxyz.js"],"hash","program-bytes","globalVm",null,"blob"]]"""
        val challenge = BotGuardProtocol.parseChallenge(raw)
        assertEquals("interp-js", challenge.interpreterJs)
        assertEquals("https://www.google.com/js/th/abcdefghijklmnopqrstuvwxyz.js", challenge.interpreterUrl)
        assertEquals("program-bytes", challenge.program)
        assertEquals("globalVm", challenge.globalName)
    }

    @Test
    fun parsesScrambledChallenge() {
        val inner = """["msg",null,["//www.gstatic.com/x/botguard.js"],"hash","prog","vm",null,"blob"]"""
        val scrambled = Base64.getEncoder().encodeToString(inner.toByteArray().map { (it - 97).toByte() }.toByteArray())
        val challenge = BotGuardProtocol.parseChallenge("""[null,"$scrambled"]""")
        assertEquals(null, challenge.interpreterJs)
        assertEquals("https://www.gstatic.com/x/botguard.js", challenge.interpreterUrl)
        assertEquals("prog", challenge.program)
        assertEquals("vm", challenge.globalName)
    }

    @Test
    fun rejectsUntrustedInterpreterHosts() {
        assertThrows(BotGuardException::class.java) { BotGuardProtocol.trustedInterpreterUrl("https://evil.example/botguard.js") }
        assertThrows(BotGuardException::class.java) { BotGuardProtocol.trustedInterpreterUrl("http://www.google.com/js/th/x.js") }
        assertThrows(BotGuardException::class.java) { BotGuardProtocol.trustedInterpreterUrl("https://user@www.google.com/js/th/x.js") }
    }

    @Test
    fun integrityTokenAndTokenBytesRoundTrip() {
        val integrity = BotGuardProtocol.parseIntegrityToken("""["AQID-_8",43200,"x"]""")
        assertEquals("new Uint8Array([1,2,3,251,255])", integrity.jsBytes)
        assertEquals(43200L, integrity.lifetimeSeconds)
        assertEquals("AQID-_8=", BotGuardProtocol.tokenFromBytes("1,2,3,251,255"))
        assertThrows(IllegalArgumentException::class.java) { BotGuardProtocol.tokenFromBytes("1,256") }
    }

    @Test
    fun requestBodiesAreJsonEscaped() {
        assertEquals("""["O43z0dpjhgX20SCx4KAo"]""", BotGuardProtocol.createBody())
        assertEquals("""["O43z0dpjhgX20SCx4KAo","a\"b"]""", BotGuardProtocol.generateBody("a\"b"))
    }
}
