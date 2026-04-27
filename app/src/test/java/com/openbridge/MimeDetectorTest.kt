package com.openbridge

import org.junit.Assert.assertEquals
import org.junit.Test

class MimeDetectorTest {

    @Test
    fun testVideoFormats() {
        // MKV
        assertMime(byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte()), "video/x-matroska")
        // MP4 (ftyp)
        assertMime(createFtypHeader("mp42"), "video/mp4")
        // AVI (RIFF ... AVI )
        val avi = ByteArray(12)
        "RIFF".toByteArray().copyInto(avi)
        "AVI ".toByteArray().copyInto(avi, 8)
        assertMime(avi, "video/x-msvideo")
        // FLV
        assertMime("FLV".toByteArray(), "video/x-flv")
    }

    @Test
    fun testAudioFormats() {
        // MP3 (ID3)
        assertMime("ID3".toByteArray(), "audio/mpeg")
        // FLAC
        assertMime("fLaC".toByteArray(), "audio/flac")
        // OGG
        assertMime("OggS".toByteArray(), "audio/ogg")
        // WAV
        val wav = ByteArray(12)
        "RIFF".toByteArray().copyInto(wav)
        "WAVE".toByteArray().copyInto(wav, 8)
        assertMime(wav, "audio/wav")
    }

    @Test
    fun testImageFormats() {
        // JPEG
        assertMime(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()), "image/jpeg")
        // PNG
        assertMime(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47), "image/png")
        // GIF
        assertMime("GIF8".toByteArray(), "image/gif")
        // WEBP
        val webp = ByteArray(12)
        "RIFF".toByteArray().copyInto(webp)
        "WEBP".toByteArray().copyInto(webp, 8)
        assertMime(webp, "image/webp")
        // BMP
        assertMime("BM".toByteArray(), "image/bmp")
        // TIFF LE
        assertMime(byteArrayOf(0x49, 0x49, 0x2A, 0x00), "image/tiff")
        // TIFF BE
        assertMime(byteArrayOf(0x4D, 0x4D, 0x00, 0x2A), "image/tiff")
    }

    @Test
    fun testDocumentAndArchiveFormats() {
        // PDF
        assertMime("%PDF".toByteArray(), "application/pdf")
        // RAR
        assertMime("Rar!".toByteArray(), "application/x-rar-compressed")
        // 7Z
        assertMime(byteArrayOf(0x37, 0x7A, 0xBC.toByte(), 0xAF.toByte()), "application/x-7z-compressed")
        // GZIP
        assertMime(byteArrayOf(0x1F.toByte(), 0x8B.toByte()), "application/gzip")
        // XZ
        assertMime(byteArrayOf(0xFD.toByte(), 0x37, 0x7A, 0x58, 0x5A, 0x00), "application/x-xz")
    }

    @Test
    fun testZipBasedFormats() {
        val zipMagic = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
        
        // Generic ZIP
        assertMime(zipMagic, "application/zip")
        
        // APK (by extension)
        assertEquals("application/vnd.android.package-archive", 
            MimeDetector.detectFromHeader(zipMagic, "test.apk").mime)
            
        // EPUB (mimetype entry)
        val epub = ByteArray(40)
        zipMagic.copyInto(epub)
        "mimetype".toByteArray().copyInto(epub, 30)
        assertMime(epub, "application/epub+zip")
        
        // DOCX ([Content_Types].xml)
        val docx = ByteArray(50)
        zipMagic.copyInto(docx)
        "[Content_Types].xml".toByteArray().copyInto(docx, 30)
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            MimeDetector.detectFromHeader(docx, "doc.docx").mime)
    }

    private fun assertMime(header: ByteArray, expectedMime: String) {
        val result = MimeDetector.detectFromHeader(header)
        assertEquals(expectedMime, result.mime)
    }

    private fun createFtypHeader(brand: String): ByteArray {
        val header = ByteArray(12)
        "ftyp".toByteArray().copyInto(header, 4)
        brand.toByteArray().copyInto(header, 8)
        return header
    }
}
