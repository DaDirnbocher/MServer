package de.mediathekview.mserver.crawler.sr.tasks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.mediathekview.mlib.daten.Film;
import de.mediathekview.mlib.daten.Resolution;
import de.mediathekview.mserver.base.utils.DateUtils;
import de.mediathekview.mserver.base.utils.HtmlDocumentUtils;
import de.mediathekview.mserver.crawler.ard.json.ArdVideoInfoDTO;
import de.mediathekview.mserver.crawler.ard.json.ArdVideoInfoJsonDeserializer;
import de.mediathekview.mserver.crawler.basic.AbstractCrawler;
import de.mediathekview.mserver.crawler.basic.AbstractDocumentTask;
import de.mediathekview.mserver.crawler.basic.AbstractUrlTask;
import de.mediathekview.mserver.crawler.sr.SrTopicUrlDTO;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import mServer.crawler.CrawlerTool;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

public class SrFilmDetailTask extends AbstractDocumentTask<Film, SrTopicUrlDTO> {
  
  private static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(SrFilmDetailTask.class);

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMANY);
  private static final DateTimeFormatter DATE_TIME_FORMATTER_ENG =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.GERMANY);

  private static final String CONTENT_SELECTOR = "div.article__content";
  private static final String TITLE_SELECTOR = CONTENT_SELECTOR + " > div > h3";
  private static final String DETAILS_SELECTOR = CONTENT_SELECTOR + " > div > p";
  private static final String DESCRIPTION_SELECTOR = CONTENT_SELECTOR + " > h1:contains(Themen)";
  private static final String VIDEO_DETAIL_ATTRIBUTE = "data-mediacollection-ardplayer";
  private static final String VIDEO_DETAIL_SELECTOR = "div[" + VIDEO_DETAIL_ATTRIBUTE + "]";
  
  public SrFilmDetailTask(final AbstractCrawler aCrawler,
      final ConcurrentLinkedQueue<SrTopicUrlDTO> aUrlToCrawlDTOs) {
    super(aCrawler, aUrlToCrawlDTOs);
  }

  @Override
  protected void processDocument(SrTopicUrlDTO aUrlDTO, Document aDocument) {
    
    try {
      if (!isRelevantType(aDocument)) {
        return;
      }
      
      final Optional<String> title = HtmlDocumentUtils.getElementString(TITLE_SELECTOR, aDocument);
      final Optional<LocalDateTime> time = parseDate(aDocument);
      final Optional<Duration> duration = parseDuration(aDocument);
      final Optional<String> description = parseDescription(aDocument);
      
      Optional<ArdVideoInfoDTO> videoInfoOptional = parseUrls(aDocument);
      if (videoInfoOptional.isPresent() 
        && title.isPresent()) {
        final Film film = new Film(UUID.randomUUID(), crawler.getSender(), title.get(),
                aUrlDTO.getTheme(), time.orElse(LocalDateTime.now()), duration.orElse(Duration.ZERO));

        film.setWebsite(new URL(aUrlDTO.getUrl()));
        if (description.isPresent()) {
          film.setBeschreibung(description.get());
        }
        
        ArdVideoInfoDTO videoInfo = videoInfoOptional.get();
        if (StringUtils.isNotBlank(videoInfo.getSubtitleUrl())) {
          film.addSubtitle(new URL(addMissingProtocol(videoInfo.getSubtitleUrl())));
        }
        
        addUrls(film, videoInfo.getVideoUrls());
        
        taskResults.add(film);
        crawler.incrementAndGetActualCount();
        crawler.updateProgress();
      } else {
        LOG.error("SrFilmDetailTask: no title or video found for url " + aUrlDTO.getUrl());
        crawler.incrementAndGetErrorCount();
        crawler.updateProgress();
      }
    } catch (MalformedURLException ex) {
      LOG.fatal("A SR URL can't be parsed.", ex);
      crawler.printErrorMessage();
      crawler.incrementAndGetErrorCount();
      crawler.updateProgress();
    }
  }

  @Override
  protected AbstractUrlTask<Film, SrTopicUrlDTO> createNewOwnInstance(ConcurrentLinkedQueue<SrTopicUrlDTO> aURLsToCrawl) {
    return new SrFilmDetailTask(crawler, aURLsToCrawl);
  }
  
  /**
   * checks whether the multimedia type is relevant
   * @param aDocument the html document
   * @return true if document is relevant
   */
  private boolean isRelevantType(final Document aDocument) {
    // ignore all documents who does not contain any video
    Optional<String> type = getDetailElement(aDocument, 0);
    return type.isPresent() && type.get().toLowerCase().contains("video");
  }
  
  private void addUrls(final Film aFilm, final Map<Resolution, String> aVideoUrls)
    throws MalformedURLException {
    
    for (final Map.Entry<Resolution, String> qualitiesEntry : aVideoUrls.entrySet()) {
      aFilm.addUrl(qualitiesEntry.getKey(), CrawlerTool.stringToFilmUrl(qualitiesEntry.getValue()));
    }
  }
  
  private Optional<ArdVideoInfoDTO> parseUrls(Document aDocument) {
      Optional<String> videoDetailUrl = HtmlDocumentUtils.getElementAttributeString(VIDEO_DETAIL_SELECTOR, VIDEO_DETAIL_ATTRIBUTE, aDocument);
      if (videoDetailUrl.isPresent()) {
        Gson gson = new GsonBuilder()
          .registerTypeAdapter(ArdVideoInfoDTO.class, new ArdVideoInfoJsonDeserializer(crawler))
          .create();

        String url = videoDetailUrl.get();
        url = addMissingProtocol(url);
        
        try {
          ArdVideoInfoDTO dto = gson.fromJson(new InputStreamReader(new URL(url).openStream()),
                          ArdVideoInfoDTO.class);
          return Optional.of(dto);
        } catch (IOException ex) {
          LOG.fatal(ex);
        }      
      }  
      
      return Optional.empty();
  }
  
  private static Optional<String> parseDescription(Document aDocument) {
    Elements x = aDocument.select(DESCRIPTION_SELECTOR);
    if (x.size() == 1) {
      Node node = x.first().nextSibling();
      return Optional.of(node.toString());
    }
    
    return Optional.empty();
  }
  
  private static Optional<LocalDateTime> parseDate(Document aDocument) {
    Optional<String> date = getDetailElement(aDocument, 1);
    if (date.isPresent()) {
      String isoDate = DateUtils.changeDateTimeForMissingISO8601Support(date.get().trim());
      try {
        LocalDateTime localDate = LocalDateTime.parse(isoDate + " 00:00", DATE_TIME_FORMATTER);
        return Optional.of(localDate);
      } catch(DateTimeParseException e) {
        try {
          LocalDateTime localDate = LocalDateTime.parse(isoDate + " 00:00", DATE_TIME_FORMATTER_ENG);
          return Optional.of(localDate);
        } catch(DateTimeParseException e1) {
          LOG.fatal(e1);
        }
      }
    }
    
    return Optional.empty();
  }
  
  private static Optional<Duration> parseDuration(Document aDocument) {
    Optional<String> duration = getDetailElement(aDocument, 2);
    if (duration.isPresent()) {
      String[] parts = duration.get().replace("Dauer:", "").trim().split(":");
      if (parts.length == 3) {
        return Optional.of(
          Duration.ofHours(Long.parseLong(parts[0]))
            .plusMinutes(Long.parseLong(parts[1]))
            .plusSeconds(Long.parseLong(parts[2]))
        );
      } else {
        LOG.debug("SrFilmDetailTask: unknown duration part count: " + duration.get());
      }
    }
    
    return Optional.empty();
  }
  
  private static Optional<String> getDetailElement(Document aDocument, int index) {
    Optional<String> details = HtmlDocumentUtils.getElementString(DETAILS_SELECTOR, aDocument);
    if (details.isPresent()) {
      String[] parts = details.get().split("\\|");
      if (parts.length == 4) {
        return Optional.of(parts[index]);
      } else {
        LOG.debug("SrFilmDetailTask: unknown details part count: " + details.get());
      }
    }
    
    return Optional.empty();
  }
  
  private static String addMissingProtocol(final String aUrl) {
    if (aUrl.startsWith("//")) {
      return "https:" + aUrl;
    }    
    
    return aUrl;
  }
}