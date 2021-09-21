import org.jsoup.Jsoup
import java.io.File
import java.util.concurrent.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class Crawler(private val protocol: String, private val domain: String, private val onlyMainDomain: Boolean) {

    private val lists = ConcurrentHashMap<String, List<String>>()

    private val queue = Channel<String>(Channel.Factory.UNLIMITED)
    private val used = ConcurrentHashMap<String, Boolean>()

    private suspend fun tryAdd(inputUrl: String) {
        val url = fixUrl(inputUrl.split("#")[0])
        if (((!onlyMainDomain && url.contains(Regex("^$protocol://[^/]*$domain")))
                    || (onlyMainDomain && url.contains(Regex("^$protocol://$domain"))))
            && !url.contains("#") && used[url] != true
        ) {
            used[url] = true
            queue.send(url)
        }
    }

    private fun fixUrl(url: String): String {
        return url
            .replace(Regex("^.*://"), "$protocol://")
            .replace(Regex("[^:/]/+")) { p -> p.value[0] + "/" }
            .replace(Regex("^$protocol://www[.]"), "$protocol://")
    }

    suspend fun run(numPermits: Int) {
        var i = 0
        used.clear()
        lists.clear()

        val startUrl = fixUrl("$protocol://$domain")
        used[startUrl] = true
        queue.send(startUrl)

        val permits = Semaphore(numPermits)

        while (!queue.isEmpty || permits.availablePermits() < numPermits) {
            val maybeUrl = queue.tryReceive()
            val currentUrl = maybeUrl.getOrNull()
            if (!maybeUrl.isSuccess || currentUrl == null) {
                continue
            }
            permits.acquire()
            GlobalScope.launch {
                try {
                    val list = mutableListOf<String>()
                    println("$i $currentUrl")
                    i++
                    val html = Jsoup.connect(currentUrl).ignoreHttpErrors(true).get()
                    for (a in html.select("a")) {
                        val absRef = fixUrl(a.absUrl("href"))
                        list.add(absRef)
                        tryAdd(absRef)
                    }

                    lists[currentUrl] = list
                } catch (e: Exception) {
                    println(e)
                } finally {
                    permits.release()
                }
            }
        }
    }

    fun writeToFile(fileName: String, sorted: Boolean = false) {
        File(fileName).printWriter().use { out ->
            for (list in if (sorted) lists.toSortedMap() else lists) {
                out.print(list.key + " :")
                for (ref in list.value) {
                    out.print(" $ref")
                }
                out.println()
            }
        }

    }
}

fun main() {
    val crawler = Crawler("https", "jetbrains.com", true)
    runBlocking { crawler.run(256) }
    println("Start writing to file")
    crawler.writeToFile("output.txt", true)
}