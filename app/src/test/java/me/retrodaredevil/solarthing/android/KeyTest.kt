package me.retrodaredevil.solarthing.android

import me.retrodaredevil.solarthing.packets.security.crypto.KeyUtil
import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec

class KeyTest {
    @Test
    fun `test keys`(){
        val pair = KeyUtil.generateKeyPair()
        val spec = PKCS8EncodedKeySpec(pair.private.encoded)
        val private = KeyFactory.getInstance(KeyUtil.FACTORY_ALGORITHM).generatePrivate(spec)
        assertEquals(pair.private, private)
        assertEquals(spec.encoded.toList(), pair.private.encoded.toList())
    }
}