package com.example.data.openlibrary

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object OpenLibraryService {
    private const val TAG = "OpenLibraryService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Curated rich open source books catalog (Internet Archive, Project Gutenberg & Public Domain)
    val curatedCatalog: List<OpenBookItem> = listOf(
        OpenBookItem(
            id = "curated-nutuk",
            title = "Nutuk",
            author = "Mustafa Kemal Atatürk",
            coverUrl = "https://covers.openlibrary.org/b/id/11145155-L.jpg",
            category = "Tarih & Siyaset",
            language = "Türkçe",
            format = "EPUB",
            source = "Açık Arşiv",
            description = "Gazi Mustafa Kemal Atatürk'ün 1919-1927 yılları arasındaki Millî Mücadele ve Cumhuriyet'in kuruluş sürecini anlattığı tarihi başyapıtı.",
            downloadUrl = "https://www.gutenberg.org/cache/epub/61480/pg61480.txt",
            estimatedPages = 540,
            downloadCount = 48200,
            directContent = """
BÖLÜM 1: 1919 SENESİ MAYISININ 19'UNCU GÜNÜ SAMSUN'A ÇIKTIM

1919 yılı Mayısının 19'uncu günü Samsun'a çıktım. Genel durum ve görünüş şöyleydi:
Osmanlı Devleti'nin dahil bulunduğu grup, I. Dünya Savaşı'nda mağlup olmuş, Osmanlı ordusu her tarafta zedelenmiş, şartları ağır bir ateşkes antlaşması imzalanmış. Büyük Harbin uzun yılları boyunca millet yorgun ve fakir düşmüş.

Millet ve ordu, Padişah ve Halife'nin kurtuluşundan ibaret bir ümide bel bağlamıştı. Halbuki asıl kurtuluş, milletin kendi azim ve iradesinde gizliydi.

MİLLETİN DURUMU VE KARAR
Bu durum karşısında tek bir karar vardı. O da millî egemenliğe dayanan, kayıtsız şartsız, bağımsız yeni bir Türk devleti kurmak!
İşte Türk istiklalini ve Türk Cumhuriyeti'ni korumak ve yaşatmak vazifesi Türk gençliğine emanet edilmiştir.

EY TÜRK GENÇLİĞİ!
Birinci vazifen; Türk istiklalini, Türk cumhuriyetini, ilelebet muhafaza ve müdafaa etmektir.
Mevcudiyetinin ve istikbalinin yegâne temeli budur. Bu temel, senin en kıymetli hazinendir.
Muhtaç olduğun kudret, damarlarındaki asil kanda mevcuttur!
            """.trimIndent()
        ),
        OpenBookItem(
            id = "curated-suc-ve-ceza",
            title = "Suç ve Ceza",
            author = "Fyodor Dostoyevski",
            coverUrl = "https://covers.openlibrary.org/b/id/12818862-L.jpg",
            category = "Dünya Klasikleri",
            language = "Türkçe",
            format = "EPUB",
            source = "Project Gutenberg",
            description = "Yoksul bir hukuk öğrencisi olan Raskolnikov'un ahlaki çatışmaları, vicdan azabı ve kurtuluş arayışını anlatan psikolojik başyapıt.",
            downloadUrl = "https://www.gutenberg.org/files/2554/2554-0.txt",
            estimatedPages = 620,
            downloadCount = 89400,
            directContent = """
BÖLÜM I

Temmuz başlarında, son derece sıcak bir günün akşamüzeri, S. Sokağı'ndaki bir pansiyonda çatı katında küçük bir oda tutmuş olan genç bir adam, sokağa çıktı ve yavaş, kararsız adımlarla K. Köprüsü'ne doğru yürümeye başladı.

Merdivenlerde ev sahibesiyle karşılaşmaktan başarıyla kurtulmuştu. Odası tam çatı katının altındaydı; bir odadan çok dolaba benziyordu. Ev sahibesi ise ona hem odayı, hem de yemeği sağlıyordu. Genç adam her dışarı çıkışında onun kapısının önünden geçmek zorundaydı ve bunu her yapışında içinde hastalıklı, korkakça bir duygu uyanıyordu.

"Neden böyle bir şeye kalkışıyorum sanki?" diye düşündü garip bir gülümsemeyle. "Böylesine korkunç bir şeyi düşünmek bile delilik değil mi?"

 Petersburg'un o meşhur boğucu sıcağı, toz duman, her köşede yükselen kireç kokusu ve durmaksızın çalışan işçilerin gürültüsü, zaten zayıf düşmüş olan sinirlerini daha da geriyordu.
            """.trimIndent()
        ),
        OpenBookItem(
            id = "curated-kucuk-prens",
            title = "Küçük Prens",
            author = "Antoine de Saint-Exupéry",
            coverUrl = "https://covers.openlibrary.org/b/id/8225261-L.jpg",
            category = "Çocuk & Felsefe",
            language = "Türkçe",
            format = "EPUB",
            source = "Internet Archive",
            description = "B612 asteroidinden Dünya'ya gelen Küçük Prens'in sevgi, dostluk, sorumluluk ve insan doğası üzerine eşsiz felsefi masalı.",
            downloadUrl = "https://archive.org/download/the-little-prince/little_prince.txt",
            estimatedPages = 96,
            downloadCount = 145000,
            directContent = """
BÖLÜM 1

Altı yaşımdayken, balta girmemiş ormanlar üzerine yazılmış bir kitapta harika bir resim görmüştüm. Bir yılanın bir avı bütün olarak yutuşunu anlatıyordu.

Kitapta şöyle deniyordu: "Boğa yılanları avlarını çiğnemeden bütünüyle yutarlar. Sonra da hareket edemez, sindirim için gereken altı ayı uyuyarak geçirirler."

Bunun üzerine ben de renkli boya kalemlerimle ilk resmimi çizdim: Birinci Çizimim.
Büyük insanlara resmimi gösterip korkup korkmadıklarını sordum.
"Neden bir şapkadan korkalım ki?" dediler.

Oysa resmim bir şapka değildi; bir fili yutmuş olan bir boğa yılanıydı! Büyükler hep açıklama isterler. Ben de ikinci resmimi çizdim: Yılanın içini çizdim ki anlayabilsinler.

Büyükler bana resim yapmayı bırakıp coğrafya, tarih, aritmetik ve dil bilgisiyle ilgilenmemi tavsiye ettiler. Böylece altı yaşımda parlak ressamlık kariyerimden vazgeçtim.
            """.trimIndent()
        ),
        OpenBookItem(
            id = "curated-donusum",
            title = "Dönüşüm",
            author = "Franz Kafka",
            coverUrl = "https://covers.openlibrary.org/b/id/8741369-L.jpg",
            category = "Dünya Klasikleri",
            language = "Türkçe",
            format = "EPUB",
            source = "Project Gutenberg",
            description = "Kumaş pazarlamacısı Gregor Samsa'nın bir sabah kendini dev bir böceğe dönüşmüş olarak bulmasıyla başlayan varoluşçu klasik.",
            downloadUrl = "https://www.gutenberg.org/files/5200/5200-0.txt",
            estimatedPages = 85,
            downloadCount = 76200,
            directContent = """
I

Gregor Samsa bir sabah bunaltıcı düşlerden uyandığında, kendini yatağında devasa bir böceğe dönüşmüş olarak buldu.

Zırh gibi sertleşmiş sırtının üzerinde yatıyordu ve başını biraz kaldırdığında, üzerinde yorganın zorlukla tutunabildiği, kubbeleşmiş, kahverengi ve yay gibi kıvrılan dilimlerle bölünmüş karnını gördü. Gövdesinin boyutlarıyla karşılaştırıldığında acınacak kadar cılız pek çok bacağı, gözlerinin önünde çaresizce titreşip duruyordu.

"Bana ne olmuş böyle?" diye düşündü. Bu bir rüya değildi.

Odanın dört tanıdık duvarı arasındaki sakin ortamı koruyan pencereye baktı. Dışarıda yağmur damlalarının çinko pervaza vurduğu duyuluyordu ve bu ses onu son derece kederlendirdi.
"Biraz daha uyusam ve bütün bu saçmalıkları unutsam nasıl olur?" diye geçirdi aklından.
            """.trimIndent()
        ),
        OpenBookItem(
            id = "curated-satranc",
            title = "Satranç",
            author = "Stefan Zweig",
            coverUrl = "https://covers.openlibrary.org/b/id/8235118-L.jpg",
            category = "Psikolojik Roman",
            language = "Türkçe",
            format = "EPUB",
            source = "Internet Archive",
            description = "Gestapo tarafından tecrit edilen Dr. B'nin zihninde kurduğu satranç dünyası ve New York'tan Buenos Aires'e giden bir yolcu gemisindeki şampiyonla karşılaşması.",
            downloadUrl = "https://archive.org/download/satranc-stefan-zweig/satranc.txt",
            estimatedPages = 110,
            downloadCount = 67300,
            directContent = """
SATRANÇ

Gece yarısı New York'tan Buenos Aires'e hareket edecek olan büyük yolcu gemisinde son dakikaların olağan telaşı hüküm sürüyordu. Karadakiler dostlarına el sallıyor, bavul taşıyıcıları koşuşturuyor, gemi düdüğü tiz seslerle ayrılık vaktini haber veriyordu.

Tam o sırada yanımdaki arkadaşım şaşkınlıkla fısıldadı: "Baksana, Mirko Czentovic de burada!"

Dünya satranç şampiyonu Czentovic, Arjantin'deki turnuvalara katılmak üzere gemideydi. Bu şöhretli ustayı yakından izleme fikri beni hemen cezbetti. Fakat onunla bir parti satranç oynayabilmek için güvertedeki diğer meraklılarla bir plan yapmamız gerekiyordu.
            """.trimIndent()
        ),
        OpenBookItem(
            id = "curated-yeralti",
            title = "Yeraltından Notlar",
            author = "Fyodor Dostoyevski",
            coverUrl = "https://covers.openlibrary.org/b/id/8235650-L.jpg",
            category = "Felsefe & Roman",
            language = "Türkçe",
            format = "EPUB",
            source = "Project Gutenberg",
            description = "İnsan bilincinin derinliklerine inen, modern insanın yabancılaşmasını ve varoluş sancılarını anlatan sarsıcı bir monolog.",
            downloadUrl = "https://www.gutenberg.org/files/600/600-0.txt",
            estimatedPages = 175,
            downloadCount = 52100,
            directContent = """
BİRİNCİ BÖLÜM: YERALTI

Ben hasta bir adamım... Huysuz bir adamım ben. Çekici bir yanım yok hiç. Sanırım karaciğerimden hastayım. Ama hastalığımdan zerre kadar anladığım yok, neresinin ağrıdığını da tam bilmiyorum.

Tıbba ve hekimlere saygım vardır ama tedavi olmuyorum ve hiç de olmadım. Batıl inançlarım yüzünden değil; sırf inadımdan tedavi olmuyorum. Siz bunu anlayamazsınız tabii. Ben anlıyorum.

 Petersburg'da kırk yıldır yaşıyorum. Yirmi yıldır memurluk yaptım, sonra miras kaldı ve emekli oldum. Şimdi bir köşeye çekildim, kendi kendimi didikliyorum.
            """.trimIndent()
        ),
        OpenBookItem(
            id = "curated-sherlock",
            title = "Sherlock Holmes: Kızıl Soruşturma",
            author = "Arthur Conan Doyle",
            coverUrl = "https://covers.openlibrary.org/b/id/8739161-L.jpg",
            category = "Polisiye & Macera",
            language = "Türkçe",
            format = "EPUB",
            source = "Project Gutenberg",
            description = "Dr. John Watson'ın dahi dedektif Sherlock Holmes ile tanışması ve Baker Sokağı 221B'de başlayan ilk gizemli cinayet vakası.",
            downloadUrl = "https://www.gutenberg.org/files/244/244-0.txt",
            estimatedPages = 210,
            downloadCount = 98400,
            directContent = """
BÖLÜM 1: BAY SHERLOCK HOLMES

1878 yılında Londra Üniversitesi'nden Tıp Doktoru unvanını aldıktan sonra, Netley'e giderek ordu cerrahları için öngörülen kursu tamamladım. Buradaki çalışmalarımı bitirince Beşinci Northumberland Piyade Alayı'na cerrah yardımcısı olarak atandım.

İkinci Afgan Savaşı patlak vermişti. Hindistan'a vardığımda alayımın cepheye hareket ettiğini öğrendim. Kandahar Muharebesi'nde bir kurşunla omzumdan yaralandım.

Uzun bir nekahat döneminden sonra hükümet bana günlük dokuz şilinlik maaş bağladı ve İngiltere'ye gönderdi. Londra'da ne bir akrabam ne de bir tanıdığım vardı. Harcamalarımı kısmak için bir ev arkadaşı aramaya başladım. İşte tam o günlerde eski dostum Stamford ile karşılaştım. Beni Baker Sokağı'ndaki bir kimya laboratuvarına, Sherlock Holmes isimli garip bir genç adamla tanıştırmaya götürdü...
            """.trimIndent()
        ),
        OpenBookItem(
            id = "curated-sokrates",
            title = "Sokrates'in Savunması",
            author = "Platon",
            coverUrl = "https://covers.openlibrary.org/b/id/8091724-L.jpg",
            category = "Felsefe",
            language = "Türkçe",
            format = "EPUB",
            source = "Internet Archive",
            description = "Antik Yunan filozofu Sokrates'in Atina Mahkemesi önünde adaleti, bilgeliği ve sorgulanmış bir yaşamı savunduğu ölümsüz diyalog.",
            downloadUrl = "https://www.gutenberg.org/files/1656/1656-0.txt",
            estimatedPages = 90,
            downloadCount = 43100,
            directContent = """
SOKRATES'İN SAVUNMASI

Atinalılar! Beni suçlayanların üzerinizde nasıl bir etki bıraktığını bilemem; ama öyle inandırıcı konuştular ki, az kalsın ben bile kendimi unutuyordum! Oysa söylediklerinin tek bir kelimesi bile doğru değildir.

En çok şaşırdığım şey, benim usta bir hatip olduğumu ve bu yüzden beni dinlerken dikkatli olmanız gerektiğini söylemeleri oldu. Çünkü ben doğruyu konuşmaktan başka hiçbir hitabet sanatı bilmem.

Sizden tek bir ricam var: Beni çarşıda, pazarda dinlediğiniz o sade dille konuştuğum zaman şaşırmayın ve sözümü kesmeyin. Yetmiş yaşımı aştım ve ömrümde ilk defa bir mahkeme huzuruna çıkıyorum. Bu yüzden buranın diline yabancıyım. Sadece gerçeği söylememe izin veriniz.

"Sorgulanmamış bir hayat, yaşanmaya değmez."
            """.trimIndent()
        ),
        OpenBookItem(
            id = "curated-1984",
            title = "1984",
            author = "George Orwell",
            coverUrl = "https://covers.openlibrary.org/b/id/8575742-L.jpg",
            category = "Distopya & Edebiyat",
            language = "Türkçe",
            format = "EPUB",
            source = "Internet Archive",
            description = "Büyük Birader'in gözetimi altındaki Okyanusya'da Winston Smith'in hakikat ve özgürlük arayışını anlatan sarsıcı distopik başyapıt.",
            downloadUrl = "https://archive.org/download/orwell1984/1984.txt",
            estimatedPages = 350,
            downloadCount = 112000,
            directContent = """
BÖLÜM 1

Nisan ayında soğuk ve berrak bir gündü, saatler on üçü vuruyordu. Winston Smith, dondurucu rüzgârdan kaçmak için çenesini göğsüne gömmüş halde Zafer Konutları'nın cam kapılarından hızla içeri süzüldü; yine de beraberinde bir toz girdabının içeri dolmasını engelleyemedi.

Koridor kaynamış lahana ve eski paçavra kokuyordu. Dip taraftaki duvara, iç mekân için fazla büyük olan renkli bir afiş asılmıştı. Afişte kırk beş yaşlarında, sert bakışlı, koyu bıyıklı, yakışıklı bir adamın kocaman yüzü görünüyordu. Winston merdivenlere yöneldi; asansörü denemenin faydası yoktu, çünkü elektrik tasarrufu kapsamında gündüzleri akım kesiliyordu.

Her sahanlıkta, asansör şaftının karşısındaki duvardan devasa yüz bakıyordu. Resim öyle ustalıkla çizilmişti ki, gözler siz hareket ettikçe sizi izliyordu. Altındaki yazıda şöyle diyordu:

BÜYÜK BİRADER'İN GÖZÜ ÜZERİNDE.
            """.trimIndent()
        ),
        OpenBookItem(
            id = "curated-kurk-mantolu",
            title = "Kürk Mantolu Madonna",
            author = "Sabahattin Ali",
            coverUrl = "https://covers.openlibrary.org/b/id/8237190-L.jpg",
            category = "Türk Edebiyatı",
            language = "Türkçe",
            format = "EPUB",
            source = "Açık Arşiv",
            description = "Raif Efendi'nin iç dünyası, Berlin'deki resim sergisinde Maria Puder ile karşılaşması ve unutulmaz bir aşkın hikayesi.",
            downloadUrl = "https://archive.org/download/kurk-mantolu-madonna/kurk_mantolu_madonna.txt",
            estimatedPages = 180,
            downloadCount = 135000,
            directContent = """
GİRİŞ

Şimdiye kadar tesadüf ettiğim insanlardan bir tanesi benim üzerimde belki en büyük tesiri yapmıştır. Aradan seneler geçtiği halde bu tesir zerre kadar azalmadı.

Bu adam Raif Efendi idi. Onu ilk gördüğüm zaman Ankara'da bir şirkette memurdum. Sessiz, sakin, kimsenin etlisine sütlüsüne karışmayan, kendi halinde bir adamdı. Masasında oturur, Almancadan tercümeler yapar, işine bakar giderdi.

Kimse onun içinde kopan fırtınalardan haberdar değildi. Ta ki hastalanıp vefat ettiği günlerde bana emanet ettiği siyah kaplı defteri okuyana kadar... O defterde Berlin günleri, resim galerisi ve Kürk Mantolu Madonna'nın öyküsü yazılıydı.
            """.trimIndent()
        ),
        OpenBookItem(
            id = "curated-beyaz-dis",
            title = "Beyaz Diş",
            author = "Jack London",
            coverUrl = "https://covers.openlibrary.org/b/id/8268800-L.jpg",
            category = "Macera Klasikleri",
            language = "Türkçe",
            format = "EPUB",
            source = "Project Gutenberg",
            description = "Kuzeyin vahşi doğasında doğup büyüyen melez bir kurt köpeğinin vahşetten insan sevgisine uzanan epik serüveni.",
            downloadUrl = "https://www.gutenberg.org/files/910/910-0.txt",
            estimatedPages = 260,
            downloadCount = 61200,
            directContent = """
BÖLÜM 1: İZLER VE SESLER

Karanlık çam ormanının iki yakasında uzanan donmuş nehir boyunca iki adam bir köpek kızağını çekiyordu. Güneşsiz, kasvetli bir gökyüzü altındaki bu ıssız beyazlıkta çıt çıkmıyordu.

Burası Vahşet'ti; Kuzey'in acımasız, sessiz ve dondurucu yüreğiydi. Kızaklarında dar, uzun tahta bir kutu vardı. Kutunun içinde ise son yolculuğuna çıkan bir soylunun cansız bedeni uzanıyordu.

Akşam çökerken arkalarından gelen ince, tiz bir kurt uluması havayı yardı. Ardından sağdan ve soldan başka ulumalar cevap verdi. Aç kurt sürüsü onların izindeydi ve gece çok uzun olacaktı...
            """.trimIndent()
        ),
        OpenBookItem(
            id = "curated-martin-eden",
            title = "Martin Eden",
            author = "Jack London",
            coverUrl = "https://covers.openlibrary.org/b/id/8231450-L.jpg",
            category = "Dünya Edebiyatı",
            language = "Türkçe",
            format = "EPUB",
            source = "Project Gutenberg",
            description = "İşçi sınıfından bir denizcinin bir burjuva kızına olan aşkı uğruna okuma, yazma ve entelektüel zirveye tırmanma mücadelesi.",
            downloadUrl = "https://www.gutenberg.org/files/1056/1056-0.txt",
            estimatedPages = 440,
            downloadCount = 47800,
            directContent = """
BÖLÜM I

Kapıyı açan genç adam, arkasından gelen misafiri içeri buyur etti. Misafir, kaba işçi ceketini çıkarmaya çekinerek ve şapkasını elleri arasında bükerek odaya adım attı.

Denizci adımlarıyla, sanki gemi güvertesinde dalgalara karşı dengede durmaya çalışıyormuş gibi sallanarak yürüyordu. Geniş omuzları, kaslı kolları ve güneşte yanmış yüzüyle bu lüks salonda kendini alabildiğine yabancı hissediyordu.

Tam o sırada kapı açıldı ve Ruth içeri girdi. Martin hayatında hiç bu kadar zarif, tertemiz ve ışık saçan bir varlık görmemişti. O an, bu kızın dünyasına layık olabilmek için ne pahasına olursa olsun okuyacağına ve yazacağına ant içti.
            """.trimIndent()
        )
    )

    // Live search against Open Library, Gutendex (Gutenberg API), StandardEbooks & Wikisource
    suspend fun searchOpenBooks(query: String, langCode: String = "tr"): List<OpenBookItem> {
        return BookAggregator.searchAll(query, langCode)
    }

    // Download content from open source URL
    suspend fun downloadBookText(downloadUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Readover-Reader/1.0 (Android Open Source)")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val rawBody = response.body?.string()
                    if (!rawBody.isNullOrBlank()) {
                        return@withContext cleanDownloadedText(rawBody)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download book text from $downloadUrl: ${e.message}")
        }
        null
    }

    private fun cleanDownloadedText(raw: String): String {
        // Strip out Gutenberg or Archive legal header & footer wrappers if present
        var text = raw
        if (text.contains("*** START OF THE PROJECT GUTENBERG")) {
            text = text.substringAfter("*** START OF THE PROJECT GUTENBERG")
            text = text.substringAfter("***")
        } else if (text.contains("*** START OF THIS PROJECT GUTENBERG")) {
            text = text.substringAfter("*** START OF THIS PROJECT GUTENBERG")
            text = text.substringAfter("***")
        }

        if (text.contains("*** END OF THE PROJECT GUTENBERG")) {
            text = text.substringBefore("*** END OF THE PROJECT GUTENBERG")
        } else if (text.contains("*** END OF THIS PROJECT GUTENBERG")) {
            text = text.substringBefore("*** END OF THIS PROJECT GUTENBERG")
        }

        // Remove html tags if any
        if (text.contains("<html") || text.contains("<body") || text.contains("<p>")) {
            text = text.replace(Regex("<[^>]*>"), " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
        }

        return text.trim()
    }
}
