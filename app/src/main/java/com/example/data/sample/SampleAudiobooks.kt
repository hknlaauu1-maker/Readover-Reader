package com.example.data.sample

import com.example.data.model.AudiobookEntity

object SampleAudiobooks {
    fun getInitialAudiobooks(): List<AudiobookEntity> {
        return listOf(
            AudiobookEntity(
                id = 1,
                title = "Küçük Prens",
                author = "Antoine de Saint-Exupéry",
                narrator = "Canan Sanan",
                coverImageUrl = "https://covers.openlibrary.org/b/id/8225261-L.jpg",
                coverColorHex = 0xFF0D9488,
                audioSourceType = "SAMPLE",
                // Open public domain LibriVox / Internet Archive audio stream
                audioUriOrUrl = "https://ia800201.us.archive.org/12/items/le_petit_prince_0911_librivox/petitprince_01_saintexupery_64kb.mp3",
                durationMs = 1000 * 60 * 18 + 42 * 1000, // 18m 42s
                currentPositionMs = 1000 * 135, // 2m 15s
                isFavorite = true,
                category = "Dünya Klasiği",
                description = "Küçük Prens'in çöldeki pilotla buluşması, gezegenler arası masalsı ve felsefi yolculuğu.",
                playbackSpeed = 1.0f,
                lastListenedTimestamp = System.currentTimeMillis() - 1000 * 60 * 10
            ),
            AudiobookEntity(
                id = 2,
                title = "Sherlock Holmes - Kızıl Soruşturma",
                author = "Sir Arthur Conan Doyle",
                narrator = "David Clarke",
                coverImageUrl = "https://covers.openlibrary.org/b/id/8739161-L.jpg",
                coverColorHex = 0xFF991B1B,
                audioSourceType = "SAMPLE",
                audioUriOrUrl = "https://ia800302.us.archive.org/1/items/study_in_scarlet_librivox/astudyinscarlet_01_doyle_64kb.mp3",
                durationMs = 1000 * 60 * 24 + 15 * 1000, // 24m 15s
                currentPositionMs = 0L,
                isFavorite = true,
                category = "Polisiye & Gizem",
                description = "Sherlock Holmes ve Dr. John Watson'ın ilk kez Baker Sokağı 221B'de tanıştığı ve ilk cinayet vakasını çözdüğü efsanevi başlangıç.",
                playbackSpeed = 1.0f,
                lastListenedTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2
            ),
            AudiobookEntity(
                id = 3,
                title = "Dönüşüm",
                author = "Franz Kafka",
                narrator = "John Greenman",
                coverImageUrl = "https://covers.openlibrary.org/b/id/8741369-L.jpg",
                coverColorHex = 0xFF4338CA,
                audioSourceType = "SAMPLE",
                audioUriOrUrl = "https://ia800301.us.archive.org/3/items/metamorphosis_librivox/metamorphosis_01_kafka_64kb.mp3",
                durationMs = 1000 * 60 * 32 + 10 * 1000, // 32m 10s
                currentPositionMs = 0L,
                isFavorite = false,
                category = "Felsefi & Psikolojik",
                description = "Gregor Samsa bir sabah bunaltıcı düşlerden uyandığında, kendini yatağında dev bir böceğe dönüşmüş olarak buldu.",
                playbackSpeed = 1.0f,
                lastListenedTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24
            ),
            AudiobookEntity(
                id = 4,
                title = "Jane Eyre",
                author = "Charlotte Brontë",
                narrator = "Elizabeth Klett",
                coverImageUrl = "https://covers.openlibrary.org/b/id/8276707-L.jpg",
                coverColorHex = 0xFF0284C7,
                audioSourceType = "SAMPLE",
                audioUriOrUrl = "https://ia800300.us.archive.org/21/items/jane_eyre_ver03_librivox/janeeyre_01_bronte_64kb.mp3",
                durationMs = 1000 * 60 * 28 + 50 * 1000,
                currentPositionMs = 0L,
                isFavorite = false,
                category = "Klasik Roman",
                description = "Tüm zorluklara rağmen bağımsızlığından ve onurundan ödün vermeyen genç bir kadının unutulmaz hikâyesi.",
                playbackSpeed = 1.0f,
                lastListenedTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 48
            )
        )
    }
}
