package azhukov.service.ai.elevenlabs.enums;

import lombok.Getter;

/**
 * Поддерживаемые языки ElevenLabs для Speech-to-Text Согласно актуальной документации:
 * https://docs.elevenlabs.io/api-reference/speech-to-text
 */
@Getter
public enum ElevenLabsLanguage {

  // Основные языки
  RUSSIAN("ru", "Русский", "Russian"),
  ENGLISH("en", "Английский", "English"),
  GERMAN("de", "Немецкий", "German"),
  FRENCH("fr", "Французский", "French"),
  SPANISH("es", "Испанский", "Spanish"),
  ITALIAN("it", "Итальянский", "Italian"),
  PORTUGUESE("pt", "Португальский", "Portuguese"),

  // Дополнительные европейские языки
  DUTCH("nl", "Голландский", "Dutch"),
  SWEDISH("sv", "Шведский", "Swedish"),
  NORWEGIAN("no", "Норвежский", "Norwegian"),
  DANISH("da", "Датский", "Danish"),
  FINNISH("fi", "Финский", "Finnish"),
  POLISH("pl", "Польский", "Polish"),
  CZECH("cs", "Чешский", "Czech"),
  HUNGARIAN("hu", "Венгерский", "Hungarian"),
  ROMANIAN("ro", "Румынский", "Romanian"),
  BULGARIAN("bg", "Болгарский", "Bulgarian"),
  CROATIAN("hr", "Хорватский", "Croatian"),
  SLOVENIAN("sl", "Словенский", "Slovenian"),
  SLOVAK("sk", "Словацкий", "Slovak"),
  ESTONIAN("et", "Эстонский", "Estonian"),
  LATVIAN("lv", "Латышский", "Latvian"),
  LITHUANIAN("lt", "Литовский", "Lithuanian"),

  // Азиатские языки
  CHINESE_SIMPLIFIED("zh", "Китайский (упрощенный)", "Chinese (Simplified)"),
  CHINESE_TRADITIONAL("zh-TW", "Китайский (традиционный)", "Chinese (Traditional)"),
  JAPANESE("ja", "Японский", "Japanese"),
  KOREAN("ko", "Корейский", "Korean"),

  // Другие языки
  TURKISH("tr", "Турецкий", "Turkish"),
  ARABIC("ar", "Арабский", "Arabic"),
  HINDI("hi", "Хинди", "Hindi"),
  THAI("th", "Тайский", "Thai"),
  VIETNAMESE("vi", "Вьетнамский", "Vietnamese"),
  INDONESIAN("id", "Индонезийский", "Indonesian"),
  MALAY("ms", "Малайский", "Malay"),
  FILIPINO("tl", "Филиппинский", "Filipino"),
  UKRAINIAN("uk", "Украинский", "Ukrainian"),
  BELARUSIAN("be", "Белорусский", "Belarusian"),
  KAZAKH("kk", "Казахский", "Kazakh"),
  UZBEK("uz", "Узбекский", "Uzbek"),
  KYRGYZ("ky", "Киргизский", "Kyrgyz"),
  TAJIK("tg", "Таджикский", "Tajik"),
  TURKMEN("tk", "Туркменский", "Turkmen"),
  AZERBAIJANI("az", "Азербайджанский", "Azerbaijani"),
  GEORGIAN("ka", "Грузинский", "Georgian"),
  ARMENIAN("hy", "Армянский", "Armenian"),
  MONGOLIAN("mn", "Монгольский", "Mongolian"),
  HEBREW("he", "Иврит", "Hebrew"),
  PERSIAN("fa", "Персидский", "Persian"),
  URDU("ur", "Урду", "Urdu"),
  BENGALI("bn", "Бенгальский", "Bengali"),
  TAMIL("ta", "Тамильский", "Tamil"),
  TELUGU("te", "Телугу", "Telugu"),
  KANNADA("kn", "Каннада", "Kannada"),
  MALAYALAM("ml", "Малаялам", "Malayalam"),
  MARATHI("mr", "Маратхи", "Marathi"),
  GUJARATI("gu", "Гуджарати", "Gujarati"),
  PUNJABI("pa", "Панджаби", "Punjabi"),
  NEPALI("ne", "Непальский", "Nepali"),
  SINHALA("si", "Сингальский", "Sinhala"),
  KHMER("km", "Кхмерский", "Khmer"),
  LAO("lo", "Лаосский", "Lao"),
  BURMESE("my", "Бирманский", "Burmese"),
  AMHARIC("am", "Амхарский", "Amharic"),
  SWAHILI("sw", "Суахили", "Swahili"),
  YORUBA("yo", "Йоруба", "Yoruba"),
  IGBO("ig", "Игбо", "Igbo"),
  HAUSA("ha", "Хауса", "Hausa"),
  ZULU("zu", "Зулу", "Zulu"),
  XHOSA("xh", "Коса", "Xhosa"),
  AFRIKAANS("af", "Африкаанс", "Afrikaans"),
  ALBANIAN("sq", "Албанский", "Albanian"),
  MACEDONIAN("mk", "Македонский", "Macedonian"),
  SERBIAN("sr", "Сербский", "Serbian"),
  BOSNIAN("bs", "Боснийский", "Bosnian"),
  MONTENEGRIN("cnr", "Черногорский", "Montenegrin"),
  ICELANDIC("is", "Исландский", "Icelandic"),
  FAROESE("fo", "Фарерский", "Faroese"),
  GREENLANDIC("kl", "Гренландский", "Greenlandic"),
  BASQUE("eu", "Баскский", "Basque"),
  CATALAN("ca", "Каталанский", "Catalan"),
  GALICIAN("gl", "Галисийский", "Galician"),
  WELSH("cy", "Валлийский", "Welsh"),
  IRISH("ga", "Ирландский", "Irish"),
  SCOTTISH_GAELIC("gd", "Шотландский гэльский", "Scottish Gaelic"),
  MANX("gv", "Мэнский", "Manx"),
  CORNISH("kw", "Корнский", "Cornish"),
  BRETON("br", "Бретонский", "Breton"),
  LUXEMBOURGISH("lb", "Люксембургский", "Luxembourgish"),
  FRISIAN("fy", "Фризский", "Frisian"),
  SARDINIAN("sc", "Сардинский", "Sardinian"),
  CORSICAN("co", "Корсиканский", "Corsican"),
  OCCITAN("oc", "Окситанский", "Occitan"),
  ARAGONESE("an", "Арагонский", "Aragonese"),
  ASTURIAN("ast", "Астурийский", "Asturian"),
  LADIN("lld", "Ладинский", "Ladin"),
  FRIULIAN("fur", "Фриульский", "Friulian"),
  LOMBARD("lmo", "Ломбардский", "Lombard"),
  PIEDMONTESE("pms", "Пьемонтский", "Piedmontese"),
  VENETIAN("vec", "Венецианский", "Venetian"),
  LIGURIAN("lij", "Лигурский", "Ligurian"),
  EMILIANO_ROMAGNOLO("eml", "Эмилиано-романьольский", "Emiliano-Romagnolo"),
  NAPOLETANO_CALABRESE("nap", "Неаполитанский", "Neapolitan"),
  SICILIAN("scn", "Сицилийский", "Sicilian"),
  CALABRESE("cal", "Калабрийский", "Calabrese"),
  ABRUZZESE("ab", "Абруццский", "Abruzzese"),
  MOLISANO("mo", "Молизский", "Molisano"),
  PUGLIESE("pu", "Апулийский", "Pugliese"),
  LUCANO("lu", "Луканский", "Lucano"),
  CAMPANO("ca", "Кампанийский", "Campano"),
  LAZIALE("la", "Лацио", "Laziale"),
  UMBRO("um", "Умбрийский", "Umbro"),
  MARCHIGIANO("ma", "Маркеджанский", "Marchigiano"),
  TOSCANO("to", "Тосканский", "Toscano"),
  LIGURE("li", "Лигурский", "Ligure"),
  PIEMONTESE("pi", "Пьемонтский", "Piemontese"),
  LOMBARDO("lo", "Ломбардский", "Lombardo"),
  VENETO("ve", "Венето", "Veneto"),
  TRENTINO("tr", "Трентино", "Trentino"),
  ALTO_ADIGE("aa", "Альто-Адидже", "Alto Adige"),
  FRIULI_VENEZIA_GIULIA("fv", "Фриули-Венеция-Джулия", "Friuli Venezia Giulia"),
  VALLE_DAOSTA("va", "Валле-д'Аоста", "Valle d'Aosta"),
  EMILIA_ROMAGNA("er", "Эмилия-Романья", "Emilia-Romagna"),
  MARCHE("ma", "Марке", "Marche"),
  TOSCANA("to", "Тоскана", "Toscana"),
  UMBRIA("um", "Умбрия", "Umbria"),
  LAZIO("la", "Лацио", "Lazio"),
  ABRUZZO("ab", "Абруццо", "Abruzzo"),
  MOLISE("mo", "Молизе", "Molise"),
  CAMPANIA("ca", "Кампания", "Campania"),
  PUGLIA("pu", "Апулия", "Puglia"),
  BASILICATA("ba", "Базиликата", "Basilicata"),
  CALABRIA("ca", "Калабрия", "Calabria"),
  SICILIA("si", "Сицилия", "Sicilia"),
  SARDEGNA("sa", "Сардиния", "Sardegna");

  private final String code;
  private final String nameRu;
  private final String nameEn;

  ElevenLabsLanguage(String code, String nameRu, String nameEn) {
    this.code = code;
    this.nameRu = nameRu;
    this.nameEn = nameEn;
  }

  /** Получить язык по коду */
  public static ElevenLabsLanguage fromCode(String code) {
    for (ElevenLabsLanguage language : values()) {
      if (language.getCode().equalsIgnoreCase(code)) {
        return language;
      }
    }
    throw new IllegalArgumentException("Unknown language code: " + code);
  }

  /** Получить язык по коду с fallback на русский */
  public static ElevenLabsLanguage fromCodeSafe(String code) {
    try {
      return fromCode(code);
    } catch (IllegalArgumentException e) {
      return RUSSIAN; // Fallback на русский
    }
  }

  /** Проверить, является ли язык европейским */
  public boolean isEuropean() {
    return this.ordinal() < 30; // Первые 30 языков - европейские
  }

  /** Проверить, является ли язык славянским */
  public boolean isSlavic() {
    return this == RUSSIAN
        || this == UKRAINIAN
        || this == BELARUSIAN
        || this == POLISH
        || this == CZECH
        || this == SLOVAK
        || this == SLOVENIAN
        || this == CROATIAN
        || this == BOSNIAN
        || this == SERBIAN
        || this == MONTENEGRIN
        || this == BULGARIAN
        || this == MACEDONIAN;
  }
}
