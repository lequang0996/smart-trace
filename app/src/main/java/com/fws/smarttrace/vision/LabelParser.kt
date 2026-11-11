package com.fws.smarttrace.vision

import com.fws.smarttrace.core.TextUtil
import com.fws.smarttrace.model.ExtractedLabel

/** Turns raw OCR text into structured fields using rules (no network, no AI). */
object LabelParser {
    private const val DATE =
        """\d{1,2}\s?[./\-]\s?\d{1,2}\s?[./\-]\s?\d{2,4}|\d{4}\s?[./\-]\s?\d{1,2}\s?[./\-]\s?\d{1,2}|\d{1,2}\s?[./\-]\s?\d{4}|\d{4}年\d{1,2}月(?:\d{1,2}日)?|\d{1,2}\s?[A-Z]{3}[A-Z]*\.?\s?\d{2,4}"""

    private val SIZE = Regex("""(\d{2,4})\s*[xX×*]\s*(\d{2,4})\s*(MM|CM)?""", RegexOption.IGNORE_CASE)
    private val QTY_AREA = Regex("""(\d{1,4})\s*(?:PCS|PC|PIECES|VIÊN|片|块)\s*[/\\]\s*(\d+[.,]?\d*)\s*(?:SQM|SQ\.?\s?M|M2|M²|㎡)""", RegexOption.IGNORE_CASE)
    private val QTY = Regex("""(\d{1,4})\s*(?:PCS|PC|PIECES|VIÊN|片|块)""", RegexOption.IGNORE_CASE)
    private val AREA = Regex("""(\d+[.,]?\d*)\s*(?:SQM|SQ\.?\s?M|M2|M²|㎡)""", RegexOption.IGNORE_CASE)
    private val GROSS = Regex("""(?:G\.?\s?W\.?|GROSS(?:\s*WEIGHT)?|毛重)\W{0,6}(\d+[.,]?\d*)\s*KGS?""", RegexOption.IGNORE_CASE)
    private val NET = Regex("""(?:N\.?\s?W\.?|NET(?:\s*(?:WEIGHT|WT))?|净重|KHỐI LƯỢNG TỊNH|TRỌNG LƯỢNG)\W{0,6}(\d+[.,]?\d*)\s*KGS?""", RegexOption.IGNORE_CASE)
    private val ANY_KG = Regex("""(\d+[.,]?\d*)\s*KGS?\b""", RegexOption.IGNORE_CASE)
    private val GRADE = Regex("""GRADE\W{0,4}(A{1,4}\+?|[A-D][1-3]?|[1-4])\b""", RegexOption.IGNORE_CASE)
    private val ITEM = Regex("""(?:ITEM|MODEL|CODE|品号|货号)\W{0,4}(?=[A-Z0-9\-]*\d)([A-Z0-9\-]{3,14})""", RegexOption.IGNORE_CASE)
    private val SHADE = Regex("""(?:COLOU?R\s*SHADE|SHADE|CALIBER|CALIBRE|TONE|色号)\W{0,4}(?=[A-Z0-9\-/ ]*\d)([A-Z0-9\-/ ]{2,14})""", RegexOption.IGNORE_CASE)
    private val BATCH = Regex("""(?:BATCH|LOT)(?:\s*(?:NO|NUMBER)\.?)?\W{0,4}(?=[A-Z0-9\-/]*\d)([A-Z0-9\-/]{3,16})|(?:批号|SỐ LÔ)\W{0,4}([A-Z0-9\-/]{3,16})""", RegexOption.IGNORE_CASE)
    private val MFG = Regex("""(?:MFG|MFD|MANUFACTURED?|PRODUCTION\s*DATE|PROD\.?\s*DATE|NSX|NGÀY SẢN XUẤT|生产日期)\W{0,10}($DATE)""", RegexOption.IGNORE_CASE)
    private val EXP = Regex("""(?:EXP|EXPIRY(?:\s*DATE)?|EXPIRES|BEST BEFORE|HSD|HẠN SỬ DỤNG|有效期至?)\W{0,10}($DATE)""", RegexOption.IGNORE_CASE)
    private val SHELF = Regex("""(?:EXP\.?|SHELF\s*LIFE|HSD|HẠN)\W{0,6}(\d{1,2})\s*(?:MONTHS?|THÁNG|个月)""", RegexOption.IGNORE_CASE)
    private val STANDARD = Regex(
        """GB/T\s?\d{3,5}(?:[.\-]\d{1,4})?|\bGB\s?\d{3,5}(?:[.\-]\d{1,4})?|TCVN\s?\d{3,5}(?:[:\-]\d{4})?|\bEN\s?\d{3,5}(?:[\-:]\d+)?|\bISO\s?\d{3,5}(?:[\-:]\d+)?|ASTM\s?[A-Z]\d{2,4}|JIS\s?[A-Z]\s?\d{3,5}|\bBS\s?\d{3,5}""",
        RegexOption.IGNORE_CASE
    )
    private val PHONE_LABELED = Regex("""(?:TEL|PHONE|ĐT|\bDT\b|ĐIỆN THOẠI|HOTLINE|LIÊN HỆ|电话)\W{0,4}(\d[\d\s\-+().]{6,18}\d)""", RegexOption.IGNORE_CASE)
    private val PHONE_CN = Regex("""\b0\d{2,3}[\-\s]\d{7,8}\b""")
    private val ORIGIN = Regex("""(?:MADE IN|PRODUCT OF|ORIGIN|XUẤT XỨ|SẢN XUẤT TẠI|产地)\W{0,3}([A-ZÀ-Ỵ]{3,}(?: [A-ZÀ-Ỵ]{3,})?)""", RegexOption.IGNORE_CASE)
    private val ADDRESS = Regex("""(?:ADDRESS|ADD\.|ĐỊA CHỈ|地址|地\s址)\W{0,3}(.{8,80})""", RegexOption.IGNORE_CASE)
    private val COMPANY = Regex("""CO\.?,?\s?LTD|COMPANY|LIMITED|\bCORP|\bJSC\b|CÔNG TY|\bCTY\b|NHÀ MÁY|NHA MAY|XÍ NGHIỆP|CƠ SỞ SẢN XUẤT|有限公司|股份有限公司|制造商|生产商""", RegexOption.IGNORE_CASE)
    private val GENERIC = Regex(
        """GRADE|SIZE|QTY|QUANTITY|ITEM|G\.?W|N\.?W|WEIGHT|COLO|SHADE|PCS|SQM|\bKG|\bMM\b|MADE|EXP|MFG|BATCH|LOT|DATE|TEL|PHONE|ADDRESS|WWW|HTTP""",
        RegexOption.IGNORE_CASE
    )
    private val PRODUCT_WORDS = Regex("""TILE|ADHESIVE|CEMENT|MORTAR|GROUT|PAINT|GLAZED|CERAMIC|PORCELAIN|WALL|FLOOR|瓷砖|胶|GẠCH|KEO|SƠN|XI MĂNG|GIẤY|KHĂN|WIPES?|TISSUE|SOAP|XÀ PHÒNG|SHAMPOO|DẦU GỘI|DETERGENT|NƯỚC (?:RỬA|GIẶT|LAU)|BATTERY|CHARGER|CABLE|MOBILE PHONE""", RegexOption.IGNORE_CASE)
    private val VN_CHARS = Regex("[ăâđêôơưạảấầẩẫậắằẳẵặẹẻẽếềểễệỉịọỏốồổỗộớờởỡợụủứừửữựỳỵỷỹ]")
    private val VN_WORDS = Regex("nhà sản xuất|hạn sử dụng|xuất xứ|nhập khẩu|định lượng|sản xuất tại|khối lượng|hướng dẫn|bảo quản")

    private val SHELF_YEARS = Regex("""(?:HẠN SỬ DỤNG|HSD|SHELF\s*LIFE|EXP\.?)\W{0,12}(\d{1,2})\s*(?:NĂM|YEARS?)""", RegexOption.IGNORE_CASE)

    // "Key: value" lines worth showing on the Specs tab. Fields that already have their own row are not repeated.
    private val SPEC_LINE = Regex(
        """^(THÀNH PHẦN CHÍNH|THÀNH PHẦN|ĐỊNH LƯỢNG|QUY CÁCH ĐÓNG GÓI|QUY CÁCH|KHỐI LƯỢNG|HƯỚNG DẪN SỬ DỤNG|HƯỚNG DẪN BẢO QUẢN|BẢO QUẢN|CÔNG DỤNG|CẢNH BÁO|LƯU Ý|INGREDIENTS?|DIRECTIONS?|STORAGE|WARNING|COMPOSITION|MATERIAL|USAGE|CAUTION|SPECIFICATIONS?|PACKAGE CONTENTS|成分|主要成分|规格|用途|使用方法|储存条件|注意事项)\s*[:：]\s*(.{2,140})$""",
        RegexOption.IGNORE_CASE
    )
    private val SPEC_HEADING = Regex(
        """^(THÀNH PHẦN|HƯỚNG DẪN SỬ DỤNG|HƯỚNG DẪN BẢO QUẢN|BẢO QUẢN|CÔNG DỤNG|CẢNH BÁO|LƯU Ý|INGREDIENTS?|DIRECTIONS?|STORAGE|WARNING|CAUTION)\s*[:：]?$""",
        RegexOption.IGNORE_CASE
    )

    private fun specs(lines: List<String>): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        var i = 0
        while (i < lines.size && out.size < 12) {
            val l = lines[i].trim()
            val m = SPEC_LINE.find(l)
            if (m != null) {
                out += m.groupValues[1].trim().lowercase().replaceFirstChar { it.uppercase() } to m.groupValues[2].trim()
            } else if (SPEC_HEADING.matches(l) && i + 1 < lines.size && !SPEC_HEADING.matches(lines[i + 1].trim())) {
                out += l.trimEnd(':', '：').lowercase().replaceFirstChar { it.uppercase() } to lines[i + 1].trim()
                i++
            }
            i++
        }
        return out.distinct()
    }

    private fun num(s: String?): Double? = s?.let { TextUtil.parseNum(it) }

    fun parse(ocr: OcrResult, hints: List<String>): ExtractedLabel {
        val raw = ocr.text.trim()
        val flat = raw.replace('\n', ' ').replace(Regex("\\s+"), " ")
        val up = flat.uppercase()

        val sizeM = SIZE.findAll(up).firstOrNull { it.groupValues[1].toInt() >= 10 && it.groupValues[2].toInt() >= 10 }
        val size = sizeM?.let {
            val unit = it.groupValues[3].uppercase().ifEmpty { if (maxOf(it.groupValues[1].toInt(), it.groupValues[2].toInt()) <= 120) "CM" else "MM" }
            "${it.groupValues[1]} x ${it.groupValues[2]} ${unit.lowercase()}"
        } ?: ""

        var qty: Int? = null
        var area: Double? = null
        QTY_AREA.find(up)?.let { qty = it.groupValues[1].toIntOrNull(); area = num(it.groupValues[2]) }
        if (qty == null) qty = QTY.find(up)?.groupValues?.get(1)?.toIntOrNull()
        if (area == null) area = AREA.find(up)?.let { num(it.groupValues[1]) }

        val gross = GROSS.find(up)?.let { num(it.groupValues[1]) }
        var net = NET.find(up)?.let { num(it.groupValues[1]) }
        if (net == null && gross == null) {
            val all = ANY_KG.findAll(up).mapNotNull { num(it.groupValues[1]) }.toList()
            if (all.size == 1) net = all[0]
        }

        val grade = GRADE.find(up)?.groupValues?.get(1)?.uppercase() ?: ""
        val item = ITEM.find(up)?.groupValues?.get(1) ?: ""
        val shade = SHADE.find(up)?.groupValues?.get(1)?.trim() ?: ""
        val batchM = BATCH.find(up)
        val batch = batchM?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } } ?: ""
        val mfg = MFG.find(up)?.groupValues?.get(1) ?: ""
        val exp = EXP.find(up)?.groupValues?.get(1) ?: ""
        val shelf = SHELF.find(up)?.groupValues?.get(1)?.toIntOrNull()
            ?: SHELF_YEARS.find(up)?.groupValues?.get(1)?.toIntOrNull()?.times(12)
        val standards = STANDARD.findAll(up).map { it.value.trim() }.distinct().take(5).toList()

        val phone = PHONE_LABELED.find(up)?.groupValues?.get(1)?.trim()
            ?: PHONE_CN.find(up)?.value ?: ""

        var origin = ORIGIN.find(up)?.groupValues?.get(1)?.trim()?.lowercase()?.replaceFirstChar { it.uppercase() } ?: ""
        if (origin.isEmpty() && flat.contains("中国制造")) origin = "China"

        val address = ADDRESS.find(flat)?.groupValues?.get(1)?.trim()?.take(80) ?: ""
        val manufacturer = raw.lines().map { it.trim() }.firstOrNull { it.length in 6..90 && COMPANY.containsMatchIn(it) } ?: ""

        val brand = ocr.lines
            .filter { l ->
                val t = l.text.trim()
                t.length in 2..24 && t.any { it.isLetter() } &&
                    t.count { it.isDigit() } <= t.length / 2 &&
                    !GENERIC.containsMatchIn(t) && !COMPANY.containsMatchIn(t)
            }
            .maxByOrNull { it.height }?.text?.trim() ?: ""

        val productName = ocr.lines.map { it.text.trim() }
            .filter { it.length in 4..60 && PRODUCT_WORDS.containsMatchIn(it) }
            .distinct().take(2).joinToString(" ")

        val hasVn = VN_CHARS.containsMatchIn(raw.lowercase()) || VN_WORDS.containsMatchIn(raw.lowercase())

        return ExtractedLabel(
            productName = productName,
            brand = brand,
            category = category(flat, hints),
            manufacturer = manufacturer,
            origin = origin,
            address = address,
            phone = phone,
            size = size,
            quantityPcs = qty,
            areaSqm = area,
            netWeightKg = net,
            grossWeightKg = gross,
            grade = grade,
            itemCode = item,
            batch = batch,
            shade = shade,
            mfgDate = mfg,
            expDate = exp,
            shelfLifeMonths = shelf,
            standards = standards,
            extraSpecs = specs(raw.lines()),
            confidence = ocr.quality,
            hasVietnameseLabel = if (raw.length >= 15) hasVn else null,
            rawText = raw.take(4000),
            visualHints = hints,
        )
    }

    fun category(text: String, hints: List<String>): String {
        val t = text.lowercase()
        return when {
            Regex("adhesive|mortar|grout|\\bglue\\b|keo dán|keo dan|胶|粘结").containsMatchIn(t) -> "adhesive"
            Regex("tile|ceramic|porcelain|gạch|gach|瓷砖|陶|砖").containsMatchIn(t) -> "tile"
            Regex("paint|sơn|涂料|漆").containsMatchIn(t) -> "paint"
            Regex("cement|xi măng|水泥").containsMatchIn(t) -> "cement"
            Regex("ingredients|nutrition|thành phần|配料|食品").containsMatchIn(t) -> "food"
            Regex("cream|lotion|serum|cosmetic|mỹ phẩm|化妆").containsMatchIn(t) -> "cosmetic"
            else -> ImageLabelEngine.categoryFromHints(hints) ?: "other"
        }
    }
}
