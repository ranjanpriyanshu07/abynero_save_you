package com.phoneguard.domain.url

/**
 * A bank, payment app or government service that scammers often copy.
 *
 * @property displayName shown to the user, e.g. "SBI".
 * @property substringKeywords distinctive words; any domain containing them is suspicious.
 * @property tokenKeywords short words that must start a domain part (e.g. "sbi-kyc"),
 * so that words like "usbinfo" do not match.
 * @property officialDomains real domains; these and their subdomains are trusted.
 */
data class Brand(
    val displayName: String,
    val substringKeywords: List<String> = emptyList(),
    val tokenKeywords: List<String> = emptyList(),
    val officialDomains: List<String>,
)

/** Lists of domains used by [UrlRedFlagChecker]. */
object KnownDomains {

    val brands: List<Brand> = listOf(
        Brand(
            "SBI",
            substringKeywords = listOf("onlinesbi", "yonosbi"),
            tokenKeywords = listOf("sbi", "yono"),
            officialDomains = listOf(
                "sbi.co.in", "onlinesbi.sbi", "onlinesbi.com", "sbi", "sbicard.com", "sbilife.co.in",
                "sbimf.com", "yonosbi.com", "sbigeneral.in", "sbisecurities.in", "sbicaps.com",
            ),
        ),
        Brand(
            "HDFC Bank",
            substringKeywords = listOf("hdfc"),
            officialDomains = listOf(
                "hdfcbank.com", "hdfc.com", "hdfclife.com", "hdfcergo.com", "hdfcsec.com", "hdfcfund.com",
                "hdfcbank.net",
            ),
        ),
        Brand(
            "ICICI Bank",
            substringKeywords = listOf("icici"),
            officialDomains = listOf(
                "icicibank.com", "icicidirect.com", "iciciprulife.com", "icicilombard.com", "icicipruamc.com",
            ),
        ),
        Brand(
            "Axis Bank",
            substringKeywords = listOf("axisbank"),
            tokenKeywords = listOf("axis"),
            officialDomains = listOf("axisbank.com", "axisbank.co.in", "axisdirect.in", "axismf.com"),
        ),
        Brand(
            "Kotak Bank",
            substringKeywords = listOf("kotak"),
            officialDomains = listOf("kotak.com", "kotaksecurities.com", "kotakmf.com", "kotaklife.com"),
        ),
        Brand(
            "PNB",
            substringKeywords = listOf("pnbindia", "netpnb"),
            tokenKeywords = listOf("pnb"),
            officialDomains = listOf("pnbindia.in", "netpnb.com", "pnb.bank.in", "pnbhousing.com"),
        ),
        Brand(
            "Bank of Baroda",
            substringKeywords = listOf("bankofbaroda", "bobibanking", "bobworld"),
            officialDomains = listOf("bankofbaroda.in", "bankofbaroda.com", "bobibanking.com", "bobworld.co.in"),
        ),
        Brand(
            "Canara Bank",
            substringKeywords = listOf("canarabank"),
            tokenKeywords = listOf("canara"),
            officialDomains = listOf("canarabank.com", "canarabank.in"),
        ),
        Brand(
            "Union Bank",
            substringKeywords = listOf("unionbank"),
            officialDomains = listOf("unionbankofindia.co.in", "unionbankonline.co.in"),
        ),
        Brand("IDFC FIRST Bank", substringKeywords = listOf("idfc"), officialDomains = listOf("idfcfirstbank.com")),
        Brand("Yes Bank", substringKeywords = listOf("yesbank"), officialDomains = listOf("yesbank.in")),
        Brand("IndusInd Bank", substringKeywords = listOf("indusind"), officialDomains = listOf("indusind.com")),
        Brand(
            "Paytm",
            substringKeywords = listOf("paytm"),
            officialDomains = listOf("paytm.com", "paytm.in", "paytmbank.com", "paytmmoney.com"),
        ),
        Brand("PhonePe", substringKeywords = listOf("phonepe"), officialDomains = listOf("phonepe.com")),
        Brand(
            "Google Pay",
            substringKeywords = listOf("googlepay"),
            tokenKeywords = listOf("gpay"),
            officialDomains = listOf("google.com", "google.co.in", "gpay.app.goo.gl"),
        ),
        Brand(
            "NPCI / BHIM UPI",
            substringKeywords = listOf("npci", "bhimupi"),
            tokenKeywords = listOf("bhim"),
            officialDomains = listOf("npci.org.in", "bhimupi.org.in"),
        ),
        Brand(
            "Aadhaar (UIDAI)",
            substringKeywords = listOf("uidai", "aadhaar", "aadhar"),
            officialDomains = listOf("uidai.gov.in", "myaadhaar.uidai.gov.in"),
        ),
        Brand(
            "Income Tax Dept",
            substringKeywords = listOf("incometax"),
            officialDomains = listOf("incometax.gov.in", "incometaxindia.gov.in"),
        ),
        Brand("RBI", tokenKeywords = listOf("rbi"), officialDomains = listOf("rbi.org.in")),
        Brand(
            "India Post",
            substringKeywords = listOf("indiapost"),
            officialDomains = listOf("indiapost.gov.in", "ippbonline.com"),
        ),
        Brand(
            "EPFO",
            substringKeywords = listOf("epfindia"),
            tokenKeywords = listOf("epfo"),
            officialDomains = listOf("epfindia.gov.in"),
        ),
        Brand(
            "Amazon",
            substringKeywords = listOf("amazon"),
            officialDomains = listOf(
                "amazon.in", "amazon.com", "amazonpay.in", "amazon.co.uk", "amazonaws.com", "media-amazon.com",
                "amazon.dev",
            ),
        ),
        Brand("Flipkart", substringKeywords = listOf("flipkart"), officialDomains = listOf("flipkart.com")),
    )

    /** Link-shortening services that hide the real destination. */
    val urlShorteners: Set<String> = setOf(
        "bit.ly", "bitly.com", "tinyurl.com", "goo.gl", "t.co", "ow.ly", "is.gd", "buff.ly", "rebrand.ly",
        "cutt.ly", "shorturl.at", "rb.gy", "tiny.cc", "s.id", "t.ly", "v.gd", "shorte.st", "adf.ly",
        "lnkd.in", "surl.li", "bl.ink", "short.io", "tinu.be", "u.to", "clck.ru", "qr.ae", "shrtco.de",
        "urlz.fr", "x.co", "inx.lv",
    )

    /** Top-level domains that are cheap and often abused by scam sites. */
    val suspiciousTlds: Set<String> = setOf(
        "xyz", "top", "club", "online", "site", "live", "icu", "buzz", "tk", "ml", "ga", "cf", "gq",
        "work", "click", "link", "rest", "cam", "monster", "cyou", "sbs", "cfd", "lol", "quest", "vip",
    )

    /** Two-part suffixes where the registrable domain has three labels (e.g. sbi.co.in). */
    val secondLevelSuffixes: Set<String> = setOf(
        "co.in", "gov.in", "org.in", "net.in", "ac.in", "nic.in", "res.in", "edu.in", "firm.in", "gen.in",
        "ind.in", "bank.in", "co.uk", "org.uk", "gov.uk", "ac.uk", "com.au", "net.au", "org.au", "co.jp",
        "com.br", "com.sg", "co.za", "com.cn", "app.goo.gl",
    )

    /** Words often seen in phishing links. */
    val scamKeywords: List<String> = listOf(
        "kyc", "verify", "verification", "update", "login", "signin", "secure", "account", "blocked",
        "suspend", "reward", "refund", "cashback", "lottery", "prize", "winner", "free", "gift", "bonus",
        "otp", "redeem", "claim", "electricity", "billpay", "pan-update", "panupdate", "kbc",
    )
}
