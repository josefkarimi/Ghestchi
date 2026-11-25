package com.example.domain

object InitialSeedData {

    fun defaultCategories(): List<Category> = listOf(
        Category(
            id = "cat-loan",
            name = "اقساط و وام",
            type = "expense",
            emoji = "🏦",
            color = "#059669",
            subcategories = listOf("قسط بانک", "خرید اقساطی", "قرعه‌کشی"),
            debtCategory = false
        ),
        Category(
            id = "cat-food",
            name = "خوراک و رستوران",
            type = "expense",
            emoji = "🍔",
            color = "#f59e0b",
            subcategories = listOf("سوپرمارکت", "رستوران", "کافه"),
            debtCategory = false
        ),
        Category(
            id = "cat-transport",
            name = "حمل و نقل و خودرو",
            type = "expense",
            emoji = "🚗",
            color = "#3b82f6",
            subcategories = listOf("بنزین موتور", "تاکسی اینترنتی", "تعویض روغن"),
            debtCategory = false
        ),
        Category(
            id = "cat-shopping",
            name = "پوشاک و خرید",
            type = "expense",
            emoji = "🛍️",
            color = "#ec4899",
            subcategories = listOf("لباس", "لوازم خانه"),
            debtCategory = false
        ),
        Category(
            id = "cat-bills",
            name = "قبوض و شارژ",
            type = "expense",
            emoji = "🧾",
            color = "#6366f1",
            subcategories = listOf("اینترنت و موبایل", "آب و برق و گاز"),
            debtCategory = false
        ),
        Category(
            id = "cat-lend",
            name = "قرض دادن / بازپرداخت قرض",
            type = "expense",
            emoji = "🤝",
            color = "#e11d48",
            subcategories = listOf("قرض دادن به دیگران", "تسویه بدهی من"),
            debtCategory = true
        ),
        Category(
            id = "cat-salary",
            name = "حقوق و درآمد",
            type = "income",
            emoji = "💰",
            color = "#10b981",
            subcategories = listOf("حقوق ماهانه", "پاداش و اضافه کار"),
            debtCategory = false
        ),
        Category(
            id = "cat-borrow",
            name = "قرض گرفتن / پس گرفتن طلب",
            type = "income",
            emoji = "📥",
            color = "#0ea5e9",
            subcategories = listOf("قرض گرفتن از دیگران", "دریافت طلب"),
            debtCategory = true
        ),
        Category(
            id = "cat-1795262456294",
            name = "تفریح",
            type = "expense",
            emoji = "📌",
            color = "#f97316",
            subcategories = listOf("سفر", "بیلیارد", "بازی", "باشگاه"),
            debtCategory = false
        )
    )

    fun initialPayload(): BackupPayload = BackupPayload(
        loans = listOf(
            Loan("loan-1795366866503", "شارژ ساختمون", "فاطمه", 1_000_000L, 12, 1, 9, 1405, "", listOf(0)),
            Loan("loan-1795243971155", "اوانو", "همراه اول", 1_930_000L, 1, 1, 9, 1405, "", listOf(0)),
            Loan("loan-1795209307772", "باشگاه ۱۴۰۵", "فریبرز", 6_000_000L, 12, 1, 1, 1405, "", (0..8).toList()),
            Loan("loan-1795209155119", "مساعده", "حقوق", 12_500_000L, 2, 1, 9, 1405, "", listOf(0)),
            Loan("loan-1795209070771", "اعتبار بلو", "بلو", 9_064_772L, 6, 6, 8, 1405, "", listOf(0, 1)),
            Loan("loan-1795208959615", "شلوار و کلاه کمپ شاپینگ", "اسنپ پی", 1_932_500L, 4, 1, 9, 1405, "", listOf(0)),
            Loan("loan-1795208830810", "کاپشن و شلوار کمپ شاپینگ", "اسنپ پی", 3_927_500L, 4, 1, 8, 1405, "", listOf(0, 1)),
            Loan("loan-1795208754417", "اسنپ پی محدثه", "اسنپ پی", 1_562_500L, 1, 1, 9, 1405, "", listOf(0)),
            Loan("loan-1795208524406", "اسنپ پی یوسف", "اسنپ پی", 7_654_182L, 1, 1, 9, 1405, "", listOf(0)),
            Loan("loan-1795208229845", "بوت اکو", "اسنپ پی", 2_860_000L, 4, 1, 8, 1405, "", listOf(0, 1)),
            Loan("loan-1795208070657", "بلو محدثه", "بلو", 3_530_000L, 6, 20, 9, 1405, "", emptyList()),
            Loan("loan-1795207911749", "طلا اسنپ پی", "اسنپ پی", 9_400_000L, 12, 21, 12, 1404, "", (0..8).toList()),
            Loan("loan-1795207766865", "گوشی محدثه", "ازکی بلو", 2_859_196L, 18, 21, 9, 1404, "", (0..11).toList()),
            Loan("loan-1795207679171", "بلو یوسف", "بلو بانک", 2_315_862L, 12, 18, 9, 1405, "", emptyList()),
            Loan("loan-1795207548163", "طلا", "بلو بانک", 17_800_000L, 6, 4, 8, 1405, "", listOf(0, 1)),
            Loan("loan-1795207431407", "وام موتور", "تجارت", 3_900_000L, 60, 22, 6, 1404, "5859831848517560", (0..14).toList()),
            Loan("loan-1795207216898", "وام ازدواج محدثه", "تجارت", 636_400L, 116, 3, 1, 1401, "5859837008918290", (0 until 57).toList()),
            Loan("loan-1795206047087", "وام ازدواج یوسف", "بانک تجارت", 1_272_800L, 116, 10, 1, 1401, "5859-8370-0891-8290", (0 until 57).toList())
        ),
        accounts = listOf(
            Account("acc-main", "تجارت جاری", 20_000_000L, "", "#0284c7"),
            Account("acc-cash", "بلو", 900_000L, "", "#e11d48"),
            Account("acc-1795288315680", "محدثه", 16_500_000L, "", "#7c3aed")
        ),
        categories = defaultCategories(),
        transactions = listOf(
            Transaction("tx-1795422900450", "income", 700_000L, "acc-cash", null, null, "cat-salary", "پاداش و اضافه کار", null, 1405, 9, 2, "دنگ کورش"),
            Transaction("tx-1795422840450", "expense", 25_000_000L, "acc-cash", null, null, "cat-lend", "تسویه بدهی من", "person-1795262291376", 1405, 9, 2, "دلار"),
            Transaction("tx-1795422548787", "income", 30_000_000L, "acc-cash", null, null, "cat-salary", "حقوق ماهانه", null, 1405, 9, 2, "لمیز"),
            Transaction("tx-1795373019725", "expense", 98_133L, "acc-cash", null, null, "cat-lend", "قرض دادن به دیگران", "person-1795288567669", 1405, 9, 1, ""),
            Transaction("tx-inst-1795368122617", "expense", 1_272_800L, "acc-cash", null, null, "cat-loan", "قسط بانک", null, 1405, 9, 1, "پرداخت قسط ۵۷ وام ازدواج یوسف", "loan-1795206047087", 56),
            Transaction("tx-inst-1795368111159", "expense", 636_400L, "acc-cash", null, null, "cat-loan", "قسط بانک", null, 1405, 9, 1, "پرداخت قسط ۵۷ وام ازدواج محدثه", "loan-1795207216898", 56),
            Transaction("tx-1795368033306", "expense", 1_500_000L, "acc-cash", null, null, "cat-1795262456294", "باشگاه", null, 1405, 9, 1, ""),
            Transaction("tx-1795367857819", "expense", 280_000L, "acc-cash", null, null, "cat-lend", "تسویه بدهی من", "person-1795254068764", 1405, 9, 1, ""),
            Transaction("tx-inst-1795367598356", "expense", 1_930_000L, "acc-cash", null, null, "cat-loan", "قسط بانک", null, 1405, 9, 1, "پرداخت قسط ۱ اوانو", "loan-1795243971155", 0),
            Transaction("tx-inst-1795367583410", "expense", 1_000_000L, "acc-cash", null, null, "cat-loan", "قرعه‌کشی", null, 1405, 9, 1, "پرداخت قسط ۱ شارژ ساختمون", "loan-1795366866503", 0),
            Transaction("tx-1795367543753", "expense", 2_000_000L, "acc-cash", null, null, "cat-lend", "تسویه بدهی من", "person-1795262291376", 1405, 9, 1, "بابت دلار برای فاطمه واریز کردم"),
            Transaction("tx-inst-1795367170961", "expense", 9_064_772L, "acc-cash", null, null, "cat-loan", "قسط بانک", null, 1405, 9, 1, "پرداخت قسط ۲ اعتبار بلو", "loan-1795209070771", 1),
            Transaction("tx-inst-1795367147408", "expense", 17_800_000L, "acc-cash", null, null, "cat-loan", "قسط بانک", null, 1405, 9, 1, "پرداخت قسط ۲ طلا", "loan-1795207548163", 1),
            Transaction("tx-1795367111536", "income", 12_500_000L, "acc-cash", null, null, "cat-salary", "پاداش و اضافه کار", null, 1405, 9, 1, "بابت ادجاسمنت"),
            Transaction("tx-inst-1795367053824", "expense", 12_500_000L, "acc-cash", null, null, "cat-loan", "قسط بانک", null, 1405, 9, 1, "پرداخت قسط ۱ مساعده", "loan-1795209155119", 0),
            Transaction("tx-1795366973364", "income", 49_646_287L, "acc-cash", null, null, "cat-salary", "حقوق ماهانه", null, 1405, 9, 1, "اسنپ"),
            Transaction("tx-inst-1795366933749", "expense", 7_654_182L, "acc-cash", null, null, "cat-loan", "خرید اقساطی", null, 1405, 9, 1, "پرداخت قسط ۱ اسنپ پی یوسف", "loan-1795208524406", 0),
            Transaction("tx-inst-1795366905248", "expense", 2_860_000L, "acc-cash", null, null, "cat-loan", "خرید اقساطی", null, 1405, 9, 1, "پرداخت قسط ۲ بوت اکو", "loan-1795208229845", 1),
            Transaction("tx-1795288598764", "expense", 2_500L, "acc-1795288315680", null, null, "cat-lend", "قرض دادن به دیگران", "person-1795288567669", 1405, 8, 30, ""),
            Transaction("tx-1795288459262", "expense", 4_675_000L, "acc-1795288315680", null, null, "cat-shopping", "لباس", null, 1405, 8, 30, "جوراب"),
            Transaction("tx-inst-1795288408361", "expense", 1_562_500L, "acc-1795288315680", null, null, "cat-loan", "خرید اقساطی", null, 1405, 8, 30, "پرداخت قسط ۱ اسنپ پی محدثه", "loan-1795208754417", 0),
            Transaction("tx-inst-1795288399662", "expense", 3_927_500L, "acc-1795288315680", null, null, "cat-loan", "خرید اقساطی", null, 1405, 8, 30, "پرداخت قسط ۲ کاپشن و شلوار کمپ شاپینگ", "loan-1795208830810", 1),
            Transaction("tx-inst-1795288386105", "expense", 1_932_500L, "acc-1795288315680", null, null, "cat-loan", "خرید اقساطی", null, 1405, 8, 30, "پرداخت قسط ۱ شلوار و کلاه کمپ شاپینگ", "loan-1795208959615", 0),
            Transaction("tx-1795275491159", "expense", 50_000L, "acc-cash", null, null, "cat-transport", "بنزین موتور", null, 1405, 8, 30, ""),
            Transaction("tx-1795269886360", "expense", 200_000L, "acc-cash", null, null, "cat-food", "سوپرمارکت", null, 1405, 8, 30, "کیوبی"),
            Transaction("tx-1795262479575", "expense", 27_000_000L, "acc-main", null, null, "cat-1795262456294", "سفر", null, 1405, 8, 30, "دلار برای ترکیه"),
            Transaction("tx-1795262323872", "income", 27_000_000L, "acc-main", null, null, "cat-borrow", "قرض گرفتن از دیگران", "person-1795262291376", 1405, 8, 30, "خرید دلار از فاطمه"),
            Transaction("tx-1795254251458", "expense", 830_000L, "acc-cash", null, null, "cat-shopping", "لباس", null, 1405, 8, 30, "کلاه دزدی کمپ شاپینگ"),
            Transaction("tx-1795254185250", "income", 830_000L, "acc-cash", null, null, "cat-borrow", "دریافت طلب", "person-1795254068764", 1405, 8, 30, "کلاه دزدی کمپ شاپینگ"),
            Transaction("tx-1795254113012", "expense", 550_000L, "acc-cash", null, null, "cat-lend", "قرض دادن به دیگران", "person-1795254068764", 1405, 8, 30, "کافه Rene همیلا"),
            Transaction("tx-1795253379550", "expense", 12_500_000L, "acc-main", null, null, "cat-lend", "قرض دادن به دیگران", "person-1795253349856", 1405, 8, 30, "بابت هزینه ایزوگام ساختمون")
        ),
        people = listOf(
            Person("person-1795253349856", "بابا", ""),
            Person("person-1795254068764", "فریبرز", ""),
            Person("person-1795262291376", "حاج علی", ""),
            Person("person-1795288567669", "رند", "")
        )
    )
}
