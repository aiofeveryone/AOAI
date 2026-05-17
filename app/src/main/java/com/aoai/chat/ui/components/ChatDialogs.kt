package com.aoai.chat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aoai.chat.R
import java.util.Locale

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_title)) },
        text = {
            Column {
                Text(stringResource(R.string.about_subtitle), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.about_description))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
        }
    )
}

@Composable
fun LanguageDialog(
    selectedLocale: Locale,
    onLocaleSelected: (Locale) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val languageRawList = listOf(
        context.getString(R.string.detect_language) to Locale.getDefault(),
        "Abkhazian" to Locale("ab"), "Acehnese" to Locale("ace"), "Acoli" to Locale("ach"),
        "Afar" to Locale("aa"), "Afrikaans" to Locale("af"), "Akan" to Locale("ak"),
        "Albanian" to Locale("sq"), "Alur" to Locale("alz"), "Amharic" to Locale("am"),
        "Arabic" to Locale("ar"), "Armenian" to Locale("ka"), "Assamese" to Locale("as"),
        "Avaric" to Locale("av"), "Awadhi" to Locale("awa"), "Aymara" to Locale("ay"),
        "Azerbaijani" to Locale("az"), "Balinese" to Locale("ban"), "Baluchi" to Locale("bal"),
        "Bambara" to Locale("bm"), "Bangla" to Locale("bn"), "Baoulé" to Locale("bci"),
        "Bashkir" to Locale("ba"), "Basque" to Locale("eu"), "Batak Karo" to Locale("btx"),
        "Batak Simalungun" to Locale("bts"), "Batak Toba" to Locale("bbc"), "Belarusian" to Locale("be"),
        "Bemba" to Locale("bem"), "Betawi" to Locale("bew"), "Bhojpuri" to Locale("bho"),
        "Bikol" to Locale("bik"), "Bosnian" to Locale("bs"), "Breton" to Locale("br"),
        "Bulgarian" to Locale("bg"), "Buriat" to Locale("bua"), "Burmese" to Locale("my"),
        "Cantonese" to Locale("zh", "HK"), "Catalan" to Locale("ca"), "Cebuano" to Locale("ceb"),
        "Central Kurdish" to Locale("ckb"), "Chamorro" to Locale("ch"), "Chechen" to Locale("ce"),
        "Chiga" to Locale("cgg"), "Chinese (Simplified)" to Locale.SIMPLIFIED_CHINESE,
        "Chinese (Traditional)" to Locale.TRADITIONAL_CHINESE, "Chuukese" to Locale("chk"),
        "Chuvash" to Locale("cv"), "Corsican" to Locale("co"), "Crimean Tatar" to Locale("crh"),
        "Croatian" to Locale("hr"), "Czech" to Locale("cs"), "Danish" to Locale("da"),
        "Dari" to Locale("prs"), "Dinka" to Locale("din"), "Divehi" to Locale("dv"),
        "Dogri" to Locale("doi"), "Dombe" to Locale("dom"), "Dutch" to Locale("nl"),
        "Dyula" to Locale("dyu"), "Dzongkha" to Locale("dz"), "English" to Locale.ENGLISH,
        "Esperanto" to Locale("eo"), "Estonian" to Locale("et"), "Ewe" to Locale("ee"),
        "Faroese" to Locale("fo"), "Fijian" to Locale("fj"), "Filipino" to Locale("fil"),
        "Finnish" to Locale("fi"), "Fon" to Locale("fon"), "French" to Locale.FRENCH,
        "Friulian" to Locale("fur"), "Fulani" to Locale("ff"), "Ga" to Locale("gaa"),
        "Galician" to Locale("gl"), "Ganda" to Locale("lg"), "Georgian" to Locale("ka"),
        "German" to Locale.GERMAN, "Greek" to Locale("el"), "Guarani" to Locale("gn"),
        "Gujarati" to Locale("gu"), "Haitian Creole" to Locale("ht"), "Hakha Chin" to Locale("cnm"),
        "Hausa" to Locale("ha"), "Hawaiian" to Locale("haw"), "Hebrew" to Locale("he"),
        "Hiligaynon" to Locale("hil"), "Hindi" to Locale("hi"), "Hmong" to Locale("hmn"),
        "Hungarian" to Locale("hu"), "Hunsrik" to Locale("hrx"), "Iban" to Locale("iba"),
        "Icelandic" to Locale("is"), "Igbo" to Locale("ig"), "Iloko" to Locale("ilo"),
        "Indonesian" to Locale("id"), "Irish" to Locale("ga"), "Italian" to Locale.ITALIAN,
        "Jamaican Patois" to Locale("jam"), "Japanese" to Locale.JAPANESE, "Javanese" to Locale("jv"),
        "Jingpo" to Locale("kac"), "Kalaallisut" to Locale("kl"), "Kannada" to Locale("kn"),
        "Kanuri" to Locale("kr"), "Kazakh" to Locale("kk"), "Khasi" to Locale("kha"),
        "Khmer" to Locale("km"), "Kinyarwanda" to Locale("rw"), "Kituba" to Locale("ktu"),
        "Kokborok" to Locale("trp"), "Komi" to Locale("kv"), "Kongo" to Locale("kg"),
        "Konkani" to Locale("kok"), "Korean" to Locale.KOREAN, "Krio" to Locale("kri"),
        "Kurdish" to Locale("ku"), "Kyrgyz" to Locale("ky"), "Lao" to Locale("lo"),
        "Latgalian" to Locale("ltg"), "Latin" to Locale("la"), "Latvian" to Locale("lv"),
        "Ligurian" to Locale("lij"), "Limburgish" to Locale("li"), "Lingala" to Locale("ln"),
        "Lithuanian" to Locale("lt"), "Lombard" to Locale("lmo"), "Luo" to Locale("luo"),
        "Luxembourgish" to Locale("lb"), "Macedonian" to Locale("mk"), "Madurese" to Locale("mad"),
        "Maithili" to Locale("mai"), "Makasar" to Locale("mak"), "Malagasy" to Locale("mg"),
        "Malay" to Locale("ms"), "Malay (Arabic)" to Locale("ms", "Arab"), "Malayalam" to Locale("ml"),
        "Maltese" to Locale("mt"), "Mam" to Locale("mam"), "Manipuri (Meitei Mayek)" to Locale("mni"),
        "Manx" to Locale("gv"), "Māori" to Locale("mi"), "Marathi" to Locale("mr"),
        "Marshallese" to Locale("mh"), "Marwari" to Locale("mwr"), "Meadow Mari" to Locale("mhr"),
        "Minangkabau" to Locale("min"), "Mizo" to Locale("lus"), "Mongolian" to Locale("mn"),
        "Morisyen" to Locale("mfe"), "Nahuatl (Eastern Huasteca)" to Locale("nhe"), "Ndau" to Locale("ndc"),
        "Nepalbhasa (Newari)" to Locale("new"), "Nepali" to Locale("ne"), "NKo" to Locale("nqo"),
        "Northern Sami" to Locale("se"), "Northern Sotho" to Locale("nso"), "Norwegian" to Locale("no"),
        "Nuer" to Locale("nus"), "Nyanja" to Locale("ny"), "Occitan" to Locale("oc"),
        "Odia" to Locale("or"), "Oromo" to Locale("om"), "Ossetic" to Locale("os"),
        "Pampanga" to Locale("pam"), "Pangasinan" to Locale("pag"), "Papiamento" to Locale("pap"),
        "Pashto" to Locale("ps"), "Persian" to Locale("fa"), "Polish" to Locale("pl"),
        "Portuguese" to Locale("pt"), "Portuguese (Portugal)" to Locale("pt", "PT"),
        "Punjabi" to Locale("pa"), "Punjabi (Arabic)" to Locale("pa", "Arab"), "Q'eqchi'" to Locale("kek"),
        "Quechua" to Locale("qu"), "Romanian" to Locale("ro"), "Romany" to Locale("rom"),
        "Rundi" to Locale("rn"), "Russian" to Locale("ru"), "Samoan" to Locale("sm"),
        "Sango" to Locale("sg"), "Sanskrit" to Locale("sa"), "Santali (Latin)" to Locale("sat"),
        "Scottish Gaelic" to Locale("gd"), "Serbian" to Locale("sr"), "Seselwa Creole French" to Locale("crs"),
        "Shan" to Locale("shn"), "Shona" to Locale("sn"), "Sicilian" to Locale("scn"),
        "Silesian" to Locale("szl"), "Sindhi" to Locale("sd"), "Sinhala" to Locale("si"),
        "Slovak" to Locale("sk"), "Slovenian" to Locale("sl"), "Somali" to Locale("so"),
        "South Ndebele" to Locale("nr"), "Southern Sotho" to Locale("st"), "Spanish" to Locale("es"),
        "Sundanese" to Locale("su"), "Susu" to Locale("sus"), "Swahili" to Locale("sw"),
        "Swati" to Locale("ss"), "Swedish" to Locale("sv"), "Tahitian" to Locale("ty"),
        "Tajik" to Locale("tg"), "Tamazight" to Locale("ber"), "Tamazight (Tifinagh)" to Locale("ber", "Tfng"),
        "Tamil" to Locale("ta"), "Tatar" to Locale("tt"), "Telugu" to Locale("te"),
        "Tetum" to Locale("tet"), "Thai" to Locale("th"), "Tibetan" to Locale("bo"),
        "Tigrinya" to Locale("ti"), "Tiv" to Locale("tiv"), "Tok Pisin" to Locale("tpi"),
        "Tongan" to Locale("to"), "Tsonga" to Locale("ts"), "Tswana" to Locale("tn"),
        "Tulu" to Locale("tcy"), "Tumbuka" to Locale("tum"), "Turkish" to Locale("tr"),
        "Turkmen" to Locale("tk"), "Tuvinian" to Locale("tyv"), "Udmurt" to Locale("udm"),
        "Ukrainian" to Locale("uk"), "Urdu" to Locale("ur"), "Uyghur" to Locale("ug"),
        "Uzbek" to Locale("uz"), "Venda" to Locale("ve"), "Venetian" to Locale("vec"),
        "Vietnamese" to Locale("vi"), "Waray" to Locale("war"), "Welsh" to Locale("cy"),
        "Western Frisian" to Locale("fy"), "Wolof" to Locale("wo"), "Xhosa" to Locale("xh"),
        "Yakut" to Locale("sah"), "Yiddish" to Locale("yi"), "Yoruba" to Locale("yo"),
        "Yucatec Maya" to Locale("yua"), "Zapotec" to Locale("zap"), "Zulu" to Locale("zu")
    )

    val languageOptions = languageRawList.map { (name, locale) ->
        if (name == context.getString(R.string.detect_language)) name to locale
        else "$name(${locale.getDisplayLanguage(Locale.KOREAN)})" to locale
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_settings)) },
        text = {
            Box(modifier = Modifier.heightIn(max = 450.dp)) {
                LazyColumn {
                    items(languageOptions) { (displayName, locale) ->
                        ListItem(
                            headlineContent = { Text(displayName) },
                            trailingContent = { if (selectedLocale.language == locale.language) { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) } },
                            modifier = Modifier.clickable { onLocaleSelected(locale); onDismiss() }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun ClearChatDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clear_history_title)) },
        text = { Text(stringResource(R.string.clear_history_body)) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
