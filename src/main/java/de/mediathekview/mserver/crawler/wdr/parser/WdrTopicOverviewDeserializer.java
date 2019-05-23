package de.mediathekview.mserver.crawler.wdr.parser;

import de.mediathekview.mserver.base.utils.UrlUtils;
import de.mediathekview.mserver.crawler.wdr.WdrConstants;
import de.mediathekview.mserver.crawler.wdr.WdrTopicUrlDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import static de.mediathekview.mserver.base.HtmlConsts.ATTRIBUTE_HREF;

public class WdrTopicOverviewDeserializer extends WdrLetterPageDeserializerBase {

  private static final String SELECTOR_URL = "div.teaser > a";
  private static final String SELECTOR_URL_LIST =
      "h3.hidden + div.teaser > div.linklist > ul > li > a";
  private static final String SELECTOR_URL_ROCKPALAST_YEARS = "div.entries > div";
  private static final String SELECTOR_URL_ROCKPALAST_YEARS_ENTRIES = "div.entry > a";

  public List<WdrTopicUrlDto> deserialize(final String aTopic, final Document aDocument) {
    final List<WdrTopicUrlDto> results = new ArrayList<>();

    addUrls(results, aTopic, aDocument, SELECTOR_URL, true);
    addUrls(results, aTopic, aDocument, SELECTOR_URL_LIST, true);
    if (isRockpalastOverviewPage(aDocument)) {
      addUrls(results, aTopic, aDocument, SELECTOR_URL_ROCKPALAST_YEARS_ENTRIES, false);
    }

    return results;
  }

  private void addUrls(
      final List<WdrTopicUrlDto> aResults,
      final String aTopic,
      final Document aDocument,
      final String aSelector,
      final boolean defaultIsFileUrl) {
    final Elements urlElements = aDocument.select(aSelector);
    urlElements.forEach(
        urlElement -> {
          String url = urlElement.attr(ATTRIBUTE_HREF);
          if (url != null && !url.isEmpty() && isUrlRelevant(url)) {
            url = UrlUtils.addDomainIfMissing(url, WdrConstants.URL_BASE);
            final boolean isFileUrl = isFileUrl(urlElement, defaultIsFileUrl);

            aResults.add(new WdrTopicUrlDto(aTopic, url, isFileUrl));
          }
        });
  }

  /**
   * * Filtert URLs heraus, die nicht durchsucht werden sollen Hintergrund: diese URLs verweisen auf
   * andere und führen bei der Suche im Rahmen der Rekursion zu endlosen Suchen.
   *
   * @param url zu prüfende URL
   * @return true, wenn die URL verarbeitet werden soll, sonst false
   */
  private boolean isUrlRelevant(final String aUrl) {
    // die Indexseite der Lokalzeit herausfiltern, da alle Beiträge
    // um die Lokalzeitenseiten der entsprechenden Regionen gefunden werden
    return !(aUrl.endsWith("lokalzeit/index.html") || aUrl.contains("wdr.de/hilfe"));
  }

  private boolean isRockpalastOverviewPage(final Document aDocument) {
    // ermitteln, ob es sich um die erste Rockpalastübersichtsseite handelt
    // dazu muss das erste Element in der Jahresauswahl aktiv sein
    final Elements yearElements = aDocument.select(SELECTOR_URL_ROCKPALAST_YEARS);

    if (yearElements != null) {
      final Element firstYearElement = yearElements.first();

      if (firstYearElement != null) {
        return firstYearElement.classNames().contains("active");
      }
    }
    return false;
  }
}
