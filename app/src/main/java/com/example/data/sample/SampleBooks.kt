package com.example.data.sample

import com.example.data.model.BookEntity

object SampleBooks {
    fun getInitialBooks(): List<BookEntity> {
        return listOf(
            BookEntity(
                id = 1,
                title = "Küçük Prens",
                author = "Antoine de Saint-Exupéry",
                format = "EPUB",
                category = "Dünya Klasiği",
                totalPages = 12,
                currentPage = 3,
                progressPercent = 0.25f,
                lastReadTimestamp = System.currentTimeMillis() - 1000 * 60 * 30, // 30 mins ago
                isFavorite = true,
                fileSizeBytes = 1024 * 420,
                coverColorHex = 0xFF0D9488,
                coverImageUrl = "https://covers.openlibrary.org/b/id/8225261-L.jpg",
                content = """
=== BÖLÜM 1: BİRİNCİ ÇİZİM ===
Altı yaşındayken, balta girmemiş ormanları anlatan "Gerçek Hikâyeler" adlı bir kitapta müthiş bir resim görmüştüm. Bir avını yutan koca bir boğa yılanını gösteriyordu. Resmin kopyası işte yukarıda duruyordu.

Kitapta şöyle deniyordu: "Boğa yılanları avlarını çiğnemeden, bir bütün olarak yutarlar. Ondan sonra da kıpırdayamazlar ve altı ay süren bir sindirim uykusuna yatarlar."

Bu orman maceraları üzerinde uzun uzun düşündüm ve sonra renkli bir kurşun kalemle ilk resmimi yapmayı başardım. 1 Nolu Resmimdir bu. Şöyle bir şeydi.

Şaheserimi büyüklere gösterdim ve resmin onları korkutup korkutmadığını sordum.

Bana şöyle cevap verdiler: "Bir şapkadan niçin korkulsun ki?"

Oysa benim resmim bir şapka değildi. Bir fili sindirmekte olan bir boğa yılanıydı. Bu kez büyüklerin anlayabilmesi için boğa yılanının içini çizdim. Büyüklere hep bir şeyleri açıklamak gerekir zaten. 2 Nolu Resmim de işte böyleydi.

=== BÖLÜM 2: ÇÖLDEKİ KARŞILAŞMA ===
Böylece altı yaşımdayken harika bir ressamlık kariyerini terk etmek zorunda kaldım. 1 ve 2 Nolu Resimlerimin başarısızlığı hevesimi kırmıştı. Büyükler hiçbir şeyi kendi başlarına anlayamıyorlardı, çocukların da onlara her şeyi tekrar tekrar açıklamaktan canları çıkıyordu.

Ben de başka bir meslek seçtim ve uçak kullanmayı öğrendim. Dünyanın neredeyse her yerine uçtum. Ve coğrafya bilgimin bana gerçekten çok faydası oldu. Bir bakışta Çin ile Arizona'yı birbirinden ayırabiliyordum. Geceleyin yolunu şaşıranlar için bu çok faydalı bir bilgidir.

Böylece ömrüm boyunca pek çok ciddi insanla pek çok temasım oldu. Uzun süre büyüklerin arasında yaşadım. Onları çok yakından gördüm. Ama bu, onlar hakkındaki fikrimi pek değiştirmedi.

Derken, altı yıl önce Sahra Çölü'nde bir arıza yaşadım. Motorumda bir şeyler kırılmıştı. Yanımda ne bir makinist ne de bir yolcu olduğundan, bu zor onarım işini tek başıma halletmeye koyuldum. Benim için bir ölüm kalım meselesiydi bu; ancak bir haftalık içme suyum kalmıştı.

İlk gece, en yakın yerleşim yerinden bin mil uzakta, kumların üzerinde uyuyakaldım. Bir gemi kazasından kurtulup okyanusun ortasında bir sal üzerinde sürüklenen birinden bile daha yapayalnızdım.

Gündoğumunda beni uyandıran tuhaf, incecik bir ses duyduğumda ne kadar şaşırdığımı tahmin edebilirsiniz:
— Lütfen... bana bir koyun çizer misiniz?
— Ne?!
— Bana bir koyun çiz...

=== BÖLÜM 3: GÜL VE GEZEGEN ===
Gözlerimi ovuşturarak ayağa fırladım. Çevreme baktım. Ve beni ciddiyetle süzen, son derece olağanüstü küçük bir çocuk gördüm.

Nereden geldiğini öğrenmem uzun zaman aldı. Küçük Prens bana sorular sorup duruyor, ama benim sorduklarımı hiç duymuyor gibiydi. Rastgele söylediği sözlerden her şeyi yavaş yavaş anladım.

Örneğin, uçağımı ilk kez gördüğünde bana sordu:
— Bu nesne de neyin nesi?
— O bir nesne değil. Uçar o. Uçaktır. Benim uçağım.
Uçabildiğimi ona gururla bildirmiştim. O zaman bağırdı:
— Nasıl! Gökten mi düştün sen?
— Evet, dedim alçakgönüllülükle.
— Ah! Bu çok komik işte!

Küçük Prens öyle hoş bir kahkaha attı ki, canım fena halde sıkıldı. Başıma gelen talihsizliklerin ciddiye alınmasını isterim çünkü. Sonra ekledi:
— Demek sen de gökten geliyorsun! Hangi gezegendensin?

O anda varlığının gizemine dair bir ışık parıldadı ve birdenbire sordum:
— Sen başka bir gezegenden mi geldin yoksa?
Ama cevap vermedi. Uçağıma bakarak yavaşça başını salladı:
— Gerçi bununla pek uzaktan gelmiş olamazsın...

=== BÖLÜM 4: BAOBABLAR VE SORUMLULUK ===
Küçük Prens'in gezegeni hakkında her gün yeni bir şey öğreniyordum; gezegeninden ayrılışı, yolculuğu hakkında... Bu bilgiler konuşmalar sırasında yavaş yavaş ortaya çıkıyordu. Üçüncü gün baobab ağaçlarının dramını da böyle öğrendim.

Bu kez de koyun sayesinde oldu; çünkü Küçük Prens sanki derin bir şüpheye kapılmış gibi birdenbire sordu bana:
— Koyunların çalıları yediği doğru, değil mi?
— Evet, doğru.
— Ah! Çok sevindim!

Koyunların küçük çalıları yemesinin neden bu kadar önemli olduğunu anlayamamıştım. Ama Küçük Prens ekledi:
— Öyleyse baobabları da yerler mi?

Ona baobabların küçük çalılar değil, kilise büyüklüğünde dev ağaçlar olduğunu ve yanına bir sürü fil alsa bile bir tek baobabı tüketemeyeceklerini anlattım.

Filler lafı Küçük Prens'i güldürdü:
— Onları üst üste koymak gerekirdi o zaman!
Ama bilgece bir tespitte bulundu:
— Baobablar da büyümeden önce küçüktürler.

=== BÖLÜM 5: TİLKİ VE EVCİLLEŞTİRMEK ===
İşte o sırada tilki ortaya çıktı.
— Günaydın, dedi tilki.
— Günaydın, diye kibarca karşılık verdi Küçük Prens, ama arkasını döndüğünde kimseyi göremedi.
— Buradayım, dedi ses, elma ağacının altında...
— Kimsin sen? dedi Küçük Prens. Çok güzelsin...
— Ben bir tilkiyim, dedi tilki.
— Gel benimle oyna, diye teklif etti Küçük Prens. O kadar üzgünüm ki...
— Seninle oynayamam, dedi tilki. Ben evcil değilim.
— Ah! Özür dilerim, dedi Küçük Prens.
Ama biraz düşündükten sonra ekledi:
— "Evcilleştirmek" ne demek?
— Çok unutulmuş bir şey bu, dedi tilki. "Bağlar kurmak" anlamına gelir.
— Bağlar kurmak mı?
— Evet, dedi tilki. "Sen benim için henüz yüz binlerce başka küçük erkek çocuk gibi bir çocuksun yalnızca. Ve benim sana ihtiyacım yok. Senin de bana ihtiyacın yok. Ben de senin için yüz binlerce tilkiden biriyim. Ama beni evcilleştirirsen, birbirimize ihtiyacımız olacak. Sen benim için dünyada tek olacaksın. Ben de senin için dünyada tek olacağım..."
                """.trimIndent()
            ),
            BookEntity(
                id = 2,
                title = "Sherlock Holmes - Kızıl Soruşturma",
                author = "Sir Arthur Conan Doyle",
                format = "EPUB",
                category = "Polisiye",
                totalPages = 18,
                currentPage = 1,
                progressPercent = 0.05f,
                lastReadTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 5,
                isFavorite = true,
                fileSizeBytes = 1024 * 680,
                coverColorHex = 0xFF991B1B,
                coverImageUrl = "https://covers.openlibrary.org/b/id/8739161-L.jpg",
                content = """
=== BÖLÜM 1: BAY SHERLOCK HOLMES ===
1878 yılında Londra Üniversitesi'nden tıp doktoru unvanını aldıktan sonra Netley'e gidip ordu cerrahları için öngörülen eğitim kursuna katıldım.

Oradaki çalışmalarımı bitirince 5. Northumberland Piyade Alayı'na cerrah yardımcısı olarak atandım.

Alay o sırada Hindistan'da konuşlanmıştı. Katılma fırsatı bulamadan İkinci Afgan Savaşı patlak verdi. Bombay'a vardığımda birliğimin düşman hatlarını yarıp dağ geçitlerinden ilerlemiş olduğunu öğrendim.

Kandahar'a kadar peşlerinden gittim; orada alayımı bulup hemen yeni görevime başladım.

Bu sefer pek çok kimseye şeref ve terfi getirdi, fakat bana talihsizlik ve felaketten başka bir şey getirmedi. Tugayımdan alınıp Berkshire birliğine bağlandım ve bu birlikle birlikte Maiwand'daki o feci muharebeye katıldım.

Orada omzumdan bir Jezail mermisiyle vuruldum; mermi kemiği parçalayıp subklavian arteri sıyırdı.

Sadık seyisim Murray'in bağlılığı ve cesareti olmasaydı kana susamış Gazilerin ellerine düşecektim. Beni bir yük atının sırtına atıp İngiliz hatlarına sağ salim ulaştırmayı başardı.

Aylar süren acı ve nekahetten sonra nihayet Londra'ya döndüm. Cebimde günde on bir şilin ve altı penilik yarım maaşımla, Büyük Britanya'nın o devasa girdabında amaçsızca sürükleniyordum.

İşte tam bu sırada eski bir tanıdığım olan Stamford ile Strand'deki Criterion Bar'da karşılaştım.

Ona kalacak uygun fiyatlı bir oda aradığımı söylediğimde bana güldü:
— Bugün bu lafı eden ikinci kişisin, dedi.
— İlki kimdi?
— Kimya laboratuvarında çalışan bir adam. Adı Sherlock Holmes.
                """.trimIndent()
            ),
            BookEntity(
                id = 3,
                title = "Dönüşüm",
                author = "Franz Kafka",
                format = "PDF",
                category = "Klasik Edebiyat",
                totalPages = 8,
                currentPage = 1,
                progressPercent = 0.12f,
                lastReadTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24,
                isFavorite = false,
                fileSizeBytes = 1024 * 310,
                coverColorHex = 0xFF4338CA,
                coverImageUrl = "https://covers.openlibrary.org/b/id/8741369-L.jpg",
                content = """
=== BÖLÜM 1: BÖCEKLEŞME ===
Gregor Samsa bir sabah bunaltıcı düşlerden uyandığında, kendini yatağında devasa bir böceğe dönüşmüş olarak buldu.

Zırh gibi sertleşmiş sırtının üzerinde yatıyordu ve başını biraz kaldırdığında, yay biçimindeki sert çizgilerle bölünmüş kahverengi, kubbe gibi bir karın görüyordu; yatak yorganı bu karnın tepesinde neredeyse duramayacak hale gelmiş, her an tamamen kayıp düşecek gibi görünüyordu.

Gövdesinin geri kalanına oranla acınacak derecede cılız olan sayısız bacakları, gözlerinin önünde çaresizce kıpırdaşıp duruyordu.

"Bana ne oldu böyle?" diye düşündü.

Bu bir rüya değildi. İnsanlara özgü, biraz küçük ama yine de derli toplu odası, dört tanıdık duvar arasında sessizce duruyordu.

Masanın üzerinde yayılmış duran kumaş numuneleri paketi -Samsa bir gezgin kumaş satıcısıydı- ve masanın biraz üstünde, kısa süre önce resimli bir dergiden kesip yaldızlı güzel bir çerçeveye yerleştirdiği resim asılıydı.

Resimde kürk bir şapka ve kürk bir etol takmış, dimdik oturan ve seyirciye doğru bütün ön kolunu kaplayan ağır bir kürk manşon uzatan bir hanımefendi görünüyordu.

Gregor'un bakışları pencereye doğru kaydı; dışarıdaki kasvetli hava -yağmur damlalarının çinko pencere pervazına vurduğu duyuluyordu- onu son derece hüzünlendirdi.

"Biraz daha uyusam da bütün bu saçmalıkları unutsam nasıl olur?" diye düşündü, ama bunu gerçekleştirmesi imkânsızdı; çünkü sağ yanına yatmaya alışıktı ve şimdiki durumunda bu konuma geçemiyordu.
                """.trimIndent()
            ),
            BookEntity(
                id = 4,
                title = "Sokrates'in Savunması",
                author = "Platon",
                format = "TXT",
                category = "Felsefe",
                totalPages = 10,
                currentPage = 1,
                progressPercent = 0f,
                lastReadTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 48,
                isFavorite = false,
                fileSizeBytes = 1024 * 180,
                coverColorHex = 0xFF059669,
                coverImageUrl = "https://covers.openlibrary.org/b/id/8091724-L.jpg",
                content = """
=== BÖLÜM 1: GİRİŞ VE SUÇLAMALAR ===
Atinalılar! Beni suçlayanların üzerinizde nasıl bir etki bıraktığını bilemem; ama doğrusu söyledikleri o kadar inandırıcıydı ki, ben bile neredeyse kim olduğumu unutacaktım. Oysa söylediklerinin içinde tek bir doğru söz bile yoktu.

Beni en çok şaşırtan yalanları ise, usta bir hatip olduğum için benim tarafımdan aldatılmaktan sakınmanız gerektiğini söylemeleri oldu.

Çünkü ağzımı açar açmaz hiç de usta bir hatip olmadığım anlaşılacak ve böylece anında yalanları yüzlerine vurulacaktı. Bunu göze almaları bana büyük bir arsızlık gibi geldi; meğerki onlar "usta hatip" derken doğruyu söyleyen kişiyi kastetmiş olsunlar!

Eğer kastettikleri buysa, evet, hatip olduğumu kabul ederim; ama onların anladığı türden bir hatip değil.

Ben bu mahkemede süslü püslü sözler söylemeyeceğim. Yetmiş yaşımı aştım ve ömrümde ilk kez bir mahkeme huzuruna çıkıyorum. Bu yüzden buranın diline yabancıyım.

Sizden tek bir ricam var: Sözlerimin şekline değil, doğru olup olmadığına bakın. Çünkü yargıcın erdemi adalette, hatibin erdemi ise doğruyu söylemektedir.
                """.trimIndent()
            )
        )
    }
}
