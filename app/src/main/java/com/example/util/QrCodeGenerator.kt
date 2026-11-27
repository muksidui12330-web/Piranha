package com.example.util

import kotlin.math.abs

/**
 * Self-contained ISO-18004 Byte-mode QR Code matrix generator (Versions 1..6, ECC Level M).
 * Allows rendering crisp offline UPI payment QR codes (`upi://pay?pa=...`) with zero extra libraries.
 */
object QrCodeGenerator {

    private data class VersionSpec(
        val version: Int,
        val size: Int,
        val dataCodewords: Int,
        val ecCodewordsPerBlock: Int,
        val numBlocks: Int,
        val alignCenter: Int?
    )

    // Exact ECC Level M capacities from ISO/IEC 18004 Table 7 & 9 (single block or 2 blocks)
    private val VERSION_SPECS = listOf(
        VersionSpec(1, 21, 16, 10, 1, null),
        VersionSpec(2, 25, 28, 16, 1, 18),
        VersionSpec(3, 29, 44, 26, 1, 22),
        VersionSpec(4, 33, 64, 18, 2, 26),
        VersionSpec(5, 37, 86, 24, 2, 30),
        VersionSpec(6, 41, 108, 16, 4, 34)
    )

    private val GF_EXP = IntArray(512)
    private val GF_LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            GF_EXP[i] = x
            GF_LOG[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) x = x xor 0x11D
        }
        for (i in 255 until 512) {
            GF_EXP[i] = GF_EXP[i - 255]
        }
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return GF_EXP[GF_LOG[a] + GF_LOG[b]]
    }

    private fun rsGeneratorPoly(ecLen: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until ecLen) {
            val next = IntArray(poly.size + 1)
            val factor = GF_EXP[i]
            for (j in poly.indices) {
                next[j] = next[j] xor poly[j]
                next[j + 1] = next[j + 1] xor gfMul(poly[j], factor)
            }
            poly = next
        }
        return poly
    }

    private fun rsComputeRemainder(data: IntArray, ecLen: Int): IntArray {
        val gen = rsGeneratorPoly(ecLen)
        val rem = IntArray(ecLen)
        for (b in data) {
            val factor = b xor rem[0]
            System.arraycopy(rem, 1, rem, 0, ecLen - 1)
            rem[ecLen - 1] = 0
            for (j in 0 until ecLen) {
                rem[j] = rem[j] xor gfMul(gen[j + 1], factor)
            }
        }
        return rem
    }

    fun encode(text: String): Array<BooleanArray>? {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val spec = VERSION_SPECS.firstOrNull { (bytes.size + 2) <= it.dataCodewords } ?: return null

        // Build bit stream: Mode 0100 (4 bits) + 8-bit char count + data bytes + terminator + pad
        val bits = ArrayList<Int>(spec.dataCodewords * 8)
        fun appendBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) bits.add((value ushr i) and 1)
        }
        appendBits(0b0100, 4)
        appendBits(bytes.size, 8)
        for (b in bytes) appendBits(b.toInt() and 0xFF, 8)

        val totalDataBits = spec.dataCodewords * 8
        val terminator = minOf(4, totalDataBits - bits.size)
        repeat(terminator) { bits.add(0) }
        while (bits.size % 8 != 0) bits.add(0)

        var padToggle = true
        while (bits.size < totalDataBits) {
            appendBits(if (padToggle) 0xEC else 0x11, 8)
            padToggle = !padToggle
        }

        val dataCodewords = IntArray(spec.dataCodewords) { i ->
            var v = 0
            for (b in 0 until 8) v = (v shl 1) or bits[i * 8 + b]
            v
        }

        // Split into blocks & compute Reed-Solomon EC codewords
        val blockSize = spec.dataCodewords / spec.numBlocks
        val blocks = Array(spec.numBlocks) { b ->
            dataCodewords.copyOfRange(b * blockSize, (b + 1) * blockSize)
        }
        val ecBlocks = Array(spec.numBlocks) { b ->
            rsComputeRemainder(blocks[b], spec.ecCodewordsPerBlock)
        }

        val finalBits = ArrayList<Int>((spec.dataCodewords + spec.numBlocks * spec.ecCodewordsPerBlock) * 8 + 7)
        for (i in 0 until blockSize) {
            for (b in 0 until spec.numBlocks) {
                val v = blocks[b][i]
                for (bit in 7 downTo 0) finalBits.add((v ushr bit) and 1)
            }
        }
        for (i in 0 until spec.ecCodewordsPerBlock) {
            for (b in 0 until spec.numBlocks) {
                val v = ecBlocks[b][i]
                for (bit in 7 downTo 0) finalBits.add((v ushr bit) and 1)
            }
        }

        val size = spec.size
        val modules = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        fun setFunction(r: Int, c: Int, dark: Boolean) {
            modules[r][c] = dark
            isFunction[r][c] = true
        }

        fun drawFinder(top: Int, left: Int) {
            for (dr in -1..7) {
                for (dc in -1..7) {
                    val r = top + dr
                    val c = left + dc
                    if (r !in 0 until size || c !in 0 until size) continue
                    val dist = maxOf(abs(dr - 3), abs(dc - 3))
                    setFunction(r, c, dist != 2 && dist != 4)
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        // Timing patterns
        for (i in 8 until size - 8) {
            setFunction(6, i, i % 2 == 0)
            setFunction(i, 6, i % 2 == 0)
        }

        // Alignment pattern (versions 2..6 have single center coordinate)
        spec.alignCenter?.let { center ->
            for (dr in -2..2) {
                for (dc in -2..2) {
                    val dist = maxOf(abs(dr), abs(dc))
                    setFunction(center + dr, center + dc, dist != 1)
                }
            }
        }

        // Reserve format info cells
        for (i in 0..8) {
            if (i != 6) {
                setFunction(8, i, false)
                setFunction(i, 8, false)
            }
        }
        for (i in size - 8 until size) {
            setFunction(8, i, false)
            setFunction(i, 8, false)
        }
        setFunction(size - 8, 8, true) // Dark module

        // Place data bits zig-zag
        var bitIdx = 0
        var upward = true
        var col = size - 1
        while (col >= 1) {
            if (col == 6) col--
            for (step in 0 until size) {
                val r = if (upward) size - 1 - step else step
                for (dc in 0..1) {
                    val c = col - dc
                    if (!isFunction[r][c]) {
                        modules[r][c] = if (bitIdx < finalBits.size) finalBits[bitIdx++] == 1 else false
                    }
                }
            }
            upward = !upward
            col -= 2
        }

        // Evaluate masks 0..7, pick lowest penalty
        var bestMask = 0
        var bestPenalty = Int.MAX_VALUE
        for (mask in 0..7) {
            val candidate = Array(size) { r ->
                BooleanArray(size) { c ->
                    val invert = !isFunction[r][c] && maskBit(mask, r, c)
                    modules[r][c] xor invert
                }
            }
            applyFormatBits(candidate, mask)
            val p = penalty(candidate)
            if (p < bestPenalty) {
                bestPenalty = p
                bestMask = mask
            }
        }

        for (r in 0 until size) {
            for (c in 0 until size) {
                if (!isFunction[r][c] && maskBit(bestMask, r, c)) {
                    modules[r][c] = !modules[r][c]
                }
            }
        }
        applyFormatBits(modules, bestMask)
        return modules
    }

    private fun maskBit(mask: Int, r: Int, c: Int): Boolean = when (mask) {
        0 -> (r + c) % 2 == 0
        1 -> r % 2 == 0
        2 -> c % 3 == 0
        3 -> (r + c) % 3 == 0
        4 -> (r / 2 + c / 3) % 2 == 0
        5 -> (r * c) % 2 + (r * c) % 3 == 0
        6 -> ((r * c) % 2 + (r * c) % 3) % 2 == 0
        7 -> ((r + c) % 2 + (r * c) % 3) % 2 == 0
        else -> false
    }

    private fun applyFormatBits(modules: Array<BooleanArray>, mask: Int) {
        val size = modules.size
        // Format level M = 00
        val data = (0b00 shl 3) or mask
        var rem = data shl 10
        for (i in 14 downTo 10) {
            if ((rem and (1 shl i)) != 0) rem = rem xor (0x537 shl (i - 10))
        }
        val bits = ((data shl 10) or rem) xor 0x5412

        for (i in 0..5) modules[8][i] = ((bits ushr i) and 1) == 1
        modules[8][7] = ((bits ushr 6) and 1) == 1
        modules[8][8] = ((bits ushr 7) and 1) == 1
        modules[7][8] = ((bits ushr 8) and 1) == 1
        for (i in 9..14) modules[14 - i][8] = ((bits ushr i) and 1) == 1

        for (i in 0..7) modules[size - 1 - i][8] = ((bits ushr i) and 1) == 1
        for (i in 8..14) modules[8][size - 15 + i] = ((bits ushr i) and 1) == 1
        modules[size - 8][8] = true
    }

    private fun penalty(m: Array<BooleanArray>): Int {
        val n = m.size
        var score = 0
        for (r in 0 until n) {
            var run = 1
            for (c in 1 until n) {
                if (m[r][c] == m[r][c - 1]) run++ else {
                    if (run >= 5) score += 3 + (run - 5)
                    run = 1
                }
            }
            if (run >= 5) score += 3 + (run - 5)
        }
        for (c in 0 until n) {
            var run = 1
            for (r in 1 until n) {
                if (m[r][c] == m[r - 1][c]) run++ else {
                    if (run >= 5) score += 3 + (run - 5)
                    run = 1
                }
            }
            if (run >= 5) score += 3 + (run - 5)
        }
        return score
    }
}
