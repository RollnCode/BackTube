package com.rollncode.backtube.logic

import android.net.Uri
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TubeUriTest {

    @Test
    fun videoFullLinkTest() {
        linkTest("www.youtube.com/watch?v=Y_EynulWwNM",
                expectedVideoId = "Y_EynulWwNM",
                expectedType = TUBE_VIDEO)

        linkTest("https://youtube.com/watch?v=Y_EynulWwNM",
                expectedVideoId = "Y_EynulWwNM",
                expectedType = TUBE_VIDEO)

        linkTest("https://www.youtube.com/watch?v=Y_EynulWwNM",
                expectedVideoId = "Y_EynulWwNM",
                expectedType = TUBE_VIDEO)

        linkTest("https://www.youtube.com/watch?v=i_JS1YG8H2c&feature=youtu.be&t=16",
                expectedVideoId = "i_JS1YG8H2c",
                expectedTimeReference = 16,
                expectedType = TUBE_VIDEO)

        linkTest("https://www.youtube.com/embed/M7lc1UVf-VE",
                expectedVideoId = "M7lc1UVf-VE",
                expectedType = TUBE_VIDEO)

        linkTest("http://www.youtube.com/attribution_link?a=JdfC0C9V6ZI&u=/watch?v=EhxJLojIE_o&feature=share",
                expectedVideoId = "EhxJLojIE_o",
                expectedType = TUBE_VIDEO)

        linkTest("http://www.youtube.com/")
        linkTest("https://www.google.com/search")
    }

    @Test
    fun videoShortLinkTest() {
        linkTest("https://youtu.be/j4dMnAPZu70",
                expectedVideoId = "j4dMnAPZu70",
                expectedType = TUBE_VIDEO)

        linkTest("https://youtu.be/j4dMnAPZu70?t=13",
                expectedVideoId = "j4dMnAPZu70",
                expectedTimeReference = 13,
                expectedType = TUBE_VIDEO)

        linkTest("https://youtu.be")
    }

    @Test
    fun playlistFullLinkTest() {
        linkTest("https://www.youtube.com/playlist?list=PLOTk2vhh9vYrqI9SOxgVniW8EShjKmi2L",
                expectedPlaylistId = "PLOTk2vhh9vYrqI9SOxgVniW8EShjKmi2L",
                expectedType = TUBE_PLAYLIST)

        linkTest("https://www.youtube.com/playlist")
        linkTest("https://www.youtube.com/watch")
    }

    @Test
    fun playlistWithVideoFullLinkTest() {
        linkTest("http://www.youtube.com/watch?v=QxHkLdQy5f0&list=RDEMP1Th0jUjYvsFq-h2usP4WQ&t=116",
                expectedVideoId = "QxHkLdQy5f0",
                expectedPlaylistId = "RDEMP1Th0jUjYvsFq-h2usP4WQ",
                expectedTimeReference = 116,
                expectedType = TUBE_PLAYLIST)

        linkTest("https://www.youtube.com/watch?t=116")
    }

    @Test
    fun playlistWithVideoShortLinkTest() {
        linkTest("https://youtu.be/P8ymgFyzbDo?list=PLOTk2vhh9vYrqI9SOxgVniW8EShjKmi2L&t=8",
                expectedVideoId = "P8ymgFyzbDo",
                expectedPlaylistId = "PLOTk2vhh9vYrqI9SOxgVniW8EShjKmi2L",
                expectedTimeReference = 8,
                expectedType = TUBE_PLAYLIST)

        linkTest("https://youtu.be/QxHkLdQy5f0?list=RDEMP1Th0jUjYvsFq-h2usP4WQ&t=116",
                expectedVideoId = "QxHkLdQy5f0",
                expectedPlaylistId = "RDEMP1Th0jUjYvsFq-h2usP4WQ",
                expectedTimeReference = 116,
                expectedType = TUBE_PLAYLIST)
    }

    private fun linkTest(link: String,
                         expectedPlaylistId: String = "",
                         expectedVideoId: String = "",
                         expectedTimeReference: Int = 0,
                         expectedType: String = TUBE_IGNORE) = TubeUri(Uri.parse(link)).run {
        System.out.println("link: $link\nentity: ${toString()}\n")

        Assert.assertEquals(expectedPlaylistId, playlistId)
        Assert.assertEquals(expectedVideoId, videoId)
        Assert.assertEquals(expectedTimeReference, timeReference)
        Assert.assertEquals(expectedType, type)
    }
}