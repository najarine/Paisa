package com.paisa.najarine.audio

data class QuranReciter(
    val id: String,
    val nameBangla: String,
    val nameArabic: String,
    val nameEnglish: String,
    val style: String,
    val networkIdentifier: String,
    val everyAyahFolder: String
)

object QuranReciters {
    val ALL_RECITERS = listOf(
        QuranReciter(
            id = "ar.alafasy",
            nameBangla = "মিশারি রশিদ আল-আফাসি",
            nameArabic = "مشاري راشد العفاسي",
            nameEnglish = "Mishary Rashid Alafasy",
            style = "মুরাত্তাল (Murattal)",
            networkIdentifier = "ar.alafasy",
            everyAyahFolder = "Alafasy_128kbps"
        ),
        QuranReciter(
            id = "ar.abdulsamad",
            nameBangla = "ক্বারী আব্দুল বাসিত আব্দুল সামাদ",
            nameArabic = "عبد الباسط عبد الصمد",
            nameEnglish = "Abdul Basit Abdul Samad",
            style = "মুরাত্তাল (Murattal)",
            networkIdentifier = "ar.abdulsamad",
            everyAyahFolder = "Abdul_Basit_Murattal_192kbps"
        ),
        QuranReciter(
            id = "ar.abdurrahmaansudais",
            nameBangla = "শায়খ আব্দুর রহমান আস-সুদাইস",
            nameArabic = "عبد الرحمن السديس",
            nameEnglish = "Abdur-Rahman As-Sudais",
            style = "ইমাম, মসজিদুল হারাম (Imam of Makkah)",
            networkIdentifier = "ar.abdurrahmaansudais",
            everyAyahFolder = "Abdurrahmaan_As-Sudais_192kbps"
        ),
        QuranReciter(
            id = "ar.husary",
            nameBangla = "শায়খ মাহমুদ খলিল আল-হুসারি",
            nameArabic = "محمود خليل الحصري",
            nameEnglish = "Mahmoud Khalil Al-Husary",
            style = "তাজবীদ ও মুরাত্তাল (Tajweed Master)",
            networkIdentifier = "ar.husary",
            everyAyahFolder = "Husary_128kbps"
        ),
        QuranReciter(
            id = "ar.hudhaify",
            nameBangla = "শায়খ আলি আল-হুজাইফি",
            nameArabic = "علي بن عبد الرحمن الحذيفي",
            nameEnglish = "Ali Al-Hudhaify",
            style = "ইমাম, মসজিদে নববী (Imam of Madinah)",
            networkIdentifier = "ar.hudhaify",
            everyAyahFolder = "Hudhaify_128kbps"
        ),
        QuranReciter(
            id = "ar.shaatree",
            nameBangla = "আবু বকর আশ-শাতরি",
            nameArabic = "أبو بكر الشاطري",
            nameEnglish = "Abu Bakr Al-Shatri",
            style = "ভাবগম্ভীর তিলাওয়াত (Emotional)",
            networkIdentifier = "ar.shaatree",
            everyAyahFolder = "Abu_Bakr_Ash-Shaatree_128kbps"
        ),
        QuranReciter(
            id = "ar.mahermuaiqly",
            nameBangla = "শায়খ মাহের আল-মুআইকলি",
            nameArabic = "ماهر المعيقلي",
            nameEnglish = "Maher Al Muaiqly",
            style = "ইমাম, মসজিদুল হারাম (Imam of Makkah)",
            networkIdentifier = "ar.mahermuaiqly",
            everyAyahFolder = "MaherAlMuaiqly128kbps"
        ),
        QuranReciter(
            id = "ar.ahmedajamy",
            nameBangla = "শায়খ আহমদ আল-আজমি",
            nameArabic = "أحمد بن علي العجمي",
            nameEnglish = "Ahmed ibn Ali al-Ajamy",
            style = "সুরেলা তিলাওয়াত (Melodious)",
            networkIdentifier = "ar.ahmedajamy",
            everyAyahFolder = "Ahmed_ibn_Ali_al-Ajamy_128kbps"
        ),
        QuranReciter(
            id = "ar.muhammadayyoub",
            nameBangla = "শায়খ মুহাম্মদ আইয়ুব",
            nameArabic = "محمد أيوب",
            nameEnglish = "Muhammad Ayyub",
            style = "ইমাম, মসজিদে নববী (Imam of Madinah)",
            networkIdentifier = "ar.muhammadayyoub",
            everyAyahFolder = "Muhammad_Ayyoub_128kbps"
        ),
        QuranReciter(
            id = "ar.muhammadjibreel",
            nameBangla = "শায়খ মুহাম্মদ জিবরিল",
            nameArabic = "محمد جبريل",
            nameEnglish = "Muhammad Jibreel",
            style = "কুরআন তিলাওয়াত (Classic)",
            networkIdentifier = "ar.muhammadjibreel",
            everyAyahFolder = "Muhammad_Jibreel_128kbps"
        ),
        QuranReciter(
            id = "ar.saudshuraim",
            nameBangla = "শায়খ সৌদ আশ-শুরাইম",
            nameArabic = "سعود الشريم",
            nameEnglish = "Saud Al-Shuraim",
            style = "ইমাম, মসজিদুল হারাম (Makkah)",
            networkIdentifier = "ar.saudshuraim",
            everyAyahFolder = "Saud_Ash-Shuraim_128kbps"
        ),
        QuranReciter(
            id = "ar.yasseraldosari",
            nameBangla = "শায়খ ইয়াসির আল-দোসারি",
            nameArabic = "ياسر الدوسري",
            nameEnglish = "Yasser Al-Dosari",
            style = "ইমাম, মসজিদুল হারাম (Makkah)",
            networkIdentifier = "ar.yasseraldosari",
            everyAyahFolder = "Yasser_Ad-Dosari_128kbps"
        ),
        QuranReciter(
            id = "ar.nasserqatami",
            nameBangla = "শায়খ নাসের আল-কাতামি",
            nameArabic = "ناصر القطامي",
            nameEnglish = "Nasser Al Qatami",
            style = "ভাবগম্ভীর তিলাওয়াত (Emotional)",
            networkIdentifier = "ar.nasserqatami",
            everyAyahFolder = "Nasser_Al_Qatami_128kbps"
        ),
        QuranReciter(
            id = "ar.minshawi",
            nameBangla = "কারী মুহাম্মদ সিদ্দিক আল-মিনশাবি",
            nameArabic = "محمد صديق المنشاوي",
            nameEnglish = "Mohamed Siddiq El-Minshawi",
            style = "তাজবীদ মাস্টার (Tajweed Legend)",
            networkIdentifier = "ar.minshawi",
            everyAyahFolder = "Minshawy_Murattal_128kbps"
        )
    )

    val DEFAULT_RECITER = ALL_RECITERS[0]

    fun getReciterById(id: String): QuranReciter {
        return ALL_RECITERS.find { it.id == id } ?: DEFAULT_RECITER
    }
}
