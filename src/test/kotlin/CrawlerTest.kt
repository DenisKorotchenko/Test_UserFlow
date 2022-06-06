import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.coroutines.Continuation

internal class CrawlerTest {

    private val crawlerHTTPSVKMainOnly = Crawler("https", "vk.com", true)
    private val crawlerHTTPVK = Crawler("http", "vk.com", false)
    private val crawlerYaRu = Crawler("https", "ya.ru", true)


    @Test
    fun testFixUrlHTTPS() {
        val f = crawlerHTTPSVKMainOnly.javaClass.getDeclaredMethod("fixUrl", String::class.java)//
        f.isAccessible = true

        val data = listOf(
            Pair("http://vk.com", "https://vk.com"),
            Pair("https://www.vk.com/", "https://vk.com"),
            Pair("http://vk.com//rzd_vgzd/", "https://vk.com/rzd_vgzd")
        )

        for (p in data) {
            val parameters = arrayOfNulls<Any>(1)
            parameters[0] = p.first
            val ans = f.invoke(crawlerHTTPSVKMainOnly, *parameters)
            assert(ans == p.second) { "Failed on ${p.first}" }
        }

    }

    @Test
    fun testFixUrlHTTP() {
        val f = crawlerHTTPVK.javaClass.getDeclaredMethod("fixUrl", String::class.java)//
        f.isAccessible = true

        val data = listOf(
            Pair("http://vk.com", "http://vk.com"),
            Pair("https://www.vk.com/", "http://vk.com"),
            Pair("http://vk.com//rzd_vgzd/", "http://vk.com/rzd_vgzd")
        )

        for (p in data) {
            val parameters = arrayOfNulls<Any>(1)
            parameters[0] = p.first
            val ans = f.invoke(crawlerHTTPVK, *parameters)
            assertEquals(p.second, ans) { "Failed on ${p.first}" }
        }

    }

    @Test
    fun tryAddMainOnly() {
        val f = crawlerHTTPSVKMainOnly.javaClass.getDeclaredMethod("tryAdd",
            String::class.java,
            Continuation::class.java
        )
        f.isAccessible = true

        val data = listOf(
            Pair("https://vk.com", true),
            Pair("https://vk.com", false),
            Pair("https://vkvk.com", false),
            Pair("https://vk.com/rzd_vgzd", true)
        )

        for (p in data) {
            val parameters = arrayOfNulls<Any>(2)
            parameters[0] = p.first
            val ans = f.invoke(crawlerHTTPSVKMainOnly, *parameters)
            assertEquals(p.second, ans) { "Failed on ${p.first}" }
        }
    }

    @Test
    fun tryAddNotOnlyMain() {
        val f = crawlerHTTPVK.javaClass.getDeclaredMethod("tryAdd",
            String::class.java,
            Continuation::class.java
        )
        f.isAccessible = true

        val data = listOf(
            Pair("https://vk.com", true),
            Pair("https://vk.com", false),
            Pair("https://vkvk.com", true),
            Pair("https://vk.com/rzd_vgzd", true)
        )

        for (p in data) {
            val parameters = arrayOfNulls<Any>(2)
            parameters[0] = p.first
            val ans = f.invoke(crawlerHTTPVK, *parameters)
            assertEquals(p.second, ans) { "Failed on ${p.first}" }
        }
    }

    @Test
    fun testYaRu() {
        runBlocking {
            crawlerYaRu.run(256, false)
        }

        val fileName = "yaruans.txt"

        crawlerYaRu.writeToFile(fileName, false)
        val lines = File(fileName).readLines()
        assertEquals(1, lines.size)
        val splitted = lines[0].split(" : ")
        assertEquals("https://ya.ru", splitted[0])
        assert(splitted[1].contains("https://yandex.ru"))
        File(fileName).delete()
    }
}