package mServer.crawler.sender.wdr;

import de.mediathekview.mlib.Const;
import de.mediathekview.mlib.daten.DatenFilm;
import java.util.Arrays;
import java.util.Collection;
import mServer.test.TestFileReader;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class WdrVideoDetailsDeserializerTest {
    
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {  
            { "/wdr/wdr_video_details1.html", "Abenteuer Erde", "Die Tricks des Überlebens 3) Im Wald", "Nur auf der Nordhalbkugel gibt es Wälder, deren Leben durch große Veränderungen geprägt wird. Jedes Jahr lässt sich hier ein wundersamer Wechsel beobachten: im Winter sinken die Temperaturen dramatisch und die Wälder werden völlig kahl. Im Frühjahr kehren mit steigenden Temperaturen die grünen Blätter und damit das Leben zurück. Autor/-in: Paul Bradshaw", "http://www1.wdr.de/mediathek/video/sendungen/abenteuer-erde/video-die-tricks-des-ueberlebens--im-wald-102.html", "http://wdradaptiv-vh.akamaihd.net/i/medp/ondemand/weltweit/fsk0/148/1480611/,1480611_16974214,1480611_16974213,1480611_16974215,1480611_16974211,1480611_16974212,.mp4.csmil/index_2_av.m3u8", "179|0_av.m3u8", "", "", "26.09.2017", "20:15:00", "00:43:20", "http://deviceids-medp.wdr.de/ondemand/148/1480611.js", "/wdr/wdr_video1.js", "http://wdradaptiv-vh.akamaihd.net/i/medp/ondemand/weltweit/fsk0/148/1480611/,1480611_16974214,1480611_16974213,1480611_16974215,1480611_16974211,1480611_16974212,.mp4.csmil/master.m3u8", "/wdr/wdr_video1.m3u8" },
            { "/wdr/wdr_video_details2.html", "Ausgerechnet", "Ausgerechnet - Schokolade", "Knapp 25 Prozent der Bevölkerung verzehren Schokolade mehrmals in der Woche. Hinzu kommt gut ein weiteres Viertel, das Schokolade etwa einmal pro Woche genießt. Frauen greifen hier generell häufiger zu als Männer.", "http://www1.wdr.de/mediathek/video/sendungen/ausgerechnet/video-ausgerechnet---schokolade-102.html", "http://wdradaptiv-vh.akamaihd.net/i/medp/ondemand/weltweit/fsk0/140/1407842/,1407842_16309723,1407842_16309728,1407842_16309725,1407842_16309726,1407842_16309724,1407842_16309727,.mp4.csmil/index_2_av.m3u8", "196|0_av.m3u8", "196|4_av.m3u8", "http://wdrmedien-a.akamaihd.net/medp/ondemand/weltweit/fsk0/140/1407842/1407842_16348809.xml", "15.07.2017", "16:00:00", "00:43:35", "http://deviceids-medp.wdr.de/ondemand/140/1407842.js", "/wdr/wdr_video2.js", "http://wdradaptiv-vh.akamaihd.net/i/medp/ondemand/weltweit/fsk0/140/1407842/,1407842_16309723,1407842_16309728,1407842_16309725,1407842_16309726,1407842_16309724,1407842_16309727,.mp4.csmil/master.m3u8", "/wdr/wdr_video2.m3u8" },
        });
    }

    private final String htmlFile;
    private final String expectedTheme;
    private final String expectedTitle;
    private final String expectedDescription;
    private final String expectedWebsite;
    private final String expectedVideoUrlSmall;
    private final String expectedVideoUrlNormal;
    private final String expectedVideoUrlHd;
    private final String expectedSubtitle;
    private final String expectedDate;
    private final String expectedTime;
    private final String expectedDuration;
    
    private final WdrVideoDetailsDeserializer target;
    
    public WdrVideoDetailsDeserializerTest(String aHtmlFile, String aTheme, String aTitle, String aDescription, String aWebsite, String aVideoUrlNormal, String aVideoUrlSmall, String aVideoUrlHd, String aSubtitle, String aDate, String aTime, String aDuration, String aJsUrl, String aJsFile, String aM3u8Url, String aM3u8File) {
        htmlFile = aHtmlFile;
        expectedDate = aDate;
        expectedDescription = aDescription;
        expectedDuration = aDuration;
        expectedTheme = aTheme;
        expectedTime = aTime;
        expectedTitle = aTitle;
        expectedSubtitle = aSubtitle;
        expectedVideoUrlSmall = aVideoUrlSmall;
        expectedVideoUrlNormal = aVideoUrlNormal;
        expectedVideoUrlHd = aVideoUrlHd;
        expectedWebsite = aWebsite;
        
        WdrUrlLoaderMock urlLoader = new WdrUrlLoaderMock();
        urlLoader.setUp(aJsUrl, aJsFile);
        urlLoader.setUp(aM3u8Url, aM3u8File);
        
        target = new WdrVideoDetailsDeserializer(urlLoader.get());
    }
    
    @Test
    public void deserializeTestWithVideo() {
        String html = TestFileReader.readFile(htmlFile);
        Document document = Jsoup.parse(html);
        
        DatenFilm actual = target.deserialize(expectedTheme, document);
        
        assertThat(actual, notNullValue());
        assertThat(actual.arr[DatenFilm.FILM_SENDER], equalTo(Const.WDR));
        assertThat(actual.arr[DatenFilm.FILM_THEMA], equalTo(expectedTheme));
        assertThat(actual.arr[DatenFilm.FILM_TITEL], equalTo(expectedTitle));
        assertThat(actual.arr[DatenFilm.FILM_BESCHREIBUNG], equalTo(expectedDescription));
        assertThat(actual.arr[DatenFilm.FILM_WEBSEITE], equalTo(expectedWebsite));
        assertThat(actual.arr[DatenFilm.FILM_DATUM], equalTo(expectedDate));
        assertThat(actual.arr[DatenFilm.FILM_ZEIT], equalTo(expectedTime));
        assertThat(actual.arr[DatenFilm.FILM_DAUER], equalTo(expectedDuration));
        assertThat(actual.getUrl(), equalTo(expectedVideoUrlNormal));
        assertThat(actual.arr[DatenFilm.FILM_URL_KLEIN], equalTo(expectedVideoUrlSmall));
        assertThat(actual.arr[DatenFilm.FILM_URL_HD], equalTo(expectedVideoUrlHd));
        assertThat(actual.getUrlSubtitle(), equalTo(expectedSubtitle));
    }
}